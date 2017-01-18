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

import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;

import java.util.Map;

/**
 * Signals a constraint violation in the database
 */
public class ConstraintViolationException extends WasabiClientException {

    private static final long serialVersionUID = 8173406361197589858L;
    private final Reason reason;

    public ConstraintViolationException(Reason reason) {
        this(reason, null, null);
    }

    public ConstraintViolationException(Reason reason, String message,
                                        Map<String, Object> properties) {
        super(reason.getErrorCode(), reason.getMessage() + ": " +
                (message != null ? message + " " : "") + "(" + properties + ")");
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    // TODO: This needs to go
    public enum Reason {
        NULL_CONSTRAINT_VIOLATION(ErrorCode.NULL_CONSTRAINT_VIOLATION, "A null constraint was violated"),
        UNIQUE_CONSTRAINT_VIOLATION(ErrorCode.UNIQUE_CONSTRAINT_VIOLATION, "An unique constraint was violated"),
        TIME_CONSTRAINT_VIOLATION(ErrorCode.TIME_CONSTRAINT_VIOLATION, "A time constraint was violated"),
        APPLICATION_CONSTRAINT_VIOLATION(ErrorCode.APPLICATION_CONSTRAINT_VIOLATION, "An application constraint was violated");

        private ErrorCode errorCode;
        private String message;

        Reason(ErrorCode errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        /*pkg*/ ErrorCode getErrorCode() {
            return errorCode;
        }

        /*pkg*/ String getMessage() {
            return message;
        }
    }
}
