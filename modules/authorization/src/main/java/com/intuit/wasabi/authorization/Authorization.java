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
package com.intuit.wasabi.authorization;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorizationobjects.Permission;
import com.intuit.wasabi.authorizationobjects.Role;
import com.intuit.wasabi.authorizationobjects.UserPermissions;
import com.intuit.wasabi.authorizationobjects.UserPermissionsList;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experimentobjects.Application;

import java.util.List;
import java.util.Map;

/**
 * Repository for authorization-related data access methods
 */
public interface Authorization {

    /**
     * Returns a list of permissions for the given role
     *
     * @param role Given role
     * @return List of Permission objects if found, empty list otherwise
     */
    List<Permission> getPermissionsFromRole(Role role);

    /**
     * Returns a list of user permissions for the given user name (ID)
     *
     * @param userID Username
     * @return list of user permissions if found, empty list otherwise
     */
    UserPermissionsList getUserPermissionsList(UserInfo.Username userID);

    /**
     * Returns a list of users and their roles for the given application name
     *
     * @param applicationName Application name
     * @return list of users and their roles if found, empty list otherwise
     */
    UserRoleList getApplicationUsers(Application.Name applicationName);

    /**
     * Returns permissions for given user within the application
     *
     * @param userID          User name (ID)
     * @param applicationName Application name
     * @return list of user permissions if found, empty list otherwise
     */
    UserPermissions getUserPermissions(UserInfo.Username userID, Application.Name applicationName);

    /**
     * Deletes a user role for the given user name (ID) within the given application name
     *
     * @param userID          User name (ID)
     * @param admin           the admin deleting the user role
     * @param applicationName Application name
     */
    void deleteUserRole(UserInfo.Username userID, Application.Name applicationName, UserInfo admin);

    /**
     * Verifies permission for the given user name (ID) within the application,
     * throws AuthenticationException if not a valid permission
     *
     * @param userID          User name (ID)
     * @param applicationName Application name
     * @param permission      Permission
     * @throws AuthenticationException if not a valid permission
     */
    void checkUserPermissions(UserInfo.Username userID, Application.Name applicationName, Permission permission);

    /**
     * Returns user name from the given auth header
     *
     * @param authHeader Authentication header
     * @return user name if found
     * @throws AuthenticationException if the user name could not be extracted
     */
    UserInfo.Username getUser(String authHeader);

    /**
     * Sets a role for the given user role object and returns status fields
     *
     * @param userRole User role
     * @param admin    the admin setting the user role
     * @return map of status fields
     */
    Map setUserRole(UserRole userRole, UserInfo admin);

    /**
     * Returns list of user roles for the given user name (ID)
     *
     * @param userID User name (ID)
     * @return list of user roles for the given user name (ID), empty list otherwise
     */
    UserRoleList getUserRoleList(UserInfo.Username userID);

    /**
     * Verifies super admin permissions for the given user name (ID)
     *
     * @param userID User name (ID)
     * @throws AuthenticationException if not a super admin
     */
    void checkSuperAdmin(UserInfo.Username userID);

    /**
     * Retrieves the full {@link UserInfo} for the given {@link com.intuit.wasabi.authenticationobjects.UserInfo.Username}
     *
     * @param userID the username that is used for the lookup
     * @return the complete {@link UserInfo}
     */
    UserInfo getUserInfo(UserInfo.Username userID);

    /**
     * Assign user to be a superadmin
     *
     * @param candidateUserInfo the superadmin candidate
     * @param assigningUserInfo  the assigning user
     */
    void assignUserToSuperAdminRole(UserInfo candidateUserInfo, UserInfo assigningUserInfo);

    /**
     * Remove user from superadmin role
     *
     * @param candidateUserInfo the candidate
     * @param assigningUserInfo  the assigning user
     */
    void removeUserFromSuperAdminRole(UserInfo candidateUserInfo, UserInfo assigningUserInfo);

    /**
     * Get all superadmin roles
     *
     * @return list of super admin roles
     */
    List<UserRole> getSuperAdminRoleList();

}
