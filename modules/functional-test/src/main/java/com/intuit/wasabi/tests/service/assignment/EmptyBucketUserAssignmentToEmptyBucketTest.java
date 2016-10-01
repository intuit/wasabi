/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.tests.service.assignment;


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
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;

/**
 * A test to check if user can be assigned if the previous assignment bucket is empty
 */
public class EmptyBucketUserAssignmentToEmptyBucketTest extends TestBase {

    private Experiment experiment;
    private User specialUser = UserFactory.createUser("SpecialForBucketTest");

    @BeforeClass
    public void setup() {
        setResponseLogLengthLimit(1000);

        experiment = ExperimentFactory.createExperiment();

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);

    }

    /**
     * Test scenario where user is being reassigned from empty bucket to another empty bucket
     */
    @Test(dependsOnGroups = {"ping"})
    public void addUserToExperimentAndEmptyBucketAndReassignToAnotherEmptyBucket() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
        List<Bucket> buckets = BucketFactory.createBuckets(experiment, 3);
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

        // Empty 2 buckets
        List<Bucket> emptyBucket = new ArrayList<>();
        emptyBucket.add(buckets.get(0));
        emptyBucket.add(buckets.get(1));
        putBucketsState(emptyBucket, Constants.BUCKET_STATE_EMPTY);

        // Assign to empty bucket (1)
        Assignment assignmentAfterEmpty = AssignmentFactory.createAssignment()
                .setAssignment(buckets.get(1).label)
                .setExperimentLabel(experiment.label)
                .setOverwrite(false);
        putAssignment(experiment, assignmentAfterEmpty, specialUser, null, HttpStatus.SC_NOT_FOUND);

    }
}
