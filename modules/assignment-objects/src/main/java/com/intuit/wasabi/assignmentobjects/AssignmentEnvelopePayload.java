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
    @JsonProperty("messageType")
    private MessageType messageType = MessageType.ASSIGNMENT;
    @JsonProperty("version")
    private String version = "3.0";
    @JsonProperty("userID")
    private User.ID userID;
    @JsonProperty("context")
    private Context context;
    @JsonProperty("createAssignment")
    private boolean createAssignment;
    @JsonProperty("putAssignment")
    private boolean putAssignment;
    @JsonProperty("ignoreSamplingPercent")
    private boolean ignoreSamplingPercent;
    @JsonProperty("segmentationProfile")
    private SegmentationProfile segmentationProfile;
    @JsonProperty("assignmentStatus")
    private Assignment.Status assignmentStatus;
    @JsonProperty("bucketLabel")
    private Bucket.Label bucketLabel;
    @JsonProperty("pageName")
    private Page.Name pageName;
    @JsonProperty("applicationName")
    private Application.Name applicationName;
    @JsonProperty("experimentLabel")
    private Experiment.Label experimentLabel;
    @JsonProperty("experimentID")
    private Experiment.ID experimentID;
    @JsonProperty("assignmentTimestamp")
    private Date assignmentTimestamp;
    @JsonProperty("timeUUID")
    private UUID timeUUID = UUIDGen.getTimeUUID();

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
    public AssignmentEnvelopePayload(
            ID userID,
            Context context,
            boolean createAssignment,
            boolean putAssignment,
            boolean ignoreSamplingPercent,
            SegmentationProfile segmentationProfile,
            Status assignmentStatus,
            Bucket.Label bucketLabel,
            Page.Name pageName,
            Name applicationName,
            Label experimentLabel,
            Experiment.ID experimentID,
            Date assignmentTimestamp) {
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

    private AssignmentEnvelopePayload() {
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
