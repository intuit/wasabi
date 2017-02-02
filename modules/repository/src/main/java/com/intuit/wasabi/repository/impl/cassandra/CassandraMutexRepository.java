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
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.ExperimentDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.query.PreparedCqlQuery;

import java.io.IOException;

/**
 * Mutax repo cassandra implementation
 *
 * @see MutexRepository
 */
public class CassandraMutexRepository implements MutexRepository {

    private final CassandraDriver driver;
    private final ExperimentsKeyspace keyspace;
    private final ExperimentRepository experimentRepository;

    /**
     * Constructor
     *
     * @param experimentRepository cassandra repository
     * @param driver               cassandra driver
     * @param keyspace             cassandra keyspace
     * @throws IOException         io exception
     * @throws ConnectionException connection exception
     */
    @Inject
    public CassandraMutexRepository(
            @CassandraRepository ExperimentRepository experimentRepository,
            @ExperimentDriver CassandraDriver driver,
            ExperimentsKeyspace keyspace)
            throws IOException, ConnectionException {

        super();
        this.experimentRepository = experimentRepository;
        this.driver = driver;
        this.keyspace = keyspace;
    }

    /**
     * Get the list of experiment ID's mutually exclusive to the given experiment
     */
    @Override
    public List<Experiment.ID> getExclusionList(Experiment.ID experimentID) {
        try {
            return new ArrayList<>(driver.getKeyspace()
                    .prepareQuery(keyspace.exclusion_CF())
                    .getKey(experimentID)
                    .execute()
                    .getResult()
                    .getColumnNames());
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not fetch exclusions for experiment \"" + experimentID + "\" ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExclusion(Experiment.ID base, Experiment.ID pair) throws RepositoryException {
        final String CQL = "begin batch " +
                "delete from exclusion where base = ? and pair = ?; " +
                "delete from exclusion where base = ? and pair = ?; " +
                "apply batch;";

        try {
            driver.getKeyspace()
                    .prepareQuery(keyspace.exclusion_CF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    // baseID for exclusion-1
                    .withByteBufferValue(base, ExperimentIDSerializer.get())
                    // pairID for exclusion-1
                    .withByteBufferValue(pair, ExperimentIDSerializer.get())
                    // baseID for exclusion-2
                    .withByteBufferValue(pair, ExperimentIDSerializer.get())
                    // pairID for exclusion-2
                    .withByteBufferValue(base, ExperimentIDSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not delete the exclusion \"" + base + "\", \"" + pair + "\"", e);
        }
    }

    @Override
    public void createExclusion(Experiment.ID baseID, Experiment.ID pairID) throws RepositoryException {

        StringBuilder cqlBuilder = new StringBuilder("begin batch ");
        // For exclusion baseID -> pairID
        cqlBuilder.append("insert into exclusion(base,pair) values(?,?) ; ");

        // For exclusion pairID -> baseID
        cqlBuilder.append("insert into exclusion(base,pair) values(?,?) ; ");
        cqlBuilder.append("apply batch;");
        try {
            PreparedCqlQuery<Experiment.ID, Experiment.ID> cqlQuery =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.exclusion_CF()).withCql(cqlBuilder.toString())
                            .asPreparedStatement();

            // For exclusion baseID -> pairID
            cqlQuery.withByteBufferValue(baseID, ExperimentIDSerializer.get());
            cqlQuery.withByteBufferValue(pairID, ExperimentIDSerializer.get());

            // For exclusion pairID -> baseID
            cqlQuery.withByteBufferValue(pairID, ExperimentIDSerializer.get());
            cqlQuery.withByteBufferValue(baseID, ExperimentIDSerializer.get());

            cqlQuery.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not insert the exclusion \"" + baseID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getExclusions(Experiment.ID base) {
        try {
            Collection<Experiment.ID> pairIDs = driver.getKeyspace().prepareQuery(keyspace.exclusion_CF())
                    .getKey(base).execute().getResult().getColumnNames();

            return experimentRepository.getExperiments(new ArrayList<>(pairIDs));
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve the exclusions for \"" + base + "\"", e);
        }
    }

    /**
     * Get non exclusion for experiment
     */
    @Override
    public ExperimentList getNotExclusions(Experiment.ID base) {
        try {

            Collection<Experiment.ID> pairIDs = driver.getKeyspace().prepareQuery(keyspace.exclusion_CF())
                    .getKey(base).execute().getResult().getColumnNames();

            //get info about the experiment
            Experiment experiment = experimentRepository.getExperiment(base);
            //get the application name
            Application.Name appName = experiment.getApplicationName();
            //get all experiments with this application name
            List<Experiment> ExpList = experimentRepository.getExperiments(appName);
            List<Experiment> notMutex = new ArrayList<>();
            for (Experiment exp : ExpList) {
                if (!pairIDs.contains(exp.getID()) && !exp.getID().equals(base)) {
                    notMutex.add(exp);
                }
            }

            ExperimentList result = new ExperimentList();
            result.setExperiments(notMutex);
            return result;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve the exclusions for \"" + base + "\"", e);
        }
    }

    /**
     * Get a list of mutually exclusive experiments for an experiment in a single cassandra call
     */
    @Override
    public Map<Experiment.ID, List<Experiment.ID>> getExclusivesList(Collection<Experiment.ID> experimentIDCollection) {
        Map<Experiment.ID, List<Experiment.ID>> result = new HashMap<>(experimentIDCollection.size());

        try {
            for (Row<Experiment.ID, Experiment.ID> row : driver.getKeyspace()
                    .prepareQuery(keyspace.exclusion_CF())
                    .getRowSlice(experimentIDCollection)
                    .execute()
                    .getResult()) {
                result.put(row.getKey(), new ArrayList<>(row.getColumns().getColumnNames()));
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not fetch mutually exclusive experiments for the list of experiments"
                    , e);
        }
        return result;
    }
}
