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
package com.intuit.wasabi.exceptions;

import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.EXPERIMENT_NOT_FOUND;

/**
 * Indicates a specified experiment wasn't found
 *
 */
public class ExperimentNotFoundException extends WasabiClientException {

    private static final long serialVersionUID = -6468985512802871598L;

    public ExperimentNotFoundException(Experiment.ID experimentID) {
        this(experimentID, null);
    }

    public ExperimentNotFoundException(Experiment.ID experimentID,
                                       Throwable rootCause) {
        super(EXPERIMENT_NOT_FOUND, "Experiment \"" + experimentID + "\" not found", rootCause);
    }

    public ExperimentNotFoundException(Experiment.Label label) {
        this(label, null);
    }

    public ExperimentNotFoundException(Experiment.Label label,
                                       Throwable rootCause) {
        super(EXPERIMENT_NOT_FOUND, "Experiment with label \"" + label + "\" not found", rootCause);
    }

    public ExperimentNotFoundException(String message) {
        this(message, null);
    }

    protected ExperimentNotFoundException(String message,
                                          Throwable rootCause) {
        super(EXPERIMENT_NOT_FOUND, message, rootCause);
    }
}
