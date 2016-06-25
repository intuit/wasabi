package com.intuit.wasabi.events;

import com.intuit.wasabi.eventobjects.EventEnvelopePayload;

public interface EventIngestionExecutor {

    /**
     * This method ingests what is contained in the {@link com.intuit.wasabi.eventobjects.EventEnvelopePayload} to real time data ingestion system.
     *
     * @param eventEnvelopePayload
     */
    void execute(EventEnvelopePayload eventEnvelopePayload);

    /**
     * Number of elements in the ingestion queue.
     *
     * @return number of elements in the queue
     */
    int queueLength();

    /**
     * Name of the ingestion executor.
     *
     * @return the name of the ingestion executor.
     */
    String name();
}
