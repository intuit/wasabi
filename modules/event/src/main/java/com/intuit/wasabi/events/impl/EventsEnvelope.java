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
package com.intuit.wasabi.events.impl;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

class EventsEnvelope implements Runnable {

    private static final Logger logger = getLogger(EventsEnvelope.class);
    private final Transaction transaction;
    private Assignment assignment;
    private Event event;

    public EventsEnvelope(final Assignment assignment, final Event event, final Transaction transaction) {
        this.assignment = assignment;
        this.event = event;
        this.transaction = transaction;
    }

    @Override
    public void run() {
        try {
            recordEvent(assignment, event);
        } catch (Exception e) {
            //TODO: Is it ok to just log the exception ??
            if (e instanceof WasabiClientException) {
                logger.warn("unable to process event, cause: {}, message: {}",
                        e.getCause(), ((WasabiClientException) e).getDetailMessage());
            }
        }
    }

    void recordEvent(Assignment assignment, Event event)
            throws Exception {
        String actionNameString = event.getName().toString();
        String payload = (event.getPayload() != null)
                ? event.getPayload().toString()
                : null;
        String userIDString = assignment.getUserID().toString();

        String context = (event.getContext() != null)
                ? event.getContext().getContext()
                : "PROD";

        Experiment.ID experimentIDValue = assignment.getExperimentID();
        Bucket.Label bucketLabelValue = assignment.getBucketLabel();

        if (bucketLabelValue != null && experimentIDValue != null) {
            // TODO: will need to update for future additions of non-binary event types
            if (event.getType().equals(Event.Type.IMPRESSION)) {
                transaction.insert("INSERT INTO event_impression " +
                                "(user_id, experiment_id, bucket_label, context, timestamp, payload) " +
                                "VALUES (?, ?, ?, ?, ?, ?)",
                        userIDString, experimentIDValue, bucketLabelValue,
                        context, event.getTimestamp(), payload);
            } else {
                transaction.insert("INSERT INTO event_action " +
                                "(user_id, experiment_id, bucket_label, action, " +
                                "context, timestamp, payload) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        userIDString, experimentIDValue,
                        bucketLabelValue, actionNameString,
                        context, event.getTimestamp(), payload);
            }
        }
    }
}
