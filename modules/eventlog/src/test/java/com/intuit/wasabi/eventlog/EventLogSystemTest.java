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

import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link EventLogSystem}.
 */

@Ignore("failing externally")
public class EventLogSystemTest {

    @Test
    public void testSystem() throws Exception {
        EventLog eventLog = mock(EventLog.class);
        EventLogSystem eventLogSystem = new EventLogSystem(eventLog);
        Field eventLogThreadField = eventLogSystem.getClass().getDeclaredField("eventLogThread");
        eventLogThreadField.setAccessible(true);
        Thread eventThread = (Thread) eventLogThreadField.get(eventLogSystem);

        eventLogSystem.start();

        assertEquals("EventLogThread", eventThread.getName());
        assertTrue("Thread not started.", eventThread.isAlive());

        eventLogSystem.stop();

        assertFalse("Thread not finished.", eventThread.isAlive());
    }
}
