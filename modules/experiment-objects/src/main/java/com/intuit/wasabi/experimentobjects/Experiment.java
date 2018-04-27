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
package com.intuit.wasabi.experimentobjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateMidnight;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT;
import static com.intuit.wasabi.experimentobjects.Experiment.State.PAUSED;
import static com.intuit.wasabi.experimentobjects.Experiment.State.RUNNING;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;

/**
 * An experiment
 */
public class Experiment implements Cloneable, ExperimentBase, Serializable {

    @ApiModelProperty(value = "unique experiment ID", dataType = "UUID", required = true)
    private Experiment.ID id;
    @ApiModelProperty(value = "experiment label; unique within the application", dataType = "String", required = true)
    private Experiment.Label label;
    @ApiModelProperty(value = "name of the application; e.g. \"QBO\"", dataType = "String", required = true)
    private Application.Name applicationName;
    @ApiModelProperty(value = "earliest time the experiment allows bucket assignments", required = true)
    private Date startTime;
    @ApiModelProperty(value = "latest time the experiment allows bucket assignments", required = true)
    private Date endTime;
    @ApiModelProperty(value = "probability of an eligible user being assigned into the experiment; " +
            "in range: (0, 1]",
            required = true)
    private Double samplingPercent;
    @ApiModelProperty(value = "description/hypothesis of the experiment", required = true)
    private String description;
    @ApiModelProperty(value = "defines whether the hypothesis of the experiment was correct", required = false)
    private String hypothesisIsCorrect;
    @ApiModelProperty(value = "results of the experiment", required = false)
    private String results;
    @ApiModelProperty(value = "defines a user segment, i.e., if the rule validates to true, user is part of the segment", required = false)
    private String rule;
    @ApiModelProperty(value = "defines a user segment in json, i.e., if the rule validates to true, user is part of the segment", required = false)
    private String ruleJson;
    @ApiModelProperty(value = "time experiment was created", required = true)
    private Date creationTime;
    @ApiModelProperty(value = "last time experiment was modified", required = true)
    private Date modificationTime;
    @ApiModelProperty(value = "state of the experiment", required = true)
    private State state;
    @ApiModelProperty(value = "is personalization enabled for this experiment", required = false)
    private Boolean isPersonalizationEnabled;
    @ApiModelProperty(value = "model name", required = false)
    private String modelName;
    @ApiModelProperty(value = "model version no.", required = false)
    private String modelVersion;
    @ApiModelProperty(value = "is this a rapid experiment", required = false)
    private Boolean isRapidExperiment;
    @ApiModelProperty(value = "maximum number of users to allow before pausing the experiment", required = false)
    private Integer userCap;
    @ApiModelProperty(value = "creator of the experiment", required = false)
    private String creatorID;
    @ApiModelProperty(value = "a set of experiment tags")
    private Set<String> tags;
    @ApiModelProperty(value = "client experimentation", required = false)
    private String sourceURL;
    @ApiModelProperty(value = "client experimentation", required = false)
    private String experimentType;

    public String getSourceURL() {
        return sourceURL;
    }

