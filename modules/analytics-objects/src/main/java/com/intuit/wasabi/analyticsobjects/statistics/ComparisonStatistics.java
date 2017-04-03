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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intuit.wasabi.exceptions.AnalyticsException;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * DTO that saves the comparison of a action or bucket
 *
 * Fields:
 * <ul>
 * <li>boolean indicating if sufficient data has been collected </li>
 * <li>fraction of the data collected </li>
 * <li>clear winner of the comparison</li>
 * <li>The difference between the two action rates as an {@link Estimate} object </li>
 * <li>smallest observable Effect Size</li>
 * </ul>
 */
public class ComparisonStatistics implements Cloneable {

    @JsonProperty("hasSufficientData")
    protected boolean sufficientData;
    protected Double fractionDataCollected;
    protected Bucket.Label clearComparisonWinner;
    protected Estimate actionRateDifference;
    protected DistinguishableEffectSize smallestDistinguishableEffectSize;

    public boolean isSufficientData() {
        return sufficientData;
    }

    public void setSufficientData(boolean value) {
        this.sufficientData = value;
    }

    public Double getFractionDataCollected() {
        return fractionDataCollected;
    }

    public void setFractionDataCollected(Double value) {
        this.fractionDataCollected = value;
    }

    public Bucket.Label getClearComparisonWinner() {
        return clearComparisonWinner;
    }

    public void setClearComparisonWinner(Bucket.Label value) {
        this.clearComparisonWinner = value;
    }

    public Estimate getActionRateDifference() {
        return actionRateDifference;
    }

    public void setActionRateDifference(Estimate value) {
        this.actionRateDifference = value;
    }

    public DistinguishableEffectSize getSmallestDistinguishableEffectSize() {
        return smallestDistinguishableEffectSize;
    }

    public void setSmallestDistinguishableEffectSize(DistinguishableEffectSize value) {
        this.smallestDistinguishableEffectSize = value;
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
    public ComparisonStatistics clone() {
        ComparisonStatistics cloned;

        try {
            cloned = (ComparisonStatistics) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("ComparisonStatistics clone not supported: " + e.getMessage(), e);
        }

        if (actionRateDifference != null) {
            cloned.setActionRateDifference(actionRateDifference.clone());
        }

        if (smallestDistinguishableEffectSize != null) {
            cloned.setSmallestDistinguishableEffectSize(smallestDistinguishableEffectSize.clone());
        }

        return cloned;
    }

    public static class Builder {

        private ComparisonStatistics item;

        public Builder() {
            this.item = new ComparisonStatistics();
        }

        public Builder withSufficientData(Boolean value) {
            this.item.sufficientData = value;
            return this;
        }

        public Builder withFractionDataCollected(Double value) {
            this.item.fractionDataCollected = value;
            return this;
        }

        public Builder withClearComparisonWinner(Bucket.Label value) {
            this.item.clearComparisonWinner = value;
            return this;
        }

        public Builder withActionRateDifference(Estimate value) {
            this.item.actionRateDifference = value;
            return this;
        }

        public Builder withSmallestDistinguishableEffectSize(DistinguishableEffectSize value) {
            this.item.smallestDistinguishableEffectSize = value;
            return this;
        }

        public ComparisonStatistics build() {
            return this.item;
        }
    }
}
