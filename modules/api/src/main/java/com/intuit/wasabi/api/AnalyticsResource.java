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
import com.intuit.wasabi.analytics.ExperimentDetails;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCumulativeCounts;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentCumulativeStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.api.pagination.PaginationHelper;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_EMPTY;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_FILTER;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_PER_PAGE_CARDVIEW;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_SORT;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_TIMEZONE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_FILTER;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_PER_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_SORT;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_TIMEZONE;
import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static com.intuit.wasabi.authorizationobjects.Permission.READ;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for getting statistics about experiments
 */
@Path("/v1/analytics")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Analytics (Counts-Statistics for an experiment)")
public class AnalyticsResource {

    private static final Logger LOGGER = getLogger(AnalyticsResource.class);

    private final HttpHeader httpHeader;
    private final AuthorizedExperimentGetter authorizedExperimentGetter;
    private Analytics analytics;
    private ExperimentDetails experimentDetails;
    private PaginationHelper<ExperimentDetail> experimentDetailPaginationHelper;
    private Favorites favorites;
    private Authorization authorization;

    @Inject
    AnalyticsResource(
            final Analytics analytics,
            final AuthorizedExperimentGetter authorizedExperimentGetter,
            final HttpHeader httpHeader,
            final PaginationHelper<ExperimentDetail> experimentDetailPaginationHelper,
            final ExperimentDetails experimentDetails,
            final Favorites favorites,
            final Authorization authorization) {
        this.analytics = analytics;
        this.authorizedExperimentGetter = authorizedExperimentGetter;
        this.httpHeader = httpHeader;
        this.experimentDetails = experimentDetails;
        this.experimentDetailPaginationHelper = experimentDetailPaginationHelper;
        this.favorites = favorites;
        this.authorization = authorization;
    }

