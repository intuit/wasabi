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

import org.junit.Test;
import org.mockito.Mock;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EventLogServiceTest {

    @Mock
    private EventLog eventLog;

    @Test
    public void instantiatable() throws Exception {
        new EventLogService(eventLog);
    }

    @Test
    public void testStartup() throws Exception {
        EventLogService eventLogService = new EventLogService(eventLog);
        eventLogService.startUp();
        Field eventLogSystemField = eventLogService.getClass().getDeclaredField("eventLogSystem");
        eventLogSystemField.setAccessible(true);
        EventLogSystem eventLogSystem = (EventLogSystem) eventLogSystemField.get(eventLogService);

        assertNotNull(eventLogSystem);
        Field eventLogThreadField = eventLogSystem.getClass().getDeclaredField("eventLogThread");
        eventLogThreadField.setAccessible(true);
        Thread eventThread = (Thread) eventLogThreadField.get(eventLogSystem);
        assertNotNull(eventThread);

        assertEquals("EventLogThread", eventThread.getName());

        eventLogService.startUp();
    }

    @Test
    public void testShutdown() throws Exception {
        EventLogService eventLogService = new EventLogService(eventLog);
        eventLogService.shutDown();
        Field eventLogSystemField = eventLogService.getClass().getDeclaredField("eventLogSystem");
        eventLogSystemField.setAccessible(true);
        EventLogSystem eventLogSystem = (EventLogSystem) eventLogSystemField.get(eventLogService);
        assertNull(eventLogSystem);

        eventLogService.startUp();
        eventLogService.shutDown();
        eventLogSystemField = eventLogService.getClass().getDeclaredField("eventLogSystem");
        eventLogSystemField.setAccessible(true);
        eventLogSystem = (EventLogSystem) eventLogSystemField.get(eventLogService);
        assertNull(eventLogSystem);


    }

}
