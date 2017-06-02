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
package com.intuit.wasabi.tests.model;

import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

/**
 * A simplified Experiment class which can be instantiated in various ways and
 * modified directly via methods or public access to make it easy to test around with it.
 */
public class Experiment extends ModelItem {

    /**
     * The experiment ID. Soft requirement: Should be fetched from server.
     */
    public String id;

    /**
     * The experiment's label. Required.
     */
    public String label;

    /**
     * The name of the application this experiment belongs to. Required.
     */
    public String applicationName;

    /**
     * The start time. Should be formatted {@code yyyy-MM-dd'T'hh:mm:ssZ}.
     * Use the {@link TestUtils} to create it. Required.
     */
    public String startTime;

    /**
     * The end time. Should be formatted {@code yyyy-MM-dd'T'hh:mm:ssZ}. Use the {@link TestUtils} to create it.
     * Required.
     */
    public String endTime;

    /**
     * The sampling percentage (0, 1]. Required.
     */
    public double samplingPercent;

    /**
     * The description/hypothesis. Optional.
     */
    public String description;

    /**
     * The precision of the experiment hypothesis. Optional.
     */
    public String hypothesisIsCorrect;

    /**
     * The results. Optional.
     */
    public String results;

    /**
     * The selection rules for this experiment. Optional.
     */
    public String rule;

    /**
     * The rules as a JSON String. Should be fetched from the server.
     */
    public String ruleJson;

    /**
     * The creation time. Should be formatted {@code yyyy-MM-dd'T'hh:mm:ssZ}. Should be fetched from server.
     */
    public String creationTime;

    /**
     * The modification time. Should be formatted {@code yyyy-MM-dd'T'hh:mm:ssZ}. Should be fetched from server.
     */
    public String modificationTime;

    /**
     * The experiment state. See {@link Constants} for possible states.
     */
    public String state;

    /**
     * Personalization status.
     */
    public Boolean isPersonalizationEnabled;

    /**
     * The model name to fetch the assignment distribution from.
     */
    public String modelName;

    /**
     * The model version.
     */
    public String modelVersion;

    /**
     * Flags if this Experiment is used for rapid Experimentation.
     */
    public Boolean isRapidExperiment;

    /**
     * userCap for Rapid Experimentation.
     */
    public Integer userCap;

    /**
     * the user who created the experiment
     */
    public String creatorID;

    /**
     * the tags for this experiment
     */
    public Set<String> tags;

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    /**
     * Creates an empty experiment.
     */
    public Experiment() {
    }

    /**
     * Creates a deep copy of the {@code other} Experiment.
     *
     * @param other an Experiment to copy.
     */
    public Experiment(Experiment other) {
        update(other);
    }

    /**
     * Creates an experiment with only an ID.
     *
     * @param id the experiment ID.
     */
    public Experiment(String id) {
        this.id = id;
    }

    /**
     * Creates an experiment.
     *
     * @param label           the experiment label (will be prefixed)
     * @param application     the application name (will be prefixed)
     * @param startTime       the start time
     * @param endTime         the end time
     * @param samplingPercent the sampling percentage (0, 1]
     */
    public Experiment(String label, Application application, String startTime, String endTime,
                      double samplingPercent) {
        this(label, application, startTime, endTime, samplingPercent, null, null, false, "", "", false, 0, null, null);
    }

    /**
     * Creates an experiment.
     *
     * @param label                    the experiment label (will be prefixed)
     * @param application              the application name (will be prefixed)
     * @param startTime                the start time
     * @param endTime                  the end time
     * @param samplingPercent          the sampling percentage (0, 1]
     * @param description              the experiment description
     * @param rule                     a rule set for the experiment
     * @param isPersonalizationEnabled flag for personalization
     * @param modelName                the model name
     * @param modelVersion             the model version
     * @param isRapidExperiment        flag if rapid experimentation
     * @param userCap                  max users for rapid experiments
     * @param creatorID                the creator
     * @param tags                     the set of tags
     */
    public Experiment(String label, Application application, String startTime, String endTime, double samplingPercent,
                      String description, String rule, Boolean isPersonalizationEnabled, String modelName,
                      String modelVersion, Boolean isRapidExperiment, Integer userCap, String creatorID,
                      Set<String> tags) {
        this.setLabel(label)
                .setApplication(application)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setSamplingPercent(samplingPercent)
                .setDescription(description)
                .setRule(rule)
                .setModelName(modelName)
                .setModelVersion(modelVersion)
                .setIsPersonalizationEnabled(isPersonalizationEnabled)
                .setIsRapidExperiment(isRapidExperiment)
                .setUserCap(userCap)
                .setCreatorID(creatorID)
                .setTags(tags);
    }

    /**
     * Sets the id and returns this instance. Allows for builder patterns.
     *
     * @param id the experiment id. Will be prefixed.
     * @return this
     */
    public Experiment setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the label and returns this instance. Allows for builder patterns.
     *
     * @param label the experiment label. Will be prefixed.
     * @return this
     */
    public Experiment setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the application name and returns this instance. Allows for builder patterns.
     *
     * @param application the application to extract the name. Will be prefixed
     * @return this
     */
    public Experiment setApplication(Application application) {
        this.applicationName = application.name;
        return this;
    }

    /**
     * Sets the start time and returns this instance. Allows for builder patterns.
     *
     * @param startTime the experiment start time
     * @return this
     */
    public Experiment setStartTime(Date startTime) {
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(startTime);
        return this.setStartTime(gcal);
    }

    /**
     * Sets the start time and returns this instance. Allows for builder patterns.
     *
     * @param startTime the experiment start time
     * @return this
     */
    public Experiment setStartTime(Calendar startTime) {
        return this.setStartTime(TestUtils.getTimeString(startTime));
    }

