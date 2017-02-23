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

import com.intuit.wasabi.analyticsobjects.Event;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * DTO to save counts for an action
 * <p>
 * Fields:
 * <ul>
 * <li>name of the action</li>
 * <li>Total counts </li>
 * <li>Counts for unique users</li>
 * </ul>
 */

public class ActionCounts extends Counts {

    private Event.Name actionName;

    public ActionCounts() {
    }

    public ActionCounts(Event.Name actionName, long eventCount, long uniqueUserCount) {
        super(eventCount, uniqueUserCount);
        this.actionName = actionName;
    }

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
    public ActionCounts clone() {
        return (ActionCounts) super.clone();
    }

    public static class Builder {

        private ActionCounts item;

        public Builder() {
            this.item = new ActionCounts();
        }

        public Builder withActionName(Event.Name value) {
            this.item.actionName = value;
            return this;
        }

        public Builder withEventCount(long value) {
            this.item.eventCount = value;
            return this;
        }

        public Builder withUniqueUserCount(long value) {
            this.item.uniqueUserCount = value;
            return this;
        }

        public Builder withCountObject(Counts value) {
            this.item.eventCount = value.eventCount;
            this.item.uniqueUserCount = value.uniqueUserCount;
            return this;
        }

        public ActionCounts build() {
            return this.item;
        }
    }
}
