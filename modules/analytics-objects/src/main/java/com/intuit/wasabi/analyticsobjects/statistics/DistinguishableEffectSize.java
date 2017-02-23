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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * DTO that saves the smallest distinguishable effect size <br>
 *
 * Fields:
 * <ul>
 * <li>negative effect size </li>
 * <li>positive effect size  </li>
 * </ul>
 */
public class DistinguishableEffectSize implements Cloneable {

    private Double negativeEffectSize;
    private Double positiveEffectSize;

    public DistinguishableEffectSize() {
    }

    public DistinguishableEffectSize(Double negativeEffectSize, Double positiveEffectSize) {
        this.negativeEffectSize = negativeEffectSize;
        this.positiveEffectSize = positiveEffectSize;
    }

    public Double getNegativeEffectSize() {
        return negativeEffectSize;
    }

    public void setNegativeEffectSize(Double value) {
        this.negativeEffectSize = value;
    }

    public Double getPositiveEffectSize() {
        return positiveEffectSize;
    }

    public void setPositiveEffectSize(Double value) {
        this.positiveEffectSize = value;
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
    public DistinguishableEffectSize clone() {
        try {
            return (DistinguishableEffectSize) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("DistinguishableEffectSize clone not supported: " + e.getMessage(), e);
        }
    }

    public static class Builder {

        DistinguishableEffectSize item;

        public Builder() {
            this.item = new DistinguishableEffectSize();
        }

        public Builder withNegativeEffectSize(Double value) {
            this.item.negativeEffectSize = value;
            return this;
        }

        public Builder withPositiveEffectSize(Double value) {
            this.item.positiveEffectSize = value;
            return this;
        }

        public DistinguishableEffectSize build() {
            return this.item;
        }
    }
}
