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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.HttpHeaders;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpHeaders httpHeaders;

    private AssignmentEnvelopePayload payload = new AssignmentEnvelopePayload();

    @Test
    public void testJson() {
        payload = new AssignmentEnvelopePayload(userID, context, createAssignment,
                putAssignment, ignoreSamplingPercent, segmentationProfile, assignmentStatus,
                bucketLabel, pageName, applicationName, experimentLabel, experimentID, date, httpHeaders);

        String payloadJson = payload.toJson();
        assertTrue(payloadJson.contains(userID.toString()));
        assertTrue(payloadJson.contains(context.toString()));
        assertTrue(payloadJson.contains(String.valueOf(createAssignment)));
        assertTrue(payloadJson.contains(String.valueOf(putAssignment)));
        assertTrue(payloadJson.contains(String.valueOf(ignoreSamplingPercent)));
        assertTrue(payloadJson.contains(segmentationProfile.getProfile().toString()));
        assertTrue(payloadJson.contains(assignmentStatus.toString()));
        assertTrue(payloadJson.contains(bucketLabel.toString()));
        assertTrue(payloadJson.contains(pageName.toString()));
        assertTrue(payloadJson.contains(applicationName.toString()));
        assertTrue(payloadJson.contains(experimentLabel.toString()));
        assertTrue(payloadJson.contains(experimentID.toString()));
        assertTrue(payloadJson.contains(String.valueOf(date.getTime())));
    }

    @Test
    public void testAssignmentEnvelopePayloadSet() {

        payload.setUserID(userID);
        payload.setContext(context);
        payload.setCreateAssignment(createAssignment);
        payload.setPutAssignment(putAssignment);
        payload.setIgnoreSamplingPercent(ignoreSamplingPercent);
        payload.setSegmentationProfile(segmentationProfile);
        payload.setAssignmentStatus(assignmentStatus);
        payload.setBucketLabel(bucketLabel);
        payload.setPageName(pageName);
        payload.setApplicationName(applicationName);
        payload.setExperimentLabel(experimentLabel);
        payload.setExperimentID(experimentID);
        payload.setDate(date);
        payload.setHttpHeaders(httpHeaders);

        assertEquals(payload.getUserID(), userID);
        assertEquals(payload.getContext(), context);
        assertTrue(payload.isCreateAssignment());
        assertTrue(payload.isPutAssignment());
        assertTrue(payload.isIgnoreSamplingPercent());
        assertEquals(payload.getSegmentationProfile(), segmentationProfile);
        assertEquals(payload.getAssignmentStatus(), assignmentStatus);
        assertEquals(payload.getBucketLabel(), bucketLabel);
        assertEquals(payload.getPageName(), pageName);
        assertEquals(payload.getApplicationName(), applicationName);
        assertEquals(payload.getExperimentLabel(), experimentLabel);
        assertEquals(payload.getExperimentID(), experimentID);
        assertEquals(payload.getDate(), date);
        assertEquals(payload.getHttpHeaders(), httpHeaders);
    }


}
