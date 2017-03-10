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

import com.intuit.wasabi.analyticsobjects.DailyBase;
import com.intuit.wasabi.exceptions.AnalyticsException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * DailyCounts Object in order to save the counts for an experiment <br>
 * Either for a specific date or the range form the start of the experiment to the given date
 * Fields:
 * <ul>
 * <li>Date</li>
 * <li>{@link Counts} object for the impressions of the experiment</li>
 * <li>{@link Counts} object for the joint action counts of the experiment</li>
 * <li>List of {@link ActionCounts} for the individual actions in the buckets</li>
 * <li>List of {@link BucketCounts} for the individual buckets in the experiment</li>
 * </ul>
 */

public class DailyCounts extends DailyBase implements Cloneable {

    @ApiModelProperty(value = "counts occurring on the date", required = true)
    private ExperimentCounts perDay;
    @ApiModelProperty(value = "counts occurring on and prior to the date", required = true)
    private ExperimentCounts cumulative;

    protected DailyCounts(Builder builder) {
        super(builder);
        this.perDay = builder.perDay_;
        this.cumulative = builder.cumulative_;
    }

    public static class Builder extends DailyBase.Builder<Builder> {
        private ExperimentCounts perDay_;
        private ExperimentCounts cumulative_;

        public Builder() {
        }

        public Builder withPerDay(ExperimentCounts value) {
            perDay_ = value;
            return getThis();
        }

        public Builder withCumulative(ExperimentCounts value) {
            cumulative_ = value;
            return getThis();
        }

        public DailyCounts build() {
            return new DailyCounts(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }

    public ExperimentCounts getPerDay() {
        return perDay;
    }

    public ExperimentCounts getCumulative() {
        return cumulative;
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
    public DailyCounts clone() {
        DailyCounts cloned;

        try {
            cloned = (DailyCounts) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("DailyCounts clone not supported: " + e.getMessage(), e);
        }

        if (perDay != null) {
            cloned.perDay = perDay.clone();
        }

        if (cumulative != null) {
            cloned.cumulative = cumulative.clone();
        }

        return cloned;
    }
}
