/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.auditlogobjects;

import com.intuit.wasabi.eventlog.events.BucketCreateEvent;
import com.intuit.wasabi.eventlog.events.BucketEvent;
import com.intuit.wasabi.eventlog.events.ChangeEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.eventlog.events.ExperimentCreateEvent;
import com.intuit.wasabi.eventlog.events.ExperimentEvent;
import com.intuit.wasabi.eventlog.events.SimpleEvent;
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
