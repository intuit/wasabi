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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.PrioritiesRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.UninterruptibleUtil;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

/**
 * Cassandra priorities repository implementation
 *
 * @see PrioritiesRepository
 */
public class CassandraPrioritiesRepository implements PrioritiesRepository {

    private PrioritiesAccessor prioritiesAccessor;

    private ExperimentAccessor experimentAccessor;

    /**
     * Logger for the class
     */
    protected static final Logger LOGGER = LoggerFactory
            .getLogger(CassandraPrioritiesRepository.class);

    @Inject
    public CassandraPrioritiesRepository(PrioritiesAccessor prioritiesAccessor,
                                         ExperimentAccessor experimentAccessor) {

        this.prioritiesAccessor = prioritiesAccessor;
        this.experimentAccessor = experimentAccessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrioritizedExperimentList getPriorities(
            Application.Name applicationName) {

        LOGGER.debug("Getting priorities for {} ", applicationName);

        PrioritizedExperimentList prioritizedExperimentList = new PrioritizedExperimentList();

        try {
            List<ID> priorityList = getPriorityList(applicationName);

            LOGGER.debug("Received priorities list {} for {} ", new Object[]{
                    priorityList, applicationName});

            if (priorityList != null) {

                final List<UUID> priorityUUIDs = priorityList.stream()
                        .map(id -> id.getRawID()).collect(Collectors.toList());

                List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentPojos =
                        priorityUUIDs.stream().map(uuid -> experimentAccessor.getExperimentById(uuid).one())
                                .collect(Collectors.toList());

                LOGGER.debug("Received experimentPojos {} for priorityUUIDs {}",
                        new Object[]{experimentPojos, priorityUUIDs});

                int priorityValue = 1;
                for (com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo : experimentPojos) {
                    prioritizedExperimentList
                            .addPrioritizedExperiment(PrioritizedExperiment
                                    .from(ExperimentHelper
                                                    .makeExperiment(experimentPojo),
                                            priorityValue).build());

                    priorityValue += 1;
                }
            }

        } catch (Exception e) {
            LOGGER.error("Exception while getting priority list for {} ",
                    new Object[]{applicationName}, e);
            throw new RepositoryException(
                    "Unable to retrieve the priority list for application: \""
                            + applicationName.toString() + "\"" + e);

        }

        LOGGER.debug("Returning prioritizedExperimentList {} ", prioritizedExperimentList);

        return prioritizedExperimentList;
    }

    /**
     * Returns the priority list for given set of applications
     *
     * @param applicationNames Set of application names
     * @return Map of PrioritizedExperimentList prioritized experiments for given application names.
     */
    @Override
    public Map<Application.Name, PrioritizedExperimentList> getPriorities(Collection<Application.Name> applicationNames) {
        Map<Application.Name, ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Application>>> prioritiesFutureMap = new HashMap<>(applicationNames.size());
        Map<Application.Name, PrioritizedExperimentList> appPrioritiesMap = new HashMap<>(applicationNames.size());
        Map<Application.Name, ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>>> experimentsFutureMap = new HashMap<>(applicationNames.size());

        try {
            //Send calls asynchronously
            applicationNames.forEach(appName -> {
                experimentsFutureMap.put(appName, experimentAccessor.asyncGetExperimentByAppName(appName.toString()));
                LOGGER.debug("Sent experimentAccessor.asyncGetExperimentByAppName({})", appName);

                prioritiesFutureMap.put(appName, prioritiesAccessor.asyncGetPriorities(appName.toString()));
                LOGGER.debug("Sent prioritiesAccessor.asyncGetPriorities ({})", appName);
            });

            //Process the Futures in the order that are expected to arrive earlier
            Map<Experiment.ID, Experiment> experimentMap = new HashMap<>();
            for (Application.Name appName : experimentsFutureMap.keySet()) {
                ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>> experimentsFuture = experimentsFutureMap.get(appName);
                UninterruptibleUtil.getUninterruptibly(experimentsFuture).all().stream().forEach(expPojo -> {
                    Experiment exp = ExperimentHelper.makeExperiment(expPojo);
                    experimentMap.put(exp.getID(), exp);
                });
            }
            LOGGER.debug("experimentMap=> {}", experimentMap);

            for (Application.Name appName : prioritiesFutureMap.keySet()) {
                ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Application>> applicationFuture = prioritiesFutureMap.get(appName);
                PrioritizedExperimentList prioritizedExperimentList = new PrioritizedExperimentList();
                int priorityValue = 1;
                for (com.intuit.wasabi.repository.cassandra.pojo.Application priority : UninterruptibleUtil.getUninterruptibly(applicationFuture).all()) {
                    for (UUID uuid : priority.getPriorities()) {
                        Experiment exp = experimentMap.get(Experiment.ID.valueOf(uuid));
                        if (nonNull(exp)) {
                            prioritizedExperimentList.addPrioritizedExperiment(PrioritizedExperiment.from(exp, priorityValue).build());
                            priorityValue += 1;
                        } else {
                            LOGGER.error("DATA_INCONSISTENCY: Experiment with ID={} is present in the priority list of 'application' table but there is no respective entry in the 'experiment' table ...", uuid);
                        }
                    }
                }
                if (LOGGER.isDebugEnabled()) {
                    for (PrioritizedExperiment exp : prioritizedExperimentList.getPrioritizedExperiments()) {
                        LOGGER.debug("prioritizedExperiment=> {} ", exp);
                    }
                }
                appPrioritiesMap.put(appName, prioritizedExperimentList);
            }
        } catch (Exception e) {
            LOGGER.error("Error while getting priorities for {}", applicationNames, e);
            throw new RepositoryException("Error while getting priorities for given applications", e);
        }
        LOGGER.debug("Returning app priorities map {}", appPrioritiesMap);
        return appPrioritiesMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriorityListLength(Application.Name applicationName) {

        LOGGER.debug("Getting priority list length for {}", applicationName);

        return getPriorityList(applicationName).size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPriorities(Application.Name applicationName,
                                 List<Experiment.ID> priorityIds) {

        LOGGER.debug("Creating priority list for {} and ids {}",
                applicationName, priorityIds);

        if (priorityIds.isEmpty()) {

            LOGGER.debug("Deleting priority list for {} and ids {}",
                    applicationName, priorityIds);
            try {
                prioritiesAccessor.deletePriorities(applicationName.toString());
            } catch (Exception e) {
                LOGGER.error(
                        "Exception while deleting priority list for {} and ids {}",
                        new Object[]{applicationName, priorityIds}, e);

                throw new RepositoryException(
                        "Unable to delete the priority list for the application: \""
                                + applicationName.toString() + "\"" + e);
            }

        } else {

            LOGGER.debug("Updating priority list for {} and ids {}",
                    applicationName, priorityIds);

            List<UUID> experimentIds = new ArrayList<>();
            for (Experiment.ID experimentId : priorityIds) {
                experimentIds.add(experimentId.getRawID());
            }

            try {
                prioritiesAccessor.updatePriorities(experimentIds,
                        applicationName.toString());
            } catch (Exception e) {
                LOGGER.error(
                        "Exception while updating priority list for {} and ids {}",
                        new Object[]{applicationName, priorityIds}, e);

                throw new RepositoryException(
                        "Unable to modify the priority list " + experimentIds
                                + " for application: \""
                                + applicationName.toString() + "\"" + e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> getPriorityList(Application.Name applicationName) {

        LOGGER.debug("Getting priority list  for {} ", applicationName);

        List<Experiment.ID> experimentIds = new ArrayList<>();
        try {
            Result<com.intuit.wasabi.repository.cassandra.pojo.Application> priorities = prioritiesAccessor
                    .getPriorities(applicationName.toString());
            for (com.intuit.wasabi.repository.cassandra.pojo.Application priority : priorities
                    .all()) {
                for (UUID uuid : priority.getPriorities()) {
                    experimentIds.add(Experiment.ID.valueOf(uuid));
                }
            }

        } catch (Exception e) {
            LOGGER.error("Exception while getting priority list for {} ",
                    new Object[]{applicationName}, e);
            throw new RepositoryException(
                    "Unable to retrieve the priority list for application: \""
                            + applicationName.toString() + "\"" + e);
        }

        return experimentIds;
    }
}
