package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.experimentobjects.Application;

import java.util.Collections;
import java.util.List;

/**
 * The Noop implementation does nothing and always returns the original lists or empty lists.
 */
public class NoopAuditLogImpl implements AuditLog {

    /**
     * Returns an empty list.
     *
     * @param applicationName the application
     * @param filterMask      the filter mask
     * @param sortOrder       the sort order
     * @return a list of filtered and sorted audit logs
     */
    @Override
    public List<AuditLogEntry> getAuditLogs(Application.Name applicationName, String filterMask, String sortOrder) {
        return Collections.emptyList();
    }

    /**
     * Returns an empty list.
     *
     * @param filterMask the filter mask
     * @param sortOrder  the sort order
     * @return the list of AuditLogEntries
     */
    @Override
    public List<AuditLogEntry> getAuditLogs(String filterMask, String sortOrder) {
        return Collections.emptyList();
    }

    /**
     * Returns an empty list.
     *
     * @param filterMask the filter mask
     * @param sortOrder  the sort order
     * @return the list of AuditLogEntries
     */
    @Override
    public List<AuditLogEntry> getGlobalAuditLogs(String filterMask, String sortOrder) {
        return Collections.emptyList();
    }

    /**
     * Returns the input list.
     *
     * @param auditLogEntries the list to be filtered
     * @param filterMasks     the filter mask
     * @return a filtered list
     */
    @Override
    public List<AuditLogEntry> filter(List<AuditLogEntry> auditLogEntries, String filterMasks) {
        return auditLogEntries;
    }

    /**
     * Returns the input list.
     *
     * @param auditLogEntries the list to be sorted
     * @param sortOrder       the sort order
     * @return a sorted list
     */
    @Override
    public List<AuditLogEntry> sort(List<AuditLogEntry> auditLogEntries, String sortOrder) {
        return auditLogEntries;
    }

}
