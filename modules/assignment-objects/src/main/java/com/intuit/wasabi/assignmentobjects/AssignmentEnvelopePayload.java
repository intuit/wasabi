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
package com.intuit.wasabi.assignmentobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.wasabi.assignmentobjects.Assignment.Status;
import com.intuit.wasabi.assignmentobjects.User.ID;
import com.intuit.wasabi.exceptions.JsonExportException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Application.Name;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.export.EnvelopePayload;
import org.apache.cassandra.utils.UUIDGen;

import javax.ws.rs.core.HttpHeaders;
import java.util.Date;
import java.util.UUID;

/**
 * Export envelope payload for assignments.
 * <p>
 * Warning: This class does not provide proper deserialization again, as some information
 * is NOT stored inside the JSON Strings, as such serializing and deserializing results
 * in data loss. Therefore it can (and should) only be used for exports!
 */
public class AssignmentEnvelopePayload implements EnvelopePayload {

    private static final String VERSION = "3.0";
    private static final String MESSAGE_TYPE = "ASSIGNMENT";
    private User.ID userID;
    private Context context;
    private boolean createAssignment;
    private boolean putAssignment;
    private boolean ignoreSamplingPercent;
    private SegmentationProfile segmentationProfile;
    private Assignment.Status assignmentStatus;
    private Bucket.Label bucketLabel;
    private Page.Name pageName;
    private Application.Name applicationName;
    private Experiment.Label experimentLabel;
    private Experiment.ID experimentID;
    private Date date;
    private UUID timeUUID;

    /**
     * @param userID                the user ID
     * @param context               the context
     * @param createAssignment      the flag create if not exists assignment
     * @param putAssignment         the flat to put assignment
     * @param ignoreSamplingPercent the flag to ignore sampling percentage
     * @param segmentationProfile   the segmentation profile {@link SegmentationProfile}
     * @param assignmentStatus      the assignment's status {@link Status}
     * @param bucketLabel           the bucket's label
     * @param pageName              the page name
     * @param applicationName       the application name
     * @param experimentLabel       the experiment label
     * @param experimentID          the experiment ID
     * @param date                  the date
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
            Date date,
            HttpHeaders ignored) {
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
        this.date = date;
        this.timeUUID = UUIDGen.getTimeUUID();
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
    public String getContext() {
        return context.getContext();
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
    public String getSegmentationProfile() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper
                .writer()
                .writeValueAsString(segmentationProfile.getProfile());
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
     * @return the date
     */
    public Date getAssignmentTimestamp() {
        return date;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * @return the message type
     */
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public UUID getTimeUUID() {
        return timeUUID;
    }

    @Override
    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writer().writeValueAsString(this);
        } catch (JsonProcessingException jsonProcExc) {
            throw new JsonExportException("Can not serialize assignment for export.", jsonProcExc);
        }
    }
}
