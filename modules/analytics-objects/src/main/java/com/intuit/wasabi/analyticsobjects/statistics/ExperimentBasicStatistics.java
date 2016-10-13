/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.analyticsobjects.statistics;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
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
import java.util.Objects;

/**
 * Base class for experiment stats
 */
public class ExperimentBasicStatistics extends AbstractContainerStatistics {

    @ApiModelProperty(required = true)
    private Map<Bucket.Label, BucketBasicStatistics> buckets;

    public static class Builder{

        private ExperimentBasicStatistics item;


        public Builder(){
            this.item = new ExperimentBasicStatistics();
        }

        public Builder withBuckets(Map<Bucket.Label, BucketBasicStatistics> value){
            this.item.buckets = value;
            return this;
        }
        public Builder withActionCounts(Map<Event.Name, ActionCounts> value){
            this.item.actionCounts = value;
            return this;
        }
        public Builder withActionRates(Map<Event.Name, ActionRate> value){
            this.item.actionRates = value;
            return this;
        }
        public Builder withImpressionCounts(Counts value){
            this.item.impressionCounts = value;
            return this;
        }
        public Builder withJointActionCounts(Counts value){
            this.item.jointActionCounts = value;
            return this;
        }
        public Builder withJointActionRate(Estimate value){
            this.item.jointActionRate = value;
            return this;
        }
        public Builder withExperimentCounts(ExperimentCounts value){
            this.item.impressionCounts = value.getImpressionCounts();
            this.item.jointActionCounts = value.getJointActionCounts();
            this.item.actionCounts = value.getActionCounts();
            return this;
        }

        public ExperimentBasicStatistics build(){
            return this.item;
        }
    }

    public Map<Bucket.Label, BucketBasicStatistics> getBuckets() {
        return buckets;
    }
    public void setBuckets(Map<Bucket.Label, BucketBasicStatistics> value) {
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
    public ExperimentBasicStatistics clone() {
        ExperimentBasicStatistics cloned = (ExperimentBasicStatistics) super.clone();

        if (Objects.nonNull(buckets)) {
            Map<Bucket.Label, BucketBasicStatistics> clonedBuckets = new HashMap<>();
            for ( Entry<Label, BucketBasicStatistics> entry : buckets.entrySet()) {
                clonedBuckets.put(entry.getKey(), entry.getValue().clone());
            }
            cloned.setBuckets(clonedBuckets);
        }

        return cloned;
    }
}
