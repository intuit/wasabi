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

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * DTO to save the comparison between two actions in two buckets, that have the same name
 *
 * Fields:
 * <ul>
 * <li>Name of the action</li>
 * <li>boolean indicating if sufficient data has been collected </li>
 * <li>fraction of the data collected </li>
 * <li>clear winner of the comparison</li>
 * <li>The difference between the two action rates as an {@link Estimate} object </li>
 * <li>{@link DistinguishableEffectSize}</li>
 * </ul>
 */

public class ActionComparisonStatistics extends ComparisonStatistics {

    private Event.Name actionName;

    public Event.Name getActionName() {
        return actionName;
    }

    public void setActionName(Event.Name value) {
        this.actionName = value;
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
    public ActionComparisonStatistics clone() {
        return (ActionComparisonStatistics) super.clone();
    }

    public static class Builder {

        private ActionComparisonStatistics item;

        public Builder() {
            this.item = new ActionComparisonStatistics();
        }

        public Builder withActionName(Event.Name value) {
            this.item.actionName = value;
            return this;
        }

        public Builder withSufficientData(Boolean value) {
            this.item.sufficientData = value;
            return this;
        }

        public Builder withFractionDataCollected(double value) {
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

        public Builder withComparisonStatistic(ComparisonStatistics value) {
            this.item.actionRateDifference = value.actionRateDifference;
            this.item.clearComparisonWinner = value.clearComparisonWinner;
            this.item.fractionDataCollected = value.fractionDataCollected;
            this.item.smallestDistinguishableEffectSize = value.smallestDistinguishableEffectSize;
            this.item.sufficientData = value.sufficientData;
            return this;
        }

        public ActionComparisonStatistics build() {
            return this.item;
        }
    }
}
