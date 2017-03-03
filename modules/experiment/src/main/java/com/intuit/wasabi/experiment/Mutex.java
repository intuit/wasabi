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

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentIDList;
import com.intuit.wasabi.experimentobjects.ExperimentList;

import java.util.List;
import java.util.Map;

/**
 * Interface to manipulate mutual exclusion object for experiments.
 */
public interface Mutex {

    /**
     * Queries the database and returns a list of experiments, with metadata, that are mutually exclusive with
     * the specified experiment.
     *
     * @param experimentID the unique experiment id
     * @return a list of experiment objects containing mutually exclusive experiments
     */
    ExperimentList getExclusions(Experiment.ID experimentID);

    /**
     * Queries the database and returns a list of experiments, with metadata, that are NOT mutually exclusive with
     * the specified experiment.
     *
     * @param experimentID the unique experiment id
     * @return a list of experiment objects containing mutually exclusive experiments
     */
    ExperimentList getNotExclusions(Experiment.ID experimentID);

    /**
     * Creates a new mutual exclusion rule and adds it to the database
     *
     * @param experimentID     experimentID object containing experiment uuid
     * @param experimentIDList experimentList object containing list of experiment uuid's
     * @param user             the {@link UserInfo} who triggered the creation of an exclusion
     * @return List of Map
     */
    List<Map> createExclusions(Experiment.ID experimentID, ExperimentIDList experimentIDList, UserInfo user);

    /**
     * Deletes a mutual exclusion relation between 2 experiments by removing it from the database.
     *
     * @param expID1 ID of experiment 1
     * @param expID2 ID of experiment 2
     * @param user   the {@link UserInfo} who deleted the exclusion
     * @throws com.intuit.wasabi.exceptions.ExperimentNotFoundException if expID_1 or expID_2 is null.
     */
    void deleteExclusion(Experiment.ID expID1, Experiment.ID expID2, UserInfo user);


    /**
     * Performs a BFS on the mutual exclusions of the given Experiment.
     *
     * @param experiment the experiment to search mutual exclusions for
     * @return the list of recursively mutually exclusive experiments
     */
    List<Experiment> getRecursiveMutualExclusions(Experiment experiment);
}
