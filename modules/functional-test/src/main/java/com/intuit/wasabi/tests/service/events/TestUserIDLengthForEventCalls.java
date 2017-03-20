/*******************************************************************************
 * Copyright 2017 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.tests.service.events;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.analytics.ExperimentCounts;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;

/**
 * 
 * created on 03/15/2017 this test class tests the new feature where the userId length in mysql tables has increased
 * from 48 to 200 characters
 *
 */
public class TestUserIDLengthForEventCalls extends TestBase {

    private User testUserID48Characters = UserFactory.createUser(StringUtils.repeat("a", 48)); // previous cap on
                                                                                               // userlength
    private User testUserID49Characters = UserFactory.createUser(StringUtils.repeat("a", 49)); // border case for
                                                                                               // previous cap on
                                                                                               // userlength
    private User testUserID200Characters = UserFactory.createUser(StringUtils.repeat("a", 200)); // new cap on
                                                                                                 // userlength
    private User testUserID201Characters = UserFactory.createUser(StringUtils.repeat("a", 201));// border case on new
                                                                                                // userlength
    private Application testApplication = null;
    Event eventImpression = new Event("IMPRESSION");
    Event eventAction = new Event("ACTION");
    private Experiment experiment = null;

    @BeforeClass
    public void setUp() {

        testApplication = ApplicationFactory.createApplication().setName("testApplication");

        // create experiment
        experiment = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -1)
                .setApplication(testApplication);
        experiment = postExperiment(experiment);

        // create buckets for the experiment (created one as it doesn't matter for the test)
        List<Bucket> bucketList = BucketFactory.createBuckets(experiment, 1);
        postBuckets(bucketList);

        // below we are starting the experiment
        experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
        putExperiment(experiment);

        // lets do some pre-assignments and impressions so that we have some values before we run the test
        postAssignment(experiment, new User("user1"));
        postAssignment(experiment, new User("user2"));
        postAssignment(experiment, new User("user3"));
        postAssignment(experiment, new User("user4"));

