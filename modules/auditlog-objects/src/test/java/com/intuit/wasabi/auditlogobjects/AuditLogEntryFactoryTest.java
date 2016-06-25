package com.intuit.wasabi.auditlogobjects;

import com.intuit.wasabi.eventlog.events.*;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

/**
 * Tests for {@link AuditLogEntryFactory}.
 */
public class AuditLogEntryFactoryTest {

    @Test
    public void testCreateFromEvent() throws Exception {
        new AuditLogEntryFactory();

        EventLogEvent[] events = new EventLogEvent[]{
                new SimpleEvent("SimpleEvent"),
                new ExperimentChangeEvent(Mockito.mock(Experiment.class), "Property", "before", "after"),
                new ExperimentCreateEvent(Mockito.mock(Experiment.class)),
                new BucketCreateEvent(Mockito.mock(Experiment.class), Mockito.mock(Bucket.class))

        };

        Field[] fields = AuditLogEntry.class.getFields();
        for (Field field : fields) {
            field.setAccessible(true);
        }

        for (EventLogEvent event : events) {
            AuditLogEntry aleFactory = AuditLogEntryFactory.createFromEvent(event);
            AuditLogEntry aleManual = new AuditLogEntry(
                    event.getTime(), event.getUser(), AuditLogAction.getActionForEvent(event),
                    event instanceof ExperimentEvent ? ((ExperimentEvent) event).getExperiment() : null,
                    event instanceof BucketEvent ? ((BucketEvent) event).getBucket().getLabel() : null,
                    event instanceof ChangeEvent ? ((ChangeEvent) event).getPropertyName() : null,
                    event instanceof ChangeEvent ? ((ChangeEvent) event).getBefore() : null,
                    event instanceof ChangeEvent ? ((ChangeEvent) event).getAfter() : null
            );

            for (Field field : fields) {
                Assert.assertEquals(field.get(aleManual), field.get(aleFactory));
            }
        }
    }
}
