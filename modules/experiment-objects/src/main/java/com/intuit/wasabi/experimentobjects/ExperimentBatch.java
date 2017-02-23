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

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Class to hold a list of experiment labels for batch assignment
 */
public class ExperimentBatch {

    @ApiModelProperty(required = true, value = "a list of experiment labels for batch assignment")
    private Set<Experiment.Label> labels;
    @ApiModelProperty(required = false, value = "a user profile for segmentation")
    private Map<String, Object> profile;
    @ApiModelProperty(required = false, value = "a user profile for personalization", hidden = true)
    private Map<String, Object> personalizationParameters;

    protected ExperimentBatch() {
        super();
    }

    public static Builder newInstance() {
        return new Builder();
    }

    public static Builder from(ExperimentBatch batch) {
        return new Builder(batch);
    }

    public static class Builder {

        private Builder() {
            instance = new ExperimentBatch();
        }

        private Builder(ExperimentBatch other) {
            this();
            instance.labels = new HashSet<>(other.labels);
            instance.profile = new HashMap<>(other.profile);
            instance.personalizationParameters = new HashMap<>(other.personalizationParameters);
        }

        public ExperimentBatch build() {
            ExperimentBatch result = instance;
            instance = null;
            return result;
        }

        public Builder withLabels(final Set<Experiment.Label> labels) {
            instance.labels = labels;
            return this;
        }

        public Builder withProfile(final Map<String, Object> profile) {
            instance.profile = profile;
            return this;
        }

        public Builder withPersonalizationParameters(final Map<String, Object> personalizationParameters) {
            instance.personalizationParameters = personalizationParameters;
            return this;
        }

        private ExperimentBatch instance;
    }

    public Set<Experiment.Label> getLabels() {
        return labels;
    }

    public Map<String, Object> getProfile() {
        return profile;
    }

    public Map<String, Object> getPersonalizationParameters() {
        return personalizationParameters;
    }

    public void setProfile(Map<String, Object> profile) {
        this.profile = profile;
    }

    public void setLabels(Set<Experiment.Label> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "ExperimentBatch labels=" + labels
                + ", profile=" + profile
                + ", personalizationParameters=" + personalizationParameters;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
