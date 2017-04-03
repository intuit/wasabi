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
package com.intuit.wasabi.experimentobjects.exceptions;

import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.State;

import java.util.Collection;
import java.util.EnumSet;

/**
 * This exception is thrown whenever an experiment state is updated in a way that
 * is not possible.
 */
public class InvalidExperimentStateException extends WasabiClientException {

    public InvalidExperimentStateException(Experiment.ID experimentID,
                                           State desiredState, State actualState) {
        this(experimentID, desiredState, actualState, null);
    }

    public InvalidExperimentStateException(Experiment.ID experimentID,
                                           Collection<State> desiredStates,
                                           State actualState) {
        this(experimentID, desiredStates, actualState, null);
    }

    public InvalidExperimentStateException(Experiment.ID experimentID,
                                           State desiredState, State actualState,
                                           Throwable rootCause) {
        this(experimentID,
                desiredState != null
                        ? EnumSet.of(desiredState)
                        : EnumSet.noneOf(State.class),
                actualState,
                rootCause);
    }

    public InvalidExperimentStateException(Experiment.ID experimentID,
                                           Collection<State> desiredStates,
                                           State actualState,
                                           Throwable rootCause) {
        super(ErrorCode.INVALID_EXPERIMENT_STATE, "Experiment \"" + experimentID +
                "\" in state \"" + actualState + "\" is not in desired state(s) \"" +
                desiredStates + "\")", rootCause);
    }

    public InvalidExperimentStateException(String message) {
        this(message, null);
    }

    public InvalidExperimentStateException(String message, Throwable rootCause) {
        super(ErrorCode.INVALID_EXPERIMENT_STATE, message, rootCause);
    }
}
