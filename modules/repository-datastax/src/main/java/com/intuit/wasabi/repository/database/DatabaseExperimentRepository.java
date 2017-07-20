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
package com.intuit.wasabi.repository.database;

import com.datastax.driver.core.Statement;
import com.google.common.base.Preconditions;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.googlecode.flyway.core.Flyway;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.exceptions.BucketNotFoundException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.State;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intuit.wasabi.experimentobjects.Experiment.State.DELETED;

/**
 * Database expriment repo for ExperimentRepository
 *
 * @see ExperimentRepository
 */
public class DatabaseExperimentRepository implements ExperimentRepository {


    private final ExperimentValidator validator;
    private TransactionFactory transactionFactory;

    @Inject
    public DatabaseExperimentRepository(TransactionFactory transactionFactory, ExperimentValidator validator,
                                        Flyway flyway) {
        super();

        this.transactionFactory = transactionFactory;
        this.validator = validator;
        initialize(flyway);
    }

    private void initialize(Flyway flyway) {
        flyway.setLocations("com/intuit/wasabi/repository/impl/mysql/migration");
        flyway.setDataSource(transactionFactory.getDataSource());
        flyway.migrate();
    }

    protected Transaction newTransaction() {
        return transactionFactory.newTransaction();
    }

    @Override
    public Experiment getExperiment(Experiment.ID experimentID) throws RepositoryException {

        final String SQL =
                "select * from experiment " +
                        "where id = ? and state != ?";

        try {
            List results = newTransaction().select(
                    SQL,
                    experimentID,
                    State.DELETED.toString());

            if (results.isEmpty()) {
                return null;
            }

            if (results.size() > 1) {
                throw new IllegalStateException(
                        "More than one experiment for ID \"" + experimentID +
                                "\" was found (found " + results.size() + ")");
            }

            Map experimentMap = (Map) results.get(0);

            // Reuse the experiment ID rather than getting from the database
            return Experiment.withID(experimentID)
                    .withLabel(Experiment.Label.valueOf((String) experimentMap.get("label")))
                    .withEndTime((Date) experimentMap.get("end_time"))
                    .withDescription((String) experimentMap.get("description"))
                    .withState(State.toExperimentState((String) experimentMap.get("state")))
                    .withModificationTime((Date) experimentMap.get("modification_time"))
                    .withStartTime((Date) experimentMap.get("start_time"))
                    .withCreationTime((Date) experimentMap.get("creation_time"))
                    .withApplicationName(Application.Name.valueOf((String) experimentMap.get("app_name")))
                    .withSamplingPercent((Double) experimentMap.get("sampling_percent"))
                    .build();


        } catch (WasabiException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve experiment \"" +
                    experimentID + "\"", e);
        }
    }

    @Override
    public Map<Application.Name, List<Experiment>> getExperimentsForApps(Collection<Application.Name> appNames) {
        throw new UnsupportedOperationException("Not supported ");
    }

    @Override
    public Experiment getExperiment(Application.Name appName,
                                    Experiment.Label experimentLabel) {

        final String SQL =
                "select * from experiment " +
                        "where app_name = ? and label = ? and state != ?";

        try {
            List results = newTransaction().select(SQL,
                    appName.toString(),
                    experimentLabel.toString(),
                    State.DELETED.toString());

            if (results.isEmpty()) {
                return null;
            }

            if (results.size() > 1) {
                throw new IllegalStateException(
                        "More than one experiment for label \"" + experimentLabel +
                                "\" was found (found " + results.size() + ")");
            }

            Map experimentMap = (Map) results.get(0);

            Experiment.ID experimentID =
                    Experiment.ID.valueOf((byte[]) experimentMap.get("id"));

            return Experiment.withID(experimentID)
                    .withLabel(Experiment.Label.valueOf((String) experimentMap.get("label")))
                    .withEndTime((Date) experimentMap.get("end_time"))
                    .withDescription((String) experimentMap.get("description"))
                    .withState(State.toExperimentState((String) experimentMap.get("state")))
                    .withModificationTime((Date) experimentMap.get("modification_time"))
                    .withStartTime((Date) experimentMap.get("start_time"))
                    .withCreationTime((Date) experimentMap.get("creation_time"))
                    .withApplicationName(Application.Name.valueOf((String) experimentMap.get("app_name")))
                    .withSamplingPercent((Double) experimentMap.get("sampling_percent"))
                    .build();
        } catch (WasabiException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve experiment \"" +
                    experimentLabel + "\"", e);
        }
    }

    @Override
    public List<Experiment.ID> getExperiments()
            throws RepositoryException {

        final String SQL =
                "select id from experiment " +
                        "where state != ? order by id";

        try {
            List<Map<String, Object>> experiments = newTransaction().select(SQL, DELETED.toString());

            List<Experiment.ID> result = new ArrayList<>();
            for (Map<String, Object> experiment : experiments) {
                result.add(Experiment.ID.valueOf((byte[]) experiment.get("id")));
            }

            return result;
        } catch (WasabiException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException(
                    "Could not retrieve experiment IDs", e);
        }
    }

    @Override
    public ExperimentList getExperiments(Collection<Experiment.ID> experimentIDs) {

        // TODO: Implement this properly as a single query using e.g. WHERE IN
        ExperimentList result = new ExperimentList(experimentIDs.size());
        for (Experiment.ID experimentID : experimentIDs) {
            result.addExperiment(getExperiment(experimentID));
        }

        return result;
    }

    @Override
    public Map<Experiment.ID, Experiment> getExperimentsMap(Collection<Experiment.ID> experimentIDs) {
        throw new UnsupportedOperationException("Not supported ");
    }

    /**
     * Get the experiments for an Application
     */
    @Override
    public Table<Experiment.ID, Experiment.Label, Experiment> getExperimentList(Application.Name appName) {
        throw new UnsupportedOperationException("Not supported ");
    }

    @Override
    public List<Application.Name> getApplicationsList() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported ");
    }

    @Override
    public Experiment.ID createExperiment(NewExperiment newExperiment)
            throws RepositoryException {

        final String SQL =
                "insert into experiment " +
                        "(id, description, sampling_percent, label, " +
                        "start_time, end_time, app_name, state) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            // Note that this timestamp gets serialized as milliseconds from
            // the epoch, so timezone is irrelevant
            newTransaction().insert(
                    SQL,
                    newExperiment.getID(),
                    newExperiment.getDescription() != null
                            ? newExperiment.getDescription()
                            : "",
                    newExperiment.getSamplingPercent(),
                    newExperiment.getLabel().toString(),
                    newExperiment.getStartTime(),
                    newExperiment.getEndTime(),
                    newExperiment.getApplicationName().toString(),
                    State.DRAFT.toString());
            return newExperiment.getID();
        } catch (WasabiException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException(
                    "Could not persist experiment in database \"" +
                            newExperiment.toString() + "\"", e);
        }
    }

    @Override
    public Experiment updateExperiment(Experiment experiment)
            throws RepositoryException {

        validator.validateExperiment(experiment);

        final String SQL =
                "update experiment " +
                        "set description=?, sampling_percent=?, state=?, " +
                        "label=?, start_time=?, end_time=?, app_name=? " +
                        "where id=?";

        int rowCount = newTransaction().update(
                SQL,
                experiment.getDescription() != null
                        ? experiment.getDescription()
                        : "",
                experiment.getSamplingPercent(),
                experiment.getState().toString(),
                experiment.getLabel().toString(),
                experiment.getStartTime(),
                experiment.getEndTime(),
                experiment.getApplicationName().toString(),
                experiment.getID());

        if (rowCount > 1) {
            throw new RepositoryException("Concurrent updates; please retry");
        }

        if (rowCount < 1) {
            throw new RepositoryException("No rows were updated");
        }

        return experiment;
    }

    @Override
    public Experiment updateExperimentState(Experiment experiment, State state)
            throws RepositoryException {

        validator.validateExperiment(experiment);

        final String SQL =
                "update experiment " +
                        "set state=? where id=?";
        int rowCount = newTransaction().update(
                SQL,
                state.toString(),
                experiment.getID());

        if (rowCount > 1) {
            throw new RepositoryException("Concurrent updates; please retry");
        }

        if (rowCount < 1) {
            throw new RepositoryException("No rows were updated");
        }

        return experiment;
    }

    @Override
    public Bucket getBucket(Experiment.ID experimentID,
                            Bucket.Label bucketLabel)
            throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void createBucket(Bucket newBucket)
            throws RepositoryException {


        final String SQL =
                "insert into bucket (" +
                        "experiment_id, description, label, allocation_percent, is_control, payload, state) " +
                        "values (?,?,?,?,?,?,?)";

        try {
            newTransaction().insert(
                    SQL,
                    newBucket.getExperimentID(),
                    newBucket.getDescription() != null
                            ? newBucket.getDescription()
                            : "",
                    newBucket.getLabel().toString(),
                    newBucket.getAllocationPercent(),
                    newBucket.isControl() != null
                            ? newBucket.isControl()
                            : false,
                    newBucket.getPayload() != null
                            ? newBucket.getPayload()
                            : "",
                    Bucket.State.OPEN.toString());
        } catch (WasabiException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Could not create bucket \"" +
                    newBucket + "\"", e);
        }
    }

    @Override
    public Bucket updateBucket(Bucket bucket)
            throws RepositoryException {

        Preconditions.checkNotNull(bucket,
                "Parameter \"bucket\" cannot be null");
        Preconditions.checkNotNull(bucket.getExperimentID(),
                "Bucket experiment ID cannot be null");
        Preconditions.checkArgument(
                bucket.getLabel() != null
                        && !bucket.getLabel().toString().trim().isEmpty(),
                "Bucket external label cannot be null or an empty string");
        Preconditions.checkArgument(bucket.getAllocationPercent() >= 0d,
                "Bucket allocation percentage must be greater than or " +
                        "equal to 0.0");
        Preconditions.checkArgument(bucket.getAllocationPercent() <= 1d,
                "Bucket allocation percentage must be less than or equal to 1.0");


        //TODO: Check if you can combine the two queries into a single transaction (NOT a single query)


        if (bucket.isControl()) {
            final String SQL = "update bucket set is_control = false where experiment_id=?";
            newTransaction().update(
                    SQL,
                    bucket.getExperimentID());
        }


        final String SQL =
                "update bucket " +
                        "set description = ?, " +
                        "    allocation_percent = ?, " +
                        "    is_control = ?, " +
                        "    payload = ? " +
                        "where experiment_id=? and label=?";

        newTransaction().update(
                SQL,
                bucket.getDescription() != null
                        ? bucket.getDescription()
                        : "",
                bucket.getAllocationPercent(),
                bucket.isControl(),
                bucket.getPayload() != null
                        ? bucket.getPayload()
                        : "",
                bucket.getExperimentID(),
                bucket.getLabel().toString());

        return bucket;
    }

    /**
     * @param bucket                      bucket to update
     * @param desiredAllocationPercentage allocation information
     * @return bucket that were passed in????
     * @throws RepositoryException
     */
    @Override
    public Bucket updateBucketAllocationPercentage(Bucket bucket, Double desiredAllocationPercentage) throws
            RepositoryException {

        final String SQL =
                "update bucket " +
                        "set allocation_percent=? " +
                        "where experiment_id=? and label=?";

        newTransaction().update(
                SQL,
                desiredAllocationPercentage.toString(),
                bucket.getExperimentID(),
                bucket.getLabel().toString());

        return bucket;
    }

    @Override
    public Bucket updateBucketState(Bucket bucket, Bucket.State desiredState) throws RepositoryException {

        final String SQL =
                "update bucket " +
                        "set state = ? " +
                        "where experiment_id=? and label=?";

        newTransaction().update(
                SQL,
                desiredState.toString(),
                bucket.getExperimentID(),
                bucket.getLabel().toString());

        return bucket;
    }


    @Override
    public BucketList updateBucketBatch(Experiment.ID experimentID, BucketList bucketList)
            throws RepositoryException {

        int bucketListSize = bucketList.getBuckets().size();
        StringBuilder SQL = new StringBuilder("UPDATE bucket SET ");
        List<String> args = new ArrayList<>(bucketListSize);

        boolean hasval = false;
        for (int i = 0; i < bucketListSize; i++) {
            if (bucketList.getBuckets().get(i).getState() != null) {
                hasval = true;
                break;
            }
        }
        if (hasval) {
            SQL.append("state = CASE label ");
            for (int i = 0; i < bucketListSize; i++) {
                Bucket b = bucketList.getBuckets().get(i);
                if (b.getState() != null) {
                    SQL.append("WHEN ? then ? ");
                }
            }
            SQL.append("END,");
        }

        hasval = false;
        for (int i = 0; i < bucketListSize; i++) {
            if (bucketList.getBuckets().get(i).getAllocationPercent() != null) {
                hasval = true;
                break;
            }
        }
        if (hasval) {
            SQL.append("allocation_percent = CASE label ");
            for (int i = 0; i < bucketListSize; i++) {
                Bucket b = bucketList.getBuckets().get(i);
                if (b.getAllocationPercent() != null) {
                    SQL.append("WHEN ? then ? ");
                }
            }
            SQL.append("END,");
        }

        hasval = false;
        for (int i = 0; i < bucketListSize; i++) {
            if (bucketList.getBuckets().get(i).isControl() != null) {
                hasval = true;
                break;
            }
        }
        if (hasval) {
            SQL.append("is_control = CASE label ");
            for (int i = 0; i < bucketListSize; i++) {
                Bucket b = bucketList.getBuckets().get(i);
                if (b.isControl() != null) {
                    SQL.append("WHEN ? then ? ");
                }
            }
            SQL.append("END,");
        }

        hasval = false;
        for (int i = 0; i < bucketListSize; i++) {
            if (bucketList.getBuckets().get(i).getPayload() != null) {
                hasval = true;
                break;
            }
        }
        if (hasval) {
            SQL.append("payload = CASE label ");
            for (int i = 0; i < bucketListSize; i++) {
                Bucket b = bucketList.getBuckets().get(i);
                if (b.getPayload() != null) {
                    SQL.append("WHEN ? then ? ");
                }
            }
            SQL.append("END,");
        }

        hasval = false;
        for (int i = 0; i < bucketListSize; i++) {
            if (bucketList.getBuckets().get(i).getDescription() != null) {
                hasval = true;
                break;
            }
        }
        if (hasval) {
            SQL.append("description = CASE label ");
            for (int i = 0; i < bucketListSize; i++) {
                Bucket b = bucketList.getBuckets().get(i);
                if (b.getDescription() != null) {
                    SQL.append("WHEN ? then ? ");
                }
            }
            SQL.append("END,");
        }

        if (",".equals(SQL.substring(SQL.length() - 1, SQL.length()))) {
            SQL.setLength(SQL.length() - 1);
        }

        SQL.append(" WHERE experiment_id = ? and label in (");
        for (int i = 0; i < bucketListSize; i++) {
            SQL.append("?,");
        }
        SQL.setLength(SQL.length() - 1);
        SQL.append(")");


        for (int i = 0; i < bucketListSize; i++) {
            Bucket b = bucketList.getBuckets().get(i);
            if (b.getState() != null) {
                args.add(b.getLabel().toString());
                args.add(b.getState().toString());
            }
        }
        for (int i = 0; i < bucketListSize; i++) {
            Bucket b = bucketList.getBuckets().get(i);
            if (b.getAllocationPercent() != null) {
                args.add(b.getLabel().toString());
                args.add(b.getAllocationPercent().toString());
            }
        }
        for (int i = 0; i < bucketListSize; i++) {
            Bucket b = bucketList.getBuckets().get(i);
            if (b.isControl() != null) {
                args.add(b.getLabel().toString());
                args.add(b.isControl().toString());
            }
        }
        for (int i = 0; i < bucketListSize; i++) {
            Bucket b = bucketList.getBuckets().get(i);
            if (b.getPayload() != null) {
                args.add(b.getLabel().toString());
                args.add(b.getPayload());
            }
        }
        for (int i = 0; i < bucketListSize; i++) {
            Bucket b = bucketList.getBuckets().get(i);
            if (b.getDescription() != null) {
                args.add(b.getLabel().toString());
                args.add(b.getDescription());
            }
        }
        args.add(experimentID.toString());
        for (int i = 0; i < bucketListSize; i++) {
            args.add(bucketList.getBuckets().get(i).getLabel().toString());
        }

        newTransaction().update(SQL.toString(), args.toArray(new String[args.size()]));

        return bucketList;

    }

    @Override
    public void deleteBucket(Experiment.ID experimentID,
                             Bucket.Label bucketLabel) {

        int numRows = newTransaction().update(
                "delete from bucket where experiment_id=? and label=?",
                experimentID,
                bucketLabel);

        if (numRows < 1) {
            throw new BucketNotFoundException(bucketLabel);
        }
    }


    @Override
    public void logBucketChanges(Experiment.ID experimentID, Bucket.Label bucketLabel,
                                 List<Bucket.BucketAuditInfo> changeList)
            throws RepositoryException {
        //Doing nothing here, as audit is recorded on cassandra repository only
    }

    @Override
    public void logExperimentChanges(Experiment.ID experimentID, List<Experiment.ExperimentAuditInfo> changeList)
            throws RepositoryException {
        //Doing nothing here, as audit is recorded on cassandra repository only
    }

    @Override
    public List<Experiment> getExperiments(Application.Name appName) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported ");
    }

    @Override
    public void deleteExperiment(NewExperiment newExperiment) throws RepositoryException {
        int numRows = newTransaction().update(
                "DELETE FROM experiment WHERE experiment_id=?",
                newExperiment.getID());

        if (numRows < 1) {
            throw new ExperimentNotFoundException(newExperiment.getID());
        }
    }

    @Override
    public void createIndicesForNewExperiment(NewExperiment newExperiment) {
        throw new UnsupportedOperationException("No support for sql - indices are only created in Cassandra");
    }

    /**
     * Get a bucket list for a list of Experiments in a single cassandra call
     */
    @Override
    public HashMap<Experiment.ID, BucketList> getBucketList(Collection<Experiment.ID> experimentIDCollection) {
        throw new UnsupportedOperationException("Not supported ");
    }

    /**
     * Get the list of buckets for an experiment
     */
    @Override
    public BucketList getBucketList(Experiment.ID experimentID) {
        throw new UnsupportedOperationException("Not supported ");
    }