    /**
     * Returns a list of all not-deleted experiments the user has access to. Provides additional information
     * of the current state, with bucket information and analytic counts.
     * <p>
     * This endpoint is paginated. Favorites are sorted to the front.
     * If {@code per_page == -1}, favorites are ignored and all experiments are returned.
     * <p>
     * Sample call: curl -H 'Content-Type: application/json' --user admin:jabba01 -X POST
     * -d '{"fromTime":"2014-06-10T00:00:00-0000","toTime":"2014-06-10T00:00:00-0000","confidenceLevel":0.99,
     * "effectSize":0.0,"actions":[],"isSingleShot":false,"context":"prod"}'
     * 'http://localhost:8080/api/v1/analytics/experiments?per_page=5&page=1'
     *
     * @param authorizationHeader the authentication headers
     * @param context             the context in which the analytics data should be retrieved
     * @param page                the page which should be returned, defaults to 1
     * @param perPage             the number of experiments per page, defaults to 10. -1 to get all values.
     * @param sort                the sorting rules
     * @param filter              the filter rules
     * @param timezoneOffset      the time zone offset from UTC
     * @return a response containing a map with a list with {@code 0} to {@code perPage} experiments,
     * if that many are on the page, and a count of how many experiments match the filter criteria.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("/experiments")
    @ApiOperation(value = "Return details of all experiments with details "
            + "for the card view, with respect to the authorization",
            response = ExperimentDetail.class)
    @Timed
    public Response getExperimentDetails(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg \"QA\", \"PROD\"")
            final Context context,

            @QueryParam("page")
            @DefaultValue(DEFAULT_PAGE)
            @ApiParam(name = "page", defaultValue = DEFAULT_PAGE, value = DOC_PAGE)
            final int page,

            @QueryParam("per_page")
            @DefaultValue(DEFAULT_PER_PAGE_CARDVIEW)
            @ApiParam(name = "per_page", defaultValue = DEFAULT_PER_PAGE_CARDVIEW, value = DOC_PER_PAGE)
            final int perPage,

            @QueryParam("filter")
            @DefaultValue("")
            @ApiParam(name = "filter", defaultValue = DEFAULT_FILTER, value = DOC_FILTER)
            final String filter,

            @QueryParam("sort")
            @DefaultValue("")
            @ApiParam(name = "sort", defaultValue = DEFAULT_SORT, value = DOC_SORT)
            final String sort,

            @QueryParam("timezone")
            @DefaultValue(DEFAULT_TIMEZONE)
            @ApiParam(name = "timezone", defaultValue = DEFAULT_TIMEZONE, value = DOC_TIMEZONE)
            final String timezoneOffset) {
        try {
            if (authorizationHeader == null)
                throw new AuthenticationException("No authorization given.");

            List<ExperimentDetail> experimentList = experimentDetails.getExperimentDetailsBase();
            List<ExperimentDetail> authorizedExperiments = new ArrayList<>();

            UserInfo.Username userName = authorization.getUser(authorizationHeader);
            Set<Application.Name> allowed = new HashSet<>();

            for (ExperimentDetail experiment : experimentList) {
                if (experiment == null) {
                    continue;
                }

                Application.Name applicationName = experiment.getApplicationName();

                if (allowed.contains(applicationName)) {
                    authorizedExperiments.add(experiment);
                } else {
                    try {
                        authorization.checkUserPermissions(userName, applicationName, READ);
                        authorizedExperiments.add(experiment);
                        allowed.add(applicationName);
                    } catch (AuthenticationException ignored) {
                        LOGGER.trace("Ignoring Authentication Exception", ignored);
                    }
                }
            }

            List<Experiment.ID> favoriteList = favorites.getFavorites(userName);
            authorizedExperiments.parallelStream().filter(
                    experiment -> favoriteList.contains(experiment.getId()))
                    .forEach(experiment -> experiment.setFavorite(true));

            //filter and paginate
            Map<String, Object> experimentResponse = experimentDetailPaginationHelper
                    .paginate("experimentDetails", authorizedExperiments, filter, timezoneOffset,
                            (perPage != -1 ? "-favorite," : "") + sort, page, perPage);

            //and now add the analytics data
            Parameters parameters = createParameters(context);
            parameters.setMetric(BinomialMetrics.NORMAL_APPROX);
            parameters.parse();

            List<ExperimentDetail> expDetailsWithAnalytics =
                    (List<ExperimentDetail>) experimentResponse.get("experimentDetails");

            expDetailsWithAnalytics = experimentDetails.getAnalyticData(expDetailsWithAnalytics, parameters);
            experimentResponse.put("experimentDetails", expDetailsWithAnalytics);

            return httpHeader.headers().entity(experimentResponse).build();
        } catch (Exception exception) {
            LOGGER.error("getExperimentDetails failed for context={}, page={}, perPage={},"
                            + " filter={}, sort={}, timezoneOffset={} with error:",
                    context, page, perPage, filter, sort, timezoneOffset,
                    exception);
            throw exception;
        }
    }

    /**
     * Returns a number of summary counts for the specified experiment.
     * <p>
     * Returns unique and non-unique counts at the experiment, bucket,
     * and action levels for both actions and impressions.
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
        try {
            authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);
            parameters.parse();

            ExperimentCounts experimentCounts = analytics.getExperimentRollup(experimentID, parameters);

            return httpHeader.headers().entity(experimentCounts).build();
        } catch (Exception exception) {
            LOGGER.error("getExperimentCountsParameters failed for experimentID={}, parameters={} with error:",
                    experimentID, parameters, exception);
            throw exception;
        }
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
        try {
            // note: auth not required because the called method is authorized add context value to parameters
            Parameters parameters = createParameters(context);

            return getExperimentCountsParameters(experimentID, parameters, authorizationHeader);
        } catch (Exception exception) {
            LOGGER.error("getExperimentCounts failed for experimentID={}, context={} with error:",
                    experimentID, context, exception);
            throw exception;
        }
    }

    private Parameters createParameters(final Context context) {
        Parameters parameters = new Parameters();

        parameters.setContext(context);

        return parameters;
    }

    /**
     * Returns a number of summary counts for the specified experiment, by day.
     * <p>
     * Returns unique and non-unique counts at the experiment, bucket,
     * and action levels for both actions and impressions.
     * For each day, includes counts for that day and cumulative counts,
     * calculated from the beginning of the experiment.
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
        try {
            authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);
            parameters.parse();

            ExperimentCumulativeCounts experimentCumulativeCounts = analytics
                    .getExperimentRollupDailies(experimentID, parameters);

            return httpHeader.headers().entity(experimentCumulativeCounts).build();
        } catch (Exception exception) {
            LOGGER.error("getExperimentCountsDailiesParameters failed for experimentID={}, parameters={} with error:",
                    experimentID, parameters, exception);
            throw exception;
        }
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
        try {
            // note: auth not required because the called method is authorized add context value to parameters
            Parameters parameters = createParameters(context);

            return getExperimentCountsDailiesParameters(experimentID, parameters, authorizationHeader);
        } catch (Exception exception) {
            LOGGER.error("getExperimentCountsDailies failed for experimentID={}, context={} with error:",
                    experimentID, context, exception);
            throw exception;
        }
    }

    /**
     * Returns a number of summary counts and statistics for the specified experiment.
     * <p>
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
        try {
            authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);
            parameters.parse();

            ExperimentStatistics experimentStatistics = analytics.getExperimentStatistics(experimentID, parameters);

            return httpHeader.headers().entity(experimentStatistics).build();
        } catch (Exception exception) {
            LOGGER.error("getExperimentStatisticsParameters failed for experimentID={}, parameters={} with error:",
                    experimentID, parameters, exception);
            throw exception;
        }
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
        try {
            // note: auth not required because the called method is authorized add context value to parameters
            Parameters parameters = createParameters(context);

            return getExperimentStatisticsParameters(experimentID, parameters, authorizationHeader);
        } catch (Exception exception) {
            LOGGER.error("getExperimentStatistics failed for experimentID={}, context={} with error:",
                    experimentID, context, exception);
            throw exception;
        }
    }

    /**
     * Returns a number of summary counts and statistics for the specified experiment, by day
     * <p>
     * Returns unique and non-unique counts at the experiment, bucket,
     * and action levels for both actions and impressions.
     * For each day, includes counts for that day and cumulative counts,
     * calculated from the beginning of the experiment.
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
        try {
            authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);
            parameters.parse();

            ExperimentCumulativeStatistics experimentCumulativeStatistics = analytics
                    .getExperimentStatisticsDailies(experimentID, parameters);

            return httpHeader.headers().entity(experimentCumulativeStatistics).build();
        } catch (Exception exception) {
            LOGGER.error("getExperimentStatisticsDailiesParameters failed for "
                            + "experimentID={}, parameters={} with error:",
                    experimentID, parameters, exception);
            throw exception;
        }
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
        try {
            // note: auth not required because the called method is authorized add context value to parameters
            Parameters parameters = createParameters(context);

            return getExperimentStatisticsDailiesParameters(experimentID, parameters, authorizationHeader);
        } catch (Exception exception) {
            LOGGER.error("getExperimentStatisticsDailies failed for experimentID={}, context={} with error:",
                    experimentID, context, exception);
            throw exception;
        }
    }

    /**
     * Gets assignment counts for the experiment in total and the specific buckets.
     *
     * @param experimentID        the ID of the experiment that the counts should be retrieved for
     * @param context             can be either "PROD" or "QA"
     * @param authorizationHeader the authorization header for this call
     * @return The total {@link AssignmentCounts} and the AssignmentCounts per Bucket
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
        try {
            authorizedExperimentGetter.getAuthorizedExperimentById(authorizationHeader, experimentID);

            AssignmentCounts assignmentCounts = analytics.getAssignmentCounts(experimentID, context);

            return httpHeader.headers().entity(assignmentCounts).build();
        } catch (Exception exception) {
            LOGGER.error("getAssignmentCounts failed for experimentID={}, context={} with error:",
                    experimentID, context, exception);
            throw exception;
        }
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
        try {
            Experiment experiment = authorizedExperimentGetter
                    .getAuthorizedExperimentByName(authorizationHeader, applicationName, experimentLabel);
            AssignmentCounts assignmentCounts = analytics.getAssignmentCounts(experiment.getID(), context);

            return httpHeader.headers().entity(assignmentCounts).build();
        } catch (Exception exception) {
            LOGGER.error("getAssignmentCountsByApp failed for applicationName={}, "
                            + "experimentLabel={}, context={} with error:",
                    applicationName, experimentLabel, context,
                    exception);
            throw exception;
        }
    }
}
