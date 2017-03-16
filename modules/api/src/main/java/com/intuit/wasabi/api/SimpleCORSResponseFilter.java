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
package com.intuit.wasabi.api;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Objects;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.intuit.wasabi.api.ApiAnnotations.ACCESS_CONTROL_MAX_AGE_DELTA_SECONDS;
import static com.intuit.wasabi.api.ApiAnnotations.APPLICATION_ID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

public class SimpleCORSResponseFilter implements ContainerResponseFilter {

    private static final String X_APPLICATION_ID = "X-Application-Id";
    private final static Logger LOGGER = getLogger(SimpleCORSResponseFilter.class);
    private final String applicationName;
    private final String deltaSeconds;

    @Inject
    public SimpleCORSResponseFilter(final @Named(APPLICATION_ID) String applicationName,
                                    final @Named(ACCESS_CONTROL_MAX_AGE_DELTA_SECONDS) String deltaSeconds) {
        LOGGER.info("Instantiated response filter {}", getClass().getName());
        this.applicationName = applicationName;
        this.deltaSeconds = deltaSeconds;
    }

    @Override
    public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse containerResponse) {
        LOGGER.trace("CORS filter called for request: {}", containerRequest);

        Response.ResponseBuilder response = Response.fromResponse(containerResponse.getResponse());

        if ("OPTIONS".equals(containerRequest.getMethod())) {
            response.status(Status.NO_CONTENT);
            if (Objects.isNull(containerResponse.getHttpHeaders().get(ACCESS_CONTROL_ALLOW_ORIGIN)))
                response.header(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            if (Objects.isNull(containerResponse.getHttpHeaders().get(ACCESS_CONTROL_ALLOW_METHODS)))
                response.header(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
            if (Objects.isNull(containerResponse.getHttpHeaders().get(ACCESS_CONTROL_REQUEST_METHOD)))
                response.header(ACCESS_CONTROL_REQUEST_METHOD, "GET, POST, PUT, DELETE, OPTIONS");
            if (Objects.isNull(containerResponse.getHttpHeaders().get(ACCESS_CONTROL_MAX_AGE)))
                response.header(ACCESS_CONTROL_MAX_AGE, deltaSeconds);
            if (Objects.isNull(containerResponse.getHttpHeaders().get(CONTENT_TYPE)))
                response.header(CONTENT_TYPE, APPLICATION_JSON);
            if (Objects.isNull(containerResponse.getHttpHeaders().get(X_APPLICATION_ID)))
                response.header(X_APPLICATION_ID, applicationName);
            response.entity(null);

            String requestHeader = containerRequest.getHeaderValue(ACCESS_CONTROL_REQUEST_HEADERS);

            if (requestHeader != null) {
                response.header(ACCESS_CONTROL_ALLOW_HEADERS, requestHeader);
            }
        }

        containerResponse.setResponse(response.build());

        return containerResponse;
    }


}
