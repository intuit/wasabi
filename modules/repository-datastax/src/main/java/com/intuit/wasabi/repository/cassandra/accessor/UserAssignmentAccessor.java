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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.UserAssignment;

import java.util.Date;
import java.util.UUID;

/**
 * Accessor interface
 */
@Accessor
public interface UserAssignmentAccessor {
    @Query("insert into user_assignment (experiment_id, user_id, context, created, bucket_label)" +
            " values (?, ?, ?, ?, ?)")
    ResultSet insertBy(UUID uuid, String userId, String context, Date created, String bucketLabel);

    @Query("insert into user_assignment (experiment_id, user_id, context, created)" +
            " values (?, ?, ?, ?)")
    ResultSet insertBy(UUID uuid, String userId, String context, Date created);

    @Query("select * from user_assignment where experiment_id = ? and user_id = ? and context = ?")
    Result<UserAssignment> selectBy(UUID experimentId, String userId, String context);

    @Query("delete from user_assignment where experiment_id = ? and user_id = ? and context = ?")
    ResultSet deleteBy(UUID experimentId, String userId, String context);

}
