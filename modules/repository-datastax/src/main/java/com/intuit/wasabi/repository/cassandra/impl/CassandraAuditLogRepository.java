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
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.mapping.Result;
import com.google.inject.Inject;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.audit.AuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The AuditLog repository wraps calls to cassandra for the AuditLogs.
 */
public class CassandraAuditLogRepository implements AuditLogRepository {

    /**
     * Logger for teh class
     */
    private static final Logger LOGGER = getLogger(CassandraAuditLogRepository.class);

    /**
     * Accessor interface for audit log table
     */
    private AuditLogAccessor accessor;

    /**
     * Constructor
     *
     * @param accessor
     */
    @Inject
    public CassandraAuditLogRepository(AuditLogAccessor accessor) {
        this.accessor = accessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getCompleteAuditLogEntryList() {
        LOGGER.debug("Getting complete audit log entries");

        try {

            Result<AuditLog> result = accessor.getCompleteAuditLogEntryList();

            return makeAuditLogEntries(result.all());
        } catch (Exception e) {
            LOGGER.error("Error while getting audit log entries list", e);
            throw new RepositoryException(
                    "Unable to receive complete audit log entry list");
        }
    }

    /**
     * Helper method to get empty string for null
     *
     * @param input string input
     * @return input or empty string
     */
    private String makeEmptyStringForNull(String input) {
        return input == null ? "" : input;
    }

    /**
     * Helper method to translate audit log list
     *
     * @param entries audit log table pojo list
     * @return audit log domain object list
     */
    protected List<AuditLogEntry> makeAuditLogEntries(List<AuditLog> entries) {
        List<AuditLogEntry> auditLogEntries = new ArrayList<>(entries.size());

        for (AuditLog auditLog : entries) {

            // Note time cannot be null since it is required during save
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(auditLog.getTime());

            String userName = makeEmptyStringForNull(auditLog.getUsername());

            String email = makeEmptyStringForNull(auditLog.getEmail());
            String firstName = makeEmptyStringForNull(auditLog.getFirstName());
            String lastName = makeEmptyStringForNull(auditLog.getLastName());
            String userId = makeEmptyStringForNull(auditLog.getUserId());

            UserInfo userInfo = UserInfo
                    .newInstance(Username.valueOf(userName)).withEmail(email)
                    .withFirstName(firstName).withLastName(lastName)
                    .withUserId(userId).build();

            // The intent of the multiple try blocks is to get as much info to
            // the client as possible. The exceptions are logged as warn
            AuditLogAction ala = null;
            String action = null;
            try {
                action = auditLog.getAction();
                ala = AuditLogAction.valueOf(action);
            } catch (Exception e) {
                LOGGER.warn("Exception while creating audit log action: "
                        + action, e);
                ala = AuditLogAction.UNSPECIFIED_ACTION;
            }

            Bucket.Label bucketLabel = null;
            String bucketLabelString = null;
            try {
                bucketLabelString = auditLog.getBucketLabel();
                if (!StringUtils.isBlank(bucketLabelString))
                    bucketLabel = Bucket.Label.valueOf(bucketLabelString);
            } catch (Exception e) {
                LOGGER.warn("Exception while creating audit log bucket label: "
                        + bucketLabelString, e);
            }

            Experiment.Label experimentLabel = null;
            String expLabelString = null;
            try {
                expLabelString = auditLog.getExperimentLabel();
                if (!StringUtils.isBlank(expLabelString))
                    experimentLabel = Label.valueOf(expLabelString);
            } catch (Exception e) {
                LOGGER.warn("Exception while creating audit log experiment label: "
                        + expLabelString, e);
            }

            Experiment.ID experimentId = null;
            UUID expId = null;
            try {
                expId = auditLog.getExperimentId();
                if (expId != null)
                    experimentId = Experiment.ID
                            .valueOf(expId);
            } catch (Exception e) {
                LOGGER.warn("Exception while creating audit log experiment id: "
                        + expId, e);
            }

            // Set application name only if it is not GLOBAL_ENTRY_APPLICATION
            Application.Name appName = null;
            if (!AuditLogRepository.GLOBAL_ENTRY_APPLICATION.toString().equals(
                    auditLog.getAppName()))
                appName = Application.Name.valueOf(auditLog.getAppName());

            AuditLogEntry entry = new AuditLogEntry(calendar, userInfo, ala,
                    appName, experimentLabel, experimentId, bucketLabel,
                    auditLog.getProperty(), auditLog.getBefore(),
                    auditLog.getAfter());

            auditLogEntries.add(entry);
        }

        return auditLogEntries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getCompleteAuditLogEntryList(int limit) {
        LOGGER.debug("Getting complete audit log entries with limit {}", limit);
        try {

            Result<AuditLog> result = accessor
                    .getCompleteAuditLogEntryList(limit);

            return makeAuditLogEntries(result.all());
        } catch (Exception e) {
            LOGGER.error(
                    "Error while getting complete audit log entries list with limit",
                    e);
            throw new RepositoryException(
                    "Unable to receive complete audit log entry list");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getAuditLogEntryList(
            Application.Name applicationName) {
        LOGGER.debug("Getting audit log entries for {}", applicationName);

        try {

            Result<AuditLog> result = accessor
                    .getAuditLogEntryList(applicationName.toString());

            return makeAuditLogEntries(result.all());
        } catch (Exception e) {
            LOGGER.error("Error while getting complete audit log entries list",
                    e);
            throw new RepositoryException(
                    "Unable to receive complete audit log entry list");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getAuditLogEntryList(
            Application.Name applicationName, int limit) {
        LOGGER.debug("Getting audit log entries for application name {} and limit {}",
                applicationName, limit);
        try {

            Result<AuditLog> result = accessor.getAuditLogEntryList(
                    applicationName.toString(), limit);

            return makeAuditLogEntries(result.all());
        } catch (Exception e) {
            LOGGER.error(
                    "Error while getting audit log entries list for application with limit",
                    e);
            throw new RepositoryException(
                    "Unable to receive audit log entry list");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getGlobalAuditLogEntryList() {
        LOGGER.debug("Getting global audit log entries");
        try {

            Result<AuditLog> result = accessor
                    .getAuditLogEntryList(AuditLogRepository.GLOBAL_ENTRY_APPLICATION
                            .toString());

            return makeAuditLogEntries(result.all());
        } catch (Exception e) {
            LOGGER.error("Error while getting global audit log entries list", e);
            throw new RepositoryException(
                    "Unable to receive global audit log entry list");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getGlobalAuditLogEntryList(int limit) {
        LOGGER.debug("Getting global audit log entries with limit {}", limit);

        try {

            Result<AuditLog> result = accessor.getAuditLogEntryList(
                    AuditLogRepository.GLOBAL_ENTRY_APPLICATION.toString(),
                    limit);

            return makeAuditLogEntries(result.all());
        } catch (Exception e) {
            LOGGER.error(
                    "Error while getting global audit log entries list with limit",
                    e);
            throw new RepositoryException(
                    "Unable to receive global audit log entry list");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean storeEntry(AuditLogEntry entry) {
        LOGGER.debug("Storing entry {}", entry);

        // Note checks for action, time and action are redundant since
        // they are already checked in AuditLogEntry
        if (entry == null || entry.getAction() == null
                || entry.getUser() == null || entry.getTime() == null) {
            throw new RepositoryException("Can not insert AuditLogEntry "
                    + entry + " into database, required values are null.");
        }

        // If application name is null use global entry name
        String applicationName = AuditLogRepository.GLOBAL_ENTRY_APPLICATION
                .toString();
        if (entry.getApplicationName() != null
                && !StringUtils.isBlank(entry.getApplicationName().toString())) {
            applicationName = entry.getApplicationName().toString();
        }

        Date time = entry.getTime().getTime();
        String action = entry.getAction().toString();

        String firstName = makeEmptyStringForNull(entry.getUser().getFirstName());
        String lastName = makeEmptyStringForNull(entry.getUser().getLastName());
        String email = makeEmptyStringForNull(entry.getUser().getEmail());
        String userId = makeEmptyStringForNull(entry.getUser().getUserId());
        String userName = makeEmptyStringForNull(entry.getUser().getUsername()
                .getUsername());

        UUID experimentId = entry.getExperimentId() == null ? null : entry
                .getExperimentId().getRawID();
        String experimentLabel = entry.getExperimentLabel() == null ? ""
                : entry.getExperimentLabel().toString();
        String bucketLabel = entry.getBucketLabel() == null ? "" : entry
                .getBucketLabel().toString();

        String changedProperty = makeEmptyStringForNull(entry.getChangedProperty());
        String before = makeEmptyStringForNull(entry.getBefore());
        String after = makeEmptyStringForNull(entry.getAfter());

        accessor.storeEntry(applicationName, time, action, firstName, lastName,
                email, userName, userId, experimentId, experimentLabel,
                bucketLabel, changedProperty, before, after);

        return true;

    }

}
