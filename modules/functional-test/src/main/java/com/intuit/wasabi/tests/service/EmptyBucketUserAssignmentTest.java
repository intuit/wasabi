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
package com.intuit.wasabi.tests.service;


import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameInclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;

/**
 * A test to check if user can be assigned if the previous assignment bucket is empty
 */
public class EmptyBucketUserAssignmentTest extends TestBase {

    private Experiment experiment;
    private List<Bucket> buckets = new ArrayList<>();
    private User specialUser = UserFactory.createUser("SpecialForBucketTest");
    private Map<User, Assignment> assignments = new HashMap<>();

    /**
     * Initializes a default experiment.
     */
    public EmptyBucketUserAssignmentTest() {
        setResponseLogLengthLimit(1000);

        experiment = ExperimentFactory.createExperiment();

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);

    }

    /**
     * Test reassignment from an empty bucket to non-empty bucket
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_AddUserToExperimentAndEmptyBucket() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
        buckets = BucketFactory.createBuckets(experiment, 3);
        postBuckets(buckets);

        // Start experiment
        experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
        Experiment exp2 = putExperiment(experiment);
        assertEqualModelItems(exp2, experiment);
        experiment.update(exp);

        // Assign special user to bucket 0
        Assignment assignment = AssignmentFactory.createAssignment()
                .setAssignment(buckets.get(0).label)
                .setExperimentLabel(experiment.label)
                .setOverwrite(true);
        Assignment putAssignment = putAssignment(experiment, assignment, specialUser);
        assertEqualModelItems(putAssignment, assignment, new DefaultNameInclusionStrategy("assignment"));
        assignments.put(specialUser, putAssignment);

        List<Bucket> emptyBucket = new ArrayList<>();
        emptyBucket.add(buckets.get(0));
        emptyBucket = putBucketsState(emptyBucket, Constants.BUCKET_STATE_EMPTY);

        Assignment assignmentAfterEmpty = AssignmentFactory.createAssignment()
                .setAssignment(buckets.get(1).label)
                .setExperimentLabel(experiment.label)
                .setOverwrite(false);
        Assignment putAssignmentAfterEmpty = putAssignment(experiment, assignmentAfterEmpty, specialUser);
        assertEqualModelItems(assignmentAfterEmpty, putAssignmentAfterEmpty, new DefaultNameInclusionStrategy("assignment"));

        Assignment assignmentForSpecialAfterReassignment = getAssignment(experiment, specialUser);

        assertEqualModelItems(assignmentForSpecialAfterReassignment, putAssignmentAfterEmpty, new DefaultNameInclusionStrategy("assignment"));
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_AddUserToExperimentAndEmptyBucketAndGetAssignment() {
        experiment = ExperimentFactory.createExperiment();

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);

        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
        buckets = BucketFactory.createBuckets(experiment, 3);
        postBuckets(buckets);

        // Start experiment
        experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
        Experiment exp2 = putExperiment(experiment);
        assertEqualModelItems(exp2, experiment);
        experiment.update(exp);

        // Assign special user to bucket 0
        Assignment assignment = AssignmentFactory.createAssignment()
                .setAssignment(buckets.get(0).label)
                .setExperimentLabel(experiment.label)
                .setOverwrite(true);
        Assignment putAssignment = putAssignment(experiment, assignment, specialUser);
        assertEqualModelItems(putAssignment, assignment, new DefaultNameInclusionStrategy("assignment"));
        assignments.put(specialUser, putAssignment);

        List<Bucket> emptyBucket = new ArrayList<>();
        emptyBucket.add(buckets.get(0));
        emptyBucket = putBucketsState(emptyBucket, Constants.BUCKET_STATE_EMPTY);

        Assignment assignmentForSpecialAfterReassignment = getAssignment(experiment, specialUser);
        Assert.assertEquals(assignmentForSpecialAfterReassignment.status, Constants.NEW_ASSIGNMENT);
        Assignment assignmentForSpecialAfterReassignment2ndCall = getAssignment(experiment, specialUser);

        Assert.assertEquals(assignmentForSpecialAfterReassignment2ndCall.status,
                Constants.ASSIGNMENT_EXISTING_ASSIGNMENT);
        Assert.assertEquals(assignmentForSpecialAfterReassignment.bucket_label,
                assignmentForSpecialAfterReassignment2ndCall.bucket_label);
    }
}
