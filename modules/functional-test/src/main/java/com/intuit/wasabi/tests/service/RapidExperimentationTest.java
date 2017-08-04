package com.intuit.wasabi.tests.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.Page;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;

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
public class RapidExperimentationTest extends TestBase {
    /**
     * This test class contains functional tests that check the rapid experimentation functionality
     * 
     * Created on 05/23/2017
     */
    private static final int RAPID_EXP_MAX_USERS = 5; // max cap on number of users
    private static final int NUMBER_OF_EXPERIMENTS_PER_BATCH = 5;
    private static final int NUMBER_OF_RAPID_EXPERIMENTS_IN_BATCH = 2; // number of rapid experiments in a batch that
                                                                       // has combination of normal and rapid
                                                                       // experiments

    private static final int NUMBER_OF_BUCKETS_PER_EXPERIMENT = 3; // number of buckets per experiment
    private static final String BATCH_PAGE = "rapidexperiment_page";
    private static final String PARTIAL_BATCH_PAGE = "rapidexperiment_partialpage";
    private static final String rapidExperimentTestApplication = "rapidExperimentApplication_functionaltest";
    Experiment rapidExperiment1 = null;
    Experiment experimentChangedToRapidExperiment = null;
    Experiment rapidExperiment2 = null;
    Page page = null;
    List<Experiment> experimentList = new ArrayList<Experiment>();
    List<Experiment> batchExperiments = new ArrayList<Experiment>();
    List<Experiment> experimentsListOfRapidApplication = new ArrayList<Experiment>();

    Application application = null;

    @BeforeClass
    public void setUp() {

        // lets terminate all the experiments that are created by the previous run of test
        application = new Application(rapidExperimentTestApplication);
        experimentsListOfRapidApplication = getApplicationExperiments(application);
        pauseTerminateAndDeleteExperiments(experimentsListOfRapidApplication);
        Assert.assertEquals(getApplicationExperiments(application).size(), 0);
        
    }

    /**
     * This test case covers a happy path test where we have a rapid experiment and we want to assert the behavior of
     * the experiment once the user cap is reached...the experiment should change to paused state
     */
    @Test
    public void testRapidExperiment() {

        Experiment exp = ExperimentFactory
                .createRapidExperiment(RAPID_EXP_MAX_USERS, "_" + UUID.randomUUID().toString(), -1)
                .setApplication(application);

        exp = createBucketsAndRunExperiment(exp, NUMBER_OF_BUCKETS_PER_EXPERIMENT);

        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            // lets do an assignment for a user with context set to PROD
            Assignment assignment = postAssignment(exp, new User("user" + i), "PROD");

            // lets assert the response code and also the state of the experiment
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
        }

        // since the experiment crossed the maxusers new assignments should return status as EXPERIMENT_PAUSED
        Assignment assignment = postAssignment(exp, new User("user10"), "PROD");
        Assert.assertEquals(assignment.status, "EXPERIMENT_PAUSED");

        // we should also assert that the state of the experiment should change to EXPERIMENT_PAUSED
        Assert.assertEquals(getExperiment(exp).state, Constants.EXPERIMENT_STATE_PAUSED);

    }

    /**
     * This test case covers a scenario where we start an experiment, do assignments and after that update the
     * experiment to rapid experiment with max users less than the number of users already assigned and assert the
     * behavior of the service
     */
    @Test
    public void testChangeRegularExperimentToRapidExperiment() {

        // lets create a normal experiment first
        Experiment exp = ExperimentFactory.createExperiment("_3_" + UUID.randomUUID().toString(), -1)
                .setApplication(application);
        exp = createBucketsAndRunExperiment(exp, NUMBER_OF_BUCKETS_PER_EXPERIMENT);

        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            // lets do an assignment for a user with context set to PROD
            Assignment assignment = postAssignment(exp, new User("user" + i), "PROD");

            // lets assert the response code and also the state of the experiment
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
        }

        // lets change the experiment now to rapid experiment and set the user cap less than number of assignments
        exp = exp.setIsRapidExperiment(true).setUserCap(3);
        exp = putExperiment(exp);
        // since the experiment crossed the maxusers the experiment should change to PUASED and response for assignment
        // should be "EXPERIMENT_PAUSED"
        Assignment assignment = postAssignment(exp, new User("user10"), "PROD");
        Assert.assertEquals(assignment.status, "EXPERIMENT_PAUSED");
        exp = getExperiment(exp);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);

    }

    /**
     * This test case tests for the put assignment API call that forces assignment call on particular bucket for a rapid
     * experiment
     */
    @Test
    public void testPutAssignmentToRapidExperiment() {

        Experiment exp = ExperimentFactory
                .createRapidExperiment(RAPID_EXP_MAX_USERS, "_" + UUID.randomUUID().toString(), -1)
                .setApplication(application);
        exp = createBucketsAndRunExperiment(exp, NUMBER_OF_BUCKETS_PER_EXPERIMENT);

        List<Bucket> bucketList = getBuckets(exp);
        Assignment assignment = AssignmentFactory.createAssignment().setAssignment(bucketList.get(0).label)
                .setExperimentLabel(exp.label).setOverwrite(true);

        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            // lets do an assignment for a user
            assignment = putAssignment(exp, assignment, new User("user" + i));
            // lets assert the status and response code and also the state of the experiment
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
        }

        // since the experiment crossed the maxusers the experiment should change to PUASED but it will still allow
        // assignments to buckets
        assignment = putAssignment(exp, assignment, new User("user6"), "PROD");
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
        Assert.assertEquals(getExperiment(exp).state, Constants.EXPERIMENT_STATE_PAUSED);

    }

    /**
     * This test case covers a scenario where we do batch assignment for a group of experiments that is a combination of
     * rapid experiments and normal experiments. It validates the behavior when we do bulk assignment of a user to that
     * batch
     */
    @Test
    public void testAssignmentForBatchWithFewRapidExperiments() {

        Page partialpage = new Page(PARTIAL_BATCH_PAGE, true);
        // lets create experiments for the batch
        for (int i = 1; i <= NUMBER_OF_EXPERIMENTS_PER_BATCH; i++) {
            Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -1)
                    .setApplication(application);

            // lets make first two experiments in the batch as rapid experiments
            if (i <= NUMBER_OF_RAPID_EXPERIMENTS_IN_BATCH) {
                exp.setIsRapidExperiment(true).setUserCap(RAPID_EXP_MAX_USERS);
            }

            exp = createBucketsAndRunExperiment(exp, NUMBER_OF_BUCKETS_PER_EXPERIMENT);
            batchExperiments.add(exp);

        }
        assignPageToExperimentsList(batchExperiments, partialpage);

        // the assignment will happen until the max users cap is reached for all experiments in batch
        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            List<Assignment> assignments = postAssignments(application, partialpage, new User("user" + i));
            assertAssignmentResponseStatus(assignments, "NEW_ASSIGNMENT");
        }

        // if we do one more assignment the rapid experiments should change to PAUSED/STOPPED state and the remaining
        // should be in running state
        List<Assignment> assignments = postAssignments(application, partialpage, new User("user1000"));

        // assert the status of the assignments, we should have two assignment status as EXPERIMENT_PAUSED
        assertAssignmentResponseStatus(assignments, "EXPERIMENT_PAUSED", 2);

        // assert the status of experiments
        for (Experiment experiment : batchExperiments) {
            Experiment exp = getExperiment(experiment);
            if (exp.isRapidExperiment)
                Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
            else
                Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
        }

    }

    /**
     * This test case covers a scenario where we do batch assignment for a group of experiments that has all rapid
     * experiments. It validates the behavior when we do bulk assignment of a user to that batch
     */
    @Test
    public void testAssignmentForBatchWithAllRapidExperiments() {

        page = new Page(BATCH_PAGE, true);
        List<Experiment> experimentList = new ArrayList<Experiment>();
        // lets create rapid experiments for the batch
        for (int i = 1; i <= NUMBER_OF_EXPERIMENTS_PER_BATCH; i++) {

            Experiment exp = ExperimentFactory
                    .createRapidExperiment(RAPID_EXP_MAX_USERS, "_" + UUID.randomUUID().toString(), -1)
                    .setApplication(application);

            exp = createBucketsAndRunExperiment(exp, NUMBER_OF_BUCKETS_PER_EXPERIMENT);
            experimentList.add(exp);
        }

        assignPageToExperimentsList(experimentList, page);

        // the assignment will happen until the max users cap is reached for all experiments in batch
        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            List<Assignment> assignments = postAssignments(application, page, new User("user" + (i + 1000)));
            assertAssignmentResponseStatus(assignments, "NEW_ASSIGNMENT");
        }

        // if we do one more assignment the rapid experiments should change to PAUSED/STOPPED state
        List<Assignment> assignments = postAssignments(application, page, new User("user100"));
        for (Experiment experiment : experimentList) {
            Experiment exp = getExperiment(experiment);
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
        }

        // assert that the response of the post assignments are EXPERIMENT_PAUSED
        assertAssignmentResponseStatus(assignments, "EXPERIMENT_PAUSED");

    }

    /**
     * This util method posts an experiment, creates buckets to the experiment and changes state of the experiment to
     * RUNNING.
     * 
     * @param exp - the experiment under consideration
     * @param numberOfBuckets - the number of buckets we need to create for experiment
     * @return Experiment - the running experiment
     */
    private Experiment createBucketsAndRunExperiment(Experiment exp, int numberOfBuckets) {

        exp = postExperiment(exp);
        createBucketsToExperiment(exp, numberOfBuckets);
        exp.state = Constants.EXPERIMENT_STATE_RUNNING;
        exp = putExperiment(exp);
        return exp;
    }

    /**
     * This method assigns a page to list of experiments
     *
     * @param experimentList - the list of experiments
     * @param page - the page to which we want to add the list of experiments to
     */
    private void assignPageToExperimentsList(List<Experiment> experimentList, Page page) {
        for (Experiment experiment : experimentList) {
            postPages(experiment, page, HttpStatus.SC_CREATED);
        }
    }

    /**
     * This method asserts that in a given list of assignments we have exactly the specified number of assignments with
     * given responseStatus
     * 
     * @param assignmentList - the list of assignments
     * @param responseStatus - the assignment status against which we want to assert
     * @param numberOfAssignments - the number of assignments that should have the state specifed
     */
    private void assertAssignmentResponseStatus(List<Assignment> assignmentList, String responseStatus,
            int numberOfAssignments) {
        int counter = 0;
        for (Assignment assignment : assignmentList) {
            if (assignment.status.equalsIgnoreCase(responseStatus))
                counter++;
        }
        Assert.assertEquals(counter, numberOfAssignments);
    }

    /**
     * This method asserts the status of the response we received from a post assignment
     * 
     * @param assignmentList - the list of assignments
     * @param assignmentStatus - the assignment status against which we want to assert
     */
    private void assertAssignmentResponseStatus(List<Assignment> assignmentList, String responseStatus) {
        for (Assignment assignment : assignmentList) {
            Assert.assertEquals(assignment.status, responseStatus);
        }
    }

    /**
     * This method pauses the experiment, terminates the experiments and eventually deletes them
     *
     * @param experimentsList - the list of experiments
     */
    private void pauseTerminateAndDeleteExperiments(List<Experiment> experimentsList) {

        for (Experiment exp : experimentsList) {

            // pause the experiment
            exp.state = Constants.EXPERIMENT_STATE_PAUSED;
            putExperiment(exp);

            // terminate the experiment
            exp.state = Constants.EXPERIMENT_STATE_TERMINATED;
            putExperiment(exp);

            // delete the experiment
            deleteExperiment(exp);
        }

    }

}
