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
import com.intuit.wasabi.experimentobjects.Bucket;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Map;

/**
 * DTO to save the counts within a bucket
 * <p>
 * Fields:
 * <ul>
 * <li>external label of the bucket</li>
 * <li>id (not parsed into the Json, just for internal refference)</li>
 * <li>{@link Counts} object for the impressions for the bucket</li>
 * <li>{@link Counts} object for the joint action counts for the bucket</li>
 * <li>List of {@link ActionCounts} object for individual actions in the bucket</li>
 * </ul>
 */

public class BucketCounts extends AbstractContainerCounts {

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

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public BucketCounts clone() {
        return (BucketCounts) super.clone();
    }

    public static class Builder {

        private BucketCounts item;

        public Builder() {
            this.item = new BucketCounts();
        }

        public Builder withLabel(Bucket.Label value) {
            this.item.label = value;
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

        public Builder withActionCounts(Map<Event.Name, ActionCounts> value) {
            this.item.actionCounts = value;
            return this;
        }

        public Builder basedOn(BucketCounts example) {
            withImpressionCounts(example.impressionCounts);
            withJointActionCounts(example.jointActionCounts);
            withActionCounts(example.actionCounts);

            return this;
        }

        public BucketCounts build() {
            return this.item;
        }
    }
}
