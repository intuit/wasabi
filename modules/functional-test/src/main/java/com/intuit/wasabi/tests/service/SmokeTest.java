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
import com.intuit.wasabi.tests.library.util.ModelUtil;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameInclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.Page;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.analytics.BucketStatistics;
import com.intuit.wasabi.tests.model.analytics.Counts;
import com.intuit.wasabi.tests.model.analytics.DailyStatistics;
import com.intuit.wasabi.tests.model.analytics.ExperimentBasicStatistics;
import com.intuit.wasabi.tests.model.analytics.ExperimentCumulativeStatistics;
import com.intuit.wasabi.tests.model.analytics.ExperimentStatistics;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;
import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItemsNoOrder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A basic smoke test to check fundamental functionality of the app.
 * This test is basically a sanity check if the app is usable at all.
 */
public class SmokeTest extends TestBase {

    private static final Logger LOGGER = getLogger(SmokeTest.class);
    private Experiment experiment;
    private List<Bucket> buckets = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private User specialUser = UserFactory.createUser("Special");
    private Map<User, Assignment> assignments = new HashMap<>();
    private Map<User, Integer> userImpressions = new HashMap<>();
    private List<Page> pages;
    private int impressionsFactorPerUser = 5;
    private Experiment mutualExclusiveExperiment;
    private DefaultNameExclusionStrategy experimentComparisonStrategy =
            new DefaultNameExclusionStrategy("id", "creationTime",
                    "modificationTime", "ruleJson", "hypothesisIsCorrect", "results");

    /**
     * Initializes a default experiment.
     */
    public SmokeTest() {
        setResponseLogLengthLimit(1000);

        experiment = ExperimentFactory.createExperiment();
        mutualExclusiveExperiment = ExperimentFactory.createExperiment();

        for (int i = 1; i <= 4; ++i) {
            users.add(UserFactory.createUser());
        }
        users.add(specialUser);
    }

    /**
     * POSTs the experiment to the server and updates it with the returned values.
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_createExperiments() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);

        Experiment expExcl = postExperiment(mutualExclusiveExperiment);
        Assert.assertNotNull(expExcl.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(expExcl.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(expExcl.state, "Experiment creation failed (No state).");
        mutualExclusiveExperiment.update(expExcl);
    }

    /**
     * GETs the experiment from the server and asserts the fetched matches the updated old one.
     */
    @Test(dependsOnMethods = {"t_createExperiments"})
    public void t_retrieveExperiment() {
        Experiment fetchedExperiment = getExperiment(experiment);
        assertEqualModelItems(fetchedExperiment, experiment, experimentComparisonStrategy);
        experiment.update(fetchedExperiment);
    }

