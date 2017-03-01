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

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.ASSIGNMENT_FAILED;

public class AssignmentException extends WasabiServerException {

    private static final long serialVersionUID = 658471630279469017L;

    public AssignmentException(String message) {
        this(message, null);
    }

    public AssignmentException(String message, Throwable rootCause) {
        super(ASSIGNMENT_FAILED, message, rootCause);
    }
}
