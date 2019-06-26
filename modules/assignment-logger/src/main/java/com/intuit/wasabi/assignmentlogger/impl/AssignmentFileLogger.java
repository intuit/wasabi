package com.intuit.wasabi.assignmentlogger.impl;

import com.intuit.wasabi.assignmentlogger.utlities.FileLogger;
import com.intuit.wasabi.assignmentlogger.AssignmentLogger;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;

import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_FILE_PATH;
import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_FILE_NAME;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class AssignmentFileLogger implements AssignmentLogger {

  private String filePath;
  private String fileName;


  @Inject
  public AssignmentFileLogger(final @Named(LOGGER_FILE_PATH) String filePath,
                                 final @Named(LOGGER_FILE_NAME) String fileName
   ) {
      super();
      this.filePath = filePath;
      this.fileName = fileName;
  }


  public void logAssignemntEnvelope(AssignmentEnvelopePayload assignmentEnvelop)
  {
    FileLogger.writeDateToFile(assignmentEnvelop.toJson(), this.filePath, this.fileName);
  }

}
