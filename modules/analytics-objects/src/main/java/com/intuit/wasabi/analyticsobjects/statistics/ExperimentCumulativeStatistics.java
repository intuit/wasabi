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
package com.intuit.wasabi.analyticsobjects.statistics;

import com.intuit.wasabi.exceptions.AnalyticsException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a wrapper DTO to save the List of DailyStatistics
 */
public class ExperimentCumulativeStatistics implements Cloneable {

    @ApiModelProperty(required = true)
    private List<DailyStatistics> days;

    public static class Builder {

        ExperimentCumulativeStatistics item;

        public Builder() {
            this.item = new ExperimentCumulativeStatistics();
        }

        public Builder withDays(List<DailyStatistics> value) {
            this.item.days = value;
            return this;
        }

        public ExperimentCumulativeStatistics build() {
            return this.item;
        }
    }

    public List<DailyStatistics> getDays() {
        return days;
    }

    public void setDays(List<DailyStatistics> value) {
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
    public ExperimentCumulativeStatistics clone() {
        ExperimentCumulativeStatistics cloned;

        try {
            cloned = (ExperimentCumulativeStatistics) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("ExperimentCumulativeStatistics clone not supported: " + e.getMessage(), e);
        }

        if (days != null) {
            List<DailyStatistics> clonedDays = new ArrayList<DailyStatistics>();
            for (DailyStatistics day : days) {
                clonedDays.add(day.clone());
            }
            cloned.setDays(clonedDays);
        }

        return cloned;
    }
}
