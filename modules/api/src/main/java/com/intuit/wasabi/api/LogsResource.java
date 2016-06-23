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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.api.APISwaggerResource.*;
import static com.intuit.wasabi.authorizationobjects.Permission.ADMIN;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;

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

        return prepareLogListResponse(auditLogs, page, perPage, filter, sort);
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

        return prepareLogListResponse(auditLogEntries, page, perPage, filter, sort);
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

    /**
     * Makes sure that the timezone offset is present in the filter mask.<br />
     * If the timezoneOffset is blank, the initial filter is returned.<br />
     * If the timezoneOffset is already set in the filter mask (no matter which one), the filter mask is
     * returned.<br />
     * If the filter does not contain the "time" property, it is added, unless the cases before are effective.
     *
     * @param filter         the filter to modify
     * @param timezoneOffset the timezoneOffset to be placed in the mask
     * @return prepared date filter
     */
    // FIXME: ?wtf?
    /*test*/ String prepareDateFilter(final String filter, final String timezoneOffset) {
        if (isBlank(timezoneOffset) || isBlank(filter)) {
            return filter;
        }

        if (!filter.contains(",time={")) {
            if (filter.contains(",time=")) {
                int ind = filter.indexOf(",time=");

                return filter.substring(0, ind + 6) + "{" + timezoneOffset + "}" + filter.substring(ind + 6);
            }

            if (filter.contains(",time")) {
                int ind = filter.indexOf(",time");

                return filter.substring(0, ind + 5) + "={" + timezoneOffset + "}" + filter.substring(ind + 5);
            }

            return filter + ",time={" + timezoneOffset + "}";
        }

        return filter;
    }

    /**
     * Calculates a fromIndex for use with {@link List#subList(int, int)} to paginate a list correctly.
     * Returns 0 for all invalid values (alongside with {@link #toIndex(int, int, int)}), so that
     * for invalid values the call to {@link List#subList(int, int)} will result in an empty list.
     * <br />
     * Invalid values are negative values for the parameters (or 0 for page or per page) as well as parameter
     * combinations which define "inaccessible pages". For example if you just have 2 entries and show up to
     * 4 per page, you will not have a second page.
     *
     * @param page         the page, must be positive
     * @param perPage      the number of entries per page, must be positive
     * @param totalEntries the total number of entries available
     * @return an index to be used as a fromIndex for {@link List#subList(int, int)}
     */

    // note: scoped for test access
    /*test*/
    int fromIndex(final int page, final int perPage, final int totalEntries) {
        // invalid parameters
        if (page <= 0 || perPage <= 0 || totalEntries < 0) {
            return 0;
        }

        // impossible pages
        if ((page - 1) * perPage >= totalEntries) {
            return 0;
        }

        // valid and possible pages
        return (page - 1) * perPage;
    }

    /**
     * Calculates a toIndex for use with {@link List#subList(int, int)} to paginate a list correctly.
     * Returns 0 for all invalid values (alongside with {@link #fromIndex(int, int, int)}), so that
     * for invalid values the call to {@link List#subList(int, int)} will result in an empty list.
     * <br />
     * Invalid values are negative values for the parameters (or 0 for page or per page) as well as parameter
     * combinations which define "inaccessible pages". For example if you just have 2 entries and show up to
     * 4 per page, you will not have a second page.
     * <br />
     * If the resulting index is bigger than the number of total entries, it is clamped to that value. This is
     * needed when pages are incomplete. Visiting the example above again but accessing page 1 with 4 entries
     * per page and just 2 entries in total, this method will yield 2 rather than 4.
     *
     * @param page         the page, must be positive
     * @param perPage      the number of entries per page, must be positive
     * @param totalEntries the total number of entries available
     * @return an index to be used as a fromIndex for {@link List#subList(int, int)}
     */
    // FIXME: ?wtf?
    /*test*/ int toIndex(final int page, final int perPage, final int totalEntries) {
        if (page <= 0 || perPage <= 0 || totalEntries < 0) {
            return 0;
        }

        if ((page - 1) * perPage >= totalEntries) {
            return 0;
        }

        return perPage * page < totalEntries ? perPage * page : totalEntries;
    }

    /**
     * Creates navigation links for a {@code Link} headers field.<br />
     * This will contain a comma separated list of entries with (relative) links in {@code < >} and a {@code rel}
     * attribute which describes the link.<br />
     *
     * Examples:
     * <ul>
     * <li>{@code &lt;/api/v1/logs/applications/MyApplication?per_page=20&page=2&gt;; rel="Page 2"}</li>
     * <li>{@code &lt;/api/v1/logs/applications/MyApplication?per_page=20&page=2&gt;; rel="Page 2", &lt;/api/v1/logs/applications/MyApplication?per_page=20&page=1&gt;; rel="Previous"}</li>
     * </ul>
     *
     * The method automatically generates links to the "First" and "Last" pages and, if applicable, for the "Next" and
     * "Previous" pages. Additionally it will generate links from pages {@code page-boundary} to {@code page+boundary},
     * excluding the current page and unreachable pages. Those pages will have their rel-attributes set to "Page X",
     * where X is their page number.
     *
     * @param path       the sub path in this endpoint, for example {@code /applications/MyApplication}
     * @param entries    the total number of entries to calculate the number of pages
     * @param page       the current page
     * @param perPage    the number of entries per page
     * @param boundary   the boundary for the links to be calculated
     * @param parameters a map with other parameters to append to the link, for example {@code &filter=-time} would
     *                   be contained with the key {@code filter} and the value {@code -time}.
     * @return a string suitable for a {@code Link} headers field describing navigational links
     */
    // FIXME: ?wtf?
    /*test*/ String linkHeaderValue(final String path, final int entries, final int page, final int perPage,
                                    final int boundary, final Map<String, String> parameters) {
        String base = "/api/v1/logs" + path + "?per_page=" + perPage;

        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            base += "&" + parameter.getKey() + "=" + parameter.getValue();
        }

        int pages = numberOfPages(entries, perPage);
        List<String> links = new ArrayList<>();

        links.add("<" + base + "&page=1>; rel=\"First\"");
        links.add("<" + base + "&page=" + pages + ">; rel=\"Last\"");

        for (int i = page - boundary; i <= page + boundary; ++i) {
            if (i <= 0 || i == page || i > pages) {
                continue;
            }

            if (i == page - 1 || i == page + 1) {
                links.add("<" + base + "&page=" + i + ">; rel=\"" + (i == page - 1 ? "Previous" : "Next") + "\"");
            }

            links.add("<" + base + "&page=" + i + ">; rel=\"Page " + i + "\"");
        }

        return join(links, ", ");
    }

    /**
     * Calculates the total number of pages needed to present the number of entries.
     * <br />
     * The number of entries must be 0 or positive, the perPage value must be strictly positive.
     * If any of these conditions does not hold, 1 is returned, as you will always have one page - it might then be
     * empty.
     *
     * @param totalEntries the number of entries
     * @param perPage      the number of entries per page
     * @return the number of pages, 1 on invalid values.
     */
    // FIXME: ?wtf?
    /*test*/ int numberOfPages(final int totalEntries, final int perPage) {
        return totalEntries < 0 || perPage <= 0 ? 1 : totalEntries / perPage + (totalEntries % perPage > 0 ? 1 : 0);
    }
}
