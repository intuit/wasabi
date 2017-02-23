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
package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;

/**
 * Helper class for converting bucket related objects
 * TODO: Currently it has static methods but can be changed to non static instance which
 * can be injected
 */
public class BucketHelper {

    public static Bucket makeBucket(
            com.intuit.wasabi.repository.cassandra.pojo.Bucket bucketPojo) {
        Bucket bucket = Bucket.newInstance(Experiment.ID.valueOf(bucketPojo.getExperimentId()),
                Bucket.Label.valueOf(bucketPojo.getLabel()))
                .withAllocationPercent(bucketPojo.getAllocation())
                .withControl(bucketPojo.isControl())
                .withDescription(bucketPojo.getDescription())
                .withPayload(bucketPojo.getPayload())
                .withState(Bucket.State.valueOf(bucketPojo.getState()))
                .build();
        return bucket;
    }

}
