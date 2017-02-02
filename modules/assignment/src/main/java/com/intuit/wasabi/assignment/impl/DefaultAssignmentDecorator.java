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
package com.intuit.wasabi.assignment.impl;

import com.google.inject.Inject;
import com.intuit.wasabi.assignment.AssignmentDecorator;
import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User.ID;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;

public class DefaultAssignmentDecorator implements AssignmentDecorator {

    @Inject
    public DefaultAssignmentDecorator() {
    }

    @Override
    public BucketList getBucketList(Experiment experiment, ID userID, SegmentationProfile segmentationProfile) {
        return null;
    }
}
