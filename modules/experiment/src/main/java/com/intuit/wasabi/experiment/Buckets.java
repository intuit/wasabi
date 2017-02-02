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
package com.intuit.wasabi.experiment;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.List;

/**
 * Interface to perform CRUD operations for buckets of an experiment. It also
 * provides validation methods to check the state of bucket.
 */
public interface Buckets {

    /**
     * Creates a new bucket for the specified experiment by adding the
     * specified metadata to the database.
     *
     * @param experimentId the unique experiment id
     * @param newBucket    Bucket object containing bucket metadata
     * @param user         the {@link UserInfo} who created the bucket
     * @return Bucket object containing bucket metadata
     */
    Bucket createBucket(Experiment.ID experimentId, Bucket newBucket, UserInfo user);

    /**
     * Deletes a bucket by removing its metadata from the database.
     *
     * @param experimentId the unique experiment id
     * @param bucketLabel  the unique bucket id
     * @param user         the {@link UserInfo} who deleted the Bucket
     */
    void deleteBucket(Experiment.ID experimentId, Bucket.Label bucketLabel, UserInfo user);

    /**
     * Queries the database and returns metadata for the specified bucket in
     * the specified experiment.
     *
     * @param experimentId the unique experiment id
     * @param bucketLabel  the unique bucket id
     * @return Bucket object containing bucket metadata
     */
    Bucket getBucket(Experiment.ID experimentId, Bucket.Label bucketLabel);

    /**
     * Queries the database and returns a list of buckets with metadata for
     * the specified experiment.
     *
     * @param experimentId    the unique experiment id
     * @param checkExperiment check if experiment exists before querying for bucket list
     * @return a list of Bucket objects containing bucket metadata
     */
    BucketList getBuckets(Experiment.ID experimentId, boolean checkExperiment);

    /**
     * Updates a bucket by updating the specified metadata in the database.
     *
     * @param experimentId the unique experiment id
     * @param bucketLabel  the unique bucket id
     * @param updates      Bucket object containing bucket metadata to be
     *                     updated
     * @param user         the {@link UserInfo} who triggered the change
     * @return Bucket object containing updated bucket metadata
     */
    Bucket updateBucket(Experiment.ID experimentId, Bucket.Label bucketLabel, Bucket updates, UserInfo user);

    /**
     * Updates new allocation percentages for the list of buckets within the specified experiment
     *
     * @param experimentID the unique experiment id
     * @param bucketList   List of Buckets
     * @return the updated list of buckets
     */
    BucketList updateBucketAllocBatch(Experiment.ID experimentID, BucketList bucketList);

    /**
     * Adjust allocation percentage for buckets within the specified experiment.
     *
     * @param experiment {@link Experiment}
     * @param newBucket  The new {@link Bucket} containing the new allocation percentage
     * @return The updated list of buckets with adjusted allocation percentages
     */
    BucketList adjustAllocationPercentages(Experiment experiment, Bucket newBucket);

    /**
     * Updates a bucket's state by updating the specified metadata in the database.
     *
     * @param experimentID the unique experiment id
     * @param bucketLabel  the unique bucket id
     * @param desiredState the desired state
     * @param user         the {@link UserInfo} who updated the state
     * @return Bucket object containing updated bucket metadata
     */
    Bucket updateBucketState(Experiment.ID experimentID, Bucket.Label bucketLabel, Bucket.State desiredState,
                             UserInfo user);

    /**
     * Updates list of buckets in batch with the specified metadata in the database.
     *
     * @param experimentID the unique experiment id
     * @param bucketList   List of Buckets
     * @param user         the {@link UserInfo} who updated the Buckets
     * @return the updated list of buckets.
     */
    BucketList updateBucketBatch(Experiment.ID experimentID, BucketList bucketList, UserInfo user);

    /**
     * Validate bucket changes with the specified metadata
     *
     * @param bucket  Current bucket
     * @param updates Bucket with the new metadata
     * @throws IllegalArgumentException if trying to update experimentID, bucket label or bucket state
     */
    void validateBucketChanges(Bucket bucket, Bucket updates);

    /**
     * Build a bucket change list and return it as a list of {@link Bucket.BucketAuditInfo}.
     *
     * @param bucket  Current Bucket
     * @param updates Bucket updates
     * @param builder {@link Bucket.Builder}
     * @return A list of changes as a list of {@link Bucket.BucketAuditInfo}.
     */
    List<Bucket.BucketAuditInfo> getBucketChangeList(Bucket bucket, Bucket updates, Bucket.Builder builder);

    /**
     * Get bucket with the specified bucketLabel and experimentID from the database and return {@link Bucket.Builder} based on that.
     *
     * @param experimentID the unique experiment id
     * @param bucketLabel  Bucket label
     * @return Bucket.Builder based on the specified bucketLabel and experimentID.
     */
    Bucket.Builder getBucketBuilder(Experiment.ID experimentID, Bucket.Label bucketLabel);

    /**
     * Go through the list of old buckets and replace them with the new buckets.
     *
     * @param oldBuckets Old bucket list
     * @param newBuckets New bucket list
     * @return BucketList with the new buckets in the new BucketList
     */
    BucketList combineOldAndNewBuckets(BucketList oldBuckets, BucketList newBuckets);

}
