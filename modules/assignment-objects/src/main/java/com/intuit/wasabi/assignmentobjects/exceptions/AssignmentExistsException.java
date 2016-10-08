/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.assignmentobjects.exceptions;

import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.exceptions.ErrorCode;
import com.intuit.wasabi.exceptions.WasabiClientException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;

/**
 * Indicates an assignment for a user exists and cannot be overwritten.
 */
public class AssignmentExistsException extends WasabiClientException {

    private static final long serialVersionUID = 4334435887110480748L;

    public AssignmentExistsException(User.ID userID, Application.Name applicationName,
                                     Experiment.Label experimentLabel) {
        this(userID, applicationName, experimentLabel, null);
    }

    public AssignmentExistsException(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                     Throwable rootCause) {
        super(ErrorCode.ASSIGNMENT_EXISTS_VIOLATION,
                String.format("Assignment for userID %s , applicationLabel %s, and experimentLabel %s already exists",
                        userID, applicationName, experimentLabel)
                , rootCause);
    }
}
