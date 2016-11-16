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
package com.intuit.wasabi.eventobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Application.Name;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.export.EnvelopePayload;
import org.apache.cassandra.utils.UUIDGen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Export envelope payload for events
 * <p>
 * Warning: This class does not provide proper deserialization again, as some information
 * is NOT stored inside the JSON Strings, as such serializing and deserializing results
 * in data loss. Therefore it can (and should) only be used for exports!
 */
public class EventEnvelopePayload implements EnvelopePayload {
    private static final String VERSION = "3.0";
    private static final String MESSAGE_TYPE = "EVENT";
    private Name applicationName;
    private Label experimentLabel;
    private Assignment assignment;
    private Event event;
    private UUID timeUUID;

    /**
     * @param applicationName the application name
     * @param experimentLabel the experiment label
     * @param assignment      the assignment {@link Assignment}
     * @param event           the event {@link Event}
     */
    public EventEnvelopePayload(Name applicationName, Label experimentLabel,
                                Assignment assignment, Event event) {
        this.applicationName = applicationName;
        this.experimentLabel = experimentLabel;
        this.assignment = assignment;
        this.event = event;
        this.timeUUID = UUIDGen.getTimeUUID();
    }

    public Application.Name getApplicationName() {
        return applicationName;
    }

    public Experiment.Label getExperimentLabel() {
        return experimentLabel;
    }

    public String getVersion() {
        return VERSION;
    }

    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public UUID getTimeUUID() {
        return timeUUID;
    }

    public Map<String, Object> getEvent() {
        HashMap<String, Object> eventMap = new HashMap<>(4);
        eventMap.put("timestamp", event.getTimestamp());
        eventMap.put("name", event.getName());
        eventMap.put("payload", event.getPayload());
        eventMap.put("type", event.getType());
        return eventMap;
    }

    public User.ID getUserID() {
        return assignment.getUserID();
    }

    public String getValue() {
        return event.getValue();
    }

    public Experiment.ID getExperimentID() {
        return assignment.getExperimentID();
    }

    public Bucket.Label getBucketLabel() {
        return assignment.getBucketLabel();
    }

    public String getContext() {
        return assignment.getContext().getContext();
    }

    @Override
    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can not serialize event for export.", e);
        }
    }


}
