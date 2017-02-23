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
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameInclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;

/**
 * This tests frequent state changes for their correctness.
 */
public class StateInconsistencyTest extends TestBase {

    private final SerializationStrategy comparisonStrategy;

    private final SerializationStrategy putStateStrategy;

    private Experiment experiment;

    /**
     * Creates a test to be run for a specific experiment.
     *
     * @param experiment the experiment
     */
    public StateInconsistencyTest(Experiment experiment) {
        this.experiment = experiment;
        comparisonStrategy = new DefaultNameExclusionStrategy(
                "creationTime", "modificationTime", "ruleJson", "id", "description", "rule"
        );
        putStateStrategy = new DefaultNameInclusionStrategy(
                "state"
        );
    }

    /**
     * Posts the experiment.
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_createExperiment() {
        Experiment created = postExperiment(experiment);

        experiment.setState(Constants.EXPERIMENT_STATE_DRAFT);
        assertEqualModelItems(created, experiment, comparisonStrategy);

        experiment.update(created);
    }

    /**
     * Checks if the experiment was in fact created.
     */
    @Test(dependsOnMethods = {"t_createExperiment"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 500)
    public void t_checkExperiment() {
        Experiment updated = getExperiment(experiment);
        assertEqualModelItems(updated, experiment, comparisonStrategy);
    }

    /**
     * Creates buckets for the experiments and posts them.
     */
    @Test(dependsOnMethods = {"t_checkExperiment"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(warmup = 500)
    public void t_createBuckets() {
        List<Bucket> buckets = BucketFactory.createBuckets(experiment, new double[]{
                0.3, 0.3, 0.4
        }, BucketFactory.bucketNameColors(3));
        postBuckets(buckets);
    }

    /**
     * Provides bucket states.
     *
     * @return a list of objects as data to be passed to the tests
     */
    @DataProvider
    public Object[][] experimentStates() {
        return new Object[][]{
                new Object[]{Constants.EXPERIMENT_STATE_RUNNING},
                new Object[]{Constants.EXPERIMENT_STATE_PAUSED},
                new Object[]{Constants.EXPERIMENT_STATE_TERMINATED},
        };
    }

    /**
     * Changes the experiment's state to state.
     *
     * @param state new state
     */
    @Test(dependsOnMethods = {"t_createBuckets"}, dataProvider = "experimentStates", retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(warmup = 500, maxTries = 3)
    public void t_iterateStates(String state) {
        experiment.setSerializationStrategy(putStateStrategy);
        experiment.setState(state);
        Experiment updated = putExperiment(experiment);
        assertEqualModelItems(experiment, updated, comparisonStrategy);
    }

    /**
     * Deletes experiments.
     */
    @Test(dependsOnMethods = {"t_iterateStates"})
    public void t_deleteExperiment() {
        deleteExperiment(experiment);
    }

}


