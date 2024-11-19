package digit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.models.Workflow;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.contract.workflow.BusinessService;
import org.egov.common.contract.workflow.BusinessServiceResponse;
import org.egov.common.contract.workflow.ProcessInstance;
import org.egov.common.contract.workflow.ProcessInstanceRequest;
import org.egov.common.contract.workflow.ProcessInstanceResponse;
import org.egov.common.contract.workflow.State;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import digit.config.Configuration;
import digit.config.PGRConstants;
import digit.repository.ServiceRequestRepository;
import digit.web.models.PGREntity;
import digit.web.models.RequestInfoWrapper;
import digit.web.models.Service;
import digit.web.models.ServiceRequest;

@Component
public class WorkflowService {

    @Autowired
    private PGRConstants pgrConstants;

    @Autowired
    private Configuration pgrConfiguration;

    @Autowired
    private ServiceRequestRepository repository;

    @Autowired
    private ObjectMapper mapper;

    /**
     * Updates the workflow status of the given service request by interacting with
     * the workflow service.
     *
     * @param serviceRequest The service request containing the workflow details to
     *                       be updated.
     * @return The updated application status from the workflow.
     */
    public String updateWorkflowStatus(ServiceRequest serviceRequest) {
        // Prepare the ProcessInstance object for the PGR service based on the service
        // request
        ProcessInstance processInstance = getProcessInstanceForPGR(serviceRequest);

        // Create a ProcessInstanceRequest to send to the workflow service
        ProcessInstanceRequest workflowRequest = new ProcessInstanceRequest(
                serviceRequest.getRequestInfo(),
                Collections.singletonList(processInstance));

        // Call the workflow service to update the process and retrieve the current
        // state
        State state = callWorkFlow(workflowRequest);

        // Update the application status in the service request with the status from the
        // workflow response
        serviceRequest.getPgrEntity().getService().setApplicationStatus(state.getApplicationStatus());

        // Return the updated application status
        return state.getApplicationStatus();
    }

    /**
     * Constructs the URL with query parameters for searching the workflow service
     * for a specific tenant and business service.
     *
     * @param tenantId        The tenant ID to include in the query.
     * @param businessService The business service name to include in the query.
     * @return A StringBuilder object containing the constructed URL.
     */
    private StringBuilder getSearchURLWithParams(String tenantId, String businessService) {
        // Initialize the URL with the workflow host configuration
        StringBuilder url = new StringBuilder(pgrConfiguration.getWfHost());

        // Append the path for business service search
        url.append(pgrConfiguration.getWfBusinessServiceSearchPath());

        // Add tenant ID as a query parameter
        url.append("?tenantId=");
        url.append(tenantId);

        // Add business service name as a query parameter
        url.append("&businessServices=");
        url.append(businessService);

        // Return the constructed URL
        return url;
    }

    public List<PGREntity> enrichWorkflow(RequestInfo requestInfo, List<PGREntity> serviceWrappers) {
        Map<String, List<PGREntity>> tenantIdToServiceWrapperMap = getTenantIdToServiceWrapperMap(serviceWrappers);

        List<PGREntity> enrichedServiceWrappers = new ArrayList<>();

        for (String tenantId : tenantIdToServiceWrapperMap.keySet()) {

            List<String> serviceRequestIds = new ArrayList<>();

            List<PGREntity> tenantSpecificWrappers = tenantIdToServiceWrapperMap.get(tenantId);

            tenantSpecificWrappers.forEach(pgrEntity -> {
                serviceRequestIds.add(pgrEntity.getService().getServiceRequestId());
            });

            RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();

            StringBuilder searchUrl = getprocessInstanceSearchURL(tenantId, StringUtils.join(serviceRequestIds, ','));
            Object result = repository.fetchResult(searchUrl, requestInfoWrapper);

            ProcessInstanceResponse processInstanceResponse = null;
            try {
                processInstanceResponse = mapper.convertValue(result, ProcessInstanceResponse.class);
            } catch (IllegalArgumentException e) {
                throw new CustomException("PARSING ERROR",
                        "Failed to parse response of workflow processInstance search");
            }

            if (CollectionUtils.isEmpty(processInstanceResponse.getProcessInstances())
                    || processInstanceResponse.getProcessInstances().size() != serviceRequestIds.size())
                throw new CustomException("WORKFLOW_NOT_FOUND", "The workflow object is not found");

            Map<String, Workflow> businessIdToWorkflow = getWorkflow(processInstanceResponse.getProcessInstances());

            tenantSpecificWrappers.forEach(pgrEntity -> {
                pgrEntity.setWorkflow(businessIdToWorkflow.get(pgrEntity.getService().getServiceRequestId()));
            });

            enrichedServiceWrappers.addAll(tenantSpecificWrappers);
        }
        return enrichedServiceWrappers;
    }

