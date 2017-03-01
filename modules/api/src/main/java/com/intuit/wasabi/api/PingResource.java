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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for checking application and infrastructure health
 */
@Path("/v1/ping")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Ping (Check Server Status)")
public class PingResource {

    private static final Logger LOGGER = getLogger(PingResource.class);

    private final HttpHeader httpHeader;
    private HealthCheckRegistry healthChecks;

    @Inject
    PingResource(final HealthCheckRegistry healthChecks, final HttpHeader httpHeader) {
        this.httpHeader = httpHeader;
        this.healthChecks = healthChecks;
    }

    /*
     * FIXME: Is apparently blocking if health-check fails
     * TODO: run periodically and push to graphite?
     */
    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Pings the server",
            notes = "Also returns the status of other components. " +
                    "Uses metrics-healthchecks and pings/check connections to MySql and Cassandra.",
            response = ComponentHealthList.class)
    @Timed
    public Response ping() {
        try {
            boolean status = true;
            Map<String, HealthCheck.Result> results = healthChecks.runHealthChecks();
            List<ComponentHealth> chs = new ArrayList<>();

            for (Entry<String, HealthCheck.Result> entry : results.entrySet()) {
                String serverId = entry.getKey();
                ComponentHealth h = new ComponentHealth(serverId);

                chs.add(h);
                h.setHealthy(entry.getValue().isHealthy());

                if (!entry.getValue().isHealthy()) {
                    status = false;

                    h.setDetailedMessage(entry.getValue().getMessage());
                }
            }

            ComponentHealthList componentHealthList = new ComponentHealthList(chs);

            componentHealthList.setVersion(httpHeader.getApplicationName());

            return httpHeader.headers(
                    status ? OK : SERVICE_UNAVAILABLE).type(APPLICATION_JSON_TYPE).entity(componentHealthList).build();
        } catch (Exception exception) {
            LOGGER.error("ping failed with error:", exception);
            throw exception;
        }
    }
}
