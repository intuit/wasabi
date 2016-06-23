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
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.events.EventsExport;
import com.intuit.wasabi.exceptions.*;
import com.intuit.wasabi.experiment.*;
import com.intuit.wasabi.experimentobjects.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.intuit.wasabi.api.APISwaggerResource.*;
import static com.intuit.wasabi.authorizationobjects.Permission.*;
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
    private Experiments experiments;
    private EventsExport export;
    private Assignments assignments;
    private Authorization authorization;
    private Buckets buckets;
    private Mutex mutex;
    private Pages pages;
    private Priorities priorities;

    @Inject
    ExperimentsResource(final Experiments experiments, final EventsExport export, final Assignments assignments,
                        final Authorization authorization, final Buckets buckets, final Mutex mutex,
                        final Pages pages, final Priorities priorities,
                        final @Named("default.time.zone") String defaultTimezone,
                        final @Named("default.time.format") String defaultTimeFormat,
                        final HttpHeader httpHeader) {
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
    }

    /**
     * Returns a list of all experiments, with metadata. Does not return
     * metadata for deleted experiments.
     *
     * @param authorizationHeader the ahtorization headers
     * @return Response object
     */
    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return details of all the experiments, with respect to the authorization",
            response = ExperimentList.class)
    @Timed
    public Response getExperiments(@HeaderParam(AUTHORIZATION)
                                   @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                   final String authorizationHeader) {
        ExperimentList experimentList = experiments.getExperiments();
        ExperimentList authorizedExperiments;

        if (authorizationHeader == null) {
            authorizedExperiments = experimentList;
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
        }

        return httpHeader.headers().entity(authorizedExperiments).build();
    }

    /**
     * Creates a new experiment, initializing its metadata as specified in the
     * JSON body of the request.
     *
     * @param newExperiment        the experiemnt to create
     * @param createNewApplication the boolean flag of whether to create new application
     * @param authorizationHeader  the authorization headers
     * @return Response object
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Create an experiment",
            response = Experiment.class)
    @Timed
    public Response postExperiment(@ApiParam(required = true)
                                   final NewExperiment newExperiment,

                                   @QueryParam("createNewApplication")
                                   @DefaultValue("false")
                                   final boolean createNewApplication,

                                   @HeaderParam(AUTHORIZATION)
                                   @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                   final String authorizationHeader) {
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
        experiments.createExperiment(newExperiment, authorization.getUserInfo(userName));

        Experiment experiment = experiments.getExperiment(newExperiment.getID());

        if (createNewApplication) {
            UserRole userRole = newInstance(experiment.getApplicationName(), ADMIN).withUserID(userName).build();

            authorization.setUserRole(userRole, null);
        }

        return httpHeader.headers(CREATED).entity(experiment).build();
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
    public Response getExperiment(@PathParam("experimentID")
                                  @ApiParam(value = "Experiment ID")
                                  final Experiment.ID experimentID,

                                  @HeaderParam(AUTHORIZATION)
                                  @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                  final String authorizationHeader) {
        Experiment experiment = experiments.getExperiment(experimentID);

        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        if (authorizationHeader != null) {
            Username userName = authorization.getUser(authorizationHeader);

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
        }

        return httpHeader.headers().entity(experiment).type(APPLICATION_JSON_TYPE).build();
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
    public Response putExperiment(@PathParam("experimentID")
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
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        // Throw an exception if the current experiment is not valid
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        if (!createNewApplication) {
            authorization.checkUserPermissions(userName, experiment.getApplicationName(), UPDATE);
        }

        experiment = experiments.updateExperiment(experimentID, experimentEntity, authorization.getUserInfo(userName));
        assert experiment != null : "Error updating experiment";

        if ((createNewApplication) && !experiment.getState().equals(DELETED)) {
            UserRole userRole = newInstance(experiment.getApplicationName(), ADMIN).withUserID(userName).build();

            authorization.setUserRole(userRole, null);
        }

        return experiment.getState().equals(DELETED) ?
                httpHeader.headers(NO_CONTENT).build() : httpHeader.headers().entity(experiment).build();
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
    public Response deleteExperiment(@PathParam("experimentID")
                                     @ApiParam(value = "Experiment ID")
                                     final Experiment.ID experimentID,

                                     @HeaderParam(AUTHORIZATION)
                                     @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                     final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), DELETE);

        // Note: deleting an experiment follows the same rules as
        // updating its state to "deleted" -- so reuse the code.
        Experiment updatedExperiment = from(experiment).withState(DELETED).build();

        experiment = experiments.updateExperiment(experimentID, updatedExperiment, authorization.getUserInfo(userName));

        assert experiment != null : "Error deleting experiment";

        return httpHeader.headers(NO_CONTENT).build();
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
    public Response getBuckets(@PathParam("experimentID")
                               @ApiParam(value = "Experiment ID")
                               final Experiment.ID experimentID,

                               @HeaderParam(AUTHORIZATION)
                               @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                               final String authorizationHeader) {
        if (authorizationHeader != null) {
            Username userName = authorization.getUser(authorizationHeader);

            authorization.checkUserPermissions(userName, experiments.getExperiment(experimentID).getApplicationName(),
                    READ);
        }

        return httpHeader.headers().entity(buckets.getBuckets(experimentID)).build();
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
    public Response postBucket(@PathParam("experimentID")
                               @ApiParam(value = "Experiment ID")
                               final Experiment.ID experimentID,

                               @ApiParam(required = true, defaultValue = DEFAULT_MODBUCK)
                               final Bucket newBucketEntity,

                               @HeaderParam(AUTHORIZATION)
                               @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                               final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), CREATE);

        Bucket newBucket = Bucket.from(newBucketEntity).withExperimentID(experimentID).build();

        LOGGER.warn("Bucket edited: user " + userName.toString() + " is adding bucket " + newBucket.toString() + " to experiment "
                + experimentID.toString());

        UserInfo user = authorization.getUserInfo(userName);
        Bucket bucket = buckets.createBucket(experimentID, newBucket, user);

        assert bucket != null : "Created bucket was null";

        return httpHeader.headers(CREATED).entity(bucket).build();
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
    public Response putBucket(@PathParam("experimentID")
                              @ApiParam(value = "Experiment ID")
                              final Experiment.ID experimentID,

                              @ApiParam(required = true, defaultValue = DEFAULT_MODBUCK)
                              final BucketList bucketList,

                              @HeaderParam(AUTHORIZATION)
                              @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                              final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), UPDATE);

        LOGGER.warn("Bucket edited: user " + userName.toString() + " is batch editing buckets for experiment " + experimentID
                .toString());

        UserInfo user = authorization.getUserInfo(userName);
        BucketList bucketList1 = buckets.updateBucketBatch(experimentID, bucketList, user);

        return httpHeader.headers().entity(bucketList1).build();
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
    public Response getBucket(@PathParam("experimentID")
                              @ApiParam(value = "Experiment ID")
                              final Experiment.ID experimentID,

                              @PathParam("bucketLabel")
                              @ApiParam(value = "Bucket Label")
                              final Bucket.Label bucketLabel,

                              @HeaderParam(AUTHORIZATION)
                              @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                              final String authorizationHeader) {
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
    public Response putBucket(@PathParam("experimentID")
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
    public Response putBucketState(@PathParam("experimentID")
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
    public Response deleteBucket(@PathParam("experimentID")
                                 @ApiParam(value = "Experiment ID")
                                 final Experiment.ID experimentID,

                                 @PathParam("bucketLabel")
                                 @ApiParam(value = "Bucket Label")
                                 final Bucket.Label bucketLabel,

                                 @HeaderParam(AUTHORIZATION)
                                 @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                 final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), DELETE);

        UserInfo user = authorization.getUserInfo(userName);

        buckets.deleteBucket(experimentID, bucketLabel, user);

        return httpHeader.headers(NO_CONTENT).build();
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
    public Response exportActions_get(@PathParam("experimentID")
                                      @ApiParam(value = "Experiment ID")
                                      final Experiment.ID experimentID,

                                      @HeaderParam(AUTHORIZATION)
                                      @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                      final String authorizationHeader) {
        //TODO: this check is redundant because it is checked again in regardless is exportActions
        if (authorizationHeader != null) {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
        }

        return exportActions(experimentID, new Parameters(), authorizationHeader);
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
    public Response exportActions(@PathParam("experimentID")
                                  @ApiParam(value = "Experiment ID")
                                  final Experiment.ID experimentID,

                                  final Parameters parameters,

                                  @HeaderParam(AUTHORIZATION)
                                  @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                  final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        // Throw an exception if the current experiment is not valid
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);

        StreamingOutput stream = export.getEventStream(experimentID, parameters);

        return httpHeader.headers()
                .header("Content-Disposition", "attachment; filename=\"events.csv\"")
                .entity(stream)
                .type(TEXT_PLAIN)
                .build();
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
    public Response createExclusions(@PathParam("experimentID")
                                     @ApiParam(value = "Experiment ID")
                                     final Experiment.ID experimentID,

                                     final ExperimentIDList experimentIDList,

                                     @HeaderParam(AUTHORIZATION)
                                     @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                     final String authorizationHeader) {
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
                .entity(ImmutableMap.<String, Object>builder().put("exclusions", exclusions).build())
                .build();
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
    public Response removeExclusions(@PathParam("experimentID_1")
                                     @ApiParam(value = "Experiment ID 1")
                                     final Experiment.ID experimentID_1,

                                     @PathParam("experimentID_2")
                                     @ApiParam(value = "Experiment ID 2")
                                     final Experiment.ID experimentID_2,

                                     @HeaderParam(AUTHORIZATION)
                                     @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                     final String authorizationHeader) {
        //todo: do we want to check that exp1 and exp2 are in the same app?
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID_1);

        // Throw an exception if the current experiment is not valid
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID_1);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), DELETE);
        //this is the user that triggered the event and will be used for logging
        mutex.deleteExclusion(experimentID_1, experimentID_2, authorization.getUserInfo(userName));

        return httpHeader.headers(NO_CONTENT).build();
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
    public Response getExclusions(@PathParam("experimentID")
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
        if (authorizationHeader != null) {
            Username userName = authorization.getUser(authorizationHeader);
            Experiment experiment = experiments.getExperiment(experimentID);

            if (experiment == null) {
                throw new ExperimentNotFoundException(experimentID);
            }

            authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);
        }

        ExperimentList experimentList = exclusive ? mutex.getExclusions(experimentID) : mutex.getNotExclusions(experimentID);
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
    public Response setPriority(@PathParam("experimentID")
                                @ApiParam(value = "Experiment ID")
                                final Experiment.ID experimentID,

                                @PathParam("priorityPosition")
                                final int priorityNum,

                                @HeaderParam(AUTHORIZATION)
                                @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        // Throw an exception if the current experiment is not valid
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), CREATE);
        priorities.setPriority(experimentID, priorityNum);

        return httpHeader.headers(CREATED).build();
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
    public Response exportAssignments(@PathParam("experimentID")
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

        StreamingOutput streamAssignment = assignments.getAssignmentStream(experimentID, context, parameters,
                ignoreNullBucket);

        return httpHeader.headers()
                .header("Content-Disposition", "attachment; filename =\"assignments.csv\"")
                .entity(streamAssignment)
                .build();
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
    public Response postPages(@PathParam("experimentID")
                              @ApiParam(value = "Experiment ID")
                              final Experiment.ID experimentID,

                              final ExperimentPageList experimentPageList,

                              @HeaderParam(AUTHORIZATION)
                              @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                              final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        // Throw an exception if the current experiment is not valid
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), CREATE);
        pages.postPages(experimentID, experimentPageList, authorization.getUserInfo(userName));

        return httpHeader.headers(CREATED).build();
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
    public Response deletePage(@PathParam("experimentID")
                               @ApiParam(value = "Experiment ID")
                               final Experiment.ID experimentID,

                               @PathParam("pageName")
                               @ApiParam(value = "Page name where the experiment will appear")
                               final Page.Name pageName,

                               @HeaderParam(AUTHORIZATION)
                               @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                               final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentID);

        // Throw an exception if the current experiment is not valid
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), DELETE);
        pages.deletePage(experimentID, pageName, authorization.getUserInfo(userName));

        return httpHeader.headers(NO_CONTENT).build();
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
    public Response getExperimentPages(@PathParam("experimentID")
                                       @ApiParam(value = "Experiment ID")
                                       final Experiment.ID experimentID,

                                       @HeaderParam(AUTHORIZATION)
                                       @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                       final String authorizationHeader) {
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
    public Response getPageExperiments(@PathParam("applicationName")
                                       @ApiParam(value = "Application Name")
                                       final Application.Name applicationName,

                                       @PathParam("pageName")
                                       @ApiParam(value = "Page name where the experiment will appear")
                                       final Page.Name pageName) {
        return httpHeader.headers().entity(pages.getPageExperiments(applicationName, pageName)).build();
    }
}
