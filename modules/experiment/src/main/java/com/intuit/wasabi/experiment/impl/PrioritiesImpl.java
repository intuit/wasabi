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

import com.intuit.wasabi.exceptions.ApplicationNotFoundException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentIDList;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.PrioritiesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intuit.wasabi.experimentobjects.Experiment.State.TERMINATED;

public class PrioritiesImpl implements Priorities {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritiesImpl.class);
    private final PrioritiesRepository prioritiesRepository;
    private final Experiments experiments;

    @Inject
    public PrioritiesImpl(PrioritiesRepository prioritiesRepository, Experiments experiments) {
        super();
        this.prioritiesRepository = prioritiesRepository;
        this.experiments = experiments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPriority(Experiment.ID experimentID, int priorityNum) {

        Experiment experiment = experiments.getExperiment(experimentID);
        if (experiment == null || experiment.getState().equals(TERMINATED)) {
            return;
        }
        Application.Name applicationName = experiment.getApplicationName();
        // Adjust the priority number to match the cassandra list index
        int adjustedPriorityNum = priorityNum - 1;

        // Check whether this experimentID exists in the priorityList
        List<Experiment.ID> experimentPriorityList = prioritiesRepository.getPriorityList(applicationName);
        if (experimentPriorityList.contains(experimentID)) {
            // If the given experimentID exists in the current priority list then check if it is already
            // at the requested priority, if it is then return else remove it from the priority list
            // So that we could add it later
            if (experimentPriorityList.indexOf(experimentID) == adjustedPriorityNum) {
                return;
            }
            experimentPriorityList.remove(experimentID);
        }
        int priorityListLength = prioritiesRepository.getPriorityListLength(applicationName);
        // If the requested priority is greater than the length of the priority list append the experiment
        // to the priority list
        if (adjustedPriorityNum >= priorityListLength) {
            experimentPriorityList.add(experimentID);
        }
        // If the requested priority is less negative, put the experiment at the top of the priority list
        else if (adjustedPriorityNum < 0) {
            experimentPriorityList.add(0, experimentID);
        }
        // Else, put the experiment at the requested position in the list
        else {
            experimentPriorityList.add(adjustedPriorityNum, experimentID);
        }
        prioritiesRepository.createPriorities(applicationName, experimentPriorityList);

        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=SET_PRIORITY, applicationName={}, configuration=[experimentName={}, priorityPosition={}]",
                experiment.getApplicationName(), experiment.getLabel(), priorityNum);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendToPriorityList(Experiment.ID experimentID) {
        Application.Name applicationName = experiments.getExperiment(experimentID).getApplicationName();
        List<Experiment.ID> priorityList = prioritiesRepository.getPriorityList(applicationName);
        if (priorityList == null) {
            priorityList = new ArrayList<>(1);
        }

        // TODO: This current logic does not modify the priority list if the given experimentID already exists in the
        // priority list. Check if it would be better to move the given experiment to the end of the list if it
        // already exists
        if (!priorityList.contains(experimentID)) {
            priorityList.add(experimentID);
            prioritiesRepository.createPriorities(applicationName, priorityList);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromPriorityList(Application.Name applicationName, Experiment.ID experimentID) {
        List<Experiment.ID> priorityList = prioritiesRepository.getPriorityList(applicationName);
        if (priorityList != null && priorityList.contains(experimentID)) {
            priorityList.remove(experimentID);
            prioritiesRepository.createPriorities(applicationName, priorityList);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPriorities(Application.Name applicationName, ExperimentIDList experimentIDList,
                                 boolean verifyPriorityList) {

        // Throw an exception if application name is invalid
        if (applicationName == null) {
            throw new ApplicationNotFoundException((Application.Name) null);
        }

        if (verifyPriorityList) {
            prioritiesRepository.createPriorities(applicationName, cleanPriorityList(applicationName,
                    experimentIDList.getExperimentIDs()));
        } else {
            prioritiesRepository.createPriorities(applicationName, experimentIDList.getExperimentIDs());
        }
    }

    public List<Experiment.ID> cleanPriorityList(Application.Name applicationName,
                                                 List<Experiment.ID> experimentIDs) {
        // Removing duplicates from the priority list
        Set<Experiment.ID> experimentSet = new LinkedHashSet<>(experimentIDs);

        // Removing the experiment ID's from the priority list that are invalid
        for (Experiment.ID experimentID : experimentIDs) {

            Experiment experiment = experiments.getExperiment(experimentID);

            // Remove DELETED experiments from the priorityList
            // Remove experiments not belonging to the pertinent application
            // Remove TERMINATED experiments from the priorityList
            if (experiment == null || !experiment.getApplicationName().equals(applicationName) ||
                    experiment.getState().equals(TERMINATED)) {
                experimentSet.remove(experimentID);
            }
        }

        // Adding valid (DRAFT, RUNNING, PAUSED) experiments from the experiment column family to the priorityList that
        // are missing
        List<Experiment> experimentList = experiments.getExperiments(applicationName);
        if (experimentList.size() != experimentSet.size()) {
            for (Experiment experiment : experimentList) {
                if (!experimentSet.contains(experiment.getID())) {
                    experimentSet.add(experiment.getID());
                }
            }
        }

        return new ArrayList<>(experimentSet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrioritizedExperimentList getPriorities(Application.Name applicationName, boolean verifyPriorityList) {
        if (verifyPriorityList) {
            List<Experiment.ID> priorityList = prioritiesRepository.getPriorityList(applicationName);
            List<Experiment> experimentList = experiments.getExperiments(applicationName);
            if ((priorityList != null ? priorityList.size() : 0) != experimentList.size()) {
                prioritiesRepository.createPriorities(applicationName, cleanPriorityList(applicationName, priorityList));
            }
        }
        return prioritiesRepository.getPriorities(applicationName);
    }


    /**
     * {@inheritDoc}
     */
    public Map<Experiment.ID, Integer> getPriorityPerID(Application.Name applicationName) {
        return getPriorities(applicationName, false)
                .getPrioritizedExperiments()
                .parallelStream()
                .collect(Collectors.toMap(PrioritizedExperiment::getID, PrioritizedExperiment::getPriority));
    }
}
