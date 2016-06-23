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
import com.intuit.wasabi.analytics.Analytics;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCumulativeCounts;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentCumulativeStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_EMPTY;
import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * API endpoint for getting statistics about experiments
 */
@Path("/v1/analytics")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Analytics (Counts-Statistics for an experiment)")
public class AnalyticsResource {

    private final HttpHeader httpHeader;
    private final AuthorizedExperimentGetter authorizedExperimentGetter;
    private Analytics analytics;

    @Inject
    AnalyticsResource(final Analytics analytics, final AuthorizedExperimentGetter authorizedExperimentGetter,
                      final HttpHeader httpHeader) {
        this.analytics = analytics;
        this.authorizedExperimentGetter = authorizedExperimentGetter;
        this.httpHeader = httpHeader;
    }

    /**
     * Returns a number of summary counts for the specified experiment.
     *
     * Returns unique and non-unique counts at the experiment, bucket, and action levels for both actions and impressions.
     *
     * @param experimentID        the unique experiment ID
     * @param parameters          parameters to customize request
     * @param authorizationHeader the authorization headers
     * @return Response object
     */

    @POST
    @Path("/experiments/{experimentID}/counts")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return a summary of the data collected for the experiment",
            notes = "Return unique and non-unique counts at the experiment, bucket, and action levels " +
                    "for both actions and impressions.",
            response = ExperimentCounts.class)
    @Timed
    public Response getExperimentCountsParameters(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @ApiParam(required = true, defaultValue = DEFAULT_EMPTY)
            final Parameters parameters,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);
        parameters.parse();

        ExperimentCounts experimentCounts = analytics.getExperimentRollup(experimentID, parameters);

        return httpHeader.headers().entity(experimentCounts).build();
    }

    /**
     * A wrapper for {@link #getExperimentCountsParameters} API with default parameters.
     *
     * @param experimentID        the unique experiment ID
     * @param context             the context string
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("/experiments/{experimentID}/counts")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "A wrapper for POST /counts API with default parameters",
            response = ExperimentCounts.class)
    @Timed
    public Response getExperimentCounts(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg \"QA\", \"PROD\"")
            final Context context,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        // note: auth not required because the called method is authorized add context value to parameters
        Parameters parameters = createParameters(context);

        return getExperimentCountsParameters(experimentID, parameters, authorizationHeader);
    }

    private Parameters createParameters(final Context context) {
        Parameters parameters = new Parameters();

        parameters.setContext(context);

        return parameters;
    }

    /**
     * Returns a number of summary counts for the specified experiment, by day.
     *
     * Returns unique and non-unique counts at the experiment, bucket, and action levels for both actions and impressions.
     * For each day, includes counts for that day and cumulative counts, calculated from the beginning of the experiment.
     *
     * @param experimentID        the unique experiment ID
     * @param parameters          parameters to customize request
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @POST
    @Path("/experiments/{experimentID}/counts/dailies")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return a daily summary of the data collected for the experiment",
            notes = "Return unique and non-unique counts at the experiment, bucket, and action levels " +
                    "for both actions and impressions. For each day, includes counts for that day and " +
                    "cumulative counts, calculated from the beginning of the experiment.",
            response = ExperimentCumulativeCounts.class)
    @Timed
    public Response getExperimentCountsDailiesParameters(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @ApiParam(required = true, defaultValue = DEFAULT_EMPTY)
            final Parameters parameters,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);
        parameters.parse();

        ExperimentCumulativeCounts experimentCumulativeCounts =
                analytics.getExperimentRollupDailies(experimentID, parameters);

        return httpHeader.headers().entity(experimentCumulativeCounts).build();
    }

    /**
     * A wrapper for {@link #getExperimentCountsDailiesParameters} with default parameters.
     *
     * @param experimentID        the unique experiment ID
     * @param context             the context string
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("/experiments/{experimentID}/counts/dailies")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "A wrapper for POST /counts/dailies API with default parameters",
            response = ExperimentCumulativeCounts.class)
    @Timed
    public Response getExperimentCountsDailies(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg QA, PROD")
            final Context context,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        // note: auth not required because the called method is authorized add context value to parameters
        Parameters parameters = createParameters(context);

        return getExperimentCountsDailiesParameters(experimentID, parameters, authorizationHeader);
    }

    /**
     * Returns a number of summary counts and statistics for the specified experiment.
     *
     * Returns unique and non-unique counts at the experiment, bucket, and action levels for both actions and impressions.
     * Also returns a number of statistics calculated from the unique counts.
     *
     * @param experimentID        the unique experiment ID
     * @param parameters          parameters to customize request
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @POST
    @Path("/experiments/{experimentID}/statistics")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return a summary of the data collected for the experiment with derived statistics",
            notes = "Return unique and non-unique counts at the experiment, bucket, and action levels " +
                    "for both actions and impressions. Also returns a number of statistics calculated " +
                    "from the unique counts.",
            response = ExperimentStatistics.class)
    @Timed
    public Response getExperimentStatisticsParameters(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @ApiParam(required = true, defaultValue = DEFAULT_EMPTY)
            final Parameters parameters,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);
        parameters.parse();

        ExperimentStatistics experimentStatistics = analytics.getExperimentStatistics(experimentID, parameters);

        return httpHeader.headers().entity(experimentStatistics).build();
    }

    /**
     * A wrapper for {@link #getExperimentStatisticsParameters} with default parameters.
     *
     * @param experimentID        the unique experiment ID
     * @param context             the context string
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("/experiments/{experimentID}/statistics")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "A wrapper for POST /statistics API with default parameters",
            response = ExperimentStatistics.class)
    @Timed
    public Response getExperimentStatistics(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg QA, PROD")
            final Context context,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        // note: auth not required because the called method is authorized add context value to parameters
        Parameters parameters = createParameters(context);

        return getExperimentStatisticsParameters(experimentID, parameters, authorizationHeader);
    }

    /**
     * Returns a number of summary counts and statistics for the specified experiment, by day
     *
     * Returns unique and non-unique counts at the experiment, bucket, and action levels for both actions and impressions.
     * For each day, includes counts for that day and cumulative counts, calculated from the beginning of the experiment.
     * Also returns a number of statistics calculated from the unique counts.
     * Comparison statistics are only calculated based on cumulative counts
     *
     * @param experimentID        the unique experiment ID
     * @param parameters          parameters to customize request
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @POST
    @Path("/experiments/{experimentID}/statistics/dailies")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return a daily summary of the data collected for the experiment with derived statistics",
            notes = "Return unique and non-unique counts at the experiment, bucket, and action levels " +
                    "for both actions and impressions. For each day, includes counts for that day and " +
                    "cumulative counts, calculated from the beginning of the experiment. Also returns " +
                    "a number of statistics calculated from the unique counts.",
            response = ExperimentCumulativeStatistics.class)
    @Timed
    public Response getExperimentStatisticsDailiesParameters(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @ApiParam(required = true, defaultValue = DEFAULT_EMPTY)
            final Parameters parameters,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);
        parameters.parse();

        ExperimentCumulativeStatistics experimentCumulativeStatistics =
                analytics.getExperimentStatisticsDailies(experimentID, parameters);

        return httpHeader.headers().entity(experimentCumulativeStatistics).build();
    }

    /**
     * A wrapper for {@link #getExperimentStatisticsDailiesParameters} with default parameters.
     *
     * @param experimentID        the unique experiment ID
     * @param context             the context string
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("/experiments/{experimentID}/statistics/dailies")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "A wrapper for POST /statistics/dailies API with default parameters",
            response = ExperimentCumulativeStatistics.class)
    @Timed
    public Response getExperimentStatisticsDailies(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg QA, PROD")
            final Context context,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        // note: auth not required because the called method is authorized add context value to parameters
        Parameters parameters = createParameters(context);

        return getExperimentStatisticsDailiesParameters(experimentID, parameters, authorizationHeader);
    }

    /**
     * Get assignments count
     * @param experimentID
     * @param context
     * @param authorizationHeader
     * @return Response object
     */
    @GET
    @Path("/experiments/{experimentID}/assignments/counts")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return a summary of assignments delivered for an experiment",
            response = AssignmentCounts.class)
    @Timed
    public Response getAssignmentCounts(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg \"QA\", \"PROD\"")
            final Context context,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);

        AssignmentCounts assignmentCounts = analytics.getAssignmentCounts(experimentID, context);

        return httpHeader.headers().entity(assignmentCounts).build();
    }

    /**
     * Returns a summary of assignments delivered for an experiment in an application.
     *
     * @param applicationName     the application name
     * @param experimentLabel     the experiment label
     * @param context             the context
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("/applications/{applicationName}/experiments/{experimentLabel}/assignments/counts")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return a summary of assignments delivered for an experiment",
            response = AssignmentCounts.class)
    @Timed
    public Response getAssignmentCountsByApp(
            @PathParam("applicationName")
            @ApiParam("Application Name")
            final Application.Name applicationName,

            @PathParam("experimentLabel")
            @ApiParam(value = "Experiment Label")
            final Experiment.Label experimentLabel,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg \"QA\", \"PROD\"")
            final Context context,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        Experiment experiment = authorizedExperimentGetter.getAuthorizedExperimentByName(authorizationHeader,
                applicationName, experimentLabel);
        AssignmentCounts assignmentCounts = analytics.getAssignmentCounts(experiment.getID(), context);

        return httpHeader.headers().entity(assignmentCounts).build();
    }
}
