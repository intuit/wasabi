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
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.EventLogListener;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.repository.AuditLogRepository;
import org.slf4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static com.intuit.wasabi.auditlog.AuditLogAnnotations.AUDITLOG_THREADPOOLSIZE_CORE;
import static com.intuit.wasabi.auditlog.AuditLogAnnotations.AUDITLOG_THREADPOOLSIZE_MAX;
import static com.intuit.wasabi.auditlogobjects.AuditLogEntryFactory.createFromEvent;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The AuditLogListener subscribes to events which should be logged for the user interface.
 */
public class AuditLogListenerImpl implements EventLogListener {

    /**
     * Executes the {@link AuditLogEntryEnvelope}s.
     */
    private final ThreadPoolExecutor threadPoolExecutor;
    private final AuditLogRepository repository;
    private Logger LOGGER = getLogger(AuditLogListenerImpl.class);

    /**
     * Initializes the audit log.
     *
     * @param eventLog           the event log to subscribe to
     * @param threadPoolSizeCore the core threadpool size (java property {@code auditlog.threadpoolsize.core})
     * @param threadPoolSizeMax  the max threadpool size (java property {@code auditlog.threadpoolsize.max})
     * @param repository         the audit log repository
     */
    @Inject
    public AuditLogListenerImpl(final EventLog eventLog,
                                final @Named(AUDITLOG_THREADPOOLSIZE_CORE) int threadPoolSizeCore,
                                final @Named(AUDITLOG_THREADPOOLSIZE_MAX) int threadPoolSizeMax,
                                final AuditLogRepository repository) {
        this.repository = repository;
        eventLog.register(this);

        threadPoolExecutor = new ThreadPoolExecutor(threadPoolSizeCore, threadPoolSizeMax, 0L, MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Will be called by the EventLogImpl with events which the listener registered for.
     *
     * @param event the event which occurred.
     */
    @Override
    public void postEvent(EventLogEvent event) {
        AuditLogEntryEnvelope auditLogEntryEnvelope = new AuditLogEntryEnvelope(createFromEvent(event), repository);

        LOGGER.debug("posting auditLogEntryEnvelope: {}", auditLogEntryEnvelope);

        threadPoolExecutor.submit(auditLogEntryEnvelope);

        LOGGER.debug("posted auditLogEntryEnvelope: {}", auditLogEntryEnvelope);
    }
}