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
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Result;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.UninterruptibleUtil;
import com.intuit.wasabi.repository.cassandra.accessor.ExclusionAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.Exclusion;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mutax repo cassandra implementation
 *
 * @see MutexRepository
 */
public class CassandraMutexRepository implements MutexRepository {

    /**
     * Logger for the class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraMutexRepository.class);

    private final ExperimentAccessor experimentAccessor;
    private final ExclusionAccessor mutexAccessor;

    private Session session;

    @Inject
    public CassandraMutexRepository(
            ExperimentAccessor experimentAccessor,
            ExclusionAccessor mutexAccessor,
            CassandraDriver driver)
            throws IOException, ConnectionException {
        this.experimentAccessor = experimentAccessor;
        this.session = driver.getSession();
        this.mutexAccessor = mutexAccessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> getExclusionList(Experiment.ID experimentID) {

        LOGGER.debug("Getting exclusions for {}", experimentID);

        List<Experiment.ID> exclusionIds = new ArrayList<>();

        try {
            Result<Exclusion> exclusions = mutexAccessor.getExclusions(experimentID.getRawID());

            for (Exclusion exclusion : exclusions.all()) {
                exclusionIds.add(Experiment.ID.valueOf(exclusion.getPair()));
            }


        } catch (Exception e) {
            LOGGER.error("Error while getting exclusions for {}", experimentID, e);
            throw new RepositoryException("Could not fetch exclusions for experiment \"" + experimentID + "\" ", e);
        }

        LOGGER.debug("Getting exclusions list of size {}", exclusionIds);

        return exclusionIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExclusion(Experiment.ID base, Experiment.ID pair) throws RepositoryException {
        LOGGER.debug("Deleting exclusions for base {} pair {}", new Object[]{base, pair});

        try {
            BatchStatement batchStatement = new BatchStatement();
            batchStatement.add(mutexAccessor.deleteExclusion(base.getRawID(),
                    pair.getRawID()));
            batchStatement.add(mutexAccessor.deleteExclusion(pair.getRawID(),
                    base.getRawID()));
            session.execute(batchStatement);
        } catch (Exception e) {
            LOGGER.error("Error while deleting exclusions for base {} pair {}",
                    new Object[]{base, pair}, e);
            throw new RepositoryException("Could not delete the exclusion \"" + base + "\", \"" + pair + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createExclusion(Experiment.ID baseID, Experiment.ID pairID) throws RepositoryException {

        LOGGER.debug("Create exclusions for {}", new Object[]{baseID, pairID});
        try {

            BatchStatement batchStatement = new BatchStatement();
            batchStatement.add(mutexAccessor.createExclusion(baseID.getRawID(),
                    pairID.getRawID()));
            batchStatement.add(mutexAccessor.createExclusion(pairID.getRawID(),
                    baseID.getRawID()));
            session.execute(batchStatement);

        } catch (Exception e) {
            LOGGER.error("Error while create exclusions for {}",
                    new Object[]{baseID, pairID}, e);
            throw new RepositoryException("Could not insert the exclusion \"" + baseID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getExclusions(Experiment.ID base) {

        LOGGER.debug("Getting exclusion list for {}", base);

        try {

            List<Exclusion> exclusions = mutexAccessor.getExclusions(base.getRawID()).all();

            List<UUID> experimentIds = new ArrayList<>();
            for (Exclusion exclusion : exclusions)
                experimentIds.add(exclusion.getPair());

            List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentPojos =
                    experimentAccessor.getExperiments(experimentIds).all();

            List<Experiment> experiments = new ArrayList<>();
            for (com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo : experimentPojos) {
                experiments.add(ExperimentHelper.makeExperiment(experimentPojo));
            }

            ExperimentList experimentList = new ExperimentList();
            experimentList.setExperiments(experiments);

            return experimentList;
        } catch (Exception e) {
            LOGGER.error("Error whil getting exclusion list for {}", base, e);
            throw new RepositoryException("Could not retrieve the exclusions for \"" + base + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getNotExclusions(Experiment.ID base) {

        LOGGER.debug("Getting not exclusions list for {}", base);

        try {
            List<Exclusion> exclusions = mutexAccessor.getExclusions(base.getRawID()).all();

            List<UUID> exclusionPairIds = new ArrayList<>();
            for (Exclusion exclusion : exclusions)
                exclusionPairIds.add(exclusion.getPair());

            //get info about the experiment
            com.intuit.wasabi.repository.cassandra.pojo.Experiment experiment =
                    experimentAccessor.getExperimentById(base.getRawID()).one();

            //get the application name
            String appName = experiment.getAppName();
            //get all experiments with this application name
            List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> ExpList =
                    experimentAccessor.getExperimentByAppName(appName).all();

            List<Experiment> notMutex = new ArrayList<>();
            for (com.intuit.wasabi.repository.cassandra.pojo.Experiment exp : ExpList) {
                if (!exclusionPairIds.contains(exp.getId()) &&
                        !exp.getId().equals(base.getRawID())) {
                    notMutex.add(ExperimentHelper.makeExperiment(exp));
                }
            }

            ExperimentList result = new ExperimentList();
            result.setExperiments(notMutex);

            LOGGER.debug("Returning exclusions list {} for {}", new Object[]{result, base});

            return result;
        } catch (Exception e) {
            LOGGER.debug("Error while getting not exclusions list for {}", base, e);
            throw new RepositoryException("Could not retrieve the exclusions for \"" + base + "\"", e);
        }
    }

    /**
     * Improved way (async) of getting mutually exclusive experiments for give experiments.
     */
    @Override
    public Map<Experiment.ID, List<Experiment.ID>> getExclusivesList(Collection<Experiment.ID> experimentIds) {
        LOGGER.debug("Getting exclusions for {}", experimentIds);
        Map<Experiment.ID, ListenableFuture<Result<Exclusion>>> exclusionFutureMap = new HashMap<>(experimentIds.size());
        Map<Experiment.ID, List<Experiment.ID>> exclusionMap = new HashMap<>(experimentIds.size());

        try {
            //Send calls asynchronously
            experimentIds.stream().forEach(experimentId -> {
                exclusionFutureMap.put(experimentId, mutexAccessor.asyncGetExclusions(experimentId.getRawID()));
                LOGGER.debug("Sent exclusionAccessor.asyncGetExclusions ({})", experimentId.getRawID());
            });

            //Process the Futures in the order that are expected to arrive earlier
            for (Experiment.ID expId : exclusionFutureMap.keySet()) {
                ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion>> exclusionFuture = exclusionFutureMap.get(expId);
                exclusionMap.put(expId, new ArrayList<>());
                UninterruptibleUtil.getUninterruptibly(exclusionFuture).all().forEach(exclusionPojo -> {
                            exclusionMap.get(expId).add(Experiment.ID.valueOf(exclusionPojo.getPair()));
                        }
                );
            }
        } catch (Exception e) {
            LOGGER.error("Error while getting exclusions for {}", experimentIds, e);
            throw new RepositoryException("Could not fetch mutually exclusive experiments for the list of experiments", e);
        }
        LOGGER.debug("Returning exclusions map {}", exclusionMap);
        return exclusionMap;
    }
}
