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
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Top level DTO to save the counts for an Experiment
 * <p>
 * Fields:
 * <ul>
 * <li>{@link Counts} object for the impressions of the experiment</li>
 * <li>{@link Counts} object for the joint action counts of the experiment</li>
 * <li>Map of {@link ActionCounts} for the individual actions in the buckets</li>
 * <li>Map of {@link BucketCounts} for the individual buckets in the experiment</li>
 * </ul>
 */

public class ExperimentCounts extends AbstractContainerCounts {

    @ApiModelProperty(value = "counts by bucket", required = true)
    protected Map<Bucket.Label, BucketCounts> buckets;

    @JsonIgnore
    public void addBucketCounts(Bucket.Label bucketLabel, BucketCounts bucketCounts) {
        if (this.buckets == null) {
            this.buckets = new HashMap<>();
        }

        this.buckets.put(bucketLabel, bucketCounts);
    }

    public Map<Bucket.Label, BucketCounts> getBuckets() {
        return buckets;
    }

    public void setBuckets(Map<Bucket.Label, BucketCounts> value) {
        this.buckets = value;
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
    public ExperimentCounts clone() {
        ExperimentCounts cloned = (ExperimentCounts) super.clone();

        if (buckets != null) {
            Map<Bucket.Label, BucketCounts> clonedBuckets = new HashMap<>();

            for (Entry<Label, BucketCounts> entry : buckets.entrySet()) {
                clonedBuckets.put(entry.getKey(), entry.getValue().clone());
            }
            cloned.setBuckets(clonedBuckets);
        }

        return cloned;
    }

    public static class Builder {

        private ExperimentCounts item;

        public Builder() {
            this.item = new ExperimentCounts();
        }

        public Builder withImpressionCounts(Counts value) {
            this.item.impressionCounts = value;
            return this;
        }

        public Builder withJointActionCounts(Counts value) {
            this.item.jointActionCounts = value;
            return this;
        }

        public Builder withActionCounts(Map<Event.Name, ActionCounts> value) {
            this.item.actionCounts = value;
            return this;
        }

        public Builder withBuckets(Map<Bucket.Label, BucketCounts> value) {
            this.item.buckets = value;
            return this;
        }

        public ExperimentCounts build() {
            return this.item;
        }
    }
}
