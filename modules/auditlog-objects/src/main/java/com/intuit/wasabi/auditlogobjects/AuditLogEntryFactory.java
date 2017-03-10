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

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.events.ApplicationEvent;
import com.intuit.wasabi.eventlog.events.BucketCreateEvent;
import com.intuit.wasabi.eventlog.events.BucketEvent;
import com.intuit.wasabi.eventlog.events.ChangeEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.ExperimentEvent;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.Calendar;

/**
 * Provides methods to create AuditLogEntries from EventLogEvents.
 */
public class AuditLogEntryFactory {


    AuditLogEntryFactory() {
    }

    /**
     * Decides which kind of event is supplied and calls the corresponding factory method.
     *
     * @param event the event
     * @return the corresponding {@link AuditLogEntry}.
     */
    public static AuditLogEntry createFromEvent(EventLogEvent event) {
        // time and user are always there
        Calendar time = event.getTime();
        UserInfo user = event.getUser();

        Application.Name appName = null;
        if (event instanceof ApplicationEvent) {
            appName = ((ApplicationEvent) event).getApplicationName();
        }

        Experiment.Label expLabel = null;
        Experiment.ID expId = null;
        if (event instanceof ExperimentEvent) {
            expLabel = ((ExperimentEvent) event).getExperiment().getLabel();
            expId = ((ExperimentEvent) event).getExperiment().getID();
        }

        Bucket.Label bucketLabel = null;
        if (event instanceof BucketEvent) {
            bucketLabel = ((BucketEvent) event).getBucket().getLabel();
        }

        // change events allow for property changes
        String property = null;
        String before = null;
        String after = null;
        if (event instanceof ChangeEvent) {
            property = ((ChangeEvent) event).getPropertyName();
            before = ((ChangeEvent) event).getBefore();
            after = ((ChangeEvent) event).getAfter();
        } else if (event instanceof BucketCreateEvent) {
            property = "allocation";
            after = String.valueOf(((BucketCreateEvent) event).getBucket().getAllocationPercent());
        }

        return new AuditLogEntry(time, user, AuditLogAction.getActionForEvent(event), appName, expLabel, expId, bucketLabel, property, before, after);
    }

}
