package digit.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.jayway.jsonpath.JsonPath;
import digit.config.Configuration;
import digit.config.PGRConstants;
import digit.web.models.RequestSearchCriteria;
import digit.web.models.ServiceRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PgrValidator {

    @Autowired
    private PGRConstants pgrConstants;
    @Autowired
    private Configuration config;

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
        // validateMDMS(requestBody, requestBody);
    }

    public void validateUpdateRequest(ServiceRequest requestBody) {

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
            throw new CustomException("INVALID_SEARCH", "TenantId is mandatory search param");
        validateSearchParam(requestInfo, criteria);
    }

    private void validateSearchParam(RequestInfo requestInfo, RequestSearchCriteria criteria) {

        if (requestInfo.getUserInfo().getType().equalsIgnoreCase("EMPLOYEE") && criteria.isEmpty())
            throw new CustomException("INVALID_SEARCH", "Search without params is not allowed");

        if (requestInfo.getUserInfo().getType().equalsIgnoreCase("EMPLOYEE")
                && criteria.getTenantId().split("\\.").length == 1) {
            throw new CustomException("INVALID_SEARCH", "Employees cannot perform state level searches.");
        }

        String allowedParamStr = null;

        if (requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN"))
            allowedParamStr = config.getAllowedCitizenSearchParameters();
        else if (requestInfo.getUserInfo().getType().equalsIgnoreCase("EMPLOYEE")
                || requestInfo.getUserInfo().getType().equalsIgnoreCase("SYSTEM"))
            allowedParamStr = config.getAllowedEmployeeSearchParameters();
        else
            throw new CustomException("INVALID SEARCH", "The userType: " + requestInfo.getUserInfo().getType() +
                    " does not have any search config");

        List<String> allowedParams = Arrays.asList(allowedParamStr.split(","));

        if (criteria.getServiceCode() != null && !allowedParams.contains("serviceCode"))
            throw new CustomException("INVALID SEARCH", "Search on serviceCode is not allowed");

        if (criteria.getServiceRequestId() != null && !allowedParams.contains("serviceRequestId"))
            throw new CustomException("INVALID SEARCH", "Search on serviceRequestId is not allowed");

        if (criteria.getApplicationStatus() != null && !allowedParams.contains("applicationStatus"))
            throw new CustomException("INVALID SEARCH", "Search on applicationStatus is not allowed");

        if (criteria.getMobileNumber() != null && !allowedParams.contains("mobileNumber"))
            throw new CustomException("INVALID SEARCH", "Search on mobileNumber is not allowed");

        if (criteria.getIds() != null && !allowedParams.contains("ids"))
            throw new CustomException("INVALID SEARCH", "Search on ids is not allowed");

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
    // private void validateMDMS(ServiceRequest requestBody) {
    // Object mdmsData = fetchMdmsData(requestBody);
    // // Fetch the service code from the request
    // String serviceCode =
    // requestBody.getPgrEntity().getService().getServiceCode();

    // // Define the JSONPath to search for the service definition in MDMS
    // String jsonPath = pgrConstants.MDMS_SERVICEDEF_SEARCH.replace("{SERVICEDEF}",
    // serviceCode);
    // List<Object> res = null;

    // try {
    // // Use JSONPath to search the mdmsData for the relevant service code
    // res = JsonPath.read(mdmsData, jsonPath);
    // } catch (Exception e) {
    // // If JSON parsing fails, throw an exception
    // throw new CustomException("JSONPATH_ERROR", "Failed to parse mdms response");
    // }

    // // If the response is empty, it means the service code was not found in MDMS
    // if (res == null || res.isEmpty()) {
    // throw new CustomException("INVALID_SERVICECODE",
    // "The service code: " + serviceCode + " is not present in MDMS");
    // }
    // }
}
