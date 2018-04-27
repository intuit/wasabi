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
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.BucketAuditInfo;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ExperimentAuditInfo;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.Experiment.State;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.UninterruptibleUtil;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.BucketAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentTagAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.BucketAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.ExperimentAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentLabelIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentState;
import com.intuit.wasabi.repository.cassandra.accessor.index.StateExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentByAppNameLabel;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentTagsByApplication;
import com.intuit.wasabi.repository.cassandra.pojo.index.StateExperimentIndex;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cassandra experiment repository
 *
 * @see ExperimentRepository
 */
public class CassandraExperimentRepository implements ExperimentRepository {

    private final ExperimentValidator validator;

    private CassandraDriver driver;

    private ExperimentAccessor experimentAccessor;

    private ExperimentLabelIndexAccessor experimentLabelIndexAccessor;

    private BucketAccessor bucketAccessor;

    private ApplicationListAccessor applicationListAccessor;

    private StateExperimentIndexAccessor stateExperimentIndexAccessor;

    private BucketAuditLogAccessor bucketAuditLogAccessor;

    private ExperimentAuditLogAccessor experimentAuditLogAccessor;

    private ExperimentTagAccessor experimentTagAccessor;

    //@TODO This and other accessors can be declared final as/if they are being Injected in constructor using guice.
    private PrioritiesAccessor prioritiesAccessor;

    /**
     * @return the experimentAccessor
     */
    public ExperimentAccessor getExperimentAccessor() {
        return experimentAccessor;
    }

    /**
     * @param experimentAccessor the experimentAccessor to set
     */
    public void setExperimentAccessor(ExperimentAccessor experimentAccessor) {
        this.experimentAccessor = experimentAccessor;
    }

    /**
     * @return the experimentLabelIndexAccessor
     */
    public ExperimentLabelIndexAccessor getExperimentLabelIndexAccessor() {
        return experimentLabelIndexAccessor;
    }

    /**
     * @param experimentLabelIndexAccessor the experimentLabelIndexAccessor to set
     */
    public void setExperimentLabelIndexAccessor(
            ExperimentLabelIndexAccessor experimentLabelIndexAccessor) {
        this.experimentLabelIndexAccessor = experimentLabelIndexAccessor;
    }

    /**
     * @return the bucketAccessor
     */
    public BucketAccessor getBucketAccessor() {
        return bucketAccessor;
    }

    /**
     * @param bucketAccessor the bucketAccessor to set
     */
    public void setBucketAccessor(BucketAccessor bucketAccessor) {
        this.bucketAccessor = bucketAccessor;
    }

    /**
     * @return the applicationListAccessor
     */
    public ApplicationListAccessor getApplicationListAccessor() {
        return applicationListAccessor;
    }

    /**
     * @param applicationListAccessor the applicationListAccessor to set
     */
    public void setApplicationListAccessor(
            ApplicationListAccessor applicationListAccessor) {
        this.applicationListAccessor = applicationListAccessor;
    }

    /**
     * @return the stateExperimentIndexAccessor
     */
    public StateExperimentIndexAccessor getStateExperimentIndexAccessor() {
        return stateExperimentIndexAccessor;
    }

    /**
     * @param stateExperimentIndexAccessor the stateExperimentIndexAccessor to set
     */
    public void setStateExperimentIndexAccessor(
            StateExperimentIndexAccessor stateExperimentIndexAccessor) {
        this.stateExperimentIndexAccessor = stateExperimentIndexAccessor;
    }

    /**
     * @return the bucketAuditLogAccessor
     */
    public BucketAuditLogAccessor getBucketAuditLogAccessor() {
        return bucketAuditLogAccessor;
    }

    /**
     * @param bucketAuditLogAccessor the bucketAuditLogAccessor to set
     */
    public void setBucketAuditLogAccessor(
            BucketAuditLogAccessor bucketAuditLogAccessor) {
        this.bucketAuditLogAccessor = bucketAuditLogAccessor;
    }

    /**
     * @return the experimentAuditLogAccessor
     */
    public ExperimentAuditLogAccessor getExperimentAuditLogAccessor() {
        return experimentAuditLogAccessor;
    }

    /**
     * @param experimentAuditLogAccessor the experimentAuditLogAccessor to set
     */
    public void setExperimentAuditLogAccessor(
            ExperimentAuditLogAccessor experimentAuditLogAccessor) {
        this.experimentAuditLogAccessor = experimentAuditLogAccessor;
    }

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = getLogger(CassandraExperimentRepository.class);

