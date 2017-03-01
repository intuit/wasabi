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
package com.intuit.wasabi.tests.service.assignment;

import com.intuit.wasabi.tests.data.AssignmentDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.ExperimentMeta;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 4/28/16.
 */
public class BasicAssignment extends TestBase {
    static final Logger LOGGER = LoggerFactory.getLogger(BasicAssignment.class);
    List<ExperimentMeta> experimentList = new ArrayList<>();

    @Test(groups = {"setup"}, dataProvider = "ExperimentTimes", dataProviderClass = AssignmentDataProvider.class)
    public void setupExperiments(String startTime, String endTime, String type, String experimentCount) {
        String data = " {\"applicationName\": \"qbo\", \"label\": \"exp_" + AssignmentDataProvider.time + "_"
                + experimentCount + "\"," + "\"samplingPercent\": 0.67, \"startTime\": \"" + startTime + "\"," +
                "\"endTime\": \"" + endTime + "\", \"description\": \"Some hypothesis\"}";

        response = apiServerConnector.doPost("/experiments", data);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        Experiment experiment = ExperimentFactory.createFromJSONString(response.asString());
        ExperimentMeta meta = new ExperimentMeta();
        meta.setMeta(type);
        meta.setExperiment(experiment);
        experimentList.add(meta);
    }

    @Test(groups = {"setup"}, dependsOnMethods = {"setupExperiments"}, dataProvider = "ExperimentBucket",
            dataProviderClass = AssignmentDataProvider.class)
    public void setupBuckets(String bucketLabel, Object percent, Object isControl) {
        String data = " {\"label\": \"" + bucketLabel + "\", \"allocationPercent\": " + percent + ", \"isControl\": " + isControl
                + ", \"description\": \"" + bucketLabel + " bucket\",\"payload\": \"HTML-JS-" + bucketLabel + "\"}";
        for (ExperimentMeta experiment : experimentList) {
            String url = "experiments/" + experiment.getExperiment().id + "/buckets";
            response = apiServerConnector.doPost(url, data);
            assertReturnCode(response, HttpStatus.SC_CREATED);
        }
    }

    @Test(dependsOnGroups = {"setup"}, groups = {"stateTest"}, dataProvider = "ExperimentUsers",
            dataProviderClass = AssignmentDataProvider.class)
    public void t_draftStateAssignment(String user) {
        for (ExperimentMeta experimentMeta : experimentList) {
            response = apiServerConnector.doPut("/experiments/" + experimentMeta.getExperiment().id,
                    "{\"state\": \"DRAFT\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            String url = "assignments/applications/qbo/experiments/" + experimentMeta.getExperiment().label
                    + "/users/" + user;
            response = apiServerConnector.doGet(url);
            LOGGER.debug("State=DRAFT meta=" + experimentMeta.getMeta() + " status=" + response.getStatusCode()
                    + " response=" + response.asString());
            assertReturnCode(response, HttpStatus.SC_OK);
            Assert.assertEquals(response.asString().contains("EXPERIMENT_IN_DRAFT_STATE"), true);
        }
    }

    @Test(dependsOnGroups = {"setup"}, dependsOnMethods = {"t_draftStateAssignment"}, groups = {"stateTest"},
            dataProvider = "ExperimentUsers", dataProviderClass = AssignmentDataProvider.class)
    public void t_runningStateAssignment(String user) {
        for (ExperimentMeta experimentMeta : experimentList) {
            response = apiServerConnector.doPut("/experiments/" + experimentMeta.getExperiment().id,
                    "{\"state\": \"RUNNING\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            clearAssignmentsMetadataCache();

            String url = "assignments/applications/qbo/experiments/" + experimentMeta.getExperiment().label
                    + "/users/" + user;
            response = apiServerConnector.doGet(url);
            LOGGER.debug("State=RUNNING meta=" + experimentMeta.getMeta() + " status=" + response.getStatusCode()
                    + " response=" + response.asString());
            if ("future".equals(experimentMeta.getMeta())) {
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("EXPERIMENT_NOT_STARTED"), true);
            } else if ("past".equals(experimentMeta.getMeta())) {
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("EXPERIMENT_EXPIRED"), true);
            } else if ("present".equals(experimentMeta.getMeta())) {
                if (user.contains("dontCreate")) {
                    assertReturnCode(response, HttpStatus.SC_NOT_FOUND);
                    Assert.assertEquals(response.asString().contains("404"), true);
                } else {
                    assertReturnCode(response, HttpStatus.SC_OK);
                    Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
                    Assignment assignment = AssignmentFactory.createFromJSONString(response.asString());
                    Assert.assertEquals(null == assignment.assignment ||
                            "blue".equals(assignment.assignment) ||
                            "red".equals(assignment.assignment), true);
                }
            } else {
                //impossible case
                Assert.fail();
            }
        }
    }


