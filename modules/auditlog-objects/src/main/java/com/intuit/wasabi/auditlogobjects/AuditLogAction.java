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

import com.intuit.wasabi.eventlog.events.AuthorizationChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketCreateEvent;
import com.intuit.wasabi.eventlog.events.BucketDeleteEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.eventlog.events.ExperimentCreateEvent;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;

/**
 * Lists the known audit log actions.
 */
public enum AuditLogAction {
    UNSPECIFIED_ACTION(null),
    EXPERIMENT_CREATED(ExperimentCreateEvent.class),
    EXPERIMENT_CHANGED(ExperimentChangeEvent.class),
    BUCKET_CREATED(BucketCreateEvent.class),
    BUCKET_CHANGED(BucketChangeEvent.class),
    BUCKET_DELETED(BucketDeleteEvent.class),
    AUTHORIZATION_CHANGE(AuthorizationChangeEvent.class),;

    private final Class<? extends EventLogEvent> eventType;

    AuditLogAction(Class<? extends EventLogEvent> eventType) {
        this.eventType = eventType;
    }

    /**
     * Returns the action associated with the supplied {@link EventLogEvent}.
     * If no action can be found, {@link #UNSPECIFIED_ACTION} is returned.
     *
     * @param event the event for the lookup
     * @return the corresponding action or {@link #UNSPECIFIED_ACTION}
     */
    public static AuditLogAction getActionForEvent(EventLogEvent event) {
        for (AuditLogAction auditLogAction : values()) {
            if (event.getClass().equals(auditLogAction.eventType)) {
                return auditLogAction;
            }
        }
        return UNSPECIFIED_ACTION;
    }

    /**
     * Returns a concise string describing the entry which can be shown in the UI.
     *
     * @param entry the audit log entry
     * @return a simple description of the entry's action
     */
    public static String getDescription(AuditLogEntry entry) {
        if (entry != null && entry.getAction() != null) {
            switch (entry.getAction()) {
                case EXPERIMENT_CREATED:
                    return "created experiment";
                case EXPERIMENT_CHANGED:
                    return getDescriptionExperimentChanged(entry);
                case BUCKET_CREATED:
                    return "created bucket " + entry.getBucketLabel() + " with an allocation of " + getPercentString(entry.getAfter());
                case BUCKET_DELETED:
                    return "deleted bucket " + entry.getBucketLabel();
                case BUCKET_CHANGED:
                    return getDescriptionBucketChanged(entry);
                case AUTHORIZATION_CHANGE:
                    return getDescriptionAuthorizationChange(entry);
            }
        }
        return "made a modification";
    }

    /**
     * Returns a concise string for entries describing authorization (user role) changes.
     *
     * @param entry the audit log entry
     * @return a simple description of the change
     */
    /*test*/
    static String getDescriptionAuthorizationChange(AuditLogEntry entry) {
        if (StringUtils.isBlank(entry.getAfter())) {
            return "removed access from " + entry.getChangedProperty();
        } else {
            String access = entry.getAfter();
            switch (entry.getAfter()) {
                case "READONLY":
                    access = "READ";
                    break;
                case "READWRITE":
                    access = "WRITE";
                    break;
                default:
                    break;
            }

            if (StringUtils.isBlank(entry.getBefore())) {
                return "granted " + access + " access to " + entry.getChangedProperty();
            }

            String accessFrom = entry.getBefore();
            switch (entry.getBefore()) {
                case "READONLY":
                    accessFrom = "READ";
                    break;
                case "READWRITE":
                    accessFrom = "WRITE";
                    break;
                default:
                    break;
            }

            return "changed access from " + accessFrom + " to " + access + " for " + entry.getChangedProperty();
        }
    }

