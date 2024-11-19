package digit.repository;

import lombok.extern.slf4j.Slf4j;

import org.egov.common.contract.models.Workflow;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import digit.config.PGRConstants;
import digit.repository.queryBuilder.PGRQueryBuilder;
import digit.repository.rowMapper.PGRRowMapper;
import digit.service.UserService;
import digit.service.WorkflowService;
import digit.util.PGRUtils;
import digit.web.models.PGREntity;
import digit.web.models.RequestSearchCriteria;
import digit.web.models.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class PGRRepository {

    @Autowired
    private PGRQueryBuilder queryBuilder;

    @Autowired
    private PGRRowMapper rowMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PGRUtils pgrUtils;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private UserService userService;

    /**
     * searches services based on search criteria and then wraps it into
     * serviceWrappers
     * 
     * @param criteria
     * @return
     */
    public List<PGREntity> getServiceWrappers(RequestSearchCriteria criteria) {
        List<Service> services = getServices(criteria);
        Map<String, Workflow> idToWorkflowMap = new HashMap<>();
        List<PGREntity> serviceWrappers = new ArrayList<>();

        for (Service service : services) {
            PGREntity serviceWrapper = PGREntity.builder().service(service)
                    .workflow(idToWorkflowMap.get(service.getServiceRequestId())).build();
            serviceWrappers.add(serviceWrapper);
        }
        return serviceWrappers;
    }

    public List<PGREntity> getSortedServiceWrappers(RequestInfo requestInfo, RequestSearchCriteria criteria) {
        // If criteria are empty or it is a mobile number search with no user IDs,
        // return an empty response
        if (criteria.isEmpty())
            return new ArrayList<>();

        // Mark the search as non-plain
        criteria.setIsPlainSearch(false);

        // Fetch service wrappers from the repository
        List<PGREntity> serviceWrappers = getServiceWrappers(criteria);

        // If no service wrappers are found, return an empty response
        if (CollectionUtils.isEmpty(serviceWrappers)) {
            return new ArrayList<>();
        }

        // Enrich the service wrappers with user and workflow data
        userService.enrichUsers(serviceWrappers);
        List<PGREntity> enrichedServiceWrappers = workflowService.enrichWorkflow(requestInfo, serviceWrappers);

        // Sort the enriched service wrappers by created time in descending order
        Map<Long, List<PGREntity>> sortedWrappers = new TreeMap<>(Collections.reverseOrder());
        for (PGREntity pgrEntity : enrichedServiceWrappers) {
            Long createdTime = pgrEntity.getService().getAuditDetails().getCreatedTime();
            sortedWrappers.computeIfAbsent(createdTime, k -> new ArrayList<>()).add(pgrEntity);
        }

        // Flatten the sorted wrappers into a single list
        List<PGREntity> sortedServiceWrappers = new ArrayList<>();
        for (List<PGREntity> wrappers : sortedWrappers.values()) {
            sortedServiceWrappers.addAll(wrappers);
        }

        return sortedServiceWrappers;
    }

    /**
     * searches services based on search criteria
     * 
     * @param criteria
     * @return
     */
    public List<Service> getServices(RequestSearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getPGRSearchQuery(criteria, preparedStmtList);
        List<Service> services = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
        return services;
    }

    /**
     * Returns the count based on the search criteria
     * 
     * @param criteria
     * @return
     */
    public Integer getCount(RequestSearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getCountQuery(criteria, preparedStmtList);
        Integer count = jdbcTemplate.queryForObject(query, preparedStmtList.toArray(), Integer.class);
        return count;
    }

    public Map<String, Integer> fetchDynamicData(String tenantId) {
        List<Object> preparedStmtListCompalintsResolved = new ArrayList<>();
        String query = queryBuilder.getResolvedComplaints(tenantId, preparedStmtListCompalintsResolved);

        int complaintsResolved = jdbcTemplate.queryForObject(query, preparedStmtListCompalintsResolved.toArray(),
                Integer.class);

        List<Object> preparedStmtListAverageResolutionTime = new ArrayList<>();
        query = queryBuilder.getAverageResolutionTime(tenantId, preparedStmtListAverageResolutionTime);

        int averageResolutionTime = jdbcTemplate.queryForObject(query, preparedStmtListAverageResolutionTime.toArray(),
                Integer.class);

        Map<String, Integer> dynamicData = new HashMap<String, Integer>();
        dynamicData.put(PGRConstants.COMPLAINTS_RESOLVED, complaintsResolved);
        dynamicData.put(PGRConstants.AVERAGE_RESOLUTION_TIME, averageResolutionTime);

        return dynamicData;
    }

}
