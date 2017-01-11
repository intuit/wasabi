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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketAssignmentCount;
import com.intuit.wasabi.analyticsobjects.counts.TotalUsers;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.ExperimentDriver;
import com.intuit.wasabi.exceptions.ConstraintViolationException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.BucketAuditInfo;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ExperimentAuditInfo;
import com.intuit.wasabi.experimentobjects.Experiment.State;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ApplicationNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.BucketLabelSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.BucketStateSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentLabelSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentStateSerializer;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.PreparedCqlQuery;
import com.netflix.astyanax.serializers.DateSerializer;
import com.toddfast.mutagen.cassandra.CassandraMutagen;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * Cassandra experiment repo
 * 
 * @see ExperimentRepository
 */
class CassandraExperimentRepository extends AbstractCassandraRepository<ExperimentsKeyspace> implements
        ExperimentRepository {

    private final ExperimentValidator validator;

    /**
     * Constructor
     * @param mutagen cassandra mutagen
     * @param driver  cassandra driver
     * @param keyspace cassandra keyspace
     * @param validator experiment validator
     * @throws IOException io exception
     * @throws ConnectionException when connection failed
     */
    @Inject
    public CassandraExperimentRepository(CassandraMutagen mutagen, @ExperimentDriver CassandraDriver driver,
                                         ExperimentsKeyspace keyspace, ExperimentValidator validator)
            throws IOException, ConnectionException {

        super(mutagen, driver, keyspace);
        this.validator = validator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment getExperiment(Experiment.ID experimentID) {
        return getExperiment(experimentID, null);
    }


    /**
     * Get experiment
     * @param experimentID experiment id
     * @param consistency cassandra consistency level
     * @return the experiment
     * @ repository failure
     */
    private Experiment getExperiment(Experiment.ID experimentID,
                                     ConsistencyLevel consistency)
             {

        Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");

        final String CQL = "select * from experiment " +
                "where id = ?";

        final ConsistencyLevel CONSISTENCY = consistency != null
                ? consistency
                : ConsistencyLevel.CL_QUORUM;

        try {
            OperationResult<CqlResult<Experiment.ID, String>> opResult =
                    getDriver().getKeyspace()
                            .prepareQuery(getKeyspace().experimentCF())
                            .setConsistencyLevel(CONSISTENCY)
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                            .execute();

            Experiment result = null;

            Rows<Experiment.ID, String> rows = opResult.getResult().getRows();
            if (!rows.isEmpty()) {
                assert rows.size() <= 1 :
                        "More than a single row returned";

                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                result = new CassandraExperiment(columns);
            }

            // Mask experiments that are in the deleted state
            if (result != null && result.getState() == State.DELETED) {
                result = null;
            }

            return result;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve experiment " +
                    "with ID \"" + experimentID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment getExperiment(Application.Name appName, Experiment.Label experimentLabel) {
        return getExperiment(appName, experimentLabel, null);
    }

    /**
     * Get experiment by params
     * @param appName application name
     * @param experimentLabel experiment label
     * @param consistency  cassandra consistency level
     * @return the experiment
     */
    private Experiment getExperiment(Application.Name appName,
                                     Experiment.Label experimentLabel, ConsistencyLevel consistency) {

        Preconditions.checkNotNull(appName, "Parameter \"appName\" cannot be null");
        Preconditions.checkNotNull(experimentLabel, "Parameter \"experimentLabel\" cannot be null");

        final String CQL = "select * from experiment_label_index " +
                "where app_name = ? and label = ?";


        final ConsistencyLevel CONSISTENCY = (consistency != null)
                ? consistency
                : ConsistencyLevel.CL_QUORUM;

        try {
            Rows<ExperimentsKeyspace.AppNameExperimentLabelComposite, String> rows =
                    getDriver().getKeyspace()
                            .prepareQuery(getKeyspace().experimentLabelIndexCF())
                            .setConsistencyLevel(CONSISTENCY)
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(appName, ApplicationNameSerializer.get())
                            .withByteBufferValue(experimentLabel, ExperimentLabelSerializer.get())
                            .execute()
                            .getResult()
                            .getRows();

            Experiment result = null;

            if (!rows.isEmpty()) {
                Row<ExperimentsKeyspace.AppNameExperimentLabelComposite, String> row = rows.getRowByIndex(0);
                Experiment.ID experimentID = Experiment.ID.valueOf(Preconditions.checkNotNull(
                        row.getColumns().getUUIDValue("id", null)));
                result = getExperiment(experimentID);
            }

            return result;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve experiment \"" + appName + "\".\"" + experimentLabel
                    + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment.ID createExperiment(NewExperiment newExperiment)
             {

        validator.validateNewExperiment(newExperiment);

        // Ensure no other experiment is using the label
        Experiment existingExperiment =
                getExperiment(newExperiment.getApplicationName(), newExperiment.getLabel(),
                        ConsistencyLevel.CL_QUORUM);

        if (existingExperiment != null) {
            throw new ConstraintViolationException(ConstraintViolationException.Reason.UNIQUE_CONSTRAINT_VIOLATION,
                    "An active experiment with label \"" + newExperiment.getApplicationName() + "\".\"" +
                            newExperiment.getLabel() + "\" already exists (id = " + existingExperiment.getID() + ")",
                    null);
        }

        // Yes, there is a race condition here, where two experiments
        // being created with the same app name/label could result in one being 
        // clobbered. In practice, this should never happen, but...
        // TODO: Implement a transactional recipe
        final String CQL = "insert into experiment " +
                "(id, description, hypothesis_is_correct, results, rule, sample_percent, start_time, end_time, " +
                "   state, label, app_name, created, modified, is_personalized, model_name, model_version," +
                " is_rapid_experiment, user_cap, creatorid) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            final Experiment.ID experimentID = newExperiment.getID();

            // Note that this timestamp gets serialized as mulliseconds from
            // the epoch, so timezone is irrelevant
            final Date NOW = new Date();
            final Experiment.State STATE = State.DRAFT;


            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().experimentCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                    .withStringValue(newExperiment.getDescription() != null
                            ? newExperiment.getDescription()
                            : "")
                    .withStringValue(newExperiment.getHypothesisIsCorrect() != null ? newExperiment.getHypothesisIsCorrect() : "")
                    .withStringValue(newExperiment.getResults() != null ? newExperiment.getResults() : "")
                    .withStringValue(newExperiment.getRule() != null
                            ? newExperiment.getRule()
                            : "")
                    .withDoubleValue(newExperiment.getSamplingPercent())
                    .withByteBufferValue(newExperiment.getStartTime(), DateSerializer.get())
                    .withByteBufferValue(newExperiment.getEndTime(), DateSerializer.get())
                    .withByteBufferValue(STATE, ExperimentStateSerializer.get())
                    .withByteBufferValue(newExperiment.getLabel(), ExperimentLabelSerializer.get())
                    .withByteBufferValue(newExperiment.getApplicationName(), ApplicationNameSerializer.get())
                            // created
                    .withByteBufferValue(NOW, DateSerializer.get())
                            // modified
                    .withByteBufferValue(NOW, DateSerializer.get())
                            //isPersonalizationEnabled
                    .withBooleanValue(newExperiment.getIsPersonalizationEnabled())
                    .withStringValue(newExperiment.getModelName())
                    .withStringValue(newExperiment.getModelVersion())
                    .withBooleanValue(newExperiment.getIsRapidExperiment())
                    .withIntegerValue(newExperiment.getUserCap())
                    .withStringValue(newExperiment.getCreatorID() != null
                            ? newExperiment.getCreatorID()
                            : "")
                    .execute();
            createApplication(newExperiment.getApplicationName());
            return experimentID;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not create experiment \"" + newExperiment + "\"", e);
        }
    }

    /**
     * Create indices for new experiment
     *
     * @param newExperiment the new experiment object
     * 
     * TODO: Need more clarification
     */
    // TODO - Why is this on the interface - if not called by the service it leaves the indices out of sync
    @Override
    public void createIndicesForNewExperiment(NewExperiment newExperiment) {
        // Point the experiment index to this experiment
        updateExperimentLabelIndex(newExperiment.getID(), newExperiment.getApplicationName(), newExperiment.getLabel(),
                newExperiment.getStartTime(), newExperiment.getEndTime(), State.DRAFT);

        try {
            updateStateIndex(null, newExperiment.getID(), ExperimentsKeyspace.ExperimentStateIndexKey.NOT_DELETED);
        } catch (ConnectionException e) {
            //remove the created ExperimentLabelIndex
            removeExperimentLabelIndex(newExperiment.getApplicationName(), newExperiment.getLabel());
            throw new RepositoryException("Could not update indices for experiment \"" + newExperiment + "\"", e);
        }
    }

    /**
     * Get the summary of assignments delivered for each experiment
     */
    @Override
    public AssignmentCounts getAssignmentCounts(Experiment.ID experimentID, Context context) {

        List<Bucket> bucketList = getBuckets(experimentID).getBuckets();

        AssignmentCounts.Builder builder = new AssignmentCounts.Builder();
        builder.withExperimentID(experimentID);

        List<BucketAssignmentCount> bucketAssignmentCountList = new ArrayList<>(bucketList.size() + 1);
        long bucketAssignmentsCount = 0, nullAssignmentsCount = 0;
        for (Bucket bucket : bucketList) {
            try {
                String cql = "select count(*) from user_bucket_index " +
                        "where experiment_id = " + experimentID +
                        " and context ='" + context +
                        "' and bucket_label = '" + bucket.getLabel() + "'";

                Rows<Experiment.ID, String> rows = getDriver().getKeyspace()
                        .prepareQuery(getKeyspace().userBucketIndexCF())
                        .withCql(cql)
                        .execute()
                        .getResult()
                        .getRows();

                if (rows.size() == 1) {
                    long count = rows.getRowByIndex(0).getColumns().getColumnByIndex(0).getLongValue();
                    bucketAssignmentCountList.add(new BucketAssignmentCount.Builder()
                            .withBucket(bucket.getLabel())
                            .withCount(count)
                            .build());
                    bucketAssignmentsCount += count;
                }

            } catch (ConnectionException e) {
                throw new RepositoryException("Could not fetch assignmentCounts for experiment " +
                        "with ID \"" + experimentID + "\"", e);
            }
        }

        // Checking the count for null assignments
        try {
            String cql = "select count(*) from user_bucket_index " +
                    "where experiment_id = " + experimentID +
                    " and context ='" + context +
                    "' and bucket_label = ''";
            Rows<Experiment.ID, String> rows = getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().userBucketIndexCF())
                    .withCql(cql)
                    .execute()
                    .getResult()
                    .getRows();

            if (rows.size() == 1) {
                nullAssignmentsCount = rows.getRowByIndex(0)
                        .getColumns()
                        .getColumnByIndex(0)
                        .getLongValue();

                bucketAssignmentCountList.add(new BucketAssignmentCount.Builder()
                        .withBucket(null)
                        .withCount(nullAssignmentsCount)
                        .build());
            } else {
                bucketAssignmentCountList.add(new BucketAssignmentCount.Builder()
                        .withBucket(null)
                        .withCount(0)
                        .build());
            }

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not fetch assignmentCounts for experiment " +
                    "with ID \"" + experimentID + "\"", e);
        }

        return builder.withBucketAssignmentCount(bucketAssignmentCountList)
                .withTotalUsers(new TotalUsers.Builder()
                        .withTotal(bucketAssignmentsCount + nullAssignmentsCount)
                        .withBucketAssignments(bucketAssignmentsCount)
                        .withNullAssignments(nullAssignmentsCount)
                        .build())
                .build();
    }

    /**
     * Get a bucket list for a list of Experiments using a single cassandra call
     */
    @Override
    public Map<Experiment.ID, BucketList> getBucketList(Collection<Experiment.ID> experimentIDCollection) {
        StringBuilder cqlQuery = new StringBuilder("select * from bucket where experiment_id in (");
        cqlQuery.append(Joiner.on(',').join(experimentIDCollection));
        cqlQuery.append(")");
        try {
            Rows<Bucket.Label, String> rows = getDriver().getKeyspace().prepareQuery(getKeyspace().bucketCF())
                    .withCql(cqlQuery.toString()).asPreparedStatement().execute().getResult().getRows();
            if (!rows.isEmpty()) {
                Map<Experiment.ID, BucketList> result = new HashMap<>();

                for (Row<Bucket.Label, String> row : rows) {
                    Bucket bucket = Bucket.from(row.getColumns()).build();
                    BucketList bucketList = result.get(bucket.getExperimentID());
                    if (bucketList == null) {
                        bucketList = new BucketList();
                        bucketList.addBucket(bucket);
                    } else {
                        bucketList.addBucket(bucket);
                    }
                    result.put(bucket.getExperimentID(), bucketList);
                }
                return result;
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not fetch buckets for the list of experiments", e);
        }
        return null;
    }

    /**
     * Get the list of buckets for an experiment
     *
     * @param experimentID experiment id
     * @return a list of buckets
     */
    @Override
    public BucketList getBucketList(Experiment.ID experimentID) {

        BucketList bucketList = new BucketList();

        final String CQL = "select * from bucket where experiment_id = ?";
        try {
            Rows<Bucket.Label, String> rows = getDriver().getKeyspace().prepareQuery(getKeyspace().bucketCF())
                    .withCql(CQL).asPreparedStatement().withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                    .execute().getResult().getRows();
            if (!rows.isEmpty()) {
                for (Row<Bucket.Label, String> row : rows) {
                    bucketList.addBucket(Bucket.from(row.getColumns()).build());
                }
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not fetch buckets for experiment \"" + experimentID + "\" ", e);
        }
        return bucketList;
    }

    @Override
    public Experiment updateExperiment(Experiment experiment)
             {

        validator.validateExperiment(experiment);

        final String CQL = "update experiment " +
                "set description = ?, hypothesis_is_correct = ?, results = ?, rule = ?, sample_percent = ?, " +
                "start_time = ?, end_time = ?, " +
                "state=?, label=?, app_name=?, modified=? , is_personalized=?, model_name=?, model_version=?," +
                " is_rapid_experiment=?, user_cap=?" +
                " where id = ?";

        try {
            // Note that this timestamp gets serialized as mulliseconds from
            // the epoch, so timezone is irrelevant
            final Date NOW = new Date();

            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().experimentCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withStringValue(experiment.getDescription() != null
                            ? experiment.getDescription()
                            : "")
                    .withStringValue(experiment.getHypothesisIsCorrect() != null ? experiment.getHypothesisIsCorrect() : "")
                    .withStringValue(experiment.getResults() != null ? experiment.getResults() : "")
                    .withStringValue(experiment.getRule() != null
                            ? experiment.getRule()
                            : "")
                    .withDoubleValue(experiment.getSamplingPercent())
                    .withByteBufferValue(experiment.getStartTime(), DateSerializer.get())
                    .withByteBufferValue(experiment.getEndTime(), DateSerializer.get())
                    .withByteBufferValue(experiment.getState(), ExperimentStateSerializer.get())
                    .withByteBufferValue(experiment.getLabel(), ExperimentLabelSerializer.get())
                    .withByteBufferValue(experiment.getApplicationName(), ApplicationNameSerializer.get())
                    .withByteBufferValue(NOW, DateSerializer.get())
                    .withBooleanValue(experiment.getIsPersonalizationEnabled())
                    .withStringValue(experiment.getModelName())
                    .withStringValue(experiment.getModelVersion())
                    .withBooleanValue(experiment.getIsRapidExperiment())
                    .withIntegerValue(experiment.getUserCap())
                    .withByteBufferValue(experiment.getID(), ExperimentIDSerializer.get())
                    .execute();

            // Point the experiment index to this experiment
            updateExperimentLabelIndex(experiment.getID(), experiment.getApplicationName(), experiment.getLabel(),
                    experiment.getStartTime(), experiment.getEndTime(), experiment.getState());

            updateStateIndex(null, experiment);

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not update experiment with ID \"" + experiment.getID() + "\"", e);
        }

        return experiment;
    }

    /**
     * {@inheritDoc}
     */
   @Override
    public Experiment updateExperimentState(Experiment experiment, State state)
             {

        validator.validateExperiment(experiment);

        final String CQL = "update experiment" +
                " set state = ?, modified = ?" +
                " where id = ?";

        try {
            // Note that this timestamp gets serialized as mulliseconds from
            // the epoch, so timezone is irrelevant
            final Date NOW = new Date();

            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().experimentCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(state, ExperimentStateSerializer.get())
                    .withByteBufferValue(NOW, DateSerializer.get())
                    .withByteBufferValue(experiment.getID(), ExperimentIDSerializer.get())
                    .execute();
            experiment = Experiment.from(experiment).withState(state).build();

            // Point the experiment index to this experiment
            updateExperimentLabelIndex(experiment.getID(), experiment.getApplicationName(), experiment.getLabel(),
                    experiment.getStartTime(), experiment.getEndTime(), experiment.getState());

            updateStateIndex(null, experiment);

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not update experiment with ID \""
                    + experiment.getID() + "\"" + " to state " + state.toString(), e);
        }

        return experiment;
    }

   /**
    * {@inheritDoc}
    */
    @Override
    public List<Experiment.ID> getExperiments() {

        try {
            // Get all experiments that are live
            Collection<Experiment.ID> experimentIDs = getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().stateExperimentIndexCF())
                    .getKey(ExperimentsKeyspace.ExperimentStateIndexKey.NOT_DELETED)
                    .execute()
                    .getResult()
                    .getColumnNames();
            return new ArrayList<>(experimentIDs);
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve experiments", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment> getExperiments(Application.Name appName) {

        Rows<Experiment.ID, String> rows = getExperimentRows(appName);

        List<Experiment> experiments = new ArrayList<>();
        if (!rows.isEmpty()) {
            for (int i = 0; i < rows.size(); i++) {
                ColumnList<String> columns = rows.getRowByIndex(i).getColumns();
                Experiment result = new CassandraExperiment(columns);
                if (result != null && result.getState() != null &&
                        result.getState() != State.TERMINATED &&
                        result.getState() != State.DELETED) {
                    experiments.add(result);
                }
            }
        }
        return experiments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExperiment(NewExperiment newExperiment)  {

        Experiment.ID experimentID = newExperiment.getID();

        // Delete the experiment metadata from experiment table
        String cql = "delete from experiment where id = ?";
        try {
            getDriver().getKeyspace().prepareQuery(getKeyspace().experimentCF())
                    .withCql(cql)
                    .asPreparedStatement()
                    .withByteBufferValue(newExperiment.getID(), ExperimentIDSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not delete experiment " +
                    "with id \"" + experimentID + "\"", e);
        }
    }

    /**
     * Get experiment rows
     *
     * @param appName {@link Application.Name}
     *
     * @return Experiment rows
     */
    @Override
    public Rows<Experiment.ID, String> getExperimentRows(Application.Name appName) {

        Preconditions.checkNotNull(appName, "Parameter \"appName\" cannot be null");

        final String CQL = "select * from experiment where app_name = ?";

        try {

            OperationResult<CqlResult<Experiment.ID, String>> opResult =
                    getDriver().getKeyspace()
                            .prepareQuery(getKeyspace().experimentCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(appName, ApplicationNameSerializer.get())
                            .execute();

            return opResult.getResult().getRows();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve experiment swith application name \"" + appName + "\"",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getExperiments(Collection<Experiment.ID> experimentIDs) {
        ExperimentList result = new ExperimentList();
        try {
            if (!experimentIDs.isEmpty()) {
                String cql = "select * from experiment where id in (" + StringUtils.join(experimentIDs, ',') + ")";
                Rows<Experiment.ID, String> rows = getDriver().getKeyspace().prepareQuery(getKeyspace().experimentCF())
                        .withCql(cql).asPreparedStatement().execute().getResult().getRows();
                for (Row<Experiment.ID, String> row : rows) {
                    Experiment experiment = new CassandraExperiment(row.getColumns());
                    if (experiment.getState() == State.DELETED) {
                        continue;
                    }
                    result.addExperiment(experiment);
                }
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve the experiments for the collection of experimentIDs", e);
        }
        return result;
    }

    /**
     * Get the experiments for an Application
     */
    @Override
    public Table<Experiment.ID, Experiment.Label, Experiment> getExperimentList(Application.Name appName) {

        Rows<Experiment.ID, String> rows = getExperimentRows(appName);
        Table<Experiment.ID, Experiment.Label, Experiment> result = HashBasedTable.create();
        if (!rows.isEmpty()) {
            for (int i = 0; i < rows.size(); i++) {
                ColumnList<String> columns = rows.getRowByIndex(i).getColumns();
                Experiment experiment = new CassandraExperiment(columns);
                if (experiment.getState() != State.TERMINATED && experiment.getState() != State.DELETED) {
                    result.put(experiment.getID(), experiment.getLabel(), experiment);
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
   @Override
    public List<Application.Name> getApplicationsList() {

        List<Application.Name> result = new ArrayList<>();

        final String CQL = "select distinct app_name from applicationList";
        try {
            Rows<Application.Name, String> rows = getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().applicationList_CF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .execute()
                    .getResult()
                    .getRows();

            if (!rows.isEmpty()) {
                for (Row<Application.Name, String> row : rows) {
                    Application.Name applicationName = Application.Name.valueOf(row.getColumns()
                            .getColumnByName("app_name")
                            .getStringValue());
                    result.add(applicationName);
                }
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve the application names", e);
        }
        return result;
    }

   /**
    * {@inheritDoc}
    */
    @Override
    public Bucket getBucket(Experiment.ID experimentID, Bucket.Label bucketLabel) {

        Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");
        Preconditions.checkNotNull(bucketLabel, "Parameter \"bucketLabel\" cannot be null");

        final String CQL = "select * from bucket " +
                "where experiment_id = ? and label = ?";

        try {
            Rows<Bucket.Label, String> rows =
                    getDriver().getKeyspace()
                            .prepareQuery(getKeyspace().bucketCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                            .withByteBufferValue(bucketLabel, BucketLabelSerializer.get())
                            .execute()
                            .getResult()
                            .getRows();

            Bucket result = null;

            if (!rows.isEmpty()) {
                assert rows.size() <= 1 : "More than a single row returned";

                ColumnList<String> columns = rows.getRowByIndex(0).getColumns();
                result = Bucket.from(columns).build();
            }

            return result;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve bucket \"" +
                    bucketLabel + "\" in experiment \"" + experimentID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createBucket(Bucket newBucket) {

        Preconditions.checkNotNull(newBucket, "Parameter \"newBucket\" cannot be null");

        final Bucket.State STATE = Bucket.State.OPEN;

        final String CQL = "insert into bucket " +
                "(experiment_id, label, description, allocation, " +
                "   is_control, payload, state) " +
                "values (?, ?, ?, ?, ?, ?, ?)";

        try {
            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().bucketCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(newBucket.getExperimentID(), ExperimentIDSerializer.get())
                    .withByteBufferValue(newBucket.getLabel(), BucketLabelSerializer.get())
                    .withStringValue(newBucket.getDescription() != null
                            ? newBucket.getDescription()
                            : "")
                    .withDoubleValue(newBucket.getAllocationPercent())
                    .withBooleanValue(newBucket.isControl() != null
                            ? newBucket.isControl()
                            : false)
                    .withStringValue(newBucket.getPayload() != null
                            ? newBucket.getPayload()
                            : "")
                    .withByteBufferValue(STATE, BucketStateSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not create bucket \"" + newBucket + "\"", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Bucket updateBucket(Bucket bucket) {

        // Note, if the bucket doesn't already exist, it will be created
        // because Cassandra always does upserts

        /*
        * First update the is_control column to false for all the buckets of an experiment
        * Then make the is_control to true only for the one requested by the user
        * */


        if (bucket.isControl()) {

            String cql = "select * from bucket " +
                    "where experiment_id = ?";

            try {
                OperationResult<CqlResult<Bucket.Label, String>> opResult = getDriver().getKeyspace()
                        .prepareQuery(getKeyspace().bucketCF())
                        .withCql(cql)
                        .asPreparedStatement()
                        .withByteBufferValue(bucket.getExperimentID(), ExperimentIDSerializer.get())
                        .execute();
                Rows<Bucket.Label, String> rows = opResult.getResult().getRows();

                List<Bucket> buckets = new ArrayList<>(rows.size());
                for (Row<Bucket.Label, String> row : rows) {
                    buckets.add(Bucket.from(row.getColumns()).build());
                }

                try {
                    cql = "update bucket set is_control=false where experiment_id =? and label =?";

                    for (Bucket bucket1 : buckets) {
                        getDriver().getKeyspace()
                                .prepareQuery(getKeyspace().bucketCF())
                                .withCql(cql)
                                .asPreparedStatement()
                                .withByteBufferValue(bucket.getExperimentID(), ExperimentIDSerializer.get())
                                .withByteBufferValue(bucket1.getLabel(), BucketLabelSerializer.get())
                                .execute();
                    }
                } catch (ConnectionException e) {
                    throw new RepositoryException("Could not update buckets", e);
                }
            } catch (ConnectionException e) {
                throw new RepositoryException("Could not retrieve buckets", e);
            }
        }


        final String CQL = "update bucket " +
                "set description = ?, allocation = ?, is_control = ?, payload = ? " +
                "where experiment_id = ? and label = ?";

        try {
            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().bucketCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withStringValue(bucket.getDescription() != null
                            ? bucket.getDescription()
                            : "")
                    .withDoubleValue(bucket.getAllocationPercent())
                    .withBooleanValue(bucket.isControl())
                    .withStringValue(bucket.getPayload() != null
                            ? bucket.getPayload()
                            : "")
                    .withByteBufferValue(bucket.getExperimentID(), ExperimentIDSerializer.get())
                    .withByteBufferValue(bucket.getLabel(), BucketLabelSerializer.get())
                    .execute();

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not update bucket \"" +
                    bucket.getExperimentID() + "\".\"" + bucket.getLabel() + "\"", e);
        }
        return bucket;
    }


    @Override
    public Bucket updateBucketAllocationPercentage(Bucket bucket, Double desiredAllocationPercentage) {

        final String CQL = "update bucket " +
                "set allocation = ? " +
                "where experiment_id = ? and label = ?";

        try {
            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().bucketCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withDoubleValue(desiredAllocationPercentage)
                    .withByteBufferValue(bucket.getExperimentID(), ExperimentIDSerializer.get())
                    .withByteBufferValue(bucket.getLabel(), BucketLabelSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not update bucket allocation percentage \"" +
                    bucket.getExperimentID() + "\".\"" + bucket.getLabel() + "\"", e);
        }

        // return the bucket with the updated values
        bucket = getBucket(bucket.getExperimentID(), bucket.getLabel());

        return bucket;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bucket updateBucketState(Bucket bucket, Bucket.State desiredState)  {

        final String CQL = "update bucket " +
                "set state = ? " +
                "where experiment_id = ? and label = ?";

        try {
            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().bucketCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(desiredState, BucketStateSerializer.get())
                    .withByteBufferValue(bucket.getExperimentID(), ExperimentIDSerializer.get())
                    .withByteBufferValue(bucket.getLabel(), BucketLabelSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not update bucket state \"" +
                    bucket.getExperimentID() + "\".\"" + bucket.getLabel() + "\"", e);
        }

        // return the bucket with the updated values
        bucket = getBucket(bucket.getExperimentID(), bucket.getLabel());

        return bucket;
    }

    /**
     * Update bucket batch
     * 
     * @param experimentID the experiment id
     * @param bucketList  the bucket list
     * 
     * @return BucketList
     */
    @Override
    public BucketList updateBucketBatch(Experiment.ID experimentID, BucketList bucketList) {

        String CQL = "BEGIN BATCH ";
        for (int i = 0; i < bucketList.getBuckets().size(); i++) {
            Bucket b = bucketList.getBuckets().get(i);
            CQL += "UPDATE bucket SET ";
            if (b.getState() != null) {
                CQL += "state = ?,";
            }
            if (b.getAllocationPercent() != null) {
                CQL += "allocation = ?,";
            }
            if (b.getDescription() != null) {
                CQL += "description = ?,";
            }
            if (b.isControl() != null) {
                CQL += "is_control = ?,";
            }
            if (b.getPayload() != null) {
                CQL += "payload = ?,";
            }
            if (",".equals(CQL.substring(CQL.length() - 1, CQL.length()))) {
                CQL = CQL.substring(0, CQL.length() - 1);
            }
            CQL += "where experiment_id = ? and label = ?;";
        }
        CQL += "APPLY BATCH;";

        try {
            PreparedCqlQuery<Bucket.Label, String> temp = getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().bucketCF())
                    .withCql(CQL)
                    .asPreparedStatement();

            for (int i = 0; i < bucketList.getBuckets().size(); i++) {
                Bucket b = bucketList.getBuckets().get(i);
                if (b.getState() != null) {
                    temp = temp.withByteBufferValue(b.getState(), BucketStateSerializer.get());
                }
                if (b.getAllocationPercent() != null) {
                    temp = temp.withDoubleValue(b.getAllocationPercent());
                }
                if (b.getDescription() != null) {
                    temp = temp.withStringValue(b.getDescription());
                }
                if (b.isControl() != null) {
                    temp = temp.withBooleanValue(b.isControl());
                }
                if (b.getPayload() != null) {
                    temp = temp.withStringValue(b.getPayload());
                }
                temp = temp.withByteBufferValue(experimentID, ExperimentIDSerializer.get());
                temp = temp.withByteBufferValue(b.getLabel(), BucketLabelSerializer.get());
            }

            temp.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not update bucket for experiment \"" +
                    experimentID + "\"", e);
        }

        // return the bucket with the updated values
        BucketList buckets;
        buckets = getBuckets(experimentID);
        return buckets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBucket(Experiment.ID experimentID,
                             Bucket.Label bucketLabel) {

        Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");
        Preconditions.checkNotNull(bucketLabel, "Parameter \"bucketLabel\" cannot be null");

        try {
            final String CQL = "delete from bucket " +
                    "where experiment_id = ? and label = ?";

            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().bucketCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                    .withByteBufferValue(bucketLabel, BucketLabelSerializer.get())
                    .execute();


        } catch (ConnectionException e) {
            throw new RepositoryException("Could not delete bucket \"" + bucketLabel + "\" from experiment with ID \"" +
                    experimentID + "\"", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void logBucketChanges(Experiment.ID experimentID, Bucket.Label bucketLabel,
                                 List<BucketAuditInfo> changeList)  {

        final long NOW = System.currentTimeMillis();
        final String CQL = "insert into bucket_audit_log " +
                "(experiment_id, label, modified, attribute_name, old_value, new_value) " +
                "values (?,?,?,?,?,?)";
        try {
            for (BucketAuditInfo changeData : changeList) {
                getDriver().getKeyspace()
                        .prepareQuery(getKeyspace().bucket_audit_log_CF())
                        .withCql(CQL)
                        .asPreparedStatement()
                        .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                        .withByteBufferValue(bucketLabel, BucketLabelSerializer.get())
                        .withLongValue(NOW)
                        .withStringValue(changeData.getAttributeName())
                        .withStringValue((changeData.getOldValue() != null) ? changeData.getOldValue() : "")
                        .withStringValue((changeData.getNewValue() != null) ? changeData.getNewValue() : "")
                        .execute();
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not log bucket changes \"" + experimentID + " : " + bucketLabel + " " +
                    "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logExperimentChanges(Experiment.ID experimentID, List<ExperimentAuditInfo> changeList) {
        final long NOW = System.currentTimeMillis();
        final String CQL = "insert into experiment_audit_log " +
                "(experiment_id, modified,attribute_name, old_value, new_value) " +
                "values(?,?,?,?,?)";
        try {

            for (ExperimentAuditInfo changeData : changeList) {
                getDriver().getKeyspace()
                        .prepareQuery(getKeyspace().experiment_audit_log_CF())
                        .withCql(CQL)
                        .asPreparedStatement()
                        .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                        .withLongValue(NOW)
                        .withStringValue(changeData.getAttributeName())
                        .withStringValue((changeData.getOldValue() != null)
                                ? changeData.getOldValue()
                                : "")
                        .withStringValue((changeData.getNewValue() != null)
                                ? changeData.getNewValue()
                                : "")
                        .execute();
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not log experiment changes for experimentID \"" +
                    experimentID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList getBuckets(Experiment.ID experimentID) {

        Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");

        // Check if the experiment is live
        Experiment experiment = getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        final String CQL = "select * from bucket " +
                "where experiment_id = ?";

        try {
            OperationResult<CqlResult<Bucket.Label, String>> opResult =
                    getDriver().getKeyspace()
                            .prepareQuery(getKeyspace().bucketCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                            .execute();

            Rows<Bucket.Label, String> rows = opResult.getResult().getRows();

            BucketList result = new BucketList(rows.size());
            for (Row<Bucket.Label, String> row : rows) {
                result.addBucket(Bucket.from(row.getColumns()).build());
            }
            return result;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve experiments", e);
        }
    }

    private void updateExperimentLabelIndex(Experiment.ID experimentID, Application.Name appName,
                                            Experiment.Label experimentLabel, Date startTime, Date endTime,
                                            Experiment.State state) {

        if (state == Experiment.State.TERMINATED || state == Experiment.State.DELETED) {
            removeExperimentLabelIndex(appName, experimentLabel);
            return;
        }

        final String CQL = "update experiment_label_index " +
                "set id = ?, modified = ?, start_time = ?, end_time = ?, state = ? " +
                "where app_name = ? and label = ?";

        try {
            // Note that this timestamp gets serialized as mulliseconds from
            // the epoch, so timezone is irrelevant
            final Date NOW = new Date();

            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().experimentLabelIndexCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                            // modified
                    .withByteBufferValue(NOW, DateSerializer.get())
                    .withByteBufferValue(startTime, DateSerializer.get())
                    .withByteBufferValue(endTime, DateSerializer.get())
                    .withByteBufferValue(state, ExperimentStateSerializer.get())
                    .withByteBufferValue(appName, ApplicationNameSerializer.get())
                    .withByteBufferValue(experimentLabel, ExperimentLabelSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not index experiment \"" +
                    experimentID + "\"", e);
        }
    }

    private void removeExperimentLabelIndex(Application.Name appName,
                                            Experiment.Label experimentLabel) {

        final String CQL = "delete from experiment_label_index " +
                "where app_name = ? and label = ?";

        try {
            // Note that this timestamp gets serialized as mulliseconds from
            // the epoch, so timezone is irrelevant

            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().experimentLabelIndexCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(appName, ApplicationNameSerializer.get())
                    .withByteBufferValue(experimentLabel, ExperimentLabelSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not remove index for " +
                    "experiment \"" + appName + "\".\"" + experimentLabel + "\"", e);
        }
    }

    /**
     * Update state index
     *
     * @param batch {@link MutationBatch}
     * @param experiment the experiment object
     *
     */
    @Override
    public void updateStateIndex(MutationBatch batch, Experiment experiment)
            throws ConnectionException {
        updateStateIndex(batch, experiment.getID(),
                experiment.getState() != State.DELETED
                        ? ExperimentsKeyspace.ExperimentStateIndexKey.NOT_DELETED
                        : ExperimentsKeyspace.ExperimentStateIndexKey.DELETED);
    }

    private void updateStateIndex(final MutationBatch batch, Experiment.ID experimentID,
                                  ExperimentsKeyspace.ExperimentStateIndexKey index)
            throws ConnectionException {

        final MutationBatch BATCH = (batch != null)
                ? batch
                : getDriver().getKeyspace().prepareMutationBatch();

        final ExperimentsKeyspace.ExperimentStateIndexKey ADD;
        final ExperimentsKeyspace.ExperimentStateIndexKey DELETE;

        switch (index) {
            case DELETED:
                ADD = ExperimentsKeyspace.ExperimentStateIndexKey.DELETED;
                DELETE = ExperimentsKeyspace.ExperimentStateIndexKey.NOT_DELETED;
                break;

            case NOT_DELETED:
                ADD = ExperimentsKeyspace.ExperimentStateIndexKey.NOT_DELETED;
                DELETE = ExperimentsKeyspace.ExperimentStateIndexKey.DELETED;
                break;

            default:
                throw new IllegalArgumentException("Unrecognized enum value: " + index);
        }

        BATCH.withRow(getKeyspace().stateExperimentIndexCF(), ADD)
                .putColumn(experimentID, "");
        BATCH.withRow(getKeyspace().stateExperimentIndexCF(), DELETE)
                .deleteColumn(experimentID);

        if (batch == null) {
            BATCH.execute();
        }
    }


    /**
     * Creates an application at top level
     * @param applicationName Application Name
     */
    @Override
    public void createApplication(Application.Name applicationName) {

        final String CQL = "insert into applicationList(app_name) values(?)";

        try {
            getDriver().getKeyspace()
                    .prepareQuery(getKeyspace().applicationList_CF())
                    .withCql(CQL).asPreparedStatement()
                    .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                    .execute().getResult();
        } catch (Exception e) {
            throw new RepositoryException("Unable to insert into top level application list: \""
                    + applicationName.toString() + "\"" + e);
        }
    }
}
