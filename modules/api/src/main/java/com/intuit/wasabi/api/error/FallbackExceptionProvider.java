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
package com.intuit.wasabi.api.error;

import com.google.inject.Inject;
import com.intuit.wasabi.api.HttpHeader;

import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * Handles Exception that fell through the services. As this shouldn't happen,
 * the errors are only mapped to internal server errors and all have the same
 * error message. This may be seen as an attempt to promote proper handling of
 * exceptional states in services.
 */
@Provider
public class FallbackExceptionProvider extends ExceptionProvider<Exception> {

    @Inject
    public FallbackExceptionProvider(final HttpHeader httpHeader, final ExceptionJsonifier exceptionJsonifier) {
        super(INTERNAL_SERVER_ERROR, APPLICATION_JSON_TYPE, httpHeader, exceptionJsonifier);
    }
}
