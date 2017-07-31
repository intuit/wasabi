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
