package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class NoopAuditLogImplTest {

    @Test
    public void testNoopAuditLogImplTest() throws Exception {
        AuditLog auditLog = new NoopAuditLogImpl();
        Assert.assertEquals("", Collections.emptyList(), auditLog.getAuditLogs(null, null, null));
        Assert.assertEquals("", Collections.emptyList(), auditLog.getAuditLogs(null, null));
        Assert.assertEquals("", Collections.emptyList(), auditLog.getGlobalAuditLogs(null, null));
        Assert.assertEquals("", Collections.emptyList(), auditLog.getGlobalAuditLogs(null, null));
        List<AuditLogEntry> inList = Collections.singletonList(Mockito.mock(AuditLogEntry.class));
        Assert.assertEquals("", inList, auditLog.filter(inList, null));
        Assert.assertEquals("", inList, auditLog.sort(inList, null));
    }
}
