/*******************************************************************************
 * Copyright 2017 Intuit
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
package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.UUID;

class ExperimentBucketKey {
    private final Experiment.ID expID;
    private final Bucket.Label bucket;

    ExperimentBucketKey(Experiment.ID expID, Bucket.Label bucket) {
        this.expID = expID;
        this.bucket = bucket;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(101, 157).
                append(expID).append(bucket).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExperimentBucketKey))
            return false;
        if (obj == this)
            return true;

        ExperimentBucketKey rhs = (ExperimentBucketKey) obj;
        return new EqualsBuilder().
                        append(expID.toString(), rhs.expID.toString()).
                        append(bucket.toString(), rhs.bucket.toString()).
                        isEquals();
    }

    public UUID getExpID(){
        return UUID.fromString(expID.toString());
    }

    public String getBucketLabel(){
        return bucket.toString();
    }
}
