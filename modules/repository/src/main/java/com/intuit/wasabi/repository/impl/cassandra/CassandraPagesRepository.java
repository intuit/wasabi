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
import com.intuit.wasabi.experimentobjects.*;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.PagesRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ApplicationNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.PageNameSerializer;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.PreparedCqlQuery;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pages repo cassandra implementation
 *
 * @see PagesRepository
 */
public class CassandraPagesRepository implements PagesRepository {

    private static final List<Page> EMPTY = new ArrayList<Page>(1);
    private static final Logger LOGGER = getLogger(CassandraPagesRepository.class);
    private final CassandraDriver driver;
    private final ExperimentsKeyspace keyspace;
    private final ExperimentRepository experimentRepository;

    @Inject
    public CassandraPagesRepository(@CassandraRepository ExperimentRepository experimentRepository,
                                    @ExperimentDriver CassandraDriver driver, ExperimentsKeyspace keyspace)
            throws IOException, ConnectionException {

        super();
        this.experimentRepository = experimentRepository;
        this.driver = driver;
        this.keyspace = keyspace;
    }

    /**
     * Add a list of pages to an experiment
     */
    @Override
    public void postPages(Application.Name applicationName, Experiment.ID experimentID,
                          ExperimentPageList experimentPageList)
            throws RepositoryException {

        // For experiment audit log saving the current state of the experiment's page list
        ExperimentPageList oldPageList = getExperimentPages(experimentID);

        StringBuilder cqlQuery = new StringBuilder("begin batch ");
        MutationBatch m = driver.getKeyspace().prepareMutationBatch();
        for (ExperimentPage experimentPage : experimentPageList.getPages()) {
            cqlQuery.append("insert into page_experiment_index(app_name, page, exp_id, assign) " +
                    "values(?,?,?,?);");
            m.withRow(keyspace.experiment_page_CF(), experimentID)
                    .putColumn(experimentPage.getName(), experimentPage.getAllowNewAssignment());
            m.withRow(keyspace.app_page_index_CF(), applicationName)
                    .putColumn(experimentPage.getName().toString(), experimentPage.getAllowNewAssignment());
        }
        cqlQuery.append("apply batch;");
        PreparedCqlQuery<ExperimentsKeyspace.AppNamePageComposite, String> preparedQuery =
                driver.getKeyspace().prepareQuery(keyspace.page_experiment_index_CF())
                        .withCql(cqlQuery.toString())
                        .asPreparedStatement();
        for (ExperimentPage experimentPage : experimentPageList.getPages()) {
            // application name
            preparedQuery.withByteBufferValue(applicationName, ApplicationNameSerializer.get());
            // page
            preparedQuery.withByteBufferValue(experimentPage.getName(), PageNameSerializer.get());
            // experiment ID
            preparedQuery.withByteBufferValue(experimentID, ExperimentIDSerializer.get());
            // assign
            preparedQuery.withBooleanValue(experimentPage.getAllowNewAssignment());
        }
        try {
            // TODO: make the insert operation atomic or selectively revert the changes made
            preparedQuery.execute();
            m.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not add the page(s) to the experiment:\"" + experimentID + "\"", e);
        }

        // For experiment audit log saving the current state of the experiment's page list that has been updated
        ExperimentPageList newPageList = getExperimentPages(experimentID);
        saveExperimentPageState(experimentID, oldPageList, newPageList);
    }

    private void saveExperimentPageState(Experiment.ID experimentID, ExperimentPageList oldPageList,
                                         ExperimentPageList newPageList) {
        List<Experiment.ExperimentAuditInfo> changeList = new ArrayList<>();
        Experiment.ExperimentAuditInfo changeData;
        changeData = new Experiment.ExperimentAuditInfo("pages", oldPageList.toString(),
                newPageList.toString());
        changeList.add(changeData);
        experimentRepository.logExperimentChanges(experimentID, changeList);
    }

    @Override
    public void deletePage(Application.Name applicationName, Experiment.ID experimentID, Page.Name pageName) {
        // TODO: Selectively revert the changes in case of a failure. OR make the entire delete operation atomic

        // For experiment audit log saving the current state of the experiment's page list
        ExperimentPageList oldPageList = getExperimentPages(experimentID);

        // Deleting the data from page_experiment_index
        String cql = "delete from page_experiment_index where app_name = ? and page = ? and exp_id = ?";
        try {
            driver.getKeyspace().prepareQuery(keyspace.page_experiment_index_CF())
                    .withCql(cql).asPreparedStatement()
                    .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                    .withByteBufferValue(pageName, PageNameSerializer.get())
                    .withByteBufferValue(experimentID, ExperimentIDSerializer.get())
                    .execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not delete the row from page_experiment_index", e);
        }

        // Deleting the data from experiment_page
        MutationBatch mutationBatch = driver.getKeyspace().prepareMutationBatch();
        mutationBatch.withRow(keyspace.experiment_page_CF(), experimentID).deleteColumn(pageName);
        try {
            mutationBatch.execute();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not delete the row from experiment_page", e);
        }

        // For experiment audit log saving the current state of the experiment's page list that has been updated
        ExperimentPageList newPageList = getExperimentPages(experimentID);
        saveExperimentPageState(experimentID, oldPageList, newPageList);
    }

    /**
     * Get the names of all pages associated with an application
     */
    @Override
    public List<Page> getPageList(Application.Name applicationName) {

        try {
            Collection<String> pages = driver.getKeyspace()
                    .prepareQuery(keyspace.app_page_index_CF())
                    .getKey(applicationName)
                    .execute()
                    .getResult()
                    .getColumnNames();
            if (!pages.isEmpty()) {
                List<Page> pageList = new ArrayList<>(pages.size());
                for (String pageName : pages) {
                    pageList.add(new Page.Builder().withName(Page.Name.valueOf(pageName)).build());
                }
                return pageList;
            } else {
                return EMPTY;
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve the pages for application:\"" + applicationName + "\"", e);
        }
    }

    /**
     * Get the names of all pages and its associated experiments for an application
     */
    @Override
    public Map<Page.Name, List<PageExperiment>> getPageExperimentList(Application.Name applicationName) {
        Map<Page.Name, List<PageExperiment>> result = new HashMap<>();
        try {
            Collection<String> pages = driver.getKeyspace()
                    .prepareQuery(keyspace.app_page_index_CF())
                    .getKey(applicationName)
                    .execute()
                    .getResult()
                    .getColumnNames();
            if (!pages.isEmpty()) {
                for (String pageName : pages) {
                    Page page = new Page.Builder().withName(Page.Name.valueOf(pageName)).build();
                    List<PageExperiment> pageExperiments = getExperiments(applicationName, page.getName());
                    result.put(page.getName(), pageExperiments);
                }
            }
            return result;

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve the pages and its associated experiments for application:\"" + applicationName + "\"", e);
        }
    }

    /**
     * Get the page information(name and allowNewAssignment) for the associated pages for an experiment
     */
    @Override
    public ExperimentPageList getExperimentPages(Experiment.ID experimentID) {
        ExperimentPageList result;
        try {
            ColumnList<Page.Name> columnList = driver.getKeyspace()
                    .prepareQuery(keyspace.experiment_page_CF())
                    .getKey(experimentID).execute().getResult();
            result = new ExperimentPageList(columnList.size());
            for (Column<Page.Name> x : columnList) {
                result.addPage(ExperimentPage.withAttributes(x.getName(), x.getBooleanValue()).build());
            }
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve the pages for experiment: \"" + experimentID + "\"", e);
        }
        return result;
    }

    /**
     * Get the experiment information(id and allowNewAssignment) for the associated experiments for a page
     */
    @Override
    public List<PageExperiment> getExperiments(Application.Name applicationName, Page.Name pageName) {
        List<PageExperiment> result = new LinkedList<>();
        final String CQL = "select * from page_experiment_index where app_name = ? and page = ?";
        Rows<ExperimentsKeyspace.AppNamePageComposite, String> rows = executeGetExperimentQuery(applicationName,
                pageName, CQL);

        if (!rows.isEmpty()) {
            for (Row<ExperimentsKeyspace.AppNamePageComposite, String> row : rows) {
                Experiment.ID experimentID = Experiment.ID.valueOf(row.getColumns()
                        .getColumnByName("exp_id").getUUIDValue());
                Experiment experiment = experimentRepository.getExperiment(experimentID);
                if (experiment == null) {
                    continue;
                }
                Experiment.Label label = experiment.getLabel();
                boolean allowNewAssignment = row.getColumns().getColumnByName("assign").getBooleanValue();
                result.add(PageExperiment.withAttributes(experimentID, label, allowNewAssignment)
                        .build());
            }
        }
        return result;
    }

    Rows<ExperimentsKeyspace.AppNamePageComposite, String> executeGetExperimentQuery(Application.Name applicationName,
                                                                                     Page.Name pageName, String CQL) {
        try {
            return driver.getKeyspace()
                    .prepareQuery(keyspace.page_experiment_index_CF())
                    .withCql(CQL).asPreparedStatement()
                    .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                    .withByteBufferValue(pageName, PageNameSerializer.get())
                    .execute()
                    .getResult()
                    .getRows();
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve the experiments for applicationName:\"" +
                    applicationName + "\", page:\"" + pageName, e);
        }
    }

    /**
     * Erase the page related data associated to an experiment
     */
    @Override
    public void erasePageData(Application.Name applicationName, Experiment.ID experimentID) {

        // Get the list of associated pages for the experiment
        ExperimentPageList experimentPageList = getExperimentPages(experimentID);
        for (ExperimentPage experimentPage : experimentPageList.getPages()) {
            deletePage(applicationName, experimentID, experimentPage.getName());
            LOGGER.debug("CassandraPagesRepository Removing page: {} from terminated experiment: {} for application: {}",
                    experimentPage.getName(), experimentID, applicationName);
        }
    }
}
