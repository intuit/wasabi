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
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;


public class PutAssignment extends TestBase {
    static final Logger LOGGER = LoggerFactory.getLogger(PutAssignment.class);
    Experiment experiment;
    String experimentLable;
    String appName;

    @Test(groups = {"setup"}, dataProvider = "PutAssignmentExperimentData", dataProviderClass = AssignmentDataProvider.class)
    public void t_setup(String appName, String label, String experimentData, String bucketLable, String bucketData) {
        response = apiServerConnector.doPost("/experiments", experimentData);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        experiment = ExperimentFactory.createFromJSONString(response.asString());
        String url = "/experiments/" + experiment.id + "/buckets";
        response = apiServerConnector.doPost(url, bucketData);
        assertReturnCode(response, HttpStatus.SC_CREATED);
        this.experimentLable = label;
        this.appName = appName;
    }


    @Test(dependsOnGroups = {"setup"}, groups = {"stateTest"},
            dataProvider = "PutAssignmentStates", dataProviderClass = AssignmentDataProvider.class)
    public void t_NullAssignmentToBucketAssignment(String state, int statusCode) {
        response = apiServerConnector.doPut("/experiments/" + experiment.id, "{\"state\": \"" + state + "\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        clearAssignmentsMetadataCache();

        String url = "/assignments/applications/" + this.appName + "/experiments/" + this.experimentLable + "/users/";
        if (!"DRAFT".equals(state) && !"TERMINATED".equals(state)) {
            assignUserStateFromNullToBucket(state, url);
            assignUserStateFromBucketToNull(state, url);
        } else {
            response = apiServerConnector.doPut(url + "user-" + state + "-1", "{\"assignment\": null}");
            Assert.assertEquals(response.getStatusCode(), statusCode);
            response = apiServerConnector.doPut(url + "user-" + state + "-2", "{\"assignment\": null}");
            Assert.assertEquals(response.getStatusCode(), statusCode);
        }
    }

    private void assignUserStateFromNullToBucket(String state, String url) {
        response = apiServerConnector.doPut(url + "user-" + state + "-1", "{\"assignment\": null}");
        LOGGER.info("State=" + state + " status=" + response.getStatusCode() + " response=" + response.asString());
        Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
        Assert.assertEquals(response.asString().contains("null"), true);

        response = apiServerConnector.doPut(url + "user-" + state + "-1", "{\"assignment\": \"onlybucket\", \"overwrite\":false}");
        assertReturnCode(response, HttpStatus.SC_CONFLICT);

        response = apiServerConnector.doGet(url + "user-" + state + "-1");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("EXISTING_ASSIGNMENT"), true);

        response = apiServerConnector.doPut(url + "user-" + state + "-1", "{\"assignment\": \"onlybucket\", \"overwrite\":true}");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
        Assert.assertEquals(response.asString().contains("onlybucket"), true);
        LOGGER.debug("State=" + state + " status=" + response.getStatusCode() + " response=" + response.asString());
    }

    private void assignUserStateFromBucketToNull(String state, String url) {
        response = apiServerConnector.doPut(url + "user-" + state + "-2", "{\"assignment\": \"onlybucket\"}");
        LOGGER.debug("State=" + state + " status=" + response.getStatusCode() + " response=" + response.asString());
        Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
        Assert.assertEquals(response.asString().contains("onlybucket"), true);

        response = apiServerConnector.doPut(url + "user-" + state + "-2", "{\"assignment\": null, \"overwrite\":false}");
        assertReturnCode(response, HttpStatus.SC_CONFLICT);

        response = apiServerConnector.doGet(url + "user-" + state + "-2");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("EXISTING_ASSIGNMENT"), true);

        response = apiServerConnector.doPut(url + "user-" + state + "-2", "{\"assignment\": null, \"overwrite\":true}");
        assertReturnCode(response, HttpStatus.SC_OK);
        Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
        Assert.assertEquals(response.asString().contains("null"), true);
        LOGGER.debug("State=" + state + " status=" + response.getStatusCode() + " response=" + response.asString());
    }

    @AfterClass
    public void t_cleanUp() {
        response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        response = apiServerConnector.doDelete("experiments/" + experiment.id);
        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
    }

}