        clearAssignmentsMetadataCache();
        postEvent(eventImpression, experiment, new User("user1"), HttpStatus.SC_CREATED);
        postEvent(eventImpression, experiment, new User("user2"), HttpStatus.SC_CREATED);
        postEvent(eventImpression, experiment, new User("user3"), HttpStatus.SC_CREATED);
        postEvent(eventImpression, experiment, new User("user4"), HttpStatus.SC_CREATED);
        clearAssignmentsMetadataCache();
        postEvent(eventAction, experiment, new User("user1"), HttpStatus.SC_CREATED);
        postEvent(eventAction, experiment, new User("user2"), HttpStatus.SC_CREATED);
        postEvent(eventAction, experiment, new User("user3"), HttpStatus.SC_CREATED);
        postEvent(eventAction, experiment, new User("user4"), HttpStatus.SC_CREATED);
    }

    @Test
    public void test48characterUserID() {

        // first we need to create an assignment for the user before we make an event call
        Assignment assignment = postAssignment(experiment, testUserID48Characters);
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

        // before we post impression && actions lets get counts of those events
        clearAssignmentsMetadataCache();
        ExperimentCounts experimentCounts = getExperimentCounts(experiment);
        int preImpressionCounts = experimentCounts.impressionCounts.eventCount;
        int preActionCounts = experimentCounts.jointActionCounts.eventCount;

        // lets create an impression and this should increase the event count by one
        clearAssignmentsMetadataCache();
        postEvent(eventImpression, experiment, testUserID48Characters, HttpStatus.SC_CREATED);
        experimentCounts = getExperimentCounts(experiment);
        int postImpressionCounts = experimentCounts.impressionCounts.eventCount;

        // lets assert that the event impression has been created
        Assert.assertEquals(postImpressionCounts - preImpressionCounts, 1);

        // lets create an action and this should increase the event count of action by one
        clearAssignmentsMetadataCache();
        postEvent(eventAction, experiment, testUserID48Characters, HttpStatus.SC_CREATED);
        experimentCounts = getExperimentCounts(experiment);
        int postActionCounts = experimentCounts.jointActionCounts.eventCount;

        // lets assert that the event action has been created
        Assert.assertEquals(postActionCounts - preActionCounts, 1);

    }

    @Test
    public void test49characterUserID() {

        // first we need to create an assignment for the user before we make an event call
        Assignment assignment = postAssignment(experiment, testUserID49Characters);
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

        // before we post event(impression) lets get counts of impression
        clearAssignmentsMetadataCache();
        ExperimentCounts experimentCounts = getExperimentCounts(experiment);
        int preImpressionCounts = experimentCounts.impressionCounts.eventCount;
        int preActionCounts = experimentCounts.jointActionCounts.eventCount;

        // lets create an impression and this should increase the event count by one
        clearAssignmentsMetadataCache();
        postEvent(eventImpression, experiment, testUserID49Characters, HttpStatus.SC_CREATED);
        experimentCounts = getExperimentCounts(experiment);
        clearAssignmentsMetadataCache();
        int postImpressionCounts = experimentCounts.impressionCounts.eventCount;

        // lets assert that the event impression has been created
        Assert.assertEquals(postImpressionCounts - preImpressionCounts, 1);

        // lets create an action and this should increase the event count of action by one
        clearAssignmentsMetadataCache();
        postEvent(eventAction, experiment, testUserID49Characters, HttpStatus.SC_CREATED);
        experimentCounts = getExperimentCounts(experiment);
        int postActionCounts = experimentCounts.jointActionCounts.eventCount;

        // lets assert that the event action has been created
        Assert.assertEquals(postActionCounts - preActionCounts, 1);
    }

    @Test
    public void test200characterUserID() {

        // first we need to create an assignment for the user before we make an event call
        Assignment assignment = postAssignment(experiment, testUserID200Characters);
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

        // before we post event(impression) lets get counts of impression
        clearAssignmentsMetadataCache();
        ExperimentCounts experimentCounts = getExperimentCounts(experiment);
        int preImpressionCounts = experimentCounts.impressionCounts.eventCount;
        int preActionCounts = experimentCounts.jointActionCounts.eventCount;

        // lets create an impression and this should increase the event count by one
        clearAssignmentsMetadataCache();
        postEvent(eventImpression, experiment, testUserID200Characters, HttpStatus.SC_CREATED);
        experimentCounts = getExperimentCounts(experiment);
        clearAssignmentsMetadataCache();
        int postImpressionCounts = experimentCounts.impressionCounts.eventCount;

        // lets assert that the event impression has been created
        Assert.assertEquals(postImpressionCounts - preImpressionCounts, 1);

        // lets create an action and this should increase the event count of action by one
        clearAssignmentsMetadataCache();
        postEvent(eventAction, experiment, testUserID200Characters, HttpStatus.SC_CREATED);
        experimentCounts = getExperimentCounts(experiment);
        int postActionCounts = experimentCounts.jointActionCounts.eventCount;

        // lets assert that the event action has been created
        Assert.assertEquals(postActionCounts - preActionCounts, 1);
    }

    /**
     * This test is to test for negative case scenario as the allowed user length is 200
     */
    @Test
    public void test201characterUserID() {

        // first we need to create an assignment for the user before we make an event call
        Assignment assignment = postAssignment(experiment, testUserID201Characters);
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

        // before we post event(impression) lets get counts of impression
        clearAssignmentsMetadataCache();
        ExperimentCounts experimentCounts = getExperimentCounts(experiment);
        int preImpressionCounts = experimentCounts.impressionCounts.eventCount;
        int preActionCounts = experimentCounts.jointActionCounts.eventCount;

        // lets create an impression and this should increase the event count by one
        clearAssignmentsMetadataCache();
        postEvent(eventImpression, experiment, testUserID201Characters, HttpStatus.SC_CREATED);
        experimentCounts = getExperimentCounts(experiment);
        clearAssignmentsMetadataCache();
        int postImpressionCounts = experimentCounts.impressionCounts.eventCount;

        // assert that the event impression should not be created as userId is more than 200 characters
        Assert.assertEquals(postImpressionCounts - preImpressionCounts, 0);

        // create an action and this should not increase the event count of action by one as userId is more than 200
        clearAssignmentsMetadataCache();
        postEvent(eventAction, experiment, testUserID201Characters, HttpStatus.SC_CREATED);
        experimentCounts = getExperimentCounts(experiment);
        int postActionCounts = experimentCounts.jointActionCounts.eventCount;

        // lets assert that the event action has not been created
        Assert.assertEquals(postActionCounts - preActionCounts, 0);
    }

    @AfterClass
    public void tearDown() {
        experiment.state = Constants.EXPERIMENT_STATE_PAUSED;
        putExperiment(experiment);

        // delete the experiment
        experiment.state = Constants.EXPERIMENT_STATE_TERMINATED;
        putExperiment(experiment);

        // lets dereference the experiment and call the Garbage collector Daemon
        experiment = null;
        System.gc();
    }
}
