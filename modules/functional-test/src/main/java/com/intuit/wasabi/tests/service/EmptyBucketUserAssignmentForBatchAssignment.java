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
package com.intuit.wasabi.tests.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Created on 1st march 2017 This test class tests scenario where a user is assigned to a batch of experiments and the
 * assigned buckets are deleted to verify that when the assignment call is made to the same user, then the user should
 * be assigned to one of the available buckets in each of the experiments... to keep things simple creating two
 * experiments with two buckets for each experiment
 *
 */
public class EmptyBucketUserAssignmentForBatchAssignment extends TestBase {

    Application batchAssignmentEmptyBucketTestApp = null; // app under which the test experiments will be created

    List<Experiment> testExperimentsList = null; // holds experiments that are created in the test class to be deleted
                                                 // eventually in the tearDown

    List<String> assignedBucketLabels = null; // holds the labels of the buckets to which the test user has been
                                              // assigned

    @BeforeClass
    public void setUp() {
        // lets create application and assign it to the variable
        batchAssignmentEmptyBucketTestApp = ApplicationFactory.createApplication()
                .setName("batchAssignmentEmptyBucketTestApp");

        // initialize the declared reference variable

        testExperimentsList = new ArrayList<Experiment>();

    }

    /**
     * This test case tests scenario where we do batch assignment of a user to list of experiments, delete the assigned
     * buckets and do assignment again for the same user against the same experiments should make a new assignment
     */
    @Test
    public void createBatchAssignmentAndEmptyeAssignedBucketTest() {

        List<Experiment> expList = new ArrayList<Experiment>();
        assignedBucketLabels = new ArrayList<String>();
        for (int i = 1; i <= 2; i++) {
            Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -1)
                    .setApplication(batchAssignmentEmptyBucketTestApp);
            exp = postExperiment(exp);

            // create buckets for the experiment
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 2);
            postBuckets(bucketList);

            // below we are starting the experiment
            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            putExperiment(exp);

            testExperimentsList.add(exp);
            expList.add(exp);
        }

        // do a POST of assignments for user1234 with context set as PROD
        List<Assignment> assignmentsList = postAssignments(batchAssignmentEmptyBucketTestApp, new User("user1234"),
                expList, "PROD");

        // lets make sure that the response has assignments equal to the number of experiments
        Assert.assertEquals(assignmentsList.size(), 2);

        // assert that all of the assignments are new for the user
        for (Assignment assignment : assignmentsList) {
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            assignedBucketLabels.add(assignment.assignment); // add the assignment bucket to the list
        }

        // lets delete the assigned buckets to the user
        List<Bucket> emptyBucket = new ArrayList<>();
        Bucket assignedBucketOfExperiment1 = new Bucket(assignedBucketLabels.get(0), expList.get(0), 50);
        Bucket assignedBucketOfExperiment2 = new Bucket(assignedBucketLabels.get(1), expList.get(1), 50);
        emptyBucket.add(assignedBucketOfExperiment1);
        emptyBucket.add(assignedBucketOfExperiment2);
        putBucketsState(emptyBucket, Constants.BUCKET_STATE_EMPTY);

        // do a POST of assignments for same user with same context- This should be new Assignment as previous
        // assignment got deleted
        assignmentsList = postAssignments(batchAssignmentEmptyBucketTestApp, new User("user1234"), expList, "PROD");

        // lets make sure that the response has assignments equal to the number of experiments
        Assert.assertEquals(assignmentsList.size(), 2);

        // assert that all of the assignments are new for the user
        for (Assignment assignment : assignmentsList) {
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
        }

    }

    /**
     * This test case tests scenario where we do batch assignment of a user to list of experiments, delete all the
     * buckets and when we do assignment again for the same user against the same experiments should get no open bucket
     */
    @Test
    public void createBatchAssignmentAndEmptyAllBucketTest() {

        List<Experiment> expList = new ArrayList<Experiment>();
        assignedBucketLabels = new ArrayList<String>();

        Map<Integer, List<Bucket>> experimentBucketsMap = new HashMap<Integer, List<Bucket>>(); // map holds all the
                                                                                                // buckets within an
                                                                                                // experiment

        for (int i = 1; i <= 2; i++) {
            Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -1)
                    .setApplication(batchAssignmentEmptyBucketTestApp);
            exp = postExperiment(exp);

            // create buckets for the experiment
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 2);
            postBuckets(bucketList);
            experimentBucketsMap.put(i, bucketList);

            // below we are starting the experiment
            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            putExperiment(exp);

            testExperimentsList.add(exp);
            expList.add(exp);
        }

        // do a POST of assignments for user1234 with context set as PROD
        List<Assignment> assignmentsList = postAssignments(batchAssignmentEmptyBucketTestApp, new User("user1234"),
                expList, "PROD");

        // lets make sure that the response has assignments equal to the number of experiments
        Assert.assertEquals(assignmentsList.size(), 2);

        // assert that all of the assignments are new for the user
        for (Assignment assignment : assignmentsList) {
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            assignedBucketLabels.add(assignment.assignment); // add the assignment bucket to the list
        }

        // lets delete all the buckets across all the experimetns
        putBucketsState(experimentBucketsMap.get(1), Constants.BUCKET_STATE_EMPTY);
        putBucketsState(experimentBucketsMap.get(2), Constants.BUCKET_STATE_EMPTY);

        // do a POST of assignments for same user with same context- This should be new Assignment as previous
        // assignment got deleted
        assignmentsList = postAssignments(batchAssignmentEmptyBucketTestApp, new User("user1234"), expList, "PROD");

        // lets make sure that the response has assignments equal to the number of experiments
        Assert.assertEquals(assignmentsList.size(), 2);

        // assert that awe get NO_OPEN_BUCKETS as we deleted all the buckets across all experiments
        for (Assignment assignment : assignmentsList) {
            Assert.assertEquals(assignment.status, "NO_OPEN_BUCKETS");
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
