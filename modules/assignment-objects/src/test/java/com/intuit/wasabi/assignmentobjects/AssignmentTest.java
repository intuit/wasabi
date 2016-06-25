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

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

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
    private Boolean cacheable = true;

    private Assignment assignment;

    @Before
    public void setUp() throws Exception {
        assignment = createAssignment();
    }

    /**
     *
     */
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
        assertNotNull(assignment.toString());
        assertNotNull(assignment.getApplicationName());
        assertNotNull(assignment.getBucketLabel());
        assertNotNull(assignment.getContext());
        assertNotNull(assignment.getCreated());
        assertNotNull(assignment.getExperimentID());
        assertNotNull(assignment.getStatus());
        assertNotNull(assignment.getUserID());
        assertTrue(assignment.isCacheable());
        assertTrue(assignment.getStatus().isCacheable());
        assertFalse(assignment.isBucketEmpty());
    }

    @Test
    public void testAssignmentWithEmptyBucket() {
        Assignment assignment = createAssignmentWithEmptyBucket();
        assertNotNull(assignment.toString());
        assertNotNull(assignment.getApplicationName());
        assertNotNull(assignment.getBucketLabel());
        assertNotNull(assignment.getContext());
        assertNotNull(assignment.getCreated());
        assertNotNull(assignment.getExperimentID());
        assertNotNull(assignment.getStatus());
        assertNotNull(assignment.getUserID());
        assertTrue(assignment.isCacheable());
        assertTrue(assignment.getStatus().isCacheable());
        assertTrue(assignment.isBucketEmpty());
    }

    @Test
    public void testAssignmentWithEmptyBucketEqualsAndHash() {
        Assignment assignment1 = createAssignmentWithEmptyBucket();
        Assignment assignment2 = createAssignmentWithEmptyBucket();
        assertEquals(assignment1, assignment2);
        assertEquals(assignment2, assignment1);
        assertEquals(assignment1, assignment1);
        assertEquals(assignment2, assignment2);
        assertEquals(assignment1.hashCode(), assignment2.hashCode());
        assertEquals(assignment2.hashCode(), assignment1.hashCode());
        assertEquals(assignment1.hashCode(), assignment1.hashCode());
        assertEquals(assignment2.hashCode(), assignment2.hashCode());
    }

    @Test
    public void testAssignmentWithEmptyBucketNotEqualsAndHash() {
        Assignment assignment1 = createAssignmentWithEmptyBucket();
        Assignment assignment2 = createAssignmentWithEmptyBucket();
        assignment2.setBucketEmpty(false);
        assertFalse(assignment1.equals(assignment2));
        assertFalse(assignment2.equals(assignment1));
        assertFalse(assignment1.toString().equals(assignment2.toString()));
        assertFalse(assignment2.toString().equals(assignment1.toString()));
        assertEquals(assignment1, assignment1);
        assertEquals(assignment2, assignment2);
        assertFalse(assignment1.hashCode() == assignment2.hashCode());
        assertFalse(assignment2.hashCode() == assignment1.hashCode());
        assertTrue(assignment1.hashCode() == assignment1.hashCode());
        assertTrue(assignment2.hashCode() == assignment2.hashCode());
    }

    @Test
    public void testAssignmentWithEmptyBucketAndDefaultNotEqualsAndHash() {
        Assignment assignment1 = createAssignment();
        Assignment assignment2 = createAssignmentWithEmptyBucket();
        assertFalse(assignment1.equals(assignment2));
        assertFalse(assignment2.equals(assignment1));
        assertFalse(assignment1.toString().equals(assignment2.toString()));
        assertFalse(assignment2.toString().equals(assignment1.toString()));
        assertEquals(assignment1, assignment1);
        assertEquals(assignment2, assignment2);
        assertFalse(assignment1.hashCode() == assignment2.hashCode());
        assertFalse(assignment2.hashCode() == assignment1.hashCode());
        assertTrue(assignment1.hashCode() == assignment1.hashCode());
        assertTrue(assignment2.hashCode() == assignment2.hashCode());
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
        assertEquals(applicationName, assignment.getApplicationName());
        assertEquals(bucketLabel, assignment.getBucketLabel());
        assertTrue(assignment.isCacheable());
        assertEquals(context, assignment.getContext());
        assertEquals(created, assignment.getCreated());
        assertEquals(experimentID, assignment.getExperimentID());
        assertEquals(status, assignment.getStatus());
        assertEquals(userID, assignment.getUserID());
    }

    @Test
    public void testAssignmentFromOther() {
        Assignment newAssignment = Assignment.from(assignment).build();

        assertNotNull(newAssignment.toString());
        assertNotNull(newAssignment.getApplicationName());
        assertNotNull(newAssignment.getBucketLabel());
        assertNotNull(newAssignment.getContext());
        assertNotNull(newAssignment.getCreated());
        assertNotNull(newAssignment.getExperimentID());
        assertNotNull(newAssignment.getStatus());
        assertNotNull(newAssignment.getUserID());

        assertEquals(assignment, newAssignment);
        assertEquals(assignment, assignment);
        assertEquals(assignment.hashCode(), assignment.hashCode());
    }


}
