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
package com.intuit.wasabi.repository.impl.cassandra;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketAssignmentCount;
import com.intuit.wasabi.analyticsobjects.counts.TotalUsers;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.DateHour;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.ExperimentDriver;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.Constants;
import com.intuit.wasabi.repository.DatabaseRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ApplicationNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.BucketLabelSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.UserIDSerializer;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.PreparedCqlQuery;
import com.netflix.astyanax.serializers.DateSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;
import org.slf4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cassandra implementation of AssignmentRepository
 *
 * @see AssignmentsRepository
 */
public class CassandraAssignmentsRepository implements AssignmentsRepository {

    private static final Logger LOG = getLogger(CassandraAssignmentsRepository.class);
    private final CassandraDriver driver;
    private final ExperimentsKeyspace keyspace;
    private final ExperimentRepository experimentRepository;
    private final AssignmentsRepository assignmentsRepository;
    private final ExperimentRepository dbRepository;
    private final EventLog eventLog;
    private final Boolean assignUserToExport;
    private final Boolean assignBucketCount;
    private final String defaultTimeFormat;
    private LinkedBlockingQueue assignmentsCountQueue = new LinkedBlockingQueue<>();
    private int assignmentsCountThreadPoolSize;
    private ThreadPoolExecutor assignmentsCountExecutor;
    private boolean assignUserToOld;
    private boolean assignUserToNew;

    @Inject
    public CassandraAssignmentsRepository(@CassandraRepository ExperimentRepository experimentRepository,
                                          @DatabaseRepository ExperimentRepository dbRepository,
                                          AssignmentsRepository assignmentsRepository,
                                          @ExperimentDriver CassandraDriver driver, ExperimentsKeyspace keyspace,
                                          EventLog eventLog, final @Named("export.pool.size") Integer assignmentsCountThreadPoolSize,
                                          final @Named("assign.user.to.old") Boolean assignUserToOld,
                                          final @Named("assign.user.to.new") Boolean assignUserToNew,
                                          final @Named("assign.user.to.export") Boolean assignUserToExport,
                                          final @Named("assign.bucket.count") Boolean assignBucketCount,
                                          final @Named("default.time.format") String defaultTimeFormat)
            throws IOException, ConnectionException {
        super();

        this.experimentRepository = experimentRepository;
        this.driver = driver;
        this.keyspace = keyspace;
        this.dbRepository = dbRepository;
        this.assignmentsRepository = assignmentsRepository;
        this.eventLog = eventLog;
        this.assignmentsCountThreadPoolSize = assignmentsCountThreadPoolSize;
        this.assignUserToOld = assignUserToOld;
        this.assignUserToNew = assignUserToNew;
        this.assignUserToExport = assignUserToExport;
        this.assignBucketCount = assignBucketCount;
        this.defaultTimeFormat = defaultTimeFormat;

        assignmentsCountExecutor = (ThreadPoolExecutor) new ThreadPoolExecutor(assignmentsCountThreadPoolSize,
                assignmentsCountThreadPoolSize, 0L, MILLISECONDS, assignmentsCountQueue);
    }