// TODO - Remove these methods since they expose astynax to the client
//    @Override
//    public void updateStateIndex(MutationBatch batch, Experiment experiment) throws ConnectionException {
//        throw new UnsupportedOperationException("Not supported ");
//
//    }

//    @Override
//    public Rows<Experiment.ID, String> getExperimentRows(Application.Name appName) {
//        throw new UnsupportedOperationException("Not supported ");
//    }

    @Override
    public BucketList getBuckets(Experiment.ID experimentID, boolean checkExperiment)
            throws RepositoryException {

        final String SQL_SELECT_ID =
                "select id from experiment " +
                        "where id=? and state != ?";

        List experiments = newTransaction().select(
                SQL_SELECT_ID,
                experimentID,
                DELETED.toString());

        if (experiments.size() == 0) {
            throw new ExperimentNotFoundException(experimentID);
        }

        final String SQL_SELECT_EXPERIMENT =
                "select label, allocation_percent, is_control, " +
                        "   payload, description " +
                        "from bucket " +
                        "where experiment_id=? order by id";


        List buckets = newTransaction().select(
                SQL_SELECT_EXPERIMENT,
                experimentID);

        BucketList returnBuckets = new BucketList();

        for (Object bucket : buckets) {
            Map bucketMap = (Map) bucket;

            Bucket.Label label = Bucket.Label.valueOf(
                    (String) bucketMap.get("label"));
            Bucket returnBuck = Bucket.newInstance(experimentID, label)
                    .withAllocationPercent((Double) bucketMap.get("allocation_percent"))
                    .withDescription((String) bucketMap.get("description"))
                    .withControl((Boolean) bucketMap.get("is_control"))
                    .withPayload((String) bucketMap.get("payload"))
                    .build();

            returnBuckets.addBucket(returnBuck);
        }

        return returnBuckets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statement createApplication(Application.Name applicationName) {
        throw new UnsupportedOperationException("Not supported ");
    }

    @Override
    public Map<Application.Name, Set<String>> getTagListForApplications(Collection<Application.Name> applicationNames) {
        throw new UnsupportedOperationException("Not supported ");
    }

    @Override
    public void updateStateIndex(Experiment experiment) {
        throw new UnsupportedOperationException("Not supported ");
    }

}
