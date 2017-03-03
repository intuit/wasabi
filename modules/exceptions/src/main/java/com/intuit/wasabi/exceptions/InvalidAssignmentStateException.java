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

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.INVALID_ASSIGNMENT;

/**
 * Indicates an assignment for a user is in invalid state.
 */
public class InvalidAssignmentStateException extends WasabiClientException {

    private static final long serialVersionUID = 4334435887110480750L;

    public InvalidAssignmentStateException(User.ID userID, Application.Name applicationName,
                                           Experiment.Label experimentLabel) {
        this(userID, applicationName, experimentLabel, null);
    }

    public InvalidAssignmentStateException(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                           Throwable rootCause) {
        super(INVALID_ASSIGNMENT, new StringBuilder("Assignment for userID \"")
                .append(userID).append("\", applicationLabel \"").append(applicationName)
                .append("\", and experimentLabel \"").append(experimentLabel)
                .append("\" is in invalid state").toString(), rootCause);
    }

    public InvalidAssignmentStateException(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel, Assignment.Status status,
                                           Throwable rootCause) {
        super(INVALID_ASSIGNMENT, new StringBuilder("Assignment for userID \"")
                .append(userID).append("\", applicationLabel \"").append(applicationName)
                .append("\", and experimentLabel \"").append(experimentLabel)
                .append("\" is in invalid state \"").append(status).append("\"").toString(), rootCause);
    }

}
