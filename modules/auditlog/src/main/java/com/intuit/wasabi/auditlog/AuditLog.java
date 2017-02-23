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
package com.intuit.wasabi.auditlog;

import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.experimentobjects.Application;

import java.util.List;

/**
 * Tagging interface for AuditLogs, which must implement EventLogListeners.
 */
public interface AuditLog {

    /**
     * Returns the auditlog for a given {@link Application.Name} up to the configuration limit.
     *
     * @param applicationName the application name
     * @return the auditlog for the given application
     */
    List<AuditLogEntry> getAuditLogs(Application.Name applicationName);

    /**
     * Returns the complete auditlog up to the configuration limit.
     *
     * @return the complete auditlog
     */
    List<AuditLogEntry> getAuditLogs();

}
