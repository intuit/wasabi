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
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experimentobjects.Experiment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages favorited experiments.
 */
@Path("/v1/favorites")
@Singleton
@Produces(APPLICATION_JSON)
@Api(value = "Allows to create and delete favorites.")
public class FavoritesResource {

    private static final Logger LOGGER = getLogger(FavoritesResource.class);

    private final HttpHeader httpHeader;
    private final Favorites favorites;
    private final Authorization authorization;

    /**
     * Instantiates the favorites resource.
     *
     * @param httpHeader    the HTTP header
     * @param favorites     the favorites implementation
     * @param authorization the authorization implementation
     */
    @Inject
    public FavoritesResource(final HttpHeader httpHeader,
                             final Favorites favorites, final Authorization authorization) {
        this.httpHeader = httpHeader;
        this.favorites = favorites;
        this.authorization = authorization;
    }

    /**
     * Favorite an experiment.
     *
     * @param authHeader the authorization
     * @param experiment the experiment JSON
     * @return the current list of experiments as an HTTP response
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

            @ApiParam(value = "id")
            final Experiment experiment) {
        try {
            UserInfo.Username userName = authorization.getUser(authHeader);

            List<Experiment.ID> favoriteList = favorites.addFavorite(userName, experiment.getID());

            return httpHeader.headers(Response.Status.OK).entity(prepareResponseEntity(favoriteList)).build();
        } catch (Exception exception) {
            LOGGER.error("postFavorite failed for experiment={} with error:", experiment, exception);
            throw exception;
        }
    }

    /**
     * Returns the current list of favorites for the authenticated user.
     *
     * @param authHeader the authorization
     * @return the current list of experiments as an HTTP response
     */
    @GET
    @ApiOperation(value = "Gets a list of favorites.",
            response = Response.class,
            httpMethod = "DELETE",
            protocols = "https")
    @Timed(name = "getFavorites")
    public Response getFavorites(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authHeader) {
        try {
            UserInfo.Username userName = authorization.getUser(authHeader);

            List<Experiment.ID> favoriteList = favorites.getFavorites(userName);

            return httpHeader.headers(Response.Status.OK).entity(prepareResponseEntity(favoriteList)).build();
        } catch (Exception exception) {
            LOGGER.error("getFavorites failed with error:", exception);
            throw exception;
        }
    }

    /**
     * Deletes a favorite
     *
     * @param authHeader   the authorization
     * @param experimentID the experiment ID
     * @return the current list of experiments as an HTTP response
     */
    @DELETE
    @Path("/{experimentID}")
    @ApiOperation(value = "Removes a favorite.",
            response = Response.class,
            httpMethod = "DELETE",
            protocols = "https")
    @Timed(name = "deleteFavorite")
    public Response deleteFavorite(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authHeader,

            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID) {
        try {
            UserInfo.Username userName = authorization.getUser(authHeader);

            List<Experiment.ID> favoriteList = favorites.deleteFavorite(userName, experimentID);

            return httpHeader.headers(Response.Status.OK).entity(prepareResponseEntity(favoriteList)).build();
        } catch (Exception exception) {
            LOGGER.error("deleteFavorite failed for experimentID={} with error:", experimentID, exception);
            throw exception;
        }
    }

    /**
     * Wraps the response list into a map to add it to the JSON key "experimentIDs".
     *
     * @param experimentIDs the list of experiment IDs
     * @return the experimentIDs wrapped in a map.
     */
    private Map<String, List<Experiment.ID>> prepareResponseEntity(List<Experiment.ID> experimentIDs) {
        Map<String, List<Experiment.ID>> responseEntity = new HashMap<>();
        responseEntity.put("experimentIDs", experimentIDs);
        return responseEntity;
    }
}