    @Inject
    public CassandraExperimentRepository(CassandraDriver driver,
                                         ExperimentAccessor experimentAccessor,
                                         ExperimentLabelIndexAccessor experimentLabelIndexAccessor,
                                         BucketAccessor bucketAccessor,
                                         ApplicationListAccessor applicationListAccessor,
                                         BucketAuditLogAccessor bucketAuditLogAccessor,
                                         ExperimentAuditLogAccessor experimentAuditLogAccessor,
                                         StateExperimentIndexAccessor stateExperimentIndexAccessor,
                                         ExperimentValidator validator,
                                         ExperimentTagAccessor experimentTagAccessor,
                                         PrioritiesAccessor prioritiesAccessor) {
        this.driver = driver;
        this.experimentAccessor = experimentAccessor;
        this.experimentLabelIndexAccessor = experimentLabelIndexAccessor;
        this.bucketAccessor = bucketAccessor;
        this.applicationListAccessor = applicationListAccessor;
        this.stateExperimentIndexAccessor = stateExperimentIndexAccessor;
        this.bucketAuditLogAccessor = bucketAuditLogAccessor;
        this.experimentAuditLogAccessor = experimentAuditLogAccessor;
        this.validator = validator;
        this.experimentTagAccessor = experimentTagAccessor;
        this.prioritiesAccessor = prioritiesAccessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment getExperiment(Experiment.ID experimentID) {
        LOGGER.debug("Getting experiment", experimentID);

        return internalGetExperiment(experimentID);
    }

    /**
     * {@inheritDoc}
     */
    protected Experiment internalGetExperiment(Experiment.ID experimentID) {

        LOGGER.debug("Getting experiment {}", experimentID);

        Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");

        try {

            com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo = experimentAccessor
                    .getExperimentById(experimentID.getRawID()).one();

            LOGGER.debug("Experiment retrieved {}", experimentPojo);

            if (experimentPojo == null)
                return null;

            if (State.DELETED.name().equals(experimentPojo.getState()))
                return null;

            return ExperimentHelper.makeExperiment(experimentPojo);
        } catch (Exception e) {
            LOGGER.error("Exception while getting experiment {}", experimentID);
            throw new RepositoryException("Could not retrieve experiment with ID \"" + experimentID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Application.Name, List<Experiment>> getExperimentsForApps(Collection<Application.Name> appNames) {
        Map<Application.Name, ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>>> experimentsFutureMap = new HashMap<>(appNames.size());
        Map<Application.Name, List<Experiment>> experimentMap = new HashMap<>(appNames.size());

        try {
            //Send calls asynchronously
            appNames.forEach(appName -> {
                experimentsFutureMap.put(appName, experimentAccessor.asyncGetExperimentByAppName(appName.toString()));
                LOGGER.debug("Sent experimentAccessor.asyncGetExperimentByAppName({})", appName);
            });

            //Process the Futures in the order that are expected to arrive earlier
            for (Application.Name appName : experimentsFutureMap.keySet()) {
                ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>> experimentsFuture = experimentsFutureMap.get(appName);
                final List<Experiment> appExperiments = new ArrayList<>();
                UninterruptibleUtil.getUninterruptibly(experimentsFuture).all().stream().forEach(expPojo -> {
                    appExperiments.add(ExperimentHelper.makeExperiment(expPojo));
                });
                experimentMap.put(appName, appExperiments);
            }
            LOGGER.debug("experimentMap=> {}", experimentMap);
        } catch (Exception e) {
            LOGGER.error("Error while getExperimentsForApps {}", appNames, e);
            throw new RepositoryException("Error while getExperimentsForApps", e);
        }
        LOGGER.debug("Returning experimentMap {}", experimentMap);
        return experimentMap;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment getExperiment(Application.Name appName,
                                    Experiment.Label experimentLabel) {

        LOGGER.debug("Getting App {} with label {}", new Object[]{appName, experimentLabel});

        return internalGetExperiment(appName, experimentLabel);
    }

    /**
     * {@inheritDoc}
     */
    protected Experiment internalGetExperiment(Application.Name appName,
                                               Experiment.Label experimentLabel) {

        LOGGER.debug("Getting experiment by app {} with label {} ", new Object[]{appName, experimentLabel});

        Preconditions.checkNotNull(appName, "Parameter \"appName\" cannot be null");
        Preconditions.checkNotNull(experimentLabel, "Parameter \"experimentLabel\" cannot be null");

        Experiment experiment = null;

        try {
            ExperimentByAppNameLabel experimentAppLabel = experimentLabelIndexAccessor
                    .getExperimentBy(appName.toString(),
                            experimentLabel.toString()).one();

            if (experimentAppLabel != null) {
                com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo = experimentAccessor
                        .getExperimentById(experimentAppLabel.getId()).one();
                experiment = ExperimentHelper.makeExperiment(experimentPojo);
            }

        } catch (Exception e) {
            LOGGER.error("Error while getting experiment by app {} with label {} ", new Object[]{appName, experimentLabel}, e);

            throw new RepositoryException("Could not retrieve experiment \"" + appName + "\".\"" + experimentLabel + "\"", e);
        }

        LOGGER.debug("Returning experiment {}", experiment);

        return experiment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment.ID createExperiment(NewExperiment newExperiment) {
        LOGGER.debug("Create experiment started... Experiment={}", newExperiment);

        final Date NOW = new Date();
        final Experiment.State DRAFT = State.DRAFT;

        try {
            //Create a batch statement with type as LOGGED to maintain atomicity of operation...
            BatchStatement batchStmt = new BatchStatement(BatchStatement.Type.LOGGED);

            //Step#1: Create an entry in the experiment table
            LOGGER.debug("Adding experiment table statement into the batch..");
            batchStmt.add(experimentAccessor.insertExperiment(
                    newExperiment.getId().getRawID(),
                    (newExperiment.getDescription() != null) ? newExperiment.getDescription() : "",
                    (newExperiment.getHypothesisIsCorrect() != null) ? newExperiment.getHypothesisIsCorrect() : "",
                    (newExperiment.getResults() != null) ? newExperiment.getResults() : "",
                    (newExperiment.getRule() != null) ? newExperiment.getRule() : "",
                    newExperiment.getSamplingPercent(),
                    newExperiment.getStartTime(),
                    newExperiment.getEndTime(),
                    DRAFT.name(),
                    newExperiment.getLabel().toString(),
                    newExperiment.getApplicationName().toString(),
                    NOW,
                    NOW,
                    newExperiment.getIsPersonalizationEnabled(),
                    newExperiment.getModelName(),
                    newExperiment.getModelVersion(),
                    newExperiment.getIsRapidExperiment(),
                    newExperiment.getUserCap(),
                    (newExperiment.getCreatorID() != null) ? newExperiment.getCreatorID() : "",
                    newExperiment.getTags(),
                    newExperiment.getSourceURL(),
                    newExperiment.getExperimentType()));

            //Step#2: Create an entry in the applicationList table
            LOGGER.debug("Adding applicationList table statement into the batch..");
            batchStmt.add(applicationListAccessor.insert(newExperiment.getApplicationName().toString()));

            //Step#3: Create an entry in the experiment_tag table
            LOGGER.debug("Adding experiment_tag table statement into the batch..");
            batchStmt.add(updateExperimentTags(newExperiment.getApplicationName(), newExperiment.getId(), newExperiment.getTags()));

            //Step#4: Create/update an entry in the application table with experiment priorities
            LOGGER.debug("Adding application table statement into the batch..");
            batchStmt.add(prioritiesAccessor.appendToPriorities(newArrayList(newExperiment.getId().getRawID()), newExperiment.getApplicationName().toString()));

            //Step#5: Create an entry in the experiment_label_index table
            LOGGER.debug("Adding experiment_label_index table statement into the batch..");
            batchStmt.add(experimentLabelIndexAccessor.insertOrUpdateStatementBy(newExperiment.getId().getRawID(), NOW,
                    newExperiment.getStartTime(), newExperiment.getEndTime(), DRAFT.name(), newExperiment.getApplicationName().toString(),
                    newExperiment.getLabel().toString()));

            //Step#6: Create an entry in the state_experiment_index table
            LOGGER.debug("Adding state_experiment_index table statement into the batch..");
            batchStmt.add(updateStateIndexStatement(newExperiment.getID(), ExperimentState.NOT_DELETED));

            //Now execute batch statement
            LOGGER.debug("Executing the batch statement.. batchStmt={}", batchStmt);
            driver.getSession().execute(batchStmt);

        } catch (Exception e) {
            LOGGER.error("Error while creating experiment {}", newExperiment, e);
            throw new RepositoryException("Exception while creating experiment " + newExperiment + " message " + e, e);
        }

        LOGGER.debug("Create experiment finished...");
        return newExperiment.getId();
    }

    /**
     * Updates the tags for a given Experiment. This also means that tags are deleted from
     * the lookup table in Cassandra if they are removed from the experiment.
     *
     * @param applicationName the Application for which the tags should be added
     * @param expId           the Id of the experiment that has changed tags
     * @param tags            list of tags for the experiment
     */
    public Statement updateExperimentTags(Application.Name applicationName, Experiment.ID expId, Set<String> tags) {
        if (tags == null) {
            tags = Collections.EMPTY_SET; // need this to update this to delete tags
        }
        // update the tag table, just rewrite the complete entry
        return experimentTagAccessor.insert(applicationName.toString(), expId.getRawID(), tags);
    }


    /**
     * {@inheritDoc}
     */
    // TODO - Why is this method is on the interface - if the client does not call it, the indices will be inconsistent ?	
    @Override
    public void createIndicesForNewExperiment(NewExperiment newExperiment) {
        // Point the experiment index to this experiment
        LOGGER.debug("Create indices for new experiment Experiment {}", newExperiment);

        updateExperimentLabelIndex(newExperiment.getID(),
                newExperiment.getApplicationName(), newExperiment.getLabel(),
                newExperiment.getStartTime(), newExperiment.getEndTime(),
                State.DRAFT);

        try {
            updateStateIndex(newExperiment.getID(), ExperimentState.NOT_DELETED);
        } catch (Exception e) {
            LOGGER.error("Create indices for new experiment Experiment {} failed", newExperiment, e);
            // remove the created ExperimentLabelIndex
            removeExperimentLabelIndex(newExperiment.getApplicationName(),
                    newExperiment.getLabel());
            throw new RepositoryException("Could not update indices for experiment \""
                    + newExperiment + "\"", e);
        }
    }

    /**
     * Improved way of getting BucketList for given experiments
     */
    @Override
    public Map<Experiment.ID, BucketList> getBucketList(Collection<Experiment.ID> experimentIds) {
        LOGGER.debug("Getting buckets list by experimentIDs {}", experimentIds);
        Map<Experiment.ID, BucketList> bucketMap = new HashMap<>();
        try {
            Map<Experiment.ID, ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket>>> bucketFutureMap = new HashMap<>();
            experimentIds.forEach(experimentId -> {
                bucketFutureMap.put(experimentId, bucketAccessor.asyncGetBucketByExperimentId(experimentId.getRawID()));
            });

            for (Experiment.ID expId : bucketFutureMap.keySet()) {
                bucketMap.put(expId, new BucketList());
                ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket>> bucketFuture = bucketFutureMap.get(expId);
                UninterruptibleUtil.getUninterruptibly(bucketFuture).all().forEach(bucketPojo -> {
                            bucketMap.get(expId).addBucket(BucketHelper.makeBucket(bucketPojo));
                        }
                );
            }
        } catch (Exception e) {
            LOGGER.error("getBucketList for {} failed", experimentIds, e);
            throw new RepositoryException("Could not fetch buckets for the list of experiments", e);
        }

        LOGGER.debug("Returning bucketMap {}", bucketMap);
        return bucketMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList getBucketList(Experiment.ID experimentID) {
        LOGGER.debug("Getting buckets list by one experimentId {}", experimentID);

        BucketList bucketList = new BucketList();

        try {
            Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketPojos =
                    bucketAccessor.getBucketByExperimentId(experimentID.getRawID());

            for (com.intuit.wasabi.repository.cassandra.pojo.Bucket bucketPojo : bucketPojos
                    .all()) {
                bucketList.addBucket(BucketHelper.makeBucket(bucketPojo));
            }

        } catch (Exception e) {
            LOGGER.error("Getting bucket list by one experiment id {} failed", experimentID, e);
            throw new RepositoryException("Could not fetch buckets for experiment \"" + experimentID
                    + "\" ", e);
        }

        LOGGER.debug("Returning buckets list by one experimentId {} bucket {}",
                new Object[]{experimentID, bucketList});

        return bucketList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment updateExperiment(Experiment experiment) {

        LOGGER.debug("Updating experiment  {}", experiment);

        validator.validateExperiment(experiment);

        try {
            // Note that this timestamp gets serialized as mulliseconds from
            // the epoch, so timezone is irrelevant
            final Date NOW = new Date();

            experimentAccessor.updateExperiment(
                    experiment.getDescription() != null ? experiment.getDescription() : "",
                    (experiment.getHypothesisIsCorrect() != null) ? experiment.getHypothesisIsCorrect() : "",
                    (experiment.getResults() != null) ? experiment.getResults() : "",
                    experiment.getRule() != null ? experiment.getRule() : "",
                    experiment.getSamplingPercent(),
                    experiment.getStartTime(),
                    experiment.getEndTime(),
                    experiment.getState().name(),
                    experiment.getLabel().toString(),
                    experiment.getApplicationName().toString(),
                    NOW,
                    experiment.getIsPersonalizationEnabled(),
                    experiment.getModelName(),
                    experiment.getModelVersion(),
                    experiment.getIsRapidExperiment(),
                    experiment.getUserCap(),
                    experiment.getTags(),
                    experiment.getSourceURL(),
                    experiment.getExperimentType(),
                    experiment.getID().getRawID());

            updateExperimentTags(experiment.getApplicationName(), experiment.getID(), experiment.getTags());

            // Point the experiment index to this experiment
            updateExperimentLabelIndex(experiment.getID(), experiment.getApplicationName(), experiment.getLabel(),
                    experiment.getStartTime(), experiment.getEndTime(), experiment.getState());

            updateStateIndex(experiment);

        } catch (Exception e) {
            LOGGER.error("Error while experiment updating experiment  {}", experiment, e);
            throw new RepositoryException("Could not update experiment with ID \"" + experiment.getID() + "\"", e);
        }

        return experiment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment updateExperimentState(Experiment experiment, State state) {

        LOGGER.debug("Updating experiment  {} state {} ", new Object[]{experiment, state});

        validator.validateExperiment(experiment);

        try {
            // Note that this timestamp gets serialized as mulliseconds from
            // the epoch, so timezone is irrelevant
            final Date NOW = new Date();

            experimentAccessor.updateExperiment(state.name(), NOW, experiment.getID().getRawID());

            experiment = Experiment.from(experiment).withState(state).build();

            // Point the experiment index to this experiment
            updateExperimentLabelIndex(experiment.getID(), experiment.getApplicationName(), experiment.getLabel(),
                    experiment.getStartTime(), experiment.getEndTime(), experiment.getState());

            updateStateIndex(experiment);

        } catch (Exception e) {
            LOGGER.error("Error while updating experiment  {} state {} ", new Object[]{experiment, state}, e);
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
        LOGGER.debug("Getting experiment ids which are live {}", ExperimentState.NOT_DELETED);

        try {
            // Get all experiments that are live
            Result<StateExperimentIndex> ids = stateExperimentIndexAccessor
                    .selectByKey(ExperimentState.NOT_DELETED.name());
            List<ID> experimentIds = ids.all().stream().map(
                    sei -> Experiment.ID.valueOf(sei.getExperimentId())).collect(Collectors.toList());

            return experimentIds;
        } catch (Exception e) {
            LOGGER.error("Error while getting experiment ids which are live {}", ExperimentState.NOT_DELETED, e);
            throw new RepositoryException("Could not retrieve experiments", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment> getExperiments(Application.Name appName) {
        LOGGER.debug("Getting experiments for application {}", appName);

        try {
            Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentPojos =
                    experimentAccessor.getExperimentByAppName(appName.toString());

            List<Experiment> experiments = experimentPojos.all().stream().filter(experiment ->
                    (experiment.getState() != null) &&
                            (!experiment.getState().equals(Experiment.State.TERMINATED.name())) &&
                            (!experiment.getState().equals(Experiment.State.DELETED.name())))
                    .map(experimentPojo -> ExperimentHelper.makeExperiment(experimentPojo))
                    .collect(Collectors.toList());

            return experiments;
        } catch (Exception e) {
            LOGGER.error("Error while getting experiments for app {}", appName, e);
            throw new RepositoryException("Could not retrieve experiments for app " + appName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExperiment(NewExperiment newExperiment) {
        LOGGER.debug("Deleting experiment {}", newExperiment);

        try {
            experimentAccessor.deleteExperiment(newExperiment.getID().getRawID());
        } catch (Exception e) {
            LOGGER.debug("Error while deleting experiment {}", newExperiment, e);
            throw new RepositoryException("Could not delete experiment "
                    + "with id \"" + newExperiment.getId() + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getExperiments(Collection<Experiment.ID> experimentIDs) {

        LOGGER.debug("Getting experiments {}", experimentIDs);

        ExperimentList result = new ExperimentList();

        try {
            if (!experimentIDs.isEmpty()) {
                Map<Experiment.ID, Experiment> experimentMap = getExperimentsMap(experimentIDs);
                List<Experiment> experiments = new ArrayList<>(experimentMap.values());
                result.setExperiments(experiments);
            }
        } catch (Exception e) {
            LOGGER.error("Error while getting experiments {}", experimentIDs, e);
            throw new RepositoryException("Could not retrieve the experiments for the collection of experimentIDs", e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Experiment.ID, Experiment> getExperimentsMap(Collection<Experiment.ID> experimentIds) {
        Map<Experiment.ID, ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>>> experimentsFutureMap = new HashMap<>(experimentIds.size());
        Map<Experiment.ID, Experiment> experimentMap = new HashMap<>(experimentIds.size());

        try {
            //Send calls asynchronously
            experimentIds.forEach(expId -> {
                experimentsFutureMap.put(expId, experimentAccessor.asyncGetExperimentById(expId.getRawID()));
                LOGGER.debug("Sent experimentAccessor.asyncGetExperimentById({})", expId);
            });

            //Process the Futures in the order that are expected to arrive earlier
            for (Experiment.ID expId : experimentsFutureMap.keySet()) {
                ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>> experimentsFuture = experimentsFutureMap.get(expId);
                UninterruptibleUtil.getUninterruptibly(experimentsFuture).all().stream().forEach(expPojo -> {
                    Experiment exp = ExperimentHelper.makeExperiment(expPojo);
                    experimentMap.put(exp.getID(), exp);
                });
            }

        } catch (Exception e) {
            LOGGER.error("Error while preparing experimentMap for {}", experimentIds, e);
            throw new RepositoryException("Error while preparing experimentMap", e);
        }
        LOGGER.debug("Returning experimentMap {}", experimentMap);
        return experimentMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table<Experiment.ID, Experiment.Label, Experiment> getExperimentList(Application.Name appName) {

        try {
            List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentPojos =
                    experimentAccessor.getExperimentByAppName(appName.toString()).all();

            Table<Experiment.ID, Experiment.Label, Experiment> result = HashBasedTable.create();
            for (com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo :
                    experimentPojos) {
                Experiment experiment = ExperimentHelper.makeExperiment(experimentPojo);
                if (experiment.getState() != State.TERMINATED && experiment.getState() != State.DELETED) {
                    result.put(experiment.getID(), experiment.getLabel(), experiment);
                }
            }

            return result;
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve experiment list " + appName.toString() + "because: " + e, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Application.Name> getApplicationsList() {

        List<Application.Name> result = new ArrayList<>();

        try {
            result = applicationListAccessor.getUniqueAppName().all().stream()
                    .map(app -> Application.Name.valueOf(app.getAppName())).collect(Collectors.toList());


        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve the application names because : " + e, e);
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

        try {
            List<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucket =
                    bucketAccessor.getBucketByExperimentIdAndBucket(
                            experimentID.getRawID(), bucketLabel.toString()).all();

            if (bucket.size() == 0)
                return null;

            if (bucket.size() > 1)
                throw new RepositoryException("More than one row found for experiment ID "
                        + experimentID + " and label " + bucketLabel);

            Bucket result = BucketHelper.makeBucket(bucket.get(0));


            return result;
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve bucket \"" +
                    bucketLabel + "\" in experiment \"" + experimentID + "\" because " + e, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createBucket(Bucket newBucket) {

        LOGGER.debug("Creating bucket {}", newBucket);

        Preconditions.checkNotNull(newBucket, "Parameter \"newBucket\" cannot be null");

        final Bucket.State STATE = Bucket.State.OPEN;

        try {
            bucketAccessor.insert(newBucket.getExperimentID().getRawID(),
                    newBucket.getLabel().toString(),
                    newBucket.getDescription(),
                    newBucket.getAllocationPercent(),
                    newBucket.isControl(),
                    newBucket.getPayload(),
                    STATE.name());

        } catch (Exception e) {
            LOGGER.error("Error creating bucket {}", newBucket, e);
            throw new RepositoryException("Could not create bucket \"" + newBucket + "\" because " + e, e);
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

            try {

                List<Bucket> buckets = bucketAccessor.getBucketByExperimentId(bucket.getExperimentID()
                        .getRawID()).all().stream().map(BucketHelper::makeBucket)
                        .collect(Collectors.toList());

                buckets.forEach(currentBucket ->
                        bucketAccessor.updateControl(false,
                                currentBucket.getExperimentID().getRawID(),
                                currentBucket.getLabel().toString()));

            } catch (Exception e) {
                throw new RepositoryException("Could not update buckets", e);
            }
        }

        try {
            bucketAccessor.updateBucket(
                    bucket.getDescription() != null ? bucket.getDescription() : "",
                    bucket.getAllocationPercent(),
                    bucket.isControl(),
                    bucket.getPayload() != null ? bucket.getPayload() : "",
                    bucket.getExperimentID().getRawID(),
                    bucket.getLabel().toString());


        } catch (Exception e) {
            throw new RepositoryException("Could not update bucket \"" + bucket.getExperimentID() + "\".\"" + bucket.getLabel() + "\"", e);
        }

        return bucket;
    }

    /**
     * @return the driver
     */
    public CassandraDriver getDriver() {
        return driver;
    }

    /**
     * @param driver the driver to set
     */
    public void setDriver(CassandraDriver driver) {
        this.driver = driver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bucket updateBucketAllocationPercentage(Bucket bucket, Double desiredAllocationPercentage) {

        try {
            bucketAccessor.updateAllocation(desiredAllocationPercentage, bucket.getExperimentID().getRawID(),
                    bucket.getLabel().toString());
        } catch (Exception e) {
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
    public Bucket updateBucketState(Bucket bucket, Bucket.State desiredState) {

        LOGGER.debug("Updating bucket {} state {}", new Object[]{bucket, desiredState});

        try {
            bucketAccessor.updateState(desiredState.name(), bucket
                    .getExperimentID().getRawID(), bucket.getLabel().toString());

            Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketPojo =
                    bucketAccessor.getBucketByExperimentIdAndBucket(bucket.getExperimentID()
                            .getRawID(), bucket.getLabel().toString());

            return BucketHelper.makeBucket(bucketPojo.one());
        } catch (Exception e) {
            LOGGER.error("Error while updating bucket {} state {}", new Object[]{bucket, desiredState}, e);
            throw new RepositoryException("Exception while updating bucket state " +
                    bucket + " state " + desiredState, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList updateBucketBatch(Experiment.ID experimentID,
                                        BucketList bucketList) {

        LOGGER.debug("bucket update {} for experiment id {}", new Object[]{bucketList, experimentID});

        ArrayList<Object> args = new ArrayList<>();

        String CQL = "BEGIN BATCH ";
        for (int i = 0; i < bucketList.getBuckets().size(); i++) {
            Bucket b = bucketList.getBuckets().get(i);
            CQL += "UPDATE bucket SET ";
            if (b.getState() != null) {
                CQL += "state = ?,";
                args.add(b.getState().name());
            }
            if (b.getAllocationPercent() != null) {
                CQL += "allocation = ?,";
                args.add(b.getAllocationPercent());
            }
            if (b.getDescription() != null) {
                CQL += "description = ?,";
                args.add(b.getDescription());
            }
            if (b.isControl() != null) {
                CQL += "is_control = ?,";
                args.add(b.isControl());
            }
            if (b.getPayload() != null) {
                CQL += "payload = ?,";
                args.add(b.getPayload());
            }
            if (",".equals(CQL.substring(CQL.length() - 1, CQL.length()))) {
                CQL = CQL.substring(0, CQL.length() - 1);
            }
            CQL += " where experiment_id = ? and label = ?;";
            args.add(experimentID.getRawID());
            args.add(b.getLabel().toString());
        }
        CQL += "APPLY BATCH;";

        LOGGER.debug("bucket update {} for experiment id {} statement{} with args {}",
                new Object[]{bucketList, experimentID, CQL, args});

        try {
            PreparedStatement preparedStatement = driver.getSession().prepare(CQL);
            BoundStatement boundStatement = new BoundStatement(preparedStatement);
            boundStatement.bind(args.toArray());
            driver.getSession().execute(boundStatement);
        } catch (Exception e) {
            throw new RepositoryException("Could not update bucket for experiment \"" + experimentID + "\"", e);
        }

        BucketList buckets;
        buckets = getBuckets(experimentID, false);
        return buckets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBucket(Experiment.ID experimentID, Bucket.Label bucketLabel) {

        Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");
        Preconditions.checkNotNull(bucketLabel, "Parameter \"bucketLabel\" cannot be null");
        try {

            bucketAccessor.deleteByExperimentIdAndLabel(experimentID.getRawID(), bucketLabel.toString());

        } catch (Exception e) {
            throw new RepositoryException("Could not delete bucket \"" + bucketLabel + "\" from experiment with ID \""
                    + experimentID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logBucketChanges(Experiment.ID experimentID, Bucket.Label bucketLabel,
                                 List<BucketAuditInfo> changeList) {

        final Date NOW = new Date();

        try {
            for (BucketAuditInfo changeData : changeList) {
                bucketAuditLogAccessor.insertBy(experimentID.getRawID(),
                        bucketLabel.toString(), NOW, changeData.getAttributeName(),
                        (changeData.getOldValue() != null) ? changeData.getOldValue() : "",
                        (changeData.getNewValue() != null) ? changeData.getNewValue() : "");

            }
        } catch (Exception e) {
            throw new RepositoryException("Could not log bucket changes \"" + experimentID + " : " + bucketLabel + " " + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logExperimentChanges(Experiment.ID experimentID, List<ExperimentAuditInfo> changeList) {
        final Date NOW = new Date();
        try {

            for (ExperimentAuditInfo changeData : changeList) {
                experimentAuditLogAccessor.insertBy(experimentID.getRawID(),
                        NOW, changeData.getAttributeName(),
                        (changeData.getOldValue() != null) ? changeData.getOldValue() : "",
                        (changeData.getNewValue() != null) ? changeData.getNewValue() : "");
            }
        } catch (Exception e) {
            throw new RepositoryException("Could not log experiment changes for experimentID \"" +
                    experimentID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList getBuckets(Experiment.ID experimentID, boolean checkExperiment) {

        LOGGER.debug("Getting buckets for {}", experimentID);

        Preconditions.checkNotNull(experimentID,
                "Parameter \"experimentID\" cannot be null");

        try {
            if (checkExperiment) {
                // Check if the experiment is live
                Experiment experiment = getExperiment(experimentID);
                if (experiment == null) {
                    throw new ExperimentNotFoundException(experimentID);
                }
            }

            Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketPojos =
                    bucketAccessor.getBucketByExperimentId(experimentID.getRawID());

            List<Bucket> buckets = bucketPojos.all().stream().map(BucketHelper::makeBucket).collect(Collectors.toList());

            BucketList bucketList = new BucketList();
            bucketList.setBuckets(buckets);

            LOGGER.debug("Returning buckets {} for experiment {}", new Object[]{
                    bucketList, experimentID});

            return bucketList;
        } catch (ExperimentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error while getting buckets for {}", experimentID, e);
            throw new RepositoryException("Unable to get buckets for " + experimentID, e);
        }
    }

    protected void updateExperimentLabelIndex(Experiment.ID experimentID,
                                              Application.Name appName, Experiment.Label experimentLabel,
                                              Date startTime, Date endTime, Experiment.State state) {

        LOGGER.debug("update experiment label index experiment id {} app {} label {} "
                        + " start time {} end time {} state {} ",
                new Object[]{experimentID, appName, experimentLabel, startTime, endTime, state});

        if (state == Experiment.State.TERMINATED
                || state == Experiment.State.DELETED) {
            removeExperimentLabelIndex(appName, experimentLabel);
            return;
        }

        try {
            // Note that this timestamp gets serialized as mulliseconds from
            // the epoch, so timezone is irrelevant
            final Date NOW = new Date();

            experimentLabelIndexAccessor.insertOrUpdateBy(experimentID.getRawID(), NOW,
                    startTime, endTime, state.name(), appName.toString(),
                    experimentLabel.toString());

        } catch (Exception e) {
            LOGGER.debug("Error while updating experiment label index experiment id {} app {} label {} "
                            + " start time {} end time {} state {} ",
                    new Object[]{experimentID, appName, experimentLabel, startTime, endTime, state}, e);
            throw new RepositoryException("Could not index experiment \"" + experimentID + "\"", e);
        }

    }

    protected void removeExperimentLabelIndex(Application.Name appName, Experiment.Label experimentLabel) {
        LOGGER.debug("Removing experiment label index for app {}, label {} ", new Object[]{
                appName, experimentLabel});

        try {
            experimentLabelIndexAccessor.deleteBy(appName.toString(),
                    experimentLabel.toString());
        } catch (Exception e) {
            LOGGER.error("Error while removing experiment label index for app {}, label {} ", new Object[]{
                    appName, experimentLabel}, e);
            throw new RepositoryException("Could not remove index for " +
                    "experiment \"" + appName + "\".\"" + experimentLabel + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStateIndex(Experiment experiment) {
        LOGGER.debug("update state index experiment {} ", experiment);

        try {
            updateStateIndex(experiment.getID(),
                    experiment.getState() != State.DELETED ? ExperimentState.NOT_DELETED : ExperimentState.DELETED);
        } catch (Exception e) {
            LOGGER.error("update state index experiment {} ", experiment, e);
            throw new RepositoryException("Exception while updating state index: " + e, e);
        }
    }

    protected void updateStateIndex(Experiment.ID experimentID, ExperimentState state) {
        driver.getSession().execute(updateStateIndexStatement(experimentID, state));
    }

    protected BatchStatement updateStateIndexStatement(Experiment.ID experimentID, ExperimentState state) {
        LOGGER.debug("update state index experiment id {} state {} ", new Object[]{experimentID, state});
        BatchStatement batchStmt = new BatchStatement();

        try {
            switch (state) {
                case DELETED:
                    LOGGER.debug("update state index insert experiment id {} state {} ",
                            new Object[]{experimentID, ExperimentState.DELETED.name()});

                    Statement insertStatement1 = stateExperimentIndexAccessor.insert(ExperimentState.DELETED.name(),
                            experimentID.getRawID(), ByteBuffer.wrap("".getBytes()));
                    batchStmt.add(insertStatement1);

                    LOGGER.debug("update state index delete experiment id {} state {} ",
                            new Object[]{experimentID, ExperimentState.NOT_DELETED.name()});

                    Statement deleteStatement1 = stateExperimentIndexAccessor.deleteBy(ExperimentState.NOT_DELETED.name(),
                            experimentID.getRawID());
                    batchStmt.add(deleteStatement1);

                    break;

                case NOT_DELETED:
                    LOGGER.debug("update state index insert experiment id {} state {} ",
                            new Object[]{experimentID, ExperimentState.NOT_DELETED.name()});

                    Statement insert2 = stateExperimentIndexAccessor.insert(ExperimentState.NOT_DELETED.name(),
                            experimentID.getRawID(), ByteBuffer.wrap("".getBytes()));
                    batchStmt.add(insert2);

                    LOGGER.debug("update state index delete experiment id {} state {} ",
                            new Object[]{experimentID, ExperimentState.DELETED.name()});

                    Statement delete2 = stateExperimentIndexAccessor.deleteBy(ExperimentState.DELETED.name(),
                            experimentID.getRawID());
                    batchStmt.add(delete2);

                    break;
                default:
                    throw new RepositoryException("Unknown experiment state: " + state);
            }

            return batchStmt;
        } catch (Exception e) {
            LOGGER.error("Error while updating state index experiment id {} state {} ",
                    new Object[]{experimentID, state}, e);
            throw new RepositoryException("Unable to update experiment " + experimentID + " with state " + state);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statement createApplication(Application.Name applicationName) {
        LOGGER.debug("Creating application {}", applicationName);
        try {
            return applicationListAccessor.insert(applicationName.toString());
        } catch (Exception e) {
            LOGGER.error("Error while creating application {}", applicationName, e);
            throw new RepositoryException("Unable to insert into top level application list: \""
                    + applicationName.toString() + "\"" + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Application.Name, Set<String>> getTagListForApplications(Collection<Application.Name> applicationNames) {

        LOGGER.debug("Retrieving Experiment Tags for applications {}", applicationNames);

        try {
            List<ListenableFuture<Result<ExperimentTagsByApplication>>> futures = new ArrayList<>();

            for (Application.Name appName : applicationNames) {
                ListenableFuture<Result<ExperimentTagsByApplication>> resultSetFuture = experimentTagAccessor
                        .getExperimentTagsAsync(appName.toString());
                futures.add(resultSetFuture);
            }

            Map<Application.Name, Set<String>> result = new HashMap<>();
            for (ListenableFuture<Result<ExperimentTagsByApplication>> future : futures) {
                List<ExperimentTagsByApplication> expTagApplication = future.get().all();

                Set<String> allTagsForApplication = new TreeSet<>();
                for (ExperimentTagsByApplication expTagsByApp : expTagApplication) {
                    allTagsForApplication.addAll(expTagsByApp.getTags());
                }

                if (!expTagApplication.isEmpty())
                    result.put(Application.Name.valueOf(expTagApplication.get(0).getAppName()), allTagsForApplication);
            }

            return result;
        } catch (Exception e) {
            LOGGER.error("Error while retieving ExperimentTags for {}", applicationNames, e);
            throw new RepositoryException("Unable to get ExperimentTags for applications: \""
                    + applicationNames.toString() + "\"" + e);
        }
    }

}
