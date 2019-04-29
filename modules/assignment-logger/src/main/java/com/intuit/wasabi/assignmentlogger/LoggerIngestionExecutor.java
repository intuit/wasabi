/*******************************************************************************

 *******************************************************************************/
package com.intuit.wasabi.assignmentlogger;

import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.inject.Inject;
import com.google.inject.name.Named;


import java.util.Map;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_EXECUTOR;

/**
 * Logger IngestionExecutor
 */
public class LoggerIngestionExecutor implements AssignmentIngestionExecutor {

  public static final String NAME = "LOGGER-INGESTOR";
  private static final Logger LOGGER = getLogger(LoggerIngestionExecutor.class);
  private ThreadPoolExecutor loggerExecutor;



  @Inject
  public LoggerIngestionExecutor(final @Named(LOGGER_EXECUTOR) ThreadPoolExecutor executor ) {
      super();
      this.loggerExecutor = executor;
  }

  @Override
   public Future<?> execute(AssignmentEnvelopePayload assignmentEnvelopePayload) {
      // // asynchronously calling the logging logic
      return this.loggerExecutor.submit(() -> {
         Thread.sleep(10000);
          LOGGER.debug("california dump damaged by the sunn  => {}", assignmentEnvelopePayload.toJson());
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
