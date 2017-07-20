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

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.ApplicationList;

/**
 * Application list accessor
 */
@Accessor
public interface ApplicationListAccessor {

    /**
     * Get application names
     *
     * @return application names
     */
    @Query("select distinct app_name from applicationList")
    Result<ApplicationList> getUniqueAppName();

    /**
     * Delete application by name
     *
     * @param appName
     */
    @Query("delete from applicationList where app_name = ?")
    void deleteBy(String appName);

    /**
     * Insert application name into the table
     *
     * @param appName
     */
    @Query("insert into applicationList (app_name) values (?)")
    Statement insert(String appName);

}
