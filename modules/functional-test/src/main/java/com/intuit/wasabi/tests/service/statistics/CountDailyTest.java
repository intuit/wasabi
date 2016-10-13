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

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.analytics.AnalyticsParameters;
import com.intuit.wasabi.tests.model.analytics.DailyStatistics;
import com.intuit.wasabi.tests.model.analytics.ExperimentBasicStatistics;
import com.intuit.wasabi.tests.model.analytics.ExperimentCumulativeStatistics;
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

/**
 * Bucket integration tests
 */
public class CountDailyTest extends TestBase {

    private static final String FROM_TIME = "fromTime";
    private static final String QBO = "qbo";
    private static final String RED = "red";
    private static final String BLUE = "blue";
    private String yesterday;
    private String today;
    private String tomorrow;

    private Experiment experiment;
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


    @BeforeClass
    public void setup() {
        setResponseLogLengthLimit(1000);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
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
        String tomorrowPlus3 = dateFormat.format(cal.getTime());
        tomorrowPlus3 += "T00:00:00+0000";

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
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(experiment, allocations, labels, control);
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
        System.out.println("Events size" + events);

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
        User[] users = {userBill, userJane, userTom};
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

        assertEquals(events.size(), 3);
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
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 2);
        for (Event event : events) {
            assertEquals(event.name, actionLoveIt);
        }

    }


    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkDailyStatsYestedayOnly() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = yesterday;
        params.toTime = yesterday;
        params.context = QBO;
        ExperimentCumulativeStatistics dailyStats = postDailyStatistics(experiment, params);
        System.out.println(dailyStats);

        assertEquals(1, dailyStats.days.size());

        DailyStatistics yesterday = dailyStats.days.get(0);
        ExperimentBasicStatistics countsYesterday = yesterday.perDay;

        assertEquals(2, countsYesterday.buckets.size());
        assertEquals(3, countsYesterday.impressionCounts.eventCount);
        assertEquals(3, countsYesterday.impressionCounts.uniqueUserCount);
        assertEquals(3,
                countsYesterday.buckets.get(RED).impressionCounts.eventCount +
                        countsYesterday.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                countsYesterday.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsYesterday.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(0, countsYesterday.jointActionCounts.eventCount);
        assertEquals(0, countsYesterday.jointActionCounts.uniqueUserCount);
        assertEquals(0,
                countsYesterday.buckets.get(RED).jointActionCounts.eventCount +
                        countsYesterday.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(0,
                countsYesterday.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsYesterday.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int redActionClickCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(RED)))
            redActionClickCount = countsYesterday.actionCounts.get(RED).eventCount;

        int redActionUniqueCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(RED)))
            redActionUniqueCount = countsYesterday.actionCounts.get(RED).uniqueUserCount;

        int blueActionClickCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(BLUE)))
            blueActionClickCount = countsYesterday.actionCounts.get(BLUE).eventCount;

        int blueActionUniqueCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(BLUE)))
            blueActionUniqueCount = countsYesterday.actionCounts.get(BLUE).uniqueUserCount;


        assertEquals(0, redActionClickCount + blueActionClickCount);
        assertEquals(0, redActionUniqueCount + blueActionUniqueCount);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkDailyStatsTodayOnly() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = today;
        params.toTime = today;
        params.context = QBO;
        ExperimentCumulativeStatistics dailyStats = postDailyStatistics(experiment, params);
        System.out.println(dailyStats);

        assertEquals(1, dailyStats.days.size());

        DailyStatistics today = dailyStats.days.get(0);
        ExperimentBasicStatistics countsToday = today.perDay;

        assertEquals(2, countsToday.buckets.size());
        assertEquals(0, countsToday.impressionCounts.eventCount);
        assertEquals(0, countsToday.impressionCounts.uniqueUserCount);
        assertEquals(0,
                countsToday.buckets.get(RED).impressionCounts.eventCount +
                        countsToday.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                countsToday.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsToday.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(3, countsToday.jointActionCounts.eventCount);
        assertEquals(3, countsToday.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                countsToday.buckets.get(RED).jointActionCounts.eventCount +
                        countsToday.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(3,
                countsToday.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsToday.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int actionClickCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionClick)))
            actionClickCount = countsToday.actionCounts.get(actionClick).eventCount;

        int actionUniqueCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionClick)))
            actionUniqueCount = countsToday.actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(3, actionClickCount);
        assertEquals(3, actionUniqueCount);

        int actionLoveItCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionLoveIt)))
            actionLoveItCount = countsToday.actionCounts.get(actionLoveIt).eventCount;

        int actionLoveItUniqueCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionLoveIt)))
            actionLoveItUniqueCount = countsToday.actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(0, actionLoveItCount);
        assertEquals(0, actionLoveItUniqueCount);
    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkDailyStatsYesterdayToTodayOnly() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = yesterday;
        params.toTime = today;
        params.context = QBO;
        ExperimentCumulativeStatistics dailyStats = postDailyStatistics(experiment, params);
        System.out.println(dailyStats);

        assertEquals(2, dailyStats.days.size());
        // Yesterday
        DailyStatistics yesterday = dailyStats.days.get(0);
        ExperimentBasicStatistics countsYesterday = yesterday.perDay;

        assertEquals(2, countsYesterday.buckets.size());
        assertEquals(3, countsYesterday.impressionCounts.eventCount);
        assertEquals(3, countsYesterday.impressionCounts.uniqueUserCount);
        assertEquals(3,
                countsYesterday.buckets.get(RED).impressionCounts.eventCount +
                        countsYesterday.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                countsYesterday.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsYesterday.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(0, countsYesterday.jointActionCounts.eventCount);
        assertEquals(0, countsYesterday.jointActionCounts.uniqueUserCount);
        assertEquals(0,
                countsYesterday.buckets.get(RED).jointActionCounts.eventCount +
                        countsYesterday.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(0,
                countsYesterday.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsYesterday.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int redActionClickCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(RED)))
            redActionClickCount = countsYesterday.actionCounts.get(RED).eventCount;

        int redActionUniqueCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(RED)))
            redActionUniqueCount = countsYesterday.actionCounts.get(RED).uniqueUserCount;

        int blueActionClickCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(BLUE)))
            blueActionClickCount = countsYesterday.actionCounts.get(BLUE).eventCount;

        int blueActionUniqueCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(BLUE)))
            blueActionUniqueCount = countsYesterday.actionCounts.get(BLUE).uniqueUserCount;


        assertEquals(0, redActionClickCount + blueActionClickCount);
        assertEquals(0, redActionUniqueCount + blueActionUniqueCount);

        // today
        DailyStatistics today = dailyStats.days.get(1);
        ExperimentBasicStatistics countsToday = today.perDay;

        assertEquals(2, countsToday.buckets.size());
        assertEquals(0, countsToday.impressionCounts.eventCount);
        assertEquals(0, countsToday.impressionCounts.uniqueUserCount);
        assertEquals(0,
                countsToday.buckets.get(RED).impressionCounts.eventCount +
                        countsToday.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                countsToday.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsToday.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(3, countsToday.jointActionCounts.eventCount);
        assertEquals(3, countsToday.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                countsToday.buckets.get(RED).jointActionCounts.eventCount +
                        countsToday.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(3,
                countsToday.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsToday.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int actionClickCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionClick)))
            actionClickCount = countsToday.actionCounts.get(actionClick).eventCount;

        int actionUniqueCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionClick)))
            actionUniqueCount = countsToday.actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(3, actionClickCount);
        assertEquals(3, actionUniqueCount);

        int actionLoveItCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionLoveIt)))
            actionLoveItCount = countsToday.actionCounts.get(actionLoveIt).eventCount;

        int actionLoveItUniqueCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionLoveIt)))
            actionLoveItUniqueCount = countsToday.actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(0, actionLoveItCount);
        assertEquals(0, actionLoveItUniqueCount);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkDailyStatsYesterdayToTomorrow() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = yesterday;
        params.toTime = tomorrow;
        params.context = QBO;
        ExperimentCumulativeStatistics dailyStats = postDailyStatistics(experiment, params);
        System.out.println(dailyStats);

        assertEquals(3, dailyStats.days.size());
        // Yesterday
        DailyStatistics yesterday = dailyStats.days.get(0);
        ExperimentBasicStatistics countsYesterday = yesterday.perDay;

        assertEquals(2, countsYesterday.buckets.size());
        assertEquals(3, countsYesterday.impressionCounts.eventCount);
        assertEquals(3, countsYesterday.impressionCounts.uniqueUserCount);
        assertEquals(3,
                countsYesterday.buckets.get(RED).impressionCounts.eventCount +
                        countsYesterday.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(3,
                countsYesterday.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsYesterday.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(0, countsYesterday.jointActionCounts.eventCount);
        assertEquals(0, countsYesterday.jointActionCounts.uniqueUserCount);
        assertEquals(0,
                countsYesterday.buckets.get(RED).jointActionCounts.eventCount +
                        countsYesterday.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(0,
                countsYesterday.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsYesterday.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int redActionClickCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(RED)))
            redActionClickCount = countsYesterday.actionCounts.get(RED).eventCount;

        int redActionUniqueCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(RED)))
            redActionUniqueCount = countsYesterday.actionCounts.get(RED).uniqueUserCount;

        int blueActionClickCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(BLUE)))
            blueActionClickCount = countsYesterday.actionCounts.get(BLUE).eventCount;

        int blueActionUniqueCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(BLUE)))
            blueActionUniqueCount = countsYesterday.actionCounts.get(BLUE).uniqueUserCount;


        assertEquals(0, redActionClickCount + blueActionClickCount);
        assertEquals(0, redActionUniqueCount + blueActionUniqueCount);

        // today
        DailyStatistics today = dailyStats.days.get(1);
        ExperimentBasicStatistics countsToday = today.perDay;

        assertEquals(2, countsToday.buckets.size());
        assertEquals(0, countsToday.impressionCounts.eventCount);
        assertEquals(0, countsToday.impressionCounts.uniqueUserCount);
        assertEquals(0,
                countsToday.buckets.get(RED).impressionCounts.eventCount +
                        countsToday.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                countsToday.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsToday.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(3, countsToday.jointActionCounts.eventCount);
        assertEquals(3, countsToday.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                countsToday.buckets.get(RED).jointActionCounts.eventCount +
                        countsToday.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(3,
                countsToday.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsToday.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int actionClickCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionClick)))
            actionClickCount = countsToday.actionCounts.get(actionClick).eventCount;

        int actionUniqueCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionClick)))
            actionUniqueCount = countsToday.actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(3, actionClickCount);
        assertEquals(3, actionUniqueCount);

        int actionLoveItCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionLoveIt)))
            actionLoveItCount = countsToday.actionCounts.get(actionLoveIt).eventCount;

        int actionLoveItUniqueCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionLoveIt)))
            actionLoveItUniqueCount = countsToday.actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(0, actionLoveItCount);
        assertEquals(0, actionLoveItUniqueCount);

        // tomorrow
        DailyStatistics tomorrow = dailyStats.days.get(2);
        ExperimentBasicStatistics countsTomorrow = tomorrow.perDay;

        assertEquals(2, countsTomorrow.buckets.size());
        assertEquals(0, countsTomorrow.impressionCounts.eventCount);
        assertEquals(0, countsTomorrow.impressionCounts.uniqueUserCount);
        assertEquals(0,
                countsTomorrow.buckets.get(RED).impressionCounts.eventCount +
                        countsTomorrow.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                countsTomorrow.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsTomorrow.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(2, countsTomorrow.jointActionCounts.eventCount);
        assertEquals(2, countsTomorrow.jointActionCounts.uniqueUserCount);
        assertEquals(2,
                countsTomorrow.buckets.get(RED).jointActionCounts.eventCount +
                        countsTomorrow.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(2,
                countsTomorrow.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsTomorrow.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int actionClickCount3 = 0;
        if (Objects.nonNull(countsTomorrow.actionCounts.get(actionClick)))
            actionClickCount3 = countsTomorrow.actionCounts.get(actionClick).eventCount;

        int actionUniqueCount3 = 0;
        if (Objects.nonNull(countsTomorrow.actionCounts.get(actionClick)))
            actionUniqueCount3 = countsTomorrow.actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(0, actionClickCount3);
        assertEquals(0, actionUniqueCount3);

        int actionLoveItCount3 = 0;
        if (Objects.nonNull(countsTomorrow.actionCounts.get(actionLoveIt)))
            actionLoveItCount3 = countsTomorrow.actionCounts.get(actionLoveIt).eventCount;

        int actionLoveItUniqueCount3 = 0;
        if (Objects.nonNull(countsTomorrow.actionCounts.get(actionLoveIt)))
            actionLoveItUniqueCount3 = countsTomorrow.actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(2, actionLoveItCount3);
        assertEquals(2, actionLoveItUniqueCount3);
    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkDailyStatsTodayToTomorrowOnly() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = today;
        params.toTime = tomorrow;
        params.context = QBO;
        ExperimentCumulativeStatistics dailyStats = postDailyStatistics(experiment, params);
        System.out.println(dailyStats);

        assertEquals(2, dailyStats.days.size());
        // Yesterday
        DailyStatistics yesterday = dailyStats.days.get(0);
        ExperimentBasicStatistics countsYesterday = yesterday.perDay;

        assertEquals(2, countsYesterday.buckets.size());
        assertEquals(0, countsYesterday.impressionCounts.eventCount);
        assertEquals(0, countsYesterday.impressionCounts.uniqueUserCount);
        assertEquals(0,
                countsYesterday.buckets.get(RED).impressionCounts.eventCount +
                        countsYesterday.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                countsYesterday.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsYesterday.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(3, countsYesterday.jointActionCounts.eventCount);
        assertEquals(3, countsYesterday.jointActionCounts.uniqueUserCount);
        assertEquals(3,
                countsYesterday.buckets.get(RED).jointActionCounts.eventCount +
                        countsYesterday.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(3,
                countsYesterday.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsYesterday.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int redActionClickCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(RED)))
            redActionClickCount = countsYesterday.actionCounts.get(RED).eventCount;

        int redActionUniqueCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(RED)))
            redActionUniqueCount = countsYesterday.actionCounts.get(RED).uniqueUserCount;

        int blueActionClickCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(BLUE)))
            blueActionClickCount = countsYesterday.actionCounts.get(BLUE).eventCount;

        int blueActionUniqueCount = 0;
        if (Objects.nonNull(countsYesterday.actionCounts.get(BLUE)))
            blueActionUniqueCount = countsYesterday.actionCounts.get(BLUE).uniqueUserCount;


        assertEquals(0, redActionClickCount + blueActionClickCount);
        assertEquals(0, redActionUniqueCount + blueActionUniqueCount);

        // today
        DailyStatistics today = dailyStats.days.get(1);
        ExperimentBasicStatistics countsToday = today.perDay;

        assertEquals(2, countsToday.buckets.size());
        assertEquals(0, countsToday.impressionCounts.eventCount);
        assertEquals(0, countsToday.impressionCounts.uniqueUserCount);
        assertEquals(0,
                countsToday.buckets.get(RED).impressionCounts.eventCount +
                        countsToday.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                countsToday.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsToday.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(2, countsToday.jointActionCounts.eventCount);
        assertEquals(2, countsToday.jointActionCounts.uniqueUserCount);
        assertEquals(2,
                countsToday.buckets.get(RED).jointActionCounts.eventCount +
                        countsToday.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(2,
                countsToday.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsToday.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int actionClickCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionClick)))
            actionClickCount = countsToday.actionCounts.get(actionClick).eventCount;

        int actionUniqueCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionClick)))
            actionUniqueCount = countsToday.actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(0, actionClickCount);
        assertEquals(0, actionUniqueCount);

        int actionLoveItCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionLoveIt)))
            actionLoveItCount = countsToday.actionCounts.get(actionLoveIt).eventCount;

        int actionLoveItUniqueCount = 0;
        if (Objects.nonNull(countsToday.actionCounts.get(actionLoveIt)))
            actionLoveItUniqueCount = countsToday.actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(2, actionLoveItCount);
        assertEquals(2, actionLoveItUniqueCount);

    }

    @Test(dependsOnMethods = {"postLoveIt"})
    public void checkDailyStatsTomorrowOnly() {
        List<String> types = new ArrayList<>();
        types.add(actionImpression);
        types.add(actionClick);
        types.add(actionLoveIt);
        AnalyticsParameters params = new AnalyticsParameters();
        params.confidenceLevel = .9999999d;
        params.actions = types;
        params.fromTime = tomorrow;
        params.toTime = tomorrow;
        params.context = QBO;
        ExperimentCumulativeStatistics dailyStats = postDailyStatistics(experiment, params);
        System.out.println(dailyStats);

        assertEquals(1, dailyStats.days.size());

        DailyStatistics tomorrow = dailyStats.days.get(0);
        ExperimentBasicStatistics countsTomorrow = tomorrow.perDay;

        assertEquals(2, countsTomorrow.buckets.size());
        assertEquals(0, countsTomorrow.impressionCounts.eventCount);
        assertEquals(0, countsTomorrow.impressionCounts.uniqueUserCount);
        assertEquals(0,
                countsTomorrow.buckets.get(RED).impressionCounts.eventCount +
                        countsTomorrow.buckets.get(BLUE).impressionCounts.eventCount);
        assertEquals(0,
                countsTomorrow.buckets.get(RED).impressionCounts.uniqueUserCount +
                        countsTomorrow.buckets.get(BLUE).impressionCounts.uniqueUserCount);

        assertEquals(2, countsTomorrow.jointActionCounts.eventCount);
        assertEquals(2, countsTomorrow.jointActionCounts.uniqueUserCount);
        assertEquals(2,
                countsTomorrow.buckets.get(RED).jointActionCounts.eventCount +
                        countsTomorrow.buckets.get(BLUE).jointActionCounts.eventCount);
        assertEquals(2,
                countsTomorrow.buckets.get(RED).jointActionCounts.uniqueUserCount +
                        countsTomorrow.buckets.get(BLUE).jointActionCounts.uniqueUserCount);

        int actionClickCount = 0;
        if (Objects.nonNull(countsTomorrow.actionCounts.get(actionClick)))
            actionClickCount = countsTomorrow.actionCounts.get(actionClick).eventCount;

        int actionUniqueCount = 0;
        if (Objects.nonNull(countsTomorrow.actionCounts.get(actionClick)))
            actionUniqueCount = countsTomorrow.actionCounts.get(actionClick).uniqueUserCount;

        assertEquals(0, actionClickCount);
        assertEquals(0, actionUniqueCount);

        int actionLoveItCount = 0;
        if (Objects.nonNull(countsTomorrow.actionCounts.get(actionLoveIt)))
            actionLoveItCount = countsTomorrow.actionCounts.get(actionLoveIt).eventCount;

        int actionLoveItUniqueCount = 0;
        if (Objects.nonNull(countsTomorrow.actionCounts.get(actionLoveIt)))
            actionLoveItUniqueCount = countsTomorrow.actionCounts.get(actionLoveIt).uniqueUserCount;

        assertEquals(2, actionLoveItCount);
        assertEquals(2, actionLoveItUniqueCount);
    }
}
