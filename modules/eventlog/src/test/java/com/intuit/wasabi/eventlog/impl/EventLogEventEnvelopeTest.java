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
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link EventLogEventEnvelope}.
 */
public class EventLogEventEnvelopeTest {

    @Test
    public void testRun() throws Exception {
        EventLogEvent event = Mockito.mock(EventLogEvent.class);
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);

        EventLogEventEnvelope envelope = new EventLogEventEnvelope(event, eventLogListener);
        Mockito.doNothing().when(eventLogListener).postEvent(event);
        envelope.run();
        Mockito.verify(eventLogListener, Mockito.times(1)).postEvent(event);
    }
}
