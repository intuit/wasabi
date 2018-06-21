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
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.exceptions.AssignmentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBatch;
import com.intuit.wasabi.experimentobjects.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_LABELLIST;
import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static com.intuit.wasabi.assignmentobjects.Assignment.Status.EXPERIMENT_EXPIRED;
import static java.lang.Boolean.FALSE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for managing assignments
 */
@Path("/v1/assignments")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Assignments (Submit-Generate user(customer) bucket assignments)")
public class AssignmentsResource {

    private static final Logger LOGGER = getLogger(AssignmentsResource.class);
    private final HttpHeader httpHeader;
    private final Assignments assignments;
    private Authorization authorization;

    @Inject
    AssignmentsResource(final Assignments assignments, final HttpHeader httpHeader, Authorization authorization) {
        this.assignments = assignments;
        this.httpHeader = httpHeader;
        this.authorization = authorization;
    }

    /**
     * Returns a bucket assignment (bucket label) for the specified user within the context of
     * a specific application and experiment, if the user is chosen to be assigned to the experiment based on the
     * probability of sampling percent. Otherwise returns a null assignment for the specified user.
     * <p>
     * By default, creates the bucket assignment if one does not exist. Otherwise set {@code createAssignment} to false.
     *
     * @param applicationName       the unique application id
     * @param experimentLabel       the experiment label, unique within the context
     *                              of an application
     * @param userID                the unique user id
     * @param context               the context string
     * @param createAssignment      the flag to create the experiment if one does not exists
     * @param ignoreSamplingPercent the flag if the sampling percentage should be ignored
     * @param headers               the authorization headers
     * @return Response object
     * bucket payload
     */
    @GET
    @Path("applications/{applicationName}/experiments/{experimentLabel}/users/{userID}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return bucket assignment for a user",
            notes = "Generate the assignment first if the user has no assignment " +
                    "for this experiment.  Return null if the user is not in the experiment.")
    @Timed
    public Response getAssignment(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("experimentLabel")
            @ApiParam(value = "Experiment Label")
            final Experiment.Label experimentLabel,

            @PathParam("userID")
            @ApiParam(value = "User(customer) ID")
            final User.ID userID,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, e.g. PROD, QA")
            final Context context,

            @QueryParam("createAssignment")
            @DefaultValue("true")
            @ApiParam(value = "whether an assignment should be generated if one doesn't exist",
                    defaultValue = "true")
            final Boolean createAssignment,

            @QueryParam("ignoreSamplingPercent")
            @DefaultValue("false")
            @ApiParam(value = "whether the sampling percent for the experiment should be ignored, " +
                    "forcing the user into the experiment (if eligible)",
                    defaultValue = "false")
            final Boolean ignoreSamplingPercent,

            @javax.ws.rs.core.Context
            final HttpHeaders headers) {
        try {
            LOGGER.debug("getAssignment userID={}, applicationName={}, experimentLabel={}, context={},"
                            + " createAssignment={}, ignoreSamplingPercent={}, headers={}",
                    userID, applicationName, experimentLabel, context, createAssignment,
                    ignoreSamplingPercent, headers);

            Assignment assignment = getAssignment(userID, applicationName, experimentLabel, context, createAssignment,
                    ignoreSamplingPercent, null, headers,false);

            return httpHeader.headers().entity(toSingleAssignmentResponseMap(assignment)).build();
        } catch (Exception exception) {
            LOGGER.error("getAssignment failed for applicationName={}, experimentLabel={}, userID={}, context={},"
                            + " createAssignment={}, ignoreSamplingPercent={}, headers={} with error:",
                    applicationName, experimentLabel, userID, context, createAssignment, ignoreSamplingPercent, headers,
                    exception);
            throw exception;
        }
    }

    private Assignment getAssignment(final User.ID userID, final Application.Name applicationName,
                                     final Experiment.Label experimentLabel, final Context context,
                                     final boolean createAssignment, final boolean ignoreSamplingPercent,
                                     final SegmentationProfile segmentationProfile, final HttpHeaders headers,boolean forceProfileCheck) {
        Assignment assignment = assignments.doSingleAssignment(userID, applicationName, experimentLabel, context,
                createAssignment, ignoreSamplingPercent, segmentationProfile, headers,forceProfileCheck);

        // This should not happen when createAssignment == true
        if (isNull(assignment)) {
            throw new AssignmentNotFoundException(userID, applicationName, experimentLabel);
        }

        return assignment;
    }

