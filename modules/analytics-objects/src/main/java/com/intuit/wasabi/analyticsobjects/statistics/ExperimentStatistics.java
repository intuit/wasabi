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

/**
 * DTO to save the statistics of an experiment. <br>
 * This is the top-level object that contains all the information for the experiment including bucket comparisons, but without cumulative counts / statistics
 * <br>
 * Fields:
 * <ul>
 * <li>list of {@link BucketStatistics} </li>
 * <li>{@link Progress}  object for the experiment overall</li>
 * <li>{@link Progress}  object for the joint actions in the bucket</li>
 * <li>Map of {@link ActionProgress}  objects for the individual actions within the buckets</li>
 * <li>Map of {@link ActionRate}s  for the individual action </li>
 * <li>{@link Estimate}  object for the joint action rate</li>
 * <li>{@link Counts}  object for the impressions of the experiment</li>
 * <li>{@link Counts}  object for the joint actions of the experiment</li>
 * </ul>
 */

public class ExperimentStatistics extends AbstractContainerStatistics {

    @ApiModelProperty(value = "statistics by bucket", required = true)
    private Map<Bucket.Label, BucketStatistics> buckets;
    @ApiModelProperty(value = "progress metrics at the experiment level", required = true)
    private Progress experimentProgress;
    @ApiModelProperty(value = "progress metrics for all actions treated as one", required = true)
    private Progress jointProgress;
    @ApiModelProperty(value = "progress metrics for each action individually", required = true)
    private Map<Event.Name, ActionProgress> actionProgress;

    public Map<Bucket.Label, BucketStatistics> getBuckets() {
        return buckets;
    }

    public void setBuckets(Map<Bucket.Label, BucketStatistics> value) {
        this.buckets = value;
    }

    public Progress getExperimentProgress() {
        return experimentProgress;
    }

    public void setExperimentProgress(Progress value) {
        this.experimentProgress = value;
    }

    public Progress getJointProgress() {
        return jointProgress;
    }

    public void setJointProgress(Progress value) {
        this.jointProgress = value;
    }

    public Map<Event.Name, ActionProgress> getActionProgress() {
        return actionProgress;
    }

    public void setActionProgress(Map<Event.Name, ActionProgress> value) {
        this.actionProgress = value;
    }

    @JsonIgnore
    public void addToBucketStatistics(Bucket.Label bucketLabel, BucketStatistics item) {
        if (this.buckets == null) {
            this.buckets = new HashMap<>();
        }
        this.buckets.put(bucketLabel, item);
    }

    @JsonIgnore
    public void addToActionProgress(Event.Name actionName, ActionProgress item) {
        if (this.actionProgress == null) {
            this.actionProgress = new HashMap<>();
        }
        this.actionProgress.put(actionName, item);
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
    public ExperimentStatistics clone() {
        ExperimentStatistics cloned = (ExperimentStatistics) super.clone();

        if (jointProgress != null) {
            cloned.setJointProgress(jointProgress.clone());
        }

        if (experimentProgress != null) {
            cloned.setExperimentProgress(experimentProgress.clone());
        }

        if (buckets != null) {
            Map<Bucket.Label, BucketStatistics> clonedBuckets = new HashMap<>();
            for (Entry<Label, BucketStatistics> entry : buckets.entrySet()) {
                clonedBuckets.put(entry.getKey(), entry.getValue().clone());
            }
            cloned.setBuckets(clonedBuckets);
        }

        if (actionProgress != null) {
            Map<Event.Name, ActionProgress> clonedActions = new HashMap<>();
            for (Entry<Name, ActionProgress> entry : actionProgress.entrySet()) {
                clonedActions.put(entry.getKey(), entry.getValue().clone());
            }
            cloned.setActionProgress(clonedActions);
        }

        return cloned;
    }

    public static class Builder {

        private ExperimentStatistics item;

        public Builder() {
            this.item = new ExperimentStatistics();
        }

        public Builder withBuckets(Map<Bucket.Label, BucketStatistics> value) {
            this.item.buckets = value;
            return this;
        }

        public Builder withExperimentProgress(Progress value) {
            this.item.experimentProgress = value;
            return this;
        }

        public Builder withJointProgress(Progress value) {
            this.item.jointProgress = value;
            return this;
        }

        public Builder withActionProgress(Map<Event.Name, ActionProgress> value) {
            this.item.actionProgress = value;
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

        public Builder withExperimentCounts(ExperimentCounts value) {
            this.item.impressionCounts = value.getImpressionCounts();
            this.item.jointActionCounts = value.getJointActionCounts();
            this.item.actionCounts = value.getActionCounts();
            return this;
        }

        public ExperimentStatistics build() {
            return this.item;
        }
    }
}
