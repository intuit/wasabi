/*******************************************************************************
 * Copyright 2017 Intuit
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

package com.intuit.wasabi.api;

import com.intuit.wasabi.authentication.AuthenticateByHttpRequest;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final AuthenticateByHttpRequest authenticateByHttpRequest;

    @Inject
    public AuthenticationFilter(AuthenticateByHttpRequest authenticateByHttpRequest) {
        this.authenticateByHttpRequest = authenticateByHttpRequest;
    }

    /**
     * Filter the request.
     * <p>
     * An implementation may modify the state of the request or
     * create a new instance.
     *
     * @param request the request.
     * @return the request.
     */
    @Override
    public ContainerRequest filter(ContainerRequest request) {
        try {
            this.authenticateByHttpRequest.authenticate(request);
        } catch (Exception exception) {
            LOGGER.error("Authentication Failure. Exception:", exception);
            throw new AuthenticationException("Authentication failure.");
        }
        return request;
    }
}
