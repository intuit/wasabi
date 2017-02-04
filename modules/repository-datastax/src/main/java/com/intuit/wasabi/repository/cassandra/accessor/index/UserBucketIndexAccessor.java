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
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

import java.util.Date;
import java.util.UUID;

/**
 * User bucket index accessor 
 */
@Accessor
public interface UserBucketIndexAccessor {

	/**
	 * Get count by params
	 * @param experimentId
	 * @param context
	 * @param bucketLable
	 * @return result set
	 */
    @Query("select count(*) from user_bucket_index where experiment_id = ? and context = ? and "
    		+ "bucket_label = ? ")
    ResultSet countUserBy(UUID experimentId, String context, String bucketLable);

    /**
     * Insert entry into the table
     * @param experimentId
     * @param userId
     * @param context
     * @param assigned
     * @param bucketLabel
     */
    @Query("insert into user_bucket_index (experiment_id, user_id, context, assigned, bucket_label) values (?, ?, ?, ?, ?)")
    void insertBy(UUID experimentId, String userId, String context, Date assigned, String bucketLabel);


    /**
     * Insert entry into the table
     * @param experimentId
     * @param userId
     * @param context
     * @param assigned
     */
    @Query("insert into user_bucket_index (experiment_id, user_id, context, assigned) values (?, ?, ?, ?)")
    void insertBy(UUID experimentId, String userId, String context, Date assigned);

    /**
     * Delete entry from table using params
     * @param experimentId
     * @param userId
     * @param context
     * @param bucketLabel
     */
    @Query("delete from user_bucket_index where experiment_id = ? and user_id = ? and context = ? and bucket_label = ?")
    void deleteBy(UUID experimentId, String userId, String context, String bucketLabel);


    /**
     * Insert entry into the table
     * @param experimentId
     * @param userId
     * @param context
     * @param assigned
     */
    @Query("insert into user_bucket_index (experiment_id, user_id, context, assigned) values (?, ?, ?, ?)")
    ResultSetFuture asyncInsertBy(UUID experimentId, String userId, String context, Date assigned);


    /**
     * Insert entry into the table
     * @param experimentId
     * @param userId
     * @param context
     * @param assigned
     * @param bucketLabel
     */
    @Query("insert into user_bucket_index (experiment_id, user_id, context, assigned, bucket_label) values (?, ?, ?, ?, ?)")
    ResultSetFuture asyncInsertBy(UUID experimentId, String userId, String context, Date assigned, String bucketLabel);


}
