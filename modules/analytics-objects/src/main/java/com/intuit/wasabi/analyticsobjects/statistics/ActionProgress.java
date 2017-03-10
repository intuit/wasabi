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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO to save the progress of an action
 *
 * Fields:
 * <ul>
 * <li>Name of the action</li>
 * <li>List of winning actions so far</li>
 * <li>List of loosing actions so far</li>
 * <li>boolean to indicate if sufficient data has been collected</li>
 * <li>fraction of the data collected so far</li>
 * </ul>
 */
public class ActionProgress extends Progress {

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
    public ActionProgress clone() {
        return (ActionProgress) super.clone();
    }

    public static class Builder {

        private ActionProgress item;

        public Builder() {
            this.item = new ActionProgress();
        }

        public Builder withActionName(Event.Name value) {
            this.item.actionName = value;
            return this;
        }

        public Builder withWinnersSoFarList(Collection<Bucket.Label> value) {
            Set<Bucket.Label> winnerSoFar = new HashSet<>();
            winnerSoFar.addAll(value);
            this.item.winnersSoFar = winnerSoFar;
            return this;
        }

        public Builder withLosersSoFarList(Collection<Bucket.Label> value) {
            Set<Bucket.Label> loserSoFar = new HashSet<>();
            loserSoFar.addAll(value);
            this.item.losersSoFar = loserSoFar;
            return this;
        }

        public Builder withSufficientData(Boolean value) {
            this.item.hasSufficientData = value;
            return this;
        }

        public Builder withFractionDataCollected(double value) {
            this.item.fractionDataCollected = value;
            return this;
        }

        public Builder withProgress(Progress value) {
            this.item.fractionDataCollected = value.fractionDataCollected;
            this.item.losersSoFar = value.losersSoFar;
            this.item.hasSufficientData = value.hasSufficientData;
            this.item.winnersSoFar = value.winnersSoFar;
            return this;
        }

        public ActionProgress build() {
            return this.item;
        }
    }
}
