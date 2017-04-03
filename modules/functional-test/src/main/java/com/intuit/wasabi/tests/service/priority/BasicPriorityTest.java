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

public class BasicPriorityTest extends TestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicPriorityTest.class);
    private final List<Experiment> validExperimentsLists = new ArrayList<>();


    @Test(groups = {"setup"}, dataProvider = "Experiments", dataProviderClass = PriorityDataProvider.class)
    public void setup(String experiment) {
        response = apiServerConnector.doPost("experiments", experiment);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment result = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        validExperimentsLists.add(result);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }

    @Test(groups = {"setup"}, dependsOnMethods = {"setup"}, dataProvider = "Application", dataProviderClass = PriorityDataProvider.class)
    public void setupMutexAndPriority(String appName) {
        String exclusion = "{\"experimentIDs\": [" + validExperimentsLists.stream().map(s -> "\"" + s.id + "\"").collect(Collectors.joining(",")) + "]}";
        response = apiServerConnector.doPost("experiments/" + validExperimentsLists.get(0).id + "/exclusions", exclusion);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPut("applications/" + appName + "/priorities", exclusion);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        response = apiServerConnector.doGet("applications/" + appName + "/priorities");
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

    @Test(groups = {"priorityChange"}, dependsOnGroups = {"setup"}, dataProvider = "Application", dataProviderClass = PriorityDataProvider.class)
    public void t_priorityChange(String appName) {
        //move the first priority to the last
        response = apiServerConnector.doPost("experiments/" + validExperimentsLists.get(0).id + "/priority/5");
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doGet("applications/" + appName + "/priorities");
        assertReturnCode(response, HttpStatus.SC_OK);
        LOGGER.info("retrieved priorities: " + response.asString());
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> result = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> prioritizedExperiments = result.get("prioritizedExperiments");
        Assert.assertEquals(prioritizedExperiments.get(4).get("id"), validExperimentsLists.get(0).id);
    }

    @Test(groups = {"priorityChange"}, dependsOnMethods = {"t_priorityChange"}, dependsOnGroups = {"setup"},
            dataProvider = "NewExperiments", dataProviderClass = PriorityDataProvider.class)
    public void addNewExperimentToPriority(String experiment) {
        response = apiServerConnector.doPost("experiments", experiment);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment newExperiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doGet("applications/" + newExperiment.applicationName + "/priorities");
        LOGGER.debug("retrieved priorities: " + response.asString());
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> result = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> prioritizedExperiments = result.get("prioritizedExperiments");
        Assert.assertEquals(prioritizedExperiments.get(5).get("id"), newExperiment.id);
        response = apiServerConnector.doPut("experiments/" + newExperiment.id, "{\"state\": \"DELETED\"}");
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        response = apiServerConnector.doGet("applications/" + newExperiment.applicationName + "/priorities");
        assertReturnCode(response, HttpStatus.SC_OK);
        result = new Gson().fromJson(response.asString(), listType);
        prioritizedExperiments = result.get("prioritizedExperiments");
        Assert.assertEquals(prioritizedExperiments.size(), 5);
        for (Map<String, Object> entry : prioritizedExperiments) {
            Assert.assertEquals("TERMINATED".equals(entry.get("state")), false);
            Assert.assertEquals("DELETED".equals(entry.get("state")), false);
        }
    }

    @Test(groups = {"priorityListChange"}, dependsOnGroups = {"setup", "priorityChange"},
            dataProvider = "Application", dataProviderClass = PriorityDataProvider.class)
    public void t_priorityListChange(String appName) {
        String exclusion = "{\"experimentIDs\": [\"89659e0b-2ee7-419d-8a4e-42938254d847\"," +
                validExperimentsLists.stream().map(s -> "\"" + s.id + "\"").collect(Collectors.joining(",")) + "]}";
        response = apiServerConnector.doPut("applications/" + appName + "/priorities", exclusion);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        response = apiServerConnector.doGet("applications/" + appName + "/priorities");
        LOGGER.debug("output: " + response.asString());
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> result = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> prioritizedExperiments = result.get("prioritizedExperiments");
        Assert.assertEquals(prioritizedExperiments.size(), 5);
    }

    @Test(groups = {"priorityListChange"}, dependsOnGroups = {"setup", "priorityChange"},
            dataProvider = "differentApp", dataProviderClass = PriorityDataProvider.class)
    public void t_differentApplication(String experiment) {
        response = apiServerConnector.doPost("experiments", experiment);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment result = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        String exclusion = "{\"experimentIDs\": [\"" + result.id + "\"," +
                validExperimentsLists.stream().map(s -> "\"" + s.id + "\"").collect(Collectors.joining(",")) + "]}";
        response = apiServerConnector.doPut("applications/" + validExperimentsLists.get(0).applicationName + "/priorities",
                exclusion);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        response = apiServerConnector.doGet("applications/" + validExperimentsLists.get(0).applicationName + "/priorities");
        LOGGER.debug("output: " + response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> resultMap = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> prioritizedExperiments = resultMap.get("prioritizedExperiments");
        Assert.assertEquals(prioritizedExperiments.size(), validExperimentsLists.size());
        response = apiServerConnector.doDelete("experiments/" + result.id);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
    }

    @Test(groups = {"priorityListChange"}, dependsOnGroups = {"setup", "priorityChange"},
            dataProvider = "Application", dataProviderClass = PriorityDataProvider.class)
    public void t_invliadUuids(String appName) {
        String exclusion = "{\"experimentIDs\": [" +
                validExperimentsLists.stream().map(s -> "\"" + s.id + "\"").collect(Collectors.joining(",")) + "]}";
        exclusion = exclusion.replace(validExperimentsLists.get(0).id, "bbbac42e-50c5-4c9a-a398-8588bf6bbe33");
        response = apiServerConnector.doPut("applications/" + validExperimentsLists.get(0).applicationName + "/priorities",
                exclusion);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        response = apiServerConnector.doGet("applications/" + validExperimentsLists.get(0).applicationName + "/priorities");
        LOGGER.debug("output: " + response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> resultMap = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> prioritizedExperiments = resultMap.get("prioritizedExperiments");
        Assert.assertEquals(prioritizedExperiments.size(), validExperimentsLists.size());
    }

    @Test(groups = {"priorityListChange"}, dependsOnGroups = {"setup", "priorityChange"},
            dataProvider = "terminatedExperiment", dataProviderClass = PriorityDataProvider.class)
    public void t_terminatedExperiment(String terminated, String redBucket, String blueBucket) {
        response = apiServerConnector.doPost("experiments", terminated);
        Experiment terminatedExperiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        response = apiServerConnector.doPost("experiments/" + terminatedExperiment.id + "/buckets", redBucket);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPost("experiments/" + terminatedExperiment.id + "/buckets", blueBucket);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPut("experiments/" + terminatedExperiment.id, "{\"state\": \"RUNNING\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        response = apiServerConnector.doPut("experiments/" + terminatedExperiment.id, "{\"state\": \"TERMINATED\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        assertPriority(terminatedExperiment);
        response = apiServerConnector.doDelete("experiments/" + terminatedExperiment.id);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
    }

    @Test(groups = {"priorityListChange"}, dependsOnGroups = {"setup", "priorityChange"},
            dataProvider = "deletedExperiment", dataProviderClass = PriorityDataProvider.class)
    public void t_deletedExperiment(String deleted) {
        response = apiServerConnector.doPost("experiments", deleted);
        Experiment deletedExperiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        response = apiServerConnector.doDelete("experiments/" + deletedExperiment.id);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        assertPriority(deletedExperiment);
    }

    private void assertPriority(Experiment assertExperiment) {
        String exclusion = "{\"experimentIDs\": [" +
                validExperimentsLists.stream().map(s -> "\"" + s.id + "\"").collect(Collectors.joining(",")) + "]}";
        exclusion = exclusion.replace(validExperimentsLists.get(0).id, assertExperiment.id);
        response = apiServerConnector.doPut("applications/" + validExperimentsLists.get(0).applicationName + "/priorities",
                exclusion);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        response = apiServerConnector.doGet("applications/" + validExperimentsLists.get(0).applicationName + "/priorities");
        LOGGER.debug("output: " + response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Type listType = new TypeToken<Map<String, ArrayList<Map<String, Object>>>>() {
        }.getType();
        Map<String, List<Map<String, Object>>> resultMap = new Gson().fromJson(response.asString(), listType);
        List<Map<String, Object>> prioritizedExperiments = resultMap.get("prioritizedExperiments");
        Assert.assertEquals(prioritizedExperiments.size(), validExperimentsLists.size());
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
