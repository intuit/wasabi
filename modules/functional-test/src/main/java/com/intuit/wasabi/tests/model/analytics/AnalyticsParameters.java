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

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps parameters for the analytics endpoint.
 */
public class AnalyticsParameters extends ModelItem {

    /**
     * the interval start time
     */
    public String fromTime;

    /**
     * the interval end time
     */
    public String toTime;

    /**
     * the confidence level
     */
    public double confidenceLevel;

    /**
     * the effect size
     */
    public double effectSize;

    /**
     * a list of actions
     */
    public List<String> actions = new ArrayList<>();

    /**
     * single shot
     */
    public boolean isSingleShot;

    /**
     * should be NORMAL_APPROX or NORMAL_APPROX_SIM
     */
    public String metric;

    /**
     * The mode - should not be used according to Swagger. Can be PRODUCTION or TEST
     */
    public String mode;

    /**
     * Context
     */
    public String context;

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
        AnalyticsParameters.serializationStrategy = serializationStrategy;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return AnalyticsParameters.serializationStrategy;
    }

}