    private Map<String, List<PGREntity>> getTenantIdToServiceWrapperMap(List<PGREntity> pgrEntities) {
        Map<String, List<PGREntity>> resultMap = new HashMap<>();
        for (PGREntity pgrEntity : pgrEntities) {
            if (resultMap.containsKey(pgrEntity.getService().getTenantId())) {
                resultMap.get(pgrEntity.getService().getTenantId()).add(pgrEntity);
            } else {
                List<PGREntity> serviceWrapperList = new ArrayList<>();
                serviceWrapperList.add(pgrEntity);
                resultMap.put(pgrEntity.getService().getTenantId(), serviceWrapperList);
            }
        }
        return resultMap;
    }

    private ProcessInstance getProcessInstanceForPGR(ServiceRequest request) {
        Service service = request.getPgrEntity().getService();
        Workflow workflow = request.getPgrEntity().getWorkflow();

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setBusinessId(service.getServiceRequestId());
        processInstance.setAction(request.getPgrEntity().getWorkflow().getAction());
        processInstance.setModuleName(pgrConstants.PGR_MODULENAME);
        processInstance.setTenantId(service.getTenantId());
        processInstance.setBusinessService(getBusinessService(request).getBusinessService());
        processInstance.setDocuments(request.getPgrEntity().getWorkflow().getDocuments());
        processInstance.setComment(workflow.getComments());

        if (!CollectionUtils.isEmpty(workflow.getAssignes())) {
            List<User> users = new ArrayList<>();

            workflow.getAssignes().forEach(uuid -> {
                User user = new User();
                user.setUuid(uuid);
                users.add(user);
            });

            processInstance.setAssignes(users);
        }
        return processInstance;
    }

    public BusinessService getBusinessService(ServiceRequest serviceRequest) {
        String tenantId = serviceRequest.getPgrEntity().getService().getTenantId();
        StringBuilder url = getSearchURLWithParams(tenantId, pgrConstants.PGR_BUSINESSSERVICE);
        RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder()
                .requestInfo(serviceRequest.getRequestInfo()).build();
        Object result = repository.fetchResult(url, requestInfoWrapper);
        BusinessServiceResponse response = null;
        try {
            response = mapper.convertValue(result, BusinessServiceResponse.class);
        } catch (IllegalArgumentException e) {
            throw new CustomException("PARSING ERROR", "Failed to parse response of workflow business service search");
        }

        if (CollectionUtils.isEmpty(response.getBusinessServices()))
            throw new CustomException("BUSINESSSERVICE_NOT_FOUND",
                    "The businessService " + pgrConstants.PGR_BUSINESSSERVICE + " is not found");

        return response.getBusinessServices().get(0);
    }

    public Map<String, Workflow> getWorkflow(List<ProcessInstance> processInstances) {
        Map<String, Workflow> businessIdToWorkflow = new HashMap<>();
        processInstances.forEach(processInstance -> {
            List<String> userIds = null;

            if (!CollectionUtils.isEmpty(processInstance.getAssignes())) {
                userIds = processInstance.getAssignes().stream().map(User::getUuid).collect(Collectors.toList());
            }

            Workflow workflow = Workflow.builder()
                    .action(processInstance.getAction())
                    .assignes(userIds)
                    .comments(processInstance.getComment())
                    .documents(processInstance.getDocuments())
                    .build();

            businessIdToWorkflow.put(processInstance.getBusinessId(), workflow);
        });
        return businessIdToWorkflow;
    }

    /**
     * Calls the workflow service with the given request and returns the current
     * state of the process.
     *
     * @param workflowReq The request to send to the workflow service.
     * @return The current state of the process.
     */
    private State callWorkFlow(ProcessInstanceRequest workflowReq) {
        ProcessInstanceResponse response = null;
        StringBuilder url = new StringBuilder(
                pgrConfiguration.getWfHost().concat(pgrConfiguration.getWfTransitionPath()));
        Object optional = repository.fetchResult(url, workflowReq);

        // Convert the response to ProcessInstanceResponse
        response = mapper.convertValue(optional, ProcessInstanceResponse.class);

        // Return the current state of the process
        return response.getProcessInstances().get(0).getState();
    }

    /**
     * Creates the URL for searching the workflow service for a process instance.
     *
     * @param tenantId         The tenant ID to search for.
     * @param serviceRequestId The ID of the service request to search for.
     * @return The URL string for searching the workflow service.
     */
    public StringBuilder getprocessInstanceSearchURL(String tenantId, String serviceRequestId) {

        // Construct the URL
        StringBuilder url = new StringBuilder(pgrConfiguration.getWfHost());

        // Append the full path
        url.append(pgrConfiguration.getWfProcessInstanceSearchPath());
        url.append("?tenantId=");
        url.append(tenantId);
        url.append("&businessIds=");
        url.append(serviceRequestId);

        // Return the constructed URL
        return url;
    }

}
