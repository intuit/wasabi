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
package com.intuit.wasabi.experiment.impl;

import com.intuit.hyrule.Rule;
import com.intuit.hyrule.RuleBuilder;
import com.intuit.wasabi.assignmentobjects.RuleCache;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.eventlog.events.ExperimentCreateEvent;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Buckets;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ExperimentAuditInfo;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.DatabaseRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intuit.wasabi.experimentobjects.Experiment.State.DELETED;
import static com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT;
import static com.intuit.wasabi.experimentobjects.Experiment.State.PAUSED;
import static com.intuit.wasabi.experimentobjects.Experiment.State.RUNNING;
import static com.intuit.wasabi.experimentobjects.Experiment.State.TERMINATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A thin wrapper around Cassandra implementation at com.intuit.wasabi.repository.impl.cassandra.CassandraExperimentRepository
 * {@link com.intuit.wasabi.repository.ExperimentRepository}
 * to keep business logic out of the persistence layer
 */
public class ExperimentsImpl implements Experiments {

    private static final Logger LOGGER = getLogger(ExperimentsImpl.class);
    private final Date currentDate = new Date();
    private final ExperimentRepository databaseRepository;
    private final ExperimentRepository cassandraRepository;
    private final Buckets buckets;
    private final Pages pages;
    private final Priorities priorities;
    private final ExperimentValidator validator;
    private final Experiments experiments;
    private final EventLog eventLog;
    private RuleCache ruleCache;

    @Inject
    public ExperimentsImpl(@DatabaseRepository ExperimentRepository databaseRepository,
                           @CassandraRepository ExperimentRepository cassandraRepository, Experiments experiments,
                           Buckets buckets, Pages pages, Priorities priorities, ExperimentValidator validator,
                           RuleCache ruleCache, EventLog eventLog) {
        super();
        this.validator = validator;
        this.databaseRepository = databaseRepository;
        this.cassandraRepository = cassandraRepository;
        this.experiments = experiments;
        this.buckets = buckets;
        this.pages = pages;
        this.priorities = priorities;
        this.ruleCache = ruleCache;
        this.eventLog = eventLog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getExperiments() {
        List<Experiment.ID> experimentIDs = cassandraRepository.getExperiments();
        return cassandraRepository.getExperiments(experimentIDs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Application.Name> getApplications() {
        return cassandraRepository.getApplicationsList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Application.Name, Set<String>> getTagsForApplications(Collection<Application.Name> applicationNames) {
        return cassandraRepository.getTagListForApplications(applicationNames != null ? applicationNames : Collections.emptySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment getExperiment(Experiment.ID id) {
        return cassandraRepository.getExperiment(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment getExperiment(Application.Name appName, Experiment.Label label) {
        return cassandraRepository.getExperiment(appName, label);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment> getExperiments(Application.Name appName) {
        return cassandraRepository.getExperiments(appName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createExperiment(NewExperiment newExperiment, UserInfo user) {
        LOGGER.debug("Create experiment started: experiment={}, UserInfo={} ", newExperiment, user);

        //Step#1: Validate new experiment
        validator.validateNewExperiment(newExperiment);
        try {

            //Step#2: Create experiment in MySQL first (before Cassandra) so that duplicate experiment
            //concern would be addressed automatically. As, AppName and experiment label have a unique constraint
            //set in MySQL table.
            LOGGER.debug("Creating an experiment in MySQL...");
            databaseRepository.createExperiment(newExperiment);

            //Step#3: Create experiment in Cassandra
            try {
                cassandraRepository.createExperiment(newExperiment);
            } catch (Exception exceptionFromCassandra) {
                LOGGER.error("Exception occurred while creating an experiment in Cassandra... Experiment={}, UserInfo={}", newExperiment, user, exceptionFromCassandra);
                // Erase from MySQL
                try {
                    databaseRepository.deleteExperiment(newExperiment);
                } catch (Exception mysqlRollbackException) {
                    LOGGER.error("An attempt to rollback of experiment in MySQL is failed...", mysqlRollbackException);
                }
                throw exceptionFromCassandra;
            }

            //Step#4: Log experiment creation event
            eventLog.postEvent(new ExperimentCreateEvent(user, newExperiment));

        } catch (Exception experimentCreateException) {
            LOGGER.error("Exception occurred while creating an experiment... Experiment={}, UserInfo={}", newExperiment, user, experimentCreateException);
            throw experimentCreateException;
        }

        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=EXPERIMENT_CREATED, applicationName={}, configuration={}",newExperiment.getApplicationName(),newExperiment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkStateTransition(Experiment.ID experimentID, Experiment.State currentState,
                                     Experiment.State desiredState) {
        if (desiredState != null && !currentState.equals(desiredState)) {

            // Throw an exception if the StateTransition is invalid
            validator.validateStateTransition(currentState, desiredState);

            /*
            If moving from a DRAFT state to a RUNNING state
            a sanity-check is required on the experiment buckets.
            Fetch the bucket information if the experiment will be
            (or remain) in an active state (running, paused)
            because that info is used to create the KV-store entry.
            * */
            if (currentState.equals(DRAFT) && desiredState.equals(RUNNING)) {
                // Throw an exception if the sanity-check fails
                BucketList bucketList = buckets.getBuckets(experimentID, false /* don't check experiment again */);
                validator.validateExperimentBuckets(bucketList.getBuckets());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkForIllegalUpdate(Experiment experiment, Experiment updates) {
        // Throw an exception if ID, ModificationTime or CreationTime is being updated.
        if (updates.getID() != null && !updates.getID().equals(experiment.getID())) {
            throw new IllegalArgumentException("Invalid experimentID \"" + updates.getID() + "\" " +
                    "Cannot change experiment ID");
        }
        if (updates.getCreationTime() != null && !updates.getCreationTime().equals(experiment.getCreationTime())) {
            throw new IllegalArgumentException("Invalid creationTime \"" + updates.getCreationTime() + "\" " +
                    "Experiment creation time cannot be modified");
        }
        if (updates.getModificationTime() != null
                && !updates.getModificationTime().equals(experiment.getModificationTime())) {
            throw new IllegalArgumentException("Invalid modificationTime \"" + updates.getModificationTime() + "\" " +
                    "Experiment modification time cannot be modified");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkForIllegalTerminatedUpdate(Experiment experiment, Experiment updates) {
        /*
        Throw an exception if the currentState is "TERMINATED"
        and if any attribute other than description is being updated
        * */
        if (experiment.getState().equals(TERMINATED)) {

            if (updates.getApplicationName() != null &&
                    !updates.getApplicationName().equals(experiment.getApplicationName())) {
                throw new IllegalArgumentException("Invalid application name \"" + updates.getApplicationName() + "\" " +
                        "Cannot change application name when the experiment is in TERMINATED state");
            }
            if (updates.getLabel() != null && !updates.getLabel().equals(experiment.getLabel())) {
                throw new IllegalArgumentException("Invalid label \"" + updates.getLabel() + "\" " +
                        "Cannot change label when the experiment is in TERMINATED state");
            }
            if (updates.getStartTime() != null && !updates.getStartTime().equals(experiment.getStartTime())) {
                throw new IllegalArgumentException("Invalid startTime \"" + updates.getStartTime() + "\" " +
                        "Cannot change start time when the experiment is in TERMINATED state");
            }
            if (updates.getEndTime() != null && !updates.getEndTime().equals(experiment.getEndTime())) {
                throw new IllegalArgumentException("Invalid endTime \"" + updates.getEndTime() + "\" " +
                        "Cannot change end time when the experiment is in TERMINATED state");
            }
            if (updates.getSamplingPercent() != null &&
                    !updates.getSamplingPercent().equals(experiment.getSamplingPercent())) {
                throw new IllegalArgumentException("Invalid sampling percentage \"" + updates.getSamplingPercent() + "\" " +
                        "Cannot change sampling percentage when the experiment is in TERMINATED state");
            }
            if (updates.getRule() != null && !updates.getRule().equals(experiment.getRule())) {
                throw new IllegalArgumentException("Invalid rule \"" + updates.getRule() + "\" " +
                        "Cannot change sampling rule when the experiment is in TERMINATED state");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkForIllegalPausedRunningUpdate(Experiment experiment, Experiment updates) {
        /*
        Throw an exception if the experiment is in RUNNING/PAUSED state
        and if applicationName or Label is being updated.
        Also, if startTime/endTime is being updated with a value that has already passed
        or when the established startTime/endTime has already passed.
        */
        if (experiment.getState().equals(RUNNING) || experiment.getState().equals(PAUSED)) {
            if (updates.getApplicationName() != null &&
                    !updates.getApplicationName().equals(experiment.getApplicationName()))
                throw new IllegalArgumentException("Cannot change AppName when the experiment is not in DRAFT state");
            if (updates.getLabel() != null && !updates.getLabel().equals(experiment.getLabel()))
                throw new IllegalArgumentException("Cannot change Label when the experiment is not in DRAFT state");
            if (updates.getStartTime() != null && !updates.getStartTime().equals(experiment.getStartTime()))
                checkForIllegalExperimentStartTime(experiment, updates);
            if (updates.getEndTime() != null && !updates.getEndTime().equals(experiment.getEndTime()))
                checkForIllegalExperimentEndTime(experiment, updates);
        }
    }

    void checkForIllegalExperimentEndTime(Experiment current, Experiment update) {
        if (update.getEndTime().before(currentDate)) {
            throw new IllegalArgumentException("Invalid endTime \"" + update.getEndTime() + "\". " +
                    "Cannot set the experiment end time to a value in the past");
        } else if (current.getEndTime().before(currentDate)) {
            throw new IllegalArgumentException("Invalid endTime \"" + update.getEndTime() + "\". " +
                    "Cannot update the experiment endTime that has already passed");
        } else {
            if (update.getStartTime() != null) {
                if (update.getEndTime().before(update.getStartTime())) {
                    throw new IllegalArgumentException("Invalid startTime \"" + update.getEndTime() + "\". " +
                            "Cannot update the experiment endTime to a value preceding the experiment startTime");
                }
            } else {
                if (update.getEndTime().before(current.getStartTime())) {
                    throw new IllegalArgumentException("Invalid startTime \"" + update.getEndTime() + "\". " +
                            "Cannot update the experiment endTime to a value preceding the experiment startTime");
                }
            }
        }
    }

    void checkForIllegalExperimentStartTime(Experiment current, Experiment update) {
        if (update.getStartTime().before(currentDate)) {
            throw new IllegalArgumentException("Invalid startTime \"" + update.getStartTime() + "\". " +
                    "Cannot set the experiment start time to a value in the past");
        } else if (current.getStartTime().before(currentDate)) {
            throw new IllegalArgumentException("Invalid startTime \"" + update.getStartTime() + "\". " +
                    "Cannot update the experiment startTime that has already passed");
        } else {
            if (update.getEndTime() != null) {
                if (update.getStartTime().after(update.getEndTime())) {
                    throw new IllegalArgumentException("Invalid startTime \"" + update.getStartTime() + "\". " +
                            "Cannot update the experiment startTime to a value beyond the experiment endTime");
                }
            } else {
                if (update.getStartTime().after(current.getEndTime())) {
                    throw new IllegalArgumentException("Invalid startTime \"" + update.getStartTime() + "\". " +
                            "Cannot update the experiment startTime to a value beyond the experiment endTime");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean buildUpdatedExperiment(Experiment experiment, Experiment updates, Experiment.Builder builder,
                                          List<ExperimentAuditInfo> changeList) {

        boolean requiresUpdate = false;
        ExperimentAuditInfo changeData;

        if (updates.getState() != null && !updates.getState().equals(experiment.getState())) {
            builder.withState(updates.getState());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("state", experiment.getState().toString(),
                    updates.getState().toString());
            changeList.add(changeData);
        }
        if (updates.getDescription() != null && !updates.getDescription().equals(experiment.getDescription())) {
            builder.withDescription(updates.getDescription());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("description", experiment.getDescription(), updates.getDescription());
            changeList.add(changeData);
        }
        if (updates.getHypothesisIsCorrect() != null && !updates.getHypothesisIsCorrect().equals(experiment.getHypothesisIsCorrect())) {
            builder.withHypothesisIsCorrect(updates.getHypothesisIsCorrect());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("hypothesis_is_correct", experiment.getHypothesisIsCorrect(), updates.getHypothesisIsCorrect());
            changeList.add(changeData);
        }
        if (updates.getResults() != null && !updates.getResults().equals(experiment.getResults())) {
            builder.withResults(updates.getResults());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("results", experiment.getResults(), updates.getResults());
            changeList.add(changeData);
        }
        if (updates.getSamplingPercent() != null
                && !updates.getSamplingPercent().equals(experiment.getSamplingPercent())) {
            builder.withSamplingPercent(updates.getSamplingPercent());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("sampling_percent", experiment.getSamplingPercent().toString(),
                    updates.getSamplingPercent().toString());
            changeList.add(changeData);
        }
        if (updates.getStartTime() != null && !updates.getStartTime().equals(experiment.getStartTime())) {
            builder.withStartTime(updates.getStartTime());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("start_time", experiment.getStartTime().toString(),
                    updates.getStartTime().toString());
            changeList.add(changeData);
        }
        if (updates.getEndTime() != null
                && !updates.getEndTime().equals(experiment.getEndTime())) {
            builder.withEndTime(updates.getEndTime());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("end_time", experiment.getEndTime().toString(), updates.getEndTime().toString());
            changeList.add(changeData);
        }

        if (updates.getIsPersonalizationEnabled() != null
                && !updates.getIsPersonalizationEnabled().equals(experiment.getIsPersonalizationEnabled())) {
            builder.withIsPersonalizationEnabled(updates.getIsPersonalizationEnabled());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("isPersonalizationEnabled",
                    experiment.getIsPersonalizationEnabled().toString(),
                    updates.getIsPersonalizationEnabled().toString());
            changeList.add(changeData);
        }

        if (updates.getModelName() != null && !updates.getModelName().equals(experiment.getModelName())) {
            builder.withModelName(updates.getModelName());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("modelName", experiment.getModelName(), updates.getModelName());
            changeList.add(changeData);
        }

        if (updates.getModelVersion() != null && !updates.getModelVersion().equals(experiment.getModelVersion())) {
            builder.withModelVersion(updates.getModelVersion());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("modelVersion", experiment.getModelVersion(), updates.getModelVersion());
            changeList.add(changeData);
        }

        if (updates.getIsRapidExperiment() != null
                && !updates.getIsRapidExperiment().equals(experiment.getIsRapidExperiment())) {
            builder.withIsRapidExperiment(updates.getIsRapidExperiment());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("isRapidExperiment",
                    experiment.getIsRapidExperiment().toString(),
                    updates.getIsRapidExperiment().toString());
            changeList.add(changeData);
        }

        if (updates.getUserCap() != null
                && !updates.getUserCap().equals(experiment.getUserCap())) {
            builder.withUserCap(updates.getUserCap());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("userCap",
                    experiment.getUserCap().toString(),
                    updates.getUserCap().toString());
            changeList.add(changeData);
        }

        if (updates.getRule() != null && !updates.getRule().equals(experiment.getRule())) {
            builder.withRule(updates.getRule());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("rule",
                    experiment.getRule(),
                    updates.getRule());
            changeList.add(changeData);
        }


        if (updates.getSourceURL() != null && !updates.getSourceURL().equals(experiment.getSourceURL())) {
            builder.withSourceURL(updates.getSourceURL());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("sourceURL",
                    experiment.getSourceURL().toString(),
                    updates.getSourceURL());
            changeList.add(changeData);
        }

        if (updates.getExperimentType() != null && !updates.getExperimentType().equals(experiment.getExperimentType())) {
            builder.withExperimentType(updates.getExperimentType());
            requiresUpdate = true;
            changeData = new ExperimentAuditInfo("experimentType",
                    experiment.getExperimentType().toString(),
                    updates.getExperimentType());
            changeList.add(changeData);
        }

        if (updates.getTags() != null && !updates.getTags().equals(experiment.getTags())) {
            builder.withTags(updates.getTags());
            requiresUpdate = true;
            Set<String> oldTags = experiment.getTags() == null ? Collections.EMPTY_SET : experiment.getTags();
            changeData = new ExperimentAuditInfo("tags",
                    oldTags.toString(),
                    updates.getTags().toString());
            changeList.add(changeData);
        }

        /*
        * Application name and label cannot be changed once the experiment is beyond the DRAFT state.
        * Hence, we are not including them as a part of the audit log.
        */
        if (updates.getLabel() != null && !updates.getLabel().equals(experiment.getLabel())) {
            builder.withLabel(updates.getLabel());
            requiresUpdate = true;
        }
        if (updates.getApplicationName() != null
                && !updates.getApplicationName().equals(experiment.getApplicationName())) {
            builder.withApplicationName(updates.getApplicationName());
            requiresUpdate = true;
        }

        return requiresUpdate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSegmentationRule(Experiment experiment, UserInfo user) {
        Rule oldRule = ruleCache.getRule(experiment.getID());
        Rule newRule;
        if (experiment.getRule() != null && experiment.getRule().length() != 0) {
            newRule = new RuleBuilder().parseExpression(experiment.getRule());
            ruleCache.setRule(experiment.getID(), newRule);
            LOGGER.debug("Segmentation rule of " + experiment.getID() + " updated from "
                    + (oldRule != null ? oldRule.getExpressionRepresentation() : null) +
                    " to " + (newRule != null ? newRule.getExpressionRepresentation() : null));
        } else {
            ruleCache.clearRule(experiment.getID());
            LOGGER.debug("Segmentation rule of " + experiment.getID() + " cleared "
                    + (oldRule != null ? oldRule.getExpressionRepresentation() : null));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment updateExperiment(final Experiment.ID experimentID, final Experiment updates, final UserInfo user) {

        // Get the current experiment
        Experiment experiment = getExperiment(experimentID);
        // Save the current experiment to be reverted back in case of mysql failure
        Experiment oldExperiment = experiment;
        // Throw an exception if the current experiment is not valid
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        // Get the current state
        Experiment.State currentState = experiment.getState();
        // Get the desired state
        Experiment.State desiredState = updates.getState();

        experiments.checkStateTransition(experimentID, currentState, desiredState);
        experiments.checkForIllegalUpdate(experiment, updates);
        experiments.checkForIllegalTerminatedUpdate(experiment, updates);
        experiments.checkForIllegalPausedRunningUpdate(experiment, updates);

        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        Experiment.Builder builder = Experiment.from(experiment);
        boolean requiresUpdate = buildUpdatedExperiment(experiment, updates, builder, changeList);

        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=UPDATING_EXPERIMENT, applicationName={}, configuration={}", oldExperiment.getApplicationName(), oldExperiment);

        if (requiresUpdate) {

            experiment = builder.build();
            boolean applicationNameChanged = !experiment.getApplicationName().equals(oldExperiment.getApplicationName
                    ());
            boolean segmentationRuleChanged = (experiment.getRule() != null && oldExperiment.getRule() != null) && !experiment.getRule().equals(oldExperiment.getRule());

            // Throw an exception if the updated experiment is not valid
            validator.validateExperiment(experiment);
            // Update both repositories
            Experiment updatedExperiment = cassandraRepository.updateExperiment(experiment);
            // To maintain consistency, revert the changes made in cassandra in case the mysql update fails
            try {
                databaseRepository.updateExperiment(experiment);
            } catch (Exception e) {
                cassandraRepository.updateExperiment(oldExperiment);
                throw e;
            }
            experiment = updatedExperiment;

            if (applicationNameChanged) {
                // Remove the now obsolete applicationName-experimentID combination from the priority list
                priorities.removeFromPriorityList(oldExperiment.getApplicationName(), experimentID);
                // Append the updated experiment to the priority list
                priorities.appendToPriorityList(experimentID);
            }

            // Update the rule cache with the new segmentation rule
            if (segmentationRuleChanged) {
                experiments.updateSegmentationRule(experiment, user);
            }

            // Update the bucket audit log
            // Do not audit the changes that are performed in the experiment's DRAFT state
            if (!experiment.getState().equals(DRAFT)) {
                cassandraRepository.logExperimentChanges(experimentID, changeList);
                for (ExperimentAuditInfo experimentAuditInfo : changeList) {
                    eventLog.postEvent(new ExperimentChangeEvent(user, experiment, experimentAuditInfo.getAttributeName(),
                            experimentAuditInfo.getOldValue(), experimentAuditInfo.getNewValue()));
                }
            }

            if (desiredState != null && (desiredState.equals(TERMINATED) || desiredState.equals(DELETED))) {
                // Remove the experiment from the priority list
                priorities.removeFromPriorityList(experiment.getApplicationName(), experimentID);
                // Remove the experiment from the page related data
                pages.erasePageData(experiment.getApplicationName(), experimentID, user);

                /*
                Special case: after a transition to the deleted state,
                the experiment is no longer visible, so return an empty result
                * */
                if (desiredState.equals(DELETED)) {
                    experiment = Experiment.from(experiment)
                            .withState(DELETED)
                            .build();
                }
            }
        }
        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, Message=EXPERIMENT_UPDATED, applicationName={}, configuration={}", experiment.getApplicationName(), experiment);

        return experiment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateExperimentState(final Experiment experiment, final Experiment.State state) {
        try {
            cassandraRepository.updateExperimentState(experiment, state);
            // To maintain consistency, revert the changes made in cassandra in case the mysql update fails
            try {
                databaseRepository.updateExperimentState(experiment, state);
            } catch (Exception exception) {
                cassandraRepository.updateExperimentState(experiment, experiment.getState());
                throw exception;
            }

            eventLog.postEvent(new ExperimentChangeEvent(experiment, "state",
                    experiment.getState().toString(), state.toString()));
        } catch (Exception exception) {
            LOGGER.error("Updating experiment state for experiment:{} failed with error:", experiment, exception);
            throw exception;
        }
        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=EXPERIMENT_STATE_UPDATED, applicationName={}, configuration=[experimentName={}, oldState={}, newState={}]",
                experiment.getApplicationName(), experiment.getLabel(), experiment.getState(), state);
    }
}
