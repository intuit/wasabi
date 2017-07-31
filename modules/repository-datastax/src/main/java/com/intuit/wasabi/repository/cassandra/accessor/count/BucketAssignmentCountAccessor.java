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
package com.intuit.wasabi.repository.cassandra.accessor.count;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount;

import java.util.UUID;

/**
 * Accessor interface
 */
@Accessor
public interface BucketAssignmentCountAccessor {
    @Query("UPDATE bucket_assignment_counts SET bucket_assignment_count = bucket_assignment_count + 1 " +
            "WHERE experiment_id =? and bucket_label = ?")
    ResultSet incrementCountBy(UUID experimentId, String bucketLabel);

    @Query("UPDATE bucket_assignment_counts SET bucket_assignment_count = bucket_assignment_count - 1 " +
            "WHERE experiment_id =? and bucket_label = ?")
    ResultSet decrementCountBy(UUID experimentId, String bucketLabel);

    @Query("SELECT * FROM bucket_assignment_counts WHERE experiment_id =?")
    Result<BucketAssignmentCount> selectBy(UUID experimentId);

    @Query("SELECT * FROM bucket_assignment_counts WHERE experiment_id =?")
    ListenableFuture<Result<BucketAssignmentCount>> selectByAsync(UUID experimentId);

}
