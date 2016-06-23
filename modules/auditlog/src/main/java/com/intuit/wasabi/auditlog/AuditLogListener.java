package com.intuit.wasabi.auditlog;

import com.intuit.wasabi.eventlog.EventLogListener;

/**
 * Tagging interface for AuditLogListeners, which must implement EventLogListeners.
 */
public interface AuditLogListener extends EventLogListener {
}