    /**
     * Sets the start time and returns this instance. Allows for builder patterns.
     *
     * @param startTime the experiment start time
     * @return this
     */
    public Experiment setStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * Sets the end time and returns this instance. Allows for builder patterns.
     *
     * @param endTime the experiment end time
     * @return this
     */
    public Experiment setEndTime(Date endTime) {
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(endTime);
        return this.setEndTime(gcal);
    }

    /**
     * Sets the end time and returns this instance. Allows for builder patterns.
     *
     * @param endTime the experiment end time
     * @return this
     */
    public Experiment setEndTime(Calendar endTime) {
        return this.setEndTime(TestUtils.getTimeString(endTime));
    }

    /**
     * Sets the end time and returns this instance. Allows for builder patterns.
     *
     * @param endTime the experiment end time
     * @return this
     */
    public Experiment setEndTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

    /**
     * Sets the sampling percentage and returns this instance. Allows for builder patterns.
     *
     * @param samplingPercent the sampling percentage (0, 1]
     * @return this
     */
    public Experiment setSamplingPercent(double samplingPercent) {
        this.samplingPercent = samplingPercent;
        return this;
    }

    /**
     * Sets the description/hypothesis and returns this instance. Allows for builder patterns.
     *
     * @param description the description
     * @return this
     */
    public Experiment setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets whether the hypothesis of the experiment was correct and returns this instance. Allows for builder patterns.
     *
     * @param hypothesisIsCorrect the description
     * @return this
     */
    public Experiment setHypothesisIsCorrect(String hypothesisIsCorrect) {
        this.hypothesisIsCorrect = hypothesisIsCorrect;
        return this;
    }

    /**
     * Sets the results and returns this instance. Allows for builder patterns.
     *
     * @param results the results
     * @return this
     */
    public Experiment setResults(String results) {
        this.results = results;
        return this;
    }

    /**
     * Sets the rule and returns this instance. Allows for builder patterns.
     *
     * @param rule the rule to be set
     * @return this
     */
    public Experiment setRule(String rule) {
        this.rule = rule;
        return this;
    }

    /**
     * Sets the state and returns this instance. Allows for builder patterns.
     *
     * @param state the state to be set
     * @return this
     */
    public Experiment setState(String state) {
        this.state = state;
        return this;
    }

    /**
     * Sets the creation time and returns this instance. Allows for builder patterns.
     *
     * @param creationTime the experiment start time
     * @return this
     */
    public Experiment setCreationTime(Date creationTime) {
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(creationTime);
        return this.setCreationTime(gcal);
    }

    /**
     * Sets the creation time and returns this instance. Allows for builder patterns.
     *
     * @param creationTime the experiment start time
     * @return this
     */
    public Experiment setCreationTime(Calendar creationTime) {
        return this.setCreationTime(TestUtils.getTimeString(creationTime));
    }

    /**
     * Sets the creation time and returns this instance. Allows for builder patterns.
     *
     * @param creationTime the experiment start time
     * @return this
     */
    public Experiment setCreationTime(String creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    /**
     * Sets the modification time and returns this instance. Allows for builder patterns.
     *
     * @param modificationTime the experiment start time
     * @return this
     */
    public Experiment setModificationTime(Date modificationTime) {
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(modificationTime);
        return this.setCreationTime(gcal);
    }

    /**
     * Sets the modification time and returns this instance. Allows for builder patterns.
     *
     * @param modificationTime the experiment start time
     * @return this
     */
    public Experiment setModificationTime(Calendar modificationTime) {
        return this.setCreationTime(TestUtils.getTimeString(modificationTime));
    }

    /**
     * Sets the modification time and returns this instance. Allows for builder patterns.
     *
     * @param modificationTime the experiment start time
     * @return this
     */
    public Experiment setModificationTime(String modificationTime) {
        this.modificationTime = modificationTime;
        return this;
    }

    /**
     * Sets the json of the rule.
     *
     * @param ruleJson the json format of the rule
     * @return this
     */
    public Experiment setRuleJson(String ruleJson) {
        this.ruleJson = ruleJson;
        return this;
    }

    /**
     * Sets the personalization enabled status.
     *
     * @param isPersonalizationEnabled enable the personalization
     * @return this
     */
    public Experiment setIsPersonalizationEnabled(Boolean isPersonalizationEnabled) {
        this.isPersonalizationEnabled = isPersonalizationEnabled;
        return this;
    }

    /**
     * Sets modelName associated with the experiment
     *
     * @param modelName the model name
     * @return this
     */
    public Experiment setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    /**
     * Sets modelVersion associated with the experiment.
     *
     * @param modelVersion the model version
     * @return this
     */
    public Experiment setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
        return this;
    }

    /**
     * Sets isRapidExperiment associated with the experiment.
     *
     * @param isRapidExperiment flag if rapid Experiment
     * @return this
     */
    public Experiment setIsRapidExperiment(Boolean isRapidExperiment) {
        this.isRapidExperiment = isRapidExperiment;
        return this;
    }

    /**
     * Sets the user Cap for rapid experimentation.
     *
     * @param userCap max number of users
     * @return this
     */
    public Experiment setUserCap(Integer userCap) {
        this.userCap = userCap;
        return this;
    }

    /**
     * Sets the creator ID
     *
     * @param creatorID the creator
     * @return this
     */
    public Experiment setCreatorID(String creatorID) {
        this.creatorID = creatorID;
        return this;
    }

    /**
     * Sets the tags for an experiment
     *
     * @param tags the tags
     * @return this
     */
    public Experiment setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        Experiment.serializationStrategy = serializationStrategy;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return Experiment.serializationStrategy;
    }

}
