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
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;
import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItemsNoOrder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tests if mutual exclusions effect all experiments correctly.
 */
public class IntegrationMutualExclusion extends TestBase {

    private static final Logger LOGGER = getLogger(IntegrationMutualExclusion.class);
    private Experiment experimentEx1;
    private Experiment experimentEx2;
    private Experiment experimentNonEx;
    private User user1;
    private User user2;
    private List<Experiment> batchMutExExperiments = new ArrayList<>();
    private Experiment experimentDefApp;
    private Experiment experimentOtherApp;

    /**
     * Tests mutual exclusions.
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_testMutualExclusion() {
        ArrayList<Experiment> experiments = new ArrayList<>(Constants.EXP_SPAWN_COUNT);

        LOGGER.info("Testing mutual exclusion functionality...");
        LOGGER.info("Creating " + Constants.EXP_SPAWN_COUNT + " new experiments to test mutual exclusions functionality...");

        for (int i = 0; i < Constants.EXP_SPAWN_COUNT; i++) {
            Experiment experiment = ExperimentFactory.createExperiment();

            Experiment created = postExperiment(experiment);
            experiment.setState(Constants.EXPERIMENT_STATE_DRAFT);
            assertEqualModelItems(created, experiment, new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson", "description", "rule"));
            experiment.update(created);

            // Make this experiment mutually exclusive with all previous experiment added in this loop.
            if (experiments.size() > 0) {
                LOGGER.info("Making this experiment " + experiment + " mutually exclusive with all previous experiments: " + experiments.toString());
                postExclusions(experiment, experiments);
            }

            // check if all exclusions are correct
            List<Experiment> mutualExclusiveExperiments = getExclusions(experiment);
            if (experiments.size() > 0) {
                assertEqualModelItemsNoOrder(mutualExclusiveExperiments, experiments, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));
            } else {
                Assert.assertEquals(experiments.size(), 0);
            }


            // check if there are other non-mutual experiments and if there are, if they at least are in the same
            // application
            List<Experiment> nonMutualExclusiveExperiments = getExclusions(experiment, true, false);
            for (Experiment nonMutex : nonMutualExclusiveExperiments) {
                Assert.assertFalse(mutualExclusiveExperiments.contains(nonMutex), "Unexpected mutual exclusive experiment!");
                Assert.assertEquals(nonMutex.applicationName, experiment.applicationName, "Application names differ.");
            }

            if (experiments.size() > 0) {
                for (Experiment exp : experiments) {
                    Assert.assertFalse(nonMutualExclusiveExperiments.contains(exp));
                }
            } else {
                Assert.assertEquals(experiments.size(), 0);
            }

            experiments.add(experiment);
        }

        // delete an exclusion and ensure it was successful
        List<Experiment> exclusiveExperimentsPre = getExclusions(experiments.get(0));
        deleteExclusion(experiments.get(0), exclusiveExperimentsPre.get(0));
        List<Experiment> exclusiveExperimentsPost = getExclusions(experiments.get(0));
        exclusiveExperimentsPre.remove(0);
        assertEqualModelItems(exclusiveExperimentsPost, exclusiveExperimentsPre, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));
    }

    /**
     * Prepares the assignments test.
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_prepareTestMutualExclusionAssignments() {
        experimentEx1 = postExperiment(ExperimentFactory.createExperiment());
        toCleanUp.add(experimentEx1);
        experimentEx2 = postExperiment(ExperimentFactory.createExperiment());
        toCleanUp.add(experimentEx2);
        experimentNonEx = postExperiment(ExperimentFactory.createExperiment());
        toCleanUp.add(experimentNonEx);

        postExclusions(Arrays.asList(experimentEx1, experimentEx2));
    }

    /**
     * Checks if the mutual exclusions are correct.
     */
    @Test(dependsOnMethods = {"t_prepareTestMutualExclusionAssignments"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(warmup = 1000, maxTries = 3)
    public void t_checkMutualExclusions() {
        Assert.assertTrue(getExclusions(experimentEx1).contains(experimentEx2), "Experiment 1 should be mutual exclusive with Experiment 2.");
        Assert.assertTrue(getExclusions(experimentEx2).contains(experimentEx1), "Experiment 2 should be mutual exclusive with Experiment 1.");
    }

    /**
     * Prepares buckets for the assignment test.
     */
    @Test(dependsOnMethods = {"t_prepareTestMutualExclusionAssignments"})
    public void t_prepareBucketsForMutualExclusionAssignment() {
        postBucket(BucketFactory.createBucket(experimentEx1).setAllocationPercent(1));
        postBucket(BucketFactory.createBucket(experimentEx2).setAllocationPercent(1));
        postBucket(BucketFactory.createBucket(experimentNonEx).setAllocationPercent(1));
    }

    /**
     * Starts experiments.
     */
    @Test(dependsOnMethods = {"t_prepareBucketsForMutualExclusionAssignment"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 1000)
    public void t_startExperimentsForMutualExclusionAssignment() {
        experimentEx1.setState(Constants.EXPERIMENT_STATE_RUNNING);
        experimentEx1 = putExperiment(experimentEx1);
        Experiment got = getExperiment(experimentEx1);
        assertEqualModelItems(got, experimentEx1, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));

        experimentEx2.setState(Constants.EXPERIMENT_STATE_RUNNING);
        experimentEx2 = putExperiment(experimentEx2);
        got = getExperiment(experimentEx2);
        assertEqualModelItems(got, experimentEx2, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));

        experimentNonEx.setState(Constants.EXPERIMENT_STATE_RUNNING);
        Experiment updated = putExperiment(experimentNonEx);
        assertEqualModelItems(updated, experimentNonEx, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));
        got = getExperiment(experimentNonEx);
        assertEqualModelItems(got, experimentNonEx, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));
    }

    /**
     * Assigns a user to the first experiment, and one to the second and tries to assign them the other way
     * around as well, which should not work.
     * Additionally both users are assigned to the non-mutual exclusive experiment, which should always work.
     */
    @Test(dependsOnMethods = {"t_startExperimentsForMutualExclusionAssignment"})
    public void t_mutualExclusionAssignment() {
        user1 = UserFactory.createUser();
        user2 = UserFactory.createUser();

        Assignment assignmentEx1U1 = postAssignment(experimentEx1, user1);
        Assert.assertNotNull(assignmentEx1U1.assignment);
        Assert.assertTrue(Arrays.asList(Constants.ASSIGNMENT_NEW_ASSIGNMENT, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT).contains(assignmentEx1U1.status));

        Assignment assignmentEx2U2 = postAssignment(experimentEx2, user2);
        Assert.assertNotNull(assignmentEx2U2.assignment);
        Assert.assertTrue(Arrays.asList(Constants.ASSIGNMENT_NEW_ASSIGNMENT, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT).contains(assignmentEx2U2.status));
    }

    /**
     * Assigns users to non-mutual experiments.
     */
    @Test(dependsOnMethods = {"t_mutualExclusionAssignment"})
    public void t_nonMutualExclusionAssignment() {
        Assignment assignmentNonExU1 = postAssignment(experimentNonEx, user1);
        Assert.assertNotNull(assignmentNonExU1.assignment);
        Assert.assertTrue(Arrays.asList(Constants.ASSIGNMENT_NEW_ASSIGNMENT, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT).contains(assignmentNonExU1.status));

        Assignment assignmentNonExU2 = postAssignment(experimentNonEx, user2);
        Assert.assertNotNull(assignmentNonExU2.assignment);
        Assert.assertTrue(Arrays.asList(Constants.ASSIGNMENT_NEW_ASSIGNMENT, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT).contains(assignmentNonExU2.status));
    }

    /**
     * Checks if the assignments were placed correctly.
     */
    @Test(dependsOnMethods = {"t_mutualExclusionAssignment"})
    public void t_checkExistingAssignments() {
        Assignment assignmentEx1U1 = postAssignment(experimentEx1, user1);
        Assert.assertNotNull(assignmentEx1U1.assignment);
        Assert.assertEquals(assignmentEx1U1.status, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT);
        Assignment assignmentEx2U2 = postAssignment(experimentEx2, user2);
        Assert.assertNotNull(assignmentEx2U2.assignment);
        Assert.assertEquals(assignmentEx2U2.status, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT);
    }

    /**
     * Checks if an assignment for mutual exclusive experiments can happen.
     */
    @Test(dependsOnMethods = {"t_checkExistingAssignments"})
    public void t_mutualExclusionAssignmentFail() {
        Assignment assignmentEx2U1 = postAssignment(experimentEx2, user1);
        Assert.assertNull(assignmentEx2U1.assignment);
        Assert.assertEquals(assignmentEx2U1.status, Constants.ASSIGNMENT_NEW_ASSIGNMENT);
        Assignment assignmentEx1U2 = postAssignment(experimentEx1, user2);
        Assert.assertNull(assignmentEx1U2.assignment);
        Assert.assertEquals(assignmentEx1U2.status, Constants.ASSIGNMENT_NEW_ASSIGNMENT);
    }

    /**
     * Deletes the mutex between experimentEx1 and experimentEx2
     */
    @Test(dependsOnMethods = {"t_mutualExclusionAssignmentFail"})
    public void t_deleteMutualExclusion() {
        deleteExclusion(experimentEx1, experimentEx2);
    }

    /**
     * Retries assignments for users to previously mutual-exclusive assignments, should return the same results as before.
     */
    @Test(dependsOnMethods = {"t_deleteMutualExclusion"})
    public void t_reassignToNotMutexAnymore() {
        // Ex 1 U 1
        Assignment assignmentEx1U1 = postAssignment(experimentEx1, user1);
        Assert.assertNotNull(assignmentEx1U1.assignment);
        Assert.assertEquals(assignmentEx1U1.status, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT);

        // Ex 2 U 2
        Assignment assignmentEx2U2 = postAssignment(experimentEx2, user2);
        Assert.assertNotNull(assignmentEx2U2.assignment);
        Assert.assertEquals(assignmentEx2U2.status, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT);

        // Ex 2 U 1
        Assignment assignmentEx2U1 = postAssignment(experimentEx2, user1);
        Assert.assertNull(assignmentEx2U1.assignment);
        Assert.assertEquals(assignmentEx2U1.status, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT);

        // Ex 1 U 2
        Assignment assignmentEx1U2 = postAssignment(experimentEx1, user2);
        Assert.assertNull(assignmentEx1U2.assignment);
        Assert.assertEquals(assignmentEx1U2.status, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT);
    }

    /**
     * Tries to assign a new user to previously mutual-exclusive assignments.
     */
    @Test(dependsOnMethods = {"t_deleteMutualExclusion"})
    public void t_assignToNotMutexAnymore() {
        User user = UserFactory.createUser();
        Assignment assignmentEx2U1 = postAssignment(experimentEx2, user);
        Assert.assertNotNull(assignmentEx2U1.assignment);
        Assert.assertTrue(Arrays.asList(Constants.ASSIGNMENT_NEW_ASSIGNMENT, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT).contains(assignmentEx2U1.status));
        Assignment assignmentEx1U2 = postAssignment(experimentEx1, user);
        Assert.assertNotNull(assignmentEx1U2.assignment);
        Assert.assertTrue(Arrays.asList(Constants.ASSIGNMENT_NEW_ASSIGNMENT, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT).contains(assignmentEx1U2.status));
    }

    /**
     * Prepares experiments for batch assignments.
     */
    @Test(dependsOnGroups = {"ping"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void t_prepBatchAssignmentMutEx() {
        batchMutExExperiments.clear();
        for (int i = 0; i < 5; ++i) {
            Experiment exp = ExperimentFactory.createExperiment();
            Experiment created = postExperiment(exp);
            assertEqualModelItems(created, exp.setState(Constants.EXPERIMENT_STATE_DRAFT),
                    new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson", "description", "rule"));
            exp.update(created);
            batchMutExExperiments.add(exp);
        }
        postExclusions(batchMutExExperiments);
    }

    /**
     * Starts the prepared experiments.
     */
    @Test(dependsOnMethods = {"t_prepBatchAssignmentMutEx"})
    public void t_startBatchExperiments() {
        for (Experiment exp : batchMutExExperiments) {
            postBuckets(BucketFactory.createBuckets(exp, 1));
            Experiment update = putExperiment(exp.setState(Constants.EXPERIMENT_STATE_RUNNING));
            assertEqualModelItems(update, exp, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));
        }
    }

    /**
     * Does batch assignments.
     */
    @Test(dependsOnMethods = {"t_startBatchExperiments"})
    public void t_batchAssignments() {
        List<Assignment> assignments = postAssignments(ApplicationFactory.defaultApplication(), UserFactory.createUser(), batchMutExExperiments);
        Assert.assertEquals(assignments.size(), 5, "Exactly 5 assignments should be returned for mutual exclusive experiments.");
        int nonNullAssignments = 0;
        for (Assignment assignment : assignments) {
            nonNullAssignments += assignment.assignment != null ? 1 : 0;
        }
        Assert.assertEquals(nonNullAssignments, 1, "Exactly one assignment should not be null.");
    }

    /**
     * Removes one mutex.
     */
    @Test(dependsOnMethods = {"t_batchAssignments"})
    public void t_removeOneBatchMutEx() {
        deleteExclusion(batchMutExExperiments.get(0), batchMutExExperiments.get(1));
        clearAssignmentsMetadataCache();
    }

    /**
     * Retries batch assignments with a new user.
     */
    @Test(dependsOnMethods = {"t_removeOneBatchMutEx"})
    public void t_batchAssignmentsAfterDeletion() {
        List<Assignment> assignments = postAssignments(ApplicationFactory.defaultApplication(), UserFactory.createUser(), batchMutExExperiments);
        Assert.assertEquals(assignments.size(), 5, "Five assignments should be returned for mutual exclusive experiments.");
        int nonNullAssignments = 0;
        for (Assignment assignment : assignments) {
            nonNullAssignments += assignment.assignment != null ? 1 : 0;
        }
        Assert.assertEquals(nonNullAssignments, 2, "Exactly two assignment should not be null.");
    }

    /**
     * Creates an experiment in the default and one in another app.
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_createAdditionalApp() {
        experimentDefApp = ExperimentFactory.createExperiment();
        experimentOtherApp = ExperimentFactory.createExperiment().setApplication(ApplicationFactory.createApplication());
        Experiment created = postExperiment(experimentDefApp);
        assertEqualModelItems(created, experimentDefApp.setState(Constants.EXPERIMENT_STATE_DRAFT), new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson", "description", "rule"));
        experimentDefApp.update(created);
        toCleanUp.add(experimentDefApp);
        created = postExperiment(experimentOtherApp);
        assertEqualModelItems(created, experimentOtherApp.setState(Constants.EXPERIMENT_STATE_DRAFT), new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson", "description", "rule"));
        experimentOtherApp.update(created);
        toCleanUp.add(experimentOtherApp);
    }

    /**
     * Tries several invalid requests.
     */
    @Test(dependsOnMethods = {"t_createAdditionalApp", "t_removeOneBatchMutEx"})
    public void t_invalidRequests() {
        // check TERMINATED experiment
        putExperiment(batchMutExExperiments.get(0).setState(Constants.EXPERIMENT_STATE_TERMINATED));

        postExclusions(batchMutExExperiments.get(1), Collections.singletonList(batchMutExExperiments.get(0)), HttpStatus.SC_CREATED);
        Assert.assertEquals(response.jsonPath().getString("exclusions[0].status"), "FAILED", "Status should be FAILED.");
        Assert.assertTrue(response.jsonPath().getString("exclusions[0].reason").contains("is in TERMINATED or DELETED state"), "Reason not valid.");

        // check DELETED experiment
        deleteExperiment(batchMutExExperiments.get(0));
        postExclusions(batchMutExExperiments.get(0), Collections.singletonList(batchMutExExperiments.get(1)), HttpStatus.SC_NOT_FOUND);
        postExclusions(batchMutExExperiments.get(1), Collections.singletonList(batchMutExExperiments.get(0)), HttpStatus.SC_CREATED);
        Assert.assertEquals(response.jsonPath().getString("exclusions[0].status"), "FAILED", "Status should be FAILED.");
        Assert.assertTrue(response.jsonPath().getString("exclusions[0].reason").contains(" not found."), "Reason not valid.");

        // invalid ID
        Experiment invalidExperiment = ExperimentFactory.createExperiment().setId("7b471e3a-fae7-428c-90d1-02c3f6a0b441");
        // check invalid ID
        postExclusions(invalidExperiment, batchMutExExperiments.subList(1, batchMutExExperiments.size()), HttpStatus.SC_NOT_FOUND);

        // check deletion with invalid ID left and right
        deleteExclusion(batchMutExExperiments.get(1), invalidExperiment, HttpStatus.SC_NOT_FOUND);
        deleteExclusion(invalidExperiment, batchMutExExperiments.get(1), HttpStatus.SC_NOT_FOUND);

        // check GET for invalid ID
        getExperiment(invalidExperiment, HttpStatus.SC_NOT_FOUND);

        // passed End time
        Experiment endTimePassedExperiment = postExperiment(
                ExperimentFactory.createExperiment()
                        .setEndTime(TestUtils.relativeTimeString(-3))
                        .setStartTime(TestUtils.relativeTimeString(-4)));
        toCleanUp.add(endTimePassedExperiment);
        postBuckets(BucketFactory.createBuckets(endTimePassedExperiment, 2));
        putExperiment(endTimePassedExperiment.setState(Constants.EXPERIMENT_STATE_RUNNING));
        postExclusions(endTimePassedExperiment, Collections.singletonList(experimentDefApp), HttpStatus.SC_NOT_FOUND);

        // different applications
        postExclusions(experimentDefApp, Collections.singletonList(experimentOtherApp), HttpStatus.SC_CREATED);
        Assert.assertEquals(response.jsonPath().getString("exclusions[0].status"), "FAILED", "Status should be FAILED.");
        Assert.assertTrue(response.jsonPath().getString("exclusions[0].reason").contains("Mutual exclusion rules can only be defined for experiments within the same application."), "Reason not valid.");

    }


    /**
     * Cleans up.
     */
    @AfterClass
    public void cleanup() {
        toCleanUp.addAll(batchMutExExperiments);
    }

}
