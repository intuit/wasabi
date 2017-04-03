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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * A Class to hold a list of experiment labels for batch assignments.
 */
public class ExperimentIDList {


    @ApiModelProperty(value = "a list of experiment uuids for batch assignment", dataType = "java.util.List")
    private List<Experiment.ID> experimentIDs;

    protected ExperimentIDList() {
        super();
    }

    public static Builder newInstance() {
        return new Builder();
    }

    public static Builder from(ExperimentIDList value) {
        return new Builder(value);
    }

    public static Builder withExperimentIDList(List<Experiment.ID> experimentIDs) {
        return new Builder(experimentIDs);
    }


    public static class Builder {

        private ExperimentIDList instance;

        private Builder() {
            instance = new ExperimentIDList();
        }

        private Builder(ExperimentIDList other) {
            this();
            instance.experimentIDs = new ArrayList<>(other.experimentIDs);
        }

        public Builder(List<Experiment.ID> experimentIDs) {
            this();
            instance.experimentIDs = new ArrayList<>(experimentIDs);
        }

        public Builder withExperimentIDs(final List<Experiment.ID> experimentIDs) {
            instance.experimentIDs = new ArrayList<>(experimentIDs);
            return this;
        }

        public ExperimentIDList build() {
            ExperimentIDList result = instance;
            instance = null;
            return result;
        }

    }

    public List<Experiment.ID> getExperimentIDs() {
        return experimentIDs;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
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
