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
package com.intuit.wasabi.authenticationobjects.exceptions;


import com.intuit.wasabi.exceptions.ErrorCode;
import com.intuit.wasabi.exceptions.WasabiClientException;

public class AuthenticationException extends WasabiClientException {

    private static final long serialVersionUID = -6721537433687700932L;

    public AuthenticationException(String message) {
        this(message, null);
    }

    public AuthenticationException(String message, Throwable rootCause) {
        super(ErrorCode.AUTHENTICATION_FAILED, message, rootCause);
    }
}
