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

import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.Permission;
import com.intuit.wasabi.authorizationobjects.Role;
import com.intuit.wasabi.authorizationobjects.UserPermissions;
import com.intuit.wasabi.authorizationobjects.UserPermissionsList;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.events.EventsExport;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experimentobjects.Application;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static java.nio.charset.Charset.forName;
import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationResourceTest {

    private static final String USERPASS = new String(encodeBase64("admin@example.com:admin01".getBytes(forName("UTF-8"))), forName("UTF-8"));
    private static final String AUTHHEADER = "Basic: " + USERPASS;
    private static final UserInfo.Username USER = UserInfo.Username.valueOf("admin@example.com");
    private static final UserInfo.Username TESTUSER = UserInfo.Username.valueOf("test_user");
    private static final Application.Name TESTAPP = Application.Name.valueOf("test_app");
    private static final Application.Name TESTAPP2 = Application.Name.valueOf("test_app2");
    @Mock
    private UriInfo uriInfo;
    @Mock
    private Experiments experiments;
    @Mock
    private EventsExport exportEvents;
    @Mock
    private Assignments assignments;
    @Mock
    private Authorization authorization;

    @Test
    public void getRolePermissions() throws Exception {

        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        HashMap<String, Object> result = new HashMap<>();

        when(authorization.getPermissionsFromRole(Role.READONLY)).thenReturn(Role.READONLY.getRolePermissions());
        result.put("permissions", Role.READONLY.getRolePermissions());
        Response response = authorizationResource.getRolePermissions(Role.READONLY.toString());
        assertThat(response.getEntity(), CoreMatchers.<Object>equalTo(result));
    }

    @Test
    public void getUserPermissions() throws Exception {

        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        UserPermissions userPermissions = UserPermissions.newInstance(TESTAPP,
                Role.SUPERADMIN.getRolePermissions()).build();
        UserPermissionsList userPermissionsList = new UserPermissionsList();
        userPermissionsList.addPermissions(userPermissions);

        when(authorization.getUserPermissionsList(USER)).thenReturn(userPermissionsList);
        Response response = authorizationResource.getUserPermissions(USER, AUTHHEADER);
        assertThat(response.getEntity(), CoreMatchers.<Object>equalTo(userPermissionsList));

        UserPermissions userPermissions1 = UserPermissions.newInstance(TESTAPP2,
                Role.READWRITE.getRolePermissions()).build();
        userPermissionsList.addPermissions(userPermissions1);
        when(authorization.getUserPermissionsList(TESTUSER)).thenReturn(userPermissionsList);
        response = authorizationResource.getUserPermissions(TESTUSER, AUTHHEADER);
        UserPermissionsList x = (UserPermissionsList) response.getEntity();
        assertThat(x.getPermissionsList(), equalTo(userPermissionsList.getPermissionsList()));
    }

    @Test
    public void getUserAppPermissions() throws Exception {

        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        UserPermissions userPermissions = UserPermissions.newInstance(TESTAPP,
                Role.SUPERADMIN.getRolePermissions()).build();
        when(authorization.getUserPermissions(USER, TESTAPP)).thenReturn(userPermissions);
        UserPermissions userPermissions1 = UserPermissions.newInstance(TESTAPP,
                Role.READONLY.getRolePermissions()).build();
        when(authorization.getUserPermissions(TESTUSER, TESTAPP2)).thenReturn(userPermissions1);

        Response response = authorizationResource.getUserAppPermissions(USER, TESTAPP, AUTHHEADER);
        assertThat(response.getEntity(), CoreMatchers.<Object>equalTo(userPermissions));

        response = authorizationResource.getUserAppPermissions(TESTUSER, TESTAPP2, AUTHHEADER);
        assertThat(response.getEntity(), CoreMatchers.<Object>equalTo(userPermissions1));
    }

    @Test
    public void assignUserRoles() throws Exception {

        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRole userRole1 = UserRole.newInstance(TESTAPP2, Role.READWRITE).withUserID(TESTUSER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);
        userRoleList.addRole(userRole1);

        List<HashMap> statuses = new ArrayList<>();

        HashMap<String, String> status = new HashMap<>();
        status.put("applicationName", userRole.getApplicationName().toString());
        status.put("userID", userRole.getUserID().toString());
        status.put("role", userRole.getRole().toString());
        status.put("roleAssignmentStatus", "SUCCESS");
        statuses.add(status);

        HashMap<String, String> status1 = new HashMap<>();
        status1.put("applicationName", userRole1.getApplicationName().toString());
        status1.put("userID", userRole1.getUserID().toString());
        status1.put("role", userRole1.getRole().toString());
        status1.put("roleAssignmentStatus", "FAILED");
        status1.put("reason", "Not Authorized");
        statuses.add(status1);

        HashMap<String, Object> statmap = new HashMap<>();
        statmap.put("assignmentStatuses", statuses);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, TESTAPP2, Permission.ADMIN);
        when(authorization.getUserInfo(USER)).thenReturn(EventLog.SYSTEM_USER);
        when(authorization.setUserRole(userRole, EventLog.SYSTEM_USER)).thenReturn(status);

        Response response = authorizationResource.assignUserRoles(userRoleList, AUTHHEADER);
        assertThat(response.getEntity(), CoreMatchers.<Object>equalTo(statmap));
    }

    @Test
    public void updateUserRoles() throws Exception {

        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRole userRole1 = UserRole.newInstance(TESTAPP2, Role.READWRITE).withUserID(TESTUSER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);
        userRoleList.addRole(userRole1);

        List<HashMap> statuses = new ArrayList<>();

        HashMap<String, String> status = new HashMap<>();
        status.put("applicationName", userRole.getApplicationName().toString());
        status.put("userID", userRole.getUserID().toString());
        status.put("role", userRole.getRole().toString());
        status.put("roleAssignmentStatus", "SUCCESS");
        statuses.add(status);

        HashMap<String, String> status1 = new HashMap<>();
        status1.put("applicationName", userRole1.getApplicationName().toString());
        status1.put("userID", userRole1.getUserID().toString());
        status1.put("role", userRole1.getRole().toString());
        status1.put("roleAssignmentStatus", "FAILED");
        status1.put("reason", "Not Authorized");
        statuses.add(status1);

        HashMap<String, Object> statmap = new HashMap<>();
        statmap.put("assignmentStatuses", statuses);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, TESTAPP2, Permission.ADMIN);
        when(authorization.getUserInfo(USER)).thenReturn(EventLog.SYSTEM_USER);
        when(authorization.setUserRole(userRole, EventLog.SYSTEM_USER)).thenReturn(status);

        Response response = authorizationResource.updateUserRoles(userRoleList, AUTHHEADER);
        assertThat(response.getEntity(), CoreMatchers.<Object>equalTo(statmap));
    }

    @Test
    public void getUserRole() throws Exception {

        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRole userRole1 = UserRole.newInstance(TESTAPP2, Role.READWRITE).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);
        userRoleList.addRole(userRole1);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserRoleList(USER)).thenReturn(userRoleList);
        Response response = authorizationResource.getUserRole(USER, AUTHHEADER);
        assert (userRoleList.equals(response.getEntity()));

        userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(TESTUSER).build();
        userRole1 = UserRole.newInstance(TESTAPP2, Role.READWRITE).withUserID(TESTUSER).build();
        userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);
        userRoleList.addRole(userRole1);

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, TESTAPP2, Permission.ADMIN);
        when(authorization.getUserRoleList(TESTUSER)).thenReturn(userRoleList);
        response = authorizationResource.getUserRole(TESTUSER, AUTHHEADER);
        UserRoleList userRoleList1 = new UserRoleList();
        userRoleList1.addRole(userRoleList.getRoleList().get(0));
        UserRoleList x = (UserRoleList) response.getEntity();
        assert (x.equals(userRoleList1));
    }

    @Test
    public void deleteUserRoles() throws Exception {

        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);

        Response response = authorizationResource.deleteUserRoles(TESTAPP, TESTUSER, AUTHHEADER);
        assertThat(response.getStatus(), CoreMatchers.<Object>equalTo(204));
    }

    @Test
    public void addUserToSuperAdminRoleSuccess() throws Exception {

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        UserInfo assigningUserInfo = UserInfo.from(TESTUSER).build();
        UserInfo candidateUserInfo = UserInfo.from(USER).build();

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);

        when(authorization.getUserInfo(USER)).thenReturn(candidateUserInfo);

        when(authorization.getUserInfo(TESTUSER)).thenReturn(assigningUserInfo);

        when(authorization.getUserRoleList(TESTUSER)).thenReturn(userRoleList);

        Response response = authorizationResource.assignUserToSuperAdmin(TESTUSER, AUTHHEADER);

        Mockito.verify(authorization, times(1)).checkSuperAdmin(USER);
        Mockito.verify(authorization, times(1))
                .assignUserToSuperAdminRole(assigningUserInfo, candidateUserInfo);
        assertThat(response.getStatus(), CoreMatchers.<Object>equalTo(204));
    }

    @Test
    public void removeUserFromSuperAdminRoleSuccess() throws Exception {

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        UserInfo assigningUserInfo = UserInfo.from(TESTUSER).build();
        UserInfo candidateUserInfo = UserInfo.from(USER).build();

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(candidateUserInfo);

        when(authorization.getUserInfo(TESTUSER)).thenReturn(assigningUserInfo);

        Response response = authorizationResource.removeUserFromSuperAdmin(TESTUSER, AUTHHEADER);

        Mockito.verify(authorization, times(1)).checkSuperAdmin(USER);
        Mockito.verify(authorization, times(1))
                .removeUserFromSuperAdminRole(assigningUserInfo, candidateUserInfo);

        assertThat(response.getStatus(), CoreMatchers.<Object>equalTo(204));
    }

    @Test
    public void getAllSuperAdminRoleListSuccess() throws Exception {

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.SUPERADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);

        when(authorization.getSuperAdminRoleList()).thenReturn(userRoleList.getRoleList());

        Response response = authorizationResource.getAllSuperAdminRoleList(AUTHHEADER);

        Mockito.verify(authorization, times(1)).checkSuperAdmin(USER);

        assertThat(response.getStatus(), CoreMatchers.<Object>equalTo(200));
        assertEquals(response.getEntity(), userRoleList.getRoleList());
    }

    @Test(expected = AuthenticationException.class)
    public void getAllSuperAdminRoleNotAuthorized() throws Exception {

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.SUPERADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(UserInfo.from(USER).build());
        doThrow(AuthenticationException.class)
                .when(authorization).checkSuperAdmin(USER);

        authorizationResource.getAllSuperAdminRoleList(AUTHHEADER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeUserFromSuperAdminRoleUserNotFound() throws Exception {

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.SUPERADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(UserInfo.from(USER).build());

        when(authorization.getUserInfo(TESTUSER)).thenReturn(null);

        Response response = authorizationResource.removeUserFromSuperAdmin(TESTUSER, AUTHHEADER);

        assertThat(response.getStatus(), CoreMatchers.<Object>equalTo(204));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUserToSuperAdminRoleUserNotFound() throws Exception {

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(UserInfo.from(USER).build());

        when(authorization.getUserInfo(TESTUSER)).thenReturn(null);

        authorizationResource.assignUserToSuperAdmin(TESTUSER, AUTHHEADER);

    }

    @Test(expected = IllegalArgumentException.class)
    public void addUserToSuperAdminRoleUsernameNull() throws Exception {

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(UserInfo.from(USER).build());

        UserInfo.Username nullUsername = UserInfo.Username.valueOf("test");
        nullUsername.setUsername(null);
        when(authorization.getUserInfo(TESTUSER)).thenReturn(UserInfo.from(nullUsername).build());

        authorizationResource.assignUserToSuperAdmin(TESTUSER, AUTHHEADER);

    }

    @Test(expected = IllegalArgumentException.class)
    public void addUserToSuperAdminRoleUsernameEmpty() throws Exception {

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(UserInfo.from(USER).build());

        UserInfo.Username emptyUsername = UserInfo.Username.valueOf("");
        when(authorization.getUserInfo(TESTUSER)).thenReturn(UserInfo.from(emptyUsername).build());

        authorizationResource.assignUserToSuperAdmin(TESTUSER, AUTHHEADER);

    }

    @Test(expected = AuthenticationException.class)
    public void addUserToSuperAdminRoleNotAuthorized() throws Exception {

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(UserInfo.from(USER).build());
        doThrow(AuthenticationException.class)
                .when(authorization).checkSuperAdmin(USER);

        authorizationResource.assignUserToSuperAdmin(TESTUSER, AUTHHEADER);
    }

    @Test(expected = AuthenticationException.class)
    public void removeUserFromSuperAdminRoleNotAuthorized() throws Exception {

        AuthorizationResource authorizationResource =
                new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(UserInfo.from(USER).build());
        doThrow(AuthenticationException.class)
                .when(authorization).checkSuperAdmin(USER);

        authorizationResource.removeUserFromSuperAdmin(TESTUSER, AUTHHEADER);
    }

    @Test
    public void getApplicationUsersByRole() throws Exception {

        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRole userRole1 = UserRole.newInstance(TESTAPP2, Role.READWRITE).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);
        userRoleList.addRole(userRole1);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getApplicationUsers(TESTAPP)).thenReturn(userRoleList);

        Response response = authorizationResource.getApplicationUsersByRole(TESTAPP, AUTHHEADER);
        assertThat(response.getEntity(), CoreMatchers.<Object>equalTo(userRoleList));
    }

    @Test
    public void testGetUserList() throws Exception {
        AuthorizationResource authorizationResource = new AuthorizationResource(authorization, new HttpHeader("jaba-???", "600"));

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);

        UserPermissionsList permissionsList = new UserPermissionsList();
        permissionsList.addPermissions(UserPermissions.newInstance(TESTAPP, Collections.singletonList(Permission.ADMIN)).build());

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getApplicationUsers(TESTAPP)).thenReturn(userRoleList);
        when(authorization.getUserPermissionsList(USER)).thenReturn(permissionsList);

        Response response = authorizationResource.getUserList(AUTHHEADER);
        assertThat(response.getEntity(), CoreMatchers.<Object>equalTo(Collections.singletonList(userRoleList)));
    }
}
