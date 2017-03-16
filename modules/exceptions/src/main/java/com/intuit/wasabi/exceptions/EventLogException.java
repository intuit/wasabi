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

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.EVENT_LOG_ERROR;

/**
 * An EventLogException can be thrown if an error in the EventLog occurs.
 */
public class EventLogException extends WasabiServerException {

    /**
     * Throws an {@link ErrorCode#EVENT_LOG_ERROR} exception.
     *
     * @param message a detailed error message
     */
    public EventLogException(String message) {
        this(message, null);
    }

    /**
     * Throws an {@link ErrorCode#EVENT_LOG_ERROR} exception.
     *
     * @param message a detailed error message
     * @param cause the cause of this exception
     */
    public EventLogException(String message, Throwable cause) {
        this(EVENT_LOG_ERROR, message, cause);
    }

    /**
     * Throws an EventLogException.
     *
     * @param errorCode the error code from {@link ErrorCode}
     * @param message a detailed error message
     */
    public EventLogException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * Throws an EventLogException.
     *
     * @param errorCode the error code from {@link ErrorCode}
     * @param message a detailed error message
     * @param cause the cause of this exception
     */
    public EventLogException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }


}
