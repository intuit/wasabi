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

package com.intuit.wasabi.tests.service.assignment;

import com.intuit.wasabi.tests.data.AssignmentDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 5/16/16.
 */
public class ExportAssignment extends TestBase {
    static final Logger LOGGER = LoggerFactory.getLogger(BatchAssignment.class);
    Experiment experiment;

    @Test(groups = {"setup"}, dataProvider = "ExportAssignmentExperimentData", dataProviderClass = AssignmentDataProvider.class)
    public void t_setup(String experimentData) {
        response = apiServerConnector.doPost("/experiments", experimentData);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        experiment = ExperimentFactory.createFromJSONString(response.asString());
        String url = "/experiments/" + experiment.id + "/buckets";
        String bucketA = "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, " +
                "\"description\": \"Bucket red\",\"payload\": \"This is bucket red\"}";
        response = apiServerConnector.doPost(url, bucketA);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        String bucketB = "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, " +
                "\"description\": \"Bucket blue\",\"payload\": \"This is bucket blue\"}";
        response = apiServerConnector.doPost(url, bucketB);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"RUNNING\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
    }

    @Test(groups = {"assign"}, dependsOnGroups = {"setup"},
            dataProvider = "ExportAssignmentExperimentUser", dataProviderClass = AssignmentDataProvider.class)
    public void t_assign(String user, String event) {
        String url = "/assignments/applications/" + experiment.applicationName + "/experiments/" + experiment.label + "/users/" + user;
        response = apiServerConnector.doPost(url);
        LOGGER.debug("experiment not found status=" + response.getStatusCode()
                + " response=" + response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Assignment assignment = AssignmentFactory.createFromJSONString(response.asString());
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT");
        Assert.assertNotNull(assignment.assignment, "Assignment should not be null for: " + assignment);
    }

    @Test(groups = {"verify"}, dependsOnGroups = {"assign", "setup"})
    public void t_verify() {
        response = apiServerConnector.doGet("/experiments/" + experiment.id + "/assignments");
        LOGGER.debug("status=" + response.getStatusCode()
                + " response=" + response.asString());
        Map<String, String[]> result = new HashMap<>();
        int row = 0;
        for (String line : response.asString().split("\n")) {
            if (row != 0) {
                String[] parts = line.split("\t");
                result.put(parts[1], parts);
            }
            row++;
        }
        for (Object[] data : AssignmentDataProvider.ExportAssignmentExperimentUser()) {
            String user = (String) data[0];
            String[] parts = result.get(user);
            Assert.assertNotNull(parts);
            Assert.assertEquals(parts[0], experiment.id);
        }
        assertReturnCode(response, HttpStatus.SC_OK);
    }

    @AfterClass
    public void t_cleanUp() {
        response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        response = apiServerConnector.doDelete("experiments/" + experiment.id);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);

    }
}
