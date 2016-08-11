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
import com.intuit.wasabi.repository.cassandra.accessor.AuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
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

	private static final Logger LOGGER = getLogger(CassandraAuditLogRepository.class);
	private AuditLogAccessor accessor;

	@Inject
	public CassandraAuditLogRepository(AuditLogAccessor accessor)
			throws IOException, ConnectionException {
		this.accessor = accessor;
	}

	/**
	 * Retrieves the complete list of AuditLogEntries for all application and
	 * global events.
	 *
	 * @return a list of those entries
	 */
	@Override
	public List<AuditLogEntry> getCompleteAuditLogEntryList() {
		try {

			Result<AuditLog> result = accessor.getCompleteAuditLogEntryList();

			return makeAuditLogEntries(result.all());
		} catch (Exception e) {
			LOGGER.error("Error while getting audit log entries list", e);
			throw new RepositoryException(
					"Unable to receive complete audit log entry list");
		}
	}

	private String makeEmptyForNull(String input) {
		return input == null ? "" : input;
	}

	private List<AuditLogEntry> makeAuditLogEntries(List<AuditLog> entries) {
		List<AuditLogEntry> auditLogEntries = new ArrayList<>(entries.size());

		for (AuditLog auditLog : entries) {

			// Note time cannot be null since it is required during save
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(auditLog.getTime());

			String userName = makeEmptyForNull(auditLog.getUsername());

			String email = makeEmptyForNull(auditLog.getEmail());
			String firstName = makeEmptyForNull(auditLog.getFirstName());
			String lastName = makeEmptyForNull(auditLog.getLastName());
			String userId = makeEmptyForNull(auditLog.getUserId());

			UserInfo userInfo = UserInfo
					.newInstance(Username.valueOf(userName)).withEmail(email)
					.withFirstName(firstName).withLastName(lastName)
					.withUserId(userId).build();

			// The intent of the multiple try blocks is to get as much info to
			// the client as possible.
			AuditLogAction ala = null;
			try {
				ala = AuditLogAction.valueOf(auditLog.getAction());
			} catch (Exception e) {
				LOGGER.error("Error while creating audit log action: "
						+ auditLog.getAction(), e);
				ala = AuditLogAction.UNSPECIFIED_ACTION;
			}

			Bucket.Label bucketLabel = null;
			try {
				bucketLabel = Bucket.Label.valueOf(auditLog.getBucketLabel());
			} catch (Exception e) {
				LOGGER.error("Error while creating audit log exp label: "
						+ auditLog.getBucketLabel(), e);
			}

			Experiment.Label experimentLabel = null;
			try {
				experimentLabel = Label.valueOf(auditLog.getExperimentLabel());
			} catch (Exception e) {
				LOGGER.error("Error while creating audit log bucket label: "
						+ auditLog.getExperimentLabel(), e);
			}

			Experiment.ID experimentId = null;
			try {
				experimentId = Experiment.ID
						.valueOf(auditLog.getExperimentId());
			} catch (Exception e) {
				LOGGER.error("Error while creating audit log bucket label: "
						+ auditLog.getExperimentId(), e);
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
	 * Retrieves a limited list of AuditLogEntries for all application and
	 * global events.
	 *
	 * @param limit
	 *            the limit
	 * @return a list of those AuditLogEntries
	 */
	@Override
	public List<AuditLogEntry> getCompleteAuditLogEntryList(int limit) {
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
	 * Retrieves the complete list of AuditLogEntries for a specified
	 * application.
	 *
	 * @param applicationName
	 *            the application to select
	 * @return a list of those entries
	 */
	@Override
	public List<AuditLogEntry> getAuditLogEntryList(
			Application.Name applicationName) {

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
	 * Retrieves a limited list of AuditLogEntries for a specified application.
	 *
	 * @param applicationName
	 *            the application to select
	 * @param limit
	 *            the limit
	 * @return a list of those AuditLogEntries
	 */
	@Override
	public List<AuditLogEntry> getAuditLogEntryList(
			Application.Name applicationName, int limit) {
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
	 * Retrieves the complete list of AuditLogEntries for global events.
	 *
	 * @return a list of those entries
	 */
	@Override
	public List<AuditLogEntry> getGlobalAuditLogEntryList() {
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
	 * Retrieves a limited list of AuditLogEntries for global events.
	 *
	 * @param limit
	 *            the limit
	 * @return a list of those AuditLogEntries
	 */
	@Override
	public List<AuditLogEntry> getGlobalAuditLogEntryList(int limit) {
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
	 * Stores an AuditLogEntry into the database. Makes sure that when no
	 * ApplicationName is supplied, the {@link #GLOBAL_ENTRY_APPLICATION} is
	 * used instead.
	 *
	 * @param entry
	 *            the entry to store
	 * @return true on success
	 * @throws RepositoryException
	 *             if the required entry values
	 *             {@link AuditLogEntry#getAction()},
	 *             {@link AuditLogEntry#getUser()},
	 *             {@link AuditLogEntry#getTime()} or the entry itself are null.
	 */
	@Override
	public boolean storeEntry(AuditLogEntry entry) {
		if (entry == null || entry.getAction() == null
				|| entry.getUser() == null || entry.getTime() == null) {
			throw new RepositoryException("Can not insert AuditLogEntry "
					+ entry + " into database, required values are null.");
		}

		String applicationName = AuditLogRepository.GLOBAL_ENTRY_APPLICATION
				.toString();
		if (entry.getApplicationName() != null
				&& ! StringUtils.isBlank(entry.getApplicationName().toString()))
			applicationName = entry.getApplicationName().toString();

		Date time = entry.getTime().getTime();
		String action = entry.getAction().toString();

		String firstName = makeEmptyForNull(entry.getUser().getFirstName());
		String lastName = makeEmptyForNull(entry.getUser().getLastName());
		String email = makeEmptyForNull(entry.getUser().getEmail());
		String userId = makeEmptyForNull(entry.getUser().getUserId());
		String userName = makeEmptyForNull(entry.getUser().getUsername()
				.getUsername());

		UUID experimentId = entry.getExperimentId() == null ? null : entry
				.getExperimentId().getRawID();
		String experimentLabel = entry.getExperimentLabel() == null ? ""
				: entry.getExperimentLabel().toString();
		String bucketLabel = entry.getBucketLabel() == null ? "" : entry
				.getBucketLabel().toString();

		String changedProperty = makeEmptyForNull(entry.getChangedProperty());
		String before = makeEmptyForNull(entry.getBefore());
		String after = makeEmptyForNull(entry.getAfter());

		accessor.storeEntry(applicationName, time, action, firstName, lastName,
				email, userName, userId, experimentId, experimentLabel,
				bucketLabel, changedProperty, before, after);

		return true;

	}

	/**
	 * Allows cql select or update queries which have only an ApplicationName as
	 * a prepared value.
	 *
	 * @param cql
	 *            the query
	 * @param applicationName
	 *            the application to put in
	 * @return the resulting rows.
	 *
	 * @throws RepositoryException
	 *             if an {@link ConnectionException} occurs.
	 */

	/* test */
	// Rows<Application.Name, String> cqlWithApplication(String cql,
	// Application.Name applicationName) {
	// try {
	// OperationResult<CqlResult<Application.Name, String>> result =
	// driver.getKeyspace()
	// .prepareQuery(keyspace.auditlogCF())
	// .withCql(cql)
	// .asPreparedStatement()
	// .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
	// .execute();
	// return result.getResult().getRows();
	// } catch (ConnectionException e) {
	// throw new RepositoryException(
	// String.format("Can not retrieve auditlog for application %s.",
	// applicationName),
	// e
	// );
	// }
	// }

	/**
	 * Allows cql select or update queries which have no prepared values.
	 *
	 * @param cql
	 *            the query
	 * @return the resulting rows.
	 *
	 * @throws RepositoryException
	 *             if an {@link ConnectionException} occurs.
	 */
	/* test */
	// Rows<Application.Name, String> cqlSelectAll(String cql) {
	// try {
	// OperationResult<CqlResult<Application.Name, String>> result =
	// driver.getKeyspace()
	// .prepareQuery(keyspace.auditlogCF())
	// .withCql(cql)
	// .asPreparedStatement()
	// .execute();
	// return result.getResult().getRows();
	// } catch (ConnectionException e) {
	// throw new RepositoryException("Can not retrieve complete auditlog.", e);
	// }
	// }

	/**
	 * Reads {@link Rows} (for example from
	 * {@link #cqlWithApplication(String, Application.Name)}) and returns the
	 * contained list of audit log entries. <br />
	 * If the rowKey is null, the key will be read from the query.
	 *
	 * @param rowKey
	 *            the key used to query cassandra
	 * @param rows
	 *            the rows object
	 * @return a list of AuditLogEntries
	 */
	/* test */
	// List<AuditLogEntry> readAuditLogEntryList(Application.Name rowKey,
	// Rows<Application.Name, String> rows) {
	// List<AuditLogEntry> auditLogEntries = new ArrayList<>();
	// for (Row<Application.Name, String> row : rows) {
	// AuditLogEntry auditLogEntry;
	// if ((auditLogEntry = readAuditLogEntry(rowKey == null ? row.getKey() :
	// rowKey, row.getColumns())) != null) {
	// auditLogEntries.add(auditLogEntry);
	// }
	// }
	// return auditLogEntries;
	// }

	/**
	 * Reads an AuditLogEntry from a ColumnList.
	 *
	 * @param applicationName
	 *            the application name / row key
	 * @param columnList
	 *            the columnList
	 * @return the auditLogEntry
	 */
	/* test */
	// AuditLogEntry readAuditLogEntry(Application.Name applicationName,
	// ColumnList<String> columnList) {
	// if (columnList == null) {
	// return null;
	// }
	// try {
	// /// application name
	// Application.Name appName =
	// AuditLogRepository.GLOBAL_ENTRY_APPLICATION.equals(applicationName) ?
	// null : applicationName;
	//
	// // deserialize time
	// Calendar time = null;
	// Date date;
	// if ((date = columnList.getDateValue("time", null)) != null) {
	// time = Calendar.getInstance();
	// time.setTime(date);
	// }
	//
	// // deserialize user
	// String username = columnList.getStringValue("user_username", "");
	// String firstname = columnList.getStringValue("user_firstname", "");
	// String lastname = columnList.getStringValue("user_lastname", "");
	// String email = columnList.getStringValue("user_email", "");
	// String userid = columnList.getStringValue("user_userid", "");
	// UserInfo user = null;
	// if (!StringUtils.isBlank(username) || !StringUtils.isBlank(userid)) {
	// user = (username != null ?
	// UserInfo.from(UserInfo.Username.valueOf(username))
	// : UserInfo.from(UserInfo.Username.valueOf(userid)))
	// .withFirstName(StringUtils.isBlank(firstname) ? null : firstname)
	// .withLastName(StringUtils.isBlank(lastname) ? null : lastname)
	// .withUserId(StringUtils.isBlank(userid) ? null : userid)
	// .withEmail(StringUtils.isBlank(email) ? null : email)
	// .build();
	// }
	//
	// // deserialize action
	// String actionStr = columnList.getStringValue("action",
	// AuditLogAction.UNSPECIFIED_ACTION.name());
	// AuditLogAction action;
	// //TODO: remove this try catch block.
	// try {
	// action = AuditLogAction.valueOf(actionStr);
	// } catch (IllegalArgumentException e) {
	// LOGGER.debug(String.format("Unknown action %s setting it to default.",
	// actionStr), e);
	// action = AuditLogAction.UNSPECIFIED_ACTION;
	// }
	//
	// // deserialize experiment properties
	// Experiment.Label experimentLabel =
	// columnList.getValue("experiment_label", ExperimentLabelSerializer.get(),
	// null);
	// Experiment.ID experimentId = columnList.getValue("experiment_id",
	// ExperimentIDSerializer.get(), null);
	//
	// // deserialize bucket properties
	// Bucket.Label bucketLabel = columnList.getValue("bucket_label",
	// BucketLabelSerializer.get(), null);
	//
	// // deserialize property changes
	// String changedProperty = columnList.getStringValue("changed_property",
	// "");
	// changedProperty = StringUtils.isBlank(changedProperty) ? null :
	// changedProperty;
	// String propertyBefore = columnList.getStringValue("property_before", "");
	// propertyBefore = StringUtils.isBlank(propertyBefore) ? null :
	// propertyBefore;
	// String propertyAfter = columnList.getStringValue("property_after", "");
	// propertyAfter = StringUtils.isBlank(propertyAfter) ? null :
	// propertyAfter;
	//
	// return new AuditLogEntry(time, user, action, appName,
	// experimentLabel, experimentId, bucketLabel,
	// changedProperty, propertyBefore, propertyAfter);
	// } catch (IllegalArgumentException e) {
	// LOGGER.warn("Can not read AuditLogEntry, data in auditlog column family might be corrupted for key '"
	// + applicationName + "': ", e);
	// }
	// return null;
	// }

}
