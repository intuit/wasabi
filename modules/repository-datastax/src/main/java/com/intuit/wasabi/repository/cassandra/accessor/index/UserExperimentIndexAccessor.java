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
package com.intuit.wasabi.repository.cassandra.accessor.index;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

import java.util.UUID;

/**
 * Accessor for user experiment index table
 */
@Accessor
public interface UserExperimentIndexAccessor {
	
	/**
	 * Insert row into table
	 * @param userId
	 * @param context
	 * @param appName
	 * @param experimentId
	 * @param bucketLabel
	 * @return result set
	 */
    @Query("insert into user_experiment_index (user_id, context, app_name, experiment_id, bucket)" +
            " values (?, ?, ?, ?, ?)")
    ResultSet insertBy(String userId, String context, String appName, UUID experimentId, String bucketLabel);

    /**
     * Insert row into table based on attributes
     * @param userId
     * @param context
     * @param appName
     * @param experimentId
     * @return result set
     */
    @Query("insert into user_experiment_index (user_id, context, app_name, experiment_id)" +
            " values (?, ?, ?, ?)")
    ResultSet insertBy(String userId, String context, String appName, UUID experimentId);

    
    /**
     * Delete row from table
     * @param userId
     * @param experimentId
     * @param context
     * @param appName
     * @return result set
     */
    @Query("delete from user_experiment_index where user_id = ? and experiment_id = ? and context = ? and app_name = ?")
    ResultSet deleteBy(String userId, UUID experimentId, String context, String appName);

}
