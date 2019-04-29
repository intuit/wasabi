package com.intuit.wasabi.assignmentlogger.impl;

import com.intuit.wasabi.assignmentlogger.utlities.FileLogger;
import com.intuit.wasabi.assignmentlogger.AssignmentLogger;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;




public class AssignmentFileLogger implements AssignmentLogger {

  public void logAssignemntEnvelope(AssignmentEnvelopePayload assignmentEnvelop)
  {
    FileLogger.writeDateToFile(assignmentEnvelop.toJson());
  }

}
