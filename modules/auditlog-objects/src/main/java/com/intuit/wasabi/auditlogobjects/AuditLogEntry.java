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
package com.intuit.wasabi.auditlogobjects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBase;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Represents an AuditLogEntry.
 */
@JsonSerialize(using = AuditLogEntry.Serializer.class)
public class AuditLogEntry {

    private final Calendar time;
    private final UserInfo user;
    private final AuditLogAction action;

    private final Application.Name applicationName;
    private final Experiment.Label experimentLabel;
    private final Experiment.ID experimentId;
    private final Bucket.Label bucketLabel;

    private final String changedProperty;
    private final String before;
    private final String after;

    /**
     * Creates an AuditLogEntry.
     *
     * @param time the event time (required)
     * @param user the event user (required)
     * @param action the event action (required)
     */
    public AuditLogEntry(Calendar time, UserInfo user, AuditLogAction action) {
        this(time, user, action, null, null, null, null, null);
    }

    /**
     * Creates an AuditLogEntry.
     *
     * @param time the event time (required)
     * @param user the event user (required)
     * @param action the event action (required)
     * @param experiment the experiment (if applicable)
     * @param bucketLabel the bucket label (if applicable)
     * @param changedProperty the changed property (if applicable)
     * @param before the property before (if applicable)
     * @param after the property after (if applicable)
     */
    public AuditLogEntry(Calendar time, UserInfo user, AuditLogAction action,
                         ExperimentBase experiment,
                         Bucket.Label bucketLabel,
                         String changedProperty, String before, String after) {
        this(time, user, action,
                experiment == null ? null : experiment.getApplicationName(),
                experiment == null ? null : experiment.getLabel(),
                experiment == null ? null : experiment.getID(),
                bucketLabel, changedProperty, before, after);
    }

    /**
     * Creates an AuditLogEntry. Usually you want to use the other constructors as they have a better parameter order.
     *
     * @see #AuditLogEntry(Calendar, UserInfo, AuditLogAction)
     * @see #AuditLogEntry(Calendar, UserInfo, AuditLogAction, ExperimentBase, Bucket.Label, String, String, String)
     *
     *
     * @param time the event time (required)
     * @param user the event invoking user (required)
     * @param action the event action (required)
     * @param applicationName application name (if applicable)
     * @param experimentLabel the experiment label (if applicable)
     * @param experimentId the experiment ID(if applicable)
     * @param bucketLabel the bucket label (if applicable)
     * @param changedProperty the changed property (if applicable)
     * @param before the property before (if applicable)
     * @param after the property after (if applicable)
     */
    public AuditLogEntry(Calendar time, UserInfo user,
                         AuditLogAction action, Application.Name applicationName,
                         Experiment.Label experimentLabel, Experiment.ID experimentId,
                         Bucket.Label bucketLabel,
                         String changedProperty, String before, String after) {
        if (time == null) {
            throw new IllegalArgumentException("time must not be null");
        }

        if (user == null) {
            throw new IllegalArgumentException("username must not be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        this.time = time;
        this.user = user;
        this.action = action;
        this.applicationName = applicationName;
        this.experimentLabel = experimentLabel;
        this.experimentId = experimentId;
        this.bucketLabel = bucketLabel;
        this.changedProperty = changedProperty;
        this.before = before;
        this.after = after;
    }

    /**
     * Returns a reconstructed user object. Does not contain the UserId.
     *
     * @return the user of this entry (no Id)
     */
    public UserInfo getUser() {
        return user;
    }

    /**
     * The time of this entry.
     *
     * @return the time
     */
    public Calendar getTime() {
        return time;
    }

    /**
     * Returns an ISO 8601 timestamp with zulu time of this entry's time.
     *
     * @return an ISO 8601 timestamp
     * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">wikipedia.org/wiki/ISO_8601</a>
     */
    public String getTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(time.getTime()).replace("+0000", "Z");
    }

    /**
     * The action associated with this entry.
     *
     * @return an action.
     */
    public AuditLogAction getAction() {
        return action;
    }

    /**
     * The application of this entry.
     *
     * @return the application.
     */
    public Application.Name getApplicationName() {
        return applicationName;
    }

    /**
     * The experiment label.
     *
     * @return the experiment label.
     */
    public Experiment.Label getExperimentLabel() {
        return experimentLabel;
    }

    /**
     * The experiment ID.
     *
     * @return the experiment ID.
     */
    public Experiment.ID getExperimentId() {
        return experimentId;
    }

    /**
     * The bucket label.
     *
     * @return the bucket label.
     */
    public Bucket.Label getBucketLabel() {
        return bucketLabel;
    }

    /**
     * The name of the changed property.
     *
     * @return the changed property's name.
     */
    public String getChangedProperty() {
        return changedProperty;
    }

    /**
     * The property value before the change.
     *
     * @return the last value
     */
    public String getBefore() {
        return before;
    }

    /**
     * The property value after the change.
     *
     * @return the value after
     */
    public String getAfter() {
        return after;
    }

    /**
     * Returns a String representation of this entry.
     *
     * @return a string representation.
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    /**
     * A serializer for the AuditLogEntry for Jackson.
     */
    public static class Serializer extends JsonSerializer<AuditLogEntry> {

        /**
         * Method that can be called to ask implementation to serialize
         * values of type this serializer handles.
         *
         * @param value    Value to serialize; can <b>not</b> be null.
         * @param jgen     Generator used to output resulting Json content
         * @param provider Provider that can be used to get serializers for
         */
        @Override
        public void serialize(AuditLogEntry value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();

            // time
            jgen.writeObjectField("time", value.getTime());

            // user
            jgen.writeObjectField("user", value.getUser());

            // action
            jgen.writeFieldName("action");
            jgen.writeStartObject();
            jgen.writeStringField("type", value.getAction().toString());
            jgen.writeStringField("message", AuditLogAction.getDescription(value));
            jgen.writeEndObject();

            // application
            if (value.getApplicationName() != null) {
                jgen.writeObjectField("applicationName", value.getApplicationName());
            }

            // experiment
            if (value.getExperimentLabel() != null || value.getExperimentId() != null) {
                jgen.writeFieldName("experiment");
                jgen.writeStartObject();
                if (value.getExperimentLabel() != null) {
                    jgen.writeObjectField("experimentLabel", value.getExperimentLabel());
                }
                if (value.getExperimentId() != null) {
                    jgen.writeObjectField("experimentId", value.getExperimentId());
                }
                jgen.writeEndObject();
            }

            // bucket
            if (value.getBucketLabel() != null) {
                jgen.writeObjectField("bucketLabel", value.getBucketLabel());
            }

            // changed property
            if (value.getChangedProperty() != null) {
                jgen.writeFieldName("change");
                jgen.writeStartObject();
                jgen.writeStringField("changedAttribute", value.getChangedProperty());
                if (value.getBefore() != null) {
                    jgen.writeStringField("before", value.getBefore());
                }
                if (value.getAfter() != null) {
                    jgen.writeStringField("after", value.getAfter());
                }
                jgen.writeEndObject();
            }

            jgen.writeEndObject();
        }
    }
}
