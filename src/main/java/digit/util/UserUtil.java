package digit.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import digit.config.Configuration;
import static digit.config.PGRConstants.*;
import org.egov.common.contract.request.Role;
import digit.repository.ServiceRequestRepository;
import digit.web.models.User;
import digit.web.models.UserDetailResponse;

import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class UserUtil {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private Configuration configs;

    /**
     * Returns UserDetailResponse by calling the user service with the given URI and
     * object.
     * 
     * @param userRequest Request object for the user service.
     * @param uri         The address of the user service endpoint.
     * @return Response from the user service, parsed as UserDetailResponse.
     */
    public UserDetailResponse userCall(Object userRequest, StringBuilder uri) {
        String dobFormat = null;

        // Determine the appropriate date of birth format based on the URI
        if (uri.toString().contains(configs.getUserSearchEndpoint())
                || uri.toString().contains(configs.getUserUpdateEndpoint())) {
            dobFormat = DOB_FORMAT_Y_M_D;
        } else if (uri.toString().contains(configs.getUserCreateEndpoint())) {
            dobFormat = DOB_FORMAT_D_M_Y;
        }

        try {
            // Fetch the response from the user service
            LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, userRequest);

            // Parse the response map to convert date fields to long values
            parseResponse(responseMap, dobFormat);

            // Convert the response map to UserDetailResponse and return it
            UserDetailResponse userDetailResponse = mapper.convertValue(responseMap, UserDetailResponse.class);
            return userDetailResponse;

        } catch (IllegalArgumentException e) {
            // Handle any exception that occurs during object mapping
            throw new CustomException(ILLEGAL_ARGUMENT_EXCEPTION_CODE, OBJECTMAPPER_UNABLE_TO_CONVERT);
        }
    }

    /**
     * Parses date fields in the response map and converts them to long values
     * representing timestamps.
     * 
     * @param responseMap LinkedHashMap obtained from the user API response.
     * @param dobFormat   The date format to be used for parsing the date of birth.
     */
    public void parseResponse(LinkedHashMap responseMap, String dobFormat) {
        // Extract users from the response map
        List<LinkedHashMap> users = (List<LinkedHashMap>) responseMap.get(USER);
        String format1 = DOB_FORMAT_D_M_Y_H_M_S;

        // If users are present, parse their date fields
        if (users != null) {
            users.forEach(map -> {
                // Parse and convert createdDate to long
                map.put(CREATED_DATE, dateTolong((String) map.get(CREATED_DATE), format1));

                // Parse and convert lastModifiedDate to long if present
                if ((String) map.get(LAST_MODIFIED_DATE) != null) {
                    map.put(LAST_MODIFIED_DATE, dateTolong((String) map.get(LAST_MODIFIED_DATE), format1));
                }

                // Parse and convert date of birth to long if present
                if ((String) map.get(DOB) != null) {
                    map.put(DOB, dateTolong((String) map.get(DOB), dobFormat));
                }

                // Parse and convert password expiry date to long if present
                if ((String) map.get(PWD_EXPIRY_DATE) != null) {
                    map.put(PWD_EXPIRY_DATE, dateTolong((String) map.get(PWD_EXPIRY_DATE), format1));
                }
            });
        }
    }

    /**
     * Converts a date string to a long value representing the timestamp.
     * 
     * @param date   The date string to be parsed.
     * @param format The format of the date string.
     * @return The long value representing the timestamp of the date.
     */
    private Long dateTolong(String date, String format) {
        SimpleDateFormat f = new SimpleDateFormat(format);
        Date d = null;
        try {
            // Parse the date string into a Date object
            d = f.parse(date);
        } catch (ParseException e) {
            // Handle parsing exception if the date format is invalid
            throw new CustomException(INVALID_DATE_FORMAT_CODE, INVALID_DATE_FORMAT_MESSAGE);
        }
        return d.getTime(); // Return the timestamp as a long value
    }

    /**
     * Enriches the user information with state-level tenant ID and other default
     * fields.
     * The function sets the username as the mobile number.
     * 
     * @param mobileNumber The mobile number of the user.
     * @param tenantId     The tenant ID for the user.
     * @param userInfo     The user object to be enriched with default fields.
     * @param userType     The type of user (e.g., "CITIZEN").
     */
    public void addUserDefaultFields(String mobileNumber, String tenantId, User userInfo, String userType) {
        // Set the role for the user (as a citizen)
        Role role = getCitizenRole(tenantId);
        userInfo.setRoles(Collections.singletonList(role));

        // Set the user type, username (mobile number), and tenant ID
        userInfo.setType(userType);
        userInfo.setUserName(mobileNumber);
        userInfo.setTenantId(getStateLevelTenant(tenantId));

        userInfo.setActive(true);
    }

    /**
     * Returns a Role object for a citizen with default values.
     * 
     * @param tenantId The tenant ID for the citizen.
     * @return A Role object representing a citizen role.
     */
    private Role getCitizenRole(String tenantId) {
        Role role = Role.builder().build();
        role.setCode(CITIZEN_UPPER); // Set role code to "CITIZEN"
        role.setName(CITIZEN_LOWER); // Set role name to "citizen"
        role.setTenantId(getStateLevelTenant(tenantId)); // Set the tenant ID
        return role;
    }

    /**
     * Extracts the state-level tenant ID from the full tenant ID.
     * 
     * @param tenantId The full tenant ID (e.g., "stateCode.tenantId").
     * @return The state-level tenant ID (e.g., "stateCode").
     */
    public String getStateLevelTenant(String tenantId) {
        return tenantId.split("\\.")[0]; // Split by '.' and return the first part (state-level)
    }
}