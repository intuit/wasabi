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

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.UnavailableException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentPage;
import com.intuit.wasabi.experimentobjects.ExperimentPageList;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.repository.PagesRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.UninterruptibleUtil;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentPageAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.ExperimentAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.AppPageIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.PageExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.AppPage;
import com.intuit.wasabi.repository.cassandra.pojo.index.PageExperimentByAppNamePage;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CassandraPagesRepository implements PagesRepository {
    private final Logger logger = LoggerFactory.getLogger(CassandraPagesRepository.class);
    private final PageExperimentIndexAccessor pageExperimentIndexAccessor;
    private final ExperimentPageAccessor experimentPageAccessor;
    private final AppPageIndexAccessor appPageIndexAccessor;
    private final ExperimentAuditLogAccessor experimentAuditLogAccessor;
    private final MappingManager mappingManager;
    private final ExperimentAccessor experimentAccessor;

    @Inject
    public CassandraPagesRepository(ExperimentAccessor experimentAccessor,
                                    PageExperimentIndexAccessor pageExperimentIndexAccessor,
                                    ExperimentPageAccessor experimentPageAccessor,
                                    AppPageIndexAccessor appPageIndexAccessor,
                                    ExperimentAuditLogAccessor experimentAuditLogAccessor,
                                    MappingManager mappingManager) {
        this.experimentAccessor = experimentAccessor;
        this.pageExperimentIndexAccessor = pageExperimentIndexAccessor;
        this.experimentPageAccessor = experimentPageAccessor;
        this.appPageIndexAccessor = appPageIndexAccessor;
        this.experimentAuditLogAccessor = experimentAuditLogAccessor;
        this.mappingManager = mappingManager;
    }

    @Override
    public void postPages(Application.Name applicationName, Experiment.ID experimentID, ExperimentPageList experimentPageList) throws RepositoryException {
        ExperimentPageList oldPageList = getExperimentPages(experimentID);
        BatchStatement batch = new BatchStatement();
        for (ExperimentPage experimentPage : experimentPageList.getPages()) {
            batch.add(pageExperimentIndexAccessor.insertBy(
                    applicationName.toString(),
                    experimentPage.getName().toString(),
                    experimentID.getRawID(),
                    experimentPage.getAllowNewAssignment()
            ));
            batch.add(experimentPageAccessor.insertBy(
                    experimentPage.getName().toString(),
                    experimentID.getRawID(),
                    experimentPage.getAllowNewAssignment()
            ));
            batch.add(appPageIndexAccessor.insertBy(
                    applicationName.toString(),
                    experimentPage.getName().toString()
            ));
        }
        executeBatchStatement(experimentID, batch);
        ExperimentPageList newPageList = getExperimentPages(experimentID);
        saveExperimentPageState(experimentID, oldPageList, newPageList);
    }

    void executeBatchStatement(Experiment.ID experimentID, BatchStatement batch) {
        try {
            ResultSet resultSet = mappingManager.getSession().execute(batch);
            logger.debug("Batch statement is applied: {} using consistency level: {}",
                    resultSet.wasApplied(),
                    resultSet.getExecutionInfo().getAchievedConsistencyLevel());
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not add the page(s) to the experiment:\"" + experimentID + "\"", e);
        }
    }

    void saveExperimentPageState(Experiment.ID experimentID, ExperimentPageList oldPageList, ExperimentPageList newPageList) {
        try {
            this.experimentAuditLogAccessor.insertBy(
                    experimentID.getRawID(),
                    new Date(),
                    "pages",
                    oldPageList.toString(),
                    newPageList.toString()
            );
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not write pages change to audit log: \"" + experimentID + "\"", e);
        }
    }

    @Override
    public void deletePage(Application.Name applicationName, Experiment.ID experimentID, Page.Name pageName) {
        // For experiment audit log saving the current state of the experiment's page list
        ExperimentPageList oldPageList = getExperimentPages(experimentID);

        BatchStatement batch = new BatchStatement();
        batch.add(pageExperimentIndexAccessor.deleteBy(
                applicationName.toString(),
                pageName.toString(),
                experimentID.getRawID()
        ));
        batch.add(experimentPageAccessor.deleteBy(pageName.toString(), experimentID.getRawID()));
        //TODO: original code does not have this
//        batch.add(appPageIndexAccessor.deleteBy(pageName.toString(), applicationName.toString()));
        executeBatchStatement(experimentID, batch);
        // For experiment audit log saving the current state of the experiment's page list that has been updated
        ExperimentPageList newPageList = getExperimentPages(experimentID);
        saveExperimentPageState(experimentID, oldPageList, newPageList);
    }

    @Override
    public List<Page> getPageList(Application.Name applicationName) {
        Stream<AppPage> resultList = getAppPagesFromCassandra(applicationName);
        return resultList
                .map(t -> new Page.Builder().withName(Page.Name.valueOf(t.getPage())).build())
                .collect(Collectors.toList());
    }

    Stream<AppPage> getAppPagesFromCassandra(Application.Name applicationName) {
        Optional<Iterator<AppPage>> optionalResult = Optional.empty();
        try {
            optionalResult = Optional.ofNullable(appPageIndexAccessor.selectBy(applicationName.toString()).iterator());
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve the pages and its associated experiments for application:\""
                    + applicationName + "\"", e);
        }
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(optionalResult.orElse(Collections.emptyIterator())
                        , Spliterator.ORDERED), false);
    }

    @Override
    public Map<Page.Name, List<PageExperiment>> getPageExperimentList(Application.Name applicationName) {

        Stream<AppPage> resultList = getAppPagesFromCassandra(applicationName);
        ImmutableMap.Builder<Page.Name, List<PageExperiment>> result = ImmutableMap.builder();
        resultList.forEach(t -> {
            Page page = new Page.Builder().withName(Page.Name.valueOf(t.getPage())).build();
            //TODO: DB change to reduce this call per page, this call may return pageexperiment not owned by the current user
            List<PageExperiment> pageExperiments = getExperiments(applicationName, page.getName());
            result.put(page.getName(), pageExperiments);
        });
        return result.build();
    }

    @Override
    public ExperimentPageList getExperimentPages(Experiment.ID experimentID) {
        ExperimentPageList experimentPageList = new ExperimentPageList();
        try {
            Result<PageExperimentByAppNamePage> result = experimentPageAccessor.selectBy(experimentID.getRawID());
            Stream<PageExperimentByAppNamePage> resultList = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false);

            List<ExperimentPage> experimentPages = resultList
                    .map(t ->
                            ExperimentPage.withAttributes(
                                    Page.Name.valueOf(t.getPage()),
                                    t.isAssign()
                            ).build()
                    ).collect(Collectors.toList());
            experimentPageList.setPages(experimentPages);
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve the pages for experiment: \"" + experimentID + "\"", e);
        }
        return experimentPageList;
    }

    @Override
    public List<PageExperiment> getExperiments(Application.Name applicationName, Page.Name pageName) {
        Stream<PageExperimentByAppNamePage> resultList = Stream.empty();
        try {
            Result<PageExperimentByAppNamePage> result = pageExperimentIndexAccessor.selectBy(applicationName.toString(), pageName.toString());
            resultList = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false);
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException(new StringBuilder("Could not retrieve the experiments for applicationName:\"").append(applicationName).append("\", page:\"").append(pageName).append("\"").toString(), e);
        }
        //TODO: make the experiment label part of the pageExperimentIndex to save a query per page
        return resultList
                .map(t -> {
                            Optional<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experiment =
                                    Optional.ofNullable(experimentAccessor.selectBy(t.getExperimentId()).one());
                            PageExperiment.Builder builder = new PageExperiment.Builder(
                                    Experiment.ID.valueOf(t.getExperimentId()),
                                    null,
                                    t.isAssign()
                            );
                            if (experiment.isPresent()) {
                                builder.setLabel(Experiment.Label.valueOf(experiment.get().getLabel()));
                            }
                            return builder.build();
                        }
                ).filter(t -> t.getLabel() != null)
                .collect(Collectors.toList());
    }

    @Override
    public List<PageExperiment> getExperimentsWithoutLabels(Application.Name applicationName, Page.Name pageName) {
        Stream<PageExperimentByAppNamePage> resultList = Stream.empty();
        try {
            Result<PageExperimentByAppNamePage> result = pageExperimentIndexAccessor.selectBy(applicationName.toString(), pageName.toString());
            resultList = StreamSupport.stream(Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false);
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve the experiments for applicationName:\"" + applicationName + "\", page:\"" + pageName, e);
        }

        return resultList.map(t -> {
                    PageExperiment.Builder builder = new PageExperiment.Builder(
                            Experiment.ID.valueOf(t.getExperimentId()),
                            null,
                            t.isAssign()
                    );
                    return builder.build();
                }
        ).collect(Collectors.toList());
    }

    @Override
    public Map<Pair<Application.Name, Page.Name>, List<PageExperiment>> getExperimentsWithoutLabels
            (Collection<Pair<Application.Name, Page.Name>> appAndPagePairs) {
        logger.debug("getExperimentsWithoutLabels {}", appAndPagePairs);
        Map<Pair<Application.Name, Page.Name>, List<PageExperiment>> resultMap = new HashMap<>();
        try {
            Map<Pair<Application.Name, Page.Name>, ListenableFuture<Result<PageExperimentByAppNamePage>>> expFutureMap = new HashMap<>();
            appAndPagePairs.forEach(pair -> {
                expFutureMap.put(pair, pageExperimentIndexAccessor.asyncSelectBy(pair.getLeft().toString(), pair.getRight().toString()));
            });

            for (Pair<Application.Name, Page.Name> pair : expFutureMap.keySet()) {
                ListenableFuture<Result<PageExperimentByAppNamePage>> expFuture = expFutureMap.get(pair);
                Stream<PageExperimentByAppNamePage> resultList = StreamSupport.stream(Spliterators.spliteratorUnknownSize(UninterruptibleUtil.getUninterruptibly(expFuture).iterator(), Spliterator.ORDERED), false);
                List<PageExperiment> pageExperimentsList = resultList.map(t -> {
                            PageExperiment.Builder builder = new PageExperiment.Builder(
                                    Experiment.ID.valueOf(t.getExperimentId()),
                                    null,
                                    t.isAssign()
                            );
                            return builder.build();
                        }
                ).collect(Collectors.toList());

                resultMap.put(pair, pageExperimentsList);
            }
        } catch (Exception e) {
            logger.error("getExperimentsWithoutLabels for {} failed", appAndPagePairs, e);
            throw new RepositoryException("Could not getExperimentsWithoutLabels", e);
        }

        logger.debug("Returning PageExperimentList map {}", resultMap);
        return resultMap;
    }

    @Override
    public void erasePageData(Application.Name applicationName, Experiment.ID experimentID) {
        ExperimentPageList experimentPageList = getExperimentPages(experimentID);
        for (ExperimentPage experimentPage : experimentPageList.getPages()) {
            deletePage(applicationName, experimentID, experimentPage.getName());
            logger.debug("CassandraPagesRepository Removing page: {} from terminated experiment: {} for application: {}",
                    experimentPage.getName(), experimentID, applicationName);
        }
    }
}