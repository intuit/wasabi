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
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.repository.cassandra.pojo.Application;

import java.util.List;
import java.util.UUID;

/**
 * Interface to priorities (application) table
 */
@Accessor
public interface PrioritiesAccessor {

    /**
     * Get the priority list for an application
     *
     * @param applicationName name of application
     * @return application instance
     */
    @Query("select * from application where app_name = ?")
    Result<Application> getPriorities(String applicationName);

    @Query("select * from application where app_name = ?")
    ListenableFuture<Result<Application>> asyncGetPriorities(String applicationName);

    /**
     * Update the priority list for an application
     *
     * @param experimentPriorityList list of experiments as their priorities
     * @param applicationName        name of application
     */
    @Query("update application set priorities = ? where app_name = ?")
    void updatePriorities(List<UUID> experimentPriorityList, String applicationName);

    /**
     * Append experiments to the priorities
     *
     * @param experimentIds experiment ids to be appended to the existing priorities (experiment ids)
     * @param applicationName name of application
     */
    @Query("update application set priorities = priorities + ? where app_name = ?")
    Statement appendToPriorities(List<UUID> experimentIds, String applicationName);

    /**
     * Delete the prioritized list for an application
     *
     * @param applicationName the app name
     */
    @Query("delete from application where app_name = ?")
    void deletePriorities(String applicationName);
}
