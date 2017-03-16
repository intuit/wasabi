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
package com.intuit.wasabi.repository;


import java.util.List;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorizationobjects.UserPermissions;
import com.intuit.wasabi.authorizationobjects.UserPermissionsList;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.experimentobjects.Application;

/**
 * Repository for getting authorization information
 *
 * @see UserInfo
 * @see Application
 */
public interface AuthorizationRepository {

    /**
     * Get user permissions
     *
     * @param userID Username Object
     * @return UserPermissionList
     */
    UserPermissionsList getUserPermissionsList(UserInfo.Username userID);

    /**
     * Get application user
     *
     * @param applicationName Application.Name Object
     * @return user role list
     */
    UserRoleList getApplicationUsers(Application.Name applicationName);

    /**
     * Get user permissions
     *
     * @param userID          UserInfo.Username Object
     * @param applicationName Application.Name Object
     * @return user permissions
     */
    UserPermissions getUserPermissions(UserInfo.Username userID, Application.Name applicationName);

    /**
     * Delete user role
     *
     * @param userID          UserInfo.Username Object
     * @param applicationName Application.Name Object
     */
    void deleteUserRole(UserInfo.Username userID, Application.Name applicationName);

    /**
     * Set user role
     *
     * @param userRole UserRole Object
     */
    void setUserRole(UserRole userRole);

    /**
     * Get user role list
     *
     * @param userID UserInfo.Username Object
     * @return user role list
     */
    UserRoleList getUserRoleList(UserInfo.Username userID);

    /**
     * Get user info
     *
     * @param userID UserInfo.Username Object
     * @return user info
     */
    UserInfo getUserInfo(UserInfo.Username userID);

    /**
     * Set user info
     *
     * @param userInfo UserInfo Object
     */
    void setUserInfo(UserInfo userInfo);

    /**
     * Check super user permissions
     *
     * @param userID          UserInfo.Username Object
     * @param applicationName Application.Name Object
     * @return user permissions
     */
    UserPermissions checkSuperAdminPermissions(UserInfo.Username userID, Application.Name applicationName);

    /**
     * Assign candidate user to superadmin role
     *
     * @param candidateUser the candidate user
     */
    void assignUserToSuperAdminRole(UserInfo candidateUser);

    /**
     * Assign user to superadmin role
     *
     * @param candidateUser the candidate user
     */
    void removeUserFromSuperAdminRole(UserInfo candidateUser);

    /**
     * Get super admins roles list
     *
     * @return list of userroles
     */
    List<UserRole> getSuperAdminRoleList();

}
