/*******************************************************************************
 * Copyright 2017 Intuit
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

import com.intuit.wasabi.eventobjects.EventEnvelopePayload;
import com.intuit.wasabi.events.EventIngestionExecutor;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class NoOpEventsIngestionExecutor implements EventIngestionExecutor {

    private static final Logger logger = getLogger(EventsEnvelope.class);

    public static final String NAME = "NO-OP-EVENTS-INGESTION-EXECUTOR";

    /**
     * This method ingests what is contained in the {@link EventEnvelopePayload} to real time data ingestion system.
     *
     * @param eventEnvelopePayload
     */
    @Override
    public void execute(EventEnvelopePayload eventEnvelopePayload) {
        logger.debug("Received event envelope: {}", eventEnvelopePayload != null ? eventEnvelopePayload.toJson() : null);
    }

    /**
     * Number of elements in the ingestion queue.
     *
     * @return number of elements in the queue
     */
    @Override
    public int queueLength() {
        return 0;
    }

    /**
     * Name of the ingestion executor.
     *
     * @return the name of the ingestion executor.
     */
    @Override
    public String name() {
        return NAME;
    }
}