    /**
     * Creates three buckets for the experiment and POSTs them to the server.
     */
    @Test(dependsOnMethods = {"t_retrieveExperiment"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(warmup = 2000, maxTries = 3)
    public void t_createThreeBuckets() {
        buckets = BucketFactory.createBuckets(experiment, 3);
        postBuckets(buckets);
    }

    /**
     * Deletes the last bucket again.
     */
    @Test(dependsOnMethods = {"t_createThreeBuckets"})
    public void t_deleteLastBucket() {
        deleteBucket(buckets.get(buckets.size() - 1));
    }

    /**
     * Creates a new bucket to replace the old one.
     */
    @Test(dependsOnMethods = {"t_deleteLastBucket"})
    public void t_createNewBucket() {
        buckets.set(buckets.size() - 1,
                BucketFactory.createBucket(experiment)
                        .setAllocationPercent(1.0d / buckets.size()));
        postBucket(buckets.get(buckets.size() - 1));
    }

    /**
     * Get a list of all buckets and compare them to the list in storage.
     * Will be retried with a 2 second timeout.
     */
    @Test(dependsOnMethods = {"t_createNewBucket"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void t_retrieveBuckets() {
        List<Bucket> bucketList = getBuckets(experiment);
        assertEqualModelItemsNoOrder(bucketList, buckets, new DefaultNameExclusionStrategy("state"));
    }

    /**
     * Sets the experiment to RUNNING.
     */
    @Test(dependsOnMethods = {"t_createNewBucket"})
    public void t_startExperiment() {
        experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
        Experiment exp = putExperiment(experiment);
        assertEqualModelItems(exp, experiment, experimentComparisonStrategy);
        experiment.update(exp);
    }

    /**
     * Retrieves the running experiment.
     */
    @Test(dependsOnMethods = {"t_startExperiment"})
    public void t_retrieveRunningExperiment() {
        Experiment exp = getExperiment(experiment);
        assertEqualModelItems(exp, experiment, experimentComparisonStrategy);
        experiment.update(exp);
    }

    /**
     * Assigns users to buckets.
     */
    @Test(dependsOnMethods = {"t_retrieveRunningExperiment"})
    @RetryTest(maxTries = 3, warmup = 1500)
    public void t_assignUsersToBuckets() {
        for (User user : users) {
            Assignment assignment = getAssignment(experiment, user);
            assignments.put(user, assignment);
        }
        for (Assignment assignment : assignments.values()) {
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT", "Assignment status wrong.");
            Assert.assertTrue(assignment.cache, "Assignment.cache not true.");
        }
    }

    /**
     * Force a change in an assignment.
     */
    @Test(dependsOnMethods = {"t_assignUsersToBuckets"})
    public void t_changeSpecialUsersAssignment() {
        Assignment specialUserAssignment = assignments.get(specialUser);

        // get "next" bucket to assign the special user to
        int oldBucket = -1;
        do {
            ++oldBucket;
        } while (!buckets.get(oldBucket).label.equals(specialUserAssignment.assignment));

        specialUserAssignment.assignment = buckets.get((oldBucket + 1) % buckets.size()).label;
        Assignment newAssignment = putAssignment(experiment, specialUserAssignment, specialUser);
        assertEqualModelItems(newAssignment, specialUserAssignment);
    }

    /**
     * Check if changing assignment was successful.
     */
    @Test(dependsOnMethods = {"t_changeSpecialUsersAssignment"})
    public void t_getUserAssignments() {
        Map<User, Assignment> userAssignments = new HashMap<>();
        for (User user : users) {
            Assignment assignment = getAssignment(experiment, user);
            userAssignments.put(user, assignment);
        }

        for (Map.Entry<User, Assignment> entry : assignments.entrySet()) {
            entry.getValue().status = "EXISTING_ASSIGNMENT";
        }

        assertEqualModelItems(userAssignments, assignments);
    }

    /**
     * Gets assignments for multiple experiments. Since we have just one experiment, this should not change the outcome.
     */
    @Test(dependsOnMethods = {"t_changeSpecialUsersAssignment"})
    public void t_getUserAssignmentsAcrossExperiments() {
        // since users are just assigned to one experiment, the assignment should not change as of now
        Assignment specialUserAssignment = assignments.get(specialUser);
        // still we need to add our expected experiment to our expected assignment
        specialUserAssignment.experimentLabel = experiment.label;
        List<Assignment> actualAssignments = postAssignments(ApplicationFactory.defaultApplication(), specialUser, Collections.singletonList(experiment));
        // context was not set, and cache can change, so we exclude those from our check
        assertEqualModelItems(actualAssignments.get(0), specialUserAssignment, new DefaultNameExclusionStrategy("context", "cache"));
        Assert.assertEquals(actualAssignments.size(), 1, "Number of returned assignments does not match.");
    }

    /**
     * Sets the user assignments to fixed buckets for easier testing later.
     */
    @Test(dependsOnMethods = {"t_changeSpecialUsersAssignment"})
    public void t_setUserAssignments() {
        // sets the users to specific buckets so it's easy to track the results
        assignments.clear();
        for (int i = 0; i < users.size(); ++i) {
            Assignment assignment = AssignmentFactory.createAssignment()
                    .setAssignment(buckets.get(i % buckets.size()).label)
                    .setExperimentLabel(experiment.label)
                    .setOverwrite(true);
            Assignment putAssignment = putAssignment(experiment, assignment, users.get(i));
            assertEqualModelItems(putAssignment, assignment, new DefaultNameInclusionStrategy("assignment"));
            assignments.put(users.get(i), putAssignment);
        }
    }

    /**
     * Submits (userIndex + 1) * 5 IMPRESSIONs and one special event for the special user.
     */
    @Test(dependsOnMethods = {"t_setUserAssignments"})
    public void t_submitEvents() {
        for (int i = 0; i < users.size(); ++i) {
            Event[] events = new Event[impressionsFactorPerUser * (i + 1)];
            Arrays.fill(events, EventFactory.createEvent());
            postEvents(Arrays.asList(events), experiment, users.get(i), HttpStatus.SC_CREATED);

            // store the number of impressions for easier calculations later
            userImpressions.put(users.get(i), impressionsFactorPerUser * (i + 1));
        }
        postEvent(EventFactory.createAction(), experiment, specialUser, HttpStatus.SC_CREATED);
        postEvent(EventFactory.createAction().setName("Action2"), experiment, users.get(1), HttpStatus.SC_CREATED);
    }

    /**
     * Retrieve all events and count them.
     */
    @Test(dependsOnMethods = {"t_submitEvents"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void t_retrieveEvents() {
        int expectedEventCount = 2; // 2 actions + for all users (userIndex + 1) * 5 IMPRESSIONs
        for (int i = 1; i <= users.size(); ++i) {
            expectedEventCount += impressionsFactorPerUser * i;
        }
        LOGGER.info("Expecting " + expectedEventCount + " events.");
        List<Event> events = postEvents(experiment);
        Assert.assertEquals(events.size(), expectedEventCount, "Received impressions do not match the number of expected impressions.");
        Event action = EventFactory.createAction();
        Event action2 = EventFactory.createAction().setName("Action2");
        int actionCount = 0;
        for (Event event : events) {
            if (event.name.equals(action.name)) {
                assertEqualModelItems(event, action, new DefaultNameExclusionStrategy("timestamp", "payload", "userId"));
                actionCount++;
                continue;
            }
            if (event.name.equals(action2.name)) {
                assertEqualModelItems(event, action2, new DefaultNameExclusionStrategy("timestamp", "payload", "userId"));
                actionCount++;
            }
        }
        Assert.assertEquals(actionCount, 2, "Action count does not fit.");
    }

    /**
     * Asserts the daily summaries. The same as t_dailySummaryOfDerivedStatistics, but for another endpoint.
     */
    @Test(dependsOnMethods = {"t_submitEvents"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2500)
    public void t_dailySummary() {
        ExperimentCumulativeStatistics dailies = getDailyStatistics(experiment);

        List<DailyStatistics> filteredStatistics =
                new ModelUtil<DailyStatistics>().filterList(dailies.days,
                        new ModelUtil.Filter<DailyStatistics>() {
                            @Override
                            public boolean filter(DailyStatistics daily) {
                                return daily.perDay.impressionCounts.eventCount > 0;
                            }
                        });

        Assert.assertEquals(filteredStatistics.size(), 1, "Only one day should have more than 0 impressions.");

        ExperimentBasicStatistics perDay = filteredStatistics.get(0).perDay;

        int expectedImpressionCount = 0; // 2 actions + for all users (userIndex + 1) * 5 IMPRESSIONs
        for (int i = 1; i <= users.size(); ++i) {
            expectedImpressionCount += impressionsFactorPerUser * i;
        }

        // check impression counts
        Counts expectedCounts = new Counts();
        expectedCounts.eventCount = expectedImpressionCount;
        expectedCounts.uniqueUserCount = users.size();
        assertEqualModelItems(perDay.impressionCounts, expectedCounts);

        // check joint action counts
        expectedCounts.eventCount = 2;
        expectedCounts.uniqueUserCount = 2;
        assertEqualModelItems(perDay.jointActionCounts, expectedCounts);

        // check individual action counts
        expectedCounts.eventCount = 1;
        expectedCounts.uniqueUserCount = 1;
        assertEqualModelItems(perDay.actionCounts.get(EventFactory.createAction().name), expectedCounts);
        assertEqualModelItems(perDay.actionCounts.get("Action2"), expectedCounts);

        // check bucket statistics
        for (Map.Entry<String, BucketStatistics> bucket : perDay.buckets.entrySet()) {
            expectedCounts.eventCount = 0;
            expectedCounts.uniqueUserCount = 0;
            for (Map.Entry<User, Assignment> assignment : assignments.entrySet()) {
                if (assignment.getValue().assignment.equals(bucket.getKey())) {
                    expectedCounts.eventCount += userImpressions.get(assignment.getKey());
                    expectedCounts.uniqueUserCount += 1;
                }
            }
            assertEqualModelItems(bucket.getValue().impressionCounts, expectedCounts);
        }

        // check if actions are in the correct buckets
        Assert.assertEquals(perDay.buckets.get(assignments.get(specialUser).assignment).actionCounts.get(EventFactory.createAction().name).eventCount, 1);
        Assert.assertEquals(perDay.buckets.get(assignments.get(users.get(1)).assignment).actionCounts.get("Action2").eventCount, 1);
        for (Map.Entry<String, BucketStatistics> bucket : perDay.buckets.entrySet()) {
            if (bucket.getKey().equals(assignments.get(specialUser).assignment) || bucket.getKey().equals(assignments.get(users.get(1)).assignment)) {
                continue;
            }
            for (Map.Entry<String, Counts> entry : bucket.getValue().actionCounts.entrySet()) {
                Assert.assertEquals(entry.getValue().eventCount, 0);
                Assert.assertEquals(entry.getValue().uniqueUserCount, 0);
            }
        }

    }

    /**
     * Checks if there are sufficient data (which shouldn't be there).
     */
    @Test(dependsOnMethods = {"t_submitEvents"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void t_summaryWithDerivedStatistics() {
        ExperimentStatistics statistics = getStatistics(experiment);
        Assert.assertFalse(statistics.experimentProgress.hasSufficientData);
    }

    /**
     * Asserts the daily summaries. The same as t_dailySummary, but for another endpoint.
     */
    @Test(dependsOnMethods = {"t_submitEvents"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void t_dailySummaryOfDerivedStatistics() {
        ExperimentCumulativeStatistics dailies = getDailyStatistics(experiment);

        List<DailyStatistics> filteredStatistics =
                new ModelUtil<DailyStatistics>().filterList(dailies.days,
                        new ModelUtil.Filter<DailyStatistics>() {
                            @Override
                            public boolean filter(DailyStatistics daily) {
                                return daily.perDay.impressionCounts.eventCount > 0;
                            }
                        });

        Assert.assertEquals(filteredStatistics.size(), 1, "Only one day should have more than 0 impressions.");

        String date = filteredStatistics.get(0).date;
        Assert.assertEquals(date, experiment.startTime.substring(0, 10));

        ExperimentBasicStatistics perDay = filteredStatistics.get(0).perDay;

        int expectedImpressionCount = 0; // 2 actions + for all users (userIndex + 1) * 5 IMPRESSIONs
        for (int i = 1; i <= users.size(); ++i) {
            expectedImpressionCount += impressionsFactorPerUser * i;
        }

        // check impression counts
        Counts expectedCounts = new Counts();
        expectedCounts.eventCount = expectedImpressionCount;
        expectedCounts.uniqueUserCount = users.size();
        assertEqualModelItems(perDay.impressionCounts, expectedCounts);

        // check joint action counts
        expectedCounts.eventCount = 2;
        expectedCounts.uniqueUserCount = 2;
        assertEqualModelItems(perDay.jointActionCounts, expectedCounts);

        // check individual action counts
        expectedCounts.eventCount = 1;
        expectedCounts.uniqueUserCount = 1;
        assertEqualModelItems(perDay.actionCounts.get(EventFactory.createAction().name), expectedCounts);
        assertEqualModelItems(perDay.actionCounts.get("Action2"), expectedCounts);

        // check bucket statistics
        for (Map.Entry<String, BucketStatistics> bucket : perDay.buckets.entrySet()) {
            expectedCounts.eventCount = 0;
            expectedCounts.uniqueUserCount = 0;
            for (Map.Entry<User, Assignment> assignment : assignments.entrySet()) {
                if (assignment.getValue().assignment.equals(bucket.getKey())) {
                    expectedCounts.eventCount += userImpressions.get(assignment.getKey());
                    expectedCounts.uniqueUserCount += 1;
                }
            }
            assertEqualModelItems(bucket.getValue().impressionCounts, expectedCounts);
        }

        // check if actions are in the correct buckets
        Assert.assertEquals(perDay.buckets.get(assignments.get(specialUser).assignment).actionCounts.get(EventFactory.createAction().name).eventCount, 1);
        Assert.assertEquals(perDay.buckets.get(assignments.get(users.get(1)).assignment).actionCounts.get("Action2").eventCount, 1);
        for (Map.Entry<String, BucketStatistics> bucket : perDay.buckets.entrySet()) {
            if (bucket.getKey().equals(assignments.get(specialUser).assignment) || bucket.getKey().equals(assignments.get(users.get(1)).assignment)) {
                continue;
            }
            for (Map.Entry<String, Counts> entry : bucket.getValue().actionCounts.entrySet()) {
                Assert.assertEquals(entry.getValue().eventCount, 0);
                Assert.assertEquals(entry.getValue().uniqueUserCount, 0);
            }
        }
    }

    /**
     * closes the buckets
     */
    @Test(dependsOnMethods = {"t_dailySummary", "t_retrieveEvents", "t_summaryWithDerivedStatistics", "t_dailySummaryOfDerivedStatistics"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(warmup = 2000, maxTries = 3)
    public void t_closeBuckets() {
        for (Bucket bucket : buckets) {
            bucket.setState(Constants.BUCKET_STATE_CLOSED).setAllocationPercent(0);
        }
        List<Bucket> closedBuckets = putBucketsState(buckets, Constants.BUCKET_STATE_CLOSED);
        assertEqualModelItems(closedBuckets, buckets);
    }

    /**
     * empties one bucket
     */
    @Test(dependsOnMethods = {"t_closeBuckets"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(warmup = 2000, maxTries = 3)
    public void t_emptyBucket() {
        buckets.get(0).setState(Constants.BUCKET_STATE_EMPTY);
        Bucket emptiedBucket = putBucketState(buckets.get(0), Constants.BUCKET_STATE_EMPTY);
        assertEqualModelItems(emptiedBucket, buckets.get(0));
    }

    /**
     * Create another experiment and make both experiments mutually exclusive.
     */
    @Test(dependsOnMethods = {"t_createExperiments"})
    public void t_excludeAnotherExperiment() {
        postExclusions(experiment, Collections.singletonList(mutualExclusiveExperiment));
    }

    /**
     * Retrieve the exclusions and check if it worked.
     */
    @Test(dependsOnMethods = {"t_excludeAnotherExperiment"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 500)
    public void t_checkExclusions() {
        List<Experiment> experiments = getExclusions(mutualExclusiveExperiment);
        Assert.assertEquals(experiments.size(), 1, "Number of mutual exclusions does not match.");
        experiments = getExclusions(experiment);
        Assert.assertEquals(experiments.size(), 1, "Number of mutual exclusions does not match.");
    }

    /**
     * Delete the mutual exclusions again.
     */
    @Test(dependsOnMethods = {"t_checkExclusions"})
    public void t_deleteExclusions() {
        deleteExclusion(experiment, mutualExclusiveExperiment);
    }

    /**
     * Retrieve the exclusions and check if it worked.
     */
    @Test(dependsOnMethods = {"t_deleteExclusions"})
    public void t_checkExclusionsAfterDeletion() {
        List<Experiment> experiments = getExclusions(mutualExclusiveExperiment);
        Assert.assertEquals(experiments.size(), 0, "Number of mutual exclusions does not match.");
        experiments = getExclusions(experiment);
        Assert.assertEquals(experiments.size(), 0, "Number of mutual exclusions does not match.");
    }

    /**
     * Set the priority for experiment.
     */
    @Test(dependsOnMethods = {"t_assignUsersToBuckets"})
    public void t_prioritizeExperiment() {
        postExperimentPriority(experiment, 0);
        List<Experiment> experiments = getApplicationPriorities(ApplicationFactory.createApplication().setName(experiment.applicationName));
        // experiment should be first, having highest priority now
        assertEqualModelItems(experiments.get(0), experiment, experimentComparisonStrategy);
    }

    /**
     * Retrieve assignments for a single experiment.
     */
    @Test(dependsOnMethods = {"t_assignUsersToBuckets"})
    public void t_retrieveExperimentAssignments() {
        List<Assignment> assignments = getAssignments(experiment);
        Assert.assertEquals(assignments.size(), users.size(), "Number of assignments does not match number of users.");
    }

    /**
     * Create a few pages and add them to the experiment.
     */
    @Test(dependsOnMethods = {"t_createExperiments"})
    public void t_submitPages() {
        pages = Arrays.asList(new Page("Page1", true), new Page("Page2", false));
        postPages(experiment, pages, HttpStatus.SC_CREATED);
    }

    /**
     * Get pages of the experiment.
     */
    @Test(dependsOnMethods = {"t_submitPages"})
    public void t_getPages() {
        List<Page> retrievedPages = getPages(experiment);
        assertEqualModelItemsNoOrder(retrievedPages, pages);
    }

    @Test(dependsOnMethods = {"t_getPages", "t_retrieveExperimentAssignments", "t_emptyBucket"})
    public void t_pauseExperiments() {
        Experiment terminated = putExperiment(experiment.setState(Constants.EXPERIMENT_STATE_PAUSED));
        assertEqualModelItems(terminated, experiment, experimentComparisonStrategy);
        terminated = putExperiment(mutualExclusiveExperiment.setState(Constants.EXPERIMENT_STATE_PAUSED));
        assertEqualModelItems(terminated, mutualExclusiveExperiment, experimentComparisonStrategy);
    }

    /**
     * Terminates the experiments.
     */
    @Test(dependsOnMethods = {"t_pauseExperiments"})
    public void t_terminateExperiments() {
        Experiment terminated = putExperiment(experiment.setState(Constants.EXPERIMENT_STATE_TERMINATED));
        assertEqualModelItems(terminated, experiment, experimentComparisonStrategy);
        terminated = putExperiment(mutualExclusiveExperiment.setState(Constants.EXPERIMENT_STATE_TERMINATED));
        assertEqualModelItems(terminated, mutualExclusiveExperiment, experimentComparisonStrategy);
    }

    /**
     * Deletes experiments.
     */
    @Test(dependsOnMethods = {"t_terminateExperiments"})
    public void t_deleteExperiments() {
        deleteExperiment(experiment);
        deleteExperiment(mutualExclusiveExperiment);
    }

    /**
     * Checks if the experiments are deleted.
     */
    @Test(dependsOnMethods = {"t_deleteExperiments"})
    public void t_checkDeletedExperiments() {
        clearAssignmentsMetadataCache();
        List<Experiment> experiments = getExperiments();
        Assert.assertFalse(experiments.contains(experiment), experiment + " not deleted.");
        Assert.assertFalse(experiments.contains(mutualExclusiveExperiment), mutualExclusiveExperiment + " not deleted.");
    }

    /**
     * recreates the standard experiment
     */
    @Test(dependsOnMethods = {"t_checkDeletedExperiments"})
    public void t_recreateExperiment() {
        experiment.setState(null).id = null;
        Experiment created = postExperiment(experiment);
        experiment.setState(Constants.EXPERIMENT_STATE_DRAFT);
        //experiment.getSerializationStrategy().add("id");
        assertEqualModelItems(created, experiment, experimentComparisonStrategy);
        //experiment.getSerializationStrategy().remove("id");
        experiment.update(created);
    }

    /**
     * Cleanup the recreated experiment.
     */
    @Test(dependsOnMethods = {"t_recreateExperiment"})
    public void t_cleanUp() {
        putExperiment(experiment.setState(Constants.EXPERIMENT_STATE_PAUSED));
        putExperiment(experiment.setState(Constants.EXPERIMENT_STATE_TERMINATED));
        putExperiment(experiment.setState(Constants.EXPERIMENT_STATE_DELETED), HttpStatus.SC_NO_CONTENT);
        deleteExperiment(experiment, HttpStatus.SC_NOT_FOUND);
        List<Experiment> experiments = getExperiments();
        Assert.assertFalse(experiments.contains(experiment), experiment + " not deleted.");
    }

}
