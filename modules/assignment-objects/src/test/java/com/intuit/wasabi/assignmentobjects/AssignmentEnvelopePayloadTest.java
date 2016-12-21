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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(payloadJson, containsString(userID.toString()));
        assertThat(payloadJson, containsString(context.toString()));
        assertThat(payloadJson, containsString(String.valueOf(createAssignment)));
        assertThat(payloadJson, containsString(String.valueOf(putAssignment)));
        assertThat(payloadJson, containsString(String.valueOf(ignoreSamplingPercent)));
        assertThat(payloadJson, containsString(segmentationProfile.getProfile().toString()));
        assertThat(payloadJson, containsString(assignmentStatus.toString()));
        assertThat(payloadJson, containsString(bucketLabel.toString()));
        assertThat(payloadJson, containsString(pageName.toString()));
        assertThat(payloadJson, containsString(applicationName.toString()));
        assertThat(payloadJson, containsString(experimentLabel.toString()));
        assertThat(payloadJson, containsString(experimentID.toString()));
        assertThat(payloadJson, containsString(String.valueOf(date.getTime())));
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

        assertThat(payload.getUserID(), is(userID));
        assertThat(payload.getContext(), is(context));
        assertThat(payload.isCreateAssignment(), is(true));
        assertThat(payload.isPutAssignment(), is(true));
        assertThat(payload.isIgnoreSamplingPercent(), is(true));
        assertThat(payload.getSegmentationProfile(), is(segmentationProfile));
        assertThat(payload.getAssignmentStatus(), is(assignmentStatus));
        assertThat(payload.getBucketLabel(), is(bucketLabel));
        assertThat(payload.getPageName(), is(pageName));
        assertThat(payload.getApplicationName(), is(applicationName));
        assertThat(payload.getExperimentLabel(), is(experimentLabel));
        assertThat(payload.getExperimentID(), is(experimentID));
        assertThat(payload.getDate(), is(date));
        assertThat(payload.getHttpHeaders(), is(httpHeaders));
    }


}
