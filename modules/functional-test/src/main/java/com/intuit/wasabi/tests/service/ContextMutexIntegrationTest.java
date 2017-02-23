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
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.intuit.wasabi.tests.library.util.Constants.ASSIGNMENT_EXISTING_ASSIGNMENT;
import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_DELETED;
import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_RUNNING;
import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_TERMINATED;
import static com.intuit.wasabi.tests.library.util.Constants.NEW_ASSIGNMENT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Context mutex experiment integration test
 */
public class ContextMutexIntegrationTest extends TestBase {

    private List<Bucket> buckets = new ArrayList<>();
    private String[] labels = {"blue", "white"};
    private double[] allocations = {.5, .5};
    private boolean[] control = {false, true};

    private String contextFoo = "FOO";
    private String contextBar = "BAR";

    private String[] contexts = new String[]{contextFoo, contextBar};

    private User userBill = UserFactory.createUser("Bill");
    private User userJane = UserFactory.createUser("Jane");

    Experiment experiment1ForMutex = null;
    Experiment experiment2ForMutex = null;

    public ContextMutexIntegrationTest() {
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_TestMutexSetup() {
        experiment1ForMutex = ExperimentFactory.createExperiment();
        Experiment experiment1 = ExperimentFactory.createCompleteExperiment();
        experiment1.samplingPercent = 1;
        Experiment exp1 = postExperiment(experiment1);
        experiment1ForMutex.update(exp1);
        buckets = BucketFactory.createCompleteBuckets(experiment1ForMutex, allocations, labels, control);
        List<Bucket> resultBuckets1 = postBuckets(buckets);
        experiment1ForMutex.state = EXPERIMENT_STATE_RUNNING;
        experiment1ForMutex.rule = "";
        experiment1ForMutex = putExperiment(experiment1ForMutex);

        experiment2ForMutex = ExperimentFactory.createExperiment();
        Experiment experiment2 = ExperimentFactory.createCompleteExperiment();
        experiment2.samplingPercent = 1;
        Experiment exp2 = postExperiment(experiment2);
        experiment2ForMutex.update(exp2);
        buckets = BucketFactory.createCompleteBuckets(experiment2ForMutex, allocations, labels, control);
        List<Bucket> resultBuckets2 = postBuckets(buckets);
        experiment2ForMutex.state = EXPERIMENT_STATE_RUNNING;
        experiment2ForMutex.rule = "";
        experiment2ForMutex = putExperiment(experiment2ForMutex);

        String mutex1And2 = "{\"experimentIDs\": [\"" + experiment1ForMutex.id + "\",\"" + experiment2ForMutex.id + "\"]}";
        response = apiServerConnector.doPost("/experiments/" + experiment1ForMutex.id + "/exclusions",
                mutex1And2);
        assertReturnCode(response, HttpStatus.SC_CREATED);

        String mutex2And1 = "{\"experimentIDs\": [\"" + experiment2ForMutex.id + "\",\"" + experiment2ForMutex.id + "\"]}";
        response = apiServerConnector.doPost("/experiments/" + experiment2ForMutex.id + "/exclusions",
                mutex2And1);
        assertReturnCode(response, HttpStatus.SC_CREATED);

    }

    @Test(dependsOnMethods = {"t_TestMutexSetup"})
    public void t_AssignJaneToExperiment1() {
        Assignment resultOfJaneTo1Context0 = postAssignment(experiment1ForMutex, userJane, contexts[0]);
        assertEquals(resultOfJaneTo1Context0.status, NEW_ASSIGNMENT);
        assertNotNull(resultOfJaneTo1Context0.assignment);

    }

    @Test(dependsOnMethods = {"t_AssignJaneToExperiment1"})
    public void t_AssignJaneToExperiment1Again() {
        Assignment resultOfJaneTo1Context0AnotherTime = postAssignment(experiment1ForMutex, userJane, contexts[0]);
        assertEquals(resultOfJaneTo1Context0AnotherTime.status, ASSIGNMENT_EXISTING_ASSIGNMENT);
    }

    @Test(dependsOnMethods = {"t_AssignJaneToExperiment1Again"})
    public void t_AssignJaneToExperiment2() {
        Assignment resultOfJaneTo2Context0 = postAssignment(experiment2ForMutex, userJane, contexts[0]);
        assertEquals(resultOfJaneTo2Context0.status, NEW_ASSIGNMENT);
        assertEquals(resultOfJaneTo2Context0.assignment, null);

    }

    @Test(dependsOnMethods = {"t_AssignJaneToExperiment2"})
    public void t_AssignJaneToExperiment1DiffernetContextAgain() {
        Assignment resultOfJaneTo2Context0AnotherTime = postAssignment(experiment2ForMutex, userJane, contexts[0]);
        assertEquals(resultOfJaneTo2Context0AnotherTime.status, ASSIGNMENT_EXISTING_ASSIGNMENT);
    }

    @Test(dependsOnMethods = {"t_AssignJaneToExperiment1DiffernetContextAgain"})
    public void t_AssignJaneToExperiment2DifferntContext() {
        Assignment resultOfJaneTo2Context1 = postAssignment(experiment2ForMutex, userJane, contexts[1]);
        assertEquals(resultOfJaneTo2Context1.status, NEW_ASSIGNMENT);
        assertNotNull(resultOfJaneTo2Context1.assignment);
    }

    @Test(dependsOnMethods = {"t_AssignJaneToExperiment1DiffernetContextAgain"})
    public void t_AssignBillToExperiment2() {
        Assignment resultOfBillTo2Context0 = postAssignment(experiment2ForMutex, userBill, contexts[0]);
        assertEquals(resultOfBillTo2Context0.status, NEW_ASSIGNMENT);
        assertNotNull(resultOfBillTo2Context0.assignment);
    }

    @AfterSuite
    public void t_TerminateAndDeleteMutextExperiment() {
        Experiment experiment = getExperiment(experiment1ForMutex);
        experiment.state = EXPERIMENT_STATE_TERMINATED;
        putExperiment(experiment);
        experiment.state = EXPERIMENT_STATE_DELETED;
        putExperiment(experiment, HttpStatus.SC_NO_CONTENT);

        experiment = getExperiment(experiment2ForMutex);
        experiment.state = EXPERIMENT_STATE_TERMINATED;
        putExperiment(experiment);
        experiment.state = EXPERIMENT_STATE_DELETED;
        putExperiment(experiment, HttpStatus.SC_NO_CONTENT);
    }
}
