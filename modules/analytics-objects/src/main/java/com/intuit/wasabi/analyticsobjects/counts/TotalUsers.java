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

import io.swagger.annotations.ApiModelProperty;

/**
 * Count holder for assignments.
 */
public class TotalUsers {

    @ApiModelProperty(value = "Reflects the total number of users who have tried for an assignment")
    protected long total;

    @ApiModelProperty(value = "Reflects the total number of users who been assigned to a bucket")
    protected long bucketAssignments;

    @ApiModelProperty(value = "Reflects the total number of users who have been delivered null assignments")
    protected long nullAssignments;

    protected TotalUsers() {
        super();
    }

    public long getTotal() {
        return total;
    }

    public long getBucketAssignments() {
        return bucketAssignments;
    }

    public long getNullAssignments() {
        return nullAssignments;
    }

    public static class Builder {
        private TotalUsers instance;

        public Builder() {
            super();
            instance = new TotalUsers();
        }

        public Builder(TotalUsers other) {
            this();
            instance.total = other.total;
            instance.bucketAssignments = other.bucketAssignments;
            instance.nullAssignments = other.nullAssignments;
        }

        public Builder withTotal(long total) {
            this.instance.total = total;
            return this;
        }

        public Builder withBucketAssignments(long bucketAssignments) {
            this.instance.bucketAssignments = bucketAssignments;
            return this;
        }

        public Builder withNullAssignments(long nullAssignments) {
            this.instance.nullAssignments = nullAssignments;
            return this;
        }

        public TotalUsers build() {
            TotalUsers result = instance;
            instance = null;
            return result;
        }

    }

}
