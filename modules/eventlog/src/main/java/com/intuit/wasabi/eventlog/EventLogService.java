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

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class EventLogService extends AbstractIdleService {

    private static final Logger LOGGER = getLogger(EventLogService.class);
    private final EventLog eventLog;
    private EventLogSystem eventLogSystem;

    @Inject
    public EventLogService(final EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    protected void startUp() throws Exception {
        if (eventLogSystem != null) {
            LOGGER.info("already started {}", serviceName());

            return;
        }

        eventLogSystem = new EventLogSystem(eventLog);

        eventLogSystem.start();
    }

    @Override
    protected void shutDown() throws Exception {
        if (eventLogSystem == null) {
            LOGGER.info("already stopped {}", serviceName());

            return;
        }

        eventLogSystem.stop();
        eventLogSystem = null;
    }
}
