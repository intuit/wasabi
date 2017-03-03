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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.EventList;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.events.Events;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_EVENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for managing events
 */
@Path("/v1/events")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Events (Record-Manage Events)")
public class EventsResource {

    private static final Logger LOGGER = getLogger(EventsResource.class);

    private final Events events;
    private final HttpHeader httpHeader;

    @Inject
    EventsResource(final Events events, final HttpHeader httpHeader) {
        this.events = events;
        this.httpHeader = httpHeader;
    }

    /**
     * Submit events for the specified user within the context of a specific
     * application and experiment. Each event is an impression or action.
     * <p>
     * Example events structure
     * <p>
     * "events": [
     * {
     * "timestamp": "...",
     * "name": "..."
     * "IMPRESSION" for impressions
     * anything else for action
     * "payload": "..."
     * json string (with escaped quotes).
     * "value": ...
     * null: binary action success
     * number: continuous/counting action
     * string: categorical action
     * boolean: not currently used
     * }
     *
     * @param applicationName the application name
     * @param experimentLabel the experiment label
     * @param userID          the current user id
     * @param eventList       the {@link com.intuit.wasabi.analyticsobjects.EventList} event list
     * @return Response object
     * @throws Exception generic exception
     */
    @POST
    @Path("applications/{applicationName}/experiments/{experimentLabel}/users/{userID}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Submit a single event or a batch of events for the specified assigned user(customer)",
            notes = "*NOTE*: For a given user, please make sure that you have the user assignment done using the " +
                    "assignments API before using this API. An event is either an impression, indicating the user " +
                    "has been exposed to the treatment, or an action, indicating that the user has done something " +
                    "that you want to track. Please record impressions first and then action - use event " +
                    "name = \"IMPRESSION\" for impressions.")
    @Timed
    public Response recordEvents(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("experimentLabel")
            @ApiParam(value = "Experiment Label")
            final Experiment.Label experimentLabel,

            @PathParam("userID")
            @ApiParam(value = "Customer User ID that is already assigned using assignments API")
            final User.ID userID,

            @ApiParam(name = "eventList", required = true, value = "For impression", defaultValue = DEFAULT_EVENT)
            final EventList eventList)
            throws Exception {
        try {
            final Date NOW = new Date();
            Set<Context> contextSet = new HashSet<>();

            for (Event event : eventList.getEvents()) {
                if (event.getTimestamp() == null) {
                    event.setTimestamp(NOW);
                }

                contextSet.add(event.getContext());

                // TODO: add checking to Event.Name constructor instead of here
                if (event.getName() == null || isBlank(event.getName().toString())) {
                    throw new IllegalArgumentException("Event name cannot be null or an empty string");
                }
            }

            events.recordEvents(applicationName, experimentLabel, userID, eventList, contextSet);

            return httpHeader.headers(CREATED).build();
        } catch (Exception exception) {
            LOGGER.error("recordEvents failed for applicationName={},"
                            + " experimentLabel={}, userID={}, eventList={} with error:",
                    applicationName, experimentLabel, userID, eventList, exception);
            throw exception;
        }
    }

    /**
     * Submit events for users within the context of a specific application
     * and experiment. Each event is an impression or action.
     *
     * @param applicationName the application name
     * @param experimentLabel the experiment label
     * @param eventList       the {@link com.intuit.wasabi.analyticsobjects.EventList} event list
     * @throws UnsupportedOperationException UnsupportedOperationException
     */
    @POST
    @Path("applications/{applicationName}/experiments/{experimentLabel}/users")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Timed
    public Response recordUsersEvents(
            @PathParam("applicationName")
            final Application.Name applicationName,

            @PathParam("experimentLabel")
            final Experiment.Label experimentLabel,

            final Map<User.ID, List<Event>> eventList) {
        LOGGER.warn("recordUsersEvents is unsupported");
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Submit events for users and experiments within the context of a
     * specific application. Each event is an impression or action.
     *
     * @param applicationName the application name
     * @param eventList       the {@link com.intuit.wasabi.analyticsobjects.EventList} event list
     * @throws UnsupportedOperationException always throws
     */
    @POST
    @Path("applications/{applicationName}/experiments")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Timed
    public Response recordExperimentsEvents(
            @PathParam("applicationName") final Application.Name applicationName,
            final Map<Experiment.Label, Map<User.ID, List<Event>>> eventList) {
        LOGGER.warn("recordExperimentsEvents is unsupported");
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Returns number of events currently in the queue
     *
     * @return Response object
     */
    @GET
    @Path("queueLength")
    @Produces(APPLICATION_JSON)
    @Timed
    public Response getEventsQueueLength() {
        try {
            return httpHeader.headers().entity(events.queuesLength()).build();
        } catch (Exception exception) {
            LOGGER.error("getEventsQueueLength failed with error:", exception);
            throw exception;
        }
    }
}
