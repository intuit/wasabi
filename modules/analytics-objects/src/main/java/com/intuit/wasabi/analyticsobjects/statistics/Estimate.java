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

/**
 * DTO to save an estimate with a value and lower and upper boundaries
 *
 * * Fields:
 * <ul>
 * <li>Double: estimate value</li>
 * <li>Double: estimate upper boundary</li>
 * <li>Double: estimate lower boundary</li>
 * </ul>
 */
public class Estimate implements Cloneable {

    @ApiModelProperty(value = "estimate of the random variable", required = true)
    protected Double estimate;
    @ApiModelProperty(value = "lower bound of the estimate", required = true)
    protected Double lowerBound;
    @ApiModelProperty(value = "upper bound of the estimate", required = true)
    protected Double upperBound;

    public Estimate() {
    }

    public Estimate(Double estimate, Double lowerBound, Double upperBound) {
        this.estimate = estimate;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public Double getEstimate() {
        return estimate;
    }

    public void setEstimate(Double value) {
        this.estimate = value;
    }

    public Double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Double value) {
        this.lowerBound = value;
    }

    public Double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Double value) {
        this.upperBound = value;
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
    public Estimate clone() {
        try {
            return (Estimate) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("Estimate clone not supported: " + e.getMessage(), e);
        }
    }

    public static class Builder {

        private Estimate item;

        public Builder() {
            this.item = new Estimate();
        }

        public Builder withEstimate(Double value) {
            this.item.estimate = value;
            return this;
        }

        public Builder withLowerBound(Double value) {
            this.item.lowerBound = value;
            return this;
        }

        public Builder withUpperBound(Double value) {
            this.item.upperBound = value;
            return this;
        }

        public Estimate build() {
            return this.item;
        }
    }
}
