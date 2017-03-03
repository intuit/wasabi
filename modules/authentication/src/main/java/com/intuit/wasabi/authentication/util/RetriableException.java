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
package com.intuit.wasabi.authentication.util;

/**
 * An exception thrown by the {@link Retriable} if it was not successful.
 */
public class RetriableException extends Exception {

    /**
     * A new RetriableException with a message.
     *
     * @param msg the message
     */
    public RetriableException(String msg) {
        this(msg, null);
    }

    /**
     * A new RetriableException with a message and a cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public RetriableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
