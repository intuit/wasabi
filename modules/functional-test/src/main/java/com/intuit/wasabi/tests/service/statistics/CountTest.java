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
package com.intuit.wasabi.tests.service.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.analytics.AnalyticsParameters;
import com.intuit.wasabi.tests.model.analytics.ExperimentCounts;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_RUNNING;
import static com.intuit.wasabi.tests.library.util.Constants.NEW_ASSIGNMENT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Bucket integration tests
 */
public class CountTest extends TestBase {

    private static final String TO_TIME = "toTime";
    private static final String FROM_TIME = "fromTime";
    private static final String QBO = "qbo";
    private static final String BLUE = "blue";
    private static final String RED = "red";
    private String yesterday;
    private String yesterdayMinus3;
    private String today;
    private String tomorrow;
    private String tomorrowPlus3;

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
    private String tomorrowPlus2;
    private String tomorrowPlus1;

    @BeforeClass
    public void setup() {
        setResponseLogLengthLimit(1000);

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
        cal.add(Calendar.DATE, 4);
        tomorrowPlus3 = dateFormat.format(cal.getTime());
        tomorrowPlus3 += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 3);
        tomorrowPlus2 = dateFormat.format(cal.getTime());
        tomorrowPlus2 += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        tomorrowPlus1 = dateFormat.format(cal.getTime());
        tomorrowPlus1 += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -4);
        yesterdayMinus3 = dateFormat.format(cal.getTime());
        yesterdayMinus3 += "T00:00:00+0000";

        experiment = ExperimentFactory.createExperiment();
        experiment.startTime = yesterday;
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

        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);
        assertEquals(events.size(), 0);
        System.out.println("Eents size" + events);

    }

    @Test(dependsOnMethods = {"checkBasicCounts"})
    public void postAssignments() {
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

    @Test(dependsOnMethods = {"postAssignments"})
    public void postImpressions() {
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = QBO;
            event.name = actionImpression;
            event.timestamp = yesterday;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, "");
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);
        assertEquals(events.size(), 3);
        for (Event event : events) {
            assertEquals(event.name, actionImpression);
        }

    }

    @Test(dependsOnMethods = {"postImpressions"})
    public void postClick() {
        User[] users = {userBill, userJane};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = QBO;
            event.name = actionClick;
            event.timestamp = today;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, today);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 2);
        for (Event event : events) {
            assertEquals(event.name, actionClick);
        }
    }

    @Test(dependsOnMethods = {"postImpressions"})
    public void postClickTommorowPlus2() {
        User[] users = {userBill, userJane};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = QBO;
            event.name = actionClick;
            event.timestamp = tomorrowPlus2;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, today);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 4);
        for (Event event : events) {
            assertEquals(event.name, actionClick);
        }

    }

    @Test(dependsOnMethods = {"postClick"})
    public void postLoveIt() {
        User[] users = {userJane, userTom};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = QBO;
            event.name = actionLoveIt;
            event.timestamp = tomorrow;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, tomorrow);
        parameters.put(TO_TIME, tomorrowPlus1);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 2);
        for (Event event : events) {
            assertEquals(event.name, actionLoveIt);
        }

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEvents() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, "");
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 9);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 3);
        assertEquals(eventClick, 4);
        assertEquals(eventLoveIt, 2);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEventsYestedayOnwards() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = yesterday;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(3, counts.impressionCounts.eventCount);
        assertEquals(3, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(6, counts.jointActionCounts.eventCount);
        assertEquals(3, counts.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(6,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        assertEquals(2, counts.actionCounts.get(actionLoveIt).eventCount);
        assertEquals(2, counts.actionCounts.get(actionLoveIt).uniqueUserCount);
        assertEquals(4, counts.actionCounts.get(actionClick).eventCount);
        assertEquals(2, counts.actionCounts.get(actionClick).uniqueUserCount);

        int redClickActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;
        assertEquals(4, redClickActionCount + blueClickActionCount);
        assertEquals(2, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(2, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(2, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkClickEventsYestedayOnwards() {
        List<String> types = new ArrayList<>();
        types.add(actionClick);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = yesterday;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(3, counts.impressionCounts.eventCount);
        assertEquals(3, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(4, counts.jointActionCounts.eventCount);
        assertEquals(2, counts.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(4,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(2,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        assertEquals(null, counts.actionCounts.get(actionLoveIt));
        assertEquals(null, counts.actionCounts.get(actionLoveIt));
        assertEquals(4, counts.actionCounts.get(actionClick).eventCount);
        assertEquals(2, counts.actionCounts.get(actionClick).uniqueUserCount);

        int redClickActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;
        assertEquals(4, redClickActionCount + blueClickActionCount);
        assertEquals(2, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(0, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(0, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkLoveItEventsYestedayOnwards() {
        List<String> types = new ArrayList<>();
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = yesterday;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(3, counts.impressionCounts.eventCount);
        assertEquals(3, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(2, counts.jointActionCounts.eventCount);
        assertEquals(2, counts.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(2,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(2,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        assertEquals(null, counts.actionCounts.get(actionClick));
        assertEquals(null, counts.actionCounts.get(actionClick));
        assertEquals(2, counts.actionCounts.get(actionLoveIt).eventCount);
        assertEquals(2, counts.actionCounts.get(actionLoveIt).uniqueUserCount);

        int redClickActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(0, redClickActionCount + blueClickActionCount);
        assertEquals(0, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(2, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(2, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEventsYestedayOnwardsOnlyImpressions() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = yesterday;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(3, counts.impressionCounts.eventCount);
        assertEquals(3, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(0, counts.jointActionCounts.eventCount);
        assertEquals(0, counts.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(0,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(0,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        assertEquals(0, Objects.isNull(counts.actionCounts.get(actionLoveIt)) ? 0 : counts.actionCounts.get(actionLoveIt).eventCount);
        assertEquals(0, Objects.isNull(counts.actionCounts.get(actionLoveIt)) ? 0 : counts.actionCounts.get(actionLoveIt).uniqueUserCount);
        assertEquals(0, Objects.isNull(counts.actionCounts.get(actionClick)) ? 0 : counts.actionCounts.get(actionClick).eventCount);
        assertEquals(0, Objects.isNull(counts.actionCounts.get(actionClick)) ? 0 : counts.actionCounts.get(actionClick).uniqueUserCount);

        int redClickActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(0, redClickActionCount + blueClickActionCount);
        assertEquals(0, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(0, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(0, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEventsYestedayOnly() {
        List<String> types = new ArrayList<>();
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = yesterday;
        params.toTime = yesterday;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(3, counts.impressionCounts.eventCount);
        assertEquals(3, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(0, counts.jointActionCounts.eventCount);
        assertEquals(0, counts.jointActionCounts.uniqueUserCount);
        assertEquals(3,

                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(0,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(0,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        assertEquals(0, Objects.isNull(counts.actionCounts.get(actionLoveIt)) ? 0 : counts.actionCounts.get(actionLoveIt).eventCount);
        assertEquals(0, Objects.isNull(counts.actionCounts.get(actionLoveIt)) ? 0 : counts.actionCounts.get(actionLoveIt).uniqueUserCount);
        assertEquals(0, Objects.isNull(counts.actionCounts.get(actionClick)) ? 0 : counts.actionCounts.get(actionClick).eventCount);
        assertEquals(0, Objects.isNull(counts.actionCounts.get(actionClick)) ? 0 : counts.actionCounts.get(actionClick).uniqueUserCount);

        int redClickActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(0, redClickActionCount + blueClickActionCount);
        assertEquals(0, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;
        assertEquals(0, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(0, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEventsTodayOnly() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = 0.99d;
        params.actions = types;
        params.fromTime = today;
        params.toTime = today;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(0, counts.impressionCounts.eventCount);
        assertEquals(0, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(2, counts.jointActionCounts.eventCount);
        assertEquals(2, counts.jointActionCounts.uniqueUserCount);
        assertEquals(0,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);
        assertEquals(2,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(2,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        assertEquals(null, counts.actionCounts.get(actionLoveIt));
        assertEquals(null, counts.actionCounts.get(actionLoveIt));
        assertEquals(2, counts.actionCounts.get(actionClick).eventCount);
        assertEquals(2, counts.actionCounts.get(actionClick).uniqueUserCount);

        int redClickActionCount = 0;

        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(2, redClickActionCount + blueClickActionCount);
        assertEquals(2, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(0, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(0, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);
    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEventsTomorrowOnly() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = 0.99d;
        params.actions = types;
        params.fromTime = tomorrow;
        params.toTime = tomorrow;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(0, counts.impressionCounts.eventCount);
        assertEquals(0, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(2, counts.jointActionCounts.eventCount);
        assertEquals(2, counts.jointActionCounts.uniqueUserCount);
        assertEquals(0,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);
        assertEquals(2,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(2,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);
        assertEquals(2, counts.actionCounts.get(actionLoveIt).eventCount);
        assertEquals(2, counts.actionCounts.get(actionLoveIt).uniqueUserCount);
        assertEquals(null, counts.actionCounts.get(actionClick));
        assertEquals(null, counts.actionCounts.get(actionClick));

        int redClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;
        assertEquals(0, redClickActionCount + blueClickActionCount);
        assertEquals(0, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;
        assertEquals(2, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(2, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);
    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEventsYesterdayOnly() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = 0.99d;
        params.actions = types;
        params.fromTime = yesterday;
        params.toTime = yesterday;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(3, counts.impressionCounts.eventCount);
        assertEquals(3, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(0, counts.jointActionCounts.eventCount);
        assertEquals(0, counts.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);
        assertEquals(0,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(0,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);
        assertEquals(null, counts.actionCounts.get(actionLoveIt));
        assertEquals(null, counts.actionCounts.get(actionLoveIt));
        assertEquals(null, counts.actionCounts.get(actionClick));
        assertEquals(null, counts.actionCounts.get(actionClick));

        int redClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;
        assertEquals(0, redClickActionCount + blueClickActionCount);
        assertEquals(0, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;
        assertEquals(0, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(0, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);
    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEventsTodayOnWards() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = 0.99d;
        params.actions = types;
        params.fromTime = today;

        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(0, counts.impressionCounts.eventCount);
        assertEquals(0, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(6, counts.jointActionCounts.eventCount);
        assertEquals(3, counts.jointActionCounts.uniqueUserCount);
        assertEquals(0,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(6,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);
        assertEquals(2, counts.actionCounts.get(actionLoveIt).eventCount);
        assertEquals(2, counts.actionCounts.get(actionLoveIt).uniqueUserCount);
        assertEquals(4, counts.actionCounts.get(actionClick).eventCount);
        assertEquals(2, counts.actionCounts.get(actionClick).uniqueUserCount);

        int redClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;
        assertEquals(4, redClickActionCount + blueClickActionCount);
        assertEquals(2, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;
        assertEquals(2, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(2, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);
    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkAllEventsTomorrowOnWards() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = 0.99d;
        params.actions = types;
        params.fromTime = tomorrow;
        params.context = QBO;
        ExperimentCounts counts = postExperimentCounts(experiment, params);

        assertEquals(0, counts.impressionCounts.eventCount);
        assertEquals(0, counts.impressionCounts.uniqueUserCount);
        assertEquals(2, counts.buckets.size());
        assertEquals(4, counts.jointActionCounts.eventCount);
        assertEquals(3, counts.jointActionCounts.uniqueUserCount);
        assertEquals(0,
                counts.buckets.get(RED).impressionCounts.eventCount +
                        counts.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                counts.buckets.get(RED).impressionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(4,
                counts.buckets.get(RED).jointActionCounts.eventCount +
                        counts.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(3,
                counts.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        counts.buckets.get(BLUE).jointActionCounts.uniqueUserCount);
        assertEquals(2, counts.actionCounts.get(actionLoveIt).eventCount);
        assertEquals(2, counts.actionCounts.get(actionLoveIt).uniqueUserCount);
        assertEquals(2, Objects.isNull(counts.actionCounts.get(actionClick)) ? 0 :
                counts.actionCounts.get(actionClick).eventCount);
        assertEquals(2, Objects.isNull(counts.actionCounts.get(actionClick)) ? 0 :
                counts.actionCounts.get(actionClick).uniqueUserCount);

        int redClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickActionCount = counts.buckets.get(RED).actionCounts.get(actionClick).eventCount;

        int redClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionClick)))
            redClickUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionClick).uniqueUserCount;

        int blueClickActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickActionCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).eventCount;

        int blueClickUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionClick)))
            blueClickUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionClick).uniqueUserCount;
        assertEquals(2, redClickActionCount + blueClickActionCount);
        assertEquals(2, redClickUniqueUserCount + blueClickUniqueUserCount);

        int redLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItActionCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).eventCount;

        int redLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(RED).actionCounts.get(actionLoveIt)))
            redLoveItUniqueUserCount = counts.buckets.get(RED).actionCounts.get(actionLoveIt).uniqueUserCount;

        int blueLoveItActionCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItActionCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).eventCount;

        int blueLoveItUniqueUserCount = 0;
        if (Objects.nonNull(counts.buckets.get(BLUE).actionCounts.get(actionLoveIt)))
            blueLoveItUniqueUserCount = counts.buckets.get(BLUE).actionCounts.get(actionLoveIt).uniqueUserCount;
        assertEquals(2, redLoveItActionCount + blueLoveItActionCount);
        assertEquals(2, redLoveItUniqueUserCount + blueLoveItUniqueUserCount);
    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAllEventFromYesterday() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, yesterday);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 9);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 3);
        assertEquals(eventClick, 4);
        assertEquals(eventLoveIt, 2);

    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAllEventFromToday() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, today);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 6);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 0);
        assertEquals(eventClick, 4);
        assertEquals(eventLoveIt, 2);

    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAllEventFromYesterdayMinus3() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, yesterdayMinus3);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 9);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 3);
        assertEquals(eventClick, 4);
        assertEquals(eventLoveIt, 2);

    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAllEventToTomorrowPLus3() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TO_TIME, tomorrowPlus3);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 9);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 3);
        assertEquals(eventClick, 4);
        assertEquals(eventLoveIt, 2);

    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAllEventFromYesterdayMinus3ToTomorrowPLus3() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TO_TIME, tomorrowPlus3);
        parameters.put(FROM_TIME, yesterdayMinus3);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 9);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 3);
        assertEquals(eventClick, 4);
        assertEquals(eventLoveIt, 2);

    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAllEventFromTomorrow() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, tomorrow);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 4);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 0);
        assertEquals(eventClick, 2);
        assertEquals(eventLoveIt, 2);

    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAllEventFromYesterdayAndToday() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, yesterday);
        parameters.put(TO_TIME, today);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 5);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 3);
        assertEquals(eventClick, 2);
        assertEquals(eventLoveIt, 0);

    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAssignmentsCount() throws Exception {
        String result = getAssignmentSummary(experiment);
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.readValue(result, Map.class);
        Map totalUsers = (Map) map.get("totalUsers");
        assertEquals(totalUsers.get("total").toString(), 3 + "");
        assertEquals(totalUsers.get("bucketAssignments").toString(), 3 + "");
        assertEquals(map.get("experimentID"), experiment.id);

        Set<String> buckets = new HashSet<>();
        long count = 0;
        List assignments = (List) map.get("assignments");
        for (int i = 0; i < assignments.size(); i++) {
            Map assignment = (Map) assignments.get(i);
            buckets.add((String) assignment.get("bucket"));
            count += (Integer) assignment.get("count");
        }
        assertEquals(3, count);
        if (buckets.size() < 1 || buckets.size() > 2)
            fail("Bucket size should be 1 or 2 : " + buckets.size());

        for (String bucket : buckets) {
            if (!(bucket.equals(RED) || bucket.equals(BLUE))) {
                fail("Bucket not " + RED + " OR " + BLUE + ": " + bucket);
            }
        }
    }

    @Test(dependsOnMethods = {"checkAllEvents"})
    public void checkAllEventFromTodayAndTomorrow() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, today);
        parameters.put(TO_TIME, tomorrowPlus3);
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 6);
        int eventImpression = 0;
        int eventClick = 0;
        int eventLoveIt = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                eventImpression++;
            else if (event.name.equals(actionClick))
                eventClick++;
            else if (event.name.equals(actionLoveIt))
                eventLoveIt++;
            else
                fail("unknown event: " + event);
        }
        assertEquals(eventImpression, 0);
        assertEquals(eventClick, 4);
        assertEquals(eventLoveIt, 2);

    }
}
