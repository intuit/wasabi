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
import com.google.inject.name.Named;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.api.pagination.PaginationHelper;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.Permission;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.events.EventsExport;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.exceptions.BucketNotFoundException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.exceptions.TimeFormatException;
import com.intuit.wasabi.exceptions.TimeZoneFormatException;
import com.intuit.wasabi.experiment.Buckets;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experiment.Mutex;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.*;
import com.intuit.wasabi.repository.RepositoryException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_ALL;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_FILTER;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_MODBUCK;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_PER_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_PUTBUCK;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_SORT;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_TIMEZONE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_All;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_FILTER;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_PER_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_SORT;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_TIMEZONE;
import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static com.intuit.wasabi.api.ApiAnnotations.DEFAULT_TIME_FORMAT;
import static com.intuit.wasabi.api.ApiAnnotations.DEFAULT_TIME_ZONE;
import static com.intuit.wasabi.authorizationobjects.Permission.CREATE;
import static com.intuit.wasabi.authorizationobjects.Permission.READ;
import static com.intuit.wasabi.authorizationobjects.Permission.UPDATE;
import static com.intuit.wasabi.authorizationobjects.Role.ADMIN;
import static com.intuit.wasabi.authorizationobjects.UserRole.newInstance;
import static com.intuit.wasabi.experimentobjects.Experiment.State.DELETED;
import static com.intuit.wasabi.experimentobjects.Experiment.State.TERMINATED;
import static com.intuit.wasabi.experimentobjects.Experiment.from;
import static java.lang.Boolean.FALSE;
import static java.util.TimeZone.getTimeZone;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for managing experiments
 */
@Path("/v1/experiments")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Experiments (Create-Modify Experiments & Buckets)")
public class ExperimentsResource {

    private static final Logger LOGGER = getLogger(ExperimentsResource.class);
    private final String defaultTimezone;
    private final String defaultTimeFormat;
    private final HttpHeader httpHeader;
    private final PaginationHelper<Experiment> experimentPaginationHelper;
    private Experiments experiments;
    private EventsExport export;
    private Assignments assignments;
    private Authorization authorization;
    private Buckets buckets;
    private Mutex mutex;
    private Pages pages;
    private Priorities priorities;
    private Favorites favorites;

    @Inject
    ExperimentsResource(final Experiments experiments, final EventsExport export, final Assignments assignments,
                        final Authorization authorization, final Buckets buckets, final Mutex mutex,
                        final Pages pages, final Priorities priorities, final Favorites favorites,
                        final @Named(DEFAULT_TIME_ZONE) String defaultTimezone,
                        final @Named(DEFAULT_TIME_FORMAT) String defaultTimeFormat,
                        final HttpHeader httpHeader, final PaginationHelper<Experiment> experimentPaginationHelper
    ) {
        this.experiments = experiments;
        this.export = export;
        this.assignments = assignments;
        this.authorization = authorization;
        this.buckets = buckets;
        this.mutex = mutex;
        this.pages = pages;
        this.priorities = priorities;
        this.defaultTimezone = defaultTimezone;
        this.defaultTimeFormat = defaultTimeFormat;
        this.httpHeader = httpHeader;
        this.experimentPaginationHelper = experimentPaginationHelper;
        this.favorites = favorites;
    }

