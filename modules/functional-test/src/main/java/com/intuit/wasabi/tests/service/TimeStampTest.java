/*
# Copyright 2016 Intuit
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###############################################################################

#
# Integration tests (client-side) of timestamp
# Requires the full stack to be running: service, DB, Cassandra, log uploader
#
*/

package com.intuit.wasabi.tests.service;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.analytics.AnalyticsParameters;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.service.segmentation.BatchRuleTest;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.tests.model.factory.ExperimentFactory.createFromJSONString;
import static com.intuit.wasabi.tests.model.factory.UserFactory.createUser;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The TimeStamp test creates an experiment and bucket, assigns a user, and checks to see if the correct
 * amount of impressions are being created when using different formats of timestamps at various times.
 */
public class TimeStampTest extends TestBase {
    private static final Logger LOGGER = getLogger(BatchRuleTest.class);

    private static final String COUNT_ERROR_MESSAGE = "Impression count does not match count of timestamps";
    private static final String TIMING_ERROR_MESSAGE = "Found impressions after expected time";
    private static final String IMPRESSION = "IMPRESSION";
    private static final String TIMESTAMP_USER = "timestamp_user";

    private static final String EXPERIMENT_START = "2013-01-01T00:00:00+0000";
    private static final String TIME_AFTER_IMPRESSIONS = "2013-09-21T10:00:00Z";
    private static final String TIME_BEFORE_IMPRESSIONS = "2013-09-21T08:00:00Z";
    private static final String TOMORROW = TestUtils.relativeTimeString(1);

    private static final List<Experiment> validExperimentsLists = new ArrayList<>();
    private static final List<String> impressionAction = new ArrayList<>();
    private static final Map<User, Assignment> assignments = new HashMap<>();

    private static User outUser = null;
    private static List<String> timeStamps;
    private static AnalyticsParameters params;

    @DataProvider
    public Object[][] sampleExperiment() {
        String label = "timestampTest_" + System.currentTimeMillis();
        Application application = new Application("Wasabi_timestamp_test");
        String startTime = EXPERIMENT_START;
        String endTime = TOMORROW;
        Double samplingPercent = 1.0;
        Experiment experiment = new Experiment(label, application, startTime, endTime, samplingPercent);
        return new Object[][]{
                new Object[]{experiment}
        };
    }

    @Test(dataProvider = "sampleExperiment")
    public void setupExperiment(Experiment experimentData) {
        LOGGER.debug(format("posting experiment: %s", experimentData.toString()));
        response = apiServerConnector.doPost("/experiments", experimentData);
        Experiment experiment = createFromJSONString(response.jsonPath().prettify());
        validExperimentsLists.add(experiment);
        assertThat(response.getStatusCode(), is(CREATED.getStatusCode()));
    }

