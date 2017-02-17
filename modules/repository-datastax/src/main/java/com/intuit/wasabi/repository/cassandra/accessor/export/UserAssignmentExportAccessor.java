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
package com.intuit.wasabi.repository.cassandra.accessor.export;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.export.UserAssignmentExport;

import java.util.Date;
import java.util.UUID;

/**
 * Accessor interface
 */
@Accessor
public interface UserAssignmentExportAccessor {

    /**
     * Insert with attributes
     *
     * @param uuid
     * @param userId
     * @param context
     * @param created
     * @param dayHour
     * @param bucketLabel
     * @param isBucketNull
     * @return result set
     */
    @Query("insert into user_assignment_export (experiment_id, user_id, context, created, day_hour, bucket_label, is_bucket_null)" +
            " values (?, ?, ?, ?, ?, ?, ?)")
    ResultSet insertBy(UUID uuid, String userId, String context, Date created, Date dayHour, String bucketLabel, boolean isBucketNull);

    /**
     * Get by attributes
     *
     * @param experimentId
     * @param dayHour
     * @param context
     * @return result
     */
    @Query("select * from user_assignment_export where experiment_id = ? and day_hour = ? and context = ?")
    Result<UserAssignmentExport> selectBy(UUID experimentId, Date dayHour, String context);

    /**
     * Get by attributes
     *
     * @param experimentId
     * @param dayHour
     * @param context
     * @param isBucketNull
     * @return result
     */
    @Query("select * from user_assignment_export where experiment_id = ? and day_hour = ? and context = ? and is_bucket_null = ?")
    Result<UserAssignmentExport> selectBy(UUID experimentId, Date dayHour, String context, boolean isBucketNull);
}
