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
import org.junit.Test;

import static io.codearte.catchexception.shade.mockito.Mockito.mock;
import static io.codearte.catchexception.shade.mockito.Mockito.when;
import static junit.framework.TestCase.assertEquals;

public class NoOpEventsIngestionExecutorTest {

    @Test
    public void executeWithNullPayloadTest() {
        EventIngestionExecutor eventIngestionExecutor = new NoOpEventsIngestionExecutor();
        eventIngestionExecutor.execute(null);
    }

    @Test
    public void executeWithMockPayloadTest() {
        EventEnvelopePayload eventEnvelopePayload = mock(EventEnvelopePayload.class);
        when(eventEnvelopePayload.toJson()).thenReturn("event payload");
        EventIngestionExecutor eventIngestionExecutor = new NoOpEventsIngestionExecutor();
        eventIngestionExecutor.execute(eventEnvelopePayload);
    }

    @Test
    public void queueLengthTest() {
        EventIngestionExecutor eventIngestionExecutor = new NoOpEventsIngestionExecutor();
        assertEquals("Queue length for no op executor should always be 0.",
                0, eventIngestionExecutor.queueLength());
    }

    @Test
    public void nameTest() {
        EventIngestionExecutor eventIngestionExecutor = new NoOpEventsIngestionExecutor();
        assertEquals("Name of no op ingestion executor not as expected",
                NoOpEventsIngestionExecutor.NAME, eventIngestionExecutor.name());
    }
}
