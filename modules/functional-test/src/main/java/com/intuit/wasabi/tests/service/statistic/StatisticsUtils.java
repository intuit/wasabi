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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intuit.wasabi.tests.data.SharedExperimentDataProvider;
import com.intuit.wasabi.tests.model.EventDateTime;
import org.testng.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 6/16/16.
 */
public class StatisticsUtils {

    static void COMPUTE_EVENT_COUNT_PER_LABEL(String bucketLabel, JsonObject events,
                                              Map<String, Map<String, Integer>> bucketLabelToEventCount) {
        Map<String, Integer> event = bucketLabelToEventCount.getOrDefault(bucketLabel, new HashMap<>());
        bucketLabelToEventCount.put(bucketLabel, event);
        for (JsonElement element : events.getAsJsonArray("events")) {
            String name = element.getAsJsonObject().get("name").getAsString().toLowerCase();
            Integer value = event.getOrDefault(name, 0);
            event.put(name, value + 1);
        }
    }


    static void COMPUTE_COUNT(JsonObject jsonObject) {
        jsonObject.remove("jointProgress");
        jsonObject.remove("actionProgress");
        jsonObject.remove("experimentProgress");
        jsonObject.remove("jointActionRate");
        jsonObject.remove("actionRates");
        JsonObject cur = jsonObject.getAsJsonObject("buckets");
        for (Map.Entry<String, JsonElement> entry : cur.entrySet()) {
            entry.getValue().getAsJsonObject().remove("actionRates");
            entry.getValue().getAsJsonObject().remove("jointActionRate");
            entry.getValue().getAsJsonObject().remove("bucketComparisons");
        }
        JsonObject experimentStatistics = jsonObject.getAsJsonObject("buckets");
        for (String label : new String[]{"red", "blue"}) {
            JsonObject bucket = experimentStatistics.getAsJsonObject(label);
            if (bucket.getAsJsonObject("actionCounts").entrySet().size() == 0) {
                experimentStatistics.remove(label);
            }
        }
    }

    /**
     * @param jsonObject
     */
    static void COMPUTE_DAILY_COUNT(JsonObject jsonObject) {
        for (JsonElement element : jsonObject.getAsJsonArray("days")) {
            element.getAsJsonObject().remove("cumulative");
            element.getAsJsonObject().remove("jointProgress");
            element.getAsJsonObject().remove("actionProgress");
            element.getAsJsonObject().remove("experimentProgress");
            element.getAsJsonObject().remove("jointActionRate");
            element.getAsJsonObject().remove("actionRates");
            JsonObject experimentStatistics = element.getAsJsonObject().getAsJsonObject("perDay");
            experimentStatistics.remove("jointActionRate");
            experimentStatistics.remove("actionRates");
            JsonObject buckets = experimentStatistics.getAsJsonObject("buckets");
            for (String label : new String[]{"red", "blue"}) {
                JsonObject bucket = buckets.getAsJsonObject(label);
                bucket.remove("actionRates");
                bucket.remove("jointActionRate");
            }
        }
    }

    static Map<String, List<EventDateTime>> EVENT_DATETIME() {
        Map<String, List<EventDateTime>> result = new HashMap<>();

        for (Object[] values : SharedExperimentDataProvider.validEvents()) {
            String bucket = (String) values[0];
            List<EventDateTime> eventAndDatetimes = result.getOrDefault(bucket, new ArrayList<>());
            result.put(bucket, eventAndDatetimes);
            JsonObject eventJsonObject = new JsonParser().parse((String) values[1]).getAsJsonObject();
            for (JsonElement element : eventJsonObject.getAsJsonArray("events")) {
                String name = element.getAsJsonObject().get("name").getAsString().toLowerCase();
                LocalDateTime value = LocalDateTime.parse(element.getAsJsonObject().get("timestamp").getAsString(),
                        SharedExperimentDataProvider.formatter);
                eventAndDatetimes.add(new EventDateTime(name, value));
            }
        }
        return result;
    }


    static Map<String, Map<String, Integer>> COUNT_EVENT_FROM_TIME_RANGE(String startTime, String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime, SharedExperimentDataProvider.formatter);
        LocalDateTime end = LocalDateTime.parse(endTime, SharedExperimentDataProvider.formatter);
        Map<String, Map<String, Integer>> result = new HashMap<>();
        for (Map.Entry<String, List<EventDateTime>> entry : EVENT_DATETIME().entrySet()) {
            Map<String, Integer> eventCount = result.getOrDefault(entry.getKey(), new HashMap<>());
            result.put(entry.getKey(), eventCount);
            for (EventDateTime eventDateTime : entry.getValue()) {
                if (!eventDateTime.getEventDatetime().isAfter(end) &&
                        !eventDateTime.getEventDatetime().isBefore(start)) {
                    Integer val = eventCount.getOrDefault(eventDateTime.getEventLabel(), 0);
                    eventCount.put(eventDateTime.getEventLabel(), val + 1);
                }
            }
        }
        return result;
    }


    static String TIME_RANGE_QUERY_BUILDER(String start, String end) {
        StringBuffer sb = new StringBuffer("{");
        boolean haveStart = false;
        if (start != null) {
            sb.append("\"fromTime\": \"").append(start).append("\"");
            haveStart = true;
        }
        if (end != null) {
            if (haveStart) {
                sb.append(",");
            }
            sb.append("\"toTime\": \"").append(end).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    static void checkActionRateDifference(JsonObject jsonObject) {
        Assert.assertTrue(jsonObject.has("estimate"));
        Assert.assertTrue(jsonObject.has("lowerBound"));
        Assert.assertTrue(jsonObject.has("upperBound"));
    }
}
