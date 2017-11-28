/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.tests.model.factory;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.model.Experiment;

/**
 * A factory for Experiments.
 */
public class ExperimentFactory {

    /**
     * Only used to create unique Experiment labels.
     */
    private static int internalId = 0;

    /**
     * Creates a basic Experiment with the required default values but no ID. The values are a name, the default
     * application, a start and end time and the sampling percentage (100%), as well as a hypothesis/description.
     * <p>
     * Sets the creatorID to admin email
     *
     * @return a default Experiment.
     */
    public static Experiment createExperiment() {
        return new Experiment(Constants.DEFAULT_PREFIX_EXPERIMENT + System.currentTimeMillis() + internalId++,
                ApplicationFactory.defaultApplication(), TestUtils.currentTimeString(),
                TestUtils.relativeTimeString(42), 1)
                        .setCreatorID(
                                System.getProperty("user-name") != null ? System.getProperty("user-name") : "admin")
                        .setDescription("A sample Experiment description.").setHypothesisIsCorrect("").setResults("");
    }

    /**
     * Creates a basic Experiment with the required default values but no ID. The values are a name, the default
     * application, a start and end time and the sampling percentage (100%), as well as a hypothesis/description.
     * <p>
     * 
     * @param String experimentLabel- the label of the experiment Sets the creatorID to admin email
     *
     * @return a default Experiment.
     */
    public static Experiment createExperiment(String experimentLabel) {
        return new Experiment(experimentLabel, ApplicationFactory.defaultApplication(), TestUtils.currentTimeString(),
                TestUtils.relativeTimeString(42), 1)
                        .setCreatorID(
                                System.getProperty("user-name") != null ? System.getProperty("user-name") : "admin")
                        .setDescription("A sample Experiment description.");
    }

    /**
     * Creates a basic Experiment with the required default values but no ID. The values are a name, the default
     * application, end time and the sampling percentage (100%), as well as a hypothesis/description.
     * <p>
     * 
     * @param experimentLabel - the label of the experiment
     * @param relativeStartDay -the relative day on which u want your experiment to start
     * @return
     */
    public static Experiment createExperiment(String experimentLabel, int relativeStartDay) {
        return new Experiment(experimentLabel, ApplicationFactory.defaultApplication(),
                TestUtils.relativeTimeString(relativeStartDay), TestUtils.relativeTimeString(42), 1)
                        .setCreatorID(
                                System.getProperty("user-name") != null ? System.getProperty("user-name") : "admin")
                        .setDescription("A sample Experiment description.");
    }

    /**
     * Creates a basic Experiment with the required default values but no ID. The values are a name, the default
     * application and the sampling percentage (100%), as well as a hypothesis/description.
     * <p>
     * 
     * @param experimentLabel - the label of the experiment
     * @param relativeStartDay -the relative day on which u want your experiment to start with respect to current time
     * @param relativeEndDay -the relative day on which u want your experiment to end with respect to current time
     * @return
     */
    public static Experiment createExperiment(String experimentLabel, int relativeStartDay, int relativeEndDay) {
        return new Experiment(experimentLabel, ApplicationFactory.defaultApplication(),
                TestUtils.relativeTimeString(relativeStartDay), TestUtils.relativeTimeString(relativeEndDay), 1)
                        .setCreatorID(
                                System.getProperty("user-name") != null ? System.getProperty("user-name") : "admin")
                        .setDescription("A sample Experiment description.");
    }

    /**
     * Creates a basic Experiment with the required default values and the optional value rule. (No ID!) The values are
     * a name, the default application, a start and end time, the sampling percentage (100%) and a a simple rule.
     *
     * @return an extended Experiment.
     */
    public static Experiment createExperimentWithRule() {
        Experiment experiment = ExperimentFactory.createExperiment();
        experiment.rule = "(salary < 10000) && (state = 'VA')";
        return experiment;
    }

