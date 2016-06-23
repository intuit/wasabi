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

/**
 * Base exception for all our custom exceptions
 */
public abstract class WasabiException extends RuntimeException {

    private final ErrorCode errorCode;
    private String detailMessage;

    protected WasabiException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    protected WasabiException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, null, cause);
    }

    protected WasabiException(ErrorCode errorCode, String message,
                              Throwable cause) {
        super(errorCode.toString() + ": " + message, cause);

        this.errorCode = errorCode;
        if (message != null) {
            this.detailMessage = message;
        }
    }

    protected WasabiException(ErrorCode errorCode, String detailMessage) {
        this(errorCode, detailMessage, null);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    @Override
    public String getMessage() {
        return getDetailMessage();
    }
}
