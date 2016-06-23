package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.eventlog.events.SimpleEvent;
import org.junit.Test;

/**
 * Tests for {@link NoopAuditLogListenerImpl}.
 */
public class NoopAuditLogListenerImplTest {

    @Test
    public void testPostEvent() {
        new NoopAuditLogListenerImpl().postEvent(new SimpleEvent("SimpleEvent"));
    }
}
