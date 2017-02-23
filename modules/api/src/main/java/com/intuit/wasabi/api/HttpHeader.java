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

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static com.intuit.wasabi.api.ApiAnnotations.ACCESS_CONTROL_MAX_AGE_DELTA_SECONDS;
import static com.intuit.wasabi.api.ApiAnnotations.APPLICATION_ID;
import static java.lang.Boolean.TRUE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;

public class HttpHeader {

    private final String applicationName;
    private final String deltaSeconds;
    private final CacheControl cacheControl;

    @Inject
    public HttpHeader(final @Named(APPLICATION_ID) String applicationName,
                      final @Named(ACCESS_CONTROL_MAX_AGE_DELTA_SECONDS) String deltaSeconds) {
        this.applicationName = applicationName;
        this.deltaSeconds = deltaSeconds;
        cacheControl = new CacheControl();

        cacheControl.setPrivate(TRUE);
        cacheControl.setNoCache(TRUE);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public ResponseBuilder headers() {
        return headers(OK);
    }

    public ResponseBuilder headers(Status status) {
        return headers(status.getStatusCode());
    }

    public ResponseBuilder headers(int status) {
        return status(status)
                .cacheControl(cacheControl)
                .header(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .header(ACCESS_CONTROL_ALLOW_HEADERS, "Authorization,X-Forwarded-For,Accept-Language,Content-Type")
                .header(ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "GET,POST,PUT,DELETE,OPTIONS")
                .header(ACCESS_CONTROL_MAX_AGE, deltaSeconds)
                .header("X-Application-Id", applicationName);
    }
}
