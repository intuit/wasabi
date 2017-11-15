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

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.exceptions.EndTimeHasPassedException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Mutex;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentIDList;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intuit.wasabi.experimentobjects.Experiment.State.DELETED;
import static com.intuit.wasabi.experimentobjects.Experiment.State.TERMINATED;

public class MutexImpl implements Mutex {

    private static final Logger LOGGER = LoggerFactory.getLogger(MutexImpl.class);
    final Date NOW = new Date();
    private final MutexRepository mutexRepository;
    private final Experiments experiments;
    private final EventLog eventLog;

    @Inject
    public MutexImpl(MutexRepository mutexRepository, Experiments experiments, EventLog eventLog) {
        super();
        this.mutexRepository = mutexRepository;
        this.experiments = experiments;
        this.eventLog = eventLog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getExclusions(Experiment.ID experimentID) {

        // Throw an exception if the input experiment is not valid
        final Experiment expID = experiments.getExperiment(experimentID);
        if (expID.getID() == null) {
            throw new ExperimentNotFoundException(experimentID);
        }
        return mutexRepository.getExclusions(experimentID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getNotExclusions(Experiment.ID experimentID) {
        // Throw an exception if the input experiment is not valid
        if (experimentID == null) {
            throw new ExperimentNotFoundException("error, experiment not found");
        }
        return mutexRepository.getNotExclusions(experimentID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExclusion(Experiment.ID expID_1, Experiment.ID expID_2, UserInfo user) {
        Experiment exp_1 = experiments.getExperiment(expID_1);
        Experiment exp_2 = experiments.getExperiment(expID_2);

        // Check that expID_1 is a valid experiment
        if (exp_1 == null) {
            throw new ExperimentNotFoundException(expID_1);
        }

        // Check that expID_2 is a valid experiment
        if (exp_2 == null) {
            throw new ExperimentNotFoundException(expID_2);
        }

        mutexRepository.deleteExclusion(expID_1, expID_2);
        eventLog.postEvent(new ExperimentChangeEvent(user, exp_1, "mutex", exp_2.getLabel().toString(), null));

        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=MUTUAL_EXCLUSION_DELETED, applicationName={}, configuration=[experiment1={}, experiment2={}]",
                exp_1.getApplicationName(), exp_1.getLabel(), exp_2.getLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map> createExclusions(Experiment.ID baseID, ExperimentIDList experimentIDList, UserInfo user) {

        // Get the base experiment
        Experiment baseExp = experiments.getExperiment(baseID);

        // Throw an exception if the base experiment is not valid
        if (baseExp == null) {
            throw new ExperimentNotFoundException(baseID);
        }

        // Get the current state of the base experiment
        Experiment.State baseCurrentState = baseExp.getState();

        // Throw an exception if base experiment is in an invalid state
        if (baseCurrentState.equals(TERMINATED) || baseCurrentState.equals(DELETED)) {
            throw new InvalidExperimentStateException(new StringBuilder("Invalid experiment state").append(baseID)
                    .append("\"").append("Can not define mutual exclusion rules when an experiment is in")
                    .append(baseCurrentState).append("\"").append("state").toString());
        }

        // Get the timestamp for current time
        final Date nowTimestamp = new Timestamp(NOW.getTime());

        // Get the end time of the base experiment
        Date baseEndTime = baseExp.getEndTime();
        final Date baseTimestamp = new Timestamp(baseEndTime.getTime());

        // Throw an exception if the base end time has passed
        if (nowTimestamp.after(baseTimestamp)) {
            throw new EndTimeHasPassedException(baseID, baseEndTime);
        }

        List<Map> results = new ArrayList<>();

        // Loop through the ExperimentBatch list
        for (Experiment.ID pairID : experimentIDList.getExperimentIDs()) {

            Map<String, Object> tempResult = new HashMap<>();
            tempResult.put("experimentID1", baseID);
            tempResult.put("experimentID2", pairID);

            // Check to see if pair experiment exists
            Experiment pairExp = experiments.getExperiment(pairID);

            if (pairExp == null) {
                tempResult.put("status", "FAILED");
                tempResult.put("reason", "Experiment2 not found.");
                results.add(tempResult);
                continue;
            }

            // Check to see if base and pair experiments are in the same application
            if (!pairExp.getApplicationName().equals(baseExp.getApplicationName())) {
                tempResult.put("status", "FAILED");
                tempResult.put("reason", new StringBuilder("Experiments 1 and 2 are not in the same application. ")
                        .append("Mutual exclusion rules can only be defined for experiments within the same application.").toString());
                results.add(tempResult);
                continue;
            }

            // Get the current state of the pair experiment
            Experiment.State pairCurrentState = pairExp.getState();
            // Throw an exception if the pair experiment is in an invalid state
            if (pairCurrentState.equals(TERMINATED) || pairCurrentState.equals(DELETED)) {
                tempResult.put("status", "FAILED");
                tempResult.put("reason", "Experiment2 is in TERMINATED or DELETED state");
                results.add(tempResult);
                continue;
            }

            // Get the end time of the pair experiment
            Date pairEndTime = pairExp.getEndTime();
            final Date pairTimestamp = new Timestamp(pairEndTime.getTime());

            // Throw an exception if the base end time has passed
            if (nowTimestamp.after(pairTimestamp)) {
                tempResult.put("status", "FAILED");
                tempResult.put("reason", "End time has passed for experiment2");
                results.add(tempResult);
                continue;
            }

            //add the pair
            try {
                mutexRepository.createExclusion(baseID, pairID);
                eventLog.postEvent(new ExperimentChangeEvent(user, baseExp, "mutex",
                        null, pairExp.getLabel() == null ? null : pairExp.getLabel().toString()));
            } catch (RepositoryException rExp) {
                LOGGER.error("Unable to store data in repository: ", rExp);
                tempResult.put("status", "FAILED");
                tempResult.put("reason", "Repository exception");
                results.add(tempResult);
                continue;
            }
            LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=MUTUAL_EXCLUSION_CREATED, applicationName={}, configuration=[experiment1={}, experiment2={}]",
                    baseExp.getApplicationName(), baseExp.getLabel(), pairExp.getLabel());
            tempResult.put("status", "SUCCESS");
            results.add(tempResult);
        }

        return results;
    }

    /**
     * {@inheritDoc}
     */
    public List<Experiment> getRecursiveMutualExclusions(Experiment experiment) {
        List<Experiment> visitedExperiments = new ArrayList<>();
        Set<Experiment> unvisitedExperiments = new HashSet<>();
        unvisitedExperiments.add(experiment);
        do {
            Set<Experiment> newUnvisitedExperiments = new HashSet<>();
            for (Experiment tempExperiment : unvisitedExperiments) {
                visitedExperiments.add(tempExperiment);
                newUnvisitedExperiments.addAll(
                        this.getExclusions(tempExperiment.getID())
                                .getExperiments()
                                // Hack: We need to make experiments compatible, converting CassandraExperiments
                                .parallelStream()
                                .map(ce -> Experiment.from(ce).build())
                                .collect(Collectors.toList())
                );
            }
            unvisitedExperiments.addAll(newUnvisitedExperiments);
            unvisitedExperiments.removeAll(visitedExperiments);
        } while (!unvisitedExperiments.isEmpty());
        return visitedExperiments;
    }
}