    /**
     * Returns a list of all experiments, with metadata. Does not return
     * metadata for deleted experiments.
     * <p>
     * This endpoint is paginated. Favorites are sorted to the front.
     * If {@code per_page == -1}, favorites are ignored and all experiments are returned.
     *
     * @param authorizationHeader the authentication headers
     * @param page                the page which should be returned, defaults to 1
     * @param perPage             the number of log entries per page, defaults to 10. -1 to get all values.
     * @param sort                the sorting rules
     * @param filter              the filter rules
     * @param timezoneOffset      the time zone offset from UTC
     * @return a response containing a map with a list with {@code 0} to {@code perPage} experiments,
     * if that many are on the page, and a count of how many experiments match the filter criteria.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return details of all the experiments, with respect to the authorization",
            response = ExperimentList.class)
    @Timed
    public Response getExperiments(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader,

            @QueryParam("page")
            @DefaultValue(DEFAULT_PAGE)
            @ApiParam(name = "page", defaultValue = DEFAULT_PAGE, value = DOC_PAGE)
            final int page,

            @QueryParam("per_page")
            @DefaultValue(DEFAULT_PER_PAGE)
            @ApiParam(name = "per_page", defaultValue = DEFAULT_PER_PAGE, value = DOC_PER_PAGE)
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
            final String timezoneOffset,

            @QueryParam("all")
            @DefaultValue("false")
            @ApiParam(name = "all", defaultValue = DEFAULT_ALL, value = DOC_All)
            final Boolean all) {

        try {
            ExperimentList experimentList = experiments.getExperiments();
            ExperimentList authorizedExperiments;

            if (authorizationHeader == null) {
                throw new AuthenticationException("No authorization given.");
            } else {
                Username userName = authorization.getUser(authorizationHeader);
                Set<Application.Name> allowed = new HashSet<>();

                authorizedExperiments = new ExperimentList();

                for (Experiment experiment : experimentList.getExperiments()) {
                    if (experiment == null) {
                        continue;
                    }

                    Application.Name applicationName = experiment.getApplicationName();

                    if (allowed.contains(applicationName)) {
                        authorizedExperiments.addExperiment(experiment);
                    } else {
                        try {
                            authorization.checkUserPermissions(userName, applicationName, READ);
                            authorizedExperiments.addExperiment(experiment);
                            allowed.add(applicationName);
                        } catch (AuthenticationException ignored) {
                            LOGGER.trace("ignoring authentication exception", ignored);
                        }
                    }
                }

                List<Experiment.ID> favoriteList = favorites.getFavorites(userName);
                authorizedExperiments.getExperiments().
                        parallelStream().filter(experiment -> favoriteList.contains(experiment.getID()))
                        .forEach(experiment -> experiment.setFavorite(true));
            }

            String experimentsResponseJsonKey = "experiments";
            List<Experiment> experimentsList = authorizedExperiments.getExperiments();
            Map<String, Object> experimentResponse = experimentPaginationHelper
                    .paginate(experimentsResponseJsonKey,
                            experimentsList,
                            filter, timezoneOffset,
                            (perPage != -1 ? "-favorite," : "") + sort,
                            page, perPage);

            if (all) {
                experimentsList.parallelStream()
                        .forEach(experiment -> {
                            Experiment.ID experimentID = experiment.getID();
                            BucketList bucketList = buckets.getBuckets(experimentID, false);
                            ExperimentList exclusionExperimentList = mutex.getExclusions(experimentID);
                            List<Experiment> exclusionList = exclusionExperimentList.getExperiments();
                            List<Experiment.ID> exclusionIdList = new ArrayList<>();
                            for (Experiment exclusionExperiment : exclusionList) {
                                exclusionIdList.add(exclusionExperiment.getID());
                            }

                            experiment.setBuckets(bucketList.getBuckets());
                            experiment.setExclusionIdList(exclusionIdList);

                            PrioritizedExperimentList prioritizedExperiments =
                                    priorities.getPriorities(experiment.getApplicationName(), true);
                            for (PrioritizedExperiment prioritizedExperiment :
                                    prioritizedExperiments.getPrioritizedExperiments()) {
                                if (prioritizedExperiment.getID().equals(experiment.getID())) {
                                    experiment.setPriority(prioritizedExperiment.getPriority());
                                }
                            }
                            ExperimentPageList experimentPageList = pages.getExperimentPages(experiment.getID());
                            experiment.setExperimentPageList(experimentPageList.getPages());
                        });
            }
            return httpHeader.headers().entity(experimentResponse).build();
        } catch (Exception exception) {
            LOGGER.error("getExperiments failed for page={}, perPage={}, "
                            + "filter={}, sort={}, timezoneOffset={} with error:",
                    page, perPage, filter, sort, timezoneOffset,
                    exception);
            throw exception;
        }
    }


    @POST
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Create an experiment",
            response = Experiment.class)
    @Timed
    public Response postExperiment(
            @ApiParam(required = true)
            final NewExperiment newExperiment,

            @QueryParam("createNewApplication")
            @DefaultValue("false")
            final boolean createNewApplication,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            if (newExperiment.getApplicationName() == null || isBlank(newExperiment.getApplicationName().toString())) {
                throw new IllegalArgumentException("Experiment application name cannot be null or an empty string");
            }

            Username userName = authorization.getUser(authorizationHeader);

            if (!createNewApplication) {
                authorization.checkUserPermissions(userName, newExperiment.getApplicationName(), CREATE);
            }

            // Avoid causing breaking change in API request body, derive creatorID from auth headers
            String creatorID = (userName != null) ? userName.getUsername() : null;

            newExperiment.setCreatorID(creatorID);

            // TODO - Should validate experiment before hand rather than catching errors later
            try {
                experiments.createExperiment(newExperiment, authorization.getUserInfo(userName));
            } catch (RepositoryException e) {
                throw new RepositoryException("Could not create experiment " + newExperiment + " because " + e);
            }

            Experiment experiment = experiments.getExperiment(newExperiment.getID());

            if (createNewApplication) {
                UserRole userRole = newInstance(experiment.getApplicationName(), ADMIN).withUserID(userName).build();

                authorization.setUserRole(userRole, null);
            }

            return httpHeader.headers(CREATED).entity(experiment).build();
        } catch (Exception exception) {
            LOGGER.error("postExperiment failed for newExperiment={}, createNewApplication={} with error:",
                    newExperiment, createNewApplication, exception);
            throw exception;
        }
    }

    /**
     * Returns metadata for the specified experiment. Does not return metadata
     * for a deleted experiment.
     *
     * @param experimentID        the unique experiment ID
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @GET
    @Path("{experimentID}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return details of a single experiment",
            response = Experiment.class)
    @Timed
    public Response getExperiment(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Experiment experiment = experiments.getExperiment(experimentID);

            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            if (authorizationHeader != null) {
                Username userName = authorization.getUser(authorizationHeader);

                authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
            }

            return httpHeader.headers().entity(experiment).type(APPLICATION_JSON_TYPE).build();
        } catch (Exception exception) {
            LOGGER.error("getExperiment failed for experimentID={} with error:", experimentID, exception);
            throw exception;
        }
    }

    /**
     * Modifies the metadata of the specified experiment, updating values
     * specified in the JSON body of the request.
     *
     * @param experimentID         the experiment id
     * @param experimentEntity     the experiment json payload
     * @param createNewApplication the boolean flag whether to create the experiment
     * @param authorizationHeader  the authorization headers
     * @return Response object contains metadata
     */
    @PUT
    @Path("{experimentID}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Update an experiment",
            notes = "Can update an experiment that is in DRAFT state to change the experiment state, or " +
                    "to change sampling percentage or to enable personalization and more.",
            response = Experiment.class)
    @Timed
    public Response putExperiment(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @ApiParam(value = "Please read the implementation notes above", required = true)
            final Experiment experimentEntity,

            @QueryParam("createNewApplication")
            @DefaultValue("false")
            final boolean createNewApplication,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            if (!createNewApplication) {
                authorization.checkUserPermissions(userName, experiment.getApplicationName(), UPDATE);
            }

            experiment = experiments.updateExperiment(experimentID, experimentEntity,
                    authorization.getUserInfo(userName));
            assert experiment != null : "Error updating experiment";

            if ((createNewApplication) && !experiment.getState().equals(DELETED)) {
                UserRole userRole = newInstance(experiment.getApplicationName(), ADMIN).withUserID(userName).build();

                authorization.setUserRole(userRole, null);
            }

            return experiment.getState().equals(DELETED) ?
                    httpHeader.headers(NO_CONTENT).build() :
                    httpHeader.headers().entity(experiment).build();
        } catch (Exception exception) {
            LOGGER.error("putExperiment failed for experimentID={}, experimentEntity={}, "
                            + "createNewApplication={} with error:",
                    experimentID, experimentEntity, createNewApplication,
                    exception);
            throw exception;
        }
    }

    /**
     * Deletes the specified experiment. An experiment can only be deleted if
     * it is in either the "draft" or "terminated" states. This is a soft
     * delete, hiding the experiment from the API without actually deleting it
     * from the database.
     *
     * @param experimentID        the experiment id
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @DELETE
    @Path("{experimentID}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Delete an experiment",
            notes = "Can only delete an experiment that is in DRAFT or TERMINATED state.  The default call is " +
                    "safe to use, but other than that please only delete experiments which you have created.")
    @Timed
    public Response deleteExperiment(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), Permission.DELETE);

            // Note: deleting an experiment follows the same rules as
            // updating its state to "deleted" -- so reuse the code.
            Experiment updatedExperiment = from(experiment).withState(DELETED).build();

            experiment = experiments.updateExperiment
                    (experimentID, updatedExperiment, authorization.getUserInfo(userName));

            assert experiment != null : "Error deleting experiment";

            return httpHeader.headers(NO_CONTENT).build();
        } catch (Exception exception) {
            LOGGER.error("deleteExperiment failed for experimentID={} with error:", experimentID, exception);
            throw exception;
        }
    }

    /**
     * Returns a list of all buckets with metadata for the specified experiment
     *
     * @param experimentID        the unique experiment ID
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @GET
    @Path("{experimentID}/buckets")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return all buckets for an experiment",
            response = BucketList.class)
    @Timed
    public Response getBuckets(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            if (authorizationHeader != null) {
                Username userName = authorization.getUser(authorizationHeader);

                authorization.checkUserPermissions(userName,
                        experiments.getExperiment(experimentID).getApplicationName(),
                        READ);
            }

            return httpHeader.headers().entity(buckets.getBuckets(experimentID, true)).build();
        } catch (Exception exception) {
            LOGGER.error("getBuckets failed for experimentID={} with error:", experimentID, exception);
            throw exception;
        }
    }

    /**
     * Creates a new bucket for the specified experiment, initializing its data
     * as specified in the JSON body of the request.
     *
     * @param experimentID        the experiment id
     * @param newBucketEntity     the experiment json payload
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @POST
    @Path("{experimentID}/buckets")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Modify a bucket for an experiment",
            notes = "Can only modify buckets for an experiment that is in DRAFT state.",
            response = Bucket.class)
    @Timed
    public Response postBucket(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @ApiParam(required = true, defaultValue = DEFAULT_MODBUCK)
            final Bucket newBucketEntity,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), CREATE);

            Bucket newBucket = Bucket.from(newBucketEntity).withExperimentID(experimentID).build();

            UserInfo user = authorization.getUserInfo(userName);
            Bucket bucket = buckets.createBucket(experimentID, newBucket, user);

            assert bucket != null : "Created bucket was null";

            LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=BUCKET_EDITED, applicationName={}, configuration=[userName={}, experimentName={}, bucket={}]",
                    experiment.getApplicationName(), userName.toString(), experiment.getLabel(), newBucket.toString());

            return httpHeader.headers(CREATED).entity(bucket).build();
        } catch (Exception exception) {
            LOGGER.error("postBucket failed for experimentID={}, newBucketEntity={} with error:",
                    experimentID, newBucketEntity, exception);
            throw exception;
        }
    }

    /**
     * Creates a new bucket for the specified experiment, initializing its data
     * as specified in the JSON body of the request.
     *
     * @param experimentID        the experiment id
     * @param bucketList          the list of buckets
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @PUT
    @Path("{experimentID}/buckets")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Modify buckets for an experiment",
            notes = "Can only modify buckets for an experiment that is in DRAFT state.",
            response = Bucket.class)
    @Timed
    public Response putBucket(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @ApiParam(required = true, defaultValue = DEFAULT_MODBUCK)
            final BucketList bucketList,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), UPDATE);

            UserInfo user = authorization.getUserInfo(userName);
            BucketList bucketList1 = buckets.updateBucketBatch(experimentID, bucketList, user);

            LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=BUCKETS_EDITED_BATCH, applicationName={}, configuration:[userName={}, experimentName={}, buckets={}]",
                    experiment.getApplicationName(), userName.toString(), experiment.getLabel(), bucketList.toString());

            return httpHeader.headers().entity(bucketList1).build();
        } catch (Exception exception) {
            LOGGER.error("putBucket failed for experimentID={}, bucketList={} with error:",
                    experimentID, bucketList, exception);
            throw exception;
        }

    }

    /**
     * Returns metadata for the specified bucket of the specified experiment.
     *
     * @param experimentID        the experiment id
     * @param bucketLabel         the bucket label
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @GET
    @Path("{experimentID}/buckets/{bucketLabel}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return a single bucket for an experiment",
            response = Bucket.class)
    @Timed
    public Response getBucket(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @PathParam("bucketLabel")
            @ApiParam(value = "Bucket Label")
            final Bucket.Label bucketLabel,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            //TODO: Duplicated code: move to a separate method
            if (authorizationHeader != null) {
                Username userName = authorization.getUser(authorizationHeader);
                Experiment experiment = experiments.getExperiment(experimentID);

                if (experiment == null) {
                    throw new ExperimentNotFoundException(experimentID);
                }

                authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
            }

            Bucket bucket = buckets.getBucket(experimentID, bucketLabel);

            if (bucket == null) {
                throw new BucketNotFoundException(bucketLabel);
            }

            return httpHeader.headers().entity(bucket).build();
        } catch (Exception exception) {
            LOGGER.error("getBucket failed for experimentID={}, bucketLabel={} with error:",
                    experimentID, bucketLabel, exception);
            throw exception;
        }
    }

    /**
     * Modifies the metadata of the specified bucket of the specified
     * experiment, updating values specified in the JSON body of the request.
     *
     * @param experimentID        the experiment id
     * @param bucketLabel         the bucket label
     * @param bucketEntity        the experiment json payload
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @PUT
    @Path("{experimentID}/buckets/{bucketLabel}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Update a bucket in an experiment",
            notes = "Can only update buckets for an experiment that is in DRAFT state.",
            response = Bucket.class)
    @Timed
    public Response putBucket(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @PathParam("bucketLabel")
            @ApiParam(value = "Bucket Label")
            final Bucket.Label bucketLabel,

            @ApiParam(required = true, defaultValue = DEFAULT_PUTBUCK)
            final Bucket bucketEntity,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), UPDATE);

            UserInfo user = authorization.getUserInfo(userName);
            Bucket bucket = buckets.updateBucket(experimentID, bucketLabel, bucketEntity, user);

            assert bucket != null : "Error updating bucket";

            return httpHeader.headers().entity(bucket).build();
        } catch (Exception exception) {
            LOGGER.error("putBucket failed for experimentID={}, bucketLabel={}, bucketEntity={} with error:",
                    experimentID, bucketLabel, bucketEntity,
                    exception);
            throw exception;
        }
    }

    /**
     * Modifies the state of the specified bucket of the specified
     * experiment.
     *
     * @param experimentID        the experiment id
     * @param bucketLabel         the bucket label
     * @param desiredState        the desired state
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @PUT
    @Path("{experimentID}/buckets/{bucketLabel}/state/{desiredState}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Update a bucket state",
            notes = "Can only close a bucket which is not in DRAFT state",
            response = Bucket.class)
    @Timed
    public Response putBucketState(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @PathParam("bucketLabel")
            @ApiParam(value = "Bucket Label")
            final Bucket.Label bucketLabel,

            @PathParam("desiredState")
            @ApiParam(value = "Desired Bucket State")
            final Bucket.State desiredState,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), UPDATE);

            UserInfo user = authorization.getUserInfo(userName);
            Bucket bucket = buckets.updateBucketState(experimentID, bucketLabel, desiredState, user);

            assert bucket != null : "Error updating bucket state";

            return httpHeader.headers().entity(bucket).build();
        } catch (Exception exception) {
            LOGGER.error("putBucketState failed for experimentID={}, bucketLabel={}, desiredState={} with error:",
                    experimentID, bucketLabel, desiredState,
                    exception);
            throw exception;
        }
    }

    /**
     * If the experiment is in "draft" state, immediately deletes the specified
     * bucket.  Otherwise returns an error.
     *
     * @param experimentID        the unique experiment id
     * @param bucketLabel         the unique bucket label
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @DELETE
    @Path("{experimentID}/buckets/{bucketLabel}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Delete a bucket in an experiment",
            notes = "Can only delete a bucket for experiment that is in DRAFT state.  The default call is " +
                    "safe to use, but other than that please only delete buckets which you have created.")
    @Timed
    public Response deleteBucket(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @PathParam("bucketLabel")
            @ApiParam(value = "Bucket Label")
            final Bucket.Label bucketLabel,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), Permission.DELETE);

            UserInfo user = authorization.getUserInfo(userName);

            buckets.deleteBucket(experimentID, bucketLabel, user);

            return httpHeader.headers(NO_CONTENT).build();
        } catch (Exception exception) {
            LOGGER.error("deleteBucket failed for experimentID={}, bucketLabel={} with error:",
                    experimentID, bucketLabel, exception);
            throw exception;
        }
    }

    /**
     * Export all events for the specified experiment, including both actions and impressions.
     * Returns a tab-delimited text file.
     *
     * @param experimentID        the unique experiment ID
     * @param authorizationHeader the authorization headers
     * @return Response object with tab-delimited rows
     */
    @GET
    @Path("{experimentID}/events")
    @Produces(TEXT_PLAIN)
    @ApiOperation(value = "Export all event records for an experiment",
            notes = "A wrapper for POST API with default parameters",
            response = StreamingOutput.class)
    @Timed
    public Response exportActions_get(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            //TODO: this check is redundant because it is checked again in regardless is exportActions
            //TODO: Duplicate code
            if (authorizationHeader != null) {
                Username userName = authorization.getUser(authorizationHeader);
                Experiment experiment = experiments.getExperiment(experimentID);

                if (experiment == null) {
                    throw new ExperimentNotFoundException(experimentID);
                }

                authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
            }

            return exportActions(experimentID, new Parameters(), authorizationHeader);
        } catch (Exception exception) {
            LOGGER.error("exportActions_get failed for experimentID={} with error:", experimentID, exception);
            throw exception;
        }
    }

    /**
     * Export all events for the specified experiment, including both actions and impressions.
     * Returns a tab-delimited text file.
     *
     * @param experimentID        the unique experiment ID
     * @param parameters          the user specified parameters
     * @param authorizationHeader the authorization headers
     * @return Response object with data in the form of tab-delimited rows
     */
    @POST
    @Path("{experimentID}/events")
    @Produces(TEXT_PLAIN)
    @ApiOperation(value = "Export all event records for an experiment",
            notes = "Download all event records for a given experiment in a tab-delimited text format.",
            response = StreamingOutput.class)
    @Timed
    public Response exportActions(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            final Parameters parameters,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);

            StreamingOutput stream = export.getEventStream(experimentID, parameters);

            return httpHeader.headers().
                    header("Content-Disposition", "attachment; filename=\"events.csv\"").
                    entity(stream).type(TEXT_PLAIN).build();
        } catch (Exception exception) {
            LOGGER.error("exportActions failed for experimentID={} with error:", experimentID, exception);
            throw exception;
        }
    }

    /**
     * Creates mutual exclusion rules for the specified experiment uuid. Returns
     * error if uuid is of an experiment that is in TERMINATED and DELETED state
     *
     * @param experimentID        the unique experiment ID
     * @param experimentIDList    JSON object containing metadata
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @POST
    @Path("{experimentID}/exclusions")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create mutual exclusion rules for an experiment",
            notes = "Can only create mutual exclusion rules for experiments in DRAFT, " +
                    "RUNNING, and PAUSED states within+" +
                    "the same application")
//          response = ??, //todo: update with proper object in @ApiOperation
    @Timed
    public Response createExclusions(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            final ExperimentIDList experimentIDList,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), CREATE);

            //this is the user that triggered the event and will be used for logging
            UserInfo user = authorization.getUserInfo(userName);
            List<Map> exclusions = mutex.createExclusions(experimentID, experimentIDList, user);

            return httpHeader.headers(CREATED)
                    .entity(ImmutableMap.<String, Object>builder().put("exclusions", exclusions).build()).build();
        } catch (Exception exception) {
            LOGGER.error("createExclusions failed for experimentID={}, experimentIDList={} with error:",
                    experimentID, experimentIDList, exception);
            throw exception;
        }
    }

    /**
     * Deletes mutual exclusion rules for the specified experiment uuid. Returns
     * error if experiments are not currently mutually exclusive.
     *
     * @param experimentID_1      the unique experiment ID (base)
     * @param experimentID_2      the unique experiment ID (pair)
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @DELETE
    @Path("/exclusions/experiment1/{experimentID_1}/experiment2/{experimentID_2}")
    @ApiOperation(value = "Delete a mutual exclusion relation",
            notes = "Can only delete mutual exclusion relations that currently " +
                    "exists.  This operation is symmetric")
//            response = ??, //todo: update with proper object in @ApiOperation
    @Timed
    public Response removeExclusions(
            @PathParam("experimentID_1")
            @ApiParam(value = "Experiment ID 1")
            final Experiment.ID experimentID_1,

            @PathParam("experimentID_2")
            @ApiParam(value = "Experiment ID 2")
            final Experiment.ID experimentID_2,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            //todo: do we want to check that exp1 and exp2 are in the same app?
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID_1);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID_1);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), Permission.DELETE);
            //this is the user that triggered the event and will be used for logging
            mutex.deleteExclusion(experimentID_1, experimentID_2, authorization.getUserInfo(userName));

            return httpHeader.headers(NO_CONTENT).build();
        } catch (Exception exception) {
            LOGGER.error("removeExclusions failed for experimentID_1={}, experimentID_2={} with error:",
                    experimentID_1, experimentID_2, exception);
            throw exception;
        }
    }

    /**
     * Returns a list of experiments that are mutually exclusive with input experimentID
     * showAll=True shows all experiments in all states, including DELETED.
     * showall=False only shows experiments in DRAFT, RUNNING, and PAUSED states.
     *
     * @param experimentID        the unique experiment ID
     * @param showAll             the flag to show all experiments (includes DELETED)
     * @param exclusive           the flag to indicate mutually exclusiveness
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @GET
    @Path("{experimentID}/exclusions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get list of mutually exclusive experiments",
            notes = "Shows list of all experiments, in all states, that are " +
                    "mutually exclusive with input experiment.")
    //            response = ??, //todo: update with proper object in @ApiOperation
    @Timed
    public Response getExclusions(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @QueryParam("showAll")
            @DefaultValue("true")
            final Boolean showAll,

            @QueryParam("exclusive")
            @DefaultValue("true")
            final Boolean exclusive,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            //TODO: Duplicate code. Move to separate method
            if (authorizationHeader != null) {
                Username userName = authorization.getUser(authorizationHeader);
                Experiment experiment = experiments.getExperiment(experimentID);

                if (experiment == null) {
                    throw new ExperimentNotFoundException(experimentID);
                }

                authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
            }

            ExperimentList experimentList =
                    exclusive ? mutex.getExclusions(experimentID) : mutex.getNotExclusions(experimentID);
            ExperimentList returnedExperiments = experimentList;

            if (!showAll) {
                ExperimentList allExperiments = new ExperimentList();

                for (Experiment experiment : experimentList.getExperiments()) {
                    if (!experiment.getState().equals(TERMINATED) && !experiment.getState().equals(DELETED)) {
                        allExperiments.addExperiment(experiment);
                    }
                }

                returnedExperiments = allExperiments;
            }

            return httpHeader.headers().entity(returnedExperiments).build();
        } catch (Exception exception) {
            LOGGER.error("getExclusions failed for experimentID={}, showAll={}, exclusive={} with error:",
                    experimentID, showAll, exclusive,
                    exception);
            throw exception;
        }
    }

    /**
     * Insert a priority for the input experiment into the priority list
     *
     * @param experimentID        the unique experiment ID
     * @param priorityNum         the desired priority position
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @POST
    @Path("{experimentID}/priority/{priorityPosition}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Insert an experiment into an application's priority list",
            notes = "Experiments can only be placed in a priority list DRAFT, RUNNING, and PAUSED states within" +
                    "the same application")
    @Timed
    public Response setPriority(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @PathParam("priorityPosition")
            final int priorityNum,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), CREATE);
            priorities.setPriority(experimentID, priorityNum);

            return httpHeader.headers(CREATED).build();
        } catch (Exception exception) {
            LOGGER.error("setPriority failed for experimentID={}, priorityNum={} with error:",
                    experimentID, priorityNum, exception);
            throw exception;
        }
    }

    /**
     * Returns all user assignments for a given experiment ID.  Will return null assignments
     * as well as those assigned to buckets within the experiment
     *
     * @param experimentID           the unique experiment ID
     * @param context                context (PROD, QA, etc.); Optional.
     * @param ignoreStringNullBucket if null bucket should be ignored
     * @param fromStringDate         the string formatted of the from date to download assignment
     * @param toStringDate           the string formatted of the to date to download assignment
     * @param timeZoneString         the string formatted of the timezone to use
     * @param authorizationHeader    the authorization headers
     * @return Response object containing metadata
     * @throws ParseException when date or timezone failed to parse
     */
    @GET
    @Path("{experimentID}/assignments")
    @Produces(TEXT_PLAIN)
    @ApiOperation(value = "Download a list of user assignments for a given experiment ID",
            notes = "Shows list of all user assignments for a given experiment ID. Returns both null" +
                    "assignments as well as bucket assignments.",
            response = StreamingOutput.class)
    @Timed
    public Response exportAssignments(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg QA, PROD")
            final Context context,

            @QueryParam("ignoreNullBucket")
            @DefaultValue("false")
            @ApiParam(value = "Filtering on the null bucket")
            final String ignoreStringNullBucket,

            @QueryParam("fromDate")
            @ApiParam(value = "from date to download assignments")
            final String fromStringDate,

            @QueryParam("toDate")
            @ApiParam(value = "to date to download assignments")
            final String toStringDate,

            @QueryParam("timeZone")
            @ApiParam(value = "value of the time zone")
            final String timeZoneString,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) throws ParseException {
        try {
            //TODO: Duplicate code: Move to separate method
            if (authorizationHeader != null) {
                Username userName = authorization.getUser(authorizationHeader);
                Experiment experiment = experiments.getExperiment(experimentID);

                if (experiment == null) {
                    throw new ExperimentNotFoundException(experimentID);
                }

                authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
            }

            //Initializing the parameters
            Parameters parameters = new Parameters();
            //Setting the filtering behavior on the null buckets --default is set to get all the assignments
            Boolean ignoreNullBucket = FALSE;

            if (ignoreStringNullBucket != null) {
                ignoreNullBucket = Boolean.parseBoolean(ignoreStringNullBucket);
            }

            parameters.setTimeZone(getTimeZone(defaultTimezone));

            //Input format of the dates
            SimpleDateFormat sdf = new SimpleDateFormat(defaultTimeFormat);

            if (timeZoneString != null) {
                TimeZone timeZone = getTimeZone(timeZoneString);

                //Note: TimeZone.getTimeZone doesn't have an inbuilt error catch. Hence, the below check is necessary.
                // Allowed time zones http://tutorials.jenkov.com/java-date-time/java-util-timezone.html
                if (!timeZone.getID().equals(timeZoneString)) {
                    throw new TimeZoneFormatException(timeZoneString);
                }

                sdf.setTimeZone(timeZone);
                // Resetting the default time zone value to user entered time zone.
                parameters.setTimeZone(timeZone);
            }

            if (fromStringDate != null) {
                try {
                    Date fromDate = sdf.parse(fromStringDate);

                    parameters.setFromTime(fromDate);
                } catch (ParseException e) {
                    throw new TimeFormatException(fromStringDate);
                }
            }

            if (toStringDate != null) {
                try {
                    Date toDate = sdf.parse(toStringDate);

                    parameters.setToTime(toDate);
                } catch (ParseException e) {
                    throw new TimeFormatException(toStringDate);
                }
            }

            StreamingOutput streamAssignment = assignments
                    .getAssignmentStream(experimentID, context, parameters, ignoreNullBucket);

            return httpHeader.headers()
                    .header("Content-Disposition", "attachment; filename =\"assignments.csv\"")
                    .entity(streamAssignment).build();
        } catch (Exception exception) {
            LOGGER.error("exportAssignments failed for experimentID={}, context={}, ignoreStringNullBucket={}," +
                            " fromStringDate={}, toStringDate={}, timeZoneString={} with error:",
                    experimentID, context, ignoreStringNullBucket, fromStringDate, toStringDate, timeZoneString,
                    exception);
            throw exception;
        }
    }

    /**
     * Add a list of pages to an experiment
     *
     * @param experimentID        the experiment id
     * @param experimentPageList  the list of experiment pages
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @POST
    @Path("{experimentID}/pages")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Post a list of pages to an experiment",
            notes = "Pages can only be added to an experiment with DRAFT, RUNNING, or PAUSED states")
    @Timed
    public Response postPages(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            final ExperimentPageList experimentPageList,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), CREATE);
            pages.postPages(experimentID, experimentPageList, authorization.getUserInfo(userName));

            return httpHeader.headers(CREATED).build();
        } catch (Exception exception) {
            LOGGER.error("postPages failed for experimentID={}, experimentPageList={} with error:",
                    experimentID, experimentPageList, exception);
            throw exception;
        }
    }

    /**
     * Remove the association of a page to an experiment
     *
     * @param experimentID        the experiment id
     * @param pageName            the page name
     * @param authorizationHeader the authorization headers
     * @return Response object
     */
    @DELETE
    @Path("{experimentID}/pages/{pageName}")
    @ApiOperation(value = "Remove a page from an experiment",
            notes = "Pages can only be added to an experiment with DRAFT, RUNNING, or PAUSED states")
    @Timed
    public Response deletePage(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @PathParam("pageName")
            @ApiParam(value = "Page name where the experiment will appear")
            final Page.Name pageName,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            // Throw an exception if the current experiment is not valid
            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), Permission.DELETE);
            pages.deletePage(experimentID, pageName, authorization.getUserInfo(userName));

            return httpHeader.headers(NO_CONTENT).build();
        } catch (Exception exception) {
            LOGGER.error("deletePage failed for experimentID={}, pageName={} with error:",
                    experimentID, pageName, exception);
            throw exception;
        }
    }

    /**
     * Get the page information(name and allowNewAssignment) for the associated pages for an experiment
     *
     * @param experimentID        the experiment id
     * @param authorizationHeader the authorization headers
     * @return Response object containing metadata
     */
    @GET
    @Path("{experimentID}/pages")
    @ApiOperation(value = "Get the associated pages information for a given experiment ID")
    @Timed
    public Response getExperimentPages(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID")
            final Experiment.ID experimentID,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            //TODO: Duplicate code: Move to separate method.
            if (authorizationHeader != null) {
                Username userName = authorization.getUser(authorizationHeader);
                Experiment experiment = experiments.getExperiment(experimentID);

                if (experiment == null) {
                    throw new ExperimentNotFoundException(experimentID);
                }

                authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
            }

            ExperimentPageList experimentPages = pages.getExperimentPages(experimentID);

            return httpHeader.headers().entity(experimentPages).build();
        } catch (Exception exception) {
            LOGGER.error("getExperimentPages failed for experimentID={} with error:", experimentID, exception);
            throw exception;
        }
    }

    /**
     * Generic Search api: Currently for just pages - Get the experiment list for the chosen page for an application
     *
     * @param applicationName the application name
     * @param pageName        the page name
     * @return Response object containing metadata of all experiments having the page for the application.
     */
    @GET
    @Path("/applications/{applicationName}/pages/{pageName}")
    @ApiOperation(value = "Return all experiments for page for app",
            response = ExperimentList.class)
    @Timed
    public Response getPageExperiments(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("pageName")
            @ApiParam(value = "Page name where the experiment will appear")
            final Page.Name pageName) {
        try {
            return httpHeader.headers().entity(pages.getPageExperiments(applicationName, pageName)).build();
        } catch (Exception exception) {
            LOGGER.error("getPageExperiments failed for applicationName={}, pageName={} with error:",
                    applicationName, pageName, exception);
            throw exception;
        }
    }


    /**
     *
     * FIXME: Traffic Analyzer change commented for Datastax-driver-migration release...
     *
     * Returns a summary of assignment ratios per day, containing several meta information like sampling
     * percentages and priorities.
     *
     * @param experimentID        the experiment ID
     * @param from                report start date, can be "START" to use the experiment's
     * @param to                  report end date, can be "END" to use the experiment's
     * @param authorizationHeader authorization
     * @param timezone            the timezone offset, +/-0000 or parsable by Java
     * @return a summary of assignment ratios per day.
     */


    /*

    @GET
    @Path("/{experimentID}/assignments/traffic/{from}/{to}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return a summary of assignments delivered for an experiment")
    @Timed
    public Response getExperimentAssignmentRatioPerDay(
            @PathParam("experimentID")
            @ApiParam(value = "Experiment ID", required = true)
            final Experiment.ID experimentID,

            @PathParam("from")
            @DefaultValue("")
            @ApiParam(value = "Start date of the format \"MM/DD/YYYY\" to use the experiment's start date, e.g. 8/23/1997. Must be URL encoded!",
                    required = true)
            final String from,

            @PathParam("to")
            @DefaultValue("")
            @ApiParam(value = "End date of the format \"MM/DD/YYYY\" to use the experiment's start date, e.g. 8/23/1997. Must be URL encoded!",
                    required = true)
            final String to,

            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader,

            @QueryParam("timezone")
            @DefaultValue(APISwaggerResource.DEFAULT_TIMEZONE)
            @ApiParam(value = "timezone offset, as parsable by https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html#of-java.lang.String-",
                    defaultValue = APISwaggerResource.DEFAULT_TIMEZONE)
            final String timezone
    ) {
        // Check authorization.
        Username username = authorization.getUser(authorizationHeader);
        Experiment experiment = getAuthorizedExperimentOrThrow(experimentID, username);

        // Parse from and to
        OffsetDateTime fromDate = parseUIDate(from, timezone, "from");
        OffsetDateTime toDate = parseUIDate(to, timezone, "to");

        List<Experiment> mutexExperiments = mutex.getRecursiveMutualExclusions(experiment);

        // Sort experiments by their priorities
        Map<Experiment.ID, Integer> experimentPriorities = priorities.getPriorityPerID(experiment.getApplicationName());
        Collections.sort(mutexExperiments, (e1, e2) -> experimentPriorities.get(e1.getID()) - experimentPriorities.get(e2.getID()));

        // Retrieve daily ratios
        ImmutableMap<String, ?> assignmentRatios = assignments.getExperimentAssignmentRatioPerDayTable(mutexExperiments, experimentPriorities, fromDate, toDate);

        // build table and dispatch it
        return httpHeader.headers().entity(assignmentRatios).build();
    }
    */

    /**
     * Gets the experiment for a given ID and checks if the user has permission.
     * If the user has READ permissions, the experiment is returned. Otherwise the authorization throws an exception.
     * <p>
     * Throws an exception if the experiment is not found.
     *
     * @param experimentID the experiment ID
     * @param username     the username
     * @return the experiment
     */
    /*test*/ Experiment getAuthorizedExperimentOrThrow(Experiment.ID experimentID, Username username) {
        Experiment experiment;
        if ((experiment = experiments.getExperiment(experimentID)) == null) {
            throw new ExperimentNotFoundException(experimentID);
        }
        authorization.checkUserPermissions(username, experiment.getApplicationName(), READ);
        // Hack: We need to make experiments compatible, converting CassandraExperiment
        experiment = Experiment.from(experiment).build();
        return experiment;
    }

    /**
     * Parses a UI-formatted date of the format M/d/y with a timezone offset.
     * If the date matches the key (case-insensitive), the onKeyMatch date is returned, otherwise
     * the parsed date is returned, taking the timezone offset into account.
     * <p>
     * Throws an IllegalArgumentException if the string can not be parsed.
     *
     * @param uiDate          the UI formatted date
     * @param timezoneOffset  the timezone offset of the user to take into account
     * @param debugIdentifier the debug identifier to identify problems in the exception
     * @return a parsed instant of the ui date
     */
    /*test*/ OffsetDateTime parseUIDate(String uiDate, String timezoneOffset, String debugIdentifier) {
        try {
            return OffsetDateTime.of(
                    LocalDateTime.of(LocalDate.from(DateTimeFormatter.ofPattern("M/d/y").parse(uiDate)),
                            LocalTime.MIDNIGHT), ZoneOffset.of(timezoneOffset));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    String.format("Can not parse \"%s\" date \"%s\", expecting format M/d/y, e.g. 05/24/2014.",
                            debugIdentifier, uiDate), e);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(
                    String.format("No proper timezoneOffset given (\"%s\"), expecting format -0000 or +0000.",
                            timezoneOffset));
        }
    }


}
