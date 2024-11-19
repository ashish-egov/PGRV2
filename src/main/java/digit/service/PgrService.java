package digit.service;

import java.util.Collections;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import digit.config.Configuration;
import digit.kafka.Producer;
import digit.repository.PGRRepository;
import digit.util.PGRUtils;
import digit.util.ResponseInfoFactory;
import digit.validator.PgrValidator;
import digit.web.models.CountResponse;
import digit.web.models.PGREntity;
import digit.web.models.RequestSearchCriteria;
import digit.web.models.SearchRequest;
import digit.web.models.ServiceRequest;
import digit.web.models.ServiceResponse;

@Component
public class PgrService {
    @Autowired
    private PgrValidator pgrValidator;

    @Autowired
    private EnrichmentService enrichmentService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private Producer producer;

    @Autowired
    private Configuration config;

    @Autowired
    private ResponseInfoFactory responseInfoFactory;

    @Autowired
    private PGRRepository pgrRepository;

    @Autowired
    private PGRUtils pgrUtils;

    /**
     * Creates a new service request based on the provided request body.
     *
     * @param requestBody The incoming request body containing the details for the
     *                    new service request.
     *                    It includes information like the service details, user
     *                    info, and request metadata.
     *                    This object will be validated and enriched before
     *                    processing.
     * @return A `ServiceResponse` containing metadata about the request (e.g.,
     *         status, messages) and
     *         a list of PGR entities. The list of entities is currently empty, but
     *         will be populated
     *         as part of the service request creation process.
     */
    public ServiceResponse create(ServiceRequest requestBody) {
        // Validate the request
        pgrValidator.validateCreateRequest(requestBody);

        // Enrich the request
        enrichmentService.enrichCreateRequest(requestBody);

        // Update workflow status
        workflowService.updateWorkflowStatus(requestBody);

        // Push to Kafka topic
        producer.push(config.getPgrCreateTopic(), requestBody.getPgrEntity());

        // Create and return the ServiceResponse
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestBody.getRequestInfo(),
                true);
        ServiceResponse response = ServiceResponse.builder().responseInfo(responseInfo)
                .pgREntities(Collections.singletonList(requestBody.getPgrEntity())).build();
        return response;
    }

    /**
     * Searches for service requests based on the provided search criteria and
     * request information.
     *
     * @param requestInfo Contains metadata about the request, such as the
     *                    requester's details,
     *                    tenant information, and correlation ID. This is used to
     *                    enrich the response
     *                    and provide audit and tracking information.
     * @param criteria    The search criteria used to filter and retrieve the
     *                    relevant service requests.
     *                    It includes parameters like service type, status, date
     *                    range, etc., to narrow down
     *                    the results.
     * @return A `ServiceResponse` containing the search results and metadata. The
     *         response will include
     *         a list of PGR entities that match the search criteria, as well as
     *         additional metadata
     *         like response status and messages.
     */
    public ServiceResponse search(SearchRequest searchRequest) {
        RequestInfo requestInfo = searchRequest.getRequestInfo();
        RequestSearchCriteria criteria = searchRequest.getCriteria();

        // Validate the search criteria
        pgrValidator.validateSearch(requestInfo, criteria);

        // Enrich the search request with additional data
        enrichmentService.enrichSearchRequest(requestInfo, criteria);

        List<PGREntity> sortedServiceWrappers = pgrRepository.getSortedServiceWrappers(requestInfo, criteria);

        // Return the response with the sorted service wrappers
        return pgrUtils.convertToServiceResponse(requestInfo, sortedServiceWrappers);
    }

    /**
     * Updates an existing service request and returns the response. This method
     * first validates the request,
     * enriches the request with additional data, updates the workflow status, and
     * then pushes the request to
     * the update topic. The response includes the updated service request.
     *
     * @param request The service request to update.
     * @return The updated service response.
     */
    public ServiceResponse update(ServiceRequest request) {
        // Validate the update request
        pgrValidator.validateUpdate(request);

        // Enrich the update request
        enrichmentService.enrichUpdateRequest(request);

        // Update workflow status
        workflowService.updateWorkflowStatus(request);

        // Push to Kafka topic
        producer.push(config.getPgrUpdateTopic(), request.getPgrEntity());

        // Create and return the ServiceResponse
        return pgrUtils.convertToServiceResponse(request.getRequestInfo(),
                Collections.singletonList(request.getPgrEntity()));
    }

    /**
     * Retrieves the count of service requests matching the provided search
     * criteria.
     * 
     * @param request The search request containing the search criteria.
     * @return The count of matching service requests wrapped in a
     *         {@link CountResponse}.
     */
    public CountResponse count(SearchRequest request) {
        // Validate the search criteria
        RequestSearchCriteria criteria = request.getCriteria();

        // Enrich the search request with additional data
        RequestInfo requestInfo = request.getRequestInfo();

        // Validate the search criteria
        criteria.setIsPlainSearch(false);

        // Get the count
        Integer count = pgrRepository.getCount(criteria);

        // Create and return the CountResponse
        CountResponse countResponse = CountResponse.builder()
                .responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(
                        requestInfo, true))
                .count(count).build();

        // Return the response
        return countResponse;
    }

}
