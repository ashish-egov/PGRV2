package digit.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.egov.common.contract.models.AuditDetails;
import org.egov.common.contract.models.Workflow;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import digit.config.Configuration;
import digit.config.PGRConstants;
import digit.repository.IdGenRepository;
import digit.util.PGRUtils;
import digit.web.models.IdResponse;
import digit.web.models.RequestSearchCriteria;
import digit.web.models.Service;
import digit.web.models.ServiceRequest;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Service for enriching requests during create, search, and update operations.
 */
@Component
public class EnrichmentService {

    @Autowired
    private PGRConstants pgrConstants;

    @Autowired
    private PGRUtils pgrUtils;

    @Autowired
    private Configuration config;

    @Autowired
    private IdGenRepository idGenRepository;

    @Autowired
    private UserService userService;

    /**
     * Enriches the request for service creation with required fields.
     *
     * @param requestBody The service request object containing service details.
     */
    public void enrichCreateRequest(ServiceRequest requestBody) {
        RequestInfo requestInfo = requestBody.getRequestInfo();
        Service service = requestBody.getPgrEntity().getService();
        Workflow workflow = requestBody.getPgrEntity().getWorkflow();
        String tenantId = service.getTenantId();

        // Set accountId for the logged-in citizen
        if (requestInfo.getUserInfo().getType().equalsIgnoreCase(pgrConstants.USERTYPE_CITIZEN))
            requestBody.getPgrEntity().getService().setAccountId(requestInfo.getUserInfo().getUuid());

        // Call the user service to enrich user-related details
        userService.callUserService(requestBody);

        // Generate audit details for the service
        AuditDetails auditDetails = pgrUtils.getAuditDetails(requestInfo.getUserInfo().getUuid(), service, true);
        service.setAuditDetails(auditDetails);

        // Generate unique IDs for the service and its address
        service.setId(UUID.randomUUID().toString());
        service.getAddress().setId(UUID.randomUUID().toString());
        service.getAddress().setTenantId(tenantId);

        // Mark the service as active
        service.setActive(true);

        // Assign unique IDs to each workflow document, if present
        if (workflow.getDocuments() != null) {
            workflow.getDocuments().forEach(document -> {
                document.setId(UUID.randomUUID().toString());
            });
        }

        // Set the accountId if it's not already present
        if (StringUtils.isEmpty(service.getAccountId()))
            service.setAccountId(service.getCitizen().getUuid());

        // Generate a unique service request ID using IDGen
        List<String> customIds = getIdList(requestInfo, tenantId, config.getServiceRequestIdGenName(),
                config.getServiceRequestIdGenFormat(), 1);
        service.setServiceRequestId(customIds.get(0));
    }

    /**
     * Enriches the request criteria for searching services.
     *
     * @param requestInfo The request information object.
     * @param criteria    The search criteria to be enriched.
     */
    public void enrichSearchRequest(RequestInfo requestInfo, RequestSearchCriteria criteria) {

        // If no criteria are provided and the user is a citizen, set the mobile number
        // as the search filter
        if (criteria.isEmpty() && requestInfo.getUserInfo().getType().equalsIgnoreCase(pgrConstants.USERTYPE_CITIZEN)) {
            String citizenMobileNumber = requestInfo.getUserInfo().getUserName();
            criteria.setMobileNumber(citizenMobileNumber);
        }

        // Set the accountId to the UUID of the logged-in user
        criteria.setAccountId(requestInfo.getUserInfo().getUuid());

        // Determine the tenant ID from the criteria or fallback to the user's tenant ID
        String tenantId = (criteria.getTenantId() != null) ? criteria.getTenantId()
                : requestInfo.getUserInfo().getTenantId();

        // If a mobile number is provided, enrich the user IDs based on the tenant
        if (criteria.getMobileNumber() != null) {
            userService.enrichUserIds(tenantId, criteria);
        }

        // Set default limits and offsets if not provided
        if (criteria.getLimit() == null)
            criteria.setLimit(config.getDefaultLimit());

        if (criteria.getOffset() == null)
            criteria.setOffset(config.getDefaultOffset());

        // Ensure the limit does not exceed the configured maximum limit
        if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxLimit())
            criteria.setLimit(config.getMaxLimit());
    }

    /**
     * Generates a list of unique IDs from the IDGen service.
     *
     * @param requestInfo The request information object.
     * @param tenantId    The tenant ID for which IDs are requested.
     * @param idKey       The key for ID generation.
     * @param idformat    The format for the generated IDs.
     * @param count       The number of IDs to generate.
     * @return A list of generated unique IDs.
     */
    private List<String> getIdList(RequestInfo requestInfo, String tenantId, String idKey,
            String idformat, int count) {
        List<IdResponse> idResponses = idGenRepository.getId(requestInfo, tenantId,
                idKey, idformat, count)
                .getIdResponses();

        // Throw an exception if no IDs are returned from the IDGen service
        if (CollectionUtils.isEmpty(idResponses))
            throw new CustomException("IDGEN ERROR", "No ids returned from idgen Service");

        // Extract and return the generated IDs from the response
        return idResponses.stream()
                .map(IdResponse::getId).collect(Collectors.toList());
    }

    /**
     * Enriches the request for updating a service with necessary fields.
     *
     * @param serviceRequest The service request to be updated.
     */
    public void enrichUpdateRequest(ServiceRequest serviceRequest) {
        RequestInfo requestInfo = serviceRequest.getRequestInfo();
        Service service = serviceRequest.getPgrEntity().getService();

        // Generate audit details for the service during update
        AuditDetails auditDetails = pgrUtils.getAuditDetails(requestInfo.getUserInfo().getUuid(), service, false);
        service.setAuditDetails(auditDetails);

        // Call the user service to enrich user-related details
        userService.callUserService(serviceRequest);
    }
}
