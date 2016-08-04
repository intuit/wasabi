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
package com.intuit.wasabi.tests.model.factory;

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.library.util.TestUtils;

/**
 * A factory for Experiments.
 */
public class ExperimentFactory {

    public static String USER_ID = "admin";

    /**
     * Only used to create unique Experiment labels.
     */
    private static int internalId = 0;

    static {
        if (System.getProperty("user-name") != null)
            USER_ID = System.getProperty("user-name");
    }

    /**
     * Creates a basic Experiment with the required default values but no ID.
     * The values are a name, the default application, a start and end time and the sampling percentage (100%).
     *
     * Sets the creatorID to admin email
     *
     * @return a default Experiment.
     */
    public static Experiment createExperiment() {
        return new Experiment(Constants.DEFAULT_PREFIX_EXPERIMENT + System.currentTimeMillis() + internalId++, ApplicationFactory.defaultApplication(),
                TestUtils.currentTimeString(), TestUtils.relativeTimeString(42), 1).setCreatorID(USER_ID);
    }

    /**
     * Creates a basic Experiment with the required default values and the optional value
     * description. (No ID!)
     * The values are a name, the default application, a start and end time, the sampling percentage (100%) and a
     * description.
     *
     * @return an extended Experiment.
     */
    public static Experiment createExperimentWithDescription() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.description = "A sample Experiment description.";
        return experiment;
    }

    /**
     * Creates a basic Experiment with the required default values and the optional value
     * rule. (No ID!)
     * The values are a name, the default application, a start and end time, the sampling percentage (100%) and a
     * a simple rule.
     *
     * @return an extended Experiment.
     */
    public static Experiment createExperimentWithRule() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.rule = "(salary < 10000) && (state = 'VA')";
        return experiment;
    }

    /**
     * Creates a fully specified Experiment (without an ID).
     * Fully specified means fully specified from an end user's perspective,
     * thus {@code id}, {@code creationTime}, {@code modificationTime} and
     * {@code jsonRule} are not set.
     *
     * @return a complete Experiment (no ID)
     */
    public static Experiment createCompleteExperiment() {
        Experiment experiment = ExperimentFactory.createExperimentWithRule();
        experiment.description = "A sample Experiment description.";
        experiment.hypothesisIsCorrect = "Sample hypothesis check";
        experiment.results = "Sample experiment results";
        return experiment;
    }

    /**
     * Creates an Experiment from a JSON String.
     *
     * @param json the JSON String.
     * @return an Experiment represented by the JSON String.
     */
    public static Experiment createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, Experiment.class);
    }
}

