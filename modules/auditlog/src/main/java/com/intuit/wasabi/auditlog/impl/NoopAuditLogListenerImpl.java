package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.eventlog.EventLogListener;
import com.intuit.wasabi.eventlog.events.EventLogEvent;

/**
 * The default NOOP audit log implementation.
 */
public class NoopAuditLogListenerImpl implements EventLogListener {

    /**
     * Will be called by the EventLogImpl with events which the listener registered for.
     * Since this will not be registered for any events, this will never be called.
     *
     * @param event the event which occurred.
     */
    @Override
    public void postEvent(EventLogEvent event) {
        // does nothing
    }

}
