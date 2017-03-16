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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.intuit.wasabi.exceptions.AnalyticsException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * DTO to save counts
 * <p>
 * Fields:
 * <ul>
 * <li>Total counts </li>
 * <li>Counts for unique users</li>
 * </ul>
 */

public class Counts implements Cloneable {

    @ApiModelProperty(value = "total number of counts (can have multiple counts per user)", required = true)
    protected long eventCount;
    @ApiModelProperty(value = "number of unique users with one or more counts", required = true)
    protected long uniqueUserCount;

    public Counts() {
    }

    public Counts(long eventCount, long uniqueUserCount) {
        this.eventCount = eventCount;
        this.uniqueUserCount = uniqueUserCount;
    }

    @JsonIgnore
    public void addEventCount(long value) {
        this.eventCount = this.eventCount + value;
    }

    @JsonIgnore
    public void addCount(Counts value) {
        this.eventCount = this.eventCount + value.getEventCount();
        this.uniqueUserCount = this.uniqueUserCount + value.getUniqueUserCount();
    }

    @JsonIgnore
    public void addUniqueUserCount(long value) {
        this.uniqueUserCount = this.uniqueUserCount + value;
    }

    public long getEventCount() {
        return eventCount;
    }

    public void setEventCount(long value) {
        this.eventCount = value;
    }

    public long getUniqueUserCount() {
        return uniqueUserCount;
    }

    public void setUniqueUserCount(long value) {
        this.uniqueUserCount = value;
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
    public Counts clone() {
        try {
            return (Counts) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("Counts clone not supported: " + e.getMessage(), e);
        }
    }

    public static class Builder {

        private Counts item;

        public Builder() {
            this.item = new Counts();
        }

        public Builder withEventCount(long value) {
            this.item.eventCount = value;
            return this;
        }

        public Builder withUniqueUserCount(long value) {
            this.item.uniqueUserCount = value;
            return this;
        }

        public Counts build() {
            return this.item;
        }
    }
}
