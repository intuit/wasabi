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
package com.intuit.wasabi.assignmentobjects;

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.HashMap;

/**
 * Test for the {@link AssignmentEnvelopePayload}
 */
@RunWith(MockitoJUnitRunner.class)
public class AssignmentEnvelopePayloadTest {

    private User.ID userID = User.ID.valueOf("testUser");
    private Context context = Context.valueOf("testContext");
    private boolean createAssignment = true;
    private boolean putAssignment = true;
    private boolean ignoreSamplingPercent = true;
    private SegmentationProfile segmentationProfile = new SegmentationProfile();
    private Assignment.Status assignmentStatus = Assignment.Status.EXISTING_ASSIGNMENT;
    private Bucket.Label bucketLabel = Bucket.Label.valueOf("testLabel");
    private Page.Name pageName = Page.Name.valueOf("testPage");
    private Application.Name applicationName = Application.Name.valueOf("testAppName");
    private Experiment.Label experimentLabel = Experiment.Label.valueOf("testExperimentLabel");
    private Experiment.ID experimentID = Experiment.ID.newInstance();
    private Date date = new Date();

    private AssignmentEnvelopePayload payload;

    @Before
    public void createAssignmendEnvelopePayload() {
        payload = new AssignmentEnvelopePayload(userID, context, createAssignment,
                putAssignment, ignoreSamplingPercent, segmentationProfile, assignmentStatus,
                bucketLabel, pageName, applicationName, experimentLabel, experimentID, date, null);
    }

    @Test
    @Ignore("Only produces output.")
    public void output() {
        System.out.println(payload.toJson());
        HashMap<String, Object> segMap = new HashMap<>();
        segMap.put("salary", 30000);
        segMap.put("state", "CA");
        segMap.put("visited", "home");
        SegmentationProfile segProfile = SegmentationProfile.from(segMap).build();
        System.out.println(new AssignmentEnvelopePayload(userID, context, createAssignment, false,
                ignoreSamplingPercent, segProfile, assignmentStatus, bucketLabel, pageName, applicationName,
                experimentLabel, experimentID, date, null).toJson());
    }
}
