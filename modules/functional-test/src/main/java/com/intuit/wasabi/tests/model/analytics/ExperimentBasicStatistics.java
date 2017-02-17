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
package com.intuit.wasabi.tests.model.analytics;

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import com.intuit.wasabi.tests.model.ModelItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps basic experiment statistics.
 */
public class ExperimentBasicStatistics extends ModelItem {

    /**
     * statistics by bucket, key = bucket label, value = bucket statistics
     */
    public Map<String, BucketStatistics> buckets = new HashMap<>();

    /**
     * joint estimates for all actions
     */
    public Estimate jointActionRate;

    /**
     * statistics per action
     */
    public Map<String, Estimate> actionRates = new HashMap<>();

    /**
     * impressions
     */
    public Counts impressionCounts;

    /**
     * counts for all actions
     */
    public Counts jointActionCounts;

    /**
     * the counts per action
     */
    public Map<String, Counts> actionCounts = new HashMap<>();

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        ExperimentBasicStatistics.serializationStrategy = serializationStrategy;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return ExperimentBasicStatistics.serializationStrategy;
    }
}
