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
import com.intuit.wasabi.authorizationobjects.UserPermissions;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentIDList;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_MODEXP;
import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static com.intuit.wasabi.authorizationobjects.Permission.READ;
import static com.intuit.wasabi.authorizationobjects.Permission.UPDATE;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for accessing & managing applications
 */
@Path("/v1/applications")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Applications (Access-Manage Applications)")
public class ApplicationsResource {

    private final HttpHeader httpHeader;
    private final AuthorizedExperimentGetter authorizedExperimentGetter;
    private Experiments experiments;
    private Authorization authorization;
    private Pages pages;
    private Priorities priorities;

    /**
     * Logger for the class
     */
    private static final Logger LOGGER = getLogger(ApplicationsResource.class);

    @Inject
    ApplicationsResource(final AuthorizedExperimentGetter authorizedExperimentGetter,
                         final Experiments experiments,
                         final Authorization authorization,
                         final Priorities priorities, final Pages pages,
                         final HttpHeader httpHeader) {
        this.authorizedExperimentGetter = authorizedExperimentGetter;
        this.pages = pages;
        this.experiments = experiments;
        this.authorization = authorization;
        this.priorities = priorities;
        this.httpHeader = httpHeader;
    }

    /**
     * Returns a list of all applications.
     *
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Returns all applications")
    @Timed
    public Response getApplications(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            if (authorization.getUser(authorizationHeader) == null) {
                throw new AuthenticationException("User is not authenticated");
            }

            List<Application.Name> applications = experiments.getApplications();

            return httpHeader.headers().entity(applications).build();
        } catch (Exception exception) {
            LOGGER.error("Get applications request failed for provided authorization headers:", exception);
            throw exception;
        }
    }

    /**
     * Returns metadata for the specified experiment.
     * <p>
     * Does not return metadata for a deleted experiment.
     *
     * @param applicationName     the application name
     * @param experimentLabel     the experiment label
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("/{applicationName}/experiments/{experimentLabel}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return metadata for a single experiment",
            response = Experiment.class)
    @Timed
    public Response getExperiment(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("experimentLabel")
            @ApiParam(value = "Experiment Label")
            final Experiment.Label experimentLabel,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Experiment experiment = authorizedExperimentGetter
                    .getAuthorizedExperimentByName(authorizationHeader, applicationName, experimentLabel);

            return httpHeader.headers().entity(experiment).build();
        } catch (Exception exception) {
            LOGGER.error("getExperiment failed for applicationName={} & experimentLabel={} with error:",
                    applicationName,
                    experimentLabel,
                    exception);
            throw exception;
        }
    }

    /**
     * Returns metadata for all experiments within an application.
     * <p>
     * Does not return metadata for a deleted or terminated experiment.
     *
     * @param applicationName     the application name
     * @param authorizationHeader the authentication headers
     * @return a response containing a list with experiments
     */
    @GET
    @Path("/{applicationName}/experiments")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Returns metadata for all experiments within an application",
            response = ExperimentList.class)
    @Timed
    public Response getExperiments(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = false)
            final String authorizationHeader) {
        try {
            return httpHeader.headers().entity(authorizedExperimentGetter
                    .getExperimentsByName(true, authorizationHeader, applicationName)).build();
        } catch (Exception exception) {
            LOGGER.error("getExperiments failed for applicationName={} with error:", applicationName, exception);
            throw exception;
        }
    }

    /**
     * Creates a rank ordered priority list
     *
     * @param applicationName     the application name
     * @param experimentIDList    the list of experiment ids
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @PUT
    @Path("/{applicationName}/priorities")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create global priority list for an application",
            notes = "Experiments can only be placed in a priority list in DRAFT, RUNNING, and PAUSED states.")
    @Timed
    public Response createPriorities(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @ApiParam(required = true, defaultValue = DEFAULT_MODEXP)
            final ExperimentIDList experimentIDList,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, UPDATE);
            priorities.createPriorities(applicationName, experimentIDList, true);

            return httpHeader.headers(NO_CONTENT).build();
        } catch (Exception exception) {
            LOGGER.error("createPriorities failed for applicationName={} and experimentIDList={} with error:",
                    applicationName,
                    experimentIDList,
                    exception);
            throw exception;
        }
    }

    /**
     * Returns the full, ordered priority list for an application
     * along with experiment meta-data and associated priority
     *
     * @param applicationName     the application name
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("{applicationName}/priorities")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get the priority list for an application",
            notes = "The returned priority list is rank ordered.")
    //            response = ??, //todo: update with proper object
    @Timed
    public Response getPriorities(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, READ);

            PrioritizedExperimentList prioritizedExperiments =
                    priorities.getPriorities(applicationName, true);

            return httpHeader.headers().entity(prioritizedExperiments).build();
        } catch (Exception exception) {
            LOGGER.error("getPriorities failed for applicationName={} with error:", applicationName, exception);
            throw exception;
        }
    }

    /**
     * Returns the set of pages associated with the application.
     *
     * @param applicationName     the application name
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("{applicationName}/pages")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get the set of pages associated with an application.")
    @Timed
    public Response getPagesForApplication(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, READ);

            ImmutableMap<String, List<Page>> applicationPages = ImmutableMap.<String, List<Page>>builder()
                    .put("pages", pages.getPageList(applicationName)).build();

            return httpHeader.headers().entity(applicationPages).build();
        } catch (Exception exception) {
            LOGGER.error("getPagesForApplication failed for applicationName={} with error:", applicationName,
                    exception);
            throw exception;
        }
    }

    /**
     * Get the experiment information(id and allowNewAssignment) for the associated experiments for a page
     *
     * @param applicationName     the application name
     * @param pageName            the page name
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("{applicationName}/pages/{pageName}/experiments")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get the experiments associated to a page",
            notes = "The experiments returned belong to a single application")
    @Timed
    public Response getExperimentsForPage(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("pageName") final Page.Name pageName,
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true) final String authorizationHeader) {
        try {
            authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, READ);

            ImmutableMap<String, List<PageExperiment>> pageExperiments =
                    ImmutableMap.<String, List<PageExperiment>>builder()
                            .put("experiments", pages.getExperiments(applicationName, pageName)).build();

            return httpHeader.headers().entity(pageExperiments).build();
        } catch (Exception exception) {
            LOGGER.error("getExperimentsForPage failed for applicationName={} & pageName={} with error:",
                    applicationName,
                    pageName,
                    exception);
            throw exception;
        }
    }

    /**
     * Returns the set of pages with their associated experiments for an application.
     *
     * @param applicationName     the application name
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("{applicationName}/pageexperimentList")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get the set of pages associated with an application.")
    @Timed
    public Response getPagesAndAssociatedExperimentsForApplication(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, READ);

            Map<Page.Name, List<PageExperiment>> pageExperimentListMap =
                    pages.getPageAndExperimentList(applicationName);

            return httpHeader.headers().entity(pageExperimentListMap).build();
        } catch (Exception exception) {
            LOGGER.error("getPagesAndAssociatedExperimentsForApplication failed for applicationName={} with error:",
                    applicationName, exception);
            throw exception;
        }
    }

    /**
     * Returns a Map of allowed Applications to the corresponding
     * tags that have been stored with them.
     *
     * @param authorizationHeader the authorization headers
     * @return Response object containing the Applications with their tags
     */
    @GET
    @Path("/tags")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Returns a Map of Applications to their tags",
            response = Map.class)
    @Timed
    public Response getAllExperimentTags(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            UserInfo.Username userName = authorization.getUser(authorizationHeader);
            Set<Application.Name> allowed = new HashSet<>();

            // get the for the user visible Applications
            List<UserPermissions> authorized = authorization.getUserPermissionsList(userName).getPermissionsList();
            for (UserPermissions perm : authorized) {
                allowed.add(perm.getApplicationName());
            }

            // get their associated tags
            Map<Application.Name, Set<String>> allTags = experiments.getTagsForApplications(allowed);

            return httpHeader.headers().entity(allTags).build();
        } catch (Exception exception) {
            LOGGER.error("Retrieving the Experiment tags failed with error:",
                    exception);
            throw exception;
        }
    }
}
