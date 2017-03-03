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
import com.intuit.wasabi.repository.cassandra.pojo.AppRole;

/**
 * Accessor interface
 */
@Accessor
public interface AppRoleAccessor {

    @Query("select * from app_roles where app_name = ?")
    Result<AppRole> getAppRoleByAppName(String appName);

    @Query("delete from app_roles where app_name = ? and user_id = ?")
    ResultSet deleteAppRoleBy(String appName, String userId);

    @Query("insert into app_roles (app_name, user_id, role) values (?, ?, ?)")
    ResultSet insertAppRoleBy(String appName, String userId, String role);

    //TODO: this is a hack for multitable batch statement
    @Query("insert into app_roles (app_name, user_id, role) values (?, ?, ?)")
    Statement insertAppRoleStatement(String appName, String userId, String role);

    @Query("delete from app_roles where app_name = ? and user_id = ?")
    Statement deleteAppRoleStatement(String appName, String userId);
}