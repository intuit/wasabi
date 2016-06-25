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

package com.intuit.wasabi.tests.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created on 6/14/16.
 */
public class StatisticsBase {

    Map<String, Integer> impressionCounts = new HashMap<>();
    Map<String, OutputBucketStatistics.ActionCount> actionCounts = new HashMap<>();
    Map<String, Integer> jointActionCounts = new HashMap<>();

    public Map<String, Integer> getImpressionCounts() {
        return impressionCounts;
    }

    public Map<String, Integer> getJointActionCounts() {
        return jointActionCounts;
    }

    /**
     * this method is used to update the reference count of the actions
     *
     * @param eventCount is a map that contains the the event label and the count of the lable
     */
    public void increaseCounts(Map<String, Integer> eventCount, Predicate<String> predicate) {
        increaseEventCount(eventCount, predicate);
        increaseActionCount(eventCount, predicate);
    }

    /**
     * this method is used to update the reference count of the actions
     *
     * @param eventCounts is a map that contains the the event label and the count of the lable
     */
    private void increaseEventCount(Map<String, Integer> eventCounts, Predicate<String> predicate) {
        int impressionCount = eventCounts.getOrDefault("impression", 0);
        int impressionUniqueCount = Math.min(impressionCount, 1);
        int jointCounts = 0;
        if (!predicate.test("click")) {
            jointCounts += eventCounts.getOrDefault("click", 0);
        }
        if (!predicate.test("love it")) {
            jointCounts += eventCounts.getOrDefault("love it", 0);
        }
        int jointUniqueCount = Math.min(jointCounts, 1);
        impressionCounts.put("eventCount", impressionCounts.getOrDefault("eventCount", 0) + impressionCount);
        impressionCounts.put("uniqueUserCount", impressionCounts.getOrDefault("uniqueUserCount", 0) + impressionUniqueCount);
        jointActionCounts.put("eventCount", jointActionCounts.getOrDefault("eventCount", 0) + jointCounts);
        jointActionCounts.put("uniqueUserCount", jointActionCounts.getOrDefault("uniqueUserCount", 0) + jointUniqueCount);
    }

    /**
     * this method is used to update the reference count of the actions
     *
     * @param eventCounts is a map that contains the the event label and the count of the label
     */
    private void increaseActionCount(Map<String, Integer> eventCounts, Predicate<String> predicate) {
        eventCounts.forEach((k, v) -> {
            if (!"impression".equals(k) && !predicate.test(k)) {
                OutputBucketStatistics.ActionCount data = actionCounts.get(k);
                if (data == null) {
                    data = new OutputBucketStatistics.ActionCount();
                    data.setActionName(k);
                    actionCounts.put(k, data);
                }
                data.setEventCount(data.getEventCount() + v);
                data.setUniqueUserCount(data.getUniqueUserCount() + 1);
            }
        });
    }

}