    public void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL;
    }



    public String getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(String experimentType) {
        this.experimentType = experimentType;
    }


    private List<Bucket> buckets = new ArrayList<>();

    private Boolean favorite;

    private List<Experiment.ID> exclusionIdList;

    private List<ExperimentPage> experimentPageList;

    private Integer priority;

    protected Experiment() {
        super();
    }

    public static Builder withID(ID id) {
        return new Builder(id);
    }

    public static Builder from(Experiment experiment) {
        return new Builder(experiment);
    }

    @Override
    public Experiment.ID getID() {
        return id;
    }

    public void setID(Experiment.ID id) {
        this.id = id;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHypothesisIsCorrect() {
        return hypothesisIsCorrect;
    }

    public void setHypothesisIsCorrect(String hypothesisIsCorrect) {
        this.hypothesisIsCorrect = hypothesisIsCorrect;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    @Override
    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getRuleJson() {
        return ruleJson;
    }

    public void setRuleJson(String ruleJson) {
        this.ruleJson = ruleJson;
    }

    public Double getSamplingPercent() {
        return samplingPercent;
    }

    public void setSamplingPercent(Double samplingPercent) {
        this.samplingPercent = samplingPercent;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        Preconditions.checkNotNull(startTime);
        this.startTime = startTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        Preconditions.checkNotNull(endTime);
        this.endTime = endTime;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Experiment.Label getLabel() {
        return label;
    }

    public void setLabel(Experiment.Label label) {
        Preconditions.checkNotNull(label);
        this.label = label;
    }

    @Override
    public Application.Name getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(Application.Name applicationName) {
        Preconditions.checkNotNull(applicationName);
        this.applicationName = applicationName;
    }

    @Override
    public Boolean getIsPersonalizationEnabled() {
        return isPersonalizationEnabled;
    }

    public void setIsPersonalizationEnabled(Boolean isPersonalizationEnabled) {
        this.isPersonalizationEnabled = isPersonalizationEnabled;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Integer getUserCap() {
        return userCap;
    }

    public void setUserCap(Integer userCap) {
        this.userCap = userCap;
    }

    public Boolean getIsRapidExperiment() {
        return isRapidExperiment;
    }

    public void setIsRapidExperiment(Boolean isRapidExperiment) {
        this.isRapidExperiment = isRapidExperiment;
    }

    public String getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(String creatorID) {
        this.creatorID = creatorID;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    @JsonIgnore
    public Boolean isFavorite() {
        return favorite;
    }

    public List<ID> getExclusionIdList() {
        return exclusionIdList;
    }

    public void setExclusionIdList(List<ID> exclusionIdList) {
        this.exclusionIdList = exclusionIdList;
    }

    public List<ExperimentPage> getExperimentPageList() {
        return experimentPageList;
    }

    public void setExperimentPageList(List<ExperimentPage> experimentPageList) {
        this.experimentPageList = experimentPageList;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(1, 31).append(id)
                .append(creationTime)
                .append(modificationTime)
                .append(description)
                .append(hypothesisIsCorrect)
                .append(results)
                .append(rule)
                .append(samplingPercent)
                .append(startTime)
                .append(endTime)
                .append(state)
                .append(label)
                .append(applicationName)
                .append(isPersonalizationEnabled)
                .append(modelName)
                .append(modelVersion)
                .append(isRapidExperiment)
                .append(userCap)
                .append(creatorID)
                .append(tags)
                .append(buckets)
                .append(sourceURL)
                .append(experimentType)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Experiment))
            return false;

        Experiment other = (Experiment) obj;
        return new EqualsBuilder()
                .append(id, other.getID())
                .append(creationTime, other.getCreationTime())
                .append(modificationTime, other.getModificationTime())
                .append(description, other.getDescription())
                .append(hypothesisIsCorrect, other.getHypothesisIsCorrect())
                .append(results, other.getResults())
                .append(rule, other.getRule())
                .append(samplingPercent, other.getSamplingPercent())
                .append(startTime, other.getStartTime())
                .append(endTime, other.getEndTime())
                .append(state, other.getState())
                .append(label, other.getLabel())
                .append(applicationName, other.getApplicationName())
                .append(isPersonalizationEnabled, other.getIsPersonalizationEnabled())
                .append(modelName, other.getModelName())
                .append(modelVersion, other.getModelVersion())
                .append(isRapidExperiment, other.getIsRapidExperiment())
                .append(userCap, other.getUserCap())
                .append(creatorID, other.getCreatorID())
                .append(tags, other.getTags())
                .append(buckets, other.getBuckets())
                .append(sourceURL,other.getSourceURL())
                .append(experimentType,other.getExperimentType())
                .isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public Experiment clone() {
        try {
            return (Experiment) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates if this experiment could change state to <tt>desiredState</tt>.
     *
     * @param desiredState the state to transition in to
     * @return <code>true</code> if state change is valid, <code>false</code>
     * otherwise
     */

    public boolean isStateTransitionValid(State desiredState) {
        if (state == null) {
            return false;
        } else {
            return state.isStateTransitionAllowed(desiredState);
        }
    }

    /**
     * Signals if this experiment allows to make changes to its data. This is
     * the case if the current state is DRAFT.
     *
     * @return <code>true</code> if changes are valid, <code>false</code> otherwise
     */

    @JsonIgnore
    public boolean isChangeable() {
        return state.equals(DRAFT) || state.equals(RUNNING) || state.equals(PAUSED);
    }

    /**
     * Signals if this experiment is deleted.
     *
     * @return <code>true</code> if state == DELETED, <code>false</code> otherwise
     */

    @JsonIgnore
    public boolean isDeleted() {
        return state.equals(State.DELETED);
    }

    /**
     * Calculates the last day of the experiment.
     * <p>
     * This is generally the experiment end date, but may be an earlier date if
     * the experiment was TERMINATED early. In this case the modification date
     * is used.
     *
     * @return earliestDay
     */
    @JsonIgnore
    public DateMidnight calculateLastDay() {
        DateMidnight earliestDay = new DateMidnight(endTime);
        if (state.equals(State.TERMINATED)) {
            DateMidnight modifiedDay = new DateMidnight(modificationTime);
            if (modifiedDay.isBefore(earliestDay)) {
                earliestDay = modifiedDay;
            }
        }
        return earliestDay;
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        if (null != tags)
            this.tags = new TreeSet<>(tags);
        else
            this.tags = tags;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<Bucket> buckets) {
        if (buckets == null)
            this.buckets = new ArrayList<>();
        else
            this.buckets = buckets;
    }

    //TODO: redesign state and state transition to be state machine
    public enum State {

        DRAFT, RUNNING, PAUSED, TERMINATED, DELETED;

        public static State toExperimentState(String state) {
            for (State experimentState : State.values()) {
                if (experimentState.name().equalsIgnoreCase(state)) {
                    return experimentState;
                }
            }

            return null;
        }

        public boolean isActiveState() {
            return this.equals(RUNNING)
                    || this.equals(PAUSED);
        }

        public boolean isStateTransitionAllowed(State newState) {
            return ExperimentStateTransition.isStateTransitionAllowed(
                    this, newState);
        }

        public enum ExperimentStateTransition {

            DRAFT(State.DRAFT, State.RUNNING, State.PAUSED, State.DELETED),
            RUNNING(State.RUNNING, State.PAUSED, State.TERMINATED),
            PAUSED(State.PAUSED, State.RUNNING, State.TERMINATED),
            TERMINATED(State.TERMINATED, State.DELETED),
            DELETED(State.DELETED);

            private static final Map<State, ArrayList<State>> m =
                    new EnumMap<>(State.class);

            static {
                for (ExperimentStateTransition trans :
                        ExperimentStateTransition.values()) {
                    for (State s : trans.getAllowedStateTransitions()) {
                        m.put(s, new ArrayList<>(
                                trans.getAllowedStateTransitions()));
                    }
                }
            }

            private final transient List<State> allowedStateTransitions;

            ExperimentStateTransition(State... allowedTransitions) {
                this.allowedStateTransitions = asList(allowedTransitions);
            }

            /**
             * @param oldState original experiment state
             * @param newState target experiment state
             * @return a boolean value if the state transition is allowed
             */
            protected static boolean isStateTransitionAllowed(State oldState,
                                                              State newState) {
                return m.get(oldState).contains(newState);
            }

            private List<State> getAllowedStateTransitions() {
                return allowedStateTransitions;
            }
        }
    }

    public static class Builder {

        private Experiment instance;

        private Builder(ID id) {
            super();
            instance = new Experiment();
            instance.id = Preconditions.checkNotNull(id);
        }


        private Builder(Experiment other) {
            this(other.getID());
            instance.creationTime = copyDate(other.creationTime);
            instance.modificationTime = copyDate(other.modificationTime);
            instance.description = other.description;
            instance.hypothesisIsCorrect = other.hypothesisIsCorrect;
            instance.results = other.results;
            instance.rule = other.rule;
            instance.ruleJson = other.ruleJson;
            instance.samplingPercent = other.samplingPercent;
            instance.startTime = copyDate(other.startTime);
            instance.endTime = copyDate(other.endTime);
            instance.state = other.state;
            instance.label = other.label;
            instance.applicationName = other.applicationName;
            instance.isPersonalizationEnabled = other.isPersonalizationEnabled;
            instance.modelName = other.modelName;
            instance.modelVersion = other.modelVersion;
            instance.isRapidExperiment = other.isRapidExperiment;
            instance.userCap = other.userCap;
            instance.creatorID = other.creatorID;
            instance.tags = other.tags;
            instance.buckets = other.buckets;
            instance.sourceURL=other.sourceURL;
            instance.experimentType=other.experimentType;
        }

        private Date copyDate(Date date) {
            return date != null
                    ? new Date(date.getTime())
                    : null;
        }

        public Builder withIsPersonalizationEnabled(Boolean isPersonalizationEnabled) {
            instance.isPersonalizationEnabled = isPersonalizationEnabled;
            return this;
        }

        public Builder withIsRapidExperiment(Boolean isRapidExperiment) {
            instance.isRapidExperiment = isRapidExperiment;
            return this;
        }

        public Builder withUserCap(Integer userCap) {
            instance.userCap = userCap;
            return this;
        }

        public Builder withModelName(String modelName) {
            instance.modelName = modelName;
            return this;
        }

        public Builder withModelVersion(String modelVersion) {
            instance.modelVersion = modelVersion;
            return this;
        }

        public Builder withCreationTime(final Date creationTime) {
            this.instance.creationTime = creationTime;
            return this;
        }

        public Builder withModificationTime(final Date modificationTime) {
            instance.modificationTime = modificationTime;
            return this;
        }

        public Builder withDescription(final String description) {
            instance.description = description;
            return this;
        }

        public Builder withHypothesisIsCorrect(final String hypothesisIsCorrect) {
            instance.hypothesisIsCorrect = hypothesisIsCorrect;
            return this;
        }

        public Builder withResults(final String results) {
            instance.results = results;
            return this;
        }

        public Builder withRule(final String rule) {
            instance.rule = rule;
            return this;
        }

        public Builder withSamplingPercent(final Double samplingPercent) {
            instance.samplingPercent = samplingPercent;
            return this;
        }

        public Builder withStartTime(final Date startTime) {
            instance.startTime = startTime;
            return this;
        }

        public Builder withEndTime(final Date endTime) {
            instance.endTime = endTime;
            return this;
        }

        public Builder withState(final State state) {
            instance.state = state;
            return this;
        }

        public Builder withLabel(final Experiment.Label label) {
            instance.label = label;
            return this;
        }

        public Builder withApplicationName(final Application.Name appName) {
            instance.applicationName = appName;
            return this;
        }

        public Builder withCreatorID(final String creatorID) {
            instance.creatorID = creatorID;
            return this;
        }

        public Builder withTags(final Set<String> tags) {
            instance.setTags(tags);
            return this;
        }

        public Builder withBuckets(final List<Bucket> buckets) {
            instance.setBuckets(buckets);
            return this;
        }

        public Builder withSourceURL(final String sourceURL) {
            instance.sourceURL = sourceURL;
            return this;
        }
        public Builder withExperimentType(final String experimentType) {
            instance.experimentType = experimentType;
            return this;
        }

        public Experiment build() {
            Experiment result = instance;
            instance = null;
            return result;
        }
    }

    /**
     * Encapsulates the ID for the experiment
     */
    @JsonSerialize(using = Experiment.ID.Serializer.class)
    @JsonDeserialize(using = Experiment.ID.Deserializer.class)
    public static class ID implements Serializable {

        private UUID id;

        private ID(UUID id) {
            super();
            this.id = Preconditions.checkNotNull(id);
        }

        /**
         * Creates a new, random ID
         *
         * @return ID
         */
        public static ID newInstance() {
            return new ID(randomUUID());
        }

        public static ID valueOf(UUID value) {
            return new ID(value);
        }

        public static ID valueOf(byte[] value) {
            if (value.length != 16) {
                throw new InvalidIdentifierException("Argument \"value\" must " +
                        "be a 16-byte array representing a UUID");
            }

            try {
                LongBuffer buffer = ByteBuffer.wrap(value).asLongBuffer();
                long msb = buffer.get();
                long lsb = buffer.get();

                return new ID(new UUID(msb, lsb));
            } catch (IllegalArgumentException e) {
                throw new InvalidIdentifierException("Invalid experiment " +
                        "identifier \"" + Arrays.toString(value) + "\"", e);
            }
        }

        public static ID valueOf(String value) {
            try {
                return new ID(UUID.fromString(value));
            } catch (IllegalArgumentException e) {
                throw new InvalidIdentifierException("Invalid experiment identifier \"" + value + "\"", e);
            }
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public String toString() {
            return id.toString();
        }

        /**
         * Returns the raw ID
         *
         * @return UUID
         */
        public UUID getRawID() {
            return id;
        }

        public static class Serializer extends JsonSerializer<ID> {
            @Override
            public void serialize(ID value, JsonGenerator generator,
                                  SerializerProvider provider)
                    throws IOException {
                generator.writeString(value.id.toString());
            }
        }

        public static class Deserializer extends JsonDeserializer<ID> {
            @Override
            public ID deserialize(JsonParser parser,
                                  DeserializationContext context)
                    throws IOException {
                return ID.valueOf(parser.getText());
            }
        }
    }

    /**
     * Encapsulates the label for the experiment
     */
    @JsonSerialize(using = Experiment.Label.Serializer.class)
    @JsonDeserialize(using = Experiment.Label.Deserializer.class)
    public static class Label implements Serializable {

        private String label;

        private Label(String label) {
            this.label = Preconditions.checkNotNull(label);
            if (!label.matches("^[_\\-$A-Za-z][_\\-$A-Za-z0-9]*")) {
                throw new InvalidIdentifierException("Experiment label \"" +
                        label + "\" must begin with a letter, dollar sign, or " +
                        "underscore, and must not contain any spaces");
            }
        }

        public static Label valueOf(String value) {
            Label result = new Label(value);
            return result;
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + Objects.hashCode(this.label);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        public static class Serializer extends JsonSerializer<Label> {
            @Override
            public void serialize(Label label, JsonGenerator generator,
                                  SerializerProvider provider)
                    throws IOException {
                generator.writeString(label.toString());
            }
        }

        public static class Deserializer
                extends JsonDeserializer<Label> {
            @Override
            public Label deserialize(JsonParser parser,
                                     DeserializationContext context)
                    throws IOException {
                return Label.valueOf(parser.getText());
            }
        }
    }

    public static class ExperimentAuditInfo {
        private String attributeName;
        private String oldValue;
        private String newValue;

        public ExperimentAuditInfo(String attributeName, String oldValue, String newValue) {
            this.attributeName = attributeName;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getNewValue() {
            return newValue;
        }
    }
}
