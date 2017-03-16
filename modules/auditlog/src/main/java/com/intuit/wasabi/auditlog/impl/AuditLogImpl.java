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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.repository.AuditLogRepository;

import java.util.List;

import static com.intuit.wasabi.auditlog.AuditLogAnnotations.AUDITLOG_FETCHLIMIT;

/**
 * Implements the AuditLog with a default implementation.
 */
public class AuditLogImpl implements AuditLog {

    private final AuditLogRepository repository;
    private int limit;

    /**
     * Constructs the AuditLogImpl. Should be called by Guice.
     *
     * @param repository the repository to query from.
     * @param limit      maximum audit log to fetch
     */
    @Inject
    public AuditLogImpl(final AuditLogRepository repository,
                        final @Named(AUDITLOG_FETCHLIMIT) int limit) {
        this.repository = repository;
        this.limit = limit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getAuditLogs(Application.Name applicationName) {
        return repository.getAuditLogEntryList(applicationName, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getAuditLogs() {
        return repository.getCompleteAuditLogEntryList(limit);
    }


}
