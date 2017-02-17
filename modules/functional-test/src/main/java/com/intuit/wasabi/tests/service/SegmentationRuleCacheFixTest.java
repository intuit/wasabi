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
import com.intuit.wasabi.tests.library.util.ModelUtil;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameInclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;

/**
 * Testing the fix for the Segmentation Rule Cache problem.
 * <p>
 * Issue was when a segmentation rule for a running experiment was updated the update would not take.
 * Root cause was that the old rule was cached.
 * Fix was to reset the cache, but that would have to be done once for each node running behind the load balancer.
 * The first assignment coming in after the rule update would still use the old rule, but then update to the new rule.
 * Therefore the tests expect to see a NO_PROFILE_MATCH with the new rule once for each node.
 */
public class SegmentationRuleCacheFixTest extends TestBase {

    private int nodeCount;
    public final int repeatCount = 30;
    private Experiment experiment;
    private User user;
    private List<User> userList;
    private List<User> userList2;
    private String ruleFirst;
    private String ruleSecond;
    private String ruleNonHeader;
    private SerializationStrategy experimentSerializationStrategy;

    private int newAssignments = 0;
    private int noProfileMatches = 0;
    private int existingAssignments = 0;

    /**
     * Initializes the test.
     *
     * @throws NumberFormatException if node.count was malformed.
     */
    @BeforeClass
    private void init() throws NumberFormatException {
        nodeCount = Integer.valueOf(appProperties.getProperty("node-count"));
        experiment = ExperimentFactory.createExperiment();
        user = UserFactory.createUser();
        userList = new ArrayList<>();
        userList2 = new ArrayList<>();
        ruleFirst = "User-Agent = \"Agent001\"";
        ruleSecond = "User-Agent = \"Agent002\"";
        ruleNonHeader = "subscriber = true";
        experimentSerializationStrategy =
                new DefaultNameExclusionStrategy("creationTime", "modificationTime", "id", "ruleJson", "rule", "description");
    }

    /**
     * Asserts that the repeat count is big enough. Otherwise no need to test, but to increase the count.
     */
    @Test(dependsOnGroups = {"ping"})
    public void assertRepeatCount() {
        Assert.assertTrue(2 * nodeCount < repeatCount, "Repeat count not big enough.");
    }

    /**
     * Creates an experiment with only one control bucket and the first rule.
     * After that the experiment is started.
     */
    @Test(dependsOnGroups = {"ping"})
    public void createExperiment() {
        experiment.setRule(ruleFirst);
        Experiment created = postExperiment(experiment);
        experiment.setState(Constants.EXPERIMENT_STATE_DRAFT);
        assertEqualModelItems(created, experiment, experimentSerializationStrategy);
        experiment.update(created);

        Bucket bucket = BucketFactory.createBucket(experiment, true).setAllocationPercent(1);
        Bucket createdBucket = postBucket(bucket);
        assertEqualModelItems(createdBucket, bucket, new DefaultNameExclusionStrategy("state"));
        bucket.update(createdBucket);

        experiment.setState(Constants.EXPERIMENT_STATE_RUNNING);
        created = putExperiment(experiment);
        assertEqualModelItems(created, experiment, experimentSerializationStrategy);
        experiment.update(created);
    }