    /**
     * Same as {@link Assignment}, but the user is chosen to be assigned to the experiment based on 1) profile
     * eligibility and 2) probability of sampling percent. Otherwise returns a null assignment for the specified user.
     *
     * @param applicationName       the unique application id
     * @param experimentLabel       the experiment label, unique within the context
     *                              of an application
     * @param userID                the unique user id
     * @param createAssignment      the flag to create the experiment if one does not exists
     * @param ignoreSamplingPercent the flag if the sampling percentage should be ignored
     * @param context               the context string
     * @param segmentationProfile   the {@link com.intuit.wasabi.assignmentobjects.SegmentationProfile} object
     * @param headers               the authorization headers
     * @return Response object
     */
    @POST
    @Path("applications/{applicationName}/experiments/{experimentLabel}/users/{userID}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return bucket assignment for a user",
            notes = "Generate the assignment first if the user has no assignment for this experiment. " +
                    "Forces the user to be in the experiment (if eligible based on profile).")
    @Timed
    public Response postAssignment(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("experimentLabel")
            @ApiParam(value = "Experiment Label")
            final Experiment.Label experimentLabel,

            @PathParam("userID")
            @ApiParam(value = "User(customer) ID")
            final User.ID userID,

            @QueryParam("createAssignment")
            @DefaultValue("true")
            @ApiParam(value = "whether an assignment should be generated if one doesn't exist",
                    defaultValue = "true")
            final Boolean createAssignment,

            @QueryParam("ignoreSamplingPercent")
            @DefaultValue("false")
            @ApiParam(value = "whether the sampling percent for the experiment should be ignored, " +
                    "forcing the user into the experiment (if eligible)",
                    defaultValue = "false")
            final Boolean ignoreSamplingPercent,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, e.g. PROD, QA")
            final Context context,

            @ApiParam(name = "segmentationProfile", value = "Segmentation Profile")
            final SegmentationProfile segmentationProfile,

            @QueryParam("forceProfileCheck")
            @DefaultValue("false")
            @ApiParam(value = "whether to force user profile match",
                    defaultValue = "false")
            final Boolean forceProfileCheck,

            @javax.ws.rs.core.Context
            final HttpHeaders headers) {
        try {
            LOGGER.debug("postAssignment userID={}, applicationName={}, experimentLabel={}, context={},"
                            + " createAssignment={}, ignoreSamplingPercent={}, segmentationProfile={}, headers={}",
                    userID, applicationName, experimentLabel, context, createAssignment, ignoreSamplingPercent,
                    segmentationProfile, headers);

            Assignment assignment = getAssignment(userID, applicationName, experimentLabel, context, createAssignment,
                    ignoreSamplingPercent, segmentationProfile, headers,forceProfileCheck);

            return httpHeader.headers().entity(toSingleAssignmentResponseMap(assignment)).build();
        } catch (Exception exception) {
            LOGGER.error("postAssignment failed for applicationName={}, experimentLabel={}, userID={}, context={},"
                            + " createAssignment={}, ignoreSamplingPercent={}," +
                            " segmentationProfile={}, headers={} with error:",
                    applicationName, experimentLabel, userID, context, createAssignment,
                    ignoreSamplingPercent, segmentationProfile, headers,
                    exception);
            throw exception;
        }
    }

