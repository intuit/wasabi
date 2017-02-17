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

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.repository.cassandra.pojo.Bucket;

import java.util.List;
import java.util.UUID;

/**
 * Access for bucket table
 */
@Accessor
public interface BucketAccessor {

    /**
     * Get buckets for the experiments
     *
     * @param experimentIds
     * @return buckets
     */
    @Query("select * from bucket where experiment_id IN ?")
    Result<Bucket> getBucketByExperimentIds(List<UUID> experimentIds);

    /**
     * Get bucket for one experiment
     *
     * @param experimentId
     * @return buckets
     */
    @Query("select * from bucket where experiment_id = ?")
    Result<Bucket> getBucketByExperimentId(UUID experimentId);

    @Query("select * from bucket where experiment_id = ?")
    ListenableFuture<Result<Bucket>> asyncGetBucketByExperimentId(UUID experimentId);

    /**
     * Insert a bucket
     *
     * @param experimentId
     * @param label
     * @param description
     * @param allocation
     * @param isControl
     * @param payload
     * @param state
     */
    @Query("insert into bucket (experiment_id, label, description, "
            + "allocation, is_control, payload, state) " +
            "values (?, ?, ?, ?, ?, ?, ?)")
    void insert(UUID experimentId, String label, String description,
                double allocation, boolean isControl, String payload, String state);

    /**
     * Delete bucket for experiment
     *
     * @param experimentId
     */
    @Query("delete from bucket where experiment_id = ?")
    void deleteByExperimentId(UUID experimentId);

    /**
     * Delete bucket for experiment id and label
     *
     * @param experimentId
     * @param label
     */
    @Query("delete from bucket where experiment_id = ? and label = ?")
    void deleteByExperimentIdAndLabel(UUID experimentId, String label);

    /**
     * Update bucket state
     *
     * @param name
     * @param rawID
     * @param label
     */
    @Query("update bucket set state = ? where experiment_id = ? and label = ?")
    void updateState(String name, UUID rawID, String label);

    /**
     * Get bucket for experiment id and label
     *
     * @param rawID
     * @param label
     * @return bucket result
     */
    @Query("select * from bucket where experiment_id = ? and label = ?")
    Result<Bucket> getBucketByExperimentIdAndBucket(UUID rawID, String label);

    /**
     * Update control status of bucket
     *
     * @param isControl
     * @param experimentId
     * @param label
     */
    @Query("update bucket set is_control= ? where experiment_id =? and label =?")
    void updateControl(boolean isControl, UUID experimentId, String label);

    /**
     * Update bucket based on params
     *
     * @param description
     * @param allocationPercent
     * @param control
     * @param payload
     * @param experimentId
     * @param label
     */
    @Query("update bucket " +
            "set description = ?, allocation = ?, is_control = ?, payload = ? " +
            "where experiment_id = ? and label = ?")
    void updateBucket(String description, Double allocationPercent, Boolean control,
                      String payload, UUID experimentId, String label);

    @Query("update bucket " +
            "set allocation = ? " +
            "where experiment_id = ? and label = ?")
    void updateAllocation(Double allocation, UUID experimentId,
                          String label);

}
