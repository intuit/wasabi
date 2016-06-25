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
package com.intuit.wasabi.repository.impl.cassandra;

import com.google.inject.Inject;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.ExperimentDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.*;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.PreparedCqlQuery;
import com.netflix.astyanax.serializers.DateSerializer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The AuditLog repository wraps calls to cassandra for the AuditLogs.
 */
public class CassandraAuditLogRepository implements AuditLogRepository {

    private static final Logger LOGGER = getLogger(CassandraAuditLogRepository.class);
    private final CassandraDriver driver;
    private final ExperimentsKeyspace keyspace;

    @Inject
    public CassandraAuditLogRepository(@ExperimentDriver CassandraDriver driver, ExperimentsKeyspace keyspace) throws IOException, ConnectionException {
        this.driver = driver;
        this.keyspace = keyspace;
    }

    /**
     * Retrieves the complete list of AuditLogEntries for all application and global events.
     *
     * @return a list of those entries
     */
    @Override
    public List<AuditLogEntry> getCompleteAuditLogEntryList() {
        String cql = "SELECT * FROM auditlog;";
        Rows<Application.Name, String> rows = cqlSelectAll(cql);
        return readAuditLogEntryList(null, rows);
    }

    /**
     * Retrieves a limited list of AuditLogEntries for all application and global events.
     *
     * @param limit the limit
     * @return a list of those AuditLogEntries
     */
    @Override
    public List<AuditLogEntry> getCompleteAuditLogEntryList(int limit) {
        String cql = "SELECT * FROM auditlog LIMIT " + limit + ";";
        Rows<Application.Name, String> rows = cqlSelectAll(cql);
        return readAuditLogEntryList(null, rows);
    }

    /**
     * Retrieves the complete list of AuditLogEntries for a specified application.
     *
     * @param applicationName the application to select
     * @return a list of those entries
     */
    @Override
    public List<AuditLogEntry> getAuditLogEntryList(Application.Name applicationName) {
        String cql = "SELECT * FROM auditlog WHERE application_name = ?;";
        Rows<Application.Name, String> rows = cqlWithApplication(cql, applicationName);
        return readAuditLogEntryList(applicationName, rows);
    }

    /**
     * Retrieves a limited list of AuditLogEntries for a specified application.
     *
     * @param applicationName the application to select
     * @param limit           the limit
     * @return a list of those AuditLogEntries
     */
    @Override
    public List<AuditLogEntry> getAuditLogEntryList(Application.Name applicationName, int limit) {
        String cql = "SELECT * FROM auditlog WHERE application_name = ? LIMIT " + limit + ";";
        Rows<Application.Name, String> rows = cqlWithApplication(cql, applicationName);
        return readAuditLogEntryList(applicationName, rows);
    }

    /**
     * Retrieves the complete list of AuditLogEntries for global events.
     *
     * @return a list of those entries
     */
    @Override
    public List<AuditLogEntry> getGlobalAuditLogEntryList() {
        String cql = "SELECT * FROM auditlog WHERE application_name = ?;";
        Rows<Application.Name, String> rows = cqlWithApplication(cql, AuditLogRepository.GLOBAL_ENTRY_APPLICATION);
        return readAuditLogEntryList(AuditLogRepository.GLOBAL_ENTRY_APPLICATION, rows);
    }

    /**
     * Retrieves a limited list of AuditLogEntries for global events.
     *
     * @param limit the limit
     * @return a list of those AuditLogEntries
     */
    @Override
    public List<AuditLogEntry> getGlobalAuditLogEntryList(int limit) {
        String cql = "SELECT * FROM auditlog WHERE application_name = ? LIMIT " + limit + ";";
        Rows<Application.Name, String> rows = cqlWithApplication(cql, AuditLogRepository.GLOBAL_ENTRY_APPLICATION);
        return readAuditLogEntryList(AuditLogRepository.GLOBAL_ENTRY_APPLICATION, rows);
    }