    /**
     * Returns a bucket assignment for the specified user within the context of
     * one application and several experiments of an application
     *
     * @param applicationName  the application name
     * @param userID           the current user id
     * @param context          the context string
     * @param createAssignment the boolean flag to create
     * @param experimentBatch  the experiment batch expone, exptwo
     * @param headers          the authorization headers
     * @return Response object
     */
    @POST
    @Path("applications/{applicationName}/users/{userID}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return bucket assignments for a user across multiple experiments",
            notes = "Generate the assignments first if the user has no assignment for the specified experiments if the "
                    + "CREATE flag is set to true and the user is eligible with respect to the segmentation profile. "
                    + "Return null if the user is not in the experiment.")
    public Response getBatchAssignments(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("userID")
            @ApiParam(value = "User(customer) ID")
            final User.ID userID,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg QA, PROD")
            final Context context,

            @QueryParam("create")
            @DefaultValue("true")
            final Boolean createAssignment,

            @ApiParam(required = true, defaultValue = DEFAULT_LABELLIST)
            final ExperimentBatch experimentBatch,

            @QueryParam("forceProfileCheck")
            @DefaultValue("false")
            @ApiParam(value = "whether to force user profile match",
                    defaultValue = "false")
            final Boolean forceProfileCheck,

            @javax.ws.rs.core.Context
            final HttpHeaders headers) {
        try {
            LOGGER.debug("getBatchAssignment userID={}, applicationName={}, context={}, createAssignment={}, "
                            + "headers={}, experimentBatch={}",
                    userID, applicationName, context, createAssignment, headers, experimentBatch);

            List<Assignment> myAssignments = assignments.doBatchAssignments(userID, applicationName,
                    context, createAssignment, FALSE,
                    headers, experimentBatch,forceProfileCheck);

            return httpHeader.headers().entity(ImmutableMap.<String, Object>builder().put("assignments",
                    toBatchAssignmentResponseMap(myAssignments)).build()).build();
        } catch (Exception exception) {
            LOGGER.error("getBatchAssignments failed for applicationName={}, userID={}, context={}, "
                            + "createAssignment={}, experimentBatch={}, headers={} with error:",
                    applicationName, userID, context, createAssignment, experimentBatch, headers,
                    exception);
            throw exception;
        }
    }

