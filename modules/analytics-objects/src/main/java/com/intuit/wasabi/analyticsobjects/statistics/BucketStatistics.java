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
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * DTO to save the statistics for a bucket
 *
 * Fields: 
 * <ul>
 * <li>Name of the bucket</li>
 * <li>List of {@link BucketComparison}s with all other buckets</li>
 * <li>List of all {@link ActionCounts} for the actions in the bucket</li>
 * <li>List of all {@link ActionRate}s for the actions in the bucket</li>
 * <li>{@link Counts} object for the impressions of this bucket</li>
 * <li>{@link Counts} object for the joint action counts</li>
 * <li>{@link Estimate} object for the joint Action rate</li>
 * </ul>
 */
public class BucketStatistics extends AbstractContainerStatistics {

    private Bucket.Label label;
    private Map<Bucket.Label, BucketComparison> bucketComparisons;

    public static class Builder {

        private BucketStatistics item;

        public Builder() {
            this.item = new BucketStatistics();
        }

        public Builder withLabel(Bucket.Label value) {
            this.item.label = value;
            return this;
        }

        public Builder withBucketComparisons(Map<Bucket.Label, BucketComparison> value) {
            this.item.bucketComparisons = value;
            return this;
        }

        public Builder withActionCounts(Map<Event.Name, ActionCounts> value) {
            this.item.actionCounts = value;
            return this;
        }

        public Builder withActionRates(Map<Event.Name, ActionRate> value) {
            this.item.actionRates = value;
            return this;
        }

        public Builder withImpressionCounts(Counts value) {
            this.item.impressionCounts = value;
            return this;
        }

        public Builder withJointActionCounts(Counts value) {
            this.item.jointActionCounts = value;
            return this;
        }

        public Builder withJointActionRate(Estimate value) {
            this.item.jointActionRate = value;
            return this;
        }

        public Builder withBucketCounts(BucketCounts value) {
            this.item.label = value.getLabel();
            this.item.impressionCounts = value.getImpressionCounts();
            this.item.jointActionCounts = value.getJointActionCounts();
            this.item.actionCounts = value.getActionCounts();
            return this;
        }

        public BucketStatistics build() {
            return this.item;
        }
    }

    public Bucket.Label getLabel() {
        return label;
    }

    public void setLabel(Bucket.Label value) {
        this.label = value;
    }

    public Map<Bucket.Label, BucketComparison> getBucketComparisons() {
        return bucketComparisons;
    }

    public void setBucketComparisons(Map<Bucket.Label, BucketComparison> value) {
        this.bucketComparisons = value;
    }

    @JsonIgnore
    public void addToBucketComparisons(Bucket.Label bucketLabel, BucketComparison item) {
        if (this.bucketComparisons == null) {
            this.bucketComparisons = new HashMap<>();
        }
        this.bucketComparisons.put(bucketLabel, item);
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
    public BucketStatistics clone() {
        BucketStatistics cloned = (BucketStatistics) super.clone();

        if (bucketComparisons != null) {
            Map<Bucket.Label, BucketComparison> clonedComparisons = new HashMap<>();
            for (Entry<Label, BucketComparison> entry : bucketComparisons.entrySet()) {
                clonedComparisons.put(entry.getKey(), entry.getValue().clone());
            }
            cloned.setBucketComparisons(clonedComparisons);
        }

        return cloned;
    }
}
