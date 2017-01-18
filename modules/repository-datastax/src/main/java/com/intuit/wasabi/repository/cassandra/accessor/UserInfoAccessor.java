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
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.UserInfo;

/**
 * Accessor interface
 */
@Accessor
public interface UserInfoAccessor {

    @Query("insert into user_info (user_id, user_email, firstname, lastname) values (?, ?, ?, ?)")
    ResultSet insertUserInfoBy(String userId, String userEmail, String firstName, String lastName);

    @Query("select * from user_info where user_id = ?")
    Result<UserInfo> getUserInfoBy(String userId);

    @Query("delete from user_info where user_id = ?")
    ResultSet deleteBy(String userId);
}
