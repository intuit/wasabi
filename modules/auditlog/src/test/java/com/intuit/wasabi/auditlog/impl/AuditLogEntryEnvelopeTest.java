package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.repository.AuditLogRepository;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link AuditLogEntryEnvelope}.
 */
public class AuditLogEntryEnvelopeTest {

    @Test
    public void testEnvelope() {
        AuditLogEntry entry = Mockito.mock(AuditLogEntry.class);
        AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);

        AuditLogEntryEnvelope envelope = new AuditLogEntryEnvelope(entry, repository);
        envelope.run();
    }
}
