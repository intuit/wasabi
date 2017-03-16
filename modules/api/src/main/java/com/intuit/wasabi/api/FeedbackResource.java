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
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.feedback.Feedback;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for managing user feedback
 */
@Path("/v1/feedback")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Feedback (Submit feedback about this AB Testing Tool)")
public class FeedbackResource {

    private static final Logger LOGGER = getLogger(FeedbackResource.class);

    private final HttpHeader httpHeader;
    private Authorization authorization;
    private Feedback feedback;

    @Inject
    FeedbackResource(final Authorization authorization, final Feedback feedback, final HttpHeader httpHeader) {
        this.authorization = authorization;
        this.feedback = feedback;
        this.httpHeader = httpHeader;
    }

    /**
     * Get all user feedback
     *
     * @param authorizationHeader
     * @return Response object
     */
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get all feedback")
    @Timed
    public Response getAllUserFeedback(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            authorization.checkSuperAdmin(authorization.getUser(authorizationHeader));

            ImmutableMap<String, List<UserFeedback>> userFeedback =
                    new ImmutableMap.Builder<String, List<UserFeedback>>()
                            .put("feedback", feedback.getAllUserFeedback()).build();

            return httpHeader.headers().entity(userFeedback).build();
        } catch (Exception exception) {
            LOGGER.error("getAllUserFeedback failed with error:", exception);
            throw exception;
        }
    }

    /**
     * Post feedback from user
     *
     * @param userFeedback
     * @param authorizationHeader
     * @return Response object
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Submit feedback")
    @Timed
    public Response postFeedback(
            @ApiParam(name = "userFeedback", value = "Please see model example", required = true)
            final UserFeedback userFeedback,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            userFeedback.setUsername(authorization.getUser(authorizationHeader));
            feedback.createUserFeedback(userFeedback);

            return httpHeader.headers(CREATED).build();
        } catch (Exception exception) {
            LOGGER.error("postFeedback failed for userFeedback={} with error:", userFeedback, exception);
            throw exception;
        }
    }

    /**
     * Get user feedback
     *
     * @param username
     * @param authorizationHeader
     * @return Response object
     */
    @GET
    @Path("/users/{username}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get single user feedback")
    @Timed
    public Response getUserFeedback(
            @PathParam("username")
            @ApiParam(value = "User name")
            final UserInfo.Username username,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            authorization.checkSuperAdmin(authorization.getUser(authorizationHeader));

            ImmutableMap<String, List<UserFeedback>> userFeedback =
                    new ImmutableMap.Builder<String, List<UserFeedback>>()
                            .put("feedbackList", feedback.getUserFeedback(username)).build();

            return httpHeader.headers().entity(userFeedback).build();
        } catch (Exception exception) {
            LOGGER.error("getUserFeedback failed for username={} with error:", username, exception);
            throw exception;
        }
    }
}
