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
package com.intuit.wasabi.assignment;

import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.exceptions.BucketDistributionNotFetchableException;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.io.IOException;

/**
 * Interface that provides method(s) to modify/enhance the default behavior of retrieving bucket list when
 * retrieving assignment(s).
 */
public interface AssignmentDecorator {

    /**
     * Gets the bucket list through an external source (AssignmentDecorator). AssignmentDecorator can
     * be a personalization engine that will execute a model to return a recommended bucket list. If the call
     * is a successful, it merges the personalization parameters with segmentation profile
     *
     * @param experiment          Experiment , it provides the modelName and modelVersion that needs to be used
     * @param userID              UserId
     * @param segmentationProfile (provides the segmentationProfile to personalize the assignment)
     * @return BucketList, which contains the buckets and the respective
     * allocation percentages for a userID
     * @throws IOException                             IO exception
     * @throws BucketDistributionNotFetchableException not able to fetch bucket distribution
     */
    BucketList getBucketList(Experiment experiment, User.ID userID, SegmentationProfile segmentationProfile)
            throws BucketDistributionNotFetchableException;
}
