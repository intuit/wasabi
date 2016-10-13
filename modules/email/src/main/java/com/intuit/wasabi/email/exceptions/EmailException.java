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
package com.intuit.wasabi.email.exceptions;

import com.intuit.wasabi.exceptions.ErrorCode;
import com.intuit.wasabi.exceptions.WasabiClientException;

import static com.intuit.wasabi.exceptions.ErrorCode.EMAIL_SERVICE_ERROR;

/**
 * This exception is used for wrong uses of the EmailService.
 */
public class EmailException extends WasabiClientException {

    /**
     * Throws an {@link ErrorCode#EMAIL_SERVICE_ERROR} exception.
     *
     * @param message a detailed error message
     */
    public EmailException(String message) {
        this(message, null);
    }

    /**
     * Throws an {@link ErrorCode#EMAIL_SERVICE_ERROR} exception.
     *
     * @param message a detailed error message
     * @param cause   the cause of this exception
     */
    public EmailException(String message, Throwable cause) {
        this(EMAIL_SERVICE_ERROR, message, cause);
    }

    /**
     * Throws an EmailException.
     *
     * @param errorCode the error code from {@link ErrorCode}
     * @param message   a detailed error message
     */
    public EmailException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * Throws an EmailException.
     *
     * @param errorCode the error code from {@link ErrorCode}
     * @param message   a detailed error message
     * @param cause     the cause of this exception
     */
    public EmailException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }


}
