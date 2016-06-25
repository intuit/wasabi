package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.repository.AuditLogRepository;

/**
 * The AuditLogEntryEnvelope wraps AuditLogEntry to be sent to cassandra.
 */
/*pkg*/ class AuditLogEntryEnvelope implements Runnable {

    /**
     * The AuditLogEntry to be sent to the databse.
     */
    private final AuditLogEntry entry;
    private final AuditLogRepository repository;

    /**
     * Wraps {@code entry} into this envelope.
     *
     * @param entry      the entry to wrap
     * @param repository the repository to store events
     */
    public AuditLogEntryEnvelope(final AuditLogEntry entry, final AuditLogRepository repository) {
        this.entry = entry;
        this.repository = repository;
    }

    /**
     * Stores the wrapped AuditLogEntry into the database.
     */
    @Override
    public void run() {
        repository.storeEntry(entry);
    }
}
