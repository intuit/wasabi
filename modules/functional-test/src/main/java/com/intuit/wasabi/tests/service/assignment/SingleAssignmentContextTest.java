/*******************************************************************************
 * Copyright 2016 Intuit
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
package com.intuit.wasabi.tests.service.assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;

/**
 * Created on 03/01/2017 This test class tests scenario where the wasabi application considers a single-assignment calls
 * with different contexts as two different assignments
 */
public class SingleAssignmentContextTest extends TestBase {

    Application singleAssignmentContextTestApp = null;
    List<Experiment> testExperimentsList;

    @BeforeClass
    public void setUp() {
        // lets create application and assign it to the variable
        singleAssignmentContextTestApp = ApplicationFactory.createApplication()
                .setName("singleAssignmentContextTestApp");

        // initialize the experiment list variable that holds list of all the experiments
        testExperimentsList = new ArrayList<Experiment>();

    }

    /**
     * This is the actual method that tests for SUT
     */
    @Test
    public void createAssignmentForExperimentStartedInPast() {

        // create experiment with start date as todaysdate -1 and assign it to the above created Application
        Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -1)
                .setApplication(singleAssignmentContextTestApp);
        exp = postExperiment(exp);

        // add it to the list of test Experiments that will eventually be deleted
        testExperimentsList.add(exp);

        // create buckets for the experiment
        List<Bucket> bucketList = BucketFactory.createBuckets(exp, 5);
        postBuckets(bucketList);

        // below we are starting the experiment
        exp.state = Constants.EXPERIMENT_STATE_RUNNING;
        putExperiment(exp);

        // lets do an assignment for a user with context set to PROD
        Assignment assignment = postAssignment(exp, new User("user123"), "PROD");

        // lets assert the status and response code
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

        // lets repeat the assignment for same user but with different context- context set to DEV
        assignment = postAssignment(exp, new User("user123"), "DEV");

        // lets assert the status and response code
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

        // lets do an assignment for a user with context set to PROD so that we will get back existing assignment
        assignment = postAssignment(exp, new User("user123"), "PROD");

        // lets assert the status and response code
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "EXISTING_ASSIGNMENT");
    }

    /**
     * This is the method that tests assignments for an experiment that has both start and end date in the past
     */
    @Test
    public void createAssignmentForExperimentStartedAndEndedInPast() {
        // create experiment with start date and end date in the past and assign it to the above created Application
        Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -5, -1)
                .setApplication(singleAssignmentContextTestApp);
        exp = postExperiment(exp);

        // add it to the list of test Experiments that will eventually be deleted
        testExperimentsList.add(exp);

        // create buckets for the experiment
        List<Bucket> bucketList = BucketFactory.createBuckets(exp, 5);
        postBuckets(bucketList);

        // below we are starting the experiment
        exp.state = Constants.EXPERIMENT_STATE_RUNNING;
        putExperiment(exp);

        // lets do an assignment for a user with context set to PROD
        Assignment assignment = postAssignment(exp, new User("user456"), "PROD");

        // lets assert the status and response code
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "EXPERIMENT_EXPIRED");

    }

    @Test
    public void createAssignmentForExperimentStartedAndEndedInFuture() {
        // create experiment with start date and end date in the future and assign it to the above created Application
        Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), 5, 10)
                .setApplication(singleAssignmentContextTestApp);
        exp = postExperiment(exp);

        // add it to the list of test Experiments that will eventually be deleted
        testExperimentsList.add(exp);

        // create buckets for the experiment
        List<Bucket> bucketList = BucketFactory.createBuckets(exp, 5);
        postBuckets(bucketList);

        // below we are starting the experiment
        exp.state = Constants.EXPERIMENT_STATE_RUNNING;
        putExperiment(exp);

        // lets do an assignment for a user with context set to PROD
        Assignment assignment = postAssignment(exp, new User("user456"), "PROD");

        // lets assert the status and response code
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "EXPERIMENT_NOT_STARTED");

    }

    /**
     * The below method tears down the test experiments that are created that we stored using the testExperimentsList
     */
    @AfterClass()
    public void tearDown() {
        for (Experiment exp : testExperimentsList) {
            exp.state = Constants.EXPERIMENT_STATE_PAUSED;
            putExperiment(exp);

            // delete the experiment
            exp.state = Constants.EXPERIMENT_STATE_TERMINATED;
            putExperiment(exp);
        }

        // lets dereference the list and call the Garbage collector Daemon
        testExperimentsList = null;
        System.gc();
    }

}
