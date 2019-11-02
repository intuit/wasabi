package com.intuit.wasabi.assignmentwriter;

import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;

/**
 * Assignments writer interface
 */
public interface AssignmentsFileWriter {

    /**
     * Writes assignment details to a file.
     * @param assignmentEnvelopePayload assignment details to be written to a file.
     */
    public void writeAssignmentDetails(AssignmentEnvelopePayload assignmentEnvelopePayload);
}
