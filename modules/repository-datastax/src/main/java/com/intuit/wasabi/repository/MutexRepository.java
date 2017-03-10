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
package com.intuit.wasabi.repository;

import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Mutex (exclusion) repository
 *
 * @see Experiment
 */
public interface MutexRepository {

    /**
     * Delete an exclusion
     *
     * @param baseID base experiment id
     * @param pairID comparison experiment id
     * @throws RepositoryException exception
     */
    void deleteExclusion(Experiment.ID baseID, Experiment.ID pairID) throws RepositoryException;

    /**
     * Create an exclusion
     *
     * @param baseID base experiment id
     * @param pairID comparison experiment id
     * @throws RepositoryException exception
     */
    void createExclusion(Experiment.ID baseID, Experiment.ID pairID) throws RepositoryException;

    /**
     * Retrieve the list of experiments that are mutually exclusive to a given experiment
     *
     * @param baseID base experiment id
     * @return list of experiments
     * @throws RepositoryException exception
     */
    ExperimentList getExclusions(Experiment.ID baseID) throws RepositoryException;

    /**
     * Retrieve the list of experiments that are <b>not</b> mutually exclusive to a given experiment
     *
     * @param baseID base experiment id
     * @return list of <b>not</b> exclusive experiments
     * @throws RepositoryException exception
     */
    ExperimentList getNotExclusions(Experiment.ID baseID) throws RepositoryException;

    /**
     * Get the list of experiment IDs mutually exclusive to the given experiment
     *
     * @param experimentID current experiment id
     * @return list of experiment ids that were mutually exclusive to the given experiment id
     */
    List<Experiment.ID> getExclusionList(Experiment.ID experimentID);

    /**
     * Get a list of mutually exclusive experiments for an experiment in a single cassandra call
     *
     * @param experimentIDCollection collection of experiment IDs
     * @return Map of experiment ID and their mutually exclusive IDs
     */
    Map<Experiment.ID, List<Experiment.ID>> getExclusivesList(Collection<Experiment.ID> experimentIDCollection);
}
