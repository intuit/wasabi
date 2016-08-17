/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.accessor;

import java.util.Date;
import java.util.UUID;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog;

/**
 * The AuditLogAccessor allows access to the auditlog table.
 */
@Accessor
public interface AuditLogAccessor {

    /**
     * Retrieves the complete list of AuditLogEntries for all application and global events.
     *
     * @return a list of auditlog entries
     */
	@Query("SELECT * FROM auditlog")
	Result<AuditLog> getCompleteAuditLogEntryList();

    /**
     * Retrieves a limited list of AuditLogEntries for all application and global events.
     *
     * @param limit the limit
     * @return a list of auditlog AuditLogEntries
     */
	@Query("SELECT * FROM auditlog limit ?")
	Result<AuditLog> getCompleteAuditLogEntryList(int limit);

    /**
     * Retrieves the complete list of AuditLogEntries for a specified application.
     *
     * @param applicationName the application to select
     * @return a list of auditlog entries
     */
    @Query("SELECT * FROM auditlog WHERE application_name = ?")
    Result<AuditLog> getAuditLogEntryList(String applicationName);

    /**
     * Retrieves a limited list of AuditLogEntries for a specified application.
     *
     * @param applicationName the application to select
     * @param limit the limit
     * @return a list of those AuditLogEntries
     */
    @Query("SELECT * FROM auditlog WHERE application_name = ? limit ?")
    Result<AuditLog> getAuditLogEntryList(String applicationName, int limit);

    /**
     * Stores an AuditLogEntry into the database.
     *
     * @param applicationName
     * @param time
     * @param action
     * @param firstName
     * @param lastName
     * @param email
     * @param userName
     * @param userId
     * @param experimentId
     * @param experimentLabel
     * @param bucketLabel
     * @param changedProperty
     * @param propertyBefore
     * @param propertyAfter
     */
    @Query("INSERT INTO auditlog ( event_id, application_name, time, action, "
                + "user_firstname, user_lastname, user_email, user_username, user_userid, "
                + "experiment_id, experiment_label, bucket_label, "
                + "changed_property, property_before, property_after )"
                + " VALUES ( uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )")
    void storeEntry(String applicationName, Date time, String action,
    		String firstName, String lastName, String email, String userName, String userId,
    		UUID experimentId, String experimentLabel, String bucketLabel,
    		String changedProperty, String propertyBefore, String propertyAfter);
}
