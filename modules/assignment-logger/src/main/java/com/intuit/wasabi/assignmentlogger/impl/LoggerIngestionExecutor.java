/*******************************************************************************

 *******************************************************************************/
package com.intuit.wasabi.assignmentlogger.impl;

import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import com.intuit.wasabi.assignmentlogger.AssignmentLogger;

import com.google.inject.Inject;
import com.google.inject.name.Named;


import java.util.Map;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_EXECUTOR;
import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_ASSIGNMENT_FILE;

/**
 * Logger IngestionExecutor
 */
public class LoggerIngestionExecutor implements AssignmentIngestionExecutor {

  public static final String NAME = "LOGGER-INGESTOR";
  private static final Logger LOGGER = getLogger(LoggerIngestionExecutor.class);
  private ThreadPoolExecutor loggerExecutor;
  private AssignmentLogger assignmentLogger;



  @Inject
  public LoggerIngestionExecutor(final @Named(LOGGER_EXECUTOR) ThreadPoolExecutor executor,
                                 final @Named(LOGGER_ASSIGNMENT_FILE) AssignmentLogger logger
   ) {
      super();
      this.loggerExecutor = executor;
      this.assignmentLogger = logger;
  }

  @Override
   public Future<?> execute(AssignmentEnvelopePayload assignmentEnvelopePayload) {
      // asynchronously calling the logging logic
      return this.loggerExecutor.submit(() -> {
          LOGGER.debug("Logging Assignment Evenlop  => {}", assignmentEnvelopePayload.toJson());
          this.assignmentLogger.logAssignemntEnvelope(assignmentEnvelopePayload);
         // Since no one needs to awaits any returned value as long as we are only logging here
         return null;
      });
    }

    @Override
    public void flushMessages()
    {

    }

    @Override
    public Map<String, Object> queueDetails()
    {
      return null;
    }

    @Override
    public int queueLength() {
        return 0;
    }

    @Override
    public String name() {
        return NAME;
    }
}
