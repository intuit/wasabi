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


import com.intuit.wasabi.tests.library.APIServerConnector;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameInclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;
import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItemsNoOrder;

/**
 * Tests segmentation based on HTTP Headers.
 */
public class SegmentOnHttpHeaderTest extends TestBase {

    private Experiment experiment;
    private User user;
    private User user2;
    private List<Bucket> buckets;
    private SerializationStrategy defExpStrategy =
            new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson", "id", "description", "rule");
    private String matchAgentValue = "MatchAgentValue";

    @BeforeTest
    public void init() {
        user = UserFactory.createUser();
        user2 = UserFactory.createUser();
    }

    /**
     * Creates an experiment that matches the User-Agent to be {@code MatchAgentValue}.
     */
    @Test(dependsOnGroups = {"ping"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void prepareExperiment() {
        experiment = ExperimentFactory.createExperiment().setRule("User-Agent = \"" + matchAgentValue + "\"");
        Experiment created = postExperiment(experiment);

        experiment.setState(Constants.EXPERIMENT_STATE_DRAFT);
        assertEqualModelItems(created, experiment, defExpStrategy);
        experiment.update(created);
    }

    @Test(dependsOnMethods = {"prepareExperiment"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void createBuckets() {
        buckets = BucketFactory.createBuckets(experiment, 4);
        postBuckets(buckets);
    }

    @Test(dependsOnMethods = {"createBuckets"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void startExperiment() {
        experiment.setState(Constants.EXPERIMENT_STATE_RUNNING);
        Experiment updated = putExperiment(experiment);
        assertEqualModelItems(updated, experiment, defExpStrategy);
        experiment.update(updated);
    }

    /**
     * Provides a valid user agent match and an invalid one and the expected results.
     * Format: {@code Object[][]{Object[]{User, String, String, boolean},...}}
     *
     * @return data for methods with (User, String, String, boolean) arguments.
     */
    @DataProvider
    public Object[][] dataProviderAssignments() {
        return new Object[][]{
                new Object[]{user, "No" + matchAgentValue, Constants.ASSIGNMENT_NO_PROFILE_MATCH, true},
                new Object[]{user, "No" + matchAgentValue, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT, false},
                new Object[]{user, matchAgentValue, Constants.ASSIGNMENT_NEW_ASSIGNMENT, true},
                new Object[]{user, "No" + matchAgentValue, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT, true},
                new Object[]{user, matchAgentValue, Constants.ASSIGNMENT_EXISTING_ASSIGNMENT, true},
                new Object[]{user2, "No" + matchAgentValue, Constants.ASSIGNMENT_NO_PROFILE_MATCH, true},
                new Object[]{user2, matchAgentValue, Constants.ASSIGNMENT_NEW_ASSIGNMENT, true},
        };
    }

    /**
     * Tries to assign users to buckets with the provided user agents.
     *
     * @param user           the user
     * @param userAgent      the user agent to use
     * @param expectedStatus the expected assignment status
     * @param shouldPass     the flag of if this is passed or not
     */
    @Test(dependsOnMethods = {"startExperiment"}, dataProvider = "dataProviderAssignments")
    @RetryTest(warmup = 2500)
    public void assignUsersToBucket(User user, String userAgent, String expectedStatus, boolean shouldPass) {
        APIServerConnector ascWithAgent = apiServerConnector.clone();
        ascWithAgent.putHeaderMapKVP("User-Agent", userAgent);

        Assignment expected = AssignmentFactory.createAssignment().setStatus(expectedStatus);
        Assignment assignment = getAssignment(experiment, user, null, true, false, HttpStatus.SC_OK, ascWithAgent);
        assertEqualModelItems(assignment, expected, new DefaultNameInclusionStrategy("status"), shouldPass);
    }

    /**
     * Provides different bucket states.
     *
     * @return bucket states
     */
    @DataProvider
    public Object[][] dataProviderBuckets() {
        return new Object[][]{
                new Object[]{Constants.BUCKET_STATE_CLOSED},
                new Object[]{Constants.BUCKET_STATE_EMPTY},
        };
    }

    /**
     * Needs bucket states. Sets the buckets to the supplied state and updates the server.
     *
     * @param state the state to set.
     */
    @Test(dependsOnMethods = {"assignUsersToBucket"}, dataProvider = "dataProviderBuckets")
    public void closeAndEmptyBuckets(String state) {
        List<Bucket> bucketList = putBucketsState(buckets, state);
        for (Bucket bucket : buckets) {
            bucket.setState(state).setAllocationPercent(0);
        }
        assertEqualModelItemsNoOrder(bucketList, buckets);
    }

    /**
     * Perform some cleanup: Pause and terminate the experiment, finally delete it.
     */
    @AfterClass
    public void cleanUp() {
        experiment.setState(Constants.EXPERIMENT_STATE_PAUSED);
        putExperiment(experiment);
        experiment.setState(Constants.EXPERIMENT_STATE_TERMINATED);
        putExperiment(experiment);
        deleteExperiment(experiment);
    }

}
