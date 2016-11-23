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
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.index.UserAssignmentByUserIdContextExperimentId;

import java.util.Date;
import java.util.UUID;

/**
 * Accessor for user assignment index table
 */
@Accessor
public interface UserAssignmentIndexAccessor {
	
	/**
	 * Insert record into table based on attributes
	 * @param uuid
	 * @param userId
	 * @param context
	 * @param created
	 * @param bucketLabel
	 * @return resultSet
	 */
    @Query("insert into user_assignment_by_userId_context_experimentId (experiment_id, user_id, context, created, bucket_label)" +
            " values (?, ?, ?, ?, ?)")
    ResultSet insertBy(UUID uuid, String userId, String context, Date created, String bucketLabel);

    /**
     * Insert record based on attributes
     * @param uuid
     * @param userId
     * @param context
     * @param created
     * @return resultSet
     */
    @Query("insert into user_assignment_by_userId_context_experimentId (experiment_id, user_id, context, created)" +
            " values (?, ?, ?, ?)")
    ResultSet insertBy(UUID uuid, String userId, String context, Date created);

	/**
	 * Get rows by parameters
	 * @param experimentId
	 * @param userId
	 * @param context
	 * @return result
	 */
    @Query("select * from user_assignment_by_userId_context_experimentId where experiment_id = ? and user_id = ? and context = ?")
    Result<UserAssignmentByUserIdContextExperimentId> selectBy(UUID experimentId, String userId, String context);


	/**
	 * delete rows by parameters
	 * @param userId
	 * @param context
	 * @param experimentId
	 * @return resultSet
	 */
	@Query("delete from user_assignment_by_userId_context_experimentId where user_id = ? and context = ? and experiment_id = ?")
	ResultSet deleteBy(String userId, String context, UUID experimentId);

}
