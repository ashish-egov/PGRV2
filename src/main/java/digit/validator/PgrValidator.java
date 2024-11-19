package digit.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import digit.config.Configuration;
import digit.config.ErrorConstants;
import digit.config.PGRConstants;
import digit.repository.PGRRepository;
import digit.util.HRMSUtil;
import digit.util.MdmsUtil;
import digit.web.models.MdmsResponseV2;
import digit.web.models.PGREntity;
import digit.web.models.RequestSearchCriteria;
import digit.web.models.Service;
import digit.web.models.ServiceRequest;
import digit.web.models.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PgrValidator {

    @Autowired
    private PGRConstants pgrConstants;
    @Autowired
    private Configuration config;

    @Autowired
    private MdmsUtil mdmsUtil;

    @Autowired
    private PGRRepository pgrRepository;

    @Autowired
    private HRMSUtil hrmsUtil;

    @Autowired
    private ErrorConstants errorConstants;

    /**
     * This method validates the creation request for the ServiceRequest object.
     * It ensures that both citizen and source are valid.
     * 
     * @param requestBody The ServiceRequest object to validate
     */
    public void validateCreateRequest(ServiceRequest requestBody) {
        // Validate the citizen object and source within the service request
        validateCitizen(requestBody);
        validateSource(requestBody.getPgrEntity().getService().getSource());
        validateMDMS(requestBody);
    }

    /**
     * This method validates the citizen object within the ServiceRequest.
     * It checks that the citizen object is not null and that required fields
     * (mobileNumber, userName) are present.
     * If the request is from an EMPLOYEE, additional checks are performed.
     *
     * @param requestBody The ServiceRequest object containing the citizen details
     *                    to validate
     */
    public void validateCitizen(ServiceRequest requestBody) {
        // Initialize a map to store validation errors
        Map<String, String> errorMap = new HashMap<>();

        // Check if the user is an EMPLOYEE (as per the constant value from
        // ServiceConstants)
        if (requestBody.getRequestInfo().getUserInfo().getType().equalsIgnoreCase(pgrConstants.USERTYPE_EMPLOYEE)) {

            // Ensure that the service and citizen objects are not null in the request body
            if (requestBody != null && requestBody.getPgrEntity() != null) {
                User citizen = requestBody.getPgrEntity().getService().getCitizen();

                // Check if the citizen object is null
                if (citizen == null) {
                    errorMap.put("INVALID_REQUEST", "Citizen object cannot be null");
                } else {
                    // Validate that mobile number and username are not null for the citizen object
                    if (citizen.getMobileNumber() == null || citizen.getUserName() == null) {
                        errorMap.put("INVALID_REQUEST",
                                "Mobile number and username are required in the citizen object");
                    }
                }
            } else {
                // If the service or citizen object is missing, add error to the map
                errorMap.put("INVALID_REQUEST", "Service or Citizen object is missing");
            }

            // If there are validation errors, throw an exception with the error map
            if (!errorMap.isEmpty()) {
                throw new CustomException(errorMap); // CustomException handles the error
            }
        }
    }

    /**
     * This method validates the source value in the service request.
     * It checks if the source is present in the allowed sources list configured in
     * the application.
     * 
     * @param source The source value to validate
     */
    private void validateSource(String source) {
        // Fetch the list of allowed sources from configuration and split by comma
        List<String> allowedSourceStr = Arrays.asList(config.getAllowedSource().split(","));

        // If the source is not present in the allowed sources list, throw a custom
        // exception
        if (!allowedSourceStr.contains(source)) {
            throw new CustomException("INVALID_SOURCE", "The source: " + source + " is not valid");
        }
    }

    public void validateSearch(RequestInfo requestInfo, RequestSearchCriteria criteria) {
        if (criteria.getTenantId() == null)
            throw new CustomException(errorConstants.INVALID_SEARCH, "TenantId is mandatory search param");
        validateSearchParam(requestInfo, criteria);
    }

    private void validateSearchParam(RequestInfo requestInfo, RequestSearchCriteria criteria) {

        if (requestInfo.getUserInfo().getType().equalsIgnoreCase("EMPLOYEE") && criteria.isEmpty())
            throw new CustomException(errorConstants.INVALID_SEARCH, "Search without params is not allowed");

        if (requestInfo.getUserInfo().getType().equalsIgnoreCase("EMPLOYEE")
                && criteria.getTenantId().split("\\.").length == 1) {
            throw new CustomException(errorConstants.INVALID_SEARCH, "Employees cannot perform state level searches.");
        }

        String allowedParamStr = null;

        if (requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN"))
            allowedParamStr = config.getAllowedCitizenSearchParameters();
        else if (requestInfo.getUserInfo().getType().equalsIgnoreCase("EMPLOYEE")
                || requestInfo.getUserInfo().getType().equalsIgnoreCase("SYSTEM"))
            allowedParamStr = config.getAllowedEmployeeSearchParameters();
        else
            throw new CustomException(errorConstants.INVALID_SEARCH,
                    "The userType: " + requestInfo.getUserInfo().getType() +
                            " does not have any search config");

        List<String> allowedParams = Arrays.asList(allowedParamStr.split(","));

        if (criteria.getServiceCode() != null && !allowedParams.contains("serviceCode"))
            throw new CustomException(errorConstants.INVALID_SEARCH, "Search on serviceCode is not allowed");

        if (criteria.getServiceRequestId() != null && !allowedParams.contains("serviceRequestId"))
            throw new CustomException(errorConstants.INVALID_SEARCH, "Search on serviceRequestId is not allowed");

        if (criteria.getApplicationStatus() != null && !allowedParams.contains("applicationStatus"))
            throw new CustomException(errorConstants.INVALID_SEARCH, "Search on applicationStatus is not allowed");

        if (criteria.getMobileNumber() != null && !allowedParams.contains("mobileNumber"))
            throw new CustomException(errorConstants.INVALID_SEARCH, "Search on mobileNumber is not allowed");

        if (criteria.getIds() != null && !allowedParams.contains("ids"))
            throw new CustomException(errorConstants.INVALID_SEARCH, "Search on ids is not allowed");

    }

    /**
     * This method validates the MDMS (Master Data Management System) response based
     * on the service code.
     * It ensures that the service code exists in the MDMS data, and if not, an
     * exception is thrown.
     *
     * @param request  The ServiceRequest object containing the service code to
     *                 validate
     * @param mdmsData The MDMS data (JSON) to check against
     */
    private void validateMDMS(ServiceRequest requestBody) {
        // Fetch MDMS data based on the request
        MdmsResponseV2 mdmsData = mdmsUtil.fetchMdmsData(
                requestBody,
                requestBody.getPgrEntity().getService().getTenantId(),
                requestBody.getPgrEntity().getService().getServiceCode());

        // Check if the fetched MDMS data is null or contains no entries
        if (mdmsData == null || mdmsData.getMdms() == null || mdmsData.getMdms().isEmpty()) {
            throw new CustomException(
                    "INVALID_SERVICECODE",
                    "The service code: " + requestBody.getPgrEntity().getService().getServiceCode()
                            + " is not present in MDMS");
        }
    }

    public void validateUpdate(ServiceRequest request) {
        String id = request.getPgrEntity().getService().getId();
        validateSource(request.getPgrEntity().getService().getSource());
        validateMDMSAndDepartment(request);
        validateReOpen(request);
        RequestSearchCriteria criteria = RequestSearchCriteria.builder().ids(Collections.singleton(id)).build();
        criteria.setIsPlainSearch(false);
        List<PGREntity> serviceWrappers = pgrRepository.getServiceWrappers(criteria);

        if (CollectionUtils.isEmpty(serviceWrappers))
            throw new CustomException("INVALID_UPDATE", "The record that you are trying to update does not exists");

    }

    private void validateMDMSAndDepartment(ServiceRequest requestBody) {
        MdmsResponseV2 mdmsData = mdmsUtil.fetchMdmsData(
                requestBody,
                requestBody.getPgrEntity().getService().getTenantId(),
                requestBody.getPgrEntity().getService().getServiceCode());

        // Check if the fetched MDMS data is null or contains no entries
        if (mdmsData == null || mdmsData.getMdms() == null || mdmsData.getMdms().isEmpty()) {
            throw new CustomException(
                    "INVALID_SERVICECODE",
                    "The service code: " + requestBody.getPgrEntity().getService().getServiceCode()
                            + " is not present in MDMS");
        }

        List<String> assignes = requestBody.getPgrEntity().getWorkflow().getAssignes();

        if (CollectionUtils.isEmpty(assignes))
            return;

        List<String> departments = hrmsUtil.getDepartment(assignes, requestBody.getRequestInfo());

        JsonNode dataNode = mdmsData.getMdms().get(0).getData(); // Get the `data` JsonNode

        String departmentServiceCode = null;
        if (dataNode != null && dataNode.has("ServiceCode")) {
            departmentServiceCode = dataNode.get("ServiceCode").asText();
        }

        Map<String, String> errorMap = new HashMap<>();

        if (!departments.contains(departmentServiceCode))
            errorMap.put("INVALID_ASSIGNMENT",
                    "The application cannot be assigned to employee of department: " + departments.toString());

        if (!errorMap.isEmpty())
            throw new CustomException(errorMap);

    }

    /**
     *
     * @param request
     */
    private void validateReOpen(ServiceRequest request) {

        if (!request.getPgrEntity().getWorkflow().getAction().equalsIgnoreCase(pgrConstants.PGR_WF_REOPEN))
            return;

        Service service = request.getPgrEntity().getService();
        RequestInfo requestInfo = request.getRequestInfo();
        Long lastModifiedTime = service.getAuditDetails().getLastModifiedTime();

        if (requestInfo.getUserInfo().getType().equalsIgnoreCase(pgrConstants.USERTYPE_CITIZEN)) {
            if (!requestInfo.getUserInfo().getUuid().equalsIgnoreCase(service.getAccountId()))
                throw new CustomException("INVALID_ACTION", "Not authorized to re-open the complain");
        }

        if (System.currentTimeMillis() - lastModifiedTime > config.getComplainMaxIdleTime())
            throw new CustomException("INVALID_ACTION", "Complaint is closed");

    }
}
