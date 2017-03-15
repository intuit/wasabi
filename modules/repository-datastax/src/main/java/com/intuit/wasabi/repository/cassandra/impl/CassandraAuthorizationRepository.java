/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.UnavailableException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.inject.Inject;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorizationobjects.Role;
import com.intuit.wasabi.authorizationobjects.UserPermissions;
import com.intuit.wasabi.authorizationobjects.UserPermissionsList;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.repository.AuthorizationRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.AppRoleAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.UserInfoAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.UserRoleAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.AppRole;
import com.intuit.wasabi.repository.cassandra.pojo.ApplicationList;
import com.intuit.wasabi.userdirectory.UserDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.nonNull;

public class CassandraAuthorizationRepository implements AuthorizationRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraAuthorizationRepository.class);
    public static final String ALL_APPLICATIONS = "*";
    static final String SUPERADMIN = "superadmin";
    static final Application.Name WILDCARD = Application.Name.valueOf("wildcard");

    private final AppRoleAccessor appRoleAccessor;
    private final ApplicationListAccessor applicationListAccessor;
    private final UserRoleAccessor userRoleAccessor;
    private final UserInfoAccessor userInfoAccessor;
    private final UserDirectory userDirectory;
    private final MappingManager manager;

    @Inject
    public CassandraAuthorizationRepository(ApplicationListAccessor applicationListAccessor,
                                            AppRoleAccessor appRoleAccessor,
                                            UserRoleAccessor userRoleAccessor,
                                            UserInfoAccessor userInfoAccessor,
                                            UserDirectory userDirectory,
                                            MappingManager mappingManager) {
        this.applicationListAccessor = applicationListAccessor;
        this.appRoleAccessor = appRoleAccessor;
        this.userRoleAccessor = userRoleAccessor;
        this.userInfoAccessor = userInfoAccessor;
        this.userDirectory = userDirectory;
        this.manager = mappingManager;
    }

    UserInfo retrieveOrDefaultUser(UserInfo.Username userID) {
        UserInfo userInfo = lookupUser(userID);
        setUserInfo(userInfo);
        return userInfo;
    }

    UserInfo lookupUser(UserInfo.Username userID) {
        UserInfo userInfo;
        try {
            LOGGER.debug("Workforce-getApplicationUsers: looking up user {}", userID.toString());
            userInfo = userDirectory.lookupUser(userID);
        } catch (AuthenticationException e) {
            LOGGER.warn(String.format("Workforce-getApplicationUsers: problem looking up user %s", userID.toString()), e);
            userInfo = UserInfo.newInstance(userID)
                    .withEmail("")
                    .withFirstName("")
                    .withLastName("")
                    .build();
        }
        return userInfo;
    }

    @Override
    public UserPermissionsList getUserPermissionsList(UserInfo.Username userID) {
        UserPermissionsList userPermissionsList = new UserPermissionsList();
        Optional<UserPermissions> superAdminUserPermissions = getSuperAdminUserPermissions(userID, WILDCARD);
        if (superAdminUserPermissions.isPresent()) {
            List<String> allAppNames = getAllApplicationNameFromApplicationList();
            allAppNames.stream()
                    .map(t ->
                            UserPermissions.newInstance(
                                    Application.Name.valueOf(t),
                                    superAdminUserPermissions.get().getPermissions()
                            ).build())
                    .forEach(userPermissionsList::addPermissions);
        } else {
            List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> resultList = getUserRoleList(userID,
                    Optional.empty());
            resultList.stream()
                    .filter(t -> t.getRole() != null)
                    .map(t ->
                            UserPermissions.newInstance(
                                    Application.Name.valueOf(t.getAppName())
                                    , Role.valueOf(t.getRole()).getRolePermissions()).build()
                    )
                    .forEach(userPermissionsList::addPermissions);
        }
        return userPermissionsList;
    }

    @Override
    public UserRoleList getApplicationUsers(Application.Name applicationName) {
        UserRoleList userRoleList = new UserRoleList();
        List<AppRole> appRoleList = getAppRoleList(applicationName);
        appRoleList.stream()
                .map(t -> convertAppRoleToUserRole(applicationName, t))
                .forEach(userRoleList::addRole);
        return userRoleList;
    }

    UserRole convertAppRoleToUserRole(Application.Name applicationName, AppRole appRole) {
        Role role = Role.toRole(appRole.getRole());
        UserInfo.Username userID = UserInfo.Username.valueOf(appRole.getUserId());
        UserInfo userInfo = getUserInfo(userID);
        if (userInfo == null) {
            userInfo = lookupUser(userID);
        }
        return UserRole.newInstance(applicationName, role)
                .withUserID(userID)
                .withUserEmail(userInfo.getEmail())
                .withFirstName(userInfo.getFirstName())
                .withLastName(userInfo.getLastName())
                .build();
    }

    List<AppRole> getAppRoleList(Application.Name applicationName) {
        List<AppRole> resultList = Collections.EMPTY_LIST;
        try {
            Result<AppRole> result = appRoleAccessor.getAppRoleByAppName(applicationName.toString());
            resultList = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false)
                    .collect(Collectors.toList());
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve info for app \"" + applicationName + "\"", e);
        }
        return resultList;
    }

    @Override
    public UserPermissions getUserPermissions(@Nonnull UserInfo.Username username,
                                              @Nonnull Application.Name applicationName) {
        Optional<UserPermissions> userPermissions = getSuperAdminUserPermissions(username, applicationName);
        return userPermissions.orElseGet(() -> getAppSpecificPermission(username, applicationName));
    }

    Optional<UserPermissions> getSuperAdminUserPermissions(@Nonnull UserInfo.Username username,
                                                           @Nonnull Application.Name applicationName) {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> resultList = getUserRolesWithWildcardAppName(
                username,
                applicationName
        );

        return resultList.stream()
                .filter(t -> SUPERADMIN.equalsIgnoreCase(t.getRole()))
                .map(m ->
                        UserPermissions.newInstance(applicationName,
                                Role.SUPERADMIN.getRolePermissions())
                                .build()
                )
                .findAny();
    }

    UserPermissions getAppSpecificPermission(UserInfo.Username username, Application.Name applicationName) {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result =
                getUserRoleList(username, Optional.ofNullable(applicationName));
        if (result.size() != 0) {
            assert result.size() <= 1 : "More than a single row returned";
            com.intuit.wasabi.repository.cassandra.pojo.UserRole role = result.get(0);
            assert role.getRole() != null : "Role cannot be null";
            return UserPermissions.newInstance(applicationName, Role.toRole(role.getRole()).getRolePermissions())
                    .build();
        }
        return null;
    }

    @Override
    public void deleteUserRole(UserInfo.Username userID, Application.Name applicationName) {
        BatchStatement batch = new BatchStatement();
        batch.add(userRoleAccessor.deleteUserRoleStatement(
                userID.getUsername(),
                applicationName.toString()
        ));
        batch.add(appRoleAccessor.deleteAppRoleStatement(
                applicationName.toString(),
                userID.getUsername()
        ));
        manager.getSession().execute(batch);
    }

    @Override
    public void setUserRole(UserRole userRole) {
        BatchStatement batch = new BatchStatement();
        batch.add(userRoleAccessor.insertUserRoleStatement(
                userRole.getUserID().toString(),
                userRole.getApplicationName().toString(),
                userRole.getRole().toString()
        ));
        batch.add(appRoleAccessor.insertAppRoleStatement(
                userRole.getApplicationName().toString(),
                userRole.getUserID().toString(),
                userRole.getRole().toString()
        ));
        manager.getSession().execute(batch);
    }

    @Override
    public UserRoleList getUserRoleList(UserInfo.Username userID) {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> possibleSuperAdmin =
                getUserRolesWithWildcardAppName(userID, WILDCARD);
        UserInfo userInfo = retrieveOrDefaultUser(userID);
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> superAdmins = possibleSuperAdmin.stream()
                .filter(v -> SUPERADMIN.equalsIgnoreCase(v.getRole()))
                .collect(Collectors.toList());
        UserRoleList userRoleList = new UserRoleList();
        //If the userID is in the superadmin list
        if (superAdmins.size() > 0) {
            List<String> allAppNamesList = getAllApplicationNameFromApplicationList();
            superAdmins.stream()
                    .map(t ->
                            allAppNamesList.stream()
                                    .map(appName ->
                                            UserRole.newInstance(Application.Name.valueOf(appName), Role.SUPERADMIN)
                                                    .withUserID(userID)
                                                    .withUserEmail(userInfo.getEmail())
                                                    .withFirstName(userInfo.getFirstName())
                                                    .withLastName(userInfo.getLastName())
                                                    .build()
                                    ).collect(Collectors.toList())
                    )
                    .flatMap(Collection::stream)
                    .forEach(userRoleList::addRole);
            return userRoleList;
        }
        //else we need to check that user's specific permission
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> resultList = getUserRoleList(userID,
                Optional.empty());
        resultList.stream()
                .map(
                        r -> UserRole.newInstance(
                                Application.Name.valueOf(r.getAppName()),
                                Role.toRole(r.getRole())
                        )
                                .withFirstName(userInfo.getFirstName())
                                .withLastName(userInfo.getLastName())
                                .withUserEmail(userInfo.getEmail())
                                .withUserID(userID)
                                .build()
                ).forEach(userRoleList::addRole);

        return userRoleList;
    }

    List<String> getAllApplicationNameFromApplicationList() {
        Result<ApplicationList> allAppNames = applicationListAccessor.getUniqueAppName();
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(allAppNames.iterator(), Spliterator.ORDERED), false)
                .map(t -> t.getAppName())
                .collect(Collectors.toList());
    }

    @Override
    public UserInfo getUserInfo(UserInfo.Username userID) {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserInfo> resultList = getUserInfoList(userID);

        if (resultList.size() > 1) {
            throw new AuthenticationException("error, more than one user with the userID " + userID.toString());
        }

        return resultList.size() == 0 ? null :
                UserInfo.newInstance(userID)
                        .withEmail(resultList.get(0).getUserEmail())
                        .withFirstName(resultList.get(0).getFirstName())
                        .withLastName(resultList.get(0).getLastName())
                        .build();
    }

    List<com.intuit.wasabi.repository.cassandra.pojo.UserInfo> getUserInfoList(UserInfo.Username userID) {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserInfo> resultList = Collections.EMPTY_LIST;
        try {
            Result<com.intuit.wasabi.repository.cassandra.pojo.UserInfo> result =
                    userInfoAccessor.getUserInfoBy(userID.getUsername());
            resultList = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false)
                    .collect(Collectors.toList());
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve info for user \"" + userID + "\"", e);
        }
        return resultList;
    }


    @Override
    public void setUserInfo(UserInfo userInfo) {
        try {
            //TODO: why is this username but in our db we called it userid which is another field
            userInfoAccessor.insertUserInfoBy(userInfo.getUsername().getUsername(),
                    userInfo.getEmail(),
                    userInfo.getFirstName(),
                    userInfo.getLastName());
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not set info for user \"" + userInfo.getUsername() + "\"", e);
        }
    }

    @Override
    public UserPermissions checkSuperAdminPermissions(UserInfo.Username userID, Application.Name applicationName) {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> resultList = getUserRolesWithWildcardAppName(userID, applicationName);
        Optional<com.intuit.wasabi.repository.cassandra.pojo.UserRole> adminRole = resultList
                .stream()
                .filter(t -> SUPERADMIN.equalsIgnoreCase(t.getRole()))
                .findAny();

        if (!adminRole.isPresent())
            return null;
        else
            return UserPermissions.newInstance(applicationName, Role.SUPERADMIN.getRolePermissions())
                    .build();
    }
    //UserRole related operations

    List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> getUserRoleList(UserInfo.Username userID,
                                                                               Optional<Application.Name> applicationName) {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> resultList = Collections.EMPTY_LIST;
        try {
            Result<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result;
            if (applicationName.isPresent()) {
                result = userRoleAccessor.getUserRolesBy(userID.getUsername(), applicationName.get().toString());
            } else {
                result = userRoleAccessor.getUserRolesByUserId(userID.getUsername());
            }
            resultList = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false)
                    .collect(Collectors.toList());
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve info for user \"" + userID + "\"", e);
        }
        return resultList;
    }

    List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> getUserRolesWithWildcardAppName(
            UserInfo.Username userID,
            Application.Name applicationName
    ) {
        //intialize to safe guard against null points
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> resultList = Collections.emptyList();
        try {
            Result<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result =
                    userRoleAccessor.getUserRolesByUserIdWithWildcardAppName(userID.getUsername());
            resultList = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false)
                    .collect(Collectors.toList());
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve permissions for user \"" + userID + "\" and application "
                    + "\"" + applicationName + "\"", e);
        }
        return resultList;
    }

    @Override
    public void assignUserToSuperAdminRole(UserInfo candidateUser) {

        LOGGER.debug("Adding user {} as superadmin", candidateUser);

        String superAdminRole = Role.SUPERADMIN.toString().toLowerCase();
        String userID = candidateUser.getUsername().toString();

        userRoleAccessor.insertUserRoleBy(userID, ALL_APPLICATIONS,
                superAdminRole);
        appRoleAccessor.insertAppRoleBy(ALL_APPLICATIONS, userID,
                superAdminRole);
    }

    @Override
    public void removeUserFromSuperAdminRole(UserInfo candidateUser) {
        LOGGER.debug("Removing user {} from user admin role", candidateUser);

        String userID = candidateUser.getUsername().toString();
        userRoleAccessor.deleteUserRoleBy(userID, ALL_APPLICATIONS);
        appRoleAccessor.deleteAppRoleBy(ALL_APPLICATIONS, userID);
    }

    @Override
    public List<UserRole> getSuperAdminRoleList() {

        LOGGER.debug("Getting super admin role list");
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> allUserRoles =
                userRoleAccessor.getAllUserRoles().all();

        LOGGER.debug("Received all roles {}", allUserRoles);

        List<UserRole> superAdmins = allUserRoles.stream().filter(
                userRole -> Role.SUPERADMIN.toString().equalsIgnoreCase(
                        userRole.getRole().toString()) && ALL_APPLICATIONS.equals(userRole.getAppName())).map(
                userRole -> getRoleWithUserInfo(userRole)).collect(Collectors.toList());

        LOGGER.debug("Returning {} roles", superAdmins);

        return superAdmins;
    }

    private UserRole getRoleWithUserInfo(com.intuit.wasabi.repository.cassandra.pojo.UserRole userRole) {

        LOGGER.debug("Getting user info for user role={}", userRole);

        Application.Name appName = userRole.getAppName().equals(ALL_APPLICATIONS) ? WILDCARD :
                Application.Name.valueOf(userRole.getAppName());

        UserInfo userInfo = getUserInfo(UserInfo.Username.valueOf(userRole.getUserId()));

        UserRole roleWithUserInfo;

        if (nonNull(userInfo)) {
            roleWithUserInfo = UserRole.newInstance(
                    appName,
                    Role.toRole(userRole.getRole())).
                    withUserID(UserInfo.Username.valueOf(userRole.getUserId())).
                    withFirstName(userInfo.getFirstName()).
                    withLastName(userInfo.getLastName()).
                    withUserEmail(userInfo.getEmail()).build();
        } else {
            roleWithUserInfo = UserRole.newInstance(appName, Role.toRole(userRole.getRole()))
                    .withUserID(UserInfo.Username.valueOf(userRole.getUserId())).build();
        }

        LOGGER.debug("Role with user info for user role={} is {}", userRole, roleWithUserInfo);

        return roleWithUserInfo;
    }

}