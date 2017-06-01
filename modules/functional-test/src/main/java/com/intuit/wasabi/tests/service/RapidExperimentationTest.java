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

    private static final String BATCH_PAGE = "rapidexperiment_page";
    private static final String PARTIAL_BATCH_PAGE = "rapidexperiment_partialpage";
    Experiment rapidExperiment1 = null;
    Experiment experimentChangedToRapidExperiment = null;
    Experiment rapidExperiment3 = null;
    Page page = null;
    List<Experiment> experimentList = new ArrayList<Experiment>();
    List<Experiment> batchExperiments = new ArrayList<Experiment>();
    List<Experiment> experimentsListOfRapidApplication = new ArrayList<Experiment>();

    Application application = null;

    @BeforeClass
    public void setUp() {

        application = new Application("rapidExperimentApplication");

        experimentsListOfRapidApplication = getApplicationExperiments(application);
        pauseTerminateAndDeleteExperiments(experimentsListOfRapidApplication);

        rapidExperiment1 = ExperimentFactory.createExperiment("_1" + UUID.randomUUID().toString(), -1)
                .setApplication(application).setIsRapidExperiment(true).setUserCap(RAPID_EXP_MAX_USERS);
        experimentChangedToRapidExperiment = ExperimentFactory.createExperiment("_2" + UUID.randomUUID().toString(), -1)
                .setApplication(application);

        rapidExperiment3 = ExperimentFactory.createExperiment("_3" + UUID.randomUUID().toString(), -1)
                .setApplication(application).setIsRapidExperiment(true).setUserCap(RAPID_EXP_MAX_USERS);

        rapidExperiment1 = postExperiment(rapidExperiment1);
        experimentChangedToRapidExperiment = postExperiment(experimentChangedToRapidExperiment);
        rapidExperiment3 = postExperiment(rapidExperiment3);
        experimentList.add(rapidExperiment1);
        experimentList.add(experimentChangedToRapidExperiment);
        experimentList.add(rapidExperiment3);
        for (Experiment exp: experimentList) {
            
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 3);
            postBuckets(bucketList);

            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            exp = putExperiment(exp);

        }

    }

    @Test
    public void testRapidExperiment() {

        Experiment exp = getExperiment(experimentList.get(0));
        for (int i = 1; i <= 5; i++) {
            // lets do an assignment for a user with context set to PROD
            Assignment assignment = postAssignment(exp, new User("user" + i), "PROD");

            // lets assert the status and response code and also the state of the experiment
            assertReturnCode(response, HttpStatus.SC_OK);
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
        }

        // since the experiment crossed the maxusers the experiment should change to PUASED
        Assignment assignment = postAssignment(exp, new User("user10"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "EXPERIMENT_PAUSED");
        Assert.assertEquals(getExperiment(exp).state, Constants.EXPERIMENT_STATE_PAUSED);    
        assignment = postAssignment(exp, new User("user11"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "EXPERIMENT_PAUSED");
        assignment = postAssignment(exp, new User("user12"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "EXPERIMENT_PAUSED");
        exp = getExperiment(exp);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
        experimentList.set(0, rapidExperiment1);
    }

    /**
     * This test case covers a scenario where we start an experiment, do assignments and after that update the
     * experiment to rapid experiment with max users less than the number of users already assigned
     */
    @Test
    public void testChangeRegularExperimentToRapidExperiment() {
        clearAssignmentsMetadataCache();
        Experiment exp = getExperiment(experimentList.get(1));

        for (int i = 1; i <= 5; i++) {
            // lets do an assignment for a user with context set to PROD
            Assignment assignment = postAssignment(exp, new User("user" + i), "PROD");

            // lets assert the status and response code and also the state of the experiment
            assertReturnCode(response, HttpStatus.SC_OK);
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
        }

        exp = exp.setIsRapidExperiment(true).setUserCap(3);
        exp = putExperiment(exp);

        clearAssignmentsMetadataCache();

        // since the experiment crossed the maxusers the experiment should change to PUASED and response for assignment
        // should be "EXPERIMENT_PAUSED"
        Assignment assignment = postAssignment(exp, new User("user10"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "EXPERIMENT_PAUSED");
        exp = getExperiment(exp);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
        experimentList.set(1, experimentChangedToRapidExperiment);
    }

    /**
     * This test case tests for the put assignment API call that forces assignment call on particular bucket for a rapid
     * experiment
     */
    @Test
    public void testPutAssignmentToRapidExperiment() {
        Experiment exp = getExperiment(experimentList.get(2));
        List<Bucket> bucketList = getBuckets(exp);
        Assignment assignment = AssignmentFactory.createAssignment().setAssignment(bucketList.get(0).label)
                .setExperimentLabel(exp.label).setOverwrite(true);
        
        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            // lets do an assignment for a user
            assignment = putAssignment(exp, assignment, new User("user" + i));
            // lets assert the status and response code and also the state of the experiment
            assertReturnCode(response, HttpStatus.SC_OK);
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
        }

        // since the experiment crossed the maxusers the experiment should change to PUASED
        assignment = putAssignment(exp, assignment, new User("user6"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);


        assignment = putAssignment(exp, assignment, new User("user8"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);

        assignment = putAssignment(exp, assignment, new User("user9"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        exp = getExperiment(exp);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
        experimentList.set(2, rapidExperiment3);
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
            exp = postExperiment(exp);
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 3);
            postBuckets(bucketList);

            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            exp = putExperiment(exp);
            batchExperiments.add(exp);

        }
        assignPageToExperimentsList(batchExperiments, partialpage);
        // the assignment will happen until the max users cap is reached for all experiments in batch
        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            List<Assignment> assignments = postAssignments(application, partialpage, new User("user" + i));
            for (Assignment assignment : assignments) {
                Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

            }
        }

        // if we do one more assignment the rapid experiments should change to PAUSED/STOPPED state and the remaining
        // should be in running state
        List<Assignment> assignments = postAssignments(application, partialpage, new User("user1000"));
        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            Experiment exp = getExperiment(batchExperiments.get(i - 1));
            if (i <= NUMBER_OF_RAPID_EXPERIMENTS_IN_BATCH) {
                Assert.assertEquals(assignments.get(i - 1).status, "EXPERIMENT_PAUSED");
                Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
            } else {
                Assert.assertEquals(assignments.get(i - 1).status, "NEW_ASSIGNMENT");
                Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
            }

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
        // lets create experiments for the batch
        for (int i = 1; i <= NUMBER_OF_EXPERIMENTS_PER_BATCH; i++) {
            Experiment exp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -1)
                    .setApplication(application).setIsRapidExperiment(true).setUserCap(RAPID_EXP_MAX_USERS);

            exp = postExperiment(exp);
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 3);
            postBuckets(bucketList);

            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            exp = putExperiment(exp);
            experimentList.add(exp);

        }

        assignPageToExperimentsList(experimentList, page);

        // the assignment will happen until the max users cap is reached for all experiments in batch
        for (int i = 1; i <= RAPID_EXP_MAX_USERS; i++) {
            List<Assignment> assignments = postAssignments(application, page, new User("user" + (i + 1000)));
            for (Assignment assignment : assignments) {
                Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

            }
        }

        // if we do one more assignment the rapid experiments should change to PAUSED/STOPPED state 
        List<Assignment> assignments = postAssignments(application, page, new User("user100"));
        for (Experiment experiment: experimentList) {
            Experiment exp = getExperiment(experiment);
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
        }
        
        //assert that the response of the post assignment is EXPERIMENT_PAUSED
        assertAssignmentResponseStatus(assignments, "EXPERIMENT_PAUSED");

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
     * This method asserts the status of the response we received from  
     * a post assignment 
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
