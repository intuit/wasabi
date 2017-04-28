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
import org.json.simple.JSONObject;

import javax.ws.rs.core.HttpHeaders;
import java.util.Date;

/**
 * Export envelope payload for assignments
 */
public class AssignmentEnvelopePayload implements EnvelopePayload {

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
    private HttpHeaders httpHeaders;


    public AssignmentEnvelopePayload() {
        super();
    }

    public AssignmentEnvelopePayload(User.ID userID,
                                     Context context, SegmentationProfile segmentationProfile,
                                     Bucket.Label bucketLabel, Page.Name pageName, HttpHeaders httpHeaders,
                                     Application.Name applicationName, Experiment.Label experimentLabel) {
        super();
        this.userID = userID;
        this.context = context;
        this.segmentationProfile = segmentationProfile;
        this.bucketLabel = bucketLabel;
        this.pageName = pageName;
        this.httpHeaders = httpHeaders;
        this.applicationName = applicationName;
        this.experimentLabel = experimentLabel;
    }

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
            HttpHeaders httpHeaders) {
        super();
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
        this.httpHeaders = httpHeaders;
    }


    /**
     * @return the userID
     */
    public User.ID getUserID() {
        return userID;
    }

    /**
     * @param userID the userID to set
     */
    public void setUserID(User.ID userID) {
        this.userID = userID;
    }

    /**
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @param context the context to set
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * @return the createAssignment
     */
    public boolean isCreateAssignment() {
        return createAssignment;
    }

    /**
     * @param createAssignment the createAssignment to set
     */
    public void setCreateAssignment(boolean createAssignment) {
        this.createAssignment = createAssignment;
    }

    /**
     * @return the putAssignment
     */
    public boolean isPutAssignment() {
        return putAssignment;
    }

    /**
     * @param putAssignment the putAssignment to set
     */
    public void setPutAssignment(boolean putAssignment) {
        this.putAssignment = putAssignment;
    }

    /**
     * @return the ignoreSamplingPercent
     */
    public boolean isIgnoreSamplingPercent() {
        return ignoreSamplingPercent;
    }

    /**
     * @param ignoreSamplingPercent the ignoreSamplingPercent to set
     */
    public void setIgnoreSamplingPercent(boolean ignoreSamplingPercent) {
        this.ignoreSamplingPercent = ignoreSamplingPercent;
    }

    /**
     * @return the segmentationProfile
     */
    public SegmentationProfile getSegmentationProfile() {
        return segmentationProfile;
    }

    /**
     * @param segmentationProfile the segmentationProfile to set
     */
    public void setSegmentationProfile(SegmentationProfile segmentationProfile) {
        this.segmentationProfile = segmentationProfile;
    }

    /**
     * @return the assignmentStatus
     */
    public Assignment.Status getAssignmentStatus() {
        return assignmentStatus;
    }

    /**
     * @param assignmentStatus the assignmentStatus to set
     */
    public void setAssignmentStatus(Assignment.Status assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    /**
     * @return the bucketLabel
     */
    public Bucket.Label getBucketLabel() {
        return bucketLabel;
    }

    /**
     * @param bucketLabel the bucketLabel to set
     */
    public void setBucketLabel(Bucket.Label bucketLabel) {
        this.bucketLabel = bucketLabel;
    }

    /**
     * @return the pageName
     */
    public Page.Name getPageName() {
        return pageName;
    }

    /**
     * @param pageName the pageName to set
     */
    public void setPageName(Page.Name pageName) {
        this.pageName = pageName;
    }

    /**
     * @return the applicationName
     */
    public Application.Name getApplicationName() {
        return applicationName;
    }

    /**
     * @param applicationName the applicationName to set
     */
    public void setApplicationName(Application.Name applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * @return the experimentLabel
     */
    public Experiment.Label getExperimentLabel() {
        return experimentLabel;
    }

    /**
     * @param experimentLabel the experimentLabel to set
     */
    public void setExperimentLabel(Experiment.Label experimentLabel) {
        this.experimentLabel = experimentLabel;
    }

    /**
     * @return the experimentID
     */
    public Experiment.ID getExperimentID() {
        return experimentID;
    }

    /**
     * @param experimentID the experimentID to set
     */
    public void setExperimentID(Experiment.ID experimentID) {
        this.experimentID = experimentID;
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }


    /**
     * @return the httpHeaders
     */
    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * @param httpHeaders the httpHeaders to set
     */
    public void setHttpHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    //TODO: the generation of json and xml is representation matter of the api, not the pojo

    @Override
    public String toJson() {
        JSONObject assignmentJson = new JSONObject();
        assignmentJson.put("userID", userID != null ? userID.toString() : "");
        assignmentJson.put("applicationName", applicationName != null ? applicationName.toString() : "");
        assignmentJson.put("experimentLabel", experimentLabel != null ? experimentLabel.toString() : "");
        assignmentJson.put("context", context != null ? context.toString() : "PROD");
        assignmentJson.put("createAssignment", createAssignment);
        assignmentJson.put("putAssignment", putAssignment);
        assignmentJson.put("ignoreSamplingPercent", ignoreSamplingPercent);
        assignmentJson.put("segmentationProfile",
                segmentationProfile != null && segmentationProfile.getProfile() != null ?
                        segmentationProfile.getProfile().toString() : "");
        assignmentJson.put("experimentID", experimentID != null ? experimentID.toString() : "");
        assignmentJson.put("pageName", pageName != null ? pageName.toString() : "");
        assignmentJson.put("assignmentStatus", assignmentStatus != null ?
                assignmentStatus.toString() : "");
        assignmentJson.put("bucketLabel", bucketLabel != null ? bucketLabel.toString() : "NULL");
        assignmentJson.put("time_uuid", UUIDGen.getTimeUUID());
        assignmentJson.put("epochTimestamp", date != null ? date.getTime() : "");
        assignmentJson.put("messageType", MessageType.ASSIGNMENT.toString());
        return assignmentJson.toString();
    }
}
