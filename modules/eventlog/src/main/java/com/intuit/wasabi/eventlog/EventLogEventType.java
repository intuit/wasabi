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
package com.intuit.wasabi.eventlog;

import com.intuit.wasabi.eventlog.events.AuthorizationChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketCreateEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.eventlog.events.ExperimentCreateEvent;
import com.intuit.wasabi.eventlog.events.SimpleEvent;

/**
 * Holds the Event types for specific event implementations, for easy switches.
 */
public enum EventLogEventType {
    AUTHORIZATION_CHANGED(AuthorizationChangeEvent.class),
    BUCKET_CHANGED(BucketChangeEvent.class),
    BUCKET_CREATED(BucketCreateEvent.class),
    EXPERIMENT_CHANGED(ExperimentChangeEvent.class),
    EXPERIMENT_CREATED(ExperimentCreateEvent.class),
    SIMPLE_EVENT(SimpleEvent.class),
    UNKNOWN(null); // default value

    private final Class eventClass;

    EventLogEventType(Class<?> eventClass) {
        this.eventClass = eventClass;
    }

    /**
     * Returns the EventLogEventType for an EventLogEvent.
     *
     * @param event the event to look up the EventLogEventType for.
     * @return the matching EventLogEventType or {@link EventLogEventType#UNKNOWN} if no match can be found.
     */
    public static EventLogEventType getType(EventLogEvent event) {
        if (event != null) {
            for (EventLogEventType type : values()) {
                if (event.getClass().equals(type.eventClass)) {
                    return type;
                }
            }
        }
        return UNKNOWN;
    }


}
