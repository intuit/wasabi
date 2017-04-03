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

import com.google.inject.Inject;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The EventLogSystem provides a wrapper around a Thread containing the {@link EventLog} implementation.
 * It allows for easy start and stop handling.
 */
public class EventLogSystem {

    private static final Logger LOGGER = getLogger(EventLogSystem.class);
    /** The event log thread. */
    private final Thread eventLogThread;

    /**
     * Instantiates the event log thread.
     *
     * @param eventLog the event log to use
     */
    @Inject
    public EventLogSystem(EventLog eventLog) {
        eventLogThread = new Thread(eventLog);
        eventLogThread.setName("EventLogThread");
    }

    /**
     * Starts the event log thread.
     */
    public void start() {
        eventLogThread.start();
        LOGGER.info("Started " + eventLogThread.getName() + " with ID " + eventLogThread.getId() + ".");
    }

    /**
     * Stops the event log thread via an interrupt and waits up to 5000 milliseconds for its join.
     */
    public void stop() {
        eventLogThread.interrupt();
        LOGGER.info("Interrupted " + eventLogThread.getName() + " with ID " + eventLogThread.getId() + ".");
        try {
            eventLogThread.join(5000);
            LOGGER.info(eventLogThread.getName() + " with ID " + eventLogThread.getId() + " joined.");
        } catch (InterruptedException e) {
            LOGGER.warn("Was interrupted while joining " + eventLogThread.getName() + ".", e);
        }
    }
}
