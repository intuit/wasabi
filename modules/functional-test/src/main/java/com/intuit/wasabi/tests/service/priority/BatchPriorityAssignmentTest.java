/*
 * ******************************************************************************
 *  * Copyright 2016 Intuit
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package com.intuit.wasabi.tests.service.priority;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intuit.wasabi.tests.data.PriorityDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BatchPriorityAssignmentTest extends TestBase {
    private final Logger LOGGER = LoggerFactory.getLogger(BatchPriorityAssignmentTest.class);
    private final List<Experiment> validExperimentsLists = new ArrayList<>();

    @Test(groups = {"setup"}, dataProvider = "batchExperiments", dataProviderClass = PriorityDataProvider.class)
    public void setup(String experiment, String redBucket, String blueBucket) {
        response = apiServerConnector.doPost("experiments", experiment);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment result = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        validExperimentsLists.add(result);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPost("experiments/" + result.id + "/buckets", redBucket);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPost("experiments/" + result.id + "/buckets", blueBucket);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPut("experiments/" + result.id, "{\"state\": \"RUNNING\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
    }

    @Test(groups = {"setup"}, dependsOnMethods = {"setup"})
    public void setupMutexAndPriority() {
        String exclusion = "{\"experimentIDs\": [" + validExperimentsLists.stream().map(s -> "\"" + s.id + "\"").collect(Collectors.joining(",")) + "]}";
        response = apiServerConnector.doPost("experiments/" + validExperimentsLists.get(0).id + "/exclusions", exclusion);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPut("applications/" + validExperimentsLists.get(0).applicationName + "/priorities", exclusion);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        response = apiServerConnector.doGet("applications/" + validExperimentsLists.get(0).applicationName + "/priorities");
        LOGGER.info("retrieved priorities: " + response.asString());
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> result = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> prioritizedExperiments = result.get("prioritizedExperiments");
        Assert.assertEquals(prioritizedExperiments.size(), validExperimentsLists.size());
        for (int i = 0; i < validExperimentsLists.size(); i++) {
            Assert.assertEquals(prioritizedExperiments.get(i).get("id"), validExperimentsLists.get(i).id);
        }
    }

    @Test(groups = {"batchAssign"}, dependsOnGroups = {"setup"})
    public void t_batchAssign() {
        String lables = "{\"labels\": [" + validExperimentsLists.stream().map(s -> "\"" + s.label + "\"").collect(Collectors.joining(",")) + "]}";
        response = apiServerConnector.doPost("/assignments/applications/" + validExperimentsLists.get(0).applicationName + "/users/johnDoe", lables);
        assertReturnCode(response, HttpStatus.SC_OK);
        LOGGER.debug("status: " + response.statusCode() + "\noutput: " + response.asString());
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> result = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> assignments = result.get("assignments");
        Assert.assertNotNull(assignments.get(0).get("assignment"));
        IntStream.range(1, assignments.size()).forEach(
                i -> Assert.assertNull(assignments.get(i).get("assignment"))
        );
    }

    @Test(groups = {"batchAssign"}, dependsOnGroups = {"setup"}, dependsOnMethods = {"t_batchAssign"})
    public void t_changePriorityBatchAssign() {
        response = apiServerConnector.doPost("experiments/" + validExperimentsLists.get(0).id + "/priority/5");
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doGet("applications/" + validExperimentsLists.get(0).applicationName + "/priorities");
        assertReturnCode(response, HttpStatus.SC_OK);
        clearAssignmentsMetadataCache();
        String lables = "{\"labels\": [" + validExperimentsLists.stream().map(s -> "\"" + s.label + "\"").collect(Collectors.joining(",")) + "]}";
        response = apiServerConnector.doPost("/assignments/applications/" + validExperimentsLists.get(0).applicationName + "/users/johnDoe2", lables);
        assertReturnCode(response, HttpStatus.SC_OK);
        LOGGER.info("output: " + response.asString());
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> result = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> assignments = result.get("assignments");
        Assert.assertNull(assignments.get(assignments.size() - 1).get("assignment"));
        IntStream.range(0, assignments.size() - 1).forEach(
                i -> Assert.assertNotNull(assignments.get(i).get("assignment"))
        );
    }

    @AfterClass
    public void t_cleanUp() {
        LOGGER.info("Clean up experiments");
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doGet("experiments/" + experiment.id);
            Experiment result = ExperimentFactory.createFromJSONString(response.asString());
            if (!"DRAFT".equals(result.state) && !"TERMINATED".equals(result.state)) {
                response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
                assertReturnCode(response, HttpStatus.SC_OK);
            }
            response = apiServerConnector.doDelete("experiments/" + experiment.id);
            assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        }
    }

}
