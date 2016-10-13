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
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.authenticationobjects.exceptions.AuthenticationException;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.UserPermissions;
import com.intuit.wasabi.authorizationobjects.UserPermissionsList;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.experimentobjects.Application;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.collect.Lists.newArrayList;
import static com.intuit.wasabi.api.APISwaggerResource.DEFAULT_ROLE;
import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_ALL_ROLES;
import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;
import static com.intuit.wasabi.authorizationobjects.Permission.ADMIN;
import static com.intuit.wasabi.authorizationobjects.Role.toRole;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * API endpoint for administering user access roles
 */
@Path("/v1/authorization")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Authorization (Administer User Access Roles)")
public class AuthorizationResource {

    private static final Logger LOGGER = getLogger(AuthorizationResource.class);
    private final Authorization authorization;
    private final HttpHeader httpHeader;

    @Inject
    AuthorizationResource(final Authorization authorization, final HttpHeader httpHeader) {
        this.authorization = authorization;
        this.httpHeader = httpHeader;
    }

    /**
     * Get permissions associated with a specific user role
     *
     * @param role User access role
     * @return Response object
     */
    @GET
    @Path("/roles/{role}/permissions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get permissions associated with a specific user role")
    @Timed
    public Response getRolePermissions(@PathParam("role")
                                       @ApiParam(defaultValue = DEFAULT_ROLE, value = EXAMPLE_ALL_ROLES)
                                       final String role) {
        return httpHeader.headers().entity(ImmutableMap.<String, Object>builder().put("permissions",
                authorization.getPermissionsFromRole(toRole(role))).build()).build();
    }

    /**
     * Get permissions for a user across applications
     *
     * @param userID              User ID
     * @param authorizationHeader
     * @return Response object
     */
    @GET
    @Path("/users/{userID}/permissions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get permissions for a user across applications")
    @Timed
    public Response getUserPermissions(@PathParam("userID")
                                       @ApiParam(value = "User ID")
                                       final Username userID,

                                       @HeaderParam(AUTHORIZATION)
                                       @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                       final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        UserPermissionsList userPermissionsList = authorization.getUserPermissionsList(userID);

        if (userName.equals(userID)) {
            return httpHeader.headers().entity(userPermissionsList).build();
        }

        UserPermissionsList authPermissionsList = new UserPermissionsList();

        for (UserPermissions userPermissions : userPermissionsList.getPermissionsList()) {
            try {
                authorization.checkUserPermissions(userName, userPermissions.getApplicationName(), ADMIN);
                authPermissionsList.addPermissions(userPermissions);
            } catch (AuthenticationException ignored) {
                // FIXME: ?are we right in intentionally swallowing this excpetion?
                LOGGER.trace("AuthenticationException in getUserPermissions", ignored);
            }
        }

        return httpHeader.headers().entity(authPermissionsList).build();
    }

    /**
     * Get permissions of one user within a single application
     *
     * @param userID
     * @param applicationName
     * @param authorizationHeader
     * @return Response object
     */
    @GET
    @Path("/users/{userID}/applications/{applicationName}/permissions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get permissions of one user within a single application")
    @Timed
    public Response getUserAppPermissions(@PathParam("userID")
                                          @ApiParam(value = "User ID")
                                          final Username userID,

                                          @PathParam("applicationName")
                                          @ApiParam(value = "Application Name")
                                          final Application.Name applicationName,

                                          @HeaderParam(AUTHORIZATION)
                                          @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                          final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);

        if (!userName.equals(userID)) {
            authorization.checkUserPermissions(userName, applicationName, ADMIN);
        }

        UserPermissions userPermissions = authorization.getUserPermissions(userID, applicationName);

