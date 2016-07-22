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
     * @return a list of filtered and sorted audit logs
     */
    @Override
    public List<AuditLogEntry> getAuditLogs(Application.Name applicationName) {
        return Collections.emptyList();
    }

    /**
     * Returns an empty list.
     *
     * @return the list of AuditLogEntries
     */
    @Override
    public List<AuditLogEntry> getAuditLogs() {
        return Collections.emptyList();
    }

}
