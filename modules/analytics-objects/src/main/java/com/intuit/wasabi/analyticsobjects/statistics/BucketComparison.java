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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.Event.Name;
import com.intuit.wasabi.exceptions.AnalyticsException;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * DTO to save the comparison of two buckets <br><br>
 * This object is always saved within a bucket statistics object, thus the "other label" refers <br>
 * to the other bucket that is compared to the bucket defined in the bucketStatistics Object
 *
 * Fields:
 * <ul>
 * <li>Name of the other bucket this bucket is compared with</li>
 * <li>A {@link ComparisonStatistics} object for the joint action comparison</li>
 * <li>List of {@link ActionComparisonStatistics} -  the comparisons between the individual actions in the two buckets</li>
 * </ul>
 */
public class BucketComparison implements Cloneable {

    private Bucket.Label otherLabel;
    private ComparisonStatistics jointActionComparison;
    private Map<Event.Name, ActionComparisonStatistics> actionComparisons;

    public Bucket.Label getOtherLabel() {
        return otherLabel;
    }

    public void setOtherLabel(Bucket.Label value) {
        this.otherLabel = value;
    }

    public ComparisonStatistics getJointActionComparison() {
        return jointActionComparison;
    }

    public void setJointActionComparison(ComparisonStatistics value) {
        this.jointActionComparison = value;
    }

    public Map<Event.Name, ActionComparisonStatistics> getActionComparisons() {
        return actionComparisons;
    }

    public void setActionComparisons(Map<Event.Name, ActionComparisonStatistics> value) {
        this.actionComparisons = value;
    }

    @JsonIgnore
    public void addToActionComparisons(Event.Name actionName, ActionComparisonStatistics item) {
        if (this.actionComparisons == null) {
            this.actionComparisons = new HashMap<>();
        }
        this.actionComparisons.put(actionName, item);
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
    public BucketComparison clone() {
        BucketComparison cloned;

        try {
            cloned = (BucketComparison) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("BucketComparison clone not supported: " + e.getMessage(), e);

        }

        if (jointActionComparison != null) {
            cloned.setJointActionComparison(jointActionComparison.clone());
        }

        if (actionComparisons != null) {
            Map<Event.Name, ActionComparisonStatistics> clonedActions = new HashMap<>();
            for (Entry<Name, ActionComparisonStatistics> entry : actionComparisons.entrySet()) {
                clonedActions.put(entry.getKey(), entry.getValue().clone());
            }
            cloned.setActionComparisons(clonedActions);
        }

        return cloned;
    }

    public static class Builder {

        private BucketComparison item;

        public Builder() {
            this.item = new BucketComparison();
        }

        public Builder withOtherLabel(Bucket.Label value) {
            this.item.otherLabel = value;
            return this;
        }

        public Builder withJointActionComparison(ComparisonStatistics value) {
            this.item.jointActionComparison = value;
            return this;
        }

        public Builder withActionComparisons(Map<Event.Name, ActionComparisonStatistics> value) {
            this.item.actionComparisons = value;
            return this;
        }

        public BucketComparison build() {
            return this.item;
        }
    }
}
