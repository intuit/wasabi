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
package com.intuit.wasabi.tests.service.segmentation;

import com.intuit.wasabi.tests.data.SegmentationDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class RuleTest extends TestBase {
    private static final Logger LOGGER = getLogger(RuleTest.class);
    private final Map<String, Experiment> validExperimentsMaps = new HashMap<>();


    @Test(groups = {"stage1"}, dataProvider = "validInput", dataProviderClass = SegmentationDataProvider.class)
    public void t_validSegmentationInput(String input) {
        LOGGER.debug(input);
        response = apiServerConnector.doPost("/experiments", input);
        Experiment experiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        validExperimentsMaps.put(experiment.applicationName, experiment);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }

    @Test(groups = {"stage1"}, dependsOnMethods = "t_validSegmentationInput")
    public void t_updateBadRuleToNoRuleExperiment() {
        Experiment experiment = validExperimentsMaps.get("seg_no_rule_" + SegmentationDataProvider.time);
        String rule = "{\"rule\": \"salary === 5\"}";
        response = apiServerConnector.doPut("/experiments/" + experiment.id, rule);
        assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);
        response = apiServerConnector.doGet("/experiments/" + experiment.id);
        assertReturnCode(response, HttpStatus.SC_OK);
    }

    @Test(groups = {"stage1"}, dependsOnMethods = "t_validSegmentationInput")
    public void t_updateBadRuleToExistingRuleExperiment() {
        Experiment experiment = validExperimentsMaps.get("seg_valid_rule_" + SegmentationDataProvider.time);
        String rule = "{\"rule\": \"vet >= False\"}";
        response = apiServerConnector.doPut("/experiments/" + experiment.id, rule);
        assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);
        response = apiServerConnector.doGet("/experiments/" + experiment.id);
        assertReturnCode(response, HttpStatus.SC_OK);
    }

    @Test(groups = {"stage1"}, dataProvider = "invalidInput", dataProviderClass = SegmentationDataProvider.class)
    public void t_createExperimentWithInvalidRule(String input) {
        response = apiServerConnector.doPost("/experiments", input);
        assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);
    }


    @Test(dependsOnGroups = {"stage1"}, groups = {"makingBuckets"})
    public void t_makingBuckets() {
        String base = "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"\"}";
        for (Experiment experiment : validExperimentsMaps.values()) {
            response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", base);
            assertReturnCode(response, HttpStatus.SC_CREATED);
            //create a second bucket
            String blueBucket = "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"\"}";
            response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", blueBucket);
            assertReturnCode(response, HttpStatus.SC_CREATED);
        }
    }

    @Test(dependsOnGroups = {"makingBuckets"}, groups = {"changingStates"},
            dataProvider = "states", dataProviderClass = SegmentationDataProvider.class)
    public void t_changingStates(String state) {
        for (Experiment experiment : validExperimentsMaps.values()) {
            String newState = "{\"state\": \"" + state + "\"}";
            response = apiServerConnector.doPut("/experiments/" + experiment.id, newState);
            assertReturnCode(response, HttpStatus.SC_OK);
            String invalidRule = "{\"rule\": \"vet >= False\"}";
            response = apiServerConnector.doPut("/experiments/" + experiment.id, invalidRule);
            assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(dependsOnMethods = {"t_changingStates"}, groups = {"changingStates"})
    public void t_changeRules() {
        for (Experiment experiment : validExperimentsMaps.values()) {
            if (experiment.applicationName.contains("valid")) {
                String newRule = "{\"rule\":\"vet=false\"}";
                response = apiServerConnector.doPut("/experiments/" + experiment.id, newRule);
                assertReturnCode(response, HttpStatus.SC_OK);
                String oldRule = "{\"rule\": \"(salary > 80000 && state = 'CA') || (salary > 60000 && vet = true)\"}";
                response = apiServerConnector.doPut("/experiments/" + experiment.id, oldRule);
                assertReturnCode(response, HttpStatus.SC_OK);
            }
        }
    }

    @Test(dependsOnGroups = {"changingStates"}, groups = {"assignment"})
    public void t_noRuleAssignment() {
        for (Experiment experiment : validExperimentsMaps.values()) {
            if (experiment.applicationName.contains("no_rule")) {
                String baseUrl = String.format("/assignments/applications/%s/experiments/%s/users/",
                        experiment.applicationName, experiment.label);
                LOGGER.debug("Assign johnDoe to New_ASSIGNMENT");
                response = apiServerConnector.doGet(baseUrl + "johnDoe");
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
                LOGGER.debug("Get existing assignment for johnDoe");
                response = apiServerConnector.doGet(baseUrl + "johnDoe");
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("EXISTING_ASSIGNMENT"), true);

                //Assign user with profile
                String data = "{\"profile\": {\"salary\": 50000, \"state\": \"CA\"}}";
                LOGGER.debug("Assign janeDoe to New_ASSIGNMENT");
                response = apiServerConnector.doPost(baseUrl + "janeDoe", data);
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
                LOGGER.debug("Get existing assignment for janeDoe");
                response = apiServerConnector.doGet(baseUrl + "janeDoe");
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("EXISTING_ASSIGNMENT"), true);
            }
        }
    }

    @Test(dependsOnGroups = {"changingStates"}, groups = {"assignment"})
    public void t_validRuleAssignment() {
        for (Experiment experiment : validExperimentsMaps.values()) {
            if (experiment.applicationName.contains("valid")) {
                String baseUrl = String.format("/assignments/applications/%s/experiments/%s/users/",
                        experiment.applicationName, experiment.label);
                String data = "{\"profile\": {\"salary\": 50000, \"state\": \"CA\", \"vet\": true}}";
                response = apiServerConnector.doPost(baseUrl + "jack", data);
                assertReturnCode(response, HttpStatus.SC_OK);
                LOGGER.debug(response.asString());
                Assert.assertEquals(response.asString().contains("NO_PROFILE_MATCH"), true);

                LOGGER.debug("change the profile, but still fail the rule");
                data = "{\"profile\": {\"salary\": 80000, \"state\": \"CA\", \"vet\": false}}";
                response = apiServerConnector.doPost(baseUrl + "jill", data);
                assertReturnCode(response, HttpStatus.SC_OK);
                LOGGER.debug(response.asString());
                Assert.assertEquals(response.asString().contains("NO_PROFILE_MATCH"), true);

                LOGGER.debug("Null assignment");
                data = "{\"profile\": {\"salary\": 80000, \"state\": \"CA\", \"vet\": true}}";
                response = apiServerConnector.doPost(baseUrl + "jane", data);
                LOGGER.debug("jane=" + response.asString());
                assertReturnCode(response, HttpStatus.SC_OK);
                Assignment assignment = AssignmentFactory.createFromJSONString(response.asString());
                Assert.assertNotNull(assignment.assignment, "Assignment should not be null");

                data = "{\"profile\": {\"salary\": 90000, \"state\": \"CA\", \"vet\": false}}";
                response = apiServerConnector.doPost(baseUrl + "bill", data);
                LOGGER.debug("bill=" + response.asString());
                assertReturnCode(response, HttpStatus.SC_OK);
                assignment = AssignmentFactory.createFromJSONString(response.asString());
                Assert.assertNotNull(assignment.assignment, "Assignment should not be null");
                Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");

                data = "{\"profile\": {\"salary\": 90000, \"state\": \"CA\", \"vet\": false, \"dummy\": true, \"extra\": 5}}";
                response = apiServerConnector.doPost(baseUrl + "bill", data);
                LOGGER.debug("bill=" + response.asString());
                assertReturnCode(response, HttpStatus.SC_OK);
                assignment = AssignmentFactory.createFromJSONString(response.asString());
                Assert.assertNotNull(assignment.assignment, "Assignment should not be null");
                Assert.assertEquals(assignment.status, "EXISTING_ASSIGNMENT");
            }
        }
    }


    @Test(dependsOnGroups = {"assignment"}, groups = ("assignmentWithOutProfile"))
    public void t_setupExperimentForAssignmentWithoutProfile() {
        String data = "{\"applicationName\":\"testApp_" + SegmentationDataProvider.time +
                "\",\"description\":\"test experiment\",\"startTime\":\""
                + SegmentationDataProvider.startTime + "\", \"endTime\":\"" + SegmentationDataProvider.endTime +
                "\",\"label\":\"expLabel_" + SegmentationDataProvider.time + "\",\"samplingPercent\":1," +
                "\"rule\":\"(State = 'CA')\"}";
        response = apiServerConnector.doPost("/experiments", data);
        LOGGER.info(response.asString());
        assertReturnCode(response, HttpStatus.SC_CREATED);
        Experiment experiment = ExperimentFactory.createFromJSONString(response.asString());
        validExperimentsMaps.put(experiment.applicationName, experiment);
        String bucket = "{\"label\": \"red\", \"allocationPercent\": 1.0, \"isControl\": true, " +
                "\"description\": \"red bucket\",\"payload\": \"HTML-JS-red\"}";
        response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", bucket);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPut("/experiments/" + experiment.id, "{\"state\": \"RUNNING\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        String page = "{\"pages\":[{\"name\":\"ConfirmationPage\",\"allowNewAssignment\":true}]}";
        response = apiServerConnector.doPost("/experiments/" + experiment.id + "/pages", page);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }

    @Test(dependsOnMethods = {"t_setupExperimentForAssignmentWithoutProfile"}, groups = ("assignmentWithOutProfile"))
    public void t_assignUser() {
        String application = "testApp_" + SegmentationDataProvider.time;
        Experiment experiment = validExperimentsMaps.get(application);
        String url = "/assignments/applications/" + application + "/experiments/" + experiment.label + "/users/user_0";
        response = apiServerConnector.doGet(url);
        LOGGER.info(response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("NO_PROFILE_MATCH"), true);
        url = "/assignments/applications/" + application + "/pages/ConfirmationPage/users/user_0";
        response = apiServerConnector.doGet(url);
        LOGGER.info(response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("NO_PROFILE_MATCH"), true);
    }


    @AfterClass
    public void t_cleanUp() {
        LOGGER.info("Clean up experiments");
        for (Experiment experiment : validExperimentsMaps.values()) {
            response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            response = apiServerConnector.doDelete("experiments/" + experiment.id);
            assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        }
    }

}
