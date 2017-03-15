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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.UserRole;

/**
 * Accessor interface
 */
@Accessor
public interface UserRoleAccessor {

    @Query("select * from user_roles")
    Result<UserRole> getAllUserRoles();

    @Query("select * from user_roles where user_id = ? and app_name = ?")
    Result<UserRole> getUserRolesBy(String userID, String appName);

    @Query("select * from user_roles where user_id = ? and app_name = '*'")
    Result<UserRole> getUserRolesByUserIdWithWildcardAppName(String userID);

    @Query("select * from user_roles where user_id = ?")
    Result<UserRole> getUserRolesByUserId(String userID);

    @Query("delete from user_roles where user_id = ? and app_name = ?")
    ResultSet deleteUserRoleBy(String userID, String appName);

    @Query("insert into user_roles (user_id, app_name, role) values (?, ?, ?)")
    ResultSet insertUserRoleBy(String userID, String appName, String role);

    //TODO: this is a hack for multitable batch statement
    @Query("insert into user_roles (user_id, app_name, role) values (?, ?, ?)")
    Statement insertUserRoleStatement(String userID, String appName, String role);

    @Query("delete from user_roles where user_id = ? and app_name = ?")
    Statement deleteUserRoleStatement(String userID, String appName);

}