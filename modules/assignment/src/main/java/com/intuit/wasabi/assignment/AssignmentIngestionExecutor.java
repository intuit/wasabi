package com.intuit.wasabi.assignment;

import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;

import java.util.Map;
import java.util.concurrent.Future;

public interface AssignmentIngestionExecutor {

    /**
     * This method ingests what is contained in the {@link com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload} to real time data ingestion system.
     *
     * @param assignmentEnvelopePayload
     * @return a Future representing pending completion of the task
     */
    public Future<?> execute(AssignmentEnvelopePayload assignmentEnvelopePayload);

    /**
     * Number of elements in the ingestion queue.
     *
     * @return number of elements in the queue
     */
    @Deprecated
    public int queueLength();

    /**
     * Details of ingestion queue.
     *
     * @return details of ingestion queue
     */
    public Map<String, Object> queueDetails();

    /**
     * Flush all messages from ingestion memory queue including active and the ones waiting. These messages should be persisted before flushed.
     */
    public void flushMessages();

    /**
     * Name of the ingestion executor.
     *
     * @return the name of the ingestion executor.
     */
    public String name();
}
