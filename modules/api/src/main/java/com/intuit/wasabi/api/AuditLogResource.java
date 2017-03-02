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
import com.intuit.wasabi.api.pagination.PaginationHelper;
import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.Permission;
import com.intuit.wasabi.experimentobjects.Application;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_FILTER;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_PER_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_SORT;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_TIMEZONE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_FILTER;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_PER_PAGE;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_SORT;
import static com.intuit.wasabi.api.APISwaggerResource.DOC_TIMEZONE;
import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static com.intuit.wasabi.authorizationobjects.Permission.ADMIN;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The API endpoint /logs provides audit logs for application admins.
 * <p>
 * The logs can be filtered, sorted, paginated. By default the page {@link APISwaggerResource#DEFAULT_PAGE} is returned,
 * containing the last {@link APISwaggerResource#DEFAULT_PER_PAGE} actions. The logs are by default sorted by their
 * occurence date, descending.
 */
@Path("/v1/logs")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Audit Logs (Activity Logs about changes in experiments-buckets)", produces = "application/json")
public class AuditLogResource {

    private static final Logger LOGGER = getLogger(AuditLogResource.class);

    private final AuditLog auditLog;
    private final Authorization authorization;
    private final HttpHeader httpHeader;
    private final PaginationHelper<AuditLogEntry> paginationHelper;

    /**
     * Instantiates a LogsResource.
     *
     * @param auditLog         the auditlog repository
     * @param authorization    the authorization
     * @param httpHeader       the HTTP header
     * @param paginationHelper the pagination helper
     */
    @Inject
    AuditLogResource(final AuditLog auditLog, final Authorization authorization,
                     final HttpHeader httpHeader, final PaginationHelper<AuditLogEntry> paginationHelper) {
        this.auditLog = auditLog;
        this.authorization = authorization;
        this.httpHeader = httpHeader;
        this.paginationHelper = paginationHelper;
    }

    /**
     * Returns a list of audit log entries for the specified application if the requesting user has access to it.
     * To have access the user needs {@link Permission#ADMIN} permissions for the application.
     * <p>
     * This endpoint is paginated.
     *
     * @param authorizationHeader the authentication headers
     * @param applicationName     the application name. If {@code null}, all logs for the authorized user's applications
     *                            are returned.
     * @param page                the page which should be returned, defaults to 1
     * @param perPage             the number of log entries per page, defaults to 10. -1 to get all values.
     * @param sort                the sorting rules
     * @param filter              the filter rules
     * @param timezoneOffset      the time zone offset from UTC
     * @return a response containing a map with a list with {@code 0} to {@code perPage} log entries,
     * if that many are on the page, and a count of how many log entries match the filter criteria.
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
    @Timed(name = "getLogsForApplication")
    public Response getLogsForApplication(
            @HeaderParam(AUTHORIZATION)
            @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
            final String authorizationHeader,

            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

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
            final String timezoneOffset) {
        try {
            List<AuditLogEntry> auditLogs;

            if (applicationName != null) {
                authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, ADMIN);
                auditLogs = auditLog.getAuditLogs(applicationName);
            } else {
                authorization.checkSuperAdmin(authorization.getUser(authorizationHeader));
                auditLogs = auditLog.getAuditLogs();
            }

            Map<String, Object> response = paginationHelper
                    .paginate("logEntries", auditLogs, filter, timezoneOffset, sort, page, perPage);

            return httpHeader.headers().entity(response).build();
        } catch (Exception exception) {
            LOGGER.error("getLogsForApplication failed for applicationName={}, page={}, perPage={},"
                            + " filter={}, sort={}, timezoneOffset={} with error:",
                    applicationName, page, perPage, filter, sort, timezoneOffset,
                    exception);
            throw exception;
        }
    }

    /**
     * Returns a list of audit log entries for all applications, if the requesting user has access to it.
     * To have access the user needs {@link Permission#SUPERADMIN} permissions.
     * <p>
     * This endpoint is paginated.
     *
     * @param authorizationHeader the authentication headers
     * @param page                the page which should be returned, defaults to 1
     * @param perPage             the number of log entries per page, defaults to 10. -1 to get all values.
     * @param sort                the sorting rules
     * @param filter              the filter rules
     * @param timezoneOffset      the time zone offset from UTC
     * @return a response containing a map with a list with {@code 0} to {@code perPage} log entries,
     * if that many are on the page, and a count of how many log entries match the filter criteria.
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
    @Timed(name = "getLogsForAllApplications")
    public Response getLogsForAllApplications(
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
            final String timezoneOffset) {
        try {
            return getLogsForApplication(authorizationHeader, null, page, perPage,
                    filter, sort, timezoneOffset);
        } catch (Exception exception) {
            LOGGER.error("getLogsForAllApplications failed for page:{}, perPage={}, filter={},"
                            + " sort={}, timezoneOffset={} with error:",
                    page, perPage, filter, sort, timezoneOffset,
                    exception);
            throw exception;
        }
    }

}
