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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intuit.wasabi.tests.data.AssignmentDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Assignment;
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

/**
 * Created on 5/16/16.
 */
public class BatchAssignment extends TestBase {
    static final Logger LOGGER = LoggerFactory.getLogger(BatchAssignment.class);
    List<Experiment> experimentList = new ArrayList<>();
    List<String> experimentLabels = new ArrayList<>();
//    List<Experiment> experimentBadList = new ArrayList<>();
//    List<String> experimentBadLabels = new ArrayList<>();

    @Test(groups = {"setup"}, dataProvider = "BatchAssignmentExperimentData", dataProviderClass = AssignmentDataProvider.class)
    public void t_setup(String experimentData) {
        response = apiServerConnector.doPost("/experiments", experimentData);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        Experiment experiment = ExperimentFactory.createFromJSONString(response.asString());
        experimentList.add(experiment);
        experimentLabels.add(experiment.label);
        String url = "/experiments/" + experiment.id + "/buckets";
        String bucketA = "{\"label\": \"A\", \"allocationPercent\": 0.5, \"isControl\": true, " +
                "\"description\": \"Bucket A\",\"payload\": \"This is bucket A\"}";
        response = apiServerConnector.doPost(url, bucketA);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        String bucketB = "{\"label\": \"B\", \"allocationPercent\": 0.5, \"isControl\": false, " +
                "\"description\": \"Bucket B\",\"payload\": \"This is bucket B\"}";
        response = apiServerConnector.doPost(url, bucketB);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }

    //TODO: the current python test have commented out due to not working tests
//    @Test(groups={"setup"}, dataProvider = "BatchAssignmentBadExperimentData", dataProviderClass = AssignmentDataProvider.class)
//    public void t_setupBadExperiment(String experimentData){
//        response = apiServerConnector.doPost("/experiments", experimentData);
//        assertReturnCode(response, HttpStatus.SC_CREATED);
//        Experiment experiment = ExperimentFactory.createFromJSONString(response.asString());
//        experimentBadList.add(experiment);
//        experimentBadLabels.add(experiment.label);
//        String url = "/experiments/"+experiment.id+"/buckets";
//        String bucketA = "{\"label\": \"Red\", \"allocationPercent\": 0.5, \"isControl\": false, " +
//                "\"description\": \"Bucket Red\",\"payload\": \"This is bucket Red\"}";
//        response = apiServerConnector.doPost(url, bucketA);
//        assertReturnCode(response, HttpStatus.SC_CREATED);
//        String bucketB = "{\"label\": \"Blue\", \"allocationPercent\": 0.5, \"isControl\": false, " +
//                "\"description\": \"Bucket Blue\",\"payload\": \"This is bucket Blue\"}";
//        response = apiServerConnector.doPost(url, bucketB);
//        assertReturnCode(response, HttpStatus.SC_CREATED);
//        response = apiServerConnector.doPut("experiments/"+experiment.id, "{\"state\": \"RUNNING\"}");
//        assertReturnCode(response, HttpStatus.SC_OK);
//    }

    @Test(groups = {"batchAssign"}, dependsOnGroups = {"setup"},
            dataProvider = "BatchAssignmentStateAndExpectedValues", dataProviderClass = AssignmentDataProvider.class)
    public void t_batchAssign(String state, boolean isAssignment, String status) {
        clearAssignmentsMetadataCache();

        String lables = "{\"labels\": [" + experimentLabels.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]}";
        String url = "/assignments/applications/testBatch/users/batchUser";
        for (Experiment experiment : experimentList) {
            response = apiServerConnector.doPut("/experiments/" + experiment.id, "{\"state\": \"" + state + "\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
        }
        response = apiServerConnector.doPost(url, lables);
        LOGGER.debug("experiment not found State=" + state + " status=" + response.getStatusCode()
                + " response=" + response.asString());
        Type listType = new TypeToken<Map<String, ArrayList<Assignment>>>() {
        }.getType();
        Map<String, List<Assignment>> batchAssignmentResult = new Gson().fromJson(response.asString(), listType);
        for (Assignment assignment : batchAssignmentResult.get("assignments")) {
            if (isAssignment) {
                Assert.assertNotNull(assignment.assignment, "Assignment should not be null for: " + assignment);
            } else {
                Assert.assertNull(assignment.assignment, "Assignment should be null for: " + assignment);
            }
            Assert.assertEquals(assignment.status, status);
        }
    }


    @AfterClass
    public void t_cleanUp() {
        for (Experiment experiment : experimentList) {
            response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"RUNNING\"}");
            response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            response = apiServerConnector.doDelete("experiments/" + experiment.id);
            assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        }
    }

}
