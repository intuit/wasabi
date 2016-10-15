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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.wasabi.assignmentobjects.Assignment.Status;
import com.intuit.wasabi.assignmentobjects.User.ID;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Application.Name;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.export.EnvelopePayload;
import com.intuit.wasabi.export.MessageType;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Export envelope payload for assignments
 */
public final class AssignmentEnvelopePayload implements EnvelopePayload {
    private static final Logger LOG = LoggerFactory.getLogger(AssignmentEnvelopePayload.class);
    private final MessageType messageType = MessageType.ASSIGNMENT;
    private final String version = "3.0";
    private final User.ID userID;
    private final Context context;
    private final boolean createAssignment;
    private final boolean putAssignment;
    private final boolean ignoreSamplingPercent;
    private final SegmentationProfile segmentationProfile;
    private final Assignment.Status assignmentStatus;
    private final Bucket.Label bucketLabel;
    private final Page.Name pageName;
    private final Application.Name applicationName;
    private final Experiment.Label experimentLabel;
    private final Experiment.ID experimentID;
    private final Date assignmentTimestamp;
    private final UUID timeUUID = UUIDGen.getTimeUUID();

    /**
     * @param userID                the user ID
     * @param context               the context
     * @param createAssignment      the flag create if not exists assignment
     * @param putAssignment         the flat to put assignment
     * @param ignoreSamplingPercent the flag to ignore sampling percentage
     * @param segmentationProfile   the segmentation profile
     * @param assignmentStatus      the assignment's status
     * @param bucketLabel           the bucket's label
     * @param pageName              the page name
     * @param applicationName       the application name
     * @param experimentLabel       the experiment label
     * @param experimentID          the experiment ID
     * @param assignmentTimestamp   the assignment's date
     */
    @JsonCreator
    public AssignmentEnvelopePayload(
            @JsonProperty("userID") ID userID,
            @JsonProperty("context") Context context,
            @JsonProperty("createAssignment") boolean createAssignment,
            @JsonProperty("putAssignment") boolean putAssignment,
            @JsonProperty("ignoreSamplingPercent") boolean ignoreSamplingPercent,
            @JsonProperty("segmentationProfile") SegmentationProfile segmentationProfile,
            @JsonProperty("assignmentStatus") Status assignmentStatus,
            @JsonProperty("bucketLabel") Bucket.Label bucketLabel,
            @JsonProperty("pageName") Page.Name pageName,
            @JsonProperty("applicationName") Name applicationName,
            @JsonProperty("experimentLabel") Label experimentLabel,
            @JsonProperty("experimentID") Experiment.ID experimentID,
            @JsonProperty("assignmentTimestamp") Date assignmentTimestamp) {
        this.userID = userID;
        this.context = context;
        this.createAssignment = createAssignment;
        this.putAssignment = putAssignment;
        this.ignoreSamplingPercent = ignoreSamplingPercent;
        this.segmentationProfile = segmentationProfile;
        this.assignmentStatus = assignmentStatus;
        this.bucketLabel = bucketLabel;
        this.pageName = pageName;
        this.applicationName = applicationName;
        this.experimentLabel = experimentLabel;
        this.experimentID = experimentID;
        this.assignmentTimestamp = assignmentTimestamp;
    }

    /**
     * @return the userID
     */
    public User.ID getUserID() {
        return userID;
    }

    /**
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return the createAssignment
     */
    public boolean isCreateAssignment() {
        return createAssignment;
    }

    /**
     * @return the putAssignment
     */
    public boolean isPutAssignment() {
        return putAssignment;
    }

    /**
     * @return the ignoreSamplingPercent
     */
    public boolean isIgnoreSamplingPercent() {
        return ignoreSamplingPercent;
    }

    /**
     * @return the segmentationProfile
     */
    public SegmentationProfile getSegmentationProfile() {
        return segmentationProfile;
    }

    /**
     * @return the assignmentStatus
     */
    public Assignment.Status getAssignmentStatus() {
        return assignmentStatus;
    }

    /**
     * @return the bucketLabel
     */
    public Bucket.Label getBucketLabel() {
        return bucketLabel;
    }

    /**
     * @return the pageName
     */
    public Page.Name getPageName() {
        return pageName;
    }

    /**
     * @return the applicationName
     */
    public Application.Name getApplicationName() {
        return applicationName;
    }

    /**
     * @return the experimentLabel
     */
    public Experiment.Label getExperimentLabel() {
        return experimentLabel;
    }

    /**
     * @return the experimentID
     */
    public Experiment.ID getExperimentID() {
        return experimentID;
    }

    /**
     * @return the assignmentTimestamp
     */
    public Date getAssignmentTimestamp() {
        return assignmentTimestamp;
    }

    /**
     * @return the time UUID
     */
    public UUID getTimeUUID() {
        return timeUUID;
    }

    /**
     * @return the message type
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    @Override
    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOG.warn("Can not serialize {} in AssignmentEnvelopePayload#toJson.", this);
            Map<String, Object> error = new HashMap<>(2);
            error.put("error", e.getMessage());
            if (LOG.isDebugEnabled()) {
                error.put("debug", Arrays.toString(e.getStackTrace()));
            }
            try {
                return objectMapper.writeValueAsString(error);
            } catch (JsonProcessingException e1) {
                throw new RuntimeException("Can not serialize error handling in AssignmentEnvelopePayload#toJson.", e1);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AssignmentEnvelopePayload && EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
