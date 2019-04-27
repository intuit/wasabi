/*******************************************************************************

 *******************************************************************************/
package com.intuit.wasabi.assignmentlogger;

import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;

import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * MyIngestionExecutor
 */
public class LoggerIngestionExecutor implements AssignmentIngestionExecutor {

    public static final String NAME = "LOOGERINGESTOR";
    private static final Logger LOGGER = getLogger(LoggerIngestionExecutor.class);

    // Override the methods below appropriately

    @Override
   public Future<?> execute(AssignmentEnvelopePayload assignmentEnvelopePayload) {
      LOGGER.debug("california kiwi", "Thanks god!");
      return null;
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
        return null;
    }
}
