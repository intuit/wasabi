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
package com.intuit.wasabi.experimentobjects.exception;

import com.intuit.wasabi.exceptions.ErrorCode;
import com.intuit.wasabi.exceptions.WasabiClientException;
import com.intuit.wasabi.experimentobjects.Application;

/**
 * Indicates a specified application name wasn't found
 */
public class ApplicationNotFoundException extends WasabiClientException {

    private static final long serialVersionUID = -4200908079010040920L;

    public ApplicationNotFoundException(Application.Name appName) {
        this(appName, null);
    }

    public ApplicationNotFoundException(String msg) {
        super(ErrorCode.APPLICATION_NOT_FOUND, msg);
    }

    public ApplicationNotFoundException(Application.Name appName,
                                        Throwable rootCause) {
        super(ErrorCode.APPLICATION_NOT_FOUND,
                "Application \"" + appName + "\" not found",
                rootCause);
    }
}
