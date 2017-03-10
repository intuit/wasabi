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
package com.intuit.wasabi.eventlog.impl;

import com.intuit.wasabi.eventlog.EventLogListener;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Wraps a {@link Runnable} around an {@link EventLogEvent} and an {@link EventLogListener} (the recipient) who is
 * subscribed to this event. When {@link EventLogEventEnvelope#run()}, the event is posted to the recipient.
 */
/*pkg*/ class EventLogEventEnvelope implements Runnable {

    /** The recipient of the event. */
    private final EventLogListener recipient;
    /** The event. */
    private final EventLogEvent event;
    private Logger LOGGER = getLogger(EventLogEventEnvelope.class);

    /**
     * Creates an envelope containing an event for the recipient.
     *
     * @param event the event
     * @param recipient the recipient
     */
    public EventLogEventEnvelope(final EventLogEvent event, final EventLogListener recipient) {
        this.event = event;
        this.recipient = recipient;
    }

    /**
     * Posts the event to the recipient. <br />
     * <br />
     *
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug("posting event: {}", event);

        recipient.postEvent(event);

        LOGGER.debug("posted event: {}", event);
    }
}