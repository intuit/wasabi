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
import com.intuit.wasabi.repository.cassandra.pojo.count.HourlyBucketCount;

import java.util.UUID;

/**
 * Accessor interface
 */
@Accessor
public interface HourlyBucketCountAccessor {
    @Query("UPDATE hourly_bucket_counts SET hourly_bucket_count = hourly_bucket_count + count " +
            "WHERE experiment_id = ? and bucket_label = ? and event_time_hour = ? and count = ?")
    ResultSet incrementCountBy(UUID experimentId, String bucketLabel, int eventTimeHour, int count);

    @Query("UPDATE hourly_bucket_counts SET hourly_bucket_count = hourly_bucket_count - count " +
            "WHERE experiment_id = ? and bucket_label = ? and event_time_hour = ? and count = ?")
    ResultSet decrementCountBy(UUID experimentId, String bucketLabel, int eventTimeHour, int count);

    @Query("SELECT * FROM hourly_bucket_counts WHERE experiment_id = ?")
    Result<HourlyBucketCount> selectBy(UUID experimentId);

}

