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
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.analytics.AnalyticsParameters;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_RUNNING;
import static org.testng.Assert.assertEquals;

/**
 * Bucket integration tests
 */
public class RollUpBadCallsIntegrationTest extends TestBase {

    private static final String FROM_TIME = "fromTime";
    private static final String QBO = "qbo";
    protected static final String RED = "red";
    protected static final String BLUE = "blue";

    protected static final String PROD = "PROD";
    protected static final String QA = "QA";

    private Experiment experiment;
    private List<Bucket> buckets = new ArrayList<>();
    private String[] labels = {BLUE, RED};
    private double[] allocations = {.50, .50,};
    private boolean[] control = {false, true};

    /**
     * Initializes a default experiment.
     */
    public RollUpBadCallsIntegrationTest() throws Exception {

        setResponseLogLengthLimit(1000);

        experiment = ExperimentFactory.createExperiment();
        experiment.samplingPercent = 1.0;
        experiment.label = "experiment";
        experiment.applicationName = QBO + UUID.randomUUID();

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy(
                "creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);

    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateTwoBuckets() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime,
                "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime,
                "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state,
                "Experiment creation failed (No state).");
        experiment.update(exp);
        buckets = BucketFactory.createCompleteBuckets(experiment, allocations,
                labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);

        Assert.assertEquals(buckets, resultBuckets);

        for (Bucket result : resultBuckets) {
            Bucket matching = null;
            for (Bucket cand : buckets) {
                if (cand.label.equals(result.label)) {
                    matching = cand;
                    break;
                }

            }
            assertEquals(result.label, matching.label);
            assertEquals(result.isControl, matching.isControl);
            assertEquals(result.allocationPercent, matching.allocationPercent);
            assertEquals(result.description, matching.description);
        }
        experiment.state = EXPERIMENT_STATE_RUNNING;
        experiment = putExperiment(experiment);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestCountsExperimentIdFoobar() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.id = "foobar";
        AnalyticsParameters parameters = new AnalyticsParameters();
        postExperimentCounts(experiment, parameters,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestCountsExperimentId0() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.id = "0";
        AnalyticsParameters parameters = new AnalyticsParameters();
        postExperimentCounts(experiment, parameters,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestCountsFromTimeFoobar() {
        AnalyticsParameters parameters = new AnalyticsParameters();
        parameters.fromTime = "foobar";
        postExperimentCounts(experiment, parameters, HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestCountToTimeFoobar() {
        AnalyticsParameters parameters = new AnalyticsParameters();
        parameters.toTime = "foobar";
        postExperimentCounts(experiment, parameters, HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestCountDailiesExperimentIdFoobar() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.id = "foobar";
        AnalyticsParameters parameters = new AnalyticsParameters();
        postExperimentCumulativeCounts(experiment, parameters,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestCountDailiesExperimentId0() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.id = "0";
        AnalyticsParameters parameters = new AnalyticsParameters();
        postExperimentCumulativeCounts(experiment, parameters,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestCountDailesFromTimeFoobar() {
        AnalyticsParameters parameters = new AnalyticsParameters();
        parameters.fromTime = "foobar";
        postExperimentCumulativeCounts(experiment, parameters,
                HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestCountDailiesToTimeFoobar() {
        AnalyticsParameters parameters = new AnalyticsParameters();
        parameters.toTime = "foobar";
        postExperimentCumulativeCounts(experiment, parameters,
                HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestPostStatisticsExperimentIdFoobar() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.id = "foobar";
        AnalyticsParameters parameters = new AnalyticsParameters();
        postStatistics(experiment, parameters,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestPostStatisticsExperimentId0() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.id = "0";
        AnalyticsParameters parameters = new AnalyticsParameters();
        postStatistics(experiment, parameters,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestPostStatisticsFromTimeFoobar() {
        AnalyticsParameters parameters = new AnalyticsParameters();
        parameters.fromTime = "foobar";
        postStatistics(experiment, parameters, HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestPostStatisticsToTimeFoobar() {
        AnalyticsParameters parameters = new AnalyticsParameters();
        parameters.toTime = "foobar";
        postStatistics(experiment, parameters, HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestPostDailyStatisticsExperimentIdFoobar() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.id = "foobar";
        AnalyticsParameters parameters = new AnalyticsParameters();
        postDailyStatistics(experiment, parameters,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestPostDailyStasticsExperimentId0() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.id = "0";
        AnalyticsParameters parameters = new AnalyticsParameters();
        postDailyStatistics(experiment, parameters,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestPostDailyStatisticsFromTimeFoobar() {
        AnalyticsParameters parameters = new AnalyticsParameters();
        parameters.fromTime = "foobar";
        postDailyStatistics(experiment, parameters, HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_BadRequestPostDailyStatisticsToTimeFoobar() {
        AnalyticsParameters parameters = new AnalyticsParameters();
        parameters.toTime = "foobar";
        postDailyStatistics(experiment, parameters, HttpStatus.SC_BAD_REQUEST);
    }

}