    /**
     * Creates a fully specified Experiment (without an ID). Fully specified means fully specified from an end user's
     * perspective, thus {@code id}, {@code creationTime}, {@code modificationTime} and {@code jsonRule} are not set.
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
     * Create Create a basic Experiment with required default values and with a Tag name
     * 
     */
  
	public static Experiment createExperimentWithTag() {
    	Set<String> tags = new HashSet<String>();
    	tags.add(Constants.DEFAULT_PREFIX_TAG);
        
    	Experiment experiment = ExperimentFactory.createCompleteExperiment();
        experiment.setTags(tags);
        return experiment;
    }

    /**
     * Creates a basic rapid Experiment with the required default values but no ID. The values are a name, the default
     * application, default number of maximum users (10), a start and end time and the sampling percentage (100%), as
     * well as a hypothesis/description and with isRapidExperiment set to true.
     * <p>
     * Sets the creatorID to admin email
     *
     * @return a rapid Experiment.
     */
    public static Experiment createRapidExperiment() {
        return createExperiment().setIsRapidExperiment(true).setUserCap(Constants.DEFAULT_RAPIDEXP_MAX_USERS);

    }

    
    /**
     * Creates a basic rapid Experiment with the required default values but no ID. The values are a name, the default
     * application, a start and end time and the sampling percentage (100%), as
     * well as a hypothesis/description and with isRapidExperiment set to true
     * 
     * @param maxUsersCap - the maximum number of users at which the experiment caps and stops.
     * @return a rapid Experiment.
     */
    public static Experiment createRapidExperiment(int maxUsersCap) {
        return createExperiment().setIsRapidExperiment(true).setUserCap(maxUsersCap);

    }
    
    
    /**
     * Creates a rapid Experiment with the required default values but no ID. The values are a name, the default
     * application, a start and end time and the sampling percentage (100%), as
     * well as a hypothesis/description and with isRapidExperiment set to true.
     * <p>
     * 
     * @param maxUsersCap - the maximum number of users at which the experiment caps and stops.
     * @param experimentLabel- the label of the experiment Sets the creatorID to admin email
     * 
     * @return a rapid Experiment.
     */
    public static Experiment createRapidExperiment(int maxUsersCap, String experimentLabel) {
        return createExperiment(experimentLabel).setIsRapidExperiment(true)
                .setUserCap(maxUsersCap);
    }

    /**
     * Creates a rapid Experiment with the required default values but no ID. The values are a name, the default
     * application, a start and end time and the sampling percentage (100%), as
     * well as a hypothesis/description and with isRapidExperiment set to true.
     * 
     * @param maxUsersCap - the maximum number of users at which the experiment caps and stops
     * @param experimentLabel - the label of the experiment Sets the creatorID to admin email
     * @param relativeStartDay - the relative day on which u want your experiment to start with respect to current time
     * 
     * @return a rapid Experiment.
     */
    public static Experiment createRapidExperiment(int maxUsersCap, String experimentLabel, int relativeStartDay) {
        return createExperiment(experimentLabel, relativeStartDay).setIsRapidExperiment(true)
                .setUserCap(maxUsersCap);
    }
    
    /**
     * Creates a rapid Experiment with the required default values but no ID. The values are a name, the default
     * application, default number of maximum users (10), a start and end time and the sampling percentage (100%), as
     * well as a hypothesis/description and with isRapidExperiment set to true.
     * <p>
     * 
     * @param int maxUsersCap- the maximum number of users at which the experiment stops
     * @param experimentLabel - the label of the experiment
     * @param relativeStartDay -the relative day on which u want your experiment to start with respect to current time
     * @param relativeEndDay -the relative day on which u want your experiment to end with respect to current time
     * 
     * @return a rapid Experiment.
     */
    public static Experiment createRapidExperiment(int maxUsersCap, String experimentLabel, int relativeStartDay, int relativeEndDay) {
        return createExperiment(experimentLabel, relativeStartDay, relativeEndDay).setIsRapidExperiment(true)
                .setUserCap(maxUsersCap);
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
