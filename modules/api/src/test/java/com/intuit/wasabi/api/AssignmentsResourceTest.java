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
package com.intuit.wasabi.api;

import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.Assignment.Status;
import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.exceptions.AssignmentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBatch;
import com.intuit.wasabi.experimentobjects.Page;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentsResourceTest {

    private final Boolean CREATE = true;
    private final Boolean FORCE_IN_EXPERIMENT = true;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private Assignments assignments;
    @Mock
    private Assignment assignment;
    @Mock
    private Context context;
    @Mock
    private SegmentationProfile segmentationProfile;
    @Mock
    private Label label;
    @Mock
    private Bucket bucket;
    @Mock
    private ExperimentBatch experimentBatch;
    @Mock
    private
    @javax.ws.rs.core.Context
    HttpHeaders headers;
    @Mock
    private Map<String, Object> submittedData;
    @Mock
    private Page.Name pageName;
    private AssignmentsResource resource;
    private Application.Name applicationName = Application.Name.valueOf("testApp");
    private Experiment.Label experimentLabel = Experiment.Label.valueOf("testExp");
    private User.ID userID = User.ID.valueOf("12345");
    private Boolean createAssignment = true;
    private Boolean ignoreSamplingPercent = true;

    @Before
    public void setUp() {
        resource = new AssignmentsResource(assignments, new HttpHeader("application-name"));
    }

    @Test
    public void testGetAssignmentNull() {
        when(assignments.getSingleAssignment(userID, applicationName, experimentLabel,
                context, createAssignment, ignoreSamplingPercent, null,
                headers, null)).thenReturn(null);
        thrown.expect(AssignmentNotFoundException.class);
        resource.getAssignment(applicationName, experimentLabel, userID, context, createAssignment, ignoreSamplingPercent, headers);
    }

    @Test
    public void getAssignment() {
        when(assignments.getSingleAssignment(userID, applicationName, experimentLabel,
                context, createAssignment, ignoreSamplingPercent, null,
                headers, null)).thenReturn(assignment);
        when(assignment.getStatus()).thenReturn(Status.NEW_ASSIGNMENT);
        when(assignment.getBucketLabel()).thenReturn(label);
        when(assignments.getBucket(any(Experiment.ID.class), any(Label.class))).thenReturn(bucket);
        when(assignment.getContext()).thenReturn(context);

        assertNotNull(resource.getAssignment(applicationName, experimentLabel, userID, context, createAssignment, ignoreSamplingPercent, headers));
    }

    @Test
    public void postAssignmentNull() throws Exception {
        when(assignments.getSingleAssignment(userID, applicationName, experimentLabel,
                context, createAssignment, ignoreSamplingPercent, segmentationProfile, headers, null)).thenReturn(null);
        thrown.expect(AssignmentNotFoundException.class);
        resource.postAssignment(applicationName, experimentLabel, userID, createAssignment, ignoreSamplingPercent, context, segmentationProfile, headers);
    }

    @Test
    public void postAssignment() throws Exception {
        when(assignments.getSingleAssignment(userID, applicationName, experimentLabel,
                context, createAssignment, ignoreSamplingPercent, segmentationProfile, headers, null)).thenReturn(assignment);
        when(assignment.getStatus()).thenReturn(Status.EXPERIMENT_PAUSED);
        when(assignment.getBucketLabel()).thenReturn(label);
        when(assignments.getBucket(any(Experiment.ID.class), any(Label.class))).thenReturn(bucket);
        when(assignment.getContext()).thenReturn(context);

        assertNotNull(resource.postAssignment(applicationName, experimentLabel, userID, createAssignment, ignoreSamplingPercent, context, segmentationProfile, headers));
    }

    @Test
    public void testPostAssignmentRuleTest()
            throws Exception {
        when(assignments.doSegmentTest(applicationName, experimentLabel, context, segmentationProfile,
                headers)).thenReturn(true);
        when(assignment.getStatus()).thenReturn(Status.EXPERIMENT_PAUSED);
        when(assignment.getBucketLabel()).thenReturn(label);
        when(assignments.getBucket(any(Experiment.ID.class), any(Label.class))).thenReturn(bucket);
        when(assignment.getContext()).thenReturn(context);

        assertNotNull(resource.postAssignmentRuleTest(applicationName, experimentLabel, context, segmentationProfile, headers));
    }

    @Test
    public void getBatchAssignmentExp() throws Exception {
        List<Map> myAssignments = new ArrayList<>();
        when(assignments.doBatchAssignments(userID, applicationName, context,
                CREATE, FORCE_IN_EXPERIMENT, headers, experimentBatch, null, null)).thenReturn(myAssignments);
        assertNotNull(resource.getBatchAssignments(applicationName, userID, context, CREATE, experimentBatch, headers));
    }

    @Test
    public void putAssignmentNullSubmittedData() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        resource.updateAssignment(applicationName, experimentLabel, userID, null, context);
    }

    @Test
    public void putAssignment() throws Exception {
        when(submittedData.containsKey("assignment")).thenReturn(true);
        when(submittedData.get("assignment")).thenReturn("foo");
        when(assignments.putAssignment(any(User.ID.class), any(Application.Name.class), any(Experiment.Label.class),
                any(Context.class), any(Bucket.Label.class), anyBoolean())).thenReturn(assignment);
        when(assignment.getStatus()).thenReturn(Status.EXPERIMENT_PAUSED);
        when(assignment.getBucketLabel()).thenReturn(label);
        when(assignments.getBucket(any(Experiment.ID.class), any(Label.class))).thenReturn(bucket);
        when(assignment.getContext()).thenReturn(context);

        assertNotNull(resource.updateAssignment(applicationName, experimentLabel, userID, submittedData, context));
    }

    @Test
    public void putAssignment1() throws Exception {
        when(submittedData.containsKey("assignment")).thenReturn(true);
        when(assignments.putAssignment(any(User.ID.class), any(Application.Name.class), any(Experiment.Label.class),
                any(Context.class), any(Bucket.Label.class), anyBoolean())).thenReturn(assignment);
        when(assignment.getStatus()).thenReturn(Status.EXPERIMENT_PAUSED);
        when(assignment.getBucketLabel()).thenReturn(label);
        when(assignments.getBucket(any(Experiment.ID.class), any(Label.class))).thenReturn(bucket);
        when(assignment.getContext()).thenReturn(context);

        assertNotNull(resource.updateAssignment(applicationName, experimentLabel, userID, submittedData, context));
    }

    @Test
    public void putAssignment2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        resource.updateAssignment(applicationName, experimentLabel, userID, submittedData, context);
    }

    @Test
    public void getBatchAssignmentForPage() throws Exception {
        List<Map> assignmentsFromPage = new ArrayList<>();

        when(assignments.doPageAssignments(applicationName, pageName, userID, context,
                createAssignment, ignoreSamplingPercent, headers, null)).thenReturn(assignmentsFromPage);

        assertNotNull(resource.getBatchAssignmentForPage(applicationName, pageName, userID, createAssignment, ignoreSamplingPercent, context, headers));
    }

    @Test
    public void postBatchAssignmentForPage() throws Exception {
        List<Map> assignmentsFromPage = new ArrayList<>();

        when(assignments.doPageAssignments(applicationName, pageName, userID, context,
                createAssignment, ignoreSamplingPercent, headers, segmentationProfile)).thenReturn(assignmentsFromPage);
        assertNotNull(resource.postBatchAssignmentForPage(applicationName, pageName, userID, createAssignment, ignoreSamplingPercent, context, segmentationProfile, headers));
    }

    @Test
    public void getAssignmentsQueueLength() throws Exception {
        assertTrue(resource.getAssignmentsQueueLength().getStatus() == HttpStatus.SC_OK);
    }
}
