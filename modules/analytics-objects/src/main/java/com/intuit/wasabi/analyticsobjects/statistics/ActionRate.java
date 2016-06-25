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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * DTO to save an action rate
 *
 * Fields:
 * <ul>
 * <li>name of the action</li>
 * <li>{@link Estimate}s value</li>
 * <li>{@link Estimate}s lower value</li>
 * <li>{@link Estimate}s upper value</li>
 * </ul>
 */
public class ActionRate extends Estimate {

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
    public ActionRate clone() {
        return (ActionRate) super.clone();
    }

    public static class Builder {

        private ActionRate item;

        public Builder() {
            this.item = new ActionRate();
        }

        public Builder withActionName(Event.Name value) {
            this.item.actionName = value;
            return this;
        }

        public Builder withEstimateValue(Double value) {
            this.item.estimate = value;
            return this;
        }

        public Builder withEstimateUpper(Double value) {
            this.item.upperBound = value;
            return this;
        }

        public Builder withEstimateLower(Double value) {
            this.item.lowerBound = value;
            return this;
        }

        public Builder withEstimate(Estimate value) {
            this.item.lowerBound = value.getLowerBound();
            this.item.upperBound = value.getUpperBound();
            this.item.estimate = value.getEstimate();

            return this;
        }

        public ActionRate build() {
            return this.item;
        }
    }
}
