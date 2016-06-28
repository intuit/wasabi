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
import com.intuit.wasabi.tests.model.*;
import com.intuit.wasabi.tests.model.analytics.AnalyticsParameters;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.service.segmentation.BatchRuleTest;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.tests.model.factory.UserFactory.createUser;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertEquals;

/**
 * The TimeStamp test.
 */
public class TimeStampTest extends TestBase {
    private static final Logger LOGGER = getLogger(BatchRuleTest.class);
    public static final String COUNT_ERROR_MESSAGE = "Impression count does not match count of timestamps";
    private final List<Experiment> validExperimentsLists = new ArrayList<>();
    private Map<User, Assignment> assignments = new HashMap<>();

    @DataProvider
    public Object[][] sampleExperiment() {
        String label = "timestampTest_" + System.currentTimeMillis();
        Application application = new Application("LUA_timestamp");
        String startTime = "2013-01-01T00:00:00+0000";
        String endTime = TestUtils.relativeTimeString(1);
        Double samplingPercent = 1.0;
        Experiment experiment = new Experiment(label, application, startTime, endTime, samplingPercent);
        return new Object[][]{
                new Object[]{experiment}
        };
    }

    @Test(dataProvider = "sampleExperiment")
    public void setupExperiment(Experiment experimentData) {
        LOGGER.debug(experimentData.toString());
        response = apiServerConnector.doPost("/experiments", experimentData);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment experiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        System.out.println("This response is equal to " + response.jsonPath().prettify());
        validExperimentsLists.add(experiment);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }

    @Test(dependsOnMethods = {"setupExperiment"})
    public void setupBucketsAndStartExperiment() {
        // Create bucket with 100% allocation and change experiment state to running
        String greenBucket = "{\"label\": \"green\", \"allocationPercent\": 1.0, \"isControl\": true, \"description\": \"Green buy button\"}";
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doPost("/experiments/" + experiment.id + "/buckets", greenBucket);
            assertReturnCode(response, HttpStatus.SC_CREATED);
            response = apiServerConnector.doPut("/experiments/" + experiment.id, "{\"state\": \"RUNNING\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
        }
    }

    @Test(dependsOnMethods = {"setupBucketsAndStartExperiment"})
    public void timeStampTest() throws InterruptedException {
        // Create fake user
        String username = "timestamp_user";
        User outUser = createUser(username);

        // Create identical timestamps in different formats
        String prefix = "2013-09-21T09:00:00";
        List<String> offSets = new ArrayList<>();
        offSets.add("Z");
        offSets.add("+0000");
        offSets.add("-0000");
        List<String> timeStamps = new ArrayList<>();
        for (String offSet : offSets) {
            timeStamps.add(prefix + offSet);
        }

        // Create another, equal timestamp in PDT
        timeStamps.add("2013-09-21T02:00:00-0700");

        for (Experiment experiment : validExperimentsLists) {

            // Assign user to experiment
            Assignment assignment = getAssignment(experiment, outUser);
            assignments.put(outUser, assignment);
            for (Assignment assignment1 : assignments.values()) {
                Assert.assertEquals(assignment1.status, "NEW_ASSIGNMENT", "Assignment status wrong.");
                Assert.assertTrue(assignment1.cache, "Assignment.cache not true.");
            }

            // Post the impressions
            for (String ts : timeStamps) {
                String actionImpression = "IMPRESSION";
                Event event = EventFactory.createEvent();
                event.name = actionImpression;
                event.timestamp = ts;
                response = postEvent(event, experiment, outUser, HttpStatus.SC_CREATED);
                assertReturnCode(response, HttpStatus.SC_CREATED);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("fromTime", "");
            List<Event> events = postEvents(experiment,
                    data, true,
                    HttpStatus.SC_OK, apiServerConnector);
            assertEquals(events.size(), timeStamps.size());
            String actionImpression1 = "IMPRESSION";
            for (Event event : events) {
                assertEquals(event.name, actionImpression1);
            }
            System.out.println("\tWaiting (5s) for database to update...");
            Thread.sleep(5000);

            // Query with different forms of timestamp to test time parsing in Analytics API as well
            List<String> types = new ArrayList<>();
            types.add(actionImpression1);
            AnalyticsParameters params = new AnalyticsParameters();
            params.confidenceLevel = 0.999d;
            params.actions = types;
            params.fromTime = experiment.startTime;
            params.toTime = experiment.endTime;
            int impressionsCount;

            // Impression count at time = timestamp
            impressionsCount = postExperimentCounts(experiment, params).impressionCounts.eventCount;
            LOGGER.info("\t\timpressions = " + impressionsCount + ", len(timestamps) = " + timeStamps.size());
            Assert.assertEquals(impressionsCount, timeStamps.size(), COUNT_ERROR_MESSAGE);

            // Impression count before time = timestamp
            String queryTime;
            queryTime = "2013-09-21T08:00:00Z";
            params.fromTime = queryTime;
            impressionsCount = postExperimentCounts(experiment, params).impressionCounts.eventCount;
            LOGGER.info("\t\tqueryTime = " + queryTime + ", impressions = " + impressionsCount + ", len(timestamps) = " + timeStamps.size());
            Assert.assertEquals(impressionsCount, timeStamps.size(), COUNT_ERROR_MESSAGE);

            // Impression count after time = timestamp
            queryTime = "2013-09-21T10:00:00Z";
            params.fromTime = queryTime;
            impressionsCount = postExperimentCounts(experiment, params).impressionCounts.uniqueUserCount;
            System.out.println("ImpressionCountsTest = " + impressionsCount);
            LOGGER.info("\t\tqueryTime = " + queryTime + ", impressions = " + impressionsCount + ", len(timestamps) = " + timeStamps.size());
            Assert.assertEquals(impressionsCount, 0, "Found impressions after time surpassed timestamp");
        }
    }

    @AfterClass
    public void cleanUp() {
        LOGGER.info("Setting experiment state to terminated and deleting valid experiments");
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
            response = apiServerConnector.doDelete("experiments/" + experiment.id);
            assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        }
    }
}