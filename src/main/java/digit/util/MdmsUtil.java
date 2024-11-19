package digit.util;

import digit.config.Configuration;
import digit.web.models.MdmsCriteriaReqV2;
import digit.web.models.MdmsCriteriaV2;
import digit.web.models.MdmsResponseV2;
import digit.web.models.ServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for interacting with MDMS (Master Data Management System)
 */
@Slf4j
@Component
public class MdmsUtil {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Configuration configs;

    /**
     * Fetches MDMS data for the given service request, tenant ID, and service code.
     *
     * @param requestBody The service request containing request information
     * @param tenantId    The tenant ID for which MDMS data is requested
     * @param serviceCode The unique service code for filtering MDMS data
     * @return MdmsResponseV2 containing the requested MDMS data
     */
    public MdmsResponseV2 fetchMdmsData(ServiceRequest requestBody, String tenantId, String serviceCode) {
        RequestInfo requestInfo = requestBody.getRequestInfo();

        // Construct the MDMS API endpoint URI
        StringBuilder uri = new StringBuilder();
        uri.append(configs.getMdmsHost()).append(configs.getMdmsEndPoint());

        // Prepare the MDMS request criteria
        MdmsCriteriaV2 mdmsCriteriav2 = getMdmsRequest(requestBody, tenantId, serviceCode);
        MdmsCriteriaReqV2 mdmsCriteriaReq = MdmsCriteriaReqV2.builder()
                .requestInfo(requestInfo)
                .mdmsCriteria(mdmsCriteriav2)
                .build();

        MdmsResponseV2 mdmsResponse;
        try {
            // Make a POST request to the MDMS API
            mdmsResponse = restTemplate.postForObject(uri.toString(), mdmsCriteriaReq, MdmsResponseV2.class);

            // Check if the response is null or empty
            if (mdmsResponse == null || ObjectUtils.isEmpty(mdmsResponse)) {
                throw new CustomException("MDMS_RESPONSE_EMPTY",
                        "Mdms response is empty or invalid for the given tenantId");
            }
        } catch (Exception e) {
            // Handle exceptions and throw a custom exception for MDMS request failures
            throw new CustomException("MDMS_REQUEST_FAILED",
                    "Failed to fetch Mdms data for the given tenantId due to an error");
        }
        return mdmsResponse;
    }

    /**
     * Creates MDMS request criteria based on the service request, tenant ID, and
     * unique identifier.
     *
     * @param requestBody      The service request containing request information
     * @param tenantId         The tenant ID for which MDMS data is requested
     * @param uniqueIdentifier The unique identifier for filtering MDMS data
     * @return MdmsCriteriaV2 containing the request criteria for MDMS
     */
    public MdmsCriteriaV2 getMdmsRequest(ServiceRequest requestBody, String tenantId, String uniqueIdentifier) {
        // Create a set of unique identifiers for MDMS filtering
        Set<String> uniqueIdentifiers = new HashSet<>();
        uniqueIdentifiers.add(uniqueIdentifier);

        // Build and return MDMS request criteria
        MdmsCriteriaV2 mdmsCriteriaV2 = MdmsCriteriaV2.builder()
                .tenantId(tenantId)
                .uniqueIdentifiers(uniqueIdentifiers)
                .build();
        return mdmsCriteriaV2;
    }
}
