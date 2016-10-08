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

import com.google.inject.Inject;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.ExperimentDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.PrioritiesRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ApplicationNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDListSerializer;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Rows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Cassandra priorities repository impl
 *
 * @see PrioritiesRepository
 */
public class CassandraPrioritiesRepository implements PrioritiesRepository {

    private final CassandraDriver driver;
    private final ExperimentsKeyspace keyspace;
    private final ExperimentRepository experimentRepository;

    @Inject
    public CassandraPrioritiesRepository(@CassandraRepository ExperimentRepository experimentRepository,
                                         @ExperimentDriver CassandraDriver driver, ExperimentsKeyspace keyspace)
            throws IOException, ConnectionException {

        super();
        this.experimentRepository = experimentRepository;
        this.driver = driver;
        this.keyspace = keyspace;
    }

    @Override
    public PrioritizedExperimentList getPriorities(Application.Name applicationName) {
        PrioritizedExperimentList prioritizedExperimentList = new PrioritizedExperimentList();

        List<Experiment.ID> priorityList = getPriorityList(applicationName);
        if (Objects.nonNull(priorityList)) {
            ExperimentList experimentList = experimentRepository.getExperiments(priorityList);
            int priorityValue = 1;
            for (Experiment experiment : experimentList.getExperiments()) {
                prioritizedExperimentList.
                        addPrioritizedExperiment(PrioritizedExperiment.from(experiment, priorityValue).build());
                priorityValue += 1;
            }
        }
        return prioritizedExperimentList;
    }

    /**
     * Returns the length of the priority list
     */
    @Override
    public int getPriorityListLength(Application.Name applicationName) {
        return getPriorityList(applicationName).size();
    }

    /**
     * Deletes existing priority list for an application, and inserts with new priority list.
     */
    @Override
    public void createPriorities(Application.Name applicationName, List<Experiment.ID> experimentPriorityList) {

        if (experimentPriorityList.isEmpty()) {

            final String CQL = "delete from application where app_name = ?";

            try {
                driver.getKeyspace().prepareQuery(keyspace.application_CF())
                        .withCql(CQL).asPreparedStatement()
                        // Application name
                        .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                        .execute().getResult();
            } catch (Exception e) {
                throw new RepositoryException("Unable to modify the priority list for the application: \""
                        + applicationName.toString() + "\"" + e);
            }
        } else {
            final String CQL = "insert into application(app_name,priorities) values(?,?)";

            try {
                driver.getKeyspace().prepareQuery(keyspace.application_CF())
                        .withCql(CQL).asPreparedStatement()
                        // Application name
                        .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                        // Experiment priority list
                        .withByteBufferValue(experimentPriorityList, ExperimentIDListSerializer.get())
                        .execute().getResult();
            } catch (Exception e) {
                throw new RepositoryException("Unable to modify the priority list for application: \""
                        + applicationName.toString() + "\"" + e);
            }
        }
    }

    /*
     * @see com.intuit.wasabi.repository.PrioritiesRepository#getPriorityList(com.intuit.wasabi.experimentobjects.Application.Name)
     */
    @Override
    public List<Experiment.ID> getPriorityList(Application.Name applicationName) {

        String CQL = "select * from application " +
                "where app_name = ?";

        try {

            Rows<Application.Name, String> rows = driver.getKeyspace()
                    .prepareQuery(keyspace.application_CF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                    .execute()
                    .getResult()
                    .getRows();

            if (!rows.isEmpty()) {

                return ExperimentIDListSerializer.get().fromByteBuffer(
                        rows.getRowByIndex(0).getColumns().
                                getColumnByName("priorities").getByteBufferValue());

            } else {
                return null;
            }

        } catch (ConnectionException e) {
            throw new RepositoryException("Unable to retrieve the priority list for application: \""
                    + applicationName.toString() + "\"" + e);
        }
    }

    /**
     * Get not exclusion list
     * TODO : More clarification
     *
     * @param base base experiment id
     * @return experiment list
     */
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
}
