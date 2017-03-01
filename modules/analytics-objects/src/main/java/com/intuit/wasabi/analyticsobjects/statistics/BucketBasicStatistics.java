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
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Map;

/**
 * Base class for bucket statistics
 */
public class BucketBasicStatistics extends AbstractContainerStatistics {

    private Bucket.Label label;

    public Bucket.Label getLabel() {
        return label;
    }

    public void setLabel(Bucket.Label value) {
        this.label = value;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /**
     * @param obj bucket
     * @return boolean true if the Object obj is equal to this BucketBasicStatistics, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public BucketBasicStatistics clone() {
        return (BucketBasicStatistics) super.clone();
    }

    public static class Builder {

        private BucketBasicStatistics item;

        public Builder() {
            this.item = new BucketBasicStatistics();
        }

        public Builder withLabel(Bucket.Label value) {
            this.item.label = value;
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

        public BucketBasicStatistics build() {
            return this.item;
        }
    }
}
