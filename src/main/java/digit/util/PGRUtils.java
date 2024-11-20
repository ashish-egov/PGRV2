package digit.util;

import java.util.List;

import org.egov.common.contract.models.AuditDetails;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import digit.web.models.PGREntity;
import digit.web.models.Service;
import digit.web.models.ServiceResponse;

@Component
public class PGRUtils {

    @Autowired
    private ResponseInfoFactory responseInfoFactory;

    /**
     * Creates or updates the audit details for a given service based on the operation type.
     *
     * @param by       The user performing the operation.
     * @param service  The service object whose audit details need to be updated (used in updates).
     * @param isCreate A flag to indicate whether the operation is create (true) or update (false).
     * @return An AuditDetails object populated with the appropriate values.
     */
    public AuditDetails getAuditDetails(String by, Service service, Boolean isCreate) {
        Long time = System.currentTimeMillis(); // Capture the current system time.
        if (isCreate) {
            // For create operations, set both created and modified details to the same user and time.
            return AuditDetails.builder()
                    .createdBy(by)
                    .lastModifiedBy(by)
                    .createdTime(time)
                    .lastModifiedTime(time)
                    .build();
        } else {
            // For update operations, retain the createdBy and createdTime values,
            // but update the lastModifiedBy and lastModifiedTime values.
            return AuditDetails.builder()
                    .createdBy(service.getAuditDetails().getCreatedBy())
                    .lastModifiedBy(by)
                    .createdTime(service.getAuditDetails().getCreatedTime())
                    .lastModifiedTime(time)
                    .build();
        }
    }

    /**
     * Converts a list of PGREntity objects into a ServiceResponse object.
     *
     * @param requestInfo    The request information received in the API call.
     * @param serviceWrappers A list of PGREntity objects to be included in the response.
     * @return A ServiceResponse object containing the response info and the list of PGREntity objects.
     */
    public ServiceResponse convertToServiceResponse(RequestInfo requestInfo, List<PGREntity> serviceWrappers) {
        // Build the response using the response info factory and the list of entities.
        return ServiceResponse.builder()
                .responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true))
                .pgREntities(serviceWrappers)
                .build();
    }
}
