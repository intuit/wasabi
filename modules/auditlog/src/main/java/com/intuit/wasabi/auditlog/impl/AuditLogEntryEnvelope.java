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
package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.repository.AuditLogRepository;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The AuditLogEntryEnvelope wraps AuditLogEntry to be sent to cassandra.
 */
/*pkg*/ class AuditLogEntryEnvelope implements Runnable {

    /**
     * The AuditLogEntry to be sent to the databse.
     */
    private final AuditLogEntry entry;
    private final AuditLogRepository repository;
    private Logger LOGGER = getLogger(AuditLogEntryEnvelope.class);

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
        LOGGER.debug("storing auditLogEntry: {}", entry);

        repository.storeEntry(entry);

        LOGGER.debug("stored auditLogEntry: {}", entry);
    }
}