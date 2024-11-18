package digit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import digit.config.Configuration;
import digit.kafka.Producer;
import digit.util.ResponseInfoFactory;
import digit.validator.PgrValidator;
import digit.web.models.RequestSearchCriteria;
import digit.web.models.Service;
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
    public ServiceResponse search(RequestInfo requestInfo, RequestSearchCriteria criteria) {
        // Validate the search criteria using a validator
        pgrValidator.validateSearch(requestInfo, criteria);

        // Enrich the search request, modifying the criteria or adding necessary data
        enrichmentService.enrichSearchRequest(requestInfo, criteria);

        // Create a ResponseInfo object from the incoming request info, indicating a
        // successful response (true)
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);

        // Build a ServiceResponse object with the responseInfo and an empty list of PGR
        // entities
        ServiceResponse response = ServiceResponse.builder()
                .responseInfo(responseInfo) // Set the response information
                .pgREntities(new ArrayList<>()) // TODO : Make search service to return list of PGR entities
                .build();

        // Return the created ServiceResponse object
        return response;
    }

}
