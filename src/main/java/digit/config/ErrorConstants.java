package digit.config;

import org.springframework.stereotype.Component;

@Component
public class ErrorConstants {
    // TODO : Need to add more error codes
    public static final String PARSING_ERROR_CODE = "PARSING_ERROR";
    public static final String INVALID_SEARCH = "INVALID_SEARCH";
    public static final String INVALID_ACCOUNTID = "INVALID_ACCOUNTID";

    public static final String FAILED_TO_PARSE_BUSINESS_SERVICE_SEARCH = "Failed to parse response of workflow business service search";
}