    /**
     * Stores an AuditLogEntry into the database.
     * Makes sure that when no ApplicationName is supplied, the {@link #GLOBAL_ENTRY_APPLICATION} is used instead.
     *
     * @param entry the entry to store
     * @return true on success
     * @throws RepositoryException if the required entry values {@link AuditLogEntry#getAction()},
     *      {@link AuditLogEntry#getUser()}, {@link AuditLogEntry#getTime()} or the entry itself are null.
     */
    @Override
    public boolean storeEntry(AuditLogEntry entry) {
        if (entry == null || entry.getAction() == null || entry.getUser() == null || entry.getTime() == null) {
            throw new RepositoryException("Can not insert AuditLogEntry " + entry + " into database, required values are null.");
        }
        String cql = "INSERT INTO auditlog ( event_id, application_name, time, action, "
                + "user_firstname, user_lastname, user_email, user_username, user_userid, "
                + "experiment_id, experiment_label, bucket_label, "
                + "changed_property, property_before, property_after )"
                + " VALUES ( uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? );";

        PreparedCqlQuery<Application.Name, String> cqlQuery = driver.getKeyspace().prepareQuery(keyspace.auditlogCF()).withCql(cql).asPreparedStatement()
                .withByteBufferValue(entry.getApplicationName() == null ? AuditLogRepository.GLOBAL_ENTRY_APPLICATION : entry.getApplicationName(), ApplicationNameSerializer.get())
                .withByteBufferValue(entry.getTime().getTime(), DateSerializer.get())
                .withStringValue(entry.getAction().toString())
                .withStringValue(entry.getUser().getFirstName() != null ? entry.getUser().getFirstName() : "")
                .withStringValue(entry.getUser().getLastName() != null ? entry.getUser().getLastName() : "")
                .withStringValue(entry.getUser().getEmail() != null ? entry.getUser().getEmail() : "")
                .withByteBufferValue(entry.getUser().getUsername(), UsernameSerializer.get())
                .withStringValue(entry.getUser().getUserId() != null ? entry.getUser().getUserId() : "");

        if (entry.getExperimentId() != null) {
            cqlQuery.withByteBufferValue(entry.getExperimentId(), ExperimentIDSerializer.get());
        } else {
            cqlQuery.withStringValue("");
        }

        if (entry.getExperimentLabel() != null) {
            cqlQuery.withByteBufferValue(entry.getExperimentLabel(), ExperimentLabelSerializer.get());
        } else {
            cqlQuery.withStringValue("");
        }

        if (entry.getBucketLabel() != null) {
            cqlQuery.withByteBufferValue(entry.getBucketLabel(), BucketLabelSerializer.get());
        } else {
            cqlQuery.withStringValue("");
        }

        cqlQuery.withStringValue(entry.getChangedProperty() != null ? entry.getChangedProperty() : "")
                .withStringValue(entry.getBefore() != null ? entry.getBefore() : "")
                .withStringValue(entry.getAfter() != null ? entry.getAfter() : "");

        try {
            cqlQuery.execute();
        } catch (ConnectionException e) {
            LOGGER.error("Could not write AuditLogEntry " + entry + " to database. Record is lost!", e);
            return false;
        }
        return true;
    }


