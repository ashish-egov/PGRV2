package digit.service;

import java.util.ArrayList;
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

    public void enrichCreateRequest(ServiceRequest requestBody) {
        RequestInfo requestInfo = requestBody.getRequestInfo();
        Service service = requestBody.getPgrEntity().getService();
        Workflow workflow = requestBody.getPgrEntity().getWorkflow();
        String tenantId = service.getTenantId();

        // Enrich accountId of the logged in citizen
        if (requestInfo.getUserInfo().getType().equalsIgnoreCase(pgrConstants.USERTYPE_CITIZEN))
            requestBody.getPgrEntity().getService().setAccountId(requestInfo.getUserInfo().getUuid());

        userService.callUserService(requestBody);

        AuditDetails auditDetails = pgrUtils.getAuditDetails(requestInfo.getUserInfo().getUuid(), service, true);

        service.setAuditDetails(auditDetails);
        service.setId(UUID.randomUUID().toString());
        service.getAddress().setId(UUID.randomUUID().toString());
        service.getAddress().setTenantId(tenantId);
        service.setActive(true);

        if (workflow.getDocuments() != null) {
            workflow.getDocuments().forEach(document -> {
                document.setId(UUID.randomUUID().toString());
            });
        }

        if (StringUtils.isEmpty(service.getAccountId()))
            service.setAccountId(service.getCitizen().getUuid());

        List<String> customIds = getIdList(requestInfo, tenantId, config.getServiceRequestIdGenName(),
                config.getServiceRequestIdGenFormat(), 1);

        service.setServiceRequestId(customIds.get(0));
    }

    public void enrichSearchRequest(RequestInfo requestInfo, RequestSearchCriteria criteria) {

        if (criteria.isEmpty() && requestInfo.getUserInfo().getType().equalsIgnoreCase(pgrConstants.USERTYPE_CITIZEN)) {
            String citizenMobileNumber = requestInfo.getUserInfo().getUserName();
            criteria.setMobileNumber(citizenMobileNumber);
        }

        criteria.setAccountId(requestInfo.getUserInfo().getUuid());

        String tenantId = (criteria.getTenantId() != null) ? criteria.getTenantId()
                : requestInfo.getUserInfo().getTenantId();

        if (criteria.getMobileNumber() != null) {
            userService.enrichUserIds(tenantId, criteria);
        }

        if (criteria.getLimit() == null)
            criteria.setLimit(config.getDefaultLimit());

        if (criteria.getOffset() == null)
            criteria.setOffset(config.getDefaultOffset());

        if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxLimit())
            criteria.setLimit(config.getMaxLimit());

    }

    private List<String> getIdList(RequestInfo requestInfo, String tenantId, String idKey,
            String idformat, int count) {
        // List<IdResponse> idResponses = idGenRepository.getId(requestInfo, tenantId,
        // idKey, idformat, count)
        // .getIdResponses();

        List<IdResponse> idResponses = new ArrayList<>();
        IdResponse idResponse = new IdResponse();
        idResponse.setId("PGR-" + java.time.LocalDate.now() + "-" + java.time.Instant.now().toEpochMilli());
        idResponses.add(idResponse);

        if (CollectionUtils.isEmpty(idResponses))
            throw new CustomException("IDGEN ERROR", "No ids returned from idgen Service");

        return idResponses.stream()
                .map(IdResponse::getId).collect(Collectors.toList());
    }
}
