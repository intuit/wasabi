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

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface for priorities repository
 */
public interface PrioritiesRepository {

    /**
     * Returns the priority list for an application
     *
     * @param applicationName name of application
     * @return PrioritizedExperimentList prioritized experiments
     */
    PrioritizedExperimentList getPriorities(Application.Name applicationName);

    /**
     * Get the length of the priority list for an application
     *
     * @param applicationName name of application
     * @return length of the priority list
     */

    int getPriorityListLength(Application.Name applicationName);

    /**
     * Update the priority list for an application
     *
     * @param applicationName        name of application
     * @param experimentPriorityList list of prioritized experiment ids in prioritized order
     */
    void createPriorities(Application.Name applicationName, List<Experiment.ID> experimentPriorityList);

    /**
     * Get prioritized experiment list by application name
     *
     * @param applicationName name of application
     * @return list of expriment ids
     */
    List<Experiment.ID> getPriorityList(Application.Name applicationName);

    /**
     * Returns the priority list for given set of applications
     *
     * @param applicationNames Set of application names
     * @return Map of PrioritizedExperimentList prioritized experiments for given application names.
     */
    Map<Application.Name, PrioritizedExperimentList> getPriorities(Collection<Application.Name> applicationNames);

}
