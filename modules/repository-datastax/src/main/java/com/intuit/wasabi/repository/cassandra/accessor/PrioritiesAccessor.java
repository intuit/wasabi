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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.Application;

import java.util.List;
import java.util.UUID;

/**
 * Interface to priorities repo
 */
@Accessor
public interface PrioritiesAccessor {

    /**
     * Get the priority list for an application
     * 
     * @param applicationName  name of application
     * @return PrioritizedExperimentList
     */
    @Query("select * from application where appName = ?")
    Result<Application> getPriorities(String applicationName);

    /**
     * Update the priority list for an application
     *
     * @param applicationName           name of application
     * @param experimentPriorityList    list of experiments as their priorities
     */
    @Query("update application set priorities = ? where appName = ?")
    void updatePriorities(List<UUID> experimentPriorityList, String applicationName);

    /**
     * Delete the prioritized list for an application
     * @param applicationName
     */
    @Query("delete from application where appName = ?")
    void deletePriorities(String applicationName);
}
