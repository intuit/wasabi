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

import com.intuit.wasabi.assignmentobjects.Assignment.Status;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for {@link Assignment}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AssignmentTest {

    private User.ID userID = User.ID.valueOf("1234567890");
    private Experiment.ID experimentID = Experiment.ID.newInstance();
    private Date created = new Date();
    private Application.Name applicationName = Application.Name.valueOf("testApp");
    private Bucket.Label bucketLabel = Bucket.Label.valueOf("testBucketLabel");
    @Mock
    private Context context;
    private Status status = Status.NEW_ASSIGNMENT;
    private Boolean cacheable = Boolean.valueOf(true);

    private Assignment assignment;

    @Before
    public void setUp() throws Exception {
        assignment = createAssignment();
    }

    private Assignment createAssignment() {
        return Assignment.newInstance(experimentID)
                .withApplicationName(applicationName)
                .withBucketLabel(bucketLabel)
                .withCacheable(cacheable)
                .withContext(context)
                .withCreated(created)
                .withStatus(status)
                .withUserID(userID)
                .build();
    }

    private Assignment createAssignmentWithEmptyBucket() {
        return Assignment.newInstance(experimentID)
                .withApplicationName(applicationName)
                .withBucketLabel(bucketLabel)
                .withCacheable(cacheable)
                .withContext(context)
                .withCreated(created)
                .withStatus(status)
                .withUserID(userID)
                .withBucketEmpty(true)
                .build();
    }

    @Test
    public void testAssignment() {
        String assigString = assignment.toString();
        assertThat(assigString, containsString(applicationName.toString()));
        assertThat(assigString, containsString(bucketLabel.toString()));
        assertThat(assigString, containsString(context.toString()));
        assertThat(assigString, containsString(created.toString()));
        assertThat(assigString, containsString(experimentID.toString()));
        assertThat(assigString, containsString(status.toString()));
        assertThat(assigString, containsString(userID.toString()));

        assertThat(assignment.getApplicationName(), is(applicationName));
        assertThat(assignment.getBucketLabel(), is(bucketLabel));
        assertThat(assignment.getContext(), is(context));
        assertThat(assignment.getCreated(), is(created));
        assertThat(assignment.getExperimentID(), is(experimentID));
        assertThat(assignment.getStatus(), is(status));
        assertThat(assignment.getUserID(), is(userID));
        assertThat(assignment.isCacheable(), is(true));
        assertThat(assignment.getStatus().isCacheable(), is(true));
        assertThat(assignment.isBucketEmpty(), is(false));
    }

    @Test
    public void testAssignmentWithEmptyBucket() {
        Assignment assignment = createAssignmentWithEmptyBucket();
        assertThat(assignment.isBucketEmpty(), is(true));
    }

    @Test
    public void testAssignmentWithEmptyBucketEqualsAndHash() {
        Assignment assignment1 = createAssignmentWithEmptyBucket();
        Assignment assignment2 = createAssignmentWithEmptyBucket();
        assertThat(assignment1, is(assignment2));
        assertThat(assignment2, is(assignment1));
        assertThat(assignment1, is(assignment1));
        assertThat(assignment2, is(assignment2));
        assertThat(assignment1.hashCode(), is(assignment2.hashCode()));
        assertThat(assignment2.hashCode(), is(assignment1.hashCode()));
        assertThat(assignment1.hashCode(), is(assignment1.hashCode()));
        assertThat(assignment2.hashCode(), is(assignment2.hashCode()));
    }

    @Test
    public void testAssignmentWithEmptyBucketNotEqualsAndHash() {
        Assignment assignment1 = createAssignmentWithEmptyBucket();
        Assignment assignment2 = createAssignmentWithEmptyBucket();
        assignment2.setBucketEmpty(false);
        assertThat(assignment1, not(assignment2));
        assertThat(assignment2, not(assignment1));
        assertThat(assignment1.toString(), not(assignment2.toString()));
        assertThat(assignment2.toString(), not(assignment1.toString()));
        assertThat(assignment1, is(assignment1));
        assertThat(assignment2, is(assignment2));
        assertThat(assignment1.hashCode(), not(assignment2.hashCode()));
        assertThat(assignment1.hashCode(), is(assignment1.hashCode()));
    }

    @Test
    public void testAssignmentWithEmptyBucketAndDefaultNotEqualsAndHash() {
        Assignment assignment1 = createAssignment();
        Assignment assignment2 = createAssignmentWithEmptyBucket();
        assertThat(assignment1, not(assignment2));
        assertThat(assignment2, not(assignment1));
        assertThat(assignment1.toString(), not(assignment2.toString()));
        assertThat(assignment2.toString(), not(assignment1.toString()));
        assertThat(assignment1, is(assignment1));
        assertThat(assignment2, is(assignment2));
        assertThat(assignment1.hashCode(), not(assignment2.hashCode()));
        assertThat(assignment1.hashCode(), is(assignment1.hashCode()));
    }

    @Test
    public void testAssignmentSet() {
        assignment.setApplicationName(applicationName);
        assignment.setBucketLabel(bucketLabel);
        assignment.setCacheable(cacheable);
        assignment.setContext(context);
        assignment.setCreated(created);
        assignment.setExperimentID(experimentID);
        assignment.setStatus(status);
        assignment.setUserID(userID);

        assertThat(assignment.getApplicationName(), is(applicationName));
        assertThat(assignment.getBucketLabel(), is(bucketLabel));
        assertThat(assignment.isCacheable(), is(true));
        assertThat(assignment.getContext(), is(context));
        assertThat(assignment.getCreated(), is(created));
        assertThat(assignment.getExperimentID(), is(experimentID));
        assertThat(assignment.getStatus(), is(status));
        assertThat(assignment.getUserID(), is(userID));
    }

    @Test
    public void testAssignmentFromOther() {
        Assignment newAssignment = Assignment.from(assignment).build();

        assertThat(newAssignment.toString(), is(assignment.toString()));
        assertThat(newAssignment.getApplicationName(), is(assignment.getApplicationName()));
        assertThat(newAssignment.getBucketLabel(), is(assignment.getBucketLabel()));
        assertThat(newAssignment.getContext(), is(assignment.getContext()));
        assertThat(newAssignment.getCreated(), is(assignment.getCreated()));
        assertThat(newAssignment.getExperimentID(), is(assignment.getExperimentID()));
        assertThat(newAssignment.getStatus(), is(assignment.getStatus()));
        assertThat(newAssignment.getUserID(), is(assignment.getUserID()));

        assertThat(assignment, is(newAssignment));
    }


}
