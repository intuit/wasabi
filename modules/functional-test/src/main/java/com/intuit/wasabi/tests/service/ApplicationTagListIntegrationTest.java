/*******************************************************************************
 * Copyright 2017 Intuit
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
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.UUID;

import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_DELETED;

/**
 * Integration Test for the retrieval of ExperimentTags by Application.
 */
public class ApplicationTagListIntegrationTest extends TestBase {

    private Experiment experiment;
    private String appName = "tagListTest-" + UUID.randomUUID();

    /**
     * Set up one experiment that has no tags.
     */
    @BeforeClass
    public void setUp() {
        setResponseLogLengthLimit(1000);

        experiment = ExperimentFactory.createExperiment();
        experiment.startTime = "2015-08-01T00:00:00+0000";
        experiment.endTime = "2015-08-08T00:00:00+0000";
        experiment.samplingPercent = 0.5;
        experiment.label = "experiment";
        experiment.applicationName = appName;

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);

    }


    @Test(dependsOnGroups = {"ping"})
    public void t_RetrieveTagList() {
        //creates an experiment for that app
        this.experiment = postExperiment(experiment);
        //retrieve response (method checks for status code OK)
        String response = getExperimentTags();
    }

    @AfterClass
    public void terminateAndDelete() {
        experiment.state = EXPERIMENT_STATE_DELETED;
        putExperiment(experiment, HttpStatus.SC_NO_CONTENT);
    }

}
