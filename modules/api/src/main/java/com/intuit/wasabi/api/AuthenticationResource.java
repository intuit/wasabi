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
import com.intuit.wasabi.authentication.Authentication;
import com.intuit.wasabi.authenticationobjects.exceptions.AuthenticationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

/**
 * API endpoint for managing user authentication
 */
@Path("/v1/authentication")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Authentication (Login-Logout)")
public class AuthenticationResource {

    private final HttpHeader httpHeader;
    private Authentication authentication;

    @Inject
    AuthenticationResource(final Authentication authentication, final HttpHeader httpHeader) {
        this.authentication = authentication;
        this.httpHeader = httpHeader;
    }

    /**
     * Log in user
     * @param authorizationHeader
     * @param grantType
     * @return Response object
     */
    @POST
    @Path("/login")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Log a user in")
    @Timed
    public Response logUserIn(@HeaderParam(AUTHORIZATION)
                              @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                              final String authorizationHeader,

                              @FormParam("grant_type")
                              @DefaultValue("client_credentials")
                              @ApiParam(value = "please enter client_credentials in this field")
                              final String grantType) {
        //FIXME: This should be taken out
        if (!"client_credentials".equals(grantType)) {
            throw new AuthenticationException("error, grant_type was not provided");
        }

        //pass the headers along to try and log the user in
        return httpHeader.headers().entity(authentication.logIn(authorizationHeader)).build();
    }

    /**
     * Verify token
     * @param tokenHeader
     * @return Response object
     */
    @GET
    @Path("/verifyToken")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Verify user's authorization")
    @Timed
    public Response verifyToken(@HeaderParam(AUTHORIZATION)
                                @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                final String tokenHeader) {
        return httpHeader.headers().entity(authentication.verifyToken(tokenHeader)).build();
    }

    /**
     * Log out user
     * @param tokenHeader
     * @return Response object
     */
    @GET
    @Path("/logout")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Log a user out")
    @Timed
    public Response logUserOut(@HeaderParam(AUTHORIZATION)
                               @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                               final String tokenHeader) {
        authentication.logOut(tokenHeader);
        return httpHeader.headers(NO_CONTENT).build();
    }

    /**
     * Check if user exists
     * @param userEmail Email of the user
      * @return Response object
    */
    @GET
    @Path("/users/{userEmail}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Check if user exists using user's email")
    @Timed
    public Response getUserExists(@PathParam("userEmail")
                                  @ApiParam(value = "Email of the user")
                                  final String userEmail) {
        return httpHeader.headers().entity(authentication.getUserExists(userEmail)).build();
    }
}
