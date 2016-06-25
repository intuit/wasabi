package com.intuit.wasabi.assignment;

import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;

public interface AssignmentIngestionExecutor {

    /**
     * This method ingests what is contained in the {@link com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload} to real time data ingestion system.
     *
     * @param assignmentEnvelopePayload
     */
    void execute(AssignmentEnvelopePayload assignmentEnvelopePayload);

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
