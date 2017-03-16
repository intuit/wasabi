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
package com.intuit.wasabi.analyticsobjects.counts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.intuit.wasabi.exceptions.AnalyticsException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Top level DTO to save the cumulative counts for an Experiment
 * <p>
 * Fields:
 * <ul>
 * <li>List of {@link DailyCounts} with the counts for each day</li>
 * </ul>
 */

public class ExperimentCumulativeCounts implements Cloneable {

    @ApiModelProperty(required = true)
    private List<DailyCounts> days;

    @JsonIgnore
    public void addDays(DailyCounts day) {
        if (this.days == null) {
            this.days = new ArrayList<>();
        }

        this.days.add(day);
    }

    public List<DailyCounts> getDays() {
        return days;
    }

    public void setDays(List<DailyCounts> value) {
        this.days = value;
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

    @Override
    public ExperimentCumulativeCounts clone() {
        ExperimentCumulativeCounts cloned;

        try {
            cloned = (ExperimentCumulativeCounts) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("ExperimentCumulativeCounts clone not supported: " + e.getMessage(), e);
        }

        if (days != null) {
            List<DailyCounts> clonedDays = new ArrayList<DailyCounts>();

            for (DailyCounts day : days) {
                clonedDays.add(day.clone());
            }

            cloned.setDays(clonedDays);
        }

        return cloned;
    }

    public static class Builder {

        ExperimentCumulativeCounts item;

        public Builder() {
            this.item = new ExperimentCumulativeCounts();
        }

        public Builder withDays(List<DailyCounts> val) {
            this.item.days = val;
            return this;
        }

        public ExperimentCumulativeCounts build() {
            return this.item;
        }
    }
}
