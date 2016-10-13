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
package com.intuit.wasabi.tests.service.segmentation;

import com.intuit.wasabi.tests.data.SegmentationDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class MutualExclusionSegmentationTest extends TestBase {
    private static final Logger LOGGER = getLogger(MutualExclusionSegmentationTest.class);
    private final List<Experiment> validExperimentsLists = new ArrayList<>();


    @Test(groups = {"setup"}, dataProvider = "experimentSetup", dataProviderClass = SegmentationDataProvider.class)
    public void setupExperiment(String data) {
        LOGGER.debug(data);
        response = apiServerConnector.doPost("/experiments", data);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment experiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        validExperimentsLists.add(experiment);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }

    @Test(dependsOnMethods = {"setupExperiment"}, groups = {"setup"})
    public void setupBucketsAndStartExperiment() {
        String redBucket = "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"\"}";
        String blueBucket = "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"\"}";
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", redBucket);
            assertReturnCode(response, HttpStatus.SC_CREATED);
            response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", blueBucket);
            assertReturnCode(response, HttpStatus.SC_CREATED);
            response = apiServerConnector.doPut("/experiments/" + experiment.id, "{\"state\": \"RUNNING\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
        }
    }

    @Test(dependsOnMethods = {"setupBucketsAndStartExperiment"}, groups = {"setup"})
    public void setupMutualExclusionRules() {
        String mutex_experiments = "{\"experimentIDs\": [\"" + validExperimentsLists.get(1).id + "\",\"" +
                validExperimentsLists.get(2).id + "\"]}";
        LOGGER.debug("experiment mutex is " + mutex_experiments);
        response = apiServerConnector.doPost("/experiments/" + validExperimentsLists.get(0).id + "/exclusions",
                mutex_experiments);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }

    @Test(dependsOnGroups = {"setup"}, groups = {"test"})
    public void mutualExclusionNoProfileMatch() {
        String url = "/assignments/applications/segmutex_" + SegmentationDataProvider.time + "/experiments/" +
                validExperimentsLists.get(1).label + "/users";
        String data = "{\"profile\": {\"salary\": 1000, \"state\": \"CA\", \"vet\": true}}";
        response = apiServerConnector.doPost(url + "/Billy", data);
        LOGGER.debug(response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("NO_PROFILE_MATCH"), true);
    }

    @Test(dependsOnGroups = {"setup"}, groups = {"test"})
    public void assignCorrectRule() {
        String url = "/assignments/applications/segmutex_" + SegmentationDataProvider.time + "/experiments/" +
                validExperimentsLists.get(0).label + "/users";
        String data = "{\"profile\": {\"salary\": 80000, \"state\": \"CA\", \"vet\": true}}";
        response = apiServerConnector.doPost(url + "/Billy", data);
        LOGGER.debug(response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
    }

    @Test(dependsOnMethods = {"assignCorrectRule"}, groups = {"test"})
    public void assignCorrectRuleMutexExperiemnt() {
        String url = "/assignments/applications/segmutex_" + SegmentationDataProvider.time + "/experiments/" +
                validExperimentsLists.get(0).label + "/users";
        String data = "{\"profile\": {\"salary\": 80000, \"state\": \"CA\", \"vet\": true}}";
        response = apiServerConnector.doPost(url + "/Billy", data);
        LOGGER.debug(response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("EXISTING_ASSIGNMENT"), true);
    }


    @AfterClass
    public void cleanUp() {
        toCleanUp.addAll(validExperimentsLists);
        cleanUpExperiments();
    }

}
