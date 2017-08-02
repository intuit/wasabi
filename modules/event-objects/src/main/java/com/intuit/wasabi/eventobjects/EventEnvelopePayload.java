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
package com.intuit.wasabi.eventobjects;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Application.Name;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.export.EnvelopePayload;
import com.intuit.wasabi.export.MessageType;
import org.apache.cassandra.utils.UUIDGen;
import org.json.simple.JSONObject;

import java.util.UUID;

/**
 * Export envelope payload for events
 *
 */
public class EventEnvelopePayload implements EnvelopePayload {
    private Name applicationName;
    private Label experimentLabel;
    private Assignment assignment;
    private Event event;

    /**
     * @param applicationName the application name
     * @param experimentLabel the experiment label
     * @param assignment   the assignment {@link Assignment}
     * @param event     the event {@link Event}
     */
    public EventEnvelopePayload(Name applicationName, Label experimentLabel,
                                Assignment assignment, Event event) {
        super();
        this.applicationName = applicationName;
        this.experimentLabel = experimentLabel;
        this.assignment = assignment;
        this.event = event;
    }

    public Name getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(Name applicationName) {
        this.applicationName = applicationName;
    }

    public Label getExperimentLabel() {
        return experimentLabel;
    }

    public void setExperimentLabel(Label experimentLabel) {
        this.experimentLabel = experimentLabel;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @Override
    public String toJson() {
        JSONObject eventJson = new JSONObject();

        /*
        * Please take care while inserting values in this map. The JSON simple library explicitly requires values
        * to be of String type to handle strings for the JSON format such as adding quotes around string values and
        * escaping any characters inside the value. If you insert a custom type in the map, it is the responsibility
        * of the custom type's toString() implementation to ensure the formatting in accordance with the JSON format.
        */
        eventJson.put("messageType", MessageType.EVENT.toString());
        eventJson.put("applicationName", applicationName != null ? applicationName.toString(): null);
        eventJson.put("experimentLabel", experimentLabel != null ? experimentLabel.toString(): null);
        eventJson.put("userID",
                assignment != null && assignment.getUserID() != null ? assignment.getUserID().toString(): null);
        eventJson.put("bucketLabel",
                assignment != null && assignment.getBucketLabel() != null ?
                        assignment.getBucketLabel().toString(): null);
        eventJson.put("time_uuid", makeUUID().toString());
        eventJson.put("experimentID",
                assignment != null && assignment.getExperimentID() != null ?
                        assignment.getExperimentID().toString(): null);
        eventJson.put("context",
                assignment != null && assignment.getContext() != null ? assignment.getContext().toString(): null);
        eventJson.put("epochTimestamp",
                event != null && event.getTimestamp() != null ? event.getTimestamp().getTime(): null);
        eventJson.put("eventType", event != null ? event.getType() + "": "null");
        eventJson.put("eventName", event != null ? event.getName() + "": "null");
        eventJson.put("eventPayload", event != null && event.getPayload() != null ? event.getPayload().toString(): null);
        eventJson.put("value", event != null ? event.getValue(): null);

        return eventJson.toString();
    }

    /**
     * Helper method for creating uuid
     * @return UUID
     */
    protected UUID makeUUID() {
        return UUIDGen.getTimeUUID();
    }
}
