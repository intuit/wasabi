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
package com.intuit.wasabi.experimentobjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.Objects;

/**
 * Specification of a new experiment
 */
@ApiModel(description = "Subset of Experiment used to create a new experiment.")
public final class NewExperiment implements ExperimentBase {

    // Note, if adding a member variable, be sure to update the builder's
    // copy contructor
    @ApiModelProperty(value = "Experiment ID", dataType = "UUID")
    private Experiment.ID id;
    @ApiModelProperty(value = "Experiment Label", dataType = "String", required = true)
    private Experiment.Label label;
    @ApiModelProperty(value = "Application Name", dataType = "String", required = true)
    private Application.Name applicationName;
    @ApiModelProperty(example = "2014-06-10T00:00:00-0000", required = true)
    private Date startTime;
    @ApiModelProperty(example = "2018-12-25T00:00:00-0000", required = true)
    private Date endTime;
    @ApiModelProperty(required = true)
    private Double samplingPercent;
    @ApiModelProperty(required = true)
    private String description;
    @ApiModelProperty(required = false)
    private String hypothesisIsCorrect = "";
    @ApiModelProperty(required = false)
    private String results = "";
    @ApiModelProperty(required = false)
    private String rule = "";
    @ApiModelProperty(required = false)
    private Boolean isPersonalizationEnabled = false;
    @ApiModelProperty(required = false)
    private String modelName = "";
    @ApiModelProperty(required = false)
    private String modelVersion = "";
    @ApiModelProperty(value = "is this a rapid experiment", required = false)
    private Boolean isRapidExperiment = false;
    @ApiModelProperty(value = "maximum number of users to allow before pausing the experiment", required = false)
    private Integer userCap = Integer.MAX_VALUE;
    @ApiModelProperty(required = false)
    private String creatorID = "";

    public NewExperiment(Experiment.ID id) {
        super();
        this.id = id;
    }

    public NewExperiment() {
        this.id = Experiment.ID.newInstance();
    }

    public static NewExperiment.Builder withID(Experiment.ID id) {
        return new NewExperiment.Builder(id);
    }

    public Experiment.ID getId() {
        return id;
    }

    public void setId(Experiment.ID id) {
        this.id = id;
    }

    /**
     * The ID for the new instance
     *
     * @return the experiment ID
     */
    @Override
    @JsonIgnore
    public Experiment.ID getID() {
        return id;
    }

    public Boolean getIsRapidExperiment() {
        return isRapidExperiment;
    }

    public void setIsRapidExperiment(Boolean isRapidExperiment) {
        this.isRapidExperiment = isRapidExperiment;
    }

    public Integer getUserCap() {
        return userCap;
    }

    public void setUserCap(Integer userCap) {
        this.userCap = userCap;
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

    @Override
    public Experiment.State getState() {
        return Experiment.State.DRAFT;
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

    public void setApplicationName(Application.Name value) {
        applicationName = value;
    }

    public String getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(String value) {
        creatorID = value;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    /**
     * Builder for a new instance
     */
    public static final class Builder {

        private NewExperiment instance;

        private Builder(Experiment.ID id) {
            instance = new NewExperiment(Preconditions.checkNotNull(id));
        }

        public Builder withDescription(final String value) {
            instance.description = value;
            return this;
        }

        public Builder withIsPersonalizationEnabled(Boolean value) {
            instance.isPersonalizationEnabled = value;
            return this;
        }

        public Builder withModelName(String value) {
            instance.modelName = value;
            return this;
        }

        public Builder withModelVersion(String value) {
            instance.modelVersion = value;
            return this;
        }

        public Builder withRule(final String value) {
            instance.rule = value;
            return this;
        }

        public Builder withSamplingPercent(Double value) {
            instance.samplingPercent = Objects.isNull(value) ? 0d : value;
            return this;
        }

        public Builder withStartTime(final Date value) {
            instance.startTime = Preconditions.checkNotNull(value);
            return this;
        }

        public Builder withEndTime(final Date value) {
            instance.endTime = Preconditions.checkNotNull(value);
            return this;
        }

        public Builder withLabel(final Experiment.Label value) {
            instance.label = Preconditions.checkNotNull(value);
            return this;
        }

        public Builder withAppName(final Application.Name value) {
            instance.applicationName = Preconditions.checkNotNull(value);
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

        public Builder withCreatorID(final String value) {
            instance.creatorID = Preconditions.checkNotNull(value);
            return this;
        }


        public NewExperiment build() {
            new ExperimentValidator().validateNewExperiment(instance);
            NewExperiment result = instance;
            instance = null;
            return result;
        }
    }
}
