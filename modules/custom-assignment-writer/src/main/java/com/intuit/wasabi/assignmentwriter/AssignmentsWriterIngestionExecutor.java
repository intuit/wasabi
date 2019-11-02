package com.intuit.wasabi.assignmentwriter;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A writer ingestion executor to write user assignment details to a JSON format file
 */
public final class AssignmentsWriterIngestionExecutor implements AssignmentIngestionExecutor {

    public static final String NAME = "WRITER_INGESTION";

    private static final Logger logger = LoggerFactory.getLogger(AssignmentsWriterIngestionExecutor.class);

    private final ThreadPoolExecutor threadPoolExecutor;
    private final AssignmentsFileWriter assignmentFileWriter;

    @Inject
    public AssignmentsWriterIngestionExecutor
        (@Named(AssignmentWriterAnnotations.ASSIGNMENTS_WRITER_THREAD_POOL) ThreadPoolExecutor threadPool,
         @Named(AssignmentWriterAnnotations.ASSIGNMENTS_FILE_WRITER) AssignmentsFileWriter assignmentFileWriter) {

        if ((threadPool == null) || (assignmentFileWriter == null))
            throw  new IllegalArgumentException();

        this.threadPoolExecutor = threadPool;
        this.assignmentFileWriter = assignmentFileWriter;
    }

    @Override
    public Future<?> execute(AssignmentEnvelopePayload assignmentEnvelopePayload) {
        // Write only new assignments
        if (assignmentEnvelopePayload.getAssignmentStatus() == Assignment.Status.NEW_ASSIGNMENT) {
            logger.debug("new assignment is created.");
            return threadPoolExecutor.submit(() -> {
                logger.debug("writing assignment details to the assignments file.");
                assignmentFileWriter.writeAssignmentDetails(assignmentEnvelopePayload);
            });
        }
        return null;
    }

    @Override
    public int queueLength() {
        return 0;
    }

    @Override
    public Map<String, Object> queueDetails() {
        return null;
    }

    @Override
    public void flushMessages() {
        // do nothing
    }

    @Override
    public String name() {
        return NAME;
    }
}