    @Test(dependsOnMethods = {"setupExperiment"})
    public void setupBucketsAndStartExperiment() {
        // Create bucket with 100% allocation and change experiment state to running
        String greenBucket = "{\"label\": \"green\", \"allocationPercent\": 1.0, " +
                "\"isControl\": true, \"description\": \"Green buy button\"}";
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", greenBucket);
            assertThat(response.getStatusCode(), is(CREATED.getStatusCode()));
            response = apiServerConnector.doPut("/experiments/" + experiment.id, "{\"state\": \"RUNNING\"}");
            assertThat(response.getStatusCode(), is(OK.getStatusCode()));
        }
    }

    @Test
    public void setupImpressionAction() {
        impressionAction.add(IMPRESSION);
    }

    @Test(dependsOnMethods = {"setupImpressionAction"})
    public void setupParams() {
        // Setup params for queries with different forms of timestamp to test time parsing in Analytics API as well
        params = new AnalyticsParameters();
        params.actions = impressionAction;
        params.confidenceLevel = 0.999d;
        params.toTime = TOMORROW;
    }

    @Test
    public void setupTimestamps() {
        // Create identical timestamps in different formats
        String prefix = "2013-09-21T09:00:00";
        List<String> offSets = new ArrayList<>();
        offSets.add("Z");
        offSets.add("+0000");
        offSets.add("-0000");
        timeStamps = new ArrayList<>();
        for (String offSet : offSets) {
            timeStamps.add(prefix + offSet);
        }
        // Create another, equal timestamp in PDT
        timeStamps.add("2013-09-21T02:00:00-0700");
    }

    @Test
    public void setupUser() {
        // Create fake user
        String username = TIMESTAMP_USER;
        outUser = createUser(username);
    }

    // Ensures that the correct amount of impressions are being registered with respect to the timestamp and experiment
    @Test(dependsOnMethods = {"setupBucketsAndStartExperiment", "setupParams", "setupTimestamps", "setupUser"})
    public void gatherImpressions() throws InterruptedException {
        int impressionsCount;
        for (Experiment experiment : validExperimentsLists) {

            // Assign user to experiment
            Assignment assignment = getAssignment(experiment, outUser);
            assignments.put(outUser, assignment);
            for (Assignment assignment1 : assignments.values()) {
                assertThat("Assignment status wrong", assignment1.status, is("NEW_ASSIGNMENT"));
                assertThat("Assignment.cache not true.", assignment1.cache, is(true));
            }

            // Post the impressions
            for (String timestamp : timeStamps) {
                Event event = EventFactory.createEvent();
                event.name = IMPRESSION;
                event.timestamp = timestamp;
                response = postEvent(event, experiment, outUser, CREATED.getStatusCode());
                assertThat(response.getStatusCode(), is(CREATED.getStatusCode()));
            }

            // Check that impressions were posted to experiment
            Map<String, Object> data = new HashMap<>();
            data.put("fromTime", "");
            List<Event> events = postEvents(experiment, data, true, HttpStatus.SC_OK, apiServerConnector);
            assertThat(events.size(), is(timeStamps.size()));
            for (Event event : events) assertThat(event.name, is(IMPRESSION));

            // Check impression count starting at experiment start time
            params.fromTime = EXPERIMENT_START;
            impressionsCount = postExperimentCounts(experiment, params).impressionCounts.eventCount;
            LOGGER.info("\t\timpressions = " + impressionsCount + ", len(timestamps) = " + timeStamps.size());
            assertThat(COUNT_ERROR_MESSAGE, impressionsCount, is(timeStamps.size()));

            // Check impression count after experiment start time but before impression time
            params.fromTime = TIME_BEFORE_IMPRESSIONS;
            impressionsCount = postExperimentCounts(experiment, params).impressionCounts.eventCount;
            LOGGER.info("\t\tqueryTime = " + TIME_BEFORE_IMPRESSIONS + ", impressions = " + impressionsCount +
                    ", len(timestamps) = " + timeStamps.size());
            assertThat(COUNT_ERROR_MESSAGE, impressionsCount, is(timeStamps.size()));

            // Check impression count after impression time
            params.fromTime = TIME_AFTER_IMPRESSIONS;
            impressionsCount = postExperimentCounts(experiment, params).impressionCounts.eventCount;
            LOGGER.info("\t\tqueryTime = " + TIME_AFTER_IMPRESSIONS + ", impressions = " + impressionsCount +
                    ", len(timestamps) = " + timeStamps.size());
            assertThat(TIMING_ERROR_MESSAGE, impressionsCount, is(0));
        }
    }

    @AfterClass
    public void cleanUp() {
        LOGGER.info("Setting experiment state to terminated and deleting valid experiments");
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
            assertThat(response.getStatusCode(), is(OK.getStatusCode()));
            response = apiServerConnector.doDelete("experiments/" + experiment.id);
            assertThat(response.getStatusCode(), is(NO_CONTENT.getStatusCode()));
        }
    }
}