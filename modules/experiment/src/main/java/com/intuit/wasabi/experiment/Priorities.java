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
package com.intuit.wasabi.experiment;

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentIDList;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;

import java.util.Map;

/**
 * Interface to perform CRUD operations for priority on experiments of an application.
 */
public interface Priorities {

    /**
     * Post a priority for an input experiment with the specified priority number.
     *
     * @param experimentID ID of experiment
     * @param priorityNum  The priority number assigned to the experiment
     */
    void setPriority(Experiment.ID experimentID, int priorityNum);

    /**
     * Appends an experiment to the priority list
     *
     * @param experimentID ID of experiment
     */
    void appendToPriorityList(Experiment.ID experimentID);

    /**
     * Removes the given experimentID from the application's priority list
     *
     * @param applicationName Name of application
     * @param experimentID    ID of experiment
     */
    void removeFromPriorityList(Application.Name applicationName, Experiment.ID experimentID);

    /**
     * Inserts a rank-ordered list of experiment IDs within an application into the database
     *
     * @param applicationName    Name of application
     * @param experimentIDList   A rank-ordered list of experiment IDs, rank-ordered
     * @param verifyPriorityList Flag to skip validation of the experimentIDList
     */
    void createPriorities(Application.Name applicationName, ExperimentIDList experimentIDList,
                          boolean verifyPriorityList);

    /**
     * Queries the database and returns the list of experiments, ordered by their priority,
     * within an application
     *
     * @param applicationName    Name of application
     * @param verifyPriorityList Flag to skip validation of the experimentIDList
     * @return A list of {@link PrioritizedExperiment} objects
     */
    PrioritizedExperimentList getPriorities(Application.Name applicationName, boolean verifyPriorityList);

    /**
     * Returns a mapping of experiment IDs to priorities for quick look ups.
     *
     * @param applicationName the application name
     * @return a map of experiment IDs to priorities
     */
    Map<Experiment.ID, Integer> getPriorityPerID(Application.Name applicationName);
}
