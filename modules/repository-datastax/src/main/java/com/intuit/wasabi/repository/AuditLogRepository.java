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
package com.intuit.wasabi.repository;

import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.experimentobjects.Application;

import java.util.List;

/**
 * The AuditLogRepository allows easy access to the auditlog tables.
 *
 * @see Application
 * @see AuditLogEntry
 */
public interface AuditLogRepository {

    /**
     * This constant shall be used to avoid null values for the application name for global events.
     */
    Application.Name GLOBAL_ENTRY_APPLICATION = Application.Name.valueOf("dGhpcyBpcyBhIG51bGwgZmllbGQ");

    /**
     * Retrieves the complete list of AuditLogEntries for all application and global events.
     *
     * @return a list of those entries
     */
    List<AuditLogEntry> getCompleteAuditLogEntryList();

    /**
     * Retrieves a limited list of AuditLogEntries for all application and global events.
     *
     * @param limit the limit of audit log entries returned
     * @return a list of those AuditLogEntries
     */
    List<AuditLogEntry> getCompleteAuditLogEntryList(int limit);

    /**
     * Retrieves the complete list of AuditLogEntries for a specified application.
     *
     * @param applicationName the application to select
     * @return a list of those entries
     */
    List<AuditLogEntry> getAuditLogEntryList(Application.Name applicationName);

    /**
     * Retrieves a limited list of AuditLogEntries for a specified application.
     *
     * @param applicationName the application to select
     * @param limit           the limit of log entried returend
     * @return a list of those AuditLogEntries
     */
    List<AuditLogEntry> getAuditLogEntryList(Application.Name applicationName, int limit);

    /**
     * Retrieves the complete list of AuditLogEntries for global events.
     *
     * @return a list of those entries
     */
    List<AuditLogEntry> getGlobalAuditLogEntryList();

    /**
     * Retrieves a limited list of AuditLogEntries for global events.
     *
     * @param limit the limit
     * @return a list of those AuditLogEntries
     */
    List<AuditLogEntry> getGlobalAuditLogEntryList(int limit);

    /**
     * Stores an AuditLogEntry into the database.
     * Makes sure that when no ApplicationName is supplied, the {@link #GLOBAL_ENTRY_APPLICATION} is used instead.
     *
     * @param entry the entry to store
     * @return true on success
     */
    boolean storeEntry(AuditLogEntry entry);
}
