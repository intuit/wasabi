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
 * Created on 03/01/2017 This test class tests scenario where the wasabi application considers batch-assignment calls
 * with different contexts as two different assignments
 */
public class BatchAssignmentContextTest extends TestBase {

    Application batchAssignmentContextTestApp = null;

    List<Experiment> testExperimentsList; // this holds experiments that are created in the test class to be deleted
                                          // eventually

    int numberOfExperimentsPerBatch = 4; // number of experiments that are associated with a batch (page) call

    @BeforeClass
    public void setUp() {
        // lets create application and assign it to the variable
        batchAssignmentContextTestApp = ApplicationFactory.createApplication().setName("batchAssignmentContextTestApp");

        // initialize the experiment list variable that holds list of all the experiments
        testExperimentsList = new ArrayList<Experiment>();

    }

    @Test
    public void createAssignmentForExperimentStartedInPast() {

        List<Experiment> expList = new ArrayList<Experiment>();

        for (int i = 1; i <= numberOfExperimentsPerBatch; i++) {
            Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -1)
                    .setApplication(batchAssignmentContextTestApp);
            exp = postExperiment(exp);

            // create buckets for the experiment
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 5);
            postBuckets(bucketList);

            // below we are starting the experiment
            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            putExperiment(exp);

            testExperimentsList.add(exp);
            expList.add(exp);
        }

        // do a POST of assignments for user1234 with context set as PROD
        List<Assignment> assignmentsList = postAssignments(batchAssignmentContextTestApp, new User("user1234"), expList,
                "PROD");

        // lets make sure that the response has assignments equal to the number of experiments
        Assert.assertEquals(assignmentsList.size(), numberOfExperimentsPerBatch);

        // assert that all of the assignments are new for the user
        for (Assignment assignment : assignmentsList) {
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
        }

        // do a POST of assignments for user1234 with context set as DEV and this should all be NEW ASSIGNMENT as it is
        // a different context
        assignmentsList = postAssignments(batchAssignmentContextTestApp, new User("user1234"), expList, "DEV");

        // lets make sure that the response has assignments equal to the number of experiments
        Assert.assertEquals(assignmentsList.size(), numberOfExperimentsPerBatch);

        // assert that all of the assignments are new for the user
        for (Assignment assignment : assignmentsList) {
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
        }

    }

    @Test
    public void createAssignmentForExperimentStartAndEndInPast() {

        List<Experiment> expList = new ArrayList<Experiment>();

        for (int i = 1; i <= numberOfExperimentsPerBatch; i++) {
            Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -5, -1)
                    .setApplication(batchAssignmentContextTestApp);
            exp = postExperiment(exp);

            // create buckets for the experiment
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 5);
            postBuckets(bucketList);

            // below we are starting the experiment
            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            putExperiment(exp);

            expList.add(exp);
            testExperimentsList.add(exp);
        }

        // do a POST of assignments for user5678 with context set as PROD
        List<Assignment> assignmentsList = postAssignments(batchAssignmentContextTestApp, new User("user5678"), expList,
                "PROD");

        // lets make sure that the response has assignments equal to the number of experiments
        Assert.assertEquals(assignmentsList.size(), numberOfExperimentsPerBatch);

        // assert that all of the assignments are new for the user
        for (Assignment assignment : assignmentsList) {
            Assert.assertEquals(assignment.status, "EXPERIMENT_EXPIRED");
        }

    }

    @Test
    public void createAssignmentForExperimentStartAndEndInFuture() {

        List<Experiment> expList = new ArrayList<Experiment>();

        for (int i = 1; i <= numberOfExperimentsPerBatch; i++) {
            Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), 1, 5)
                    .setApplication(batchAssignmentContextTestApp);
            exp = postExperiment(exp);

            // create buckets for the experiment
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 5);
            postBuckets(bucketList);

            // below we are starting the experiment
            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            putExperiment(exp);

            expList.add(exp);
            testExperimentsList.add(exp);
        }

        // do a POST of assignments for user5678 with context set as PROD
        List<Assignment> assignmentsList = postAssignments(batchAssignmentContextTestApp, new User("user5678"), expList,
                "PROD");

        // lets make sure that the response has assignments equal to the number of experiments
        Assert.assertEquals(assignmentsList.size(), numberOfExperimentsPerBatch);

        // assert that all of the assignments are new for the user
        for (Assignment assignment : assignmentsList) {
            Assert.assertEquals(assignment.status, "EXPERIMENT_NOT_STARTED");
        }

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
