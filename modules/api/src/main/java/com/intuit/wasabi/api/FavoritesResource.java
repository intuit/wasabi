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
package com.intuit.wasabi.api;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experimentobjects.Experiment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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


/**
 * TODO: documentation
 */
@Path("/v1/favorites")
@Singleton
@Produces(APPLICATION_JSON)
@Api(value = "Allows to create and delete favorites.")
public class FavoritesResource {

    private final HttpHeader httpHeader;
    private final Favorites favorites;
    private final Authorization authorization;

    /**
     *
     * @param httpHeader
     * @param favorites
     * @param authorization
     */
    @Inject
    public FavoritesResource(final HttpHeader httpHeader, final Favorites favorites, final Authorization authorization) {
        this.httpHeader = httpHeader;
        this.favorites = favorites;
        this.authorization = authorization;
    }

    /**
     *
     * @param authHeader
     * @param experimentID
     * @return
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Stores a favorite.",
            response = Response.class,
            httpMethod = "POST",
            produces = APPLICATION_JSON,
            protocols = "https")
    @Timed(name = "postFavorite")
    public Response postFavorite(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authHeader,

            @ApiParam(value = "experimentID")
            final Experiment.ID experimentID
    ) {
        UserInfo.Username userName = authorization.getUser(authHeader);

        favorites.addFavorite(userName, experimentID);

        List<Experiment.ID> favoriteList = favorites.getFavorites(userName);

        return httpHeader.headers(Response.Status.OK).entity(favoriteList).build();
    }

    /**
     *
     * @param authHeader
     * @return
     */
    @GET
    @ApiOperation(value = "Gets a list of favorites.",
            response = Response.class,
            httpMethod = "DELETE",
            protocols = "https")
    @Timed(name = "deleteFavorite")
    public Response getFavorites(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authHeader
    ) {
        UserInfo.Username userName = authorization.getUser(authHeader);

        List<Experiment.ID> favoriteList = favorites.getFavorites(userName);

        return httpHeader.headers(Response.Status.OK).entity(favoriteList).build();
    }

    /**
     *
     * @param authHeader
     * @param experimentID
     * @return
     */
    @DELETE
    @Path("/{experimentID}")
    @ApiOperation(value = "Removes a favorite.",
            response = Response.class,
            httpMethod = "DELETE",
            protocols = "https")
    @Timed(name = "deleteFavorites")
    public Response deleteFavorites(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authHeader,

            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID
    ) {
        UserInfo.Username userName = authorization.getUser(authHeader);

        favorites.deleteFavorite(userName, experimentID);

        List<Experiment.ID> favoriteList = favorites.getFavorites(userName);

        return httpHeader.headers(Response.Status.OK).entity(favoriteList).build();
    }
}