    /**
     * Retrieves a running experiment.
     */
    @Test(dependsOnMethods = {"createExperiment"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 500)
    public void retrieveRunningExperiment() {
        experiment = getExperiment(experiment);
        Assert.assertEquals(experiment.state, Constants.EXPERIMENT_STATE_RUNNING);
    }

    /**
     * Provides assignment data.
     *
     * @return assignment data
     */
    @DataProvider
    public Object[][] assignmentDataProvider() {
        return new Object[][]{
                new Object[]{user, "NoMatchForThisAgentValue", Constants.ASSIGNMENT_NO_PROFILE_MATCH},
                new Object[]{user, "Agent001", Constants.ASSIGNMENT_NEW_ASSIGNMENT},
                new Object[]{user, "Agent001", Constants.ASSIGNMENT_EXISTING_ASSIGNMENT},
        };
    }

    /**
     * Tries to get an assignment, but fails as the rule fails.
     *
     * @param user           the user
     * @param userAgent      the user agent
     * @param expectedStatus the expected status
     */
    @Test(dependsOnMethods = {"retrieveRunningExperiment"}, dataProvider = "assignmentDataProvider")
    public void assignUser(User user, String userAgent, String expectedStatus) {
        APIServerConnector asc = apiServerConnector.clone();
        asc.putHeaderMapKVP("User-Agent", userAgent);
        Assignment assignment = getAssignment(experiment, user, null, true, false, HttpStatus.SC_OK, asc);
        Assert.assertEquals(assignment.status, expectedStatus, "Status does not match.");
    }

    /**
     * Sets the second segmentation rule
     */
    @Test(dependsOnMethods = {"assignUser"})
    public void setNewRule() {
        experiment.setRule(ruleSecond);
        Experiment modified = putExperiment(experiment);
        assertEqualModelItems(modified, experiment, experimentSerializationStrategy);
        experiment.update(modified);
    }

    /**
     * Verifies the rule.
     */
    @Test(dependsOnMethods = {"setNewRule"})
    public void verifyRule() {
        Experiment updated = getExperiment(experiment);
        assertEqualModelItems(updated, experiment, new DefaultNameInclusionStrategy("rule", "id"));
    }

    /**
     * The main test: Assigns many users with a match for the new rule, counts the matches.
     */
    @Test(dependsOnMethods = {"verifyRule"}, invocationCount = repeatCount)
    public void assignToMatchRuleTwo() {
        APIServerConnector asc = apiServerConnector.clone();
        asc.putHeaderMapKVP("User-Agent", "Agent002");
        User user = UserFactory.createUser();
        userList.add(user);
        Assignment assignment = getAssignment(experiment, user, null, true, false, HttpStatus.SC_OK, asc);
        switch (assignment.status) {
            case Constants.ASSIGNMENT_NEW_ASSIGNMENT:
                newAssignments++;
                break;
            case Constants.ASSIGNMENT_NO_PROFILE_MATCH:
                noProfileMatches++;
                break;
            default:
                Assert.fail("Expected " + Constants.ASSIGNMENT_NO_PROFILE_MATCH + " or " + Constants.ASSIGNMENT_NEW_ASSIGNMENT);
        }
    }

    /**
     * Verify the results of {@link #assignToMatchRuleTwo()}.
     */
    @Test(dependsOnMethods = {"assignToMatchRuleTwo"})
    public void verifyResultsOfAssignments() {
        Assert.assertTrue(noProfileMatches <= nodeCount, "Expecting less no-profile matches than nodes.");
        Assert.assertEquals(newAssignments + noProfileMatches, repeatCount, "Assignments should give "
                + Constants.ASSIGNMENT_NO_PROFILE_MATCH + " or " + Constants.ASSIGNMENT_NEW_ASSIGNMENT); // not needed?
    }

    /**
     * Retrieves the assignments and checks if the count is as expected.
     */
    @Test(dependsOnMethods = {"verifyResultsOfAssignments"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 5, warmup = 2500)
    public void getAssignmentsAfterVerification() {
        List<Assignment> assignments = getAssignments(experiment);
        // +1 for user 0
        Assert.assertEquals(assignments.size(), newAssignments + 1, "Number of assignments does not match.");
    }

    /**
     * Posts impressions for the users.
     */
    @Test(dependsOnMethods = {"getAssignmentsAfterVerification"})
    public void postUserImpressions() {
        for (User user : userList) {
            postEvent(EventFactory.createImpression(), experiment, user, HttpStatus.SC_CREATED);
        }
    }

    /**
     * Sets the non header rule and resets the counters.
     */
    @Test(dependsOnMethods = {"postUserImpressions"})
    public void setNewRuleThree() {
        experiment.setRule(ruleNonHeader);
        Experiment modified = putExperiment(experiment);
        assertEqualModelItems(modified, experiment, experimentSerializationStrategy);
        experiment.update(modified);
        existingAssignments = 0;
        newAssignments = 0;
        noProfileMatches = 0;
    }

    /**
     * Verifies the non header rule.
     */
    @Test(dependsOnMethods = {"setNewRuleThree"})
    public void verifyNonHeaderRule() {
        Experiment created = getExperiment(experiment);
        assertEqualModelItems(created, experiment, experimentSerializationStrategy);
    }

    /**
     * Assigns a user without a User-Agent (GET). Should result in NO_PROFILE_MATCH.
     */
    @Test(dependsOnMethods = {"verifyNonHeaderRule"})
    public void assignUsersToBuckets_no_key_get() {
        Assignment assignment = getAssignment(experiment, UserFactory.createUser(user.userID + "_no_key"));
        Assert.assertEquals(assignment.status, Constants.ASSIGNMENT_NO_PROFILE_MATCH);
    }

    /**
     * Assigns a user without a User-Agent (POST). Should result in NO_PROFILE_MATCH.
     */
    @Test(dependsOnMethods = {"assignUsersToBuckets_no_key_get"})
    public void assignUsersToBuckets_no_key_post() {
        Assignment assignment = postAssignment(experiment, UserFactory.createUser(user.userID + "_no_key"));
        Assert.assertEquals(assignment.status, Constants.ASSIGNMENT_NO_PROFILE_MATCH);
    }

    /**
     * Assigns a user with a profile which does not match.
     */
    @Test(dependsOnMethods = {"assignUsersToBuckets_no_key_post"})
    public void assignUsersToBuckets_key_post_no_match() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("subscriber", false);
        Assignment assignment = postAssignment(experiment, UserFactory.createUser(user.userID + "_no_key"), null, true, false, profile);
        Assert.assertEquals(assignment.status, Constants.ASSIGNMENT_NO_PROFILE_MATCH);
    }

    /**
     * Assigns a user with a profile which does not match.
     */
    @Test(dependsOnMethods = {"assignUsersToBuckets_key_post_no_match"}, invocationCount = repeatCount)
    public void assignUsersToBuckets_key_post_match() {
        User user = UserFactory.createUser();
        userList2.add(user);
        Map<String, Object> profile = new HashMap<>();
        profile.put("subscriber", true);
        Assignment assignment = postAssignment(experiment, user, null, true, false, profile);
        switch (assignment.status) {
            case Constants.ASSIGNMENT_NEW_ASSIGNMENT:
                newAssignments++;
                break;
            case Constants.ASSIGNMENT_NO_PROFILE_MATCH:
                noProfileMatches++;
                break;
            default:
                Assert.fail("Expected " + Constants.ASSIGNMENT_NO_PROFILE_MATCH + " or " + Constants.ASSIGNMENT_NEW_ASSIGNMENT);
        }
    }

    /**
     * Verifies the results from {@link #assignUsersToBuckets_key_post_match()}. Resets the counters.
     */
    @Test(dependsOnMethods = {"assignUsersToBuckets_key_post_match"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 5, warmup = 2500)
    public void verifyResultsOfNoHeaderAssignments() {
        List<Assignment> assignments = getAssignments(experiment);
        final List<String> userIDs = new ArrayList<>();
        for (User user : userList2) {
            userIDs.add(user.userID);
        }
        ModelUtil.Filter<Assignment> filter = new ModelUtil.Filter<Assignment>() {
            @Override
            public boolean filter(Assignment collectionItem) {
                return userIDs.contains(collectionItem.user_id);
            }
        };
        assignments = new ModelUtil<Assignment>().filterList(assignments, filter);
        Assert.assertEquals(assignments.size(), newAssignments, "Number of assignments does not match.");
        existingAssignments = 0;
        newAssignments = 0;
        noProfileMatches = 0;
    }

    /**
     * Assign existing users to get existing assignments.
     */
    @Test(dependsOnMethods = {"verifyResultsOfNoHeaderAssignments"})
    public void assignExistingUsers() {
        for (User user : userList2) {
            Assignment assignment = getAssignment(experiment, user);
            switch (assignment.status) {
                case Constants.ASSIGNMENT_EXISTING_ASSIGNMENT:
                    existingAssignments++;
                    break;
                case Constants.ASSIGNMENT_NEW_ASSIGNMENT:
                    newAssignments++;
                    break;
                case Constants.ASSIGNMENT_NO_PROFILE_MATCH:
                    noProfileMatches++;
                    break;
                default:
                    Assert.fail("Status " + assignment.status + " not expected.");
            }
        }
    }

    /**
     * Verifies the results from {@link #assignExistingUsers()}. Resets the counters.
     */
    @Test(dependsOnMethods = {"assignExistingUsers"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 5, warmup = 2500)
    public void verifyResultsOfRepeatedAssignments() {
        List<Assignment> assignments = getAssignments(experiment);
        final List<String> userIDs = new ArrayList<>();
        for (User user : userList2) {
            userIDs.add(user.userID);
        }
        ModelUtil.Filter<Assignment> filter = new ModelUtil.Filter<Assignment>() {
            @Override
            public boolean filter(Assignment collectionItem) {
                return userIDs.contains(collectionItem.user_id);
            }
        };
        assignments = new ModelUtil<Assignment>().filterList(assignments, filter);
        Assert.assertEquals(assignments.size(), newAssignments + existingAssignments, "Number of assignments does not match.");
        existingAssignments = 0;
        newAssignments = 0;
        noProfileMatches = 0;
    }

    /**
     * Removes the segmentation rules.
     */
    @Test(dependsOnMethods = {"verifyResultsOfRepeatedAssignments"})
    public void removeSegRule() {
        experiment.rule = "";
        Experiment modified = putExperiment(experiment);
        assertEqualModelItems(modified, experiment, experimentSerializationStrategy);
        experiment.update(modified);
    }

    /**
     * Clean up.
     */
    @Test(dependsOnMethods = {"removeSegRule"})
    public void cleanUp() {
        experiment.setState(Constants.EXPERIMENT_STATE_PAUSED);
        putExperiment(experiment);
        experiment.setState(Constants.EXPERIMENT_STATE_TERMINATED);
        putExperiment(experiment);
        deleteExperiment(experiment);
    }

}
