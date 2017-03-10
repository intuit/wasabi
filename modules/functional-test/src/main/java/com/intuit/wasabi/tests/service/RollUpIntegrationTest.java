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
package com.intuit.wasabi.tests.service;

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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_RUNNING;
import static com.intuit.wasabi.tests.library.util.Constants.NEW_ASSIGNMENT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * RollUp integration tests
 */
public class RollUpIntegrationTest extends TestBase {

    private static final String CONTEXT = "context";
    private static final String ACTION = "action";
    private static final String DISTINCT_USER_COUNT = "distinct_user_count";
    private static final String USER_COUNT = "user_count";
    private static final String BUCKET_LABEL = "bucket_label";
    private static final String FROM_TIME = "fromTime";
    private static final String QBO = "qbo";
    protected static final String RED = "red";
    protected static final String BLUE = "blue";

    protected static final String PROD = "PROD";
    protected static final String QA = "QA";

    private String yesterday;
    private String today;
    private String tomorrow;

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
    private String tomorrowPlus3;

    private Connection connection;

    /**
     * Initializes a default experiment.
     */
    public RollUpIntegrationTest() throws Exception {

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

        experiment = ExperimentFactory.createExperiment();
        experiment.startTime = yesterday;
        experiment.endTime = tomorrowPlus3;
        experiment.samplingPercent = 1.0;
        experiment.label = "experiment";
        experiment.applicationName = QBO + UUID.randomUUID();

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy(
                "creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);

    }

    @BeforeClass
    public void prepareDBConnection() throws Exception {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        connection = DriverManager.getConnection(
                appProperties.getProperty("database.url"),
                appProperties.getProperty("database.username"),
                appProperties.getProperty("database.password"));
    }

    @AfterClass
    public void closeDBConnection() throws Exception {
        connection.close();
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateTwoBuckets() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime,
                "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime,
                "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state,
                "Experiment creation failed (No state).");
        experiment.update(exp);
        buckets = BucketFactory.createCompleteBuckets(experiment, allocations,
                labels, control);
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
            assertEquals(result.label, matching.label);
            assertEquals(result.isControl, matching.isControl);
            assertEquals(result.allocationPercent, matching.allocationPercent);
            assertEquals(result.description, matching.description);
        }
        experiment.state = EXPERIMENT_STATE_RUNNING;
        experiment = putExperiment(experiment);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_CheckBasicCounts() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, "");
        List<Event> events = postEvents(experiment, parameters, true,
                HttpStatus.SC_OK, apiServerConnector);
        assertEquals(events.size(), 0);
        System.out.println("Events size" + events);

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "") + "' group by bucket_label";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            if (result.next()) {
                String bucket = result.getString(BUCKET_LABEL);
                int userCount = result.getInt(USER_COUNT);
                int distinctUserCount = result.getInt(DISTINCT_USER_COUNT);
                fail("There should not be any rows but found with first bucket: "
                        + bucket
                        + " userCount: "
                        + userCount
                        + " distinct_user_count : " + distinctUserCount);
            }
        }
    }

    @Test(dependsOnMethods = {"t_CheckBasicCounts"})
    public void t_PostAssignments() {

        for (User user : users) {
            Assignment result = postAssignment(experiment, user, PROD);

            assertEquals(result.status, NEW_ASSIGNMENT);
        }

        for (User user : users) {
            Assignment result = postAssignment(experiment, user, QA);

            assertEquals(result.status, NEW_ASSIGNMENT);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, "");
        List<Event> events = postEvents(experiment, parameters, true,
                HttpStatus.SC_OK, apiServerConnector);
        assertEquals(events.size(), 0);
        for (Event event : events) {
            assertEquals(event.name, actionImpression);
        }
    }

    @Test(dependsOnMethods = {"t_PostAssignments"})
    public void t_PostImpressionsYesterday() {
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = PROD;
            event.name = actionImpression;
            event.timestamp = yesterday;
            Response result = postEvent(event, experiment, user,
                    HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, "");
        parameters.put(CONTEXT, PROD);
        List<Event> events = postEvents(experiment, parameters, true,
                HttpStatus.SC_OK, apiServerConnector);
        assertEquals(events.size(), 3);
        for (Event event : events) {
            assertEquals(event.name, actionImpression);
        }

    }

    @Test(dependsOnMethods = {"t_PostImpressionsYesterday"})
    public void t_PostClickToday() {
        User[] users = {userBill, userJane, userTom};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = PROD;
            event.name = actionClick;
            event.timestamp = today;
            Response result = postEvent(event, experiment, user,
                    HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, today);
        parameters.put(CONTEXT, PROD);
        List<Event> events = postEvents(experiment, parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 3);
        for (Event event : events) {
            assertEquals(event.name, actionClick);
        }

    }

    @Test(dependsOnMethods = {"t_PostClickToday"})
    public void t_PostLoveItTomorrow() {
        User[] users = {userJane, userTom};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = PROD;
            event.name = actionLoveIt;
            event.timestamp = tomorrow;
            Response result = postEvent(event, experiment, user,
                    HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, tomorrow);
        parameters.put(CONTEXT, PROD);

        List<Event> events = postEvents(experiment, parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 4);
        for (Event event : events) {
            if (event.name.equals(actionLoveIt)
                    || event.name.equals(actionImpression)) {
                // ok
            } else {
                fail("event not loveit or impression: " + event);
            }
        }

    }

    @Test(dependsOnMethods = {"t_PostClickToday"})
    public void t_PostImpressionTomorrowQA() {
        User[] users = {userTom, userBill};
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = QA;
            event.name = actionImpression;
            event.timestamp = tomorrow;
            Response result = postEvent(event, experiment, user,
                    HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForYesterdayByBucketFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.yesterday
                + "' group by bucket_label";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            if (result.next()) {
                String bucket = result.getString(BUCKET_LABEL);
                int userCount = result.getInt(USER_COUNT);
                int distinctUserCount = result.getInt(DISTINCT_USER_COUNT);
                fail("There should not be any rows but found with first bucket: "
                        + bucket
                        + " userCount: "
                        + userCount
                        + " distinct_user_count : "
                        + distinctUserCount
                        + " query: " + queryGroupByBucket);
            }
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForYesterdayByBucketAndActionFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucketAndAction = "select bucket_label, action, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.yesterday
                + "' group by bucket_label, action";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement
                     .executeQuery(queryGroupByBucketAndAction)) {
            if (result.next()) {
                String bucket = result.getString(BUCKET_LABEL);
                int userCount = result.getInt(USER_COUNT);
                int distinctUserCount = result.getInt(DISTINCT_USER_COUNT);
                fail("There should not be any rows but found with first bucket: "
                        + bucket
                        + " userCount: "
                        + userCount
                        + " distinct_user_count : "
                        + distinctUserCount
                        + " query: " + queryGroupByBucketAndAction);
            }
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForYesterdayByBucketFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.yesterday
                + "' group by bucket_label";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            int userCount = 0;
            int distinctUserCount = 0;
            boolean atLeastOneRow = false;
            while (result.next()) {
                userCount += result.getInt(USER_COUNT);
                distinctUserCount += result.getInt(DISTINCT_USER_COUNT);
                atLeastOneRow = true;
            }
            if (!atLeastOneRow) {
                fail("At least one row expected for query: "
                        + queryGroupByBucket);
            }

            assertEquals(userCount, 3);
            assertEquals(distinctUserCount, 3);
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForYesterdayByBucketFromMySQLProdContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and context = 'PROD' and timestamp = '" + this.yesterday
                + "' group by bucket_label";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            int userCount = 0;
            int distinctUserCount = 0;
            boolean atLeastOneRow = false;
            while (result.next()) {
                userCount += result.getInt(USER_COUNT);
                distinctUserCount += result.getInt(DISTINCT_USER_COUNT);
                atLeastOneRow = true;
            }
            if (!atLeastOneRow) {
                fail("At least one row expected for query: "
                        + queryGroupByBucket);
            }

            assertEquals(userCount, 3);
            assertEquals(distinctUserCount, 3);
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForYesterdayByBucketFromMySQLQAContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and context = 'QA' and timestamp = '" + this.yesterday
                + "' group by bucket_label";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            if (result.next()) {
                fail("Should have no rows for query: " + queryGroupByBucket);
            }
        }

    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForTomorrowByBucketFromMySQLProdContext()
            throws Exception {
        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and context = 'PROD' and timestamp = '" + this.tomorrow
                + "' group by bucket_label";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            if (result.next()) {
                fail("Should have no rows for query: " + queryGroupByBucket);
            }
        }

    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTodayByBucketFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.today
                + "' group by bucket_label";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            int userCount = 0;
            int distinctUserCount = 0;
            boolean atLeastOneRow = false;
            while (result.next()) {
                String bucket = result.getString(BUCKET_LABEL);
                userCount += result.getInt(USER_COUNT);
                distinctUserCount += result.getInt(DISTINCT_USER_COUNT);
                atLeastOneRow = true;
            }
            if (!atLeastOneRow) {
                fail("At least one row expected for query: "
                        + queryGroupByBucket);
            }
            assertEquals(userCount, 3);
            assertEquals(distinctUserCount, 3);
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTodayByBucketAndActionFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucketAndAction = "select bucket_label, action, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.today
                + "' group by bucket_label, action";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement
                     .executeQuery(queryGroupByBucketAndAction)) {
            int userCount = 0;
            int distinctUserCount = 0;
            boolean atLeastOneRow = false;
            while (result.next()) {
                String action = result.getString(ACTION);
                userCount += result.getInt(USER_COUNT);
                distinctUserCount += result.getInt(DISTINCT_USER_COUNT);
                assertEquals(actionClick, action);
                atLeastOneRow = true;
            }
            if (!atLeastOneRow) {
                fail("At least one row expected for query: "
                        + queryGroupByBucketAndAction);
            }
            assertEquals(userCount, 3);
            assertEquals(distinctUserCount, 3);
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTodayByBucketFromMySQLQAContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and context = 'QA' and timestamp = '" + this.today
                + "' group by bucket_label";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            if (result.next()) {
                fail("There should be no row expected for query: "
                        + queryGroupByBucket);
            }
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTodayByBucketAndActionFromMySQLQAContext()
            throws Exception {

        String queryGroupByBucketAndAction = "select bucket_label, action, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and context = 'QA' and timestamp = '" + this.today
                + "' group by bucket_label, action";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement
                     .executeQuery(queryGroupByBucketAndAction)) {
            if (result.next()) {
                fail("There should be no row expected for query: "
                        + queryGroupByBucketAndAction);
            }
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTomorrowByBucketFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.tomorrow
                + "' group by bucket_label";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            int userCount = 0;
            int distinctUserCount = 0;
            boolean atLeastOneRow = false;
            while (result.next()) {
                String bucket = result.getString(BUCKET_LABEL);
                userCount += result.getInt(USER_COUNT);
                distinctUserCount += result.getInt(DISTINCT_USER_COUNT);
                atLeastOneRow = true;
            }
            if (!atLeastOneRow) {
                fail("At least one row expected for query: "
                        + queryGroupByBucket);
            }
            assertEquals(userCount, 2);
            assertEquals(distinctUserCount, 2);
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTomorrowByBucketAndActionFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucketAndAction = "select bucket_label, action, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.tomorrow
                + "' group by bucket_label, action";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement
                     .executeQuery(queryGroupByBucketAndAction)) {
            int userCount = 0;
            int distinctUserCount = 0;
            boolean atLeastOneRow = false;
            while (result.next()) {
                String bucket = result.getString(BUCKET_LABEL);
                userCount += result.getInt(USER_COUNT);
                distinctUserCount += result.getInt(DISTINCT_USER_COUNT);
                atLeastOneRow = true;
                if (RED.equals(bucket) || BLUE.equals(bucket)) {
                    // ok
                } else {
                    fail("Bucket should be red or blue " + bucket);
                }
            }
            if (!atLeastOneRow) {
                fail("At least one row expected for query: "
                        + queryGroupByBucketAndAction);
            }
            assertEquals(userCount, 2);
            assertEquals(distinctUserCount, 2);
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForTodayByBucketFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.today
                + "' group by bucket_label";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            if (result.next()) {
                fail("should be no impressions for today " + queryGroupByBucket);
            }
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForTomorrowByBucketFromMySQLAllContext()
            throws Exception {

        String queryGroupByBucket = "select bucket_label, "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experiment.id.replace("-", "").toUpperCase()
                + "' and timestamp = '" + this.tomorrow
                + "' group by bucket_label";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(queryGroupByBucket)) {
            int userCount = 0;
            int distinctUserCount = 0;
            boolean atLeastOneRow = false;
            while (result.next()) {
                String bucket = result.getString(BUCKET_LABEL);
                userCount += result.getInt(USER_COUNT);
                distinctUserCount += result.getInt(DISTINCT_USER_COUNT);
                atLeastOneRow = true;
            }
            if (!atLeastOneRow) {
                fail("At least one row expected for query: "
                        + queryGroupByBucket);
            }
            assertEquals(userCount, 2);
            assertEquals(distinctUserCount, 2);
        }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_PostRollUpsForTomorrow() throws Exception {

        AnalyticsParameters paramsQA = new AnalyticsParameters();
        paramsQA.confidenceLevel = .9999999d;
        paramsQA.actions = null;
        paramsQA.context = QA;

        long startTime = System.nanoTime();
        ExperimentCounts countsBeforeQA = postExperimentCounts(experiment,
                paramsQA);
        long endTime = System.nanoTime();

        System.out.println("Before analytics time QA was: "
                + (endTime - startTime));

        AnalyticsParameters paramsProd = new AnalyticsParameters();
        paramsProd.confidenceLevel = .9999999d;
        paramsProd.actions = null;
        paramsProd.context = PROD;

        startTime = System.nanoTime();
        ExperimentCounts countsBeforeProd = postExperimentCounts(experiment,
                paramsProd);
        endTime = System.nanoTime();

        System.out.println("Before analytics time Prod was: "
                + (endTime - startTime));

        prepareRollUpTable();

        startTime = System.nanoTime();

        ExperimentCounts countsAfterQA = postExperimentCounts(experiment,
                paramsQA);
        endTime = System.nanoTime();

        System.out.println("After analytics time QA was: "
                + (endTime - startTime));

        if (!countsBeforeQA.equals(countsAfterQA))
            fail("CountBeforeQA: " + countsBeforeQA + " \nCountAfterQA: "
                    + countsAfterQA + "\nShould be equal");

        startTime = System.nanoTime();

        ExperimentCounts countsAfterProd = postExperimentCounts(experiment,
                paramsProd);
        endTime = System.nanoTime();

        System.out.println("After analytics time Prod was: "
                + (endTime - startTime));

        if (!countsBeforeProd.equals(countsAfterProd))
            fail("CountBeforeProd: " + countsBeforeProd + " \nCountAfterProd: "
                    + countsAfterProd + "\nShould be equal");
    }

    /**
     * Note - This method prepares the mysql rollup table for action and events.
     * The steps involved in preparing the roll up table are :
     * <ol>
     * <li>Insert non cumulative counts for events and impression into roll up
     * table
     * <li>Insert cumulative counts for events and impressions into roll up
     * table
     * <li>Insert non-cumulative counts for events by action into rollup table
     * <li>Insert cumulative counts for events by action into rollup table
     * </ol>
     *
     * @throws Exception
     */
    protected void prepareRollUpTable() throws SQLException {
        String experimentId = experiment.id.replace("-", "").toUpperCase();

        // Counts for tomorrow only
        String queryImpressionGroupByRedAndQAForTomorrow = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experimentId + "' and timestamp = '" + this.tomorrow
                + "' and bucket_label = '" + RED + "' and context = '" + QA
                + "'";

        int[] countImpressionRedQATomorrow = getUserAndDistinctUserCount(
                connection, queryImpressionGroupByRedAndQAForTomorrow);

        String queryActionGroupByRedAndQAForTomorrow = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experimentId + "' and timestamp = '" + this.tomorrow
                + "' and bucket_label = '" + RED + "' and context = '" + QA
                + "'";

        int[] countActionRedQATomorrow = getUserAndDistinctUserCount(
                connection, queryActionGroupByRedAndQAForTomorrow);

        String queryImpressionGroupByBlueAndQAForTomorrow = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experimentId + "' and timestamp = '" + this.tomorrow
                + "' and bucket_label = '" + BLUE + "' and context = '" + QA
                + "'";

        int[] countImpressionBlueQATomorrow = getUserAndDistinctUserCount(
                connection, queryImpressionGroupByBlueAndQAForTomorrow);

        String queryActionGroupByBlueAndQAForTomorrow = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experimentId + "' and timestamp = '" + this.tomorrow
                + "' and bucket_label = '" + BLUE + "' and context = '" + QA
                + "'";

        int[] countActionBlueQATomorrow = getUserAndDistinctUserCount(
                connection, queryActionGroupByBlueAndQAForTomorrow);

        String queryImpressionGroupByRedAndProdForTomorrow = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experimentId + "' and timestamp = '" + this.tomorrow
                + "' and bucket_label = '" + RED + "' and context = '" + PROD
                + "'";

        int[] countImpressionRedProdTomorrow = getUserAndDistinctUserCount(
                connection, queryImpressionGroupByRedAndProdForTomorrow);

        String queryActionGroupByRedAndProdForTomorrow = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experimentId + "' and timestamp = '" + this.tomorrow
                + "' and bucket_label = '" + RED + "' and context = '" + PROD
                + "'";

        int[] countActionRedProdTomorrow = getUserAndDistinctUserCount(
                connection, queryActionGroupByRedAndProdForTomorrow);

        String queryImpressionGroupByBlueAndProdForTomorrow = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experimentId + "' and timestamp = '" + this.tomorrow
                + "' and bucket_label = '" + BLUE + "' and context = '" + PROD
                + "'";

        int[] countImpressionBlueProdTomorrow = getUserAndDistinctUserCount(
                connection, queryImpressionGroupByBlueAndProdForTomorrow);

        String queryActionGroupByBlueAndProdForTomorrow = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experimentId + "' and timestamp = '" + this.tomorrow
                + "' and bucket_label = '" + BLUE + "' and context = '" + PROD
                + "'";

        int[] countActionBlueProdTomorrow = getUserAndDistinctUserCount(
                connection, queryActionGroupByBlueAndProdForTomorrow);

        String day = tomorrow.substring(0, 10);

        String rollUpQuery = "REPLACE INTO experiment_rollup "
                + "(experiment_id, day, cumulative, bucket_label, action, "
                + "impression_count, impression_user_count, "
                + "action_count, action_user_count,context) "
                + "values(unhex('" + experimentId + "'),'" + day + "',0,'"
                + RED + "',''," + (countImpressionRedQATomorrow[0]) + ","
                + (countImpressionRedQATomorrow[1]) + ","
                + (countActionRedQATomorrow[0]) + ","
                + (countActionRedQATomorrow[1]) + ",'" + QA + "')";

        insertIntoRollUp(rollUpQuery);

        rollUpQuery = "REPLACE INTO experiment_rollup "
                + "(experiment_id, day, cumulative, bucket_label, action, "
                + "impression_count, impression_user_count, "
                + "action_count, action_user_count,context) "
                + "values(unhex('" + experimentId + "'),'" + day + "',0,'"
                + BLUE + "',''," + (countImpressionBlueQATomorrow[0]) + ","
                + (countImpressionBlueQATomorrow[1]) + ","
                + (countActionBlueQATomorrow[0]) + ","
                + (countActionBlueQATomorrow[1]) + ",'" + QA + "')";

        insertIntoRollUp(rollUpQuery);

        rollUpQuery = "REPLACE INTO experiment_rollup "
                + "(experiment_id, day, cumulative, bucket_label, action, "
                + "impression_count, impression_user_count, "
                + "action_count, action_user_count,context) "
                + "values(unhex('" + experimentId + "'),'" + day + "',0,'"
                + BLUE + "',''," + (countImpressionBlueProdTomorrow[0]) + ","
                + (countImpressionBlueProdTomorrow[1]) + ","
                + (countActionBlueProdTomorrow[0]) + ","
                + (countActionBlueProdTomorrow[1]) + ",'" + PROD + "')";

        insertIntoRollUp(rollUpQuery);

        rollUpQuery = "REPLACE INTO experiment_rollup "
                + "(experiment_id, day, cumulative, bucket_label, action, "
                + "impression_count, impression_user_count, "
                + "action_count, action_user_count,context) "
                + "values(unhex('" + experimentId + "'),'" + day + "',0,'"
                + RED + "',''," + (countImpressionRedProdTomorrow[0]) + ","
                + (countImpressionRedProdTomorrow[1]) + ","
                + (countActionRedProdTomorrow[0]) + ","
                + (countActionRedProdTomorrow[1]) + ",'" + PROD + "')";

        insertIntoRollUp(rollUpQuery);

        String queryActionByBucketActionContext = "SELECT bucket_label, action, context, COUNT(user_id) as user_count, "
                + " COUNT(DISTINCT user_id) as distinct_user_count "
                + " FROM event_action where hex(experiment_id) = '"
                + experimentId
                + "' and timestamp = '"
                + this.tomorrow
                + "' GROUP BY bucket_label, action ";

        try (Statement statementActionByBucketActionContext = connection
                .createStatement();
             ResultSet resultActionByBucketActionContext = statementActionByBucketActionContext
                     .executeQuery(queryActionByBucketActionContext)) {
            while (resultActionByBucketActionContext.next()) {
                String bucket = resultActionByBucketActionContext
                        .getString(BUCKET_LABEL);
                String action = resultActionByBucketActionContext
                        .getString(ACTION);
                String context = resultActionByBucketActionContext
                        .getString(CONTEXT);
                int userCount = resultActionByBucketActionContext
                        .getInt(USER_COUNT);
                int distinctUserCount = resultActionByBucketActionContext
                        .getInt(DISTINCT_USER_COUNT);

                rollUpQuery = "REPLACE INTO experiment_rollup "
                        + "(experiment_id, day, cumulative, bucket_label, action, "
                        + "action_count, action_user_count,context) "
                        + "values(unhex('" + experimentId + "'),'" + day
                        + "',0,'" + bucket + "','" + action + "'," + userCount
                        + "," + distinctUserCount + ",'" + context + "')";
                insertIntoRollUp(rollUpQuery);
            }

        }

        // Counts for tomorrow cumulative
        String queryImpressionGroupByRedAndQAForTomorrowCumulative = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experimentId + "' and timestamp <= '" + this.tomorrow
                + "' and bucket_label = '" + RED + "' and context = '" + QA
                + "'";

        int[] countImpressionRedQATomorrowCumulative = getUserAndDistinctUserCount(
                connection, queryImpressionGroupByRedAndQAForTomorrowCumulative);

        String queryActionGroupByRedAndQAForTomorrowCumulative = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experimentId + "' and timestamp <= '" + this.tomorrow
                + "' and bucket_label = '" + RED + "' and context = '" + QA
                + "'";

        int[] countActionRedQATomorrowCumulative = getUserAndDistinctUserCount(
                connection, queryActionGroupByRedAndQAForTomorrowCumulative);

        String queryImpressionGroupByBlueAndQAForTomorrowCumulative = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experimentId + "' and timestamp <= '" + this.tomorrow
                + "' and bucket_label = '" + BLUE + "' and context = '" + QA
                + "'";

        int[] countImpressionBlueQATomorrowCumulative = getUserAndDistinctUserCount(
                connection,
                queryImpressionGroupByBlueAndQAForTomorrowCumulative);

        String queryActionGroupByBlueAndQAForTomorrowCumulative = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experimentId + "' and timestamp <= '" + this.tomorrow
                + "' and bucket_label = '" + BLUE + "' and context = '" + QA
                + "'";

        int[] countActionBlueQATomorrowCumulative = getUserAndDistinctUserCount(
                connection, queryActionGroupByBlueAndQAForTomorrowCumulative);

        String queryImpressionGroupByRedAndProdForTomorrowCumulative = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experimentId
                + "' and timestamp <= '"
                + this.tomorrow
                + "' and bucket_label = '"
                + RED
                + "' and context = '"
                + PROD
                + "'";

        int[] countImpressionRedProdTomorrowCumulative = getUserAndDistinctUserCount(
                connection,
                queryImpressionGroupByRedAndProdForTomorrowCumulative);

        String queryActionGroupByRedAndProdForTomorrowCumulative = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experimentId + "' and timestamp <= '" + this.tomorrow
                + "' and bucket_label = '" + RED + "' and context = '" + PROD
                + "'";

        int[] countActionRedProdTomorrowCumulative = getUserAndDistinctUserCount(
                connection, queryActionGroupByRedAndProdForTomorrowCumulative);

        String queryImpressionGroupByBlueAndProdForTomorrowCumulative = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_impression where hex(experiment_id) = '"
                + experimentId
                + "' and timestamp <= '"
                + this.tomorrow
                + "' and bucket_label = '"
                + BLUE
                + "' and context = '"
                + PROD
                + "'";

        int[] countImpressionBlueProdTomorrowCumulative = getUserAndDistinctUserCount(
                connection,
                queryImpressionGroupByBlueAndProdForTomorrowCumulative);

        String queryActionGroupByBlueAndProdForTomorrowCumulative = "Select "
                + "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
                + "from event_action where hex(experiment_id) = '"
                + experimentId + "' and timestamp <= '" + this.tomorrow
                + "' and bucket_label = '" + BLUE + "' and context = '" + PROD
                + "'";

        int[] countActionBlueProdTomorrowCumulative = getUserAndDistinctUserCount(
                connection, queryActionGroupByBlueAndProdForTomorrowCumulative);

        rollUpQuery = "REPLACE INTO experiment_rollup "
                + "(experiment_id, day, cumulative, bucket_label, action, "
                + "impression_count, impression_user_count, "
                + "action_count, action_user_count,context) "
                + "values(unhex('" + experimentId + "'),'" + day + "',1,'"
                + RED + "',''," + (countImpressionRedQATomorrowCumulative[0])
                + "," + (countImpressionRedQATomorrowCumulative[1]) + ","
                + (countActionRedQATomorrowCumulative[0]) + ","
                + (countActionRedQATomorrowCumulative[1]) + ",'" + QA + "')";

        insertIntoRollUp(rollUpQuery);

        rollUpQuery = "REPLACE INTO experiment_rollup "
                + "(experiment_id, day, cumulative, bucket_label, action, "
                + "impression_count, impression_user_count, "
                + "action_count, action_user_count,context) "
                + "values(unhex('" + experimentId + "'),'" + day + "',1,'"
                + BLUE + "',''," + (countImpressionBlueQATomorrowCumulative[0])
                + "," + (countImpressionBlueQATomorrowCumulative[1]) + ","
                + (countActionBlueQATomorrowCumulative[0]) + ","
                + (countActionBlueQATomorrowCumulative[1]) + ",'" + QA + "')";

        insertIntoRollUp(rollUpQuery);

        rollUpQuery = "REPLACE INTO experiment_rollup "
                + "(experiment_id, day, cumulative, bucket_label, action, "
                + "impression_count, impression_user_count, "
                + "action_count, action_user_count,context) "
                + "values(unhex('" + experimentId + "'),'" + day + "',1,'"
                + BLUE + "','',"
                + (countImpressionBlueProdTomorrowCumulative[0]) + ","
                + (countImpressionBlueProdTomorrowCumulative[1]) + ","
                + (countActionBlueProdTomorrowCumulative[0]) + ","
                + (countActionBlueProdTomorrowCumulative[1]) + ",'" + PROD
                + "')";

        insertIntoRollUp(rollUpQuery);

        rollUpQuery = "REPLACE INTO experiment_rollup "
                + "(experiment_id, day, cumulative, bucket_label, action, "
                + "impression_count, impression_user_count, "
                + "action_count, action_user_count,context) "
                + "values(unhex('" + experimentId + "'),'" + day + "',1,'"
                + RED + "',''," + (countImpressionRedProdTomorrowCumulative[0])
                + "," + (countImpressionRedProdTomorrowCumulative[1]) + ","
                + (countActionRedProdTomorrowCumulative[0]) + ","
                + (countActionRedProdTomorrowCumulative[1]) + ",'" + PROD
                + "')";

        insertIntoRollUp(rollUpQuery);

        String queryActionByBucketActionContextCumulative = "SELECT bucket_label, action, context, COUNT(user_id) as user_count, "
                + " COUNT(DISTINCT user_id) as distinct_user_count "
                + " FROM event_action where hex(experiment_id) = '"
                + experimentId
                + "' and timestamp <= '"
                + this.tomorrow
                + "' GROUP BY bucket_label, action ";

        try (Statement statementActionByBucketActionContextCumulative = connection
                .createStatement();
             ResultSet resultActionByBucketActionContextCumulative = statementActionByBucketActionContextCumulative
                     .executeQuery(queryActionByBucketActionContextCumulative)) {

            while (resultActionByBucketActionContextCumulative.next()) {
                String bucket = resultActionByBucketActionContextCumulative
                        .getString(BUCKET_LABEL);
                String action = resultActionByBucketActionContextCumulative
                        .getString(ACTION);
                String context = resultActionByBucketActionContextCumulative
                        .getString(CONTEXT);
                int userCount = resultActionByBucketActionContextCumulative
                        .getInt(USER_COUNT);
                int distinctUserCount = resultActionByBucketActionContextCumulative
                        .getInt(DISTINCT_USER_COUNT);

                rollUpQuery = "REPLACE INTO experiment_rollup "
                        + "(experiment_id, day, cumulative, bucket_label, action, "
                        + "action_count, action_user_count,context) "
                        + "values(unhex('" + experimentId + "'),'" + day
                        + "',1,'" + bucket + "','" + action + "'," + userCount
                        + "," + distinctUserCount + ",'" + context + "')";
                insertIntoRollUp(rollUpQuery);
            }
        }
    }

    private void insertIntoRollUp(String rollUpQuery) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(rollUpQuery)) {
        }
    }

    private int[] getUserAndDistinctUserCount(Connection connection,
                                              String query) throws SQLException {

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(query)) {

            if (!result.next())
                fail("Should have at least one row for query " + query);

            int userCount = result.getInt(USER_COUNT);
            int distinctUserCount = result.getInt(DISTINCT_USER_COUNT);

            return new int[]{userCount, distinctUserCount};
        }
    }

}