    /**
     * Returns a concise string for entries describing experiment changes
     *
     * @param entry the audit log entry
     * @return a simple description of the change
     */
    /*test*/
    static String getDescriptionExperimentChanged(AuditLogEntry entry) {
        switch (entry.getChangedProperty()) {
            // direct experiment properties
            case "state":
                if (entry.getAfter() != null) {
                    switch (Experiment.State.valueOf(entry.getAfter())) {
                        case DRAFT:
                            return "set experiment into draft state";
                        case RUNNING:
                            return "started experiment";
                        case PAUSED:
                            return "stopped experiment";
                        case TERMINATED:
                            return "terminated experiment";
                        case DELETED:
                            return "deleted experiment";
                        default:
                            break;
                    }
                }
                break;
            case "description":
                return "changed experiment description to \"" + entry.getAfter() + "\"";
            case "sampling_percent":
                return "changed sampling rate to " + getPercentString(entry.getAfter());
            case "start_time":
                return "changed start time to " + entry.getAfter();
            case "end_time":
                return "changed end time to " + entry.getAfter();
            case "isPersonalizationEnabled":
                return Boolean.parseBoolean(entry.getAfter()) ? "enabled personalization" : "disabled personalization";
            case "modelName":
                if (StringUtils.isBlank(entry.getAfter())) {
                    return "removed model name";
                }
                return "changed model name to " + entry.getAfter();
            case "modelVersion":
                if (StringUtils.isBlank(entry.getAfter())) {
                    return "removed model version";
                }
                return "changed model version to " + entry.getAfter();
            case "isRapidExperiment":
                return Boolean.parseBoolean(entry.getAfter()) ? "enabled rapid experimentation" : "disabled rapid experimentation";
            case "userCap":
                return "changed user cap for rapid experimentation to " + entry.getAfter();
            case "label":
                return "changed label from " + entry.getBefore() + " to " + entry.getAfter();
            case "applicationName":
                return "moved experiment to application " + entry.getApplicationName().toString();
            case "rule":
                return StringUtils.isBlank(entry.getAfter()) ? "removed segmentation rule" : "changed segmentation rule to " + entry.getAfter();

            // indirect properties
            case "pages":
                if (!StringUtils.isBlank(entry.getBefore())) {
                    return "all pages".equals(entry.getBefore()) ? "removed all pages" : "removed page " + entry.getBefore();
                } else {
                    return "set pages to: " + entry.getAfter();
                }
            case "mutex":
                if (!StringUtils.isBlank(entry.getBefore()) && StringUtils.isBlank(entry.getAfter())) {
                    return "removed mutual exclusion to " + entry.getBefore();
                } else if (!StringUtils.isBlank(entry.getAfter())) {
                    return "added mutual exclusion to " + entry.getAfter();
                } else {
                    return "modified mutual exclusions";
                }
        }
        return "changed experiment";
    }

    /**
     * Returns a concise string for entries describing bucket changes.
     *
     * @param entry the audit log entry
     * @return a simple description of the change
     */
    /*test*/
    static String getDescriptionBucketChanged(AuditLogEntry entry) {
        switch (entry.getChangedProperty()) {
            case "is_control":
                if (!StringUtils.isBlank(entry.getAfter()) && Boolean.parseBoolean(entry.getAfter())) {
                    return "assigned control group to bucket " + entry.getBucketLabel();
                } else {
                    return "removed control group from bucket " + entry.getBucketLabel();
                }
            case "allocation":
                return "changed allocation of bucket " + entry.getBucketLabel() + " to " + getPercentString(entry.getAfter());
            case "description":
                if (StringUtils.isBlank(entry.getBefore()) && !StringUtils.isBlank(entry.getAfter())) {
                    return "added description to bucket " + entry.getBucketLabel() + ": " + entry.getAfter();
                }
                if (StringUtils.isBlank(entry.getAfter())) {
                    return "removed bucket description of " + entry.getBucketLabel();
                }
                return "changed bucket description of " + entry.getBucketLabel() + " to " + entry.getAfter();
            case "payload":
                return !StringUtils.isBlank(entry.getAfter()) ? "changed payload of bucket " + entry.getBucketLabel()
                        + " to " + entry.getAfter() : "removed payload from bucket " + entry.getBucketLabel();
            case "state":
                return entry.getAfter().equals(Bucket.State.CLOSED.toString()) ? "closed bucket "
                        + entry.getBucketLabel() : "changed state of bucket " + entry.getBucketLabel() + " to " + entry.getAfter();
        }
        return "changed bucket " + entry.getBucketLabel();
    }

    /**
     * Returns a number representation of the format ###.##% for the double contained in {@code doubleString}.
     *
     * @param doubleString an input string representing a double
     * @return an output string representing a percentage (including %)
     */
    /*test*/
    static String getPercentString(String doubleString) {
        try {
            return new DecimalFormat("###.##%").format(Double.parseDouble(doubleString));
        } catch (NumberFormatException ignored) {
            return doubleString;
        }
    }

}
