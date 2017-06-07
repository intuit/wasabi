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
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

/**
 * Object for holding an experiment with Priotization.
 * <p>
 * TODO: At the moment this is a clone of experiment. This should be changed in the future.
 */
public class PrioritizedExperiment implements Cloneable, ExperimentBase, Serializable {

    @ApiModelProperty(value = "unique experiment ID", required = true)
    private Experiment.ID id;
    @ApiModelProperty(value = "experiment label; unique within the application", required = true)
    private Experiment.Label label;
    @ApiModelProperty(value = "name of the application; e.g. \"QBO\"", required = true)
    private Application.Name applicationName;
    @ApiModelProperty(value = "earliest time the experiment allows bucket assignments", required = true)
    private Date startTime;
    @ApiModelProperty(value = "latest time the experiment allows bucket assignments", required = true)
    private Date endTime;
    @ApiModelProperty(value = "probability of an eligible user being assigned into the experiment; " +
            "in range: (0, 1]",
            required = true)
    private Double samplingPercent;
    @ApiModelProperty(value = "defines a user segment, i.e., if the rule validates to true, user is part of the segment", required = false)
    private String rule;
    @ApiModelProperty(value = "description of the experiment", required = true)
    private String description;
    @ApiModelProperty(value = "time experiment was created", required = true)
    private Date creationTime;
    @ApiModelProperty(value = "last time experiment was modified", required = true)
    private Date modificationTime;
    @ApiModelProperty(value = "state of the experiment", required = true)
    private Experiment.State state;
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
    @ApiModelProperty(required = false)
    private String creatorID = "";
    @ApiModelProperty(value = "priority within the application", required = true)
    private Integer priority;
    @ApiModelProperty(value = "a set of experiment tags")
    private Set<String> tags;

    protected PrioritizedExperiment() {
        super();
    }

    public static Builder withID(Experiment.ID id) {
        return new Builder(id);
    }

    public static Builder from(Experiment experiment, Integer priority) {
        return new Builder(experiment, priority);
    }

    @Override
    public Experiment.ID getID() {
        return id;
    }

    public void setID(Experiment.ID id) {
        this.id = id;
    }

    @Override
    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        this.startTime = startTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public Experiment.State getState() {
        return state;
    }

    public void setState(Experiment.State state) {
        this.state = state;
    }

    @Override
    public Experiment.Label getLabel() {
        return label;
    }

    public void setLabel(Experiment.Label label) {
        this.label = label;
    }

    @Override
    public Application.Name getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(Application.Name applicationName) {
        this.applicationName = applicationName;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder(1, 31).append(id)
                .append(creationTime)
                .append(modificationTime)
                .append(description)
                .append(samplingPercent)
                .append(rule)
                .append(startTime)
                .append(endTime)
                .append(state)
                .append(label)
                .append(applicationName)
                .append(priority)
                .append(isPersonalizationEnabled)
                .append(modelName)
                .append(modelVersion)
                .append(isRapidExperiment)
                .append(userCap)
                .append(creatorID)
                .append(tags)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof PrioritizedExperiment))
            return false;

        PrioritizedExperiment other = (PrioritizedExperiment) obj;
        return new EqualsBuilder()
                .append(id, other.getID())
                .append(creationTime, other.getCreationTime())
                .append(modificationTime, other.getModificationTime())
                .append(description, other.getDescription())
                .append(samplingPercent, other.getSamplingPercent())
                .append(rule, other.getRule())
                .append(startTime, other.getStartTime())
                .append(endTime, other.getEndTime())
                .append(state, other.getState())
                .append(label, other.getLabel())
                .append(applicationName, other.getApplicationName())
                .append(priority, other.getPriority())
                .append(isPersonalizationEnabled, other.getIsPersonalizationEnabled())
                .append(modelName, other.getModelName())
                .append(modelVersion, other.getModelVersion())
                .append(userCap, other.getUserCap())
                .append(isRapidExperiment, other.getIsRapidExperiment())
                .append(creatorID, other.getCreatorID())
                .append(tags, other.getTags())
                .isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public PrioritizedExperiment clone() throws CloneNotSupportedException {
        return (PrioritizedExperiment) super.clone();
    }

    /**
     * Signals if this experiment is deleted.
     *
     * @return <code>true</code> if state == DELETED, <code>false</code> otherwise
     */

    @JsonIgnore
    public boolean isDeleted() {
        return state.equals(Experiment.State.DELETED);
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

    public static class Builder {

        private PrioritizedExperiment instance;

        private Builder(Experiment.ID id) {
            super();
            instance = new PrioritizedExperiment();
            instance.id = Preconditions.checkNotNull(id);
        }


        private Builder(Experiment other, Integer priority) {
            this(other.getID());
            instance.creationTime = copyDate(other.getCreationTime());
            instance.modificationTime = copyDate(other.getModificationTime());
            instance.setDescription(other.getDescription());
            instance.setSamplingPercent(other.getSamplingPercent());
            instance.setRule(other.getRule());
            instance.setStartTime(copyDate(other.getStartTime()));
            instance.setEndTime(copyDate(other.getEndTime()));
            instance.setState(other.getState());
            instance.setLabel(other.getLabel());
            instance.setApplicationName(other.getApplicationName());
            instance.setPriority(priority);
            instance.setIsPersonalizationEnabled(other.getIsPersonalizationEnabled());
            instance.setModelName(other.getModelName());
            instance.setModelVersion(other.getModelVersion());
            instance.isRapidExperiment = other.getIsRapidExperiment();
            instance.userCap = other.getUserCap();
            instance.creatorID = other.getCreatorID();
            instance.tags = other.getTags();
        }

        private Date copyDate(Date date) {
            return date != null
                    ? new Date(date.getTime())
                    : null;
        }

        public Builder withCreationTime(final Date creationTime) {
            this.instance.creationTime = creationTime;
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

        public Builder withDescription(final String description) {
            instance.description = description;
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

        public Builder withState(final Experiment.State state) {
            instance.state = state;
            return this;
        }

        public Builder withIsPersonalizationEnabled(Boolean isPersonalizationEnabled) {
            instance.isPersonalizationEnabled = isPersonalizationEnabled;
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

        public Builder withLabel(final Experiment.Label label) {
            instance.label = label;
            return this;
        }

        public Builder withApplicationName(final Application.Name appName) {
            instance.applicationName = appName;
            return this;
        }

        public Builder withPriority(int priority) {
            instance.setPriority(priority);
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

        public PrioritizedExperiment build() {
            PrioritizedExperiment result = instance;
            instance = null;
            return result;
        }
    }


}
