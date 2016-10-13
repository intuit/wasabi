/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.tests.service.events;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.analytics.AnalyticsParameters;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_RUNNING;
import static com.intuit.wasabi.tests.library.util.Constants.NEW_ASSIGNMENT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Bucket integration tests
 */
public class EventsTest extends TestBase {

    private static final String TO_TIME = "toTime";
    private static final String FROM_TIME = "fromTime";
    private static final String QBO = "qbo";
    private static final String BLUE = "blue";
    private static final String RED = "red";
    private String yesterday;
    private String yesterdayMinus3;
    private String yesterdayMinus5;
    private String today;
    private String tomorrow;
    private String tomorrowPlus3;
    private String tomorrowPlus5;

    private Experiment experiment;
    private List<Bucket> buckets = new ArrayList<>();
    private String[] labels = {BLUE, RED};
    private double[] allocations = {.50, .50,};
    private boolean[] control = {false, true};
    private User userBill = UserFactory.createUser("Bill");
    private User userJane = UserFactory.createUser("Jane");
    private User userTom = UserFactory.createUser("Tom");

    private User[] users = {userBill, userJane, userTom};

    private String actionImpression = "IMPRESSION";
    private String actionClick = "click";
    private String actionLoveIt = "love it";
    private SimpleDateFormat dateFormat;


    /**
     * Initializes a default experiment.
     */
    public EventsTest() {
        setResponseLogLengthLimit(1000);
    }

    @BeforeClass
    public void setup() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        yesterday = dateFormat.format(cal.getTime());
        yesterday += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 0);
        today = dateFormat.format(cal.getTime());
        today += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        tomorrow = dateFormat.format(cal.getTime());
        tomorrow += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 3);
        tomorrowPlus3 = dateFormat.format(cal.getTime());
        tomorrowPlus3 += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3);
        yesterdayMinus3 = dateFormat.format(cal.getTime());
        yesterdayMinus3 += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 5);
        tomorrowPlus5 = dateFormat.format(cal.getTime());
        tomorrowPlus5 += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -5);
        yesterdayMinus5 = dateFormat.format(cal.getTime());
        yesterdayMinus5 += "T00:00:00+0000";

        experiment = ExperimentFactory.createExperiment();
        experiment.startTime = yesterdayMinus3;
        experiment.endTime = tomorrowPlus3;
        experiment.samplingPercent = 1.0;
        experiment.label = "experiment";
        experiment.applicationName = QBO + UUID.randomUUID();

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);
    }

    @Test(dependsOnGroups = {"ping"})
    public void createTwoBuckets() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
        buckets = BucketFactory.createCompleteBuckets(experiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);

        Assert.assertEquals(buckets, resultBuckets);

        for (Bucket result : resultBuckets) {
            Bucket matching = null;
            for (Bucket cand : buckets) {
                if (cand.label.equals(result.label)) {
                    matching = cand;
                    break;
                }

            }
            if (Objects.isNull(matching)) {
                Assert.fail("No matching bucket found.");
            }
            assertEquals(result.label, matching.label);
            assertEquals(result.isControl, matching.isControl);
            assertEquals(result.allocationPercent, matching.allocationPercent);
            assertEquals(result.description, matching.description);
        }
        experiment.state = EXPERIMENT_STATE_RUNNING;
        experiment = putExperiment(experiment);

    }

    @Test(dependsOnMethods = {"createTwoBuckets"})
    public void checkBasicCounts() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, "");
        AnalyticsParameters params = new AnalyticsParameters();

        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);
        assertEquals(events.size(), 0);
        System.out.println("Eents size" + events);

    }

    @Test(dependsOnMethods = {"checkBasicCounts"})
    public void postTestAssignments() {

        for (User user : users) {
            Assignment result = postAssignment(experiment, user, QBO);

            assertEquals(result.status, NEW_ASSIGNMENT);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, "");
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);
        assertEquals(events.size(), 0);
        for (Event event : events) {
            assertEquals(event.name, actionImpression);
        }
    }

    @Test(dependsOnMethods = {"postTestAssignments"})
    public void postImpressionWithoutName() {
        User user = userBill;
        Event event = EventFactory.createEvent();
        event.name = null;
        event.context = QBO;
        event.timestamp = yesterday;
        Response result = postEvent(event, experiment, user, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"postTestAssignments"})
    public void postImpressionWithPayload() {
        User user = userBill;
        Event event = EventFactory.createEvent();
        event.payload = "{\"testKey\":\"testValue\"}";
        event.context = QBO;
        event.timestamp = yesterday;
        Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
    }

    @Test(dependsOnMethods = {"postTestAssignments"})
    public void postImpressionAndLoveItWithPayload() {
        User user = userBill;
        Event event1 = EventFactory.createEvent();
        event1.payload = "{\"testKey1\":\"testValue1\"}";
        event1.context = QBO;
        event1.timestamp = yesterday;

        Event event2 = EventFactory.createEvent();
        event2.payload = "{\"testKey2\":\"testValue2\"}";
        event2.context = QBO;
        event2.value = actionLoveIt;
        event2.timestamp = yesterday;

        List<Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);

        Response result = postEvents(events, experiment, user, HttpStatus.SC_CREATED);
    }

    @Test(dependsOnMethods = {"postTestAssignments"})
    public void postImpressionWithBadDate() {
        User user = userBill;
        Event event = EventFactory.createEvent();
        event.name = null;
        event.context = QBO;
        event.timestamp = "garbage";
        Response result = postEvent(event, experiment, user, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"postTestAssignments"})
    public void postClickWithoutName() {
        User[] users = {userBill, userJane};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = QBO;
            event.name = null;
            event.timestamp = today;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_BAD_REQUEST);
            assertEquals(result.getStatusCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(dependsOnMethods = {"postTestAssignments"})
    public void postClickWithPayload() {
        User[] users = {userBill, userJane};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = QBO;
            event.payload = "{\"testKey\":\"testValue\"}";
            event.timestamp = today;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
    }

    @Test(dependsOnMethods = {"postImpressionAndLoveItWithPayload"})
    public void checkAllEventsFrom5DaysBefore() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TO_TIME, yesterdayMinus5);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 0);

    }

    @Test(dependsOnMethods = {"postImpressionAndLoveItWithPayload"})
    public void checkAllEventsFrom5DaysAfter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, tomorrowPlus5);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 0);
    }

    @Test(dependsOnMethods = {"postImpressionAndLoveItWithPayload"})
    public void checkAllEventsFrom3DaysBefore() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, yesterdayMinus3);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 5);

        List<String> users = new ArrayList<>();
        users.add(userBill.userID);
        users.add(userJane.userID);

        for (Event event : events) {
            assertTrue(users.contains(event.userId));
        }

    }

    @Test(dependsOnMethods = {"postTestAssignments"})
    public void postClickWithJSONPayloadBad() {
        String data = "{'events': [{'name': 'IMPRESSION','timestamp': '2013-07-02T08:23:45Z',"
                + "'payload': {'someKey':'someValue'}}]}";
        response = apiServerConnector.doPost("/events/applications/" + experiment.applicationName + "/experiments/experiment/users/Bill",
                data);
        assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"postTestAssignments"})
    public void postClickWithBadDate() {
        User[] users = {userBill, userJane};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = QBO;
            event.name = null;
            event.timestamp = "garbage" + today;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_BAD_REQUEST);
            assertEquals(result.getStatusCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }
}
