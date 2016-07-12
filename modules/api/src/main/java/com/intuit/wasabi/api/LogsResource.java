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
import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.Permission;
import com.intuit.wasabi.experimentobjects.Application;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.api.APISwaggerResource.*;
import static com.intuit.wasabi.authorizationobjects.Permission.ADMIN;
import static com.intuit.wasabi.api.util.PaginationHelper.*;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * The API endpoint /logs provides audit logs for application admins.
 *
 * The logs can be filtered, sorted, paginated. By default the page {@link APISwaggerResource#DEFAULT_PAGE} is returned,
 * containing the last {@link APISwaggerResource#DEFAULT_PER_PAGE} actions. The logs are by default sorted by their
 * occurence date, descending.
 */
@Path("/v1/logs")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Audit Logs (Activity Logs about changes in experiments-buckets)", produces = "application/json")
public class LogsResource {

    private final AuditLog auditLog;
    private final Authorization authorization;
    private final HttpHeader httpHeader;

    private final String responseName = "logEntries";

    /**
     * Instantiates a LogsResource.
     *
     * @param auditLog      the auditlog repository
     * @param authorization the authorization
     */
    @Inject
    LogsResource(final AuditLog auditLog, final Authorization authorization, final HttpHeader httpHeader) {
        this.auditLog = auditLog;
        this.authorization = authorization;
        this.httpHeader = httpHeader;
    }

    /**
     * Returns a list of audit log entries for the specified application if the requesting user has access to it.
     * To have access the user needs {@link Permission#ADMIN} permissions for the application.
     *
     * Before returning the paginated list of log entries it gets
     * <ul>
     * <li>filtered,</li>
     * <li>sorted,</li>
     * <li>counted,</li>
     * <li>paginated.</li>
     * </ul>
     * For details on how the filtering and sorting work, especially for the grammar structures used,
     * see {@link AuditLog#filter(List, String)} and {@link AuditLog#sort(List, String)}.
     *
     * @param authorizationHeader the authentication headers
     * @param applicationName     the name of the application for which the log should be fetched
     * @param page                the page which should be returned, defaults to 1 (latest changes)
     * @param perPage             the number of log entries per page, defaults to 10
     * @param sort                the sorting rules
     * @param filter              the filter rules
     * @param timezoneOffset      the timezone offset from GMT
     * @return a response containing a list with {@code 0 - perPage} experiments, if that many are on the page.
     */
    @GET
    @Path("/applications/{applicationName}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Returns all logs for the specified application if the requesting user has Admin permissions.",
            notes = "Returns all logs for the specified application if the requesting user has Admin permissions. "
                    + "The parameters allow for filtering, sorting, and pagination.",
            response = Response.class,
            httpMethod = "GET",
            produces = "application/json",
            protocols = "https")
    @Timed(name = "getLogs")
    public Response getLogs(@HeaderParam(AUTHORIZATION)
                            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                            final String authorizationHeader,

                            @PathParam("applicationName")
                            @ApiParam(value = "Application Name")
                            final Application.Name applicationName,

                            @QueryParam("page")
                            @DefaultValue(DEFAULT_PAGE)
                            @ApiParam(value = "Defines the page to retrieve", defaultValue = DEFAULT_PAGE)
                            final int page,

                            @QueryParam("per_page")
                            @DefaultValue(DEFAULT_PER_PAGE)
                            @ApiParam(name = "perPage", value = "Defines the entries per page.", defaultValue = DEFAULT_PER_PAGE)
                            final int perPage,

                            @QueryParam("sort")
                            @DefaultValue("-time")
                            @ApiParam(name = "sort", defaultValue = "-time",
                                    value = "Allows to specify the sort order.<br />"
                                            + "<pre>"
                                            + "SortOrder    := Property | PropertyList\n"
                                            + "PropertyList := Property,SortOrder\n"
                                            + "Property     := ALProperty | -ALProperty\n"
                                            + "ALProperty   := firstname, lastname, username, mail, app, experiment, bucket, time, attr, before, after, desc, action"
                                            + "</pre>"
                                            + "The prefix - allows for descending sorting.")
                            final String sort,

                            @QueryParam("filter")
                            @DefaultValue("")
                            @ApiParam(name = "filter",
                                    value = "Allows to specify filter rules.<br />"
                                            + "<pre>"
                                            + "FilterMask   := Value | Value,KeyValueList | KeyValueList"
                                            + "KeyValueList := Property=Value | Property=Value,KeyValueList"
                                            + "Property     := firstname | lastname | username | mail | user | experiment | bucket | app | time | attr | before | after"
                                            + "Value        := any value, may not contain commas (,) followed by a Property. If it starts with an escaped"
                                            + "                dash (\\-), the value is negated, thus shall not match."
                                            + "</pre>")
                            final String filter,

                            @QueryParam("timezone")
                            @DefaultValue("+0000")
                            @ApiParam(name = "timezone", defaultValue = "+0000", value = "Allows to specify the user's timezone offset to UTC. Should be in the format +0000 (or -0000). Default is +0000.")
                            final String timezoneOffset) {
        authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, ADMIN);

        List<AuditLogEntry> auditLogs = auditLog.getAuditLogs(applicationName,
                prepareDateFilter(filter, timezoneOffset), sort);

        String path = this.getClass().getAnnotation(Path.class).value();
        return preparePageFilterResponse(responseName, path, auditLogs, page, perPage, filter, sort);

    }

    /**
     * Returns a list of audit log entries for all applications, if the requesting user has access to it.
     * To have access the user needs {@link Permission#SUPERADMIN} permissions.
     *
     * Before returning the paginated list of log entries it gets
     * <ul>
     * <li>filtered,</li>
     * <li>sorted,</li>
     * <li>counted,</li>
     * <li>paginated.</li>
     * </ul>
     * For details on how the filtering and sorting work, especially for the grammar structures used,
     * see {@link AuditLog#filter(List, String)} and {@link AuditLog#sort(List, String)}.
     *
     * @param authorizationHeader the authentication headers
     * @param page                the page which should be returned, defaults to 1 (latest changes)
     * @param perPage             the number of log entries per page, defaults to 10
     * @param sort                the sorting rules
     * @param filter              the filter rules
     * @param timezoneOffset      the time zone offset from GMT
     * @return a response containing a list with {@code 0 - perPage} experiments, if that many are on the page.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Returns all logs if the requesting user has SuperAdmin permissions.",
            notes = "Returns all logs if the requesting user has SuperAdmin permissions. "
                    + "The parameters allow for filtering, sorting, and pagination.",
            response = Response.class,
            httpMethod = "GET",
            produces = "application/json",
            protocols = "https")
    @Timed(name = "getCompleteLogs")
    public Response getCompleteLogs(@HeaderParam(AUTHORIZATION)
                                    @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                    final String authorizationHeader,

                                    @QueryParam("page")
                                    @DefaultValue(DEFAULT_PAGE)
                                    @ApiParam(defaultValue = DEFAULT_PAGE) final int page,

                                    @QueryParam("per_page")
                                    @DefaultValue(DEFAULT_PER_PAGE)
                                    @ApiParam(defaultValue = DEFAULT_PER_PAGE) final int perPage,

                                    @QueryParam("sort")
                                    @DefaultValue("-time")
                                    @ApiParam(name = "sort", defaultValue = "-time",
                                            value = "Allows to specify the sort order.<br />"
                                                    + "<pre>"
                                                    + "SortOrder    := Property | PropertyList\n"
                                                    + "PropertyList := Property,SortOrder\n"
                                                    + "Property     := ALProperty | -ALProperty\n"
                                                    + "ALProperty   := firstname, lastname, username, mail, app, experiment, bucket, time, attr, before, after, desc, action"
                                                    + "</pre>"
                                                    + "The prefix - allows for descending sorting.")
                                    final String sort,

                                    @QueryParam("filter")
                                    @DefaultValue("")
                                    @ApiParam(name = "filter",
                                            value = "Allows to specify filter rules.<br />"
                                                    + "<pre>"
                                                    + "FilterMask   := Value | Value,KeyValueList | KeyValueList"
                                                    + "KeyValueList := Property=Value | Property=Value,KeyValueList"
                                                    + "Property     := firstname | lastname | username | mail | user | experiment | bucket | app | time | attr | before | after"
                                                    + "Value        := any value, may not contain commas (,) followed by a Property. If it starts with an escaped"
                                                    + "                dash (\\-), the value is negated, thus shall not match."
                                                    + "</pre>")
                                    final String filter,
                                    @QueryParam("timezone") @DefaultValue("+0000") @ApiParam(name = "timezone", value = "Allows to specify the user's timezone offset to UTC. Should be in the format +0000 (or -0000). If not specified, it is handled as +0000.")
                                    final String timezoneOffset) {
        authorization.checkSuperAdmin(authorization.getUser(authorizationHeader));

        List<AuditLogEntry> auditLogEntries = auditLog.getAuditLogs(prepareDateFilter(filter, timezoneOffset), sort);

        String path = this.getClass().getAnnotation(Path.class).value();
        return preparePageFilterResponse(responseName, path, auditLogEntries, page, perPage, filter, sort);
    }

    /**
     * Prepares the response for a list of AuditLogEntries.
     *
     * @param auditLogEntries the list of AuditLogEntries
     * @param page            the current page
     * @param perPage         the number of entries per page
     * @param filter          the applied filter
     * @param sort            the applied sort order
     * @return the response
     */
    // FIXME: ?wtf?
    /*test*/ Response prepareLogListResponse(final List<AuditLogEntry> auditLogEntries, final int page,
                                             final int perPage, final String filter, final String sort) {
        int total = auditLogEntries.size();
        Map<String, Object> auditLogResponse = new HashMap<>();

        auditLogResponse.put("logEntries", auditLogEntries.subList(fromIndex(page, perPage, total), toIndex(page, perPage, total)));
        auditLogResponse.put("totalEntries", total);

        Map<String, String> params = new HashMap<>();

        params.put("filter", filter);
        params.put("sort", sort);

        return httpHeader.headers()
                .header("Link", linkHeaderValue("", total, page, perPage, 3, params))
                .entity(auditLogResponse)
                .build();
    }
}