    /**
     * Allows cql select or update queries which have only an ApplicationName as a prepared value.
     *
     * @param cql the query
     * @param applicationName the application to put in
     * @return the resulting rows.
     *
     * @throws RepositoryException if an {@link ConnectionException} occurs.
     */
    /*test*/ Rows<Application.Name, String> cqlWithApplication(String cql, Application.Name applicationName) {
        try {
            OperationResult<CqlResult<Application.Name, String>> result =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.auditlogCF())
                            .withCql(cql)
                            .asPreparedStatement()
                            .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                            .execute();
            return result.getResult().getRows();
        } catch (ConnectionException e) {
            throw new RepositoryException(
                    String.format("Can not retrieve auditlog for application %s.", applicationName),
                    e
            );
        }
    }

    /**
     * Allows cql select or update queries which have no prepared values.
     *
     * @param cql the query
     * @return the resulting rows.
     *
     * @throws RepositoryException if an {@link ConnectionException} occurs.
     */
    /*test*/ Rows<Application.Name, String> cqlSelectAll(String cql) {
        try {
            OperationResult<CqlResult<Application.Name, String>> result =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.auditlogCF())
                            .withCql(cql)
                            .asPreparedStatement()
                            .execute();
            return result.getResult().getRows();
        } catch (ConnectionException e) {
            throw new RepositoryException("Can not retrieve complete auditlog.", e);
        }
    }

    /**
     * Reads {@link Rows} (for example from {@link #cqlWithApplication(String, Application.Name)}) and
     * returns the contained list of audit log entries.
     * <br />
     * If the rowKey is null, the key will be read from the query.
     *
     * @param rowKey the key used to query cassandra
     * @param rows the rows object
     * @return a list of AuditLogEntries
     */
    /*test*/ List<AuditLogEntry> readAuditLogEntryList(Application.Name rowKey, Rows<Application.Name, String> rows) {
        List<AuditLogEntry> auditLogEntries = new ArrayList<>();
        for (Row<Application.Name, String> row : rows) {
            AuditLogEntry auditLogEntry;
            if ((auditLogEntry = readAuditLogEntry(rowKey == null ? row.getKey() : rowKey, row.getColumns())) != null) {
                auditLogEntries.add(auditLogEntry);
            }
        }
        return auditLogEntries;
    }

    /**
     * Reads an AuditLogEntry from a ColumnList.
     *
     * @param applicationName the application name / row key
     * @param columnList the columnList
     * @return the auditLogEntry
     */
    /*test*/ AuditLogEntry readAuditLogEntry(Application.Name applicationName, ColumnList<String> columnList) {
        if (columnList == null) {
            return null;
        }
        try {
            /// application name
            Application.Name appName = AuditLogRepository.GLOBAL_ENTRY_APPLICATION.equals(applicationName) ? null : applicationName;

            // deserialize time
            Calendar time = null;
            Date date;
            if ((date = columnList.getDateValue("time", null)) != null) {
                time = Calendar.getInstance();
                time.setTime(date);
            }

            // deserialize user
            String username = columnList.getStringValue("user_username", "");
            String firstname = columnList.getStringValue("user_firstname", "");
            String lastname = columnList.getStringValue("user_lastname", "");
            String email = columnList.getStringValue("user_email", "");
            String userid = columnList.getStringValue("user_userid", "");
            UserInfo user = null;
            if (!StringUtils.isBlank(username) || !StringUtils.isBlank(userid)) {
                user = (username != null ? UserInfo.from(UserInfo.Username.valueOf(username))
                        : UserInfo.from(UserInfo.Username.valueOf(userid)))
                        .withFirstName(StringUtils.isBlank(firstname) ? null : firstname)
                        .withLastName(StringUtils.isBlank(lastname) ? null : lastname)
                        .withUserId(StringUtils.isBlank(userid) ? null : userid)
                        .withEmail(StringUtils.isBlank(email) ? null : email)
                        .build();
            }

            // deserialize action
            String actionStr = columnList.getStringValue("action", AuditLogAction.UNSPECIFIED_ACTION.name());
            AuditLogAction action;
            //TODO: remove this try catch block.
            try {
                action = AuditLogAction.valueOf(actionStr);
            } catch (IllegalArgumentException e) {
                LOGGER.debug(String.format("Unknown action %s setting it to default.", actionStr), e);
                action = AuditLogAction.UNSPECIFIED_ACTION;
            }

            // deserialize experiment properties
            Experiment.Label experimentLabel = columnList.getValue("experiment_label", ExperimentLabelSerializer.get(), null);
            Experiment.ID experimentId = columnList.getValue("experiment_id", ExperimentIDSerializer.get(), null);

            // deserialize bucket properties
            Bucket.Label bucketLabel = columnList.getValue("bucket_label", BucketLabelSerializer.get(), null);

            // deserialize property changes
            String changedProperty = columnList.getStringValue("changed_property", "");
            changedProperty = StringUtils.isBlank(changedProperty) ? null : changedProperty;
            String propertyBefore = columnList.getStringValue("property_before", "");
            propertyBefore = StringUtils.isBlank(propertyBefore) ? null : propertyBefore;
            String propertyAfter = columnList.getStringValue("property_after", "");
            propertyAfter = StringUtils.isBlank(propertyAfter) ? null : propertyAfter;

            return new AuditLogEntry(time, user, action, appName,
                    experimentLabel, experimentId, bucketLabel,
                    changedProperty, propertyBefore, propertyAfter);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Can not read AuditLogEntry, data in auditlog column family might be corrupted for key '" + applicationName + "': ", e);
        }
        return null;
    }

}
