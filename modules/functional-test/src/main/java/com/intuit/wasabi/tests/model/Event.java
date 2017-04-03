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
package com.intuit.wasabi.tests.model;

import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

/**
 * A very simple Event wrapper.
 * By default all events are IMPRESSIONs.
 */
public class Event extends ModelItem {
    /**
     * this event's timestamp
     */
    public String timestamp;

    /**
     * The event type, by default "IMPRESSION", but can be different to represent an Action.
     */
    public String name = "IMPRESSION";

    /**
     * this event's context
     */
    public String context;

    /**
     * The payload coming with the event.
     */
    public String payload;

    /**
     * This event's value.
     */
    public String value;

    public String userId;

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    /**
     * Creates a simple IMPRESSION.
     */
    public Event() {
    }


    /**
     * Copies an event.
     *
     * @param other the event to copy
     */
    public Event(Event other) {
        update(other);
    }

    /**
     * Creates a specific Event.
     *
     * @param name event type
     */
    public Event(String name) {
        this.setName(name);
    }

    /**
     * Sets this event's name, thus overwriting the default "IMPRESSION'.
     *
     * @param name the new name
     * @return this
     */
    public Event setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets this event's timestamp.
     *
     * @param timestamp the new timestamp
     * @return this
     */
    public Event setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Sets this event's context.
     *
     * @param context the new context
     * @return this
     */
    public Event setContext(String context) {
        this.context = context;
        return this;
    }

    /**
     * Sets this event's payload.
     *
     * @param payload the new payload
     * @return this
     */
    public Event setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Sets this event's value. Note that this has currently no effect as the value is not implemented.
     * TODO: once supported remove the note
     *
     * @param value the new value
     * @return this
     */
    public Event setValue(String value) {
        this.value = value;
        return this;
    }

    /**
     * Sets the SerializationStrategy which is used for JSON serialization and
     * {@link Experiment#equals(Object)}.
     *
     * @param serializationStrategy the serialization strategy
     */
    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        Event.serializationStrategy = serializationStrategy;
    }

    /**
     * Returns the current SerializationStrategy.
     *
     * @return the current SerializationStategy.
     */
    @Override
    public SerializationStrategy getSerializationStrategy() {
        return Event.serializationStrategy;
    }
}
