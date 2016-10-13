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
package com.intuit.wasabi.assignmentobjects;

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.Objects;

/**
 * Attempt at building response object for GET assignment API to hang swagger annotations on.
 * Doesn't work because this can't handle the case where the assignment field is not present.
 * Leaving code in for now in case there's a solution -- if not, please remove this class.
 */
public class Assignment {

    @ApiModelProperty(value = "if the assignment can be cached", dataType = "String", required = true)
    private User.ID userID;
    @ApiModelProperty(value = "if the assignment can be cached", dataType = "UUID", required = true)
    private Experiment.ID experimentID;
    @ApiModelProperty(value = "date the assignment was made", required = true)
    private Date created;
    @ApiModelProperty(value = "Name of the application to which the experiment belongs", dataType = "String")
    private Application.Name applicationName;
    @ApiModelProperty(value = "bucket label or null if the user is not in the experiment",
            notes = "not present if no assignment can be returned", dataType = "String")
    private Bucket.Label bucketLabel;
    @ApiModelProperty(value = "context for the experiment, eg \"PROD\", \"QA\"", dataType = "String")
    private Context context;
    @ApiModelProperty(value = "details about the assignment or reason why no assignment can be returned",
            required = true)
    private Status status;
    @ApiModelProperty(value = "if the assignment can be cached", required = true)
    private Boolean cacheable;

    @ApiModelProperty(value = "if the bucket was empty resulting in null assignment", required = false)
    private boolean bucketEmpty = false;

    protected Assignment() {
        super();
    }

    public static Builder newInstance(Experiment.ID experimentID) {
        return new Builder(experimentID);
    }

    public static Builder from(Assignment assignmentResponse) {
        return new Builder(assignmentResponse);
    }

    public boolean isBucketEmpty() {
        return bucketEmpty;
    }

    public void setBucketEmpty(boolean bucketEmpty) {
        this.bucketEmpty = bucketEmpty;
    }

    public User.ID getUserID() {
        return userID;
    }

    public void setUserID(User.ID userID) {
        this.userID = userID;
    }

    public Experiment.ID getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(Experiment.ID experimentID) {
        this.experimentID = experimentID;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Bucket.Label getBucketLabel() {
        return bucketLabel;
    }

    public void setBucketLabel(Bucket.Label value) {
        this.bucketLabel = value;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context value) {
        this.context = value;
    }

    public Assignment.Status getStatus() {
        return status;
    }

    public void setStatus(Assignment.Status value) {
        this.status = value;
    }

    public Boolean isCacheable() {
        return cacheable;
    }

    public void setCacheable(Boolean value) {
        this.cacheable = value;
    }

    public Application.Name getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(Application.Name applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (Objects.isNull(obj)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Assignment)) {
            return false;
        }

        Assignment other = (Assignment) obj;
        return new EqualsBuilder()
                .append(userID, other.getUserID())
                .append(bucketLabel, other.getBucketLabel())
                .append(status, other.getStatus())
                .append(context, other.getContext())
                .append(cacheable, other.isCacheable())
                .append(experimentID, other.getExperimentID())
                .append(context, other.getContext())
                .append(applicationName, other.getApplicationName())
                .append(bucketEmpty, other.isBucketEmpty())
                .isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public enum Status {
        EXPERIMENT_NOT_FOUND(false),
        EXPERIMENT_NOT_STARTED(false),
        EXPERIMENT_IN_DRAFT_STATE(false),
        EXPERIMENT_EXPIRED(true),
        EXPERIMENT_PAUSED(false),
        EXISTING_ASSIGNMENT(true),
        NEW_ASSIGNMENT(true),
        NO_OPEN_BUCKETS(true),
        NO_PROFILE_MATCH(false);

        private boolean cacheable;

        Status(boolean cacheable) {
            this.cacheable = cacheable;
        }

        public boolean isCacheable() {
            return cacheable;
        }
    }

    public static class Builder {

        private Assignment instance;

        private Builder(Experiment.ID experimentID) {
            instance = new Assignment();
            instance.experimentID = experimentID;
        }

        private Builder(Assignment other) {
            this(other.experimentID);
            instance.userID = other.userID;
            instance.created = other.created;
            instance.bucketLabel = other.bucketLabel;
            instance.status = other.status;
            instance.cacheable = other.cacheable;
            instance.context = other.context;
            instance.applicationName = other.applicationName;
            instance.bucketEmpty = other.bucketEmpty;
        }

        public Builder withUserID(final User.ID userID) {
            instance.userID = userID;
            return this;
        }

        public Builder withCreated(final Date created) {
            instance.created = created;
            return this;
        }

        public Builder withBucketLabel(final Bucket.Label bucketLabel) {
            instance.bucketLabel = bucketLabel;
            return this;
        }

        public Builder withStatus(final Assignment.Status status) {
            instance.status = status;
            return this;
        }

        public Builder withCacheable(final Boolean cacheable) {
            instance.cacheable = cacheable;
            return this;
        }

        public Builder withContext(final Context context) {
            instance.context = context;
            return this;
        }

        public Builder withApplicationName(final Application.Name applicationName) {
            instance.applicationName = applicationName;
            return this;
        }

        public Builder withBucketEmpty(boolean bucketEmpty) {
            instance.bucketEmpty = bucketEmpty;
            return this;
        }

        public Assignment build() {
            Assignment result = instance;
            instance = null;
            return result;
        }
    }
}
