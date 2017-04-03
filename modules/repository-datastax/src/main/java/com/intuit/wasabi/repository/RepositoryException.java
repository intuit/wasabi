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
package com.intuit.wasabi.repository;

import com.intuit.wasabi.exceptions.WasabiServerException;

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.REPOSITORY_ERROR;

/**
 * Signals that an error accessing or manipulating a repository has occurred
 */
public class RepositoryException extends WasabiServerException {

    /**
     *
     */
    private static final long serialVersionUID = 1814487426074671501L;

    /**
     * Constructor
     */
    public RepositoryException() {
        super(REPOSITORY_ERROR);
    }

    /**
     * Constructor
     *
     * @param message custom message to report
     */
    public RepositoryException(String message) {
        super(REPOSITORY_ERROR, message);
    }

    /**
     * Constructor
     *
     * @param rootCause root cause of the error
     */
    public RepositoryException(Throwable rootCause) {
        super(REPOSITORY_ERROR, rootCause);
    }

    /**
     * Constructor
     *
     * @param message   custom message to report
     * @param rootCause root cause of the error
     */
    public RepositoryException(String message, Throwable rootCause) {
        super(REPOSITORY_ERROR, message, rootCause);
    }
}
