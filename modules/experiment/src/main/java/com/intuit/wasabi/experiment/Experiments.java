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
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateTransitionException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface to perform CRUD operations on experiment. In addition, it also
 * provides validation methods to check the state of experiments.
 */
public interface Experiments {

    /**
     * Creates a new experiment by adding the specified metadata to the database
     *
     * @param newExperiment {@link NewExperiment} object
     * @param user          the user creating the experiment
     */
    void createExperiment(NewExperiment newExperiment, UserInfo user);

    /**
     * Queries the database and returns metadata for the specified experiment.
     *
     * @param id The unique experiment id
     * @return an Experiment object, or null if the experiment is not found or
     * in state "deleted"
     */
    Experiment getExperiment(Experiment.ID id);

    /**
     * Queries the repository and returns metadata for the specified experiment and label.
     *
     * @param appName Name of application
     * @param label   {@link Experiment.Label}
     * @return an Experiment object, or null if the experiment is not found or
     * in state "deleted"
     */
    Experiment getExperiment(Application.Name appName, Experiment.Label label);

    /**
     * ONLY needed if we allow assigning to all experiments in a app
     * Queries the database and returns a list of experiments associated with
     * application appName with metadata.
     *
     * @param appName Name of application
     * @return A list of Experiment objects containing experiment metadata. It does not include experiments with state "deleted".
     */
    List<Experiment> getExperiments(Application.Name appName);

    /**
     * Queries the database and returns a list of experiments with metadata.
     *
     * @return a list of Experiment objects containing experiment metadata. It does not include experiments with state "deleted".
     */
    ExperimentList getExperiments();

    /**
     * Updates an experiment with specified experiment metadata.
     *
     * @param experimentID The unique experiment id
     * @param updates      Experiment object containing experiment metadata to
     *                     be updated
     * @param user         the user updating the experiment
     * @return Experiment object containing updated experiment metadata
     */
    Experiment updateExperiment(Experiment.ID experimentID, Experiment updates, UserInfo user);

    /**
     * Updates experiment state for given experiment
     *
     * @param experiment The experiment object containing existing experiment information
     * @param state The state the experiment needs to be updated to
     */
    void updateExperimentState(Experiment experiment, Experiment.State state);

    /**
     * Check state transition from current to desired state.
     *
     * @param experimentID The unique experiment id
     * @param currentState The current state of an experiment
     * @param desiredState The desired state of an experiment
     * @throws InvalidExperimentStateTransitionException if the state transition is invalid
     * @throws IllegalArgumentException                  if 1) No experiment buckets specified.
     *                                                   2) total bucket allocation is not 1.0. 3) More than one bucket is
     *                                                   specified as a control bucket
     */
    void checkStateTransition(Experiment.ID experimentID, Experiment.State currentState, Experiment.State desiredState);

    /**
     * Check for illegal update of an experiment.
     *
     * @param experiment Current Experiment
     * @param updates    Experiment metadata that will be checked against
     * @throws IllegalArgumentException if Experiment ID, ModificationTime or CreationTime is being updated
     */
    void checkForIllegalUpdate(Experiment experiment, Experiment updates);

    /**
     * Check for illegal update when current experiment is in TERMINATED state.
     *
     * @param experiment Current Experiment
     * @param updates    Experiment metadata that will be checked against
     * @throws IllegalArgumentException if the currentState is "TERMINATED" and if any attribute other than description is being updated
     */
    void checkForIllegalTerminatedUpdate(Experiment experiment, Experiment updates);

    /**
     * Check for illegal update when current experiment is RUNNING/PAUSED state.
     *
     * @param experiment Current Experiment
     * @param updates    Experiment metadata that will be checked against
     * @throws IllegalArgumentException if the experiment is in RUNNING/PAUSED
     *                                  state and if applicationName or Label is being updated. Also, if
     *                                  startTime/endTime is being updated with a value that has already
     *                                  passed or when the established startTime/endTime has already
     *                                  passed.
     */
    void checkForIllegalPausedRunningUpdate(Experiment experiment, Experiment updates);

    /**
     * Build updated experiment with the specified Experiment updates.
     *
     * @param experiment Current Experiment
     * @param updates    Experiment metadata that will be checked against
     * @param builder    {@link  Experiment.Builder}
     * @param changeList List of {@link Experiment.ExperimentAuditInfo}
     * @return true if update is required. changeList will be updated upon return.
     */
    boolean buildUpdatedExperiment(Experiment experiment, Experiment updates, Experiment.Builder builder,
                                   List<Experiment.ExperimentAuditInfo> changeList);

    /**
     * Update segmentation rule with the specified experiment metadata.
     *
     * @param experiment {@link Experiment} object
     * @param user       the user updating the segmentation rule.
     */
    void updateSegmentationRule(Experiment experiment, UserInfo user);

    /**
     * Queries the database and returns a list of application names
     *
     * @return a list of Application.Name types
     */
    List<Application.Name> getApplications();

    /**
     * Returns all tags that belong to the Applications specified in the given
     * {@link com.intuit.wasabi.experimentobjects.Application.Name}s.
     *
     * @param applicationNames the {@link com.intuit.wasabi.experimentobjects.Application.Name}s for which the
     *                         tags should be retrieved
     * @return a {@link Map} containing the Applications and their tags
     */
    Map<Application.Name, Set<String>> getTagsForApplications(Collection<Application.Name> applicationNames);
}