    @Test(dependsOnGroups = {"setup"}, dependsOnMethods = {"t_draftStateAssignment", "t_runningStateAssignment"},
            groups = {"stateTest"},
            dataProvider = "ExperimentUsers", dataProviderClass = AssignmentDataProvider.class)
    public void t_pausedStateAssignment(String user) {
        for (ExperimentMeta experimentMeta : experimentList) {
            response = apiServerConnector.doPut("/experiments/" + experimentMeta.getExperiment().id,
                    "{\"state\": \"PAUSED\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            clearAssignmentsMetadataCache();

            String url = "assignments/applications/qbo/experiments/" + experimentMeta.getExperiment().label
                    + "/users/" + user;
            response = apiServerConnector.doGet(url);
            LOGGER.debug("State=PAUSED meta=" + experimentMeta.getMeta() + " status=" + response.getStatusCode()
                    + " response=" + response.asString());
            if ("future".equals(experimentMeta.getMeta())) {
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("EXPERIMENT_NOT_STARTED"), true);
            } else if ("past".equals(experimentMeta.getMeta())) {
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("EXPERIMENT_EXPIRED"), true);
            } else if ("present".equals(experimentMeta.getMeta())) {
                if (user.contains("dontCreate")) {
                    assertReturnCode(response, HttpStatus.SC_NOT_FOUND);
                    Assert.assertEquals(response.asString().contains("404"), true);
                } else {
                    assertReturnCode(response, HttpStatus.SC_OK);
                    Assert.assertEquals(response.asString().contains("EXISTING_ASSIGNMENT"), true);
                    Assignment assignment = AssignmentFactory.createFromJSONString(response.asString());
                    Assert.assertEquals(null == assignment.assignment ||
                            "blue".equals(assignment.assignment) ||
                            "red".equals(assignment.assignment), true);
                }
            } else {
                //impossible case
                Assert.fail();
            }
        }
    }

    @Test(dependsOnGroups = {"setup"}, groups = {"stateTest"},
            dependsOnMethods = {"t_draftStateAssignment", "t_runningStateAssignment", "t_pausedStateAssignment"},
            dataProvider = "ExperimentUsers", dataProviderClass = AssignmentDataProvider.class)
    public void t_runningAfterPausedStateAssignment(String user) {
        for (ExperimentMeta experimentMeta : experimentList) {
            response = apiServerConnector.doPut("/experiments/" + experimentMeta.getExperiment().id,
                    "{\"state\": \"RUNNING\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            clearAssignmentsMetadataCache();

            String url = "assignments/applications/qbo/experiments/" + experimentMeta.getExperiment().label
                    + "/users/" + user;
            response = apiServerConnector.doGet(url);
            LOGGER.info("State=RUNNING meta=" + experimentMeta.getMeta() + " status=" + response.getStatusCode()
                    + " response=" + response.asString());
            if ("future".equals(experimentMeta.getMeta())) {
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("EXPERIMENT_NOT_STARTED"), true);
            } else if ("past".equals(experimentMeta.getMeta())) {
                assertReturnCode(response, HttpStatus.SC_OK);
                Assert.assertEquals(response.asString().contains("EXPERIMENT_EXPIRED"), true);
            } else if ("present".equals(experimentMeta.getMeta())) {
                if (user.contains("dontCreate")) {
                    assertReturnCode(response, HttpStatus.SC_NOT_FOUND);
                    Assert.assertEquals(response.asString().contains("404"), true);
                } else {
                    assertReturnCode(response, HttpStatus.SC_OK);
                    Assert.assertEquals(response.asString().contains("EXISTING_ASSIGNMENT"), true);
                    Assignment assignment = AssignmentFactory.createFromJSONString(response.asString());
                    Assert.assertEquals(null == assignment.assignment ||
                            "blue".equals(assignment.assignment) ||
                            "red".equals(assignment.assignment), true);
                }
            } else {
                //impossible case
                Assert.fail();
            }
        }
    }

    @Test(dependsOnGroups = {"setup", "stateTest"}, groups = {"firstCall"})
    public void t_firstCallToPausedExperiment() {
        ExperimentMeta experimentMeta = experimentList.get(experimentList.size() - 1);
        response = apiServerConnector.doPut("/experiments/" + experimentMeta.getExperiment().id,
                "{\"state\": \"PAUSED\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        clearAssignmentsMetadataCache();

        String url = "assignments/applications/qbo/experiments/" + experimentMeta.getExperiment().label
                + "/users/anotheruser";
        response = apiServerConnector.doGet(url);
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("EXPERIMENT_PAUSED"), true);
        LOGGER.info("State=PAUSED meta=" + experimentMeta.getMeta() + " status=" + response.getStatusCode()
                + " response=" + response.asString());
        response = apiServerConnector.doGet(url + "?ignoreSamplingPercent=true");
        LOGGER.info("Ignore Sampling State=PAUSED meta=" + experimentMeta.getMeta() + " status=" + response.getStatusCode()
                + " response=" + response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("EXPERIMENT_PAUSED"), true);
        response = apiServerConnector.doPut("/experiments/" + experimentMeta.getExperiment().id,
                "{\"state\": \"RUNNING\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        clearAssignmentsMetadataCache();

        url = "assignments/applications/qbo/experiments/notanexistingexperiment/users/anotheruser";
        response = apiServerConnector.doGet(url);
        LOGGER.info("experiment not found State=PAUSED meta=" + experimentMeta.getMeta() + " status=" + response.getStatusCode()
                + " response=" + response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("EXPERIMENT_NOT_FOUND"), true);
    }


    @AfterClass
    public void t_cleanUp() {
        for (ExperimentMeta experiment : experimentList) {
            response = apiServerConnector.doPut("experiments/" + experiment.getExperiment().id, "{\"state\": \"RUNNING\"}");
            response = apiServerConnector.doPut("experiments/" + experiment.getExperiment().id, "{\"state\": \"TERMINATED\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            response = apiServerConnector.doDelete("experiments/" + experiment.getExperiment().id);
            assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        }
    }
}
