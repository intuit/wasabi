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

public class MutualExclusionBatchTest extends TestBase {
    private static final Logger LOGGER = getLogger(MutualExclusionBatchTest.class);
    private final List<Experiment> validExperimentsLists = new ArrayList<>();

    @Test(groups = {"setup"}, dataProvider = "mutexBatchExperimentSetup", dataProviderClass = SegmentationDataProvider.class)
    public void t_setupExperiment(String data) {
        LOGGER.debug(data);
        response = apiServerConnector.doPost("/experiments", data);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment experiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        validExperimentsLists.add(experiment);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        String redBucket = "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"\"}";
        String blueBucket = "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"\"}";
        response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", redBucket);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", blueBucket);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        response = apiServerConnector.doPut("/experiments/" + experiment.id, "{\"state\": \"RUNNING\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
    }

    @Test(dependsOnMethods = {"t_setupExperiment"}, groups = {"setup"})
    public void t_setMutualExclusion() {
        String mutex_experiments = "{\"experimentIDs\": [\"" + validExperimentsLists.get(1).id + "\",\"" +
                validExperimentsLists.get(2).id + "\"]}";
        LOGGER.debug("experiment mutex is " + mutex_experiments);
        response = apiServerConnector.doPost("/experiments/" + validExperimentsLists.get(0).id + "/exclusions",
                mutex_experiments);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }


    @Test(dependsOnGroups = {"setup"}, groups = {"test"})
    public void t_batchAssignmentWithMutexExclusion() {
        String url = "/assignments/applications/segmutexbatch_" + SegmentationDataProvider.time + "/users/user_0";
        String data = "{\"labels\": [\"batch_exp_label_" + SegmentationDataProvider.time + "_0\",\"batch_exp_label_"
                + SegmentationDataProvider.time + "_1\",\"batch_exp_label_" + SegmentationDataProvider.time + "_2\"]," +
                "\"profile\": {\"state\":\"CA\"}}";
        response = apiServerConnector.doPost(url, data);
        LOGGER.debug(response.asString());
        assertReturnCode(response, HttpStatus.SC_OK);
        Type listType = new TypeToken<Map<String, ArrayList<Assignment>>>() {
        }.getType();
        Map<String, List<Assignment>> batchAssignmentResult = new Gson().fromJson(response.asString(), listType);
        List<Assignment> assignments = batchAssignmentResult.get("assignments");
        Assert.assertEquals(assignments.get(0).status, "NEW_ASSIGNMENT");
        Assert.assertEquals(assignments.get(1).status, "NO_PROFILE_MATCH");
        Assert.assertEquals(assignments.get(2).status, "NO_PROFILE_MATCH");
        Assert.assertNotNull(assignments.get(0).assignment);
        Assert.assertNull(assignments.get(1).assignment);
        Assert.assertNull(assignments.get(2).assignment);
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
