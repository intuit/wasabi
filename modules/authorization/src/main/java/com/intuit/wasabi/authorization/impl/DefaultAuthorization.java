/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.authorization.impl;

import com.google.common.base.Optional;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.exceptions.AuthenticationException;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.Permission;
import com.intuit.wasabi.authorizationobjects.Role;
import com.intuit.wasabi.authorizationobjects.UserPermissions;
import com.intuit.wasabi.authorizationobjects.UserPermissionsList;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.AuthorizationChangeEvent;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AuthorizationRepository;
import com.intuit.wasabi.repository.RepositoryException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.intuit.wasabi.authorizationobjects.Permission.SUPERADMIN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The default authorization implementation for noop authentication
 */
public class DefaultAuthorization implements Authorization {

    private static final List<Permission> SUPERADMIN_PERMISSIONS = new ArrayList<>();
    private static final String SPACE = " ";
    private static final CharSequence BASIC = "Basic";
    private static final String COLON = ":";
    private static final Logger LOGGER = getLogger(DefaultAuthorization.class);

    static {
        SUPERADMIN_PERMISSIONS.add(SUPERADMIN);
    }

    private final AuthorizationRepository authorizationRepository;
    private final Experiments experiments;
    private final EventLog eventLog;

    @Inject
    public DefaultAuthorization(final AuthorizationRepository authorizationRepository, final Experiments experiments,
                                final EventLog eventLog) {
        super();

        this.authorizationRepository = authorizationRepository;
        this.experiments = experiments;
        this.eventLog = eventLog;
    }

    @Override
    public List<Permission> getPermissionsFromRole(Role role) {
        return role.getRolePermissions();
    }

    @Override
    public UserPermissionsList getUserPermissionsList(UserInfo.Username userID) {
        return authorizationRepository.getUserPermissionsList(userID);
    }

    @Override
    public UserRoleList getApplicationUsers(Application.Name applicationName) {
        return authorizationRepository.getApplicationUsers(applicationName);
    }

    @Override
    public UserPermissions getUserPermissions(UserInfo.Username userID, Application.Name applicationName) {
        return authorizationRepository.getUserPermissions(userID, applicationName);
    }

    @Override
    public void deleteUserRole(UserInfo.Username userID, Application.Name applicationName, UserInfo admin) {
        authorizationRepository.deleteUserRole(userID, applicationName);
        UserInfo user = getUserInfo(userID);
        eventLog.postEvent(new AuthorizationChangeEvent(admin, applicationName, user, "", ""));
    }

    @Override
    public void checkUserPermissions(UserInfo.Username userID, Application.Name applicationName, Permission permission) {
        //get the user's permissions for this applicationName
        UserPermissions userPermissions = getUserPermissions(userID, applicationName);
        //check that the user is permitted to perform the action
        if (Objects.isNull(userPermissions) || !userPermissions.getPermissions().contains(permission)) {
            throw new AuthenticationException("error, user " + userID + " not authorized to " + permission
                    .toString() + " on application " + applicationName.toString());
        }
    }

    //TODO: move this to authentication instead of authorization
    @Override
    public UserInfo.Username getUser(String authHeader) {
        return parseUsername(Optional.fromNullable(authHeader));
    }

    @Override
    public Map setUserRole(UserRole userRole, UserInfo admin) {
        Map<String, String> status = new HashMap<>();
        status.put("applicationName", userRole.getApplicationName().toString());
        status.put("userID", userRole.getUserID().toString());
        status.put("role", userRole.getRole().toString());

        // check that the application exists
        List<Experiment> experimentList = experiments.getExperiments(userRole.getApplicationName());

        // get current permissions

        if (!experimentList.isEmpty()) {
            List<UserRole> userRoleList = authorizationRepository.getUserRoleList(userRole.getUserID()).getRoleList();
            Role oldRole = null;
            for (UserRole role : userRoleList) {
                if (role.getApplicationName().equals(userRole.getApplicationName())) {
                    oldRole = role.getRole();
                    break;
                }
            }
            try {
                // set new permissions
                authorizationRepository.setUserRole(userRole);
                status.put("roleAssignmentStatus", "SUCCESS");

                // prepare event for log
                UserInfo user = getUserInfo(userRole.getUserID());
                eventLog.postEvent(new AuthorizationChangeEvent(admin, userRole.getApplicationName(), user,
                        Objects.isNull(oldRole) || "superadmin".equalsIgnoreCase(oldRole.toString()) ? null : oldRole.toString(),
                        userRole.getRole().toString()));
            } catch (RepositoryException e) {
                LOGGER.info("RepoitoryException for setting user Role in DefaultAuthorization ", e);
                status.put("roleAssignmentStatus", "FAILED");
                status.put("reason", "RepositoryException");
            }
        } else {
            status.put("roleAssignmentStatus", "FAILED");
            status.put("reason", "No application named " + userRole.getApplicationName());
        }
        return status;
    }

    @Override
    public UserRoleList getUserRoleList(UserInfo.Username userID) {
        return authorizationRepository.getUserRoleList(userID);
    }

    @Override
    public void checkSuperAdmin(UserInfo.Username userID) {
        if (Objects.isNull(authorizationRepository.checkSuperAdminPermissions(userID, null))) {
            throw new AuthenticationException("error, user " + userID + " is not a superadmin");
        }
    }

    @Override
    public UserInfo getUserInfo(UserInfo.Username userID) {
        UserInfo result;
        if (Objects.nonNull(userID) && !StringUtils.isBlank(userID.toString())) {
            result = authorizationRepository.getUserInfo(userID);
        } else {
            throw new AuthenticationException("The user name was null or empty for retrieving the UserInfo.");
        }
        return result;
    }

    private UserInfo.Username parseUsername(Optional<String> authHeader) {
        if (!authHeader.isPresent()) {
            throw new AuthenticationException("Null Authentication Header is not supported");
        }

        if (!authHeader.or(SPACE).contains(BASIC)) {
            throw new AuthenticationException("Only Basic Authentication is supported");
        }

        final String encodedUserPassword = authHeader.get().substring(authHeader.get().lastIndexOf(SPACE));
        LOGGER.trace("Base64 decoded username and password is: {}", encodedUserPassword);
        String usernameAndPassword;
        try {
            usernameAndPassword = new String(Base64.decodeBase64(encodedUserPassword.getBytes()));
        } catch (Exception e) {
            throw new AuthenticationException("error parsing username and password", e);
        }

        //Split username and password tokens
        String[] fields = usernameAndPassword.split(COLON);

        if (fields.length > 2) {
            throw new AuthenticationException("More than one username and password provided, or one contains ':'");
        } else if (fields.length < 2) {
            throw new AuthenticationException("Username or password are empty.");
        }

        if (StringUtils.isBlank(fields[0]) || StringUtils.isBlank(fields[1])) {
            throw new AuthenticationException("Username or password are empty.");
        }

        return UserInfo.Username.valueOf(fields[0]);
    }
}
