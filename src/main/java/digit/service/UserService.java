package digit.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.contract.user.CreateUserRequest;
import org.egov.common.contract.user.UserDetailResponse;
import org.egov.common.contract.user.UserSearchRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import digit.config.Configuration;
import digit.config.PGRConstants;
import digit.util.UserUtil;
import digit.web.models.PGREntity;
import digit.web.models.RequestSearchCriteria;
import digit.web.models.ServiceRequest;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class UserService {

    @Autowired
    private UserUtil userUtils;

    @Autowired
    private Configuration config;

    @Autowired
    private PGRConstants pgrConstants;

    /**
     * Calls the appropriate user service method based on the presence of account ID
     * or citizen details in the service.
     *
     * @param request The service request containing PGR entity and service details,
     *                used to determine which user service method to call.
     */
    public void callUserService(ServiceRequest request) {
        // If the service has an accountId, enrich the user details using enrichUser
        if (!StringUtils.isEmpty(request.getPgrEntity().getService().getAccountId())) {
            enrichUser(request); // Enrich user details based on accountId
        }
        // If the service does not have accountId but contains citizen details, update
        // or insert user using upsertUser
        else if (request.getPgrEntity().getService().getCitizen() != null) {
            upsertUser(request); // Update or insert user details based on citizen information
        }
    }

    /**
     * Enriches the services in the list with user details based on the account ID.
     *
     * @param pgrEntities A list of PGREntities whose services need to be enriched
     *                    with user details.
     */
    public void enrichUsers(List<PGREntity> pgrEntities) {
        // Collect all unique account IDs from the services
        Set<String> uuids = new HashSet<>();
        pgrEntities.forEach(pgrEntity -> {
            uuids.add(pgrEntity.getService().getAccountId());
        });

        // Fetch user details in bulk based on the collected account IDs
        Map<String, User> idToUserMap = searchBulkUser(new LinkedList<>(uuids));

        // Enrich each service with the corresponding user details
        pgrEntities.forEach(serviceWrapper -> {
            serviceWrapper.getService().setCitizen(idToUserMap.get(serviceWrapper.getService().getAccountId()));
        });
    }

    private void upsertUser(ServiceRequest request) {

        User user = request.getPgrEntity().getService().getCitizen();
        String tenantId = request.getPgrEntity().getService().getTenantId();
        User userServiceResponse = null;

        // Search on mobile number as user name
        UserDetailResponse userDetailResponse = searchUser(userUtils.getStateLevelTenant(tenantId), null,
                user.getMobileNumber());
        if (!userDetailResponse.getUser().isEmpty()) {
            User userFromSearch = userDetailResponse.getUser().get(0);
            if (!user.getName().equalsIgnoreCase(userFromSearch.getName())) {
                userServiceResponse = updateUser(request.getRequestInfo(), user, userFromSearch);
            } else
                userServiceResponse = userDetailResponse.getUser().get(0);
        } else {
            userServiceResponse = createUser(request.getRequestInfo(), tenantId, user);
        }

        // Enrich the accountId
        request.getPgrEntity().getService().setAccountId(userServiceResponse.getUuid());
    }

    private void enrichUser(ServiceRequest request) {

        String accountId = request.getPgrEntity().getService().getAccountId();
        String tenantId = request.getPgrEntity().getService().getTenantId();

        UserDetailResponse userDetailResponse = searchUser(userUtils.getStateLevelTenant(tenantId), accountId, null);

        if (userDetailResponse.getUser().isEmpty())
            throw new CustomException("INVALID_ACCOUNTID", "No user exist for the given accountId");

        else
            request.getPgrEntity().getService().setCitizen(userDetailResponse.getUser().get(0));

    }

    private User createUser(RequestInfo requestInfo, String tenantId, User userInfo) {
        userUtils.addUserDefaultFields(userInfo.getMobileNumber(), tenantId, userInfo, pgrConstants.USERTYPE_CITIZEN);
        StringBuilder uri = new StringBuilder(config.getUserHost())
                .append(config.getUserContextPath())
                .append(config.getUserCreateEndpoint());

        UserDetailResponse userDetailResponse = userUtils.userCall(new CreateUserRequest(requestInfo, userInfo), uri);
        return userDetailResponse.getUser().get(0);
    }

    /**
     * Updates the given user by calling user service
     * 
     * @param requestInfo
     * @param user
     * @param userFromSearch
     * @return
     */
    private User updateUser(RequestInfo requestInfo, User user, User userFromSearch) {

        userFromSearch.setName(user.getName());
        // userFromSearch.setActive(true);

        StringBuilder uri = new StringBuilder(config.getUserHost())
                .append(config.getUserContextPath())
                .append(config.getUserUpdateEndpoint());

        UserDetailResponse userDetailResponse = userUtils.userCall(new CreateUserRequest(requestInfo, userFromSearch),
                uri);

        return userDetailResponse.getUser().get(0);

    }

    /**
     * calls the user search API based on the given accountId and userName
     * 
     * @param stateLevelTenant
     * @param accountId
     * @param userName
     * @return
     */
    private UserDetailResponse searchUser(String stateLevelTenant, String accountId, String userName) {

        UserSearchRequest userSearchRequest = new UserSearchRequest();
        userSearchRequest.setActive(true);
        userSearchRequest.setUserType(pgrConstants.USERTYPE_CITIZEN);
        userSearchRequest.setTenantId(stateLevelTenant);

        if (StringUtils.isEmpty(accountId) && StringUtils.isEmpty(userName))
            return null;

        if (!StringUtils.isEmpty(accountId))
            userSearchRequest.setUuid(Collections.singletonList(accountId));

        if (!StringUtils.isEmpty(userName))
            userSearchRequest.setUserName(userName);

        StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
        return userUtils.userCall(userSearchRequest, uri);

    }

    /**
     * calls the user search API based on the given list of user uuids
     * 
     * @param uuids
     * @return
     */
    private Map<String, User> searchBulkUser(List<String> uuids) {

        UserSearchRequest userSearchRequest = new UserSearchRequest();
        userSearchRequest.setActive(true);
        userSearchRequest.setUserType(pgrConstants.USERTYPE_CITIZEN);

        if (!CollectionUtils.isEmpty(uuids))
            userSearchRequest.setUuid(uuids);

        StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
        UserDetailResponse userDetailResponse = userUtils.userCall(userSearchRequest, uri);
        List<User> users = userDetailResponse.getUser();

        if (CollectionUtils.isEmpty(users))
            throw new CustomException("USER_NOT_FOUND", "No user found for the uuids");

        Map<String, User> idToUserMap = users.stream().collect(Collectors.toMap(User::getUuid, Function.identity()));

        return idToUserMap;
    }

    /**
     * Enriches the list of userUuids associated with the mobileNumber in the search
     * criteria
     * 
     * @param tenantId
     * @param criteria
     */
    public void enrichUserIds(String tenantId, RequestSearchCriteria criteria) {

        String mobileNumber = criteria.getMobileNumber();

        UserSearchRequest userSearchRequest = new UserSearchRequest();
        userSearchRequest.setActive(true);
        userSearchRequest.setUserType(pgrConstants.USERTYPE_CITIZEN);
        userSearchRequest.setTenantId(tenantId);
        userSearchRequest.setMobileNumber(mobileNumber);

        StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
        UserDetailResponse userDetailResponse = userUtils.userCall(userSearchRequest, uri);
        List<User> users = userDetailResponse.getUser();

        Set<String> userIds = users.stream().map(User::getUuid).collect(Collectors.toSet());
        criteria.setUserIds(userIds);
    }

}