    @Override
    @Timed
    public Set<Experiment.ID> getUserAssignments(User.ID userID, Application.Name appLabel, Context context) {

        final String CQL = "select * from experiment_user_index " +
                "where user_id = ? and app_name = ? and context = ?";

        try {
            Rows<User.ID, String> rows =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.experimentUserIndexCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UserIDSerializer.get())
                            .withByteBufferValue(appLabel, ApplicationNameSerializer.get())
                            .withStringValue(context.getContext())
                            .execute()
                            .getResult()
                            .getRows();

            Set<Experiment.ID> result = new HashSet<>();

            //return all experiments to which the user is assigned for which the
            // assignment is not to bucket null
            if (!rows.isEmpty()) {
                for (int i = 0; i < rows.size(); i++) {
                    ColumnList<String> columns = rows.getRowByIndex(i).getColumns();
                    //check that the bucket assignment is not null
                    if (columns.getStringValue("bucket", null) != null) {
                        result.add(Experiment.ID.valueOf(columns.getUUIDValue("experiment_id", null)));
                    }
                }
            }

            return result;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve assignment for " +
                    "experimentID = \"" + appLabel + "\" userID = \"" +
                    userID + "\"", e);
        }
    }


    /**
     * Assigns the user to the specified bucket. Note, there is no validation
     * that the experiment is running, is not deleted, or anything else. This
     * method should only be used for cases where a user has been pre-assigned
     * to a bucket.
     *
     * @param assignment The assignment specification
     * @param experiment the experiment object
     * @param date       date
     */
    @Override
    @Timed
    public Assignment assignUser(Assignment assignment, Experiment experiment, Date date) {
        Assignment new_assignment = null;

        if (assignUserToOld) {
            //Writing assignment to the old table - user_assignment
            new_assignment = assignUserToOld(assignment, date);
        }
        if (assignUserToNew) {
            //Writing assignment to the new table - user_assignment_look_up
            new_assignment = assignUserToLookUp(assignment, date);
        }

        //Updating the assignment bucket counts, user_assignment_export
        // in a asynchronous AssignmentCountEnvelope thread
        assignmentsCountExecutor.execute(new AssignmentCountEnvelope(assignmentsRepository, experimentRepository,
                dbRepository, experiment, assignment, true, eventLog, date, assignUserToExport, assignBucketCount));

        indexUserToExperiment(assignment);
        indexUserToBucket(assignment);
        indexExperimentsToUser(assignment);

        return new_assignment;
    }

    /**
     * Adds an assignment associated with a new user
     *
     * @param assignment assignment of the new user
     * @param date       date
     * @return user's assignment
     */
    @Timed
    protected Assignment assignUserToLookUp(Assignment assignment, Date date) {
        final String CQL;
        Date paramDate = date;
        if (paramDate == null) {
            paramDate = new Date();
        }
        if (assignment.getBucketLabel() != null) {
            CQL = "insert into user_assignment_look_up " +
                    "(experiment_id, user_id, context, created, bucket_label) " +
                    "values (?, ?, ?, ?, ?)";
        } else {
            CQL = "insert into user_assignment_look_up " +
                    "(experiment_id, user_id, context, created) " +
                    "values (?, ?, ?, ?)";
        }
        try {

            PreparedCqlQuery<User.ID, String> query =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userAssignmentLookUp())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(assignment.getExperimentID(), ExperimentIDSerializer.get())
                            .withByteBufferValue(assignment.getUserID(), UserIDSerializer.get())
                            .withStringValue(assignment.getContext().getContext())
                            .withByteBufferValue(paramDate, DateSerializer.get());
            if (assignment.getBucketLabel() != null) {
                query.withByteBufferValue(assignment.getBucketLabel(), BucketLabelSerializer.get());
            }
            query.execute();
            return (Assignment.newInstance(assignment.getExperimentID())
                    .withBucketLabel(assignment.getBucketLabel())
                    .withUserID(assignment.getUserID())
                    .withContext(assignment.getContext())
                    .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                    .withCreated(paramDate)
                    .withCacheable(null)
                    .build());
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not save user assignment \"" +
                    assignment + "\"", e);
        }
    }

    //    Adding users to the old user assignment table. This is a fail safe.
    @Override
    @Timed
    public Assignment assignUserToOld(Assignment assignment, Date date) {
        final String CQL;

        if (assignment.getBucketLabel() != null) {
            CQL = "insert into user_assignment " +
                    "(experiment_id, user_id, context, created, bucket_label) " +
                    "values (?, ?, ?, ?, ?)";
        } else {
            CQL = "insert into user_assignment " +
                    "(experiment_id, user_id, context, created) " +
                    "values (?, ?, ?, ?)";
        }

        try {
            PreparedCqlQuery<ExperimentsKeyspace.UserAssignmentComposite, String> query =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userAssignmentCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(assignment.getExperimentID(), ExperimentIDSerializer.get())
                            .withByteBufferValue(assignment.getUserID(), UserIDSerializer.get())
                            .withStringValue(assignment.getContext().getContext())
                            .withByteBufferValue(date, DateSerializer.get());

            if (assignment.getBucketLabel() != null) {
                query.withByteBufferValue(assignment.getBucketLabel(), BucketLabelSerializer.get());
            }

            query.execute();
            return (Assignment.newInstance(assignment.getExperimentID())
                    .withBucketLabel(assignment.getBucketLabel())
                    .withUserID(assignment.getUserID())
                    .withContext(assignment.getContext())
                    .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                    .withCreated(date)
                    .withCacheable(null)
                    .build());

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not save to the old table user assignment \"" +
                    assignment + "\"", e);
        }
    }

    /**
     * Deletes the existing Assignment of a User in an Experiment.
     *
     * @param experimentID experiment id
     * @param userID       user id
     * @param context      context
     */
    protected void deleteUserFromLookUp(Experiment.ID experimentID, User.ID userID, Context context) {
        final String CQL = "delete from user_assignment_look_up where user_id = ? and context = ? and experiment_id = ?";
        try {
            PreparedCqlQuery<ExperimentsKeyspace.UserAssignmentComposite, String> query =
                    driver.getKeyspace().prepareQuery(keyspace.userAssignmentCF()).withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UserIDSerializer.get())
                            .withStringValue(context.getContext())
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get());
            query.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not delete user assignment for Experiment:" + experimentID +
                    " and User " + userID, e);
        }
    }

    /**
     * This column family is meant to be used for exporting user assignments
     *
     * @param assignment assignment
     * @param date       date
     */
    @Override
    @Timed
    public void assignUserToExports(Assignment assignment, Date date) {
        final String CQL;
        CQL = "insert into user_assignment_export " +
                "(experiment_id, user_id, context, created, day_hour, bucket_label, is_bucket_null) " +
                "values (?, ?, ?, ?, ?, ?, ?)";
        try {
            DateHour dateHour = new DateHour();
            dateHour.setDateHour(date);
            Date day_hour = dateHour.getDayHour();
            PreparedCqlQuery<ExperimentsKeyspace.ExperimentIDDayHourComposite, String> query =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userAssignmentExport())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(assignment.getExperimentID().getRawID(), UUIDSerializer.get())
                            .withByteBufferValue(assignment.getUserID(), UserIDSerializer.get())
                            .withStringValue(assignment.getContext().getContext())
                            .withByteBufferValue(date, DateSerializer.get())
                            .withByteBufferValue(day_hour, DateSerializer.get());
            if (assignment.getBucketLabel() != null) {
                query.withByteBufferValue(assignment.getBucketLabel(), BucketLabelSerializer.get());
                query.withBooleanValue(false);
            } else {
                query.withStringValue("NO_ASSIGNMENT");
                query.withBooleanValue(true);
            }
            query.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not save user assignment in user_assignment_export \"" +
                    assignment + "\"", e);
        }
    }


    //add to cassandra table a user-indexed table of experiments for mutual exclusion
    @Timed
    private void indexExperimentsToUser(Assignment assignment) {

        String CQL = (assignment.getBucketLabel() != null)

                ? "insert into experiment_user_index " +
                "(user_id, context, app_name, experiment_id, bucket) " +
                "values (?, ?, ?, ?, ?)"
                : "insert into experiment_user_index " +
                "(user_id, context, app_name, experiment_id) " +
                "values (?, ?, ?, ?)";

        try {

            PreparedCqlQuery<User.ID, String> query =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.experimentUserIndexCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(assignment.getUserID(), UserIDSerializer.get())
                            .withStringValue(assignment.getContext().getContext())
                            .withByteBufferValue(assignment.getApplicationName(), ApplicationNameSerializer.get())
                            .withByteBufferValue(assignment.getExperimentID(), ExperimentIDSerializer.get());
            if (assignment.getBucketLabel() != null) {
                query.withByteBufferValue(assignment.getBucketLabel(), BucketLabelSerializer.get());
            }
            query.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not index experiment to user \"" + assignment + "\"", e);
        }
    }

    @Timed
    private void indexUserToExperiment(Assignment assignment) {

        final String CQL = "insert into user_experiment_index " +
                "(app_name, user_id, context, experiment_id, bucket_label) " +
                "values (?, ?, ?, ?, ?)";
        try {
            PreparedCqlQuery<Application.Name, String> query =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userExperimentIndexCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(assignment.getApplicationName(), ApplicationNameSerializer.get())
                            .withByteBufferValue(assignment.getUserID(), UserIDSerializer.get())
                            .withStringValue(assignment.getContext().getContext())
                            .withByteBufferValue(assignment.getExperimentID(), ExperimentIDSerializer.get());


            if (assignment.getBucketLabel() != null) {
                query.withByteBufferValue(assignment.getBucketLabel(), BucketLabelSerializer.get());
            } else {
                query.withValue(ByteBuffer.wrap(new byte[0]));
            }

            query.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException(
                    "Could not index user to experiment \"" + assignment + "\"", e);
        }
    }

    @Timed
    private void indexUserToBucket(Assignment assignment) {

        final String CQL = "insert into user_bucket_index " +
                "(experiment_id, user_id, context, assigned, bucket_label) " +
                "values (?, ?, ?, ?, ?)";

        try {
            final Date NOW = new Date();

            PreparedCqlQuery<Application.Name, String> query =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userExperimentIndexCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(assignment.getExperimentID(), ExperimentIDSerializer.get())
                            .withByteBufferValue(assignment.getUserID(), UserIDSerializer.get())
                            .withStringValue(assignment.getContext().getContext())
                            .withByteBufferValue(NOW, DateSerializer.get());


            if (assignment.getBucketLabel() != null) {
                query.withByteBufferValue(assignment.getBucketLabel(), BucketLabelSerializer.get());
            } else {
                query.withValue(ByteBuffer.wrap(new byte[0]));
            }

            query.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException(
                    "Could not index user to bucket \"" + assignment + "\"", e);
        }
    }

    /**
     * Return a Map of experimentID's and assigned buckets for the a user across all the experiments for which a
     * valid assignment (null or non-null) was delivered
     */
    @Override
    @Timed
    public Table<Experiment.ID, Experiment.Label, String> getAssignments(User.ID userID, Application.Name appLabel,
                                                                         Context context,
                                                                         Table<Experiment.ID, Experiment.Label,
                                                                                 Experiment> allExperiments) {

        final String CQL = "select * from experiment_user_index " +
                "where user_id = ? and app_name = ? and context = ?";

        try {
            Rows<User.ID, String> rows =
                    driver.getKeyspace().prepareQuery(keyspace.experimentUserIndexCF()).withCql(CQL).asPreparedStatement()
                            .withByteBufferValue(userID, UserIDSerializer.get())
                            .withByteBufferValue(appLabel, ApplicationNameSerializer.get())
                            .withStringValue(context.getContext())
                            .execute().getResult().getRows();
            Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
            if (!rows.isEmpty()) {
                for (int i = 0; i < rows.size(); i++) {
                    ColumnList<String> columns = rows.getRowByIndex(i).getColumns();
                    Experiment.ID experimentID = Experiment.ID.valueOf(columns.getUUIDValue("experiment_id", null));
                    Iterator<Experiment> iterator = allExperiments.row(experimentID).values().iterator();
                    if (iterator.hasNext()) {
                        result.put(experimentID,
                                iterator.next().getLabel(),
                                columns.getStringValue("bucket", "null"));
                    }
                }
            }
            return result;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve assignments for " +
                    "application = \"" + appLabel + "\" userID = \"" +
                    userID + "\" and context " + context.getContext(), e);
        }
    }

    @Timed
    protected Assignment getAssignmentFromLookUp(Experiment.ID experimentID, User.ID userID, Context context) {

        final String CQL =
                "select * from user_assignment_look_up " +
                        "where experiment_id = ? and user_id = ? and context = ?";

        try {
            Rows<User.ID, String> rows =
                    driver.getKeyspace().prepareQuery(keyspace.userAssignmentLookUp()).withCql(CQL).asPreparedStatement()
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                            .withByteBufferValue(userID, UserIDSerializer.get())
                            .withStringValue(context.getContext())
                            .execute().getResult().getRows();

            Assignment result = null;

            if (!rows.isEmpty()) {
                assert rows.size() <= 1 : "More than a single row returned";

                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                Experiment.ID experimentIDdb = Experiment.ID.valueOf(columns.getUUIDValue("experiment_id", null));
                User.ID userIDdb = User.ID.valueOf(columns.getStringValue("user_id", null));
                Date created = Preconditions.checkNotNull(columns.getDateValue("created", null));
                Context contextdb = Context.valueOf(columns.getStringValue("context", null));
                String rawBucketLabel = columns.getStringValue("bucket_label", null);
                Bucket.Label bucketLabel = (rawBucketLabel != null)
                        ? Bucket.Label.valueOf(rawBucketLabel)
                        : null;

                /*
                * The requirement for EMPTY buckets is to make the existing AND future assignments null.
                * It is also a requirement to preserve the list of the users assigned to a bucket which has been designated
                * as EMPTY bucket. i.e. We should be able to retrieve the list of users who have been assigned to a bucket
                * before its state was made EMPTY.
                * We will be able to do that if we do not update the existing user assignments in the user_assignment
                * and user_bucket_index  column families.
                * However, the below hack will return a null assignment  even though the existing assignment
                * for a user who had been assigned to an EMPTY bucket is not null
                * */
                boolean bucketEmpty = experimentRepository.getBucket(experimentID, bucketLabel).getState().equals(Bucket.State.EMPTY);
                if (bucketLabel != null &&
                        bucketEmpty) {
                    bucketLabel = null;
                }
                result = Assignment.newInstance(experimentIDdb)
                        .withBucketLabel(bucketLabel)
                        .withUserID(userIDdb)
                        .withContext(contextdb)
                        .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                        .withCreated(created)
                        .withCacheable(false)
                        .withBucketEmpty(bucketEmpty)
                        .build();
                LOG.info("Assignment for experiment %s for user %s on context %s is of bucket %s", experimentID, userID, context, bucketLabel);
            }

            return result;

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve assignment for " +
                    "experimentID = \"" + experimentID + "\" userID = \"" + userID + "\"", e);
        }
    }

    //TODO: After migration getAssignmentOld should be deleted
    @Timed
    protected Assignment getAssignmentOld(Experiment.ID experimentID, User.ID userID, Context context) {
        final String CQL =
                "select * from user_assignment " +
                        "where experiment_id = ? and user_id = ? and context = ?";

        try {
            Rows<ExperimentsKeyspace.UserAssignmentComposite, String> rows =
                    driver.getKeyspace().prepareQuery(keyspace.userAssignmentCF()).withCql(CQL).asPreparedStatement()
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                            .withByteBufferValue(userID, UserIDSerializer.get())
                            .withStringValue(context.getContext())
                            .execute().getResult().getRows();

            Assignment result = null;

            if (!rows.isEmpty()) {
                assert rows.size() <= 1 : "More than a single row returned";

                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                Experiment.ID experimentIDdb = Experiment.ID.valueOf(columns.getUUIDValue("experiment_id", null));
                User.ID userIDdb = User.ID.valueOf(columns.getStringValue("user_id", null));
                Date created = Preconditions.checkNotNull(columns.getDateValue("created", null));
                Context contextdb = Context.valueOf(columns.getStringValue("context", null));
                String rawBucketLabel = columns.getStringValue("bucket_label", null);
                Bucket.Label bucketLabel = (rawBucketLabel != null)
                        ? Bucket.Label.valueOf(rawBucketLabel)
                        : null;

                /*
                * The requirement for EMPTY buckets is to make the existing AND future assignments null.
                * It is also a requirement to preserve the list of the users assigned to a bucket which has been designated
                * as EMPTY bucket. i.e. We should be able to retrieve the list of users who have been assigned to a bucket
                * before its state was made EMPTY.
                * We will be able to do that if we do not update the existing user assignments in the user_assignment
                * and user_bucket_index  column families.
                * However, the below hack will return a null assignment  even though the existing assignment
                * for a user who had been assigned to an EMPTY bucket is not null
                * */
                boolean isBucketEmpty = false;
                if (bucketLabel != null &&
                        experimentRepository.getBucket(experimentID, bucketLabel).getState().equals(Bucket.State.EMPTY)) {
                    bucketLabel = null;
                    isBucketEmpty = true;
                }

                result = Assignment.newInstance(experimentIDdb)
                        .withBucketLabel(bucketLabel)
                        .withUserID(userIDdb)
                        .withContext(contextdb)
                        .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                        .withCreated(created)
                        .withCacheable(false)
                        .withBucketEmpty(isBucketEmpty)
                        .build();

                LOG.info("CassandraAssignmentsRepository got assignment  " + result);
            }

            return result;

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve assignment for " +
                    "experimentID = \"" + experimentID + "\" userID = \"" + userID + "\"", e);
        }
    }

    @Timed
    @Override
    public Assignment getAssignment(Experiment.ID experimentID, User.ID userID, Context context) {
        Assignment result = null;
        if (assignUserToNew) {
            //check for the assignment data in new - user_assignment_look_up table
            result = getAssignmentFromLookUp(experimentID, userID, context);
        }
        //if it is not present in the user_assignment_look_up table and old flag is set to tru, then check for the data in user_assignment table
        if (assignUserToOld && (result == null)) {
            result = getAssignmentOld(experimentID, userID, context);
        }
        return result;
    }


    /**
     * Deletes the existing Assignment between a User and an Experiment.
     */
    @Override
    @Timed
    public void deleteAssignment(Experiment experiment, User.ID userID, Context context, Application.Name appName,
                                 Assignment currentAssignment) {
        // Deletes the assignment data across all the relevant tables in a consistent manner
        deleteUserFromLookUp(experiment.getID(), userID, context);
        //Updating the assignment bucket counts by -1 in a asynchronous AssignmentCountEnvelope thread
        // false to subtract 1 from the count for the bucket
        assignmentsCountExecutor.execute(new AssignmentCountEnvelope(assignmentsRepository, experimentRepository,
                dbRepository, experiment, currentAssignment, false, eventLog, null, assignUserToExport,
                assignBucketCount));
        deleteAssignmentOld(experiment.getID(), userID, context, appName, currentAssignment.getBucketLabel());
        removeIndexUserToExperiment(userID, experiment.getID(), context, appName);
        removeIndexUserToBucket(userID, experiment.getID(), context, currentAssignment.getBucketLabel());
        removeIndexExperimentsToUser(userID, experiment.getID(), context, appName);
    }

    /**
     * Deletes the existing Assignment between a User and an Experiment.
     *
     * @param experimentID experiment id
     * @param userID       user id
     * @param context      context
     * @param appName      application name
     * @param bucketLabel  bucket label
     */
    //TODO: After migration deleteAssignment should be deleted
    protected void deleteAssignmentOld(Experiment.ID experimentID, User.ID userID, Context context, Application.Name appName,
                                       Bucket.Label bucketLabel) {
        final String CQL = "delete from user_assignment where experiment_id = ? and user_id = ? and context = ?";

        try {
            PreparedCqlQuery<ExperimentsKeyspace.UserAssignmentComposite, String> query =
                    driver.getKeyspace().prepareQuery(keyspace.userAssignmentCF()).withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                            .withByteBufferValue(userID, UserIDSerializer.get())
                            .withStringValue(context.getContext());
            query.execute();

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not delete user assignment for Experiment:" + experimentID +
                    " and User " + userID, e);
        }
    }


    /**
     * Removes the referenced pair from the user_experiment_index.
     *
     * @param userID       user id
     * @param experimentID experiment id
     * @param context      context
     * @param appName      application name
     */
    public void removeIndexUserToExperiment(User.ID userID, Experiment.ID experimentID, Context context,
                                            Application.Name appName) {
        final String CQL = "delete from user_experiment_index " +
                "where user_id = ? and experiment_id = ? and context = ? and app_name = ?";

        try {
            PreparedCqlQuery<Application.Name, String> query =
                    driver.getKeyspace().prepareQuery(keyspace.userExperimentIndexCF()).withCql(CQL).asPreparedStatement()
                            .withByteBufferValue(userID, UserIDSerializer.get()) // user_id
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get()) // experiment_id
                            .withStringValue(context.getContext()) //context
                            .withByteBufferValue(appName, ApplicationNameSerializer.get()); //appName
            query.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException(
                    "Could not remove index from user_experiment for user: " + userID + " to experiment: " + experimentID, e);
        }
    }

    /**
     * Removes the referenced pair from the user_bucket_index.
     */
    @Override
    @Timed
    public void removeIndexUserToBucket(User.ID userID, Experiment.ID experimentID, Context context,
                                        Bucket.Label bucketLabel) {
        final String CQL = "delete from user_bucket_index " +
                "where experiment_id = ? and user_id = ? and context = ? and bucket_label = ?";

        try {
            PreparedCqlQuery<Application.Name, String> query =
                    driver.getKeyspace().prepareQuery(keyspace.userExperimentIndexCF()).withCql(CQL).asPreparedStatement()
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                            .withByteBufferValue(userID, UserIDSerializer.get())
                            .withStringValue(context.getContext())
                            .withByteBufferValue(bucketLabel, BucketLabelSerializer.get());
            query.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException(
                    "Could not delete index from user_bucket for user: " + userID + "to experiment: " + experimentID, e);
        }
    }


    @Override
    public StreamingOutput getAssignmentStream(final Experiment.ID experimentID, final Context context,
                                               final Parameters parameters, final Boolean ignoreNullBucket) {
        final Experiment id = experimentRepository.getExperiment(experimentID);
        if (id == null) {
            throw new ExperimentNotFoundException(experimentID);
        }
        Date from_ts = parameters.getFromTime();
        Date to_ts = parameters.getToTime();
        Timestamp from_ts_new;
        if (from_ts != null) {
            from_ts_new = new Timestamp(from_ts.getTime());
        } else {
            from_ts = id.getCreationTime(); //Sets the default lower limit time of download to experiment creation time
            from_ts_new = new Timestamp(from_ts.getTime());
        }

        Timestamp to_ts_new;
        if (to_ts != null) {
            to_ts_new = new Timestamp(to_ts.getTime());
        } else {
            to_ts = new Date(); // Sets the default upper limit time of download to current time stamp
            to_ts_new = new Timestamp(to_ts.getTime());

        }

        // Fetches the relevant partitions for a given time window where the user assignments data resides.
        final List<DateHour> dateHours = getUserAssignmentPartitions(from_ts_new, to_ts_new);
        final String CQL;

        if (ignoreNullBucket == false) {
            CQL =
                    "select * from user_assignment_export " +
                            "where experiment_id = ? and day_hour = ? and context = ?";
        } else {
            CQL =
                    "select * from user_assignment_export " +
                            "where experiment_id = ? and day_hour = ? and context = ? and is_bucket_null = false";
        }

        return new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                Writer writer = new BufferedWriter(new OutputStreamWriter(os, Constants.DEFAULT_CHAR_SET));

                String header = "experiment_id" + "\t" +
                        "user_id" + "\t" +
                        "context" + "\t" +
                        "bucket_label" + "\t" +
                        "created" + "\t" +
                        System.getProperty("line.separator");
                writer.write(header);
                String output;
                DateFormat formatter = new SimpleDateFormat(defaultTimeFormat);

                formatter.setTimeZone(parameters.getTimeZone());
                try {
                    // Iterating through the different partitions across which the data resides
                    for (int ipart = 0; ipart < dateHours.size(); ipart = ipart + 1) {
                        Rows<ExperimentsKeyspace.ExperimentIDDayHourComposite, String> data =
                                driver.getKeyspace()
                                        .prepareQuery(keyspace.userAssignmentExport())
                                        .withCql(CQL)
                                        .asPreparedStatement()
                                        // experiment_id
                                        .withByteBufferValue(
                                                experimentID,
                                                ExperimentIDSerializer.get())
                                        //day_hour
                                        .withByteBufferValue(dateHours.get(ipart).getDayHour(), DateSerializer.get())
                                        //context
                                        .withStringValue(context.getContext())
                                        .execute()
                                        .getResult()
                                        .getRows();

                        for (int index = 0; index < data.size(); index = index + 1) {
                            ColumnList<String> columns = data.getRowByIndex(index).getColumns();
                            String id = String.valueOf(columns.getUUIDValue("experiment_id", null));
                            String user = columns.getStringValue("user_id", null);
                            String context_string = columns.getStringValue("context", null);
                            String bucket_label = columns.getStringValue("bucket_label", null);
                            Date date = columns.getDateValue("created", null);
                            final Date converted_date = new Timestamp(date.getTime());
                            String created = formatter.format(converted_date);
                            output = id + '\t' + user + '\t' + context_string + '\t' + bucket_label + '\t' + created
                                    + System.getProperty("line.separator");
                            writer.write(output);
                        }

                    }
                } catch (ConnectionException e) {
                    throw new RepositoryException("Could not retrieve assignment for " +
                            "experimentID = \"" + experimentID, e);
                } finally {
                    writer.close();
                }

            }
        };
    }


    /**
     * Removes the referenced pair from the experiment_user_index.
     *
     * @param userID       user id
     * @param experimentID experiment id
     * @param context      context
     * @param appName      application name
     */
    public void removeIndexExperimentsToUser(User.ID userID, Experiment.ID experimentID, Context context,
                                             Application.Name appName) {
        String CQL = "delete from experiment_user_index " +
                "where user_id = ? and experiment_id = ? and context = ? and app_name = ?";

        try {

            PreparedCqlQuery<User.ID, String> query =
                    driver.getKeyspace().prepareQuery(keyspace.experimentUserIndexCF()).withCql(CQL).asPreparedStatement()
                            .withByteBufferValue(userID, UserIDSerializer.get()) // user_id
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get())  // experiment_id
                            .withStringValue(context.getContext())   //context
                            .withByteBufferValue(appName, ApplicationNameSerializer.get());   //appName
            query.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException(
                    "Could not delete index from experiment_user_index for user: " + userID + "to experiment: " +
                            experimentID, e);
        }
    }

    // Adds n_hours to a given time stamp
    public Date addHoursMinutes(Date date, int n_hours, int n_minutes) {
        // creates calendar
        Calendar cal = Calendar.getInstance();
        // sets calendar time/date
        cal.setTime(date);
        // adds n_hours hours
        cal.add(Calendar.HOUR_OF_DAY, n_hours);
        cal.add(Calendar.MINUTE, n_minutes);
        // returns new date object, n_hours hour in the future
        return cal.getTime();
    }


    // Fetches all the partition for a given time window
    public List<DateHour> getUserAssignmentPartitions(Date from_time, Date to_time) {
        int ONE_HOUR = 1;
        int ZERO_MINUTES = 0;
        List<DateHour> dateHours = new ArrayList<DateHour>();
        // Sanity check
        if (from_time.after(to_time)) {
            return dateHours;
        }
        Date time_upper_limit = addHoursMinutes(to_time, ONE_HOUR, ZERO_MINUTES);
        //initializing the iteration time
        Date iteration_time = from_time;
        while (iteration_time.before(time_upper_limit)) {
            DateHour dateHour = new DateHour();
            dateHour.setDateHour(iteration_time);
            dateHours.add(dateHour);
            iteration_time = addHoursMinutes(iteration_time, ONE_HOUR, ZERO_MINUTES);
        }
        return dateHours;
    }

    @Override
    @Timed
    public void pushAssignmentToStaging(String exception, String data) {
        String cql = "insert into staging(time, type, exep , msg) values(now(), 'ASSIGNMENT', ? , ?)";
        try {
            driver.getKeyspace().prepareQuery(keyspace.stagingCF()).withCql(cql).asPreparedStatement()
                    .withByteBufferValue(exception, StringSerializer.get())
                    .withByteBufferValue(data, StringSerializer.get()).execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not push the assignment to staging", e);
        }

    }

    /**
     * Updates the assignment count of an experiment on a per bucket basis
     *
     * @param experiment Experiment
     * @param assignment Assignment
     * @param countUp    boolean, if true increment by 1, else decrement by 1
     */
    @Override
    @Timed
    public void updateBucketAssignmentCount(Experiment experiment, Assignment assignment, boolean countUp) {
        Bucket.Label bucketLabel = assignment.getBucketLabel();
        Bucket.Label bucketLabel1 = null;
        String CQL;
        bucketLabel1 = (bucketLabel == null) ? Bucket.Label.valueOf("NULL") : bucketLabel;
        if (countUp) {
            CQL = "UPDATE bucket_assignment_counts SET bucket_assignment_count = bucket_assignment_count + 1 " +
                    "WHERE experiment_id =? and bucket_label = ?";
        } else {
            CQL = "UPDATE bucket_assignment_counts SET bucket_assignment_count = bucket_assignment_count - 1 " +
                    "WHERE experiment_id =? and bucket_label = ?";
        }
        try {
            driver.getKeyspace()
                    .prepareQuery(keyspace.bucketAssignmentCountsCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(experiment.getID(), ExperimentIDSerializer.get())
                    .withByteBufferValue(bucketLabel1, BucketLabelSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not update the bucket count for experiment " + experiment.getID()
                    + " bucket " + bucketLabel1.toString(), e);
        }
    }

    /**
     * Fetches the bucket assignment count associated with an experiment both per bucket and the total
     *
     * @param experiment experiment
     * @return assignment counts
     */
    @Override
    @Timed
    public AssignmentCounts getBucketAssignmentCount(Experiment experiment) {

        AssignmentCounts assignmentCounts;

        List<BucketAssignmentCount> bucketAssignmentCountList = new ArrayList<BucketAssignmentCount>();
        Integer totalAssignments = 0;
        Integer nullAssignments = 0;

        String CQL = "SELECT * FROM bucket_assignment_counts  " +
                "WHERE experiment_id =?";
        try {
            Rows<Experiment.ID, String> rows = driver.getKeyspace()
                    .prepareQuery(keyspace.bucketAssignmentCountsCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(experiment.getID(), ExperimentIDSerializer.get())
                    .execute()
                    .getResult()
                    .getRows();

            if (!rows.isEmpty()) {
                for (int i = 0; i < rows.size(); i++) {
                    ColumnList<String> columns = rows.getRowByIndex(i).getColumns();
                    if (columns.getStringValue("bucket_label", null) != null) {
                        Bucket.Label bucketLabel = Bucket.Label.valueOf(columns.getStringValue("bucket_label", null));
                        Long numberOfAssignments = columns.getColumnByName("bucket_assignment_count").getLongValue();
                        totalAssignments = totalAssignments + numberOfAssignments.intValue();
                        // Updates the BucketAssignmentCountList with # of assignments for that bucket
                        if (!"NULL".equals(bucketLabel.toString())) {
                            bucketAssignmentCountList.add(new BucketAssignmentCount.Builder()
                                    .withBucket(bucketLabel)
                                    .withCount(numberOfAssignments.intValue())
                                    .build());
                        } else {
                            nullAssignments = numberOfAssignments.intValue();
                            bucketAssignmentCountList.add(new BucketAssignmentCount.Builder()
                                    .withBucket(null)
                                    .withCount(nullAssignments)
                                    .build());
                        }
                    }
                }

            } else {
                bucketAssignmentCountList.add(new BucketAssignmentCount.Builder()
                        .withBucket(null)
                        .withCount(0)
                        .build());
            }

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not fetch the bucket assignment counts for experiment " + experiment.getID(), e);
        }

        // Updates the assignmentCounts with bucketAssignmentCountList and
        // the total # of bucket assignments for that experiment
        assignmentCounts = new AssignmentCounts.Builder()
                .withBucketAssignmentCount(bucketAssignmentCountList)
                .withExperimentID(experiment.getID())
                .withTotalUsers(new TotalUsers.Builder()
                        .withBucketAssignments((long) totalAssignments - nullAssignments)
                        .withNullAssignments(nullAssignments)
                        .withTotal(totalAssignments)
                        .build()).build();
        return assignmentCounts;
    }

    @Override
    public void insertExperimentBucketAssignment(Experiment.ID experimentID, Instant date, boolean bucketAssignment) {
        try {
            driver.getKeyspace()
                    .prepareQuery(keyspace.experimentAssignmentType())
                    .withCql("INSERT INTO experiment_assignment_type ( experiment_id, timestamp, bucket_assignment ) VALUES ( ?, ?, ? );")
                    .asPreparedStatement()
                    .withUUIDValue(experimentID.getRawID())
                    .withLongValue(date.toEpochMilli())
                    .withBooleanValue(bucketAssignment)
                    .execute();
        } catch (ConnectionException e) {
            LOG.error("Failed to write into experiment_assignment_type for experiment {} on {}. Exception: {}",
                    experimentID.getRawID().toString(), date, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<OffsetDateTime, Double> getExperimentBucketAssignmentRatioPerDay(Experiment.ID experimentID, OffsetDateTime fromDate, OffsetDateTime toDate) {

        Map<OffsetDateTime, Double> experimentBucketAssignmentRatios = new HashMap<>();

        OffsetDateTime currentDate = fromDate;
        do { // while (currentDate.isBefore(toDate))
            OffsetDateTime currentDatePlusOne = currentDate.plusDays(1).truncatedTo(ChronoUnit.DAYS);

            // counts[0]: bucket assignments, counts[1]: total
            final int[] counts = new int[2];
            try {
                driver.getKeyspace()
                        .prepareQuery(keyspace.experimentAssignmentType())
                        .withCql("SELECT bucket_assignment FROM experiment_assignment_type WHERE experiment_id = ? AND timestamp >= ? AND timestamp < ? ;")
                        .asPreparedStatement()
                        .withUUIDValue(experimentID.getRawID())
                        .withLongValue(currentDate.toInstant().toEpochMilli())
                        .withLongValue(currentDatePlusOne.toInstant().toEpochMilli())
                        .execute()
                        .getResult()
                        .getRows()
                        .forEach(row -> {
                            ColumnList<String> columns = row.getColumns();
                            try {
                                if (columns.getBooleanValue("bucket_assignment", false)) {
                                    counts[0] += 1;
                                }
                                counts[1] += 1;
                            } catch (NullPointerException ignore) {
                            }
                        });
            } catch (ConnectionException e) {
                throw new RepositoryException(
                        String.format("Failed to select from experiment_assignment_type for experiment %s between %s and %s.",
                                experimentID.getRawID().toString(),
                                currentDate.toString(),
                                currentDate.plusDays(1).toString()),
                        e);
            }
            experimentBucketAssignmentRatios.put(currentDate, counts[1] > 0 ? (double) counts[0] / (double) counts[1] : 0);

            currentDate = currentDatePlusOne;
        } while (!currentDate.isAfter(toDate));

        return experimentBucketAssignmentRatios;
    }

}

