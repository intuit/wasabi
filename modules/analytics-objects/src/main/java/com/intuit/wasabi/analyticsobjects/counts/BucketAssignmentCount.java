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

import com.intuit.wasabi.experimentobjects.Bucket;
import io.swagger.annotations.ApiModelProperty;

/**
 * Bucket assignment count holder
 */
public class BucketAssignmentCount {

    @ApiModelProperty(value = "identifier for the bucket", dataType = "String")
    protected Bucket.Label bucket;

    @ApiModelProperty(value = "count of assignments for the bucket")
    protected long count;

    protected BucketAssignmentCount() {
        super();
    }

    public Bucket.Label getBucket() {
        return bucket;
    }

    public long getCount() {
        return count;
    }

    public static class Builder {
        private BucketAssignmentCount instance;

        public Builder() {
            super();
            instance = new BucketAssignmentCount();
        }

        public Builder(BucketAssignmentCount other) {
            this();
            instance.bucket = other.bucket;
            instance.count = other.count;
        }

        public Builder withBucket(Bucket.Label bucket) {
            this.instance.bucket = bucket;
            return this;
        }

        public Builder withCount(long count) {
            this.instance.count = count;
            return this;
        }

        public BucketAssignmentCount build() {
            BucketAssignmentCount result = instance;
            instance = null;
            return result;
        }
    }

}
