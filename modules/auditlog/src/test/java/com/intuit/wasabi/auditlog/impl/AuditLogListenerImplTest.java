package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.SimpleEvent;
import com.intuit.wasabi.repository.AuditLogRepository;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link AuditLogListenerImpl}.
 */
public class AuditLogListenerImplTest {

    @Test
    public void testPostEvent() {
        new AuditLogListenerImpl(Mockito.mock(EventLog.class), 2, 4, Mockito.mock(AuditLogRepository.class)).postEvent(new SimpleEvent("SimpleEvent"));
    }


}
