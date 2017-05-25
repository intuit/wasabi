package com.intuit.wasabi.tests.service;

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
    private static final int RAPID_EXP_MAX_USERS = 5;
    Experiment rapidExperiment1 = null;
    Experiment rapidExperiment2 = null;
    Experiment rapidExperiment3 = null;
    List<Experiment> expList = new ArrayList<Experiment>();

    @BeforeClass
    public void setUp() {

        rapidExperiment1 = ExperimentFactory.createExperiment("_1" + UUID.randomUUID().toString())
                .setApplication(new Application("rapidExperimentApplication")).setIsRapidExperiment(true)
                .setUserCap(RAPID_EXP_MAX_USERS);
        rapidExperiment2 = ExperimentFactory.createExperiment("_2" + UUID.randomUUID().toString())
                .setApplication(new Application("rapidExperimentApplication"));
        
        rapidExperiment3 = ExperimentFactory.createExperiment("_3" + UUID.randomUUID().toString())
                .setApplication(new Application("rapidExperimentApplication")).setIsRapidExperiment(true)
                .setUserCap(RAPID_EXP_MAX_USERS);
        
        rapidExperiment1 = postExperiment(rapidExperiment1);
        rapidExperiment2 = postExperiment(rapidExperiment2);
        rapidExperiment3 = postExperiment(rapidExperiment3);
        expList.add(rapidExperiment1);
        expList.add(rapidExperiment2);
        expList.add(rapidExperiment3);
        for (int i = 1; i <= 3; i++) {
            Experiment exp = expList.get(i - 1);
            List<Bucket> bucketList = BucketFactory.createBuckets(exp, 3);
            postBuckets(bucketList);

            exp.state = Constants.EXPERIMENT_STATE_RUNNING;
            exp = putExperiment(exp);
        }

    }

    @Test
    public void testRapidExperiment() {

        Experiment exp = getExperiment(expList.get(0));
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
        exp = getExperiment(exp);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
        expList.set(0, rapidExperiment1);
    }

    /**
     * This test case covers a scenario where we start an experiment, do assignments and after that update the
     * experiment to rapid experiment with max users less than the number of users already assigned
     */
    @Test
    public void testChangeRegularExperimentToRapidExperiment() {
        Experiment exp = getExperiment(expList.get(1));
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
        System.out.println("*****" + exp);
        // since the experiment crossed the maxusers the experiment should change to PUASED
        Assignment assignment = postAssignment(exp, new User("user10"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(assignment.status, "EXPERIMENT_PAUSED");
        exp = getExperiment(exp);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
        expList.set(1, rapidExperiment2);
    }

    /**
     * This test case tests for the put assignment API call 
     * that forces assignment call on particular bucket
     * for a rapid experiment 
     */
    @Test
    public void testPutAssignmentExperimentToRapidExperiment() {
        Experiment exp = getExperiment(expList.get(2));
        List<Bucket> bucketList = getBuckets(exp);
        Assignment assignment = AssignmentFactory.createAssignment()
                .setAssignment(bucketList.get(0).label)
                .setExperimentLabel(exp.label)
                .setOverwrite(true);
        for (int i = 1; i <= 5; i++) {
            // lets do an assignment for a user       
            assignment = putAssignment(exp, assignment, new User("user"+i));
            // lets assert the status and response code and also the state of the experiment
            assertReturnCode(response, HttpStatus.SC_OK);
            Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
            Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_RUNNING);
        }

        // since the experiment crossed the maxusers the experiment should change to PUASED
        assignment = putAssignment(exp, assignment,new User("user6"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        
//        Assignment assignment2 = postAssignment(exp, new User("user7"), "PROD");
//        assertReturnCode(response, HttpStatus.SC_OK);
//        Assert.assertEquals(assignment2.status, "EXPERIMENT_PAUSED");
        
        assignment = putAssignment(exp, assignment,new User("user8"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        
       
        assignment = putAssignment(exp, assignment,new User("user9"), "PROD");
        assertReturnCode(response, HttpStatus.SC_OK);
        exp = getExperiment(exp);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED);
        expList.set(2, rapidExperiment3);
    }
    
    @AfterClass
    public void tearDown() {

        for (int i = 1; i <= 2; i++) {
            Experiment exp = expList.get(i - 1);
            exp.state = Constants.EXPERIMENT_STATE_PAUSED;
            putExperiment(exp);
            exp.state = Constants.EXPERIMENT_STATE_TERMINATED;
            putExperiment(exp);

            // lets dereference the experiment
            exp = null;
        }

        // lets call the Garbage collector Daemon
        System.gc();

    }
}
