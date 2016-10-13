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
package com.intuit.wasabi.api.error;

import com.intuit.wasabi.api.HttpHeader;
import com.intuit.wasabi.exceptions.WasabiException;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Provider
public class WasabiExceptionProvider extends ExceptionProvider<WasabiException> {

    @Inject
    public WasabiExceptionProvider(final HttpHeader httpHeader, final ExceptionJsonifier exceptionJsonifier) {
        super(null, APPLICATION_JSON_TYPE, httpHeader, exceptionJsonifier);
    }
}
