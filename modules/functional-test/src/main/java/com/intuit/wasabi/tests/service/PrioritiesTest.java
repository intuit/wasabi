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
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tests for experiment priorities in application.
 */
public class PrioritiesTest extends TestBase {

    private static final Logger LOGGER = getLogger(PrioritiesTest.class);

    private final Application priorityApp = ApplicationFactory.createApplication().setName(Constants.DEFAULT_PREFIX_APPLICATION + "PrioritiesTest");
    private final Application newApp = ApplicationFactory.createApplication().setName(Constants.NEW_PREFIX_APPLICATION + "PrioritiesTest");
    private Experiment experiment;

    private final List<Experiment> experiments = new ArrayList<>(Constants.EXP_SPAWN_COUNT);
    private List<Experiment> priorities;
    private final String[] labels = {"red", "blue"};
    private final double[] allocations = {.50, .50};
    private final boolean[] control = {false, false};

    /**
     * Cleans up the app first.
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_prepareApp() {
        List<Experiment> experiments = getApplicationExperiments(priorityApp);
        for (Experiment experiment : experiments) {
            deleteExperiment(experiment);
        }
    }

    /**
     * Tests priority handling.
     */
    @Test(dependsOnMethods = {"t_prepareApp"})
    public void t_createAndValidatePriorityList() {

        LOGGER.info("Creating %d experiments.", Constants.EXP_SPAWN_COUNT);

        for (int i = 0; i < Constants.EXP_SPAWN_COUNT; ++i) {
            experiment = ExperimentFactory.createExperiment().setApplication(priorityApp);
            experiments.add(experiment);
            Experiment created = postExperiment(experiment);
            experiment.update(created);
            toCleanUp.add(experiment);
        }

        LOGGER.info("Making the first experiment mutually exclusive with the others.");
        postExclusions(experiments.get(0), experiments.subList(1, experiments.size()));

        LOGGER.info("Setting priorities.");
        putApplicationPriorities(priorityApp, experiments);

        LOGGER.info("Retrieving priority list");
        List<Experiment> priorities = getApplicationPriorities(priorityApp);

        LOGGER.info("Checking if the priorities match.");
        assertEqualModelItems(priorities, experiments, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson", "hypothesisIsCorrect", "results"));

    }

    /**
     * Tests that priority list will not accept invalid uuids.
     */
    @Test(dependsOnMethods = {"t_createAndValidatePriorityList"})
    public void t_testNewExperimentAddDeleteFlowOnPriorityList() {
        LOGGER.info("Adding a new experiment to the application.");
        Experiment nExperiment = ExperimentFactory.createExperiment().setApplication(priorityApp);
        Experiment created = postExperiment(nExperiment);

        final List<Experiment> newExpWithPriorityListExp = new ArrayList<>(experiments.size() + 1);
        newExpWithPriorityListExp.addAll(experiments);
        newExpWithPriorityListExp.add(created);

        LOGGER.info("Checking that the new experiment automatically gets added to the end of the priority list.");
        priorities = getApplicationPriorities(priorityApp);

        //Once the experiment is added, it automatically reflects at the end of the priority list of the application
        assertEqualModelItems(priorities, newExpWithPriorityListExp, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson", "hypothesisIsCorrect", "results"));

        LOGGER.info("Check if deleted experiments disappear from the priority list.");
        deleteExperiment(created);

        priorities = getApplicationPriorities(priorityApp);

        //Once the experiment is deleted, it stops reflecting in the priority list of the application
        assertEqualModelItems(priorities, experiments, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson", "hypothesisIsCorrect", "results"));

    }

    /**
     * Tests that priority list will not accept experiment of different applications.
     */
    @Test(dependsOnMethods = {"t_createAndValidatePriorityList"})
    public void t_testAddingExperimentOfDifferentAppToPriorityList() {
        LOGGER.info("Checking that priority list will not accept experiments from a different application");
        Experiment experiment = ExperimentFactory.createExperiment().setApplication(newApp);
        Experiment expForDifferentAppCreated = postExperiment(experiment);
        experiment.update(expForDifferentAppCreated);
        toCleanUp.add(experiment);

        final List<Experiment> differentAppExpWithPriorityListExp = new ArrayList<>(experiments.size() + 1);
        differentAppExpWithPriorityListExp.addAll(experiments);
        differentAppExpWithPriorityListExp.add(expForDifferentAppCreated);

        LOGGER.info("Setting priorities.");
        putApplicationPriorities(priorityApp, differentAppExpWithPriorityListExp);

        LOGGER.info("Retrieving priority list");
        priorities = getApplicationPriorities(priorityApp);

        //The priority list reflects only the same application experiments, not the new experiment of different application
        assertEqualModelItems(priorities, experiments, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson", "hypothesisIsCorrect", "results"));

    }

    /**
     * Tests that priority list will not accept experiments with invalid uuids.
     */
    @Test(dependsOnMethods = {"t_createAndValidatePriorityList"})
    public void t_testAddingInvalidUUIDExperimentToPriorityList() {
        LOGGER.info("Checking that priority list will not accept invalid uuids");
        Experiment invalidUUIDExperiment = new Experiment();
        invalidUUIDExperiment.id = "bbbac42e-50c5-4c9a-a398-8588bf6bbe33";
        toCleanUp.add(invalidUUIDExperiment);

        final List<Experiment> invalidUUIDExpWithPriorityListExp = new ArrayList<>(experiments.size() + 1);
        invalidUUIDExpWithPriorityListExp.addAll(experiments);
        invalidUUIDExpWithPriorityListExp.add(invalidUUIDExperiment);

        LOGGER.info("Setting priorities.");
        putApplicationPriorities(priorityApp, invalidUUIDExpWithPriorityListExp);

        LOGGER.info("Retrieving priority list");
        priorities = getApplicationPriorities(priorityApp);

        //The priority List reflects only the normal experiments, not the one with invalid uuid
        assertEqualModelItems(priorities, experiments, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson", "hypothesisIsCorrect", "results"));

    }

    /**
     * Tests that priority list will not accept experiments in TERMINATED or DELETED states.
     */
    @Test(dependsOnMethods = {"t_createAndValidatePriorityList"})
    public void t_testAddingTerminatedExperimentToPriorityList() {
        LOGGER.info("Checking that priority list will not accept experiments in TERMINATED or DELETED states");
        experiment = ExperimentFactory.createExperiment().setApplication(priorityApp);
        Experiment experimentToBeTerminated = postExperiment(experiment);
        toCleanUp.add(experimentToBeTerminated);

        List<Bucket> buckets = BucketFactory.createCompleteBuckets(experimentToBeTerminated, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        Assert.assertEquals(buckets, resultBuckets);

        experimentToBeTerminated.setState(Constants.EXPERIMENT_STATE_RUNNING);
        putExperiment(experimentToBeTerminated);
        Assert.assertEquals(experimentToBeTerminated.state, Constants.EXPERIMENT_STATE_RUNNING);

        experimentToBeTerminated.setState(Constants.EXPERIMENT_STATE_TERMINATED);
        putExperiment(experimentToBeTerminated);
        Assert.assertEquals(experimentToBeTerminated.state, Constants.EXPERIMENT_STATE_TERMINATED);

        final List<Experiment> terminatedExpWithPriorityListExp = new ArrayList<>(experiments.size() + 1);
        terminatedExpWithPriorityListExp.addAll(experiments);
        terminatedExpWithPriorityListExp.add(experimentToBeTerminated);

        LOGGER.info("Setting priorities.");
        putApplicationPriorities(priorityApp, terminatedExpWithPriorityListExp);

        LOGGER.info("Retrieving priority list");
        priorities = getApplicationPriorities(priorityApp);

        //The priority List reflects only the normal experiments, not the terminated one
        assertEqualModelItems(priorities, experiments, new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson", "hypothesisIsCorrect", "results"));

    }

}