        return httpHeader.headers().entity(userPermissions).build();
    }

    /**
     * Assign roles for a list of users and applications
     *
     * @param userRoleList
     * @param authorizationHeader
     * @return Response object
     */
    @POST
    @Path("/roles")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Assign roles for a list of users and applications")
    @Timed
    public Response assignUserRoles(@ApiParam(name = "userRoleList", value = "Please see model example", required = true)
                                    final UserRoleList userRoleList,

                                    @HeaderParam(AUTHORIZATION)
                                    @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                    final String authorizationHeader) {
        List<Map> status = updateUserRole(userRoleList, authorizationHeader);

        return httpHeader.headers()
                .entity(ImmutableMap.<String, Object>builder().put("assignmentStatuses", status).build()).build();
    }

    /**
     * Get user role
     *
     * @param userID
     * @param authorizationHeader
     * @return Response object
     */
    @GET
    @Path("/users/{userID}/roles")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get access roles for a user across applications")
    @Timed
    public Response getUserRole(@PathParam("userID")
                                @ApiParam(value = "User ID")
                                final Username userID,

                                @HeaderParam(AUTHORIZATION)
                                @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        UserRoleList userRoles = authorization.getUserRoleList(userID);

        if (userName.equals(userID)) {
            return httpHeader.headers().entity(userRoles).build();
        }

        UserRoleList authRoles = new UserRoleList();

        for (UserRole userRole : userRoles.getRoleList()) {
            try {
                authorization.checkUserPermissions(userName, userRole.getApplicationName(), ADMIN);
                authRoles.addRole(userRole);
            } catch (AuthenticationException ignored) {
                // FIXME: ?are we right in intentionally swallowing this exception?
                LOGGER.trace("AuthenticationException in getUserRole", ignored);
            }
        }

        return httpHeader.headers().entity(authRoles).build();
    }

    /**
     * Update user roles
     *
     * @param userRoleList        list of roles for the user
     * @param authorizationHeader http header
     * @return response object
     */
    @PUT
    @Path("/roles")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Update roles for a list of users and applications")
    @Timed
    public Response updateUserRoles(@ApiParam(name = "userRoleList", value = "Please see model example", required = true)
                                    final UserRoleList userRoleList,

                                    @HeaderParam(AUTHORIZATION)
                                    @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                    final String authorizationHeader) {
        List<Map> statuses = updateUserRole(userRoleList, authorizationHeader);

        return httpHeader.headers()
                .entity(ImmutableMap.<String, Object>builder().put("assignmentStatuses", statuses).build()).build();
    }

    private List<Map> updateUserRole(@ApiParam(required = true)
                                             UserRoleList userRoleList,

                                     @HeaderParam(AUTHORIZATION)
                                     @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                             String authorizationHeader) {
        Username subject = authorization.getUser(authorizationHeader);
        UserInfo admin = authorization.getUserInfo(subject);
        List<Map> status = newArrayList();

        for (UserRole userRole : userRoleList.getRoleList()) {
            try {
                authorization.checkUserPermissions(subject, userRole.getApplicationName(), ADMIN);
                status.add(authorization.setUserRole(userRole, admin));
            } catch (AuthenticationException e) {
                LOGGER.error("Unable to check user permissions", e);

                status.add(ImmutableMap.<String, String>builder()
                        .put("applicationName", userRole.getApplicationName().toString())
                        .put("userID", userRole.getUserID().toString())
                        .put("role", userRole.getRole().toString())
                        .put("roleAssignmentStatus", "FAILED")
                        .put("reason", "Not Authorized").build());
            }
        }

        return status;
    }

    /**
     * Delete a user's role within an application
     *
     * @param applicationName
     * @param userID
     * @param authorizationHeader
     * @return Response object
     */
    @DELETE
    @Path("/applications/{applicationName}/users/{userID}/roles")
    @Produces(APPLICATION_JSON)
//    @RolesAllowed("ADMIN")
    @ApiOperation(value = "Delete a user's role within an application")
    @Timed
    public Response deleteUserRoles(@PathParam("applicationName")
                                    @ApiParam(value = "Application Name")
                                    final Application.Name applicationName,

                                    @PathParam("userID")
                                    @ApiParam(value = "User ID")
                                    final Username userID,

                                    @HeaderParam(AUTHORIZATION)
                                    @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                    final String authorizationHeader) {
        Username userName = authorization.getUser(authorizationHeader);
        UserInfo admin = authorization.getUserInfo(userName);

        authorization.checkUserPermissions(userName, applicationName, ADMIN);
        authorization.deleteUserRole(userID, applicationName, admin);

        return httpHeader.headers(NO_CONTENT).build();
    }

    /**
     * Get roles for users
     *
     * @param applicationName
     * @param authorizationHeader
     * @return Response object
     */
    @GET
    @Path("/applications/{applicationName}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get roles for all users within an application")
    @Timed
    public Response getApplicationUsersByRole(@PathParam("applicationName")
                                              @ApiParam(value = "Application Name")
                                              final Application.Name applicationName,

                                              @HeaderParam(AUTHORIZATION)
                                              @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                              final String authorizationHeader) {
        // As long as you are an authenticated user, anyone should be able to see list of applications and admins
        Username userName = authorization.getUser(authorizationHeader);

        if (Objects.isNull(userName)) {
            throw new AuthenticationException("User is not authenticated");
        }

        return httpHeader.headers().entity(authorization.getApplicationUsers(applicationName)).build();
    }

    /**
     * Returns a list of roles for all users in the applications of the requesting user.
     * This is related to calling /applications/{applicationName} multiple times.
     *
     * @param authHeader the authorization header
     * @return a list of lists of roles
     */
    @GET
    @Path("/applications")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get access roles for all users for all applications that the given user belongs to")
    @Timed
    public Response getUserList(@HeaderParam(AUTHORIZATION)
                                @ApiParam(value = EXAMPLE_AUTHORIZATION_HEADER, required = true)
                                        String authHeader) {

        UserInfo.Username subject = authorization.getUser(authHeader);

        if (Objects.isNull(subject)) {
            throw new AuthenticationException("User is not authenticated");
        }

        UserPermissionsList userPermissionsList = authorization.getUserPermissionsList(subject);
        List<UserRoleList> userRoleListList = new ArrayList<>();
        for (UserPermissions userPermissions : userPermissionsList.getPermissionsList()) {
            UserRoleList list = authorization.getApplicationUsers(userPermissions.getApplicationName());
            if (!list.getRoleList().isEmpty()) {
                userRoleListList.add(list);
            }
        }

        return httpHeader.headers().entity(userRoleListList).build();
    }
}
