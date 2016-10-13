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
package com.intuit.wasabi.authorization.impl;

import com.intuit.wasabi.authentication.Authentication;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorizationobjects.*;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.authenticationobjects.exceptions.AuthenticationException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AuthorizationRepository;
import com.intuit.wasabi.repository.RepositoryException;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class DefaultAuthorizationTest {
    private static final String USERPASS = new String(Base64.encodeBase64("admin:".getBytes(Charset.forName("UTF-8"))));
    private static final String AUTHHEADER = "Basic: " + USERPASS;
    private static final UserInfo.Username USER = UserInfo.Username.valueOf("admin");
    private static final Application.Name TESTAPP = Application.Name.valueOf("test_app");

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private AuthorizationRepository authorizationRepository;
    @Mock
    private Experiments experiments;
    @Mock
    private Authentication authentication;
    @Mock
    private EventLog eventLog;
    @Mock
    private UserPermissionsList userPermissionsList;
    @Mock
    private UserRoleList userRoleList;
    @Mock
    private UserPermissions userPermissions;
    private DefaultAuthorization defaultAuthorization;

    @Before
    public void setUp() throws Exception {
        defaultAuthorization = new DefaultAuthorization(authorizationRepository, experiments, eventLog);
    }

    @Test
    public void testGetPermissionsFromRole() throws Exception {
        List<Role> roleList = new ArrayList<>();
        roleList.add(Role.ADMIN);
        roleList.add(Role.READONLY);
        roleList.add(Role.READWRITE);
        roleList.add(Role.SUPERADMIN);

        for (Role role : roleList) {
            List<Permission> permissionList = defaultAuthorization.getPermissionsFromRole(role);
            assertThat(permissionList, equalTo(role.getRolePermissions()));
        }
    }

    @Test
    public void testGetUserPermissionsList() throws Exception {
        when(authorizationRepository.getUserPermissionsList(USER)).thenReturn(userPermissionsList);
        assertNotNull(defaultAuthorization.getUserPermissionsList(USER));
        assertThat(defaultAuthorization.getUserPermissionsList(USER), is(userPermissionsList));
    }

    @Test
    public void testGetApplicationUsers() throws Exception {
        when(authorizationRepository.getApplicationUsers(TESTAPP)).thenReturn(userRoleList);
        assertNotNull(defaultAuthorization.getApplicationUsers(TESTAPP));
        assertThat(defaultAuthorization.getApplicationUsers(TESTAPP), is(userRoleList));
    }

    @Test
    public void testGetUserPermissions() throws Exception {
        when(authorizationRepository.getUserPermissions(USER, TESTAPP)).thenReturn(userPermissions);
        assertNotNull(defaultAuthorization.getUserPermissions(USER, TESTAPP));
        assertThat(defaultAuthorization.getUserPermissions(USER, TESTAPP), is(userPermissions));
    }

    @Test
    public void testDeleteUserRole() throws Exception {
        when(authorizationRepository.getUserInfo(USER)).thenReturn(EventLog.SYSTEM_USER);
        defaultAuthorization.deleteUserRole(USER, TESTAPP, EventLog.SYSTEM_USER);
        verify(authorizationRepository).deleteUserRole(USER, TESTAPP);
    }

    @Test
    public void testCheckUserPermissions() throws Exception {

        when(authorizationRepository.getUserPermissions(USER, TESTAPP)).thenReturn(UserPermissions.newInstance
                (TESTAPP, Role.ADMIN.getRolePermissions()).build());
        defaultAuthorization.checkUserPermissions(USER, TESTAPP, Permission.ADMIN);

        when(authorizationRepository.getUserPermissions(USER, TESTAPP)).thenReturn(UserPermissions.newInstance
                (TESTAPP, Role.READONLY.getRolePermissions()).build());
        thrown.expect(AuthenticationException.class);
        defaultAuthorization.checkUserPermissions(USER, TESTAPP, Permission.ADMIN);

        when(authorizationRepository.getUserPermissions(USER, TESTAPP)).thenReturn(null);
        thrown.expect(AuthenticationException.class);
        defaultAuthorization.checkUserPermissions(USER, TESTAPP, Permission.ADMIN);
    }

    @Test
    public void testGetUserHeaderNull(){
        thrown.expect(AuthenticationException.class);
        thrown.expectMessage("Null Authentication Header is not supported");
        UserInfo.Username user = defaultAuthorization.getUser(null);
        assertThat(user, is(USER));
    }

    @Test
    public void testGetUserWithoutPassword() throws Exception {
        thrown.expect(AuthenticationException.class);
        thrown.expectMessage("Username or password are empty.");
        UserInfo.Username user = defaultAuthorization.getUser(AUTHHEADER);
        assertThat(user, is(USER));
    }

    @Test
    public void testGetCorrectUser(){
        UserInfo.Username user = defaultAuthorization.getUser("Basic d2FzYWJpX3JlYWRlcjp3YXNhYmkwMQ==");
        assertThat(user.getUsername(), is("wasabi_reader"));
    }

    @Test
    public void testSetUserRole() throws Exception {
        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(TESTAPP).build();
        Experiment experiment1 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(TESTAPP).build();
        List<Experiment> experimentList = new ArrayList<>();

        when(experiments.getExperiments(TESTAPP)).thenReturn(experimentList);

        Map<String, String> status = createTestStatus(userRole);
        Map map = defaultAuthorization.setUserRole(userRole, EventLog.SYSTEM_USER);
        assertEquals(status, map);

        experimentList.add(experiment);
        experimentList.add(experiment1);
        status.remove("reason");
        status.put("roleAssignmentStatus", "SUCCESS");
        when(experiments.getExperiments(TESTAPP)).thenReturn(experimentList);
        UserRoleList userRoleList = Mockito.mock(UserRoleList.class);
        when(authorizationRepository.getUserRoleList(userRole.getUserID())).thenReturn(userRoleList);
        when(authorizationRepository.getUserInfo(userRole.getUserID())).thenReturn(EventLog.SYSTEM_USER);
        when(userRoleList.getRoleList()).thenReturn(Collections.<UserRole>emptyList());
        map = defaultAuthorization.setUserRole(userRole, EventLog.SYSTEM_USER);
        assertEquals(status, map);

        experimentList.add(experiment);
        experimentList.add(experiment1);
        when(experiments.getExperiments(TESTAPP)).thenReturn(experimentList);
        status.put("reason", "RepositoryException");
        status.put("roleAssignmentStatus", "FAILED");
        doThrow(RepositoryException.class).when(authorizationRepository)
                .setUserRole(userRole);
        map = defaultAuthorization.setUserRole(userRole, EventLog.SYSTEM_USER);
        assertEquals(status, map);
    }

    private Map<String, String> createTestStatus(UserRole userRole){
        Map<String, String> status = new HashMap<>();
        status.put("userID", userRole.getUserID().toString());
        status.put("role", userRole.getRole().toString());
        status.put("applicationName", userRole.getApplicationName().toString());
        status.put("roleAssignmentStatus", "FAILED");
        status.put("reason", "No application named " + userRole.getApplicationName());
        return status;
    }

    @Test
    public void testGetUserRoleList() throws Exception {
        when(authorizationRepository.getUserRoleList(USER)).thenReturn(userRoleList);
        assertNotNull(defaultAuthorization.getUserRoleList(USER));
    }

    @Test
    public void testCheckSuperAdminException() throws Exception {
        when(authorizationRepository.checkSuperAdminPermissions(USER, null)).thenReturn(null);
        thrown.expect(AuthenticationException.class);
        defaultAuthorization.checkSuperAdmin(USER);
    }

    @Test
    public void testCheckSuperAdminSuccess() throws Exception {
        when(authorizationRepository.checkSuperAdminPermissions(USER, null)).thenReturn(Mockito.mock(UserPermissions.class));
        try {
            defaultAuthorization.checkSuperAdmin(USER);
        } catch(AuthenticationException e) {
            Assert.fail("Expected successful call to checkSuperAdmin, but got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetUserInfoException() throws Exception {
        when(authorizationRepository.getUserInfo(EventLog.SYSTEM_USER.getUsername())).thenReturn(EventLog.SYSTEM_USER);
        assertEquals(EventLog.SYSTEM_USER, defaultAuthorization.getUserInfo(EventLog.SYSTEM_USER.getUsername()));

        thrown.expect(AuthenticationException.class);
        defaultAuthorization.getUserInfo(null);
    }


}