    /**
     * Specify a bucket assignment for the specified user within the context of
     * a specific application and experiment.
     * <p>
     * Cannot use when the experiment is in DRAFT state because the buckets may change.
     *
     * @param applicationName the unique application id
     * @param experimentLabel the experiment label, unique within the context
     *                        of an application
     * @param userID          the unique user id
     * @param submittedData   the data submitted by user
     * @param context         the context string
     * @return Response object
     */
    @PUT
    @Path("applications/{applicationName}/experiments/{experimentLabel}/users/{userID}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    /*@ApiOperation(value = "Set a bucket assignment for a user",
            notes = "Set assignment to null if the user is not in the experiment.")*/
    @Timed
    public Response updateAssignment(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("experimentLabel")
            @ApiParam(value = "Experiment Label")
            final Experiment.Label experimentLabel,

            @PathParam("userID")
            @ApiParam(value = "User ID")
            final User.ID userID,

            @ApiParam(value = "Submitted Data")
            final Map<String, Object> submittedData,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg \"QA\", \"PROD\"")
            final Context context) {
        try {
            LOGGER.debug("udpateAssignment userID={}, applicationName={}, experimentLabel={}, context={}, "
                    + "submittedData={}", userID, applicationName, experimentLabel, context, submittedData);

            if (submittedData == null) {
                throw new IllegalArgumentException("Assignment JSON not found in request body");
            }

            Label submittedLabel;
            boolean overwrite;

            if (submittedData.containsKey("assignment")) {
                String submittedAssignment = (String) submittedData.get("assignment");

                // Bucket.Label constructor doesn't accept a null String (indicating assignment out of the experiment).
                // So we have to handle that case by explicitly setting submittedLabel to null.
                submittedLabel = submittedAssignment != null ? Label.valueOf(submittedAssignment) : null;
            } else {
                throw new IllegalArgumentException("Request entity JSON must contain an \"assignment\" property");
            }

            overwrite = submittedData.containsKey("overwrite") && (boolean) submittedData.get("overwrite");

            Assignment response = assignments
                    .putAssignment(userID, applicationName, experimentLabel, context, submittedLabel, overwrite);

            return httpHeader.headers().entity(toSingleAssignmentResponseMap(response)).build();
        } catch (Exception exception) {
            LOGGER.error("updateAssignment failed for applicationName={}, experimentLabel={},"
                            + " userID={}, submittedData={}, context={} with error:",
                    applicationName, experimentLabel, userID, submittedData, context,
                    exception);
            throw exception;
        }
    }

    /**
     * Create/Retrieve assignments for a single user for experiments associated to a single page
     *
     * @param applicationName       Application Name
     * @param pageName              Page Name
     * @param userID                User Id
     * @param createAssignment      Creates Assignment if set to true, default true
     * @param ignoreSamplingPercent Forces USer into experiment if set to true, default false
     * @param context               Environment Context
     * @param headers               Headers
     * @return Response object
     */
    @GET
    @Path("applications/{applicationName}/pages/{pageName}/users/{userID}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return bucket assignments for a user for all the experiments associated with a page",
            notes = "If you want to pass segmentation information, use the POST-Call for this method")
    @Timed
    public Response getBatchAssignmentForPage(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("pageName")
            @ApiParam(value = "Page Name")
            final Page.Name pageName,

            @PathParam("userID")
            @ApiParam(value = "User(customer) ID")
            final User.ID userID,

            @QueryParam("createAssignment")
            @DefaultValue("true")
            @ApiParam(value = "conditional to generate an assignment if one doesn't exist",
                    defaultValue = "true")
            final boolean createAssignment,

            @QueryParam("ignoreSamplingPercent")
            @DefaultValue("false")
            @ApiParam(value = "whether the sampling percent for the experiment should be ignored, " +
                    "forcing the user into the experiment (if eligible)",
                    defaultValue = "false")
            final boolean ignoreSamplingPercent,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg QA, PROD")
            final Context context,

            @javax.ws.rs.core.Context
                    HttpHeaders headers) {
        try {
            LOGGER.debug("getBatchAssignmentsForPage applicationName={}, pageName={}, userID={},"
                            + " context={}, createAssignment={}, ignoreSamplingPercent={}, headers={}",
                    applicationName, pageName, userID, context, createAssignment, ignoreSamplingPercent, headers);

            List<Assignment> assignmentsFromPage = assignments.doPageAssignments(applicationName,
                    pageName, userID, context, createAssignment, ignoreSamplingPercent, headers, null,false);

            return httpHeader.headers()
                    .entity(ImmutableMap.<String, Object>builder().put("assignments",
                            toBatchAssignmentResponseMap(assignmentsFromPage)).build()).build();
        } catch (Exception exception) {
            LOGGER.error("getBatchAssignmentsForPage failed for applicationName={}, pageName={}, userID={},"
                            + " createAssignment={}, ignoreSamplingPercent={}, context={}, headers={} with error:",
                    applicationName, pageName, userID, createAssignment, ignoreSamplingPercent, context, headers,
                    exception);
            throw exception;
        }
    }

    /**
     * Create/Retrieve assignments for a single user for experiments associated to a single page
     *
     * @param applicationName       Application Name
     * @param pageName              Page Name
     * @param userID                User Id
     * @param createAssignment      createAssignment boolean true will create an assignment
     * @param ignoreSamplingPercent If true, will force user into experiment, default false
     * @param context               Environment context
     * @param segmentationProfile   the {@link com.intuit.wasabi.assignmentobjects.SegmentationProfile} object
     * @param headers               Headers
     * @return Response object which is List of Assignments for user for experiment of the page.
     */
    @POST
    @Path("applications/{applicationName}/pages/{pageName}/users/{userID}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Return bucket assignments for a user for all the experiments associated with a page",
            notes = "The mutual exclusion and segmentation rules apply")
    @Timed
    public Response postBatchAssignmentForPage(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("pageName")
            @ApiParam("Page Name")
                    Page.Name pageName,

            @PathParam("userID")
            @ApiParam(value = "User(customer) ID")
            final User.ID userID,

            @QueryParam("createAssignment")
            @DefaultValue("true")
            @ApiParam(value = "conditional to generate an assignment if one doesn't exist",
                    defaultValue = "true")
            final boolean createAssignment,

            @QueryParam("ignoreSamplingPercent")
            @DefaultValue("false")
            @ApiParam(value = "whether the sampling percent for the experiment should be ignored, " +
                    "forcing the user into the experiment (if eligible)",
                    defaultValue = "false")
            final boolean ignoreSamplingPercent,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg QA, PROD")
            final Context context,

            @ApiParam(value = "Segmentation Profile")
            final SegmentationProfile segmentationProfile,

            @QueryParam("forceProfileCheck")
            @DefaultValue("false")
            @ApiParam(value = "whether to force user profile match",
                    defaultValue = "false")
            final Boolean forceProfileCheck,

            @javax.ws.rs.core.Context final HttpHeaders headers) {
        try {
            LOGGER.debug("postBatchAssignmentForPage applicationName={}, pageName={}, userID={}, context={}, "
                            + "createAssignment={}, ignoreSamplingPercent={}, headers={}, segmentationProfile={}",
                    applicationName, pageName, userID, context, createAssignment, ignoreSamplingPercent, headers,
                    segmentationProfile);

            List<Assignment> assignmentsFromPage =
                    assignments.doPageAssignments(applicationName, pageName, userID, context,
                            createAssignment, ignoreSamplingPercent, headers, segmentationProfile,forceProfileCheck);

            return httpHeader.headers()
                    .entity(ImmutableMap.<String, Object>builder().put("assignments",
                            toBatchAssignmentResponseMap(assignmentsFromPage)).build()).build();
        } catch (Exception exception) {
            LOGGER.error("postBatchAssignmentForPage failed for applicationName={}, pageName={}, userID={}, "
                            + "createAssignment={}, ignoreSamplingPercent={}, context={}, segmentationProfile={},"
                            + " headers={} with error:", applicationName, pageName, userID, createAssignment,
                    ignoreSamplingPercent, context, segmentationProfile, headers, exception);
            throw exception;
        }
    }

    /**
     * Tests if the profile parameters passed would satisfy the rule in the experiment specified.
     *
     * @param applicationName     the application name
     * @param experimentLabel     the experiment label
     * @param context             the context string
     * @param segmentationProfile the {@link com.intuit.wasabi.assignmentobjects.SegmentationProfile} object
     * @param headers             the authorization headers
     * @return Response object contains the rules fit with the requested parameters
     */
    @POST
    @Path("applications/{applicationName}/experiments/{experimentLabel}/ruletest")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Test the segmentation rule of an experiment")
    @Timed
    public Response postAssignmentRuleTest(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("experimentLabel")
            @ApiParam(value = "Experiment Label")
            final Experiment.Label experimentLabel,

            @QueryParam("context")
            @DefaultValue("PROD")
            @ApiParam(value = "context for the experiment, eg QA, PROD")
            final Context context,

            @ApiParam(name = "segmentationProfile", value = "Segmentation Profile")
            final SegmentationProfile segmentationProfile,

            @javax.ws.rs.core.Context
            final HttpHeaders headers) {
        try {
            boolean ruleResult = assignments
                    .doSegmentTest(applicationName, experimentLabel, context, segmentationProfile, headers);

            return httpHeader.headers().entity(ImmutableMap.<String, Object>builder().put("result", ruleResult).build())
                    .build();
        } catch (Exception exception) {
            LOGGER.error("postAssignmentRuleTest failed for applicationName={}, experimentLabel={},"
                            + " context={}, segmentationProfile={}, headers={} with error:",
                    applicationName, experimentLabel, context, segmentationProfile, headers,
                    exception);
            throw exception;
        }
    }

    /**
     * Get the length of the assignments queue
     *
     * @return Response object
     */
    @GET
    @Path("queueLength")
    @Produces(APPLICATION_JSON)
    public Response getAssignmentsQueueLength() {
        try {
            return httpHeader.headers().entity(assignments.queuesLength()).build();
        } catch (Exception exception) {
            LOGGER.error("getAssignmentsQueueLength failed with error:", exception);
            throw exception;
        }
    }

    /**
     * Get the length of the assignments queue
     *
     * @return Response object
     */
    @GET
    @Path("queueDetails")
    @Produces(APPLICATION_JSON)
    public Response getAssignmentsQueueDetails() {
        try {
            return httpHeader.headers().entity(assignments.queuesDetails()).build();
        } catch (Exception exception) {
            LOGGER.error("getAssignmentsQueueDetails failed with error:", exception);
            throw exception;
        }
    }

    /**
     * Flush all active and queued messages from the ingestion queues.
     *
     * @return Response object
     */
    @POST
    @Path("flushMessages")
    @Produces(APPLICATION_JSON)
    public Response flushMessages(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            Username userName = authorization.getUser(authorizationHeader);
            authorization.checkSuperAdmin(userName);
            assignments.flushMessages();
            return httpHeader.headers(HttpStatus.SC_NO_CONTENT).build();
        } catch (Exception exception) {
            LOGGER.error("flushMessages failed with error:", exception);
            throw exception;
        }
    }

    @POST
    @Path("clearMetadataCache")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Clear assignments metadata cache...")
    @Timed
    public Response clearMetadataCache(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader) {
        try {
            UserInfo.Username userName = authorization.getUser(authorizationHeader);
            authorization.checkSuperAdmin(userName);

            boolean result = Boolean.TRUE;
            try {
                assignments.clearMetadataCache();
            } catch (Exception e) {
                LOGGER.error("Exception occurred while clearing assignments metadata cache...", e);
                result = Boolean.FALSE;
            }
            return httpHeader.headers().entity(result).build();
        } catch (Exception exception) {
            LOGGER.error("clearMetadataCache failed with error:", exception);
            throw exception;
        }
    }

    /**
     * Get the details of assignments metadata cache
     *
     * @return Details of assignments metadata cache - cache entities and size of each entity cache
     */
    @GET
    @Path("metadataCacheDetails")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get assignments metadata cache details...")
    @Timed
    public Response getMetadataCacheDetails() {
        try {
            return httpHeader.headers().entity(assignments.metadataCacheDetails()).build();
        } catch (Exception exception) {
            LOGGER.error("getMetadataCacheDetails failed with error:", exception);
            throw exception;
        }
    }

    /**
     * Convert Assignment object collection to the List of response MAP expected by the end user.
     *
     * @param assignments
     * @return
     */
    protected List<Map<String, Object>> toBatchAssignmentResponseMap(Collection<Assignment> assignments) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        assignments.forEach(assignment -> {
            responseList.add(toBatchAssignmentResponseMap(assignment));
        });
        return responseList;
    }

    /**
     * Convert Assignment object of batch-assignment to the response MAP expected by the end user.
     * Batch-assignment response map can contain:
     * - experimentLabel
     * - assignment
     * - payload
     * - status
     *
     * @param assignment
     * @return response map
     */
    protected Map<String, Object> toBatchAssignmentResponseMap(final Assignment assignment) {
        Map<String, Object> response = newHashMap();

        //Add experimentLabel for batch-assignment flow only
        if (nonNull(assignment.getExperimentLabel())) {
            response.put("experimentLabel", assignment.getExperimentLabel());
        }

        // Only include `assignment` property if there is a definitive assignment, either to a bucket or not
        if (assignment.getStatus() != EXPERIMENT_EXPIRED) {
            response.put("assignment",
                    nonNull(assignment.getBucketLabel()) ? assignment.getBucketLabel().toString() : null);

            if (nonNull(assignment.getBucketLabel())) {
                response.put("payload", assignment.getPayload());
            }
        }

        response.put("status", assignment.getStatus());

        return response;
    }

    /**
     * Convert Assignment object of single-assignment to the response MAP expected by the end user.
     * <p>
     * Single-assignment response map can contain:
     * - assignment
     * - payload
     * - status
     * - cache
     * - context
     *
     * @param assignment
     * @return response map
     */
    protected Map<String, Object> toSingleAssignmentResponseMap(final Assignment assignment) {
        Map<String, Object> response = newHashMap();

        // Only include `assignment` property if there is a definitive assignment, either to a bucket or not
        if (assignment.getStatus().isDefinitiveAssignment()) {
            response.put("assignment",
                    nonNull(assignment.getBucketLabel()) ? assignment.getBucketLabel().toString() : null);

            if (nonNull(assignment.getBucketLabel())) {
                response.put("payload", assignment.getPayload());
            }
        }

        response.put("status", assignment.getStatus());

        response.put("cache", assignment.getStatus().isDefinitiveAssignment());
        if (assignment.getContext() != null) {
            response.put("context", assignment.getContext().toString());
        }

        return response;
    }

}
