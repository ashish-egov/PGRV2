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

    public AuditDetails getAuditDetails(String by, Service service, Boolean isCreate) {
        Long time = System.currentTimeMillis();
        if (isCreate)
            return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time)
                    .build();
        else
            return AuditDetails.builder().createdBy(service.getAuditDetails().getCreatedBy()).lastModifiedBy(by)
                    .createdTime(service.getAuditDetails().getCreatedTime()).lastModifiedTime(time).build();
    }

    public ServiceResponse convertToServiceResponse(RequestInfo requestInfo, List<PGREntity> serviceWrappers) {
        return ServiceResponse.builder()
                .responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true))
                .pgREntities(serviceWrappers)
                .build();
    }
}
