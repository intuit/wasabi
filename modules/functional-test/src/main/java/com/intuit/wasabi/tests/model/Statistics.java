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

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 6/13/16.
 */
public class Statistics extends StatisticsBase {
    @SerializedName("buckets")
    private Map<String, OutputBucketStatistics> experimentStatistics = new HashMap<>();

    public OutputBucketStatistics getStatisticsByLable(String label) {
        OutputBucketStatistics statistics = experimentStatistics.getOrDefault(label, new OutputBucketStatistics(label));
        experimentStatistics.put(label, statistics);
        return statistics;
    }

    public Map<String, OutputBucketStatistics> get() {
        return this.experimentStatistics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Statistics)) return false;

        Statistics that = (Statistics) o;

        if (!experimentStatistics.equals(that.experimentStatistics)) return false;
        if (!impressionCounts.equals(that.impressionCounts)) return false;
        if (!actionCounts.equals(that.actionCounts)) return false;
        return jointActionCounts.equals(that.jointActionCounts);

    }

    @Override
    public int hashCode() {
        int result = experimentStatistics.hashCode();
        result = 31 * result + impressionCounts.hashCode();
        result = 31 * result + actionCounts.hashCode();
        result = 31 * result + jointActionCounts.hashCode();
        return result;
    }
}
