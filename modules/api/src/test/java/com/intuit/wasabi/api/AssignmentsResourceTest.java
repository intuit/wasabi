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
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.exceptions.AssignmentNotFoundException;
import com.intuit.wasabi.exceptions.AuthenticationException;
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
import javax.ws.rs.core.Response;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.Charset.forName;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentsResourceTest {

    private static final String USERPASS = new String(encodeBase64("admin@example.com:admin01".getBytes(forName("UTF-8"))), forName("UTF-8"));
    private static final String AUTHHEADER = "Basic: " + USERPASS;
    private static final UserInfo.Username USER = UserInfo.Username.valueOf("admin@example.com");
    private static final String HOST_IP = "hostIP";
    private static final String RULE_CACHE = "ruleCache";
    private static final String QUEUE_SIZE = "queueSize";

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
    @javax.ws.rs.core.Context
    private HttpHeaders headers;
    @Mock
    private Map<String, Object> submittedData;
    @Mock
    private Page.Name pageName;
    @Mock
    private Authorization authorization;
    private AssignmentsResource resource;
    private Application.Name applicationName = Application.Name.valueOf("testApp");
    private Experiment.Label experimentLabel = Experiment.Label.valueOf("testExp");
    private User.ID userID = User.ID.valueOf("12345");
    private Boolean createAssignment = true;
    private Boolean ignoreSamplingPercent = true;
    private Boolean forceProfileCheck = false;

    @Before
    public void setUp() {
        resource = new AssignmentsResource(assignments, new HttpHeader("application-name", "600"), authorization);
    }

    @Test
    public void testGetAssignmentNull() {
        when(assignments.doSingleAssignment(userID, applicationName, experimentLabel,
                context, createAssignment, ignoreSamplingPercent, null,
                headers,forceProfileCheck)).thenReturn(null);
        thrown.expect(AssignmentNotFoundException.class);
        resource.getAssignment(applicationName, experimentLabel, userID, context, createAssignment, ignoreSamplingPercent, headers);
    }

    @Test
    public void getAssignment() {
        when(assignments.doSingleAssignment(userID, applicationName, experimentLabel,
                context, createAssignment, ignoreSamplingPercent, null,
                headers,forceProfileCheck)).thenReturn(assignment);
        when(assignment.getStatus()).thenReturn(Status.NEW_ASSIGNMENT);
        when(assignment.getBucketLabel()).thenReturn(label);
        when(assignment.getContext()).thenReturn(context);

        assertNotNull(resource.getAssignment(applicationName, experimentLabel, userID, context, createAssignment, ignoreSamplingPercent, headers));
    }

    @Test
    public void postAssignmentNull() throws Exception {
        when(assignments.doSingleAssignment(userID, applicationName, experimentLabel,
                context, createAssignment, ignoreSamplingPercent, segmentationProfile, headers,forceProfileCheck)).thenReturn(null);
        thrown.expect(AssignmentNotFoundException.class);
        resource.postAssignment(applicationName, experimentLabel, userID, createAssignment, ignoreSamplingPercent, context, segmentationProfile,forceProfileCheck,headers);
    }

    @Test
    public void postAssignment() throws Exception {
        when(assignments.doSingleAssignment(userID, applicationName, experimentLabel,
                context, createAssignment, ignoreSamplingPercent, segmentationProfile, headers,forceProfileCheck)).thenReturn(assignment);
        when(assignment.getStatus()).thenReturn(Status.EXPERIMENT_PAUSED);
        when(assignment.getBucketLabel()).thenReturn(label);
        when(assignment.getContext()).thenReturn(context);

        assertNotNull(resource.postAssignment(applicationName, experimentLabel, userID, createAssignment, ignoreSamplingPercent, context, segmentationProfile, forceProfileCheck, headers));
    }

    @Test
    public void testPostAssignmentRuleTest()
            throws Exception {
        when(assignments.doSegmentTest(applicationName, experimentLabel, context, segmentationProfile,
                headers)).thenReturn(true);
        when(assignment.getStatus()).thenReturn(Status.EXPERIMENT_PAUSED);
        when(assignment.getBucketLabel()).thenReturn(label);
        when(assignment.getContext()).thenReturn(context);

        assertNotNull(resource.postAssignmentRuleTest(applicationName, experimentLabel, context, segmentationProfile, headers));
    }

    @Test
    public void getBatchAssignmentExp() throws Exception {
        List<Assignment> myAssignments = new ArrayList<>();
        when(assignments.doBatchAssignments(userID, applicationName, context,
                CREATE, FORCE_IN_EXPERIMENT, headers, experimentBatch,forceProfileCheck)).thenReturn(myAssignments);
        assertNotNull(resource.getBatchAssignments(applicationName, userID, context, CREATE, experimentBatch, forceProfileCheck, headers));
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
        List<Assignment> assignmentsFromPage = new ArrayList<>();

        when(assignments.doPageAssignments(applicationName, pageName, userID, context,
                createAssignment, ignoreSamplingPercent, headers, null,forceProfileCheck)).thenReturn(assignmentsFromPage);

        assertNotNull(resource.getBatchAssignmentForPage(applicationName, pageName, userID, createAssignment,
                ignoreSamplingPercent, context, headers));
    }

    @Test
    public void postBatchAssignmentForPage() throws Exception {
        List<Assignment> assignmentsFromPage = new ArrayList<>();

        when(assignments.doPageAssignments(applicationName, pageName, userID, context,
                createAssignment, ignoreSamplingPercent, headers, segmentationProfile,forceProfileCheck)).thenReturn(assignmentsFromPage);

        assertNotNull(resource.postBatchAssignmentForPage(applicationName, pageName, userID, createAssignment,
                ignoreSamplingPercent, context, segmentationProfile, forceProfileCheck, headers));
    }

    @Test
    public void getAssignmentsQueueLength() throws Exception {
        assertThat(resource.getAssignmentsQueueLength().getStatus(), is(HttpStatus.SC_OK));
    }

    @Test
    public void getAssignmentsQueueDetails() throws Exception {
        Map<String, Object> queueDetailsMap = new HashMap<String, Object>();
        try {
            queueDetailsMap.put(HOST_IP, InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            // ignore
        }
        Map<String, Object> testIngestionExecutorMap = new HashMap<String, Object>();
        testIngestionExecutorMap.put(QUEUE_SIZE, new Integer(0));
        Map<String, Object> ruleCacheMap = new HashMap<String, Object>();
        ruleCacheMap.put(QUEUE_SIZE, new Integer(0));
        queueDetailsMap.put(RULE_CACHE, ruleCacheMap);
        queueDetailsMap.put("test", testIngestionExecutorMap);
        assertThat(resource.getAssignmentsQueueDetails().getStatus(), is(HttpStatus.SC_OK));
        when(assignments.queuesDetails()).thenReturn(queueDetailsMap);
        Response response = resource.getAssignmentsQueueDetails();
        assertEquals(queueDetailsMap, response.getEntity());
    }

    @Test
    public void flushMessages() throws Exception {
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        assertThat(resource.flushMessages(AUTHHEADER).getStatus(), is(HttpStatus.SC_NO_CONTENT));
    }

    @Test
    public void flushMessagesNotSuperAdmin() throws Exception {
        // fewer allowed experiments
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        //this throw is so that only the allowed (TESTAPP) experiments get returned
        doThrow(AuthenticationException.class).when(authorization).checkSuperAdmin(USER);
        try {
            resource.flushMessages(AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }


    @Test
    public void clearAssignmentsMetadataCacheTest() throws Exception {
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        assertThat(resource.clearMetadataCache(AUTHHEADER).getStatus(), is(HttpStatus.SC_OK));
    }

    @Test
    public void clearAssignmentsMetadataCacheNotSuperAdminTest() throws Exception {
        // fewer allowed experiments
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        //this throw is so that only the allowed (TESTAPP) experiments get returned
        doThrow(AuthenticationException.class).when(authorization).checkSuperAdmin(USER);
        try {
            resource.clearMetadataCache(AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void getMetadataCacheDetailsTest() throws Exception {
        assertThat(resource.getMetadataCacheDetails().getStatus(), is(HttpStatus.SC_OK));
    }

    @Test
    public void toMapTest() {

        Assignment assignment = Assignment.newInstance(Experiment.ID.newInstance())
                .withExperimentLabel(Experiment.Label.valueOf("TestExpLabel"))
                .withApplicationName(Application.Name.valueOf("testApp"))
                .withUserID(User.ID.valueOf("TestUser"))
                .withBucketLabel(Bucket.Label.valueOf("red"))
                .withPayload("RedBucketPayload")
                .withCacheable(false)
                .withContext(Context.valueOf("TEST"))
                .withStatus(Status.EXISTING_ASSIGNMENT)
                .build();

        Map<String, Object> response1 = resource.toSingleAssignmentResponseMap(assignment);
        assertThat(response1.size(), is(5));
        assertNull(response1.get("experimentLabel"));

        Map<String, Object> response2 = resource.toBatchAssignmentResponseMap(assignment);
        assertThat(response2.size(), is(4));
        assertNotNull(response2.get("experimentLabel"));
    }

    @Test
    public void toMapListTest() {

        Assignment assignment1 = Assignment.newInstance(Experiment.ID.newInstance())
                .withExperimentLabel(Experiment.Label.valueOf("TestExpLabel"))
                .withApplicationName(Application.Name.valueOf("testApp"))
                .withUserID(User.ID.valueOf("TestUser"))
                .withBucketLabel(Bucket.Label.valueOf("red"))
                .withPayload("RedBucketPayload")
                .withCacheable(false)
                .withContext(Context.valueOf("TEST"))
                .withStatus(Status.EXISTING_ASSIGNMENT)
                .build();

        Assignment assignment2 = Assignment.newInstance(Experiment.ID.newInstance())
                .withExperimentLabel(Experiment.Label.valueOf("TestExpLabel2"))
                .withApplicationName(Application.Name.valueOf("testApp2"))
                .withUserID(User.ID.valueOf("TestUser2"))
                .withBucketLabel(Bucket.Label.valueOf("red2"))
                .withPayload("RedBucketPayload2")
                .withCacheable(false)
                .withContext(Context.valueOf("TEST"))
                .withStatus(Status.NEW_ASSIGNMENT)
                .build();

        List<Assignment> assignments = newArrayList(assignment1, assignment2);

        Map<String, Object> response1 = resource.toSingleAssignmentResponseMap(assignments.get(0));
        assertThat(response1.size(), is(5));
        assertNull(response1.get("experimentLabel"));

        response1 = resource.toSingleAssignmentResponseMap(assignments.get(1));
        assertThat(response1.size(), is(5));
        assertNull(response1.get("experimentLabel"));

        List<Map<String, Object>> response2 = resource.toBatchAssignmentResponseMap(assignments);
        assertThat(response2.size(), is(2));
        assertNotNull(response2.get(0).get("experimentLabel"));
        assertNotNull(response2.get(1).get("experimentLabel"));
    }

}
