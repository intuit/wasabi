package com.intuit.wasabi.tests.service.segmentation;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intuit.wasabi.tests.data.SegmentationDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class BatchRuleTest extends TestBase {
    private static final Logger LOGGER = getLogger(BatchRuleTest.class);
    private final List<Experiment> validExperimentsLists = new ArrayList<>();

    @Test(groups = {"createExperimentWithSegementation"},
            dataProvider = "batchSetup", dataProviderClass = SegmentationDataProvider.class)
    public void t_setupExperiment(String data) {
        LOGGER.debug(data);
        response = apiServerConnector.doPost("/experiments", data);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment experiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        validExperimentsLists.add(experiment);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }

    @Test(dependsOnMethods = {"t_setupExperiment"}, groups = {"createExperimentWithSegementation"})
    public void t_setupBucketsAndStartExperiment() {
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

    @Test(dependsOnGroups = {"createExperimentWithSegementation"}, groups = {"batchAssignment"},
            dataProvider = "batchAssignmentData", dataProviderClass = SegmentationDataProvider.class)
    public void t_batchAssignExperiment(int index, String applicationName, String userId, String data) {
        LOGGER.debug(data);
        clearAssignmentsMetadataCache();
        response = apiServerConnector.doPost("/assignments/applications/" + applicationName + "/users/" + userId, data);
        Type listType = new TypeToken<Map<String, ArrayList<Assignment>>>() {
        }.getType();
        Map<String, List<Assignment>> batchAssignmentResult = new Gson().fromJson(response.asString(), listType);
        LOGGER.debug(response.asString());
        List<Assignment> assignments = batchAssignmentResult.get("assignments");
        for (int i = 0; i < assignments.size(); i++) {
            Assignment assignment = assignments.get(i);
            if (i == index) {
                Assert.assertEquals(assignment.status.contains("NEW_ASSIGNMENT"), true);
            } else {
                Assert.assertEquals(assignment.status.contains("NO_PROFILE_MATCH"), true);
            }
        }
    }

    @AfterClass
    public void t_cleanUp() {
        LOGGER.info("Clean up experiments");
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            response = apiServerConnector.doDelete("experiments/" + experiment.id);
            assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        }
    }
}
