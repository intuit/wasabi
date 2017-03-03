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

import java.util.Map;

/**
 *
 */
public class OutputBucketStatistics extends StatisticsBase {
    private final String label;

    public OutputBucketStatistics(String label) {
        this.label = label;
    }

    Map<String, ActionCount> getActionCounts() {
        return actionCounts;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutputBucketStatistics)) return false;

        OutputBucketStatistics that = (OutputBucketStatistics) o;

        if (!label.equals(that.label)) return false;
        if (!actionCounts.equals(that.actionCounts)) return false;
        if (!impressionCounts.equals(that.impressionCounts)) return false;
        return jointActionCounts.equals(that.jointActionCounts);

    }

    @Override
    public int hashCode() {
        int result = label.hashCode();
        result = 31 * result + actionCounts.hashCode();
        result = 31 * result + impressionCounts.hashCode();
        result = 31 * result + jointActionCounts.hashCode();
        return result;
    }

    public static class ActionCount {
        private String actionName;
        private Integer eventCount;
        private Integer uniqueUserCount;

        public ActionCount() {
            eventCount = 0;
            uniqueUserCount = 0;
        }

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName;
        }

        public Integer getEventCount() {
            return eventCount;
        }

        public void setEventCount(Integer eventCount) {
            this.eventCount = eventCount;
        }

        public Integer getUniqueUserCount() {
            return uniqueUserCount;
        }

        public void setUniqueUserCount(Integer uniqueUserCount) {
            this.uniqueUserCount = uniqueUserCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ActionCount)) return false;

            ActionCount that = (ActionCount) o;

            if (!actionName.equals(that.actionName)) return false;
            if (!eventCount.equals(that.eventCount)) return false;
            return uniqueUserCount.equals(that.uniqueUserCount);

        }

        @Override
        public int hashCode() {
            int result = actionName.hashCode();
            result = 31 * result + eventCount.hashCode();
            result = 31 * result + uniqueUserCount.hashCode();
            return result;
        }
    }
}
