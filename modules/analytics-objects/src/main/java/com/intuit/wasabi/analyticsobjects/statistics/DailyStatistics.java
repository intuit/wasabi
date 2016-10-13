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
package com.intuit.wasabi.analyticsobjects.statistics;

import com.intuit.wasabi.analyticsobjects.DailyBase;
import com.intuit.wasabi.analyticsobjects.exceptions.AnalyticsException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Objects;

/**
 * Daily statistics wrapper class
 * <p>
 * {@see ExperimentBasicStatistics}
 * {@see ExperimentStatistics}
 */
public class DailyStatistics extends DailyBase implements Cloneable {

    @ApiModelProperty(required = true)
    private ExperimentBasicStatistics perDay;
    @ApiModelProperty(required = true)
    private ExperimentStatistics cumulative;

    protected DailyStatistics(Builder builder) {
        super(builder);
        this.perDay = builder.perDay_;
        this.cumulative = builder.cumulative_;
    }

    public ExperimentBasicStatistics getPerDay() {
        return perDay;
    }

    public ExperimentStatistics getCumulative() {
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
    public DailyStatistics clone() {
        DailyStatistics cloned;

        try {
            cloned = (DailyStatistics) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("DailyStatistics clone not supported: " + e.getMessage(), e);
        }

        if (Objects.nonNull(perDay)) {
            cloned.perDay = perDay.clone();
        }

        if (Objects.nonNull(cumulative)) {
            cloned.cumulative = cumulative.clone();
        }

        return cloned;
    }

    public static class Builder extends DailyBase.Builder<Builder> {
        private ExperimentBasicStatistics perDay_;
        private ExperimentStatistics cumulative_;

        public Builder() {
        }

        public Builder withPerDay(ExperimentBasicStatistics value) {
            perDay_ = value;
            return getThis();
        }

        public Builder withCumulative(ExperimentStatistics value) {
            cumulative_ = value;
            return getThis();
        }

        public DailyStatistics build() {
            return new DailyStatistics(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

    }
}
