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

package com.intuit.wasabi.tests.service.statistic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intuit.wasabi.tests.data.SharedExperimentDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.OutputBucketStatistics;
import com.intuit.wasabi.tests.model.Statistics;
import com.intuit.wasabi.tests.model.analytics.DailyStatistics;
import com.intuit.wasabi.tests.model.analytics.ExperimentCumulativeStatistics;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatisticTest extends TestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticTest.class);
    private final List<Experiment> validExperimentsLists = new ArrayList<>();
    private final Map<String, Statistics> statisticsMap = new HashMap<>();
    private final Map<String, Map<String, String>> experimentUserBucketMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> bucketLabelToEventCount = new HashMap<>();

    @Test(groups = {"setup"}, dataProvider = "ExperimentAAndB", dataProviderClass = SharedExperimentDataProvider.class)
    public void t_setup(String experiment) {
        response = apiServerConnector.doPost("experiments", experiment);
        LOGGER.debug(response.jsonPath().prettify());
        Experiment result = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
        validExperimentsLists.add(result);
        assertReturnCode(response, HttpStatus.SC_CREATED);
    }


    @Test(groups = {"setup"}, dependsOnMethods = {"t_setup"},
            dataProvider = "ExperimentBuckets", dataProviderClass = SharedExperimentDataProvider.class)
    public void t_addBuckets(String redBucket, String blueBucket) {
        for (Experiment experiment : validExperimentsLists) {
            String url = "experiments/" + experiment.id + "/buckets";
            response = apiServerConnector.doPost(url, redBucket);
            assertReturnCode(response, HttpStatus.SC_CREATED);
            response = apiServerConnector.doPost(url, blueBucket);
            assertReturnCode(response, HttpStatus.SC_CREATED);
            response = apiServerConnector.doPut("/experiments/" + experiment.id, "{\"state\": \"RUNNING\"}");
            assertReturnCode(response, HttpStatus.SC_OK);
        }
    }


    @Test(groups = {"setup"}, dependsOnMethods = {"t_setup", "t_addBuckets"},
            dataProvider = "ExperimentUsers", dataProviderClass = SharedExperimentDataProvider.class)
    public void t_assignUsers(String userId) {
        for (Experiment experiment : validExperimentsLists) {
            String url = "assignments/applications/qbo/experiments/" + experiment.label + "/users/" + userId;
            response = apiServerConnector.doGet(url);
            assertReturnCode(response, HttpStatus.SC_OK);
            Assert.assertEquals(response.asString().contains("NEW_ASSIGNMENT"), true);
            Assignment assignment = AssignmentFactory.createFromJSONString(response.asString());
            Assert.assertEquals(null == assignment.assignment ||
                    "blue".equals(assignment.assignment) ||
                    "red".equals(assignment.assignment), true);
        }
    }


    @Test(groups = {"setup"}, dataProvider = "ValidEvents", dataProviderClass = SharedExperimentDataProvider.class)
    public void preRunStats(String bucketLabel, String events) {
        JsonObject jsonObject = new JsonParser().parse(events).getAsJsonObject();
        StatisticsUtils.COMPUTE_EVENT_COUNT_PER_LABEL(bucketLabel, jsonObject, bucketLabelToEventCount);
    }


    @Test(groups = {"setup"}, dependsOnMethods = {"t_setup", "t_addBuckets", "t_assignUsers"},
            dataProvider = "UsersAndBucketEvents", dataProviderClass = SharedExperimentDataProvider.class)
    public void t_postEvents(String user, String bucketLabel, String events) {
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doGet("assignments/applications/qbo/experiments/" + experiment.label +
                    "/users/" + user);
            assertReturnCode(response, HttpStatus.SC_OK);
            Assignment assignment = AssignmentFactory.createFromJSONString(response.asString());
            LOGGER.info("BucketLable is " + bucketLabel + " assignment Lable is " + assignment.assignment);
            if (bucketLabel.equals(assignment.assignment)) {
                String url = "events/applications/qbo/experiments/" + experiment.label + "/users/";
                response = apiServerConnector.doPost(url + user, events);
                assertReturnCode(response, HttpStatus.SC_CREATED);
                Map<String, String> userBucketMap = experimentUserBucketMap.getOrDefault(experiment.label,
                        new HashMap<>());
                experimentUserBucketMap.put(experiment.label, userBucketMap);
                userBucketMap.put(user, bucketLabel);
                assertReturnCode(response, HttpStatus.SC_CREATED);
            } else {
                LOGGER.info("Bucket " + bucketLabel + " does not match with expected bucket " + assignment.bucket_label
                        + ", not event is posted for " + user);
            }
        }
    }


    @Test(groups = {"experimentStatistic"}, dependsOnGroups = {"setup"})
    public void computeStatistics() {

        for (Experiment experiment : validExperimentsLists) {
            Statistics statistics = statisticsMap.getOrDefault(experiment.label, new Statistics());
            statisticsMap.put(experiment.label, statistics);

            for (Map.Entry<String, String> entry : experimentUserBucketMap.get(experiment.label).entrySet()) {
                Map<String, Integer> eventCounts = bucketLabelToEventCount.get(entry.getValue());
                OutputBucketStatistics bucketStatistics = statistics.getStatisticsByLable(entry.getValue());
                statistics.increaseCounts(eventCounts, (p) -> false);
                bucketStatistics.increaseCounts(eventCounts, (p) -> false);
            }
        }
    }


    @Test(groups = {"experimentA"}, dependsOnGroups = {"setup", "experimentStatistic"})
    public void t_experimentA() {
        response = apiServerConnector.doGet("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics");
        assertReturnCode(response, HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonParser().parse(response.asString()).getAsJsonObject();
        StatisticsUtils.COMPUTE_COUNT(jsonObject);

        LOGGER.debug(jsonObject.toString());
        Statistics statistics = statisticsMap.getOrDefault(validExperimentsLists.get(0).label, new Statistics());
        Statistics result = new Gson().fromJson(jsonObject, Statistics.class);
        Assert.assertTrue(result.equals(statistics));
    }


    @Test(groups = {"experimentA"}, dependsOnGroups = {"setup", "experimentStatistic"}, dependsOnMethods = {"t_experimentA"})
    public void update_experimentA_1() {
        response = apiServerConnector.doPost("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics", "{}");
        assertReturnCode(response, HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonParser().parse(response.asString()).getAsJsonObject();
        StatisticsUtils.COMPUTE_COUNT(jsonObject);
        LOGGER.info(jsonObject.toString());
        Statistics statistics = statisticsMap.getOrDefault(validExperimentsLists.get(0).label, new Statistics());
        Statistics result = new Gson().fromJson(jsonObject, Statistics.class);
        Assert.assertTrue(result.equals(statistics));
    }


    @Test(groups = {"experimentA"}, dependsOnGroups = {"setup", "experimentStatistic"},
            dependsOnMethods = {"t_experimentA"})
    public void update_experimentA_2() {
        response = apiServerConnector.doPost("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics",
                "{\"actions\": [\"click\", \"love it\"]}");
        assertReturnCode(response, HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonParser().parse(response.asString()).getAsJsonObject();
        StatisticsUtils.COMPUTE_COUNT(jsonObject);
        LOGGER.info(jsonObject.toString());
        Statistics statistics = statisticsMap.getOrDefault(validExperimentsLists.get(0).label, new Statistics());
        Statistics result = new Gson().fromJson(jsonObject, Statistics.class);
        Assert.assertTrue(result.equals(statistics));
    }


    @Test(groups = {"experimentA"}, dependsOnGroups = {"setup", "experimentStatistic"},
            dependsOnMethods = {"t_experimentA"})
    public void update_experimentA_3() {
        Set<String> actions = new HashSet<>();
        actions.add("love it");
        Statistics statistics = new Statistics();
        for (Map.Entry<String, String> entry : experimentUserBucketMap.get(validExperimentsLists.get(0).label).entrySet()) {
            Map<String, Integer> eventCounts = bucketLabelToEventCount.get(entry.getValue());
            OutputBucketStatistics bucketStatistics = statistics.getStatisticsByLable(entry.getValue());
            statistics.increaseCounts(eventCounts, (p) -> actions.contains(p));
            bucketStatistics.increaseCounts(eventCounts, (p) -> actions.contains(p));
        }
        response = apiServerConnector.doPost("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics",
                "{\"actions\": [\"click\"]}");
        assertReturnCode(response, HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonParser().parse(response.asString()).getAsJsonObject();
        StatisticsUtils.COMPUTE_COUNT(jsonObject);
        LOGGER.info(jsonObject.toString());
        Statistics result = new Gson().fromJson(jsonObject, Statistics.class);
        Assert.assertTrue(result.equals(statistics));
    }


    @Test(groups = {"experimentA"}, dependsOnGroups = {"setup", "experimentStatistic"},
            dependsOnMethods = {"t_experimentA"})
    public void update_experimentA_4() {
        Set<String> actions = new HashSet<>();
        actions.add("click");
        Statistics statistics = new Statistics();
        for (Map.Entry<String, String> entry : experimentUserBucketMap.get(validExperimentsLists.get(0).label).entrySet()) {
            Map<String, Integer> eventCounts = bucketLabelToEventCount.get(entry.getValue());
            OutputBucketStatistics bucketStatistics = statistics.getStatisticsByLable(entry.getValue());
            statistics.increaseCounts(eventCounts, (p) -> actions.contains(p));
            bucketStatistics.increaseCounts(eventCounts, (p) -> actions.contains(p));
        }
        response = apiServerConnector.doPost("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics",
                "{\"actions\": [\"love it\"]}");
        assertReturnCode(response, HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonParser().parse(response.asString()).getAsJsonObject();
        StatisticsUtils.COMPUTE_COUNT(jsonObject);
        LOGGER.info(jsonObject.toString());
        Statistics result = new Gson().fromJson(jsonObject, Statistics.class);
        Assert.assertTrue(result.equals(statistics));
    }


    @Test(groups = {"experimentA"},
            dependsOnGroups = {"setup", "experimentStatistic"},
            dependsOnMethods = {"t_experimentA"},
            dataProvider = "TimeRanges",
            dataProviderClass = SharedExperimentDataProvider.class)
    public void t_timeRangeSearch(String start, String end) {
        Map<String, Map<String, Integer>> val = StatisticsUtils.COUNT_EVENT_FROM_TIME_RANGE(start, end);
        response = apiServerConnector.doPost("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics",
                "{\"fromTime\": \"" + start + "\", \"toTime\": \"" + end + "\"}");
        assertReturnCode(response, HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonParser().parse(response.asString()).getAsJsonObject();
        StatisticsUtils.COMPUTE_COUNT(jsonObject);
        LOGGER.debug(jsonObject.toString());
        Statistics statistics = new Statistics();
        for (Map.Entry<String, String> entry : experimentUserBucketMap.get(validExperimentsLists.get(0).label).entrySet()) {
            Map<String, Integer> eventCounts = val.get(entry.getValue());
            statistics.increaseCounts(eventCounts, (p) -> false);
            if (eventCounts.size() > 0) {
                OutputBucketStatistics bucketStatistics = statistics.getStatisticsByLable(entry.getValue());
                bucketStatistics.increaseCounts(eventCounts, (p) -> false);
            }
        }
        Statistics result = new Gson().fromJson(jsonObject, Statistics.class);
        if (result.getImpressionCounts().getOrDefault("uniqueUserCount", 0) == 0 &&
                result.getJointActionCounts().getOrDefault("uniqueUserCount", 0) == 0) {
            Assert.assertEquals(result.getImpressionCounts(), statistics.getImpressionCounts());
            Assert.assertEquals(result.getJointActionCounts(), statistics.getJointActionCounts());
        } else {
            Assert.assertTrue(result.equals(statistics));
        }
    }


    @Test(groups = {"experimentA"}
            , dependsOnGroups = {"setup", "experimentStatistic"}
    )
    public void t_dailyStatistics() {
        LOGGER.info(SharedExperimentDataProvider.todayDT.toString());
        response = apiServerConnector.doGet("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics/dailies");
        LOGGER.debug(response.asString());
        JsonArray jsonArray = new JsonParser().parse(response.asString()).getAsJsonObject().getAsJsonArray("days");

        for (JsonElement element : jsonArray) {
            String date = element.getAsJsonObject().get("date").getAsString();
            JsonObject perDay = element.getAsJsonObject().getAsJsonObject("perDay");
            LocalDateTime startDateTime = LocalDateTime.parse(date + "T00:00:00-0000", SharedExperimentDataProvider.formatter);
            LocalDateTime endDateTime = startDateTime.plusDays(1).minusSeconds(1);

            Map<String, Map<String, Integer>> val = StatisticsUtils.COUNT_EVENT_FROM_TIME_RANGE(
                    startDateTime.format(SharedExperimentDataProvider.formatter),
                    endDateTime.format(SharedExperimentDataProvider.formatter));
            Statistics perDayStatistics = new Statistics();
            for (Map.Entry<String, String> entry : experimentUserBucketMap.get(validExperimentsLists.get(0).label).entrySet()) {
                Map<String, Integer> eventCounts = val.get(entry.getValue());
                perDayStatistics.increaseCounts(eventCounts, (p) -> false);
                if (eventCounts.size() > 0) {
                    OutputBucketStatistics bucketStatistics = perDayStatistics.getStatisticsByLable(entry.getValue());
                    bucketStatistics.increaseCounts(eventCounts, (p) -> false);
                }
            }

            Statistics result = new Gson().fromJson(perDay, Statistics.class);
            //TODO: full implement equals, for now we are only checking the impression count and joint action count.
            Assert.assertTrue(result.getJointActionCounts().equals(perDayStatistics.getJointActionCounts()));
            Assert.assertTrue(result.getImpressionCounts().equals(perDayStatistics.getImpressionCounts()));
        }
    }


    @Test(groups = {"experimentA"},
            dependsOnGroups = {"setup", "experimentStatistic"},
            dataProvider = "EmptyTimeRangeQueryAndResponse",
            dataProviderClass = SharedExperimentDataProvider.class)
    public void t_DailyCountsEmpty(String start, String end, int expectedResult) {
        String query = StatisticsUtils.TIME_RANGE_QUERY_BUILDER(start, end);
        response = apiServerConnector.doPost("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics/dailies",
                query);
        assertReturnCode(response, HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonParser().parse(response.asString()).getAsJsonObject();
        Assert.assertEquals(jsonObject.getAsJsonArray("days").size(), expectedResult);
    }


    @Test(groups = {"experimentA"},
            dependsOnGroups = {"setup", "experimentStatistic"},
            dataProvider = "TimeRangeQueryNonEmpty",
            dataProviderClass = SharedExperimentDataProvider.class
    )
    public void t_DailyCountValidRange(String start, String end, int expectedResult, int numOfDays) {
        String query = StatisticsUtils.TIME_RANGE_QUERY_BUILDER(start, end);
        response = apiServerConnector.doPost("/analytics/experiments/" + validExperimentsLists.get(0).id + "/statistics/dailies",
                query);
        assertReturnCode(response, HttpStatus.SC_OK);
        ExperimentCumulativeStatistics stats = new Gson().fromJson(response.asString(), ExperimentCumulativeStatistics.class);
        LOGGER.info(stats.toString());
        Assert.assertEquals(stats.days.size(), numOfDays);
        for (DailyStatistics day : stats.days) {
            if (day.date.equals(SharedExperimentDataProvider.yesterday) ||
                    day.date.equals(SharedExperimentDataProvider.today) ||
                    day.date.equals(SharedExperimentDataProvider.tomorrow)) {
                //TODO: figure out a good way to test statistics
                Assert.assertTrue(day.cumulative.jointActionCounts.uniqueUserCount <= expectedResult);
            }
        }
    }

    @AfterClass
    public void t_cleanUp() {
        LOGGER.info("Clean up experiments");
        for (Experiment experiment : validExperimentsLists) {
            response = apiServerConnector.doGet("experiments/" + experiment.id);
            Experiment result = ExperimentFactory.createFromJSONString(response.asString());
            if (!"DRAFT".equals(result.state) && !"TERMINATED".equals(result.state)) {
                response = apiServerConnector.doPut("experiments/" + experiment.id, "{\"state\": \"TERMINATED\"}");
                assertReturnCode(response, HttpStatus.SC_OK);
            }
            response = apiServerConnector.doDelete("experiments/" + experiment.id);
            assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
        }
    }
}
