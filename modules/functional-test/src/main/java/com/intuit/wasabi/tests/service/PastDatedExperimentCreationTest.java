/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.tests.service;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.ModelAssert;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

public class PastDatedExperimentCreationTest extends TestBase {
    Experiment experiment;

    /**
     * Creates an experiment with a start date in the past and an end date in the past.
     * This should be an unsuccessful operation.
     */
    @Test
    public void createExperimentPastEndDate() {
        //create an experiment
        experiment = ExperimentFactory.createCompleteExperiment();
        toCleanUp.add(experiment);

        // set start time and end time to previous dates
        experiment.startTime = TestUtils.relativeTimeString(-10);
        experiment.endTime = TestUtils.relativeTimeString(-5);
        postExperiment(experiment, HttpStatus.SC_BAD_REQUEST);
    }


    /**
     * Creates an experiment with a start date in the past and an end date in the future.
     * This should be a successful operation.
     */
    @Test
    public void createExperimentPastStartDateFutureEndDate() {
        //create an experiment
        experiment = ExperimentFactory.createCompleteExperiment();
        toCleanUp.add(experiment);

        // set start time and end time to previous dates
        experiment.startTime = TestUtils.relativeTimeString(-10);
        experiment.endTime = TestUtils.relativeTimeString(5);
        Experiment exp = postExperiment(experiment);
        ModelAssert.assertEqualModelItems(exp, experiment, new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson"));
    }

}
