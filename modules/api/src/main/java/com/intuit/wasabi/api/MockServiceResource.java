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

import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intuit.wasabi.exceptions.DatabaseException;
import com.intuit.wasabi.exceptions.WasabiServerException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import static java.util.Objects.nonNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for checking application and infrastructure health
 */
@Path("/v1/mock-service")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Mock Service")
public class MockServiceResource {

    private static final Logger LOGGER = getLogger(MockServiceResource.class);

    private final HttpHeader httpHeader;
    Random random = new SecureRandom();

    @Inject
    MockServiceResource(final HttpHeader httpHeader) {
        this.httpHeader = httpHeader;
    }

    @GET
    @Path("trinity/delays/{delayMS}/errors/{errorPercentage}")
    @Produces(APPLICATION_JSON)
    @Timed
    public Response trinity(
            @PathParam("delayMS")
            @ApiParam(value = "delayMS")
            @DefaultValue("10")
            final Integer delayMS,

            @PathParam("errorPercentage")
            @ApiParam(value = "errorPercentage")
            @DefaultValue("0")
            final Integer errorPercentage
     ) {
        try {

            try {
                Thread.sleep(delayMS);
            } catch (InterruptedException ie) {

            }

            Integer cPoint = random.nextInt(100);
            if(cPoint<=errorPercentage) {
                throw new DatabaseException("Artificial Exception...");
            }

            return httpHeader.headers(HttpStatus.SC_NO_CONTENT).build();
        } catch (Exception exception) {
            LOGGER.error("ping failed with error:", exception);
            throw exception;
        }
    }
}
