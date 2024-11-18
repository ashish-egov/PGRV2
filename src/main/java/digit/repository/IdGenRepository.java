package digit.repository;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import digit.config.Configuration;
import digit.web.models.IdGenerationRequest;
import digit.web.models.IdGenerationResponse;
import digit.web.models.IdRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class IdGenRepository {

    private RestTemplate restTemplate;

    private Configuration config;

    @Autowired
    public IdGenRepository(Configuration config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    /**
     * Call iDgen to generateIds
     * 
     * @param requestInfo The rquestInfo of the request
     * @param tenantId    The tenantiD of the service request
     * @param name        Name of the foramt
     * @param format      Format of the ids
     * @param count       Total Number of idGen ids required
     * @return
     */
    public IdGenerationResponse getId(RequestInfo requestInfo, String tenantId, String name, String format, int count) {

        List<IdRequest> reqList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            reqList.add(IdRequest.builder().idName(name).format(format).tenantId(tenantId).build());
        }
        IdGenerationRequest req = IdGenerationRequest.builder().idRequests(reqList).requestInfo(requestInfo).build();
        IdGenerationResponse response = restTemplate.postForObject(
                config.getIdGenHost() + config.getIdGenPath(), req,
                IdGenerationResponse.class);
        return response;
    }

}
