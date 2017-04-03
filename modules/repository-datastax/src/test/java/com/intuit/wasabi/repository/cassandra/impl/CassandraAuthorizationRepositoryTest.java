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
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.authorizationobjects.Role;
import com.intuit.wasabi.authorizationobjects.UserPermissions;
import com.intuit.wasabi.authorizationobjects.UserPermissionsList;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.repository.cassandra.accessor.AppRoleAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.UserInfoAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.UserRoleAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.AppRole;
import com.intuit.wasabi.repository.cassandra.pojo.ApplicationList;
import com.intuit.wasabi.repository.cassandra.pojo.UserInfo;
import com.intuit.wasabi.repository.cassandra.pojo.UserRole;
import com.intuit.wasabi.userdirectory.UserDirectory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CassandraAuthorizationRepositoryTest {
    private final Logger logger = LoggerFactory.getLogger(CassandraAuthorizationRepositoryTest.class);
    @Mock
    ApplicationListAccessor applicationListAccessor;
    @Mock
    AppRoleAccessor appRoleAccessor;
    @Mock
    UserInfoAccessor userInfoAccessor;
    @Mock
    UserRoleAccessor userRoleAccessor;

    @Mock
    Result<ApplicationList> applicationListResult;
    @Mock
    Result<AppRole> appRoleResult;
    @Mock
    Result<UserInfo> userInfoResult;
    @Mock
    Result<UserInfo> userInfoResult2;
    @Mock
    Result<UserRole> userRoleResult;

    @Mock
    UserDirectory userDirectory;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MappingManager mappingMapager;

    private CassandraAuthorizationRepository repository;
    private CassandraAuthorizationRepository spyRepository;


    @Before
    public void setUp() throws Exception {
        repository = new CassandraAuthorizationRepository(applicationListAccessor,
                appRoleAccessor,
                userRoleAccessor,
                userInfoAccessor,
                userDirectory,
                mappingMapager);
        spyRepository = spy(repository);
    }


    @Test
    public void userLookup() {
        com.intuit.wasabi.authenticationobjects.UserInfo userInfo =
                com.intuit.wasabi.authenticationobjects.UserInfo.newInstance(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"))
                        .withEmail("test")
                        .withFirstName("test")
                        .withLastName("test")
                        .build();
        com.intuit.wasabi.authenticationobjects.UserInfo.Username userName =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test");
        when(userDirectory.lookupUser(eq(userName)))
                .thenReturn(userInfo);
        assertThat(userInfo, is(repository.lookupUser(userName)));
    }

    @Test
    public void userLookupException() {
        com.intuit.wasabi.authenticationobjects.UserInfo.Username userName =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test");
        com.intuit.wasabi.authenticationobjects.UserInfo expected =
                com.intuit.wasabi.authenticationobjects.UserInfo.newInstance(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"))
                        .withEmail("")
                        .withFirstName("")
                        .withLastName("")
                        .build();
        doThrow(new AuthenticationException("authentication failed")).when(userDirectory).lookupUser(eq(userName));
        assertThat(expected, is(repository.lookupUser(userName)));
    }

    @Test
    public void setUserInfoTest() {
        com.intuit.wasabi.authenticationobjects.UserInfo expected =
                com.intuit.wasabi.authenticationobjects.UserInfo.newInstance(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"))
                        .withEmail("test")
                        .withFirstName("test")
                        .withLastName("test")
                        .build();
        repository.setUserInfo(expected);
        verify(userInfoAccessor, times(1)).insertUserInfoBy(eq("test"), eq("test"), eq("test"), eq("test"));
    }

    @Test
    public void getUserRolesWithWildcardAppNameTest() {
        List<UserRole> userRoleList = new ArrayList<>();
        userRoleList.add(
                UserRole.builder().appName("test").role("test").userId("test").build()
        );
        when(userRoleAccessor.getUserRolesByUserIdWithWildcardAppName(eq("test"))).thenReturn(userRoleResult);
        when(userRoleResult.iterator()).thenReturn(userRoleList.iterator());

        List<UserRole> result = repository.getUserRolesWithWildcardAppName(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                Application.Name.valueOf("test"));

        logger.info(result.toString());
        assertThat(result.size(), is(1));
    }

    @Test
    public void assignUserToSuperadminTest() {
        com.intuit.wasabi.authenticationobjects.UserInfo uInfo =
                com.intuit.wasabi.authenticationobjects.UserInfo.newInstance(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.
                                valueOf("test1")).build();
        repository.assignUserToSuperAdminRole(uInfo);
        verify(appRoleAccessor).insertAppRoleBy(
                CassandraAuthorizationRepository.ALL_APPLICATIONS, "test1",
                CassandraAuthorizationRepository.SUPERADMIN);
        verify(userRoleAccessor).insertUserRoleBy("test1",
                CassandraAuthorizationRepository.ALL_APPLICATIONS, CassandraAuthorizationRepository.SUPERADMIN);
    }

    @Test
    public void removeUserFromSuperadminTest() {
        com.intuit.wasabi.authenticationobjects.UserInfo uInfo =
                com.intuit.wasabi.authenticationobjects.UserInfo.newInstance(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.
                                valueOf("test1")).build();
        repository.removeUserFromSuperAdminRole(uInfo);
        verify(appRoleAccessor).deleteAppRoleBy(
                CassandraAuthorizationRepository.ALL_APPLICATIONS, "test1");
        verify(userRoleAccessor).deleteUserRoleBy("test1",
                CassandraAuthorizationRepository.ALL_APPLICATIONS);
    }

    @Test
    public void getAllUserRolesOneSuperadminTest() {
        List<UserRole> userRoleList = new ArrayList<>();
        userRoleList.add(
                UserRole.builder().appName("*").role("superadmin").userId("test").build()
        );
        List<UserInfo> userInfo = new ArrayList<>();
        userInfo.add(UserInfo.builder()
                .firstName("test1")
                .lastName("test1")
                .userEmail("test1@test.com")
                .userId("test")
                .build());

        when(userInfoAccessor.getUserInfoBy(eq("test"))).thenReturn(userInfoResult);
        when(userInfoResult.iterator()).thenReturn(userInfo.iterator());

        when(userRoleAccessor.getAllUserRoles()).thenReturn(userRoleResult);
        when(userRoleResult.all()).thenReturn(userRoleList);

        List<com.intuit.wasabi.authorizationobjects.UserRole>
                result = repository.getSuperAdminRoleList();

        logger.info(result.toString());
        assertThat(result.size(), is(1));
    }

    @Test
    public void getAllUserRolesNoSuperuserTest() {
        List<UserRole> userRoleList = new ArrayList<>();
        when(userRoleAccessor.getAllUserRoles()).thenReturn(userRoleResult);
        when(userRoleResult.all()).thenReturn(userRoleList);

        List<com.intuit.wasabi.authorizationobjects.UserRole>
                result = repository.getSuperAdminRoleList();

        logger.info(result.toString());
        assertThat(result.size(), is(0));
    }


    @Test
    public void checkSuperAdminPermissionsTest() {
        List<UserRole> spiedUserRole = new ArrayList<>();
        spiedUserRole.add(
                UserRole.builder()
                        .appName("test")
                        .role(CassandraAuthorizationRepository.SUPERADMIN)
                        .userId("test")
                        .build()
        );
        doReturn(spiedUserRole).when(spyRepository).getUserRolesWithWildcardAppName(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                Application.Name.valueOf("test")
        );

        UserPermissions result = spyRepository.checkSuperAdminPermissions(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                Application.Name.valueOf("test")
        );

        assertThat(result.getApplicationName().toString(), is("test"));
        assertThat(result.getPermissions(), is(Role.SUPERADMIN.getRolePermissions()));
    }

    @Test
    public void checkSuperAdminPermissionsNullTest() {
        doReturn(Collections.EMPTY_LIST).when(spyRepository).getUserRolesWithWildcardAppName(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                Application.Name.valueOf("test")
        );
        UserPermissions result = spyRepository.checkSuperAdminPermissions(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                Application.Name.valueOf("test")
        );
        assertThat(result, is(nullValue()));
    }

    @Test
    public void getUserInfoListTest() {
        List<UserInfo> userInfo = new ArrayList<>();
        userInfo.add(UserInfo.builder()
                .firstName("test1")
                .lastName("test1")
                .userEmail("test1@test.com")
                .userId("test")
                .build());
        userInfo.add(UserInfo.builder()
                .firstName("test2")
                .lastName("test2")
                .userEmail("test2@test.com")
                .userId("test")
                .build());
        userInfo.add(UserInfo.builder()
                .firstName("test3")
                .lastName("test3")
                .userEmail("test3@test.com")
                .userId("test")
                .build());

        when(userInfoAccessor.getUserInfoBy(eq("test"))).thenReturn(userInfoResult);
        when(userInfoResult.iterator()).thenReturn(userInfo.iterator());

        List<UserInfo> result = repository.getUserInfoList(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test")
        );

        logger.info(result.toString());
        assertThat(result.size(), is(3));
    }

    @Test
    public void getUserInfoEmptyResult() {
        List<UserInfo> userInfo = Collections.EMPTY_LIST;
        doReturn(userInfo).when(spyRepository).getUserInfoList(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test")
        );
        com.intuit.wasabi.authenticationobjects.UserInfo expected =
                spyRepository.getUserInfo(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"));
        assertThat(expected, is(nullValue()));
    }

    @Test
    public void getUserInfoSingleResult() {
        //TODO: once we fixed userid is uuid and username is string problem we can use the proper setter/getter here
        List<UserInfo> userInfo = new ArrayList<>();
        userInfo.add(UserInfo.builder()
                .firstName("test")
                .lastName("test")
                .userEmail("test@test.com")
                .userId("test")
                .build()
        );
        doReturn(userInfo).when(spyRepository).getUserInfoList(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test")
        );
        com.intuit.wasabi.authenticationobjects.UserInfo expected =
                spyRepository.getUserInfo(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"));
        assertThat(expected.getEmail(), is("test@test.com"));
        assertThat(expected.getFirstName(), is("test"));
        assertThat(expected.getLastName(), is("test"));
        assertThat(expected.getUsername().getUsername(), is("test"));
    }

    @Test(expected = AuthenticationException.class)
    public void getUserInfoMultipleResults() {
        List<UserInfo> mocked = mock(List.class);
        when(mocked.size()).thenReturn(2);
        doReturn(mocked).when(spyRepository).getUserInfoList(
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test")
        );
        com.intuit.wasabi.authenticationobjects.UserInfo expected =
                spyRepository.getUserInfo(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"));
    }

    @Test
    public void getUserRolesListTest() {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> userRoleList = new ArrayList<>();
        userRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                .appName("test1")
                .role("role1")
                .userId("user1")
                .build()
        );
        userRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                .appName("test2")
                .role("role2")
                .userId("user2")
                .build()
        );
        userRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                .appName("test3")
                .role("role3")
                .userId("user3")
                .build()
        );
        when(userRoleAccessor.getUserRolesByUserId(eq("test"))).thenReturn(userRoleResult);
        when(userRoleResult.iterator()).thenReturn(userRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result =
                repository.getUserRoleList(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                        Optional.empty());
        assertThat(result.size(), is(3));
    }

    @Test
    public void getUserRolesListSingleTest() {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> userRoleList = new ArrayList<>();
        userRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                .appName("test1")
                .role("role1")
                .userId("user1")
                .build()
        );
        when(userRoleAccessor.getUserRolesByUserId(eq("test"))).thenReturn(userRoleResult);
        when(userRoleResult.iterator()).thenReturn(userRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result =
                repository.getUserRoleList(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                        Optional.empty());
        assertThat(result.size(), is(1));
    }

    @Test
    public void getUserRoleEmptyTest() {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> userRoleList = new ArrayList<>();
        when(userRoleAccessor.getUserRolesByUserId(eq("test"))).thenReturn(userRoleResult);
        when(userRoleResult.iterator()).thenReturn(userRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result =
                repository.getUserRoleList(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                        Optional.empty());
        assertThat(result.size(), is(0));
    }

    @Test
    public void getUserRoleEmptyWithAppNameTest() {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> userRoleList = new ArrayList<>();
        when(userRoleAccessor.getUserRolesBy(eq("test"), eq("testApp"))).thenReturn(userRoleResult);
        when(userRoleResult.iterator()).thenReturn(userRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result =
                repository.getUserRoleList(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                        Optional.ofNullable(Application.Name.valueOf("testApp")));
        assertThat(result.size(), is(0));
    }

    @Test
    public void getLimitedUserRolesTest() {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> userRoleList = new ArrayList<>();
        userRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                .appName("test1")
                .role("role1")
                .userId("user1")
                .build()
        );
        userRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                .appName("test2")
                .role("role2")
                .userId("user2")
                .build()
        );
        userRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                .appName("test3")
                .role("role3")
                .userId("user3")
                .build()
        );
        when(userRoleAccessor.getUserRolesBy(
                eq("test"),
                eq("testApp")
                )
        ).thenReturn(userRoleResult);
        when(userRoleResult.iterator()).thenReturn(userRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result =
                repository.getUserRoleList(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                        Optional.ofNullable(Application.Name.valueOf("testApp")));
        assertThat(result.size(), is(3));
    }

    @Test
    public void getLimitedUserRolesZeroTest() {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> userRoleList = new ArrayList<>();
        when(userRoleAccessor.getUserRolesBy(
                eq("test"),
                eq("testApp")
                )
        ).thenReturn(userRoleResult);
        when(userRoleResult.iterator()).thenReturn(userRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> result =
                repository.getUserRoleList(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test"),
                        Optional.ofNullable(Application.Name.valueOf("testApp")));
        assertThat(result.size(), is(0));
    }

    @Test
    public void retrieveOrDefaultUserTest() {
        com.intuit.wasabi.authenticationobjects.UserInfo mocked =
                mock(com.intuit.wasabi.authenticationobjects.UserInfo.class);
        com.intuit.wasabi.authenticationobjects.UserInfo.Username username =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test");
        doReturn(mocked).when(spyRepository).lookupUser(eq(username));
        doNothing().when(spyRepository).setUserInfo(anyObject());
        spyRepository.retrieveOrDefaultUser(username);
        verify(spyRepository, times(1)).lookupUser(username);
        verify(spyRepository, times(1)).setUserInfo(anyObject());
    }

    @Test
    public void getUserRoleListContainsSuperAdminTest() {
        com.intuit.wasabi.authenticationobjects.UserInfo.Username username =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test_user");
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> userRoleList = new ArrayList<>();
        userRoleList.add(
                UserRole.builder()
                        .appName("test")
                        .role(CassandraAuthorizationRepository.SUPERADMIN)
                        .userId("test")
                        .build()
        );

        com.intuit.wasabi.authenticationobjects.UserInfo userInfo =
                new com.intuit.wasabi.authenticationobjects.UserInfo.Builder(username)
                        .withEmail("test@test.com")
                        .withUserId("test_id")
                        .withFirstName("test_fn")
                        .withLastName("test_ln")
                        .build();

        List<ApplicationList> allAppsNames = new ArrayList<>();
        allAppsNames.add(ApplicationList.builder().appName("testApp1").build());
        allAppsNames.add(ApplicationList.builder().appName("testApp2").build());

        doReturn(userRoleList).when(spyRepository).getUserRolesWithWildcardAppName(
                eq(username),
                eq(CassandraAuthorizationRepository.WILDCARD)
        );
        doReturn(userInfo).when(spyRepository).retrieveOrDefaultUser(username);
        when(applicationListAccessor.getUniqueAppName()).thenReturn(applicationListResult);
        when(applicationListResult.iterator()).thenReturn(allAppsNames.iterator());
        UserRoleList result = spyRepository.getUserRoleList(username);
        assertThat(result.getRoleList().size(), is(allAppsNames.size()));
        result.getRoleList().forEach(
                v -> assertThat(v.getRole().getRolePermissions(), is(Role.SUPERADMIN.getRolePermissions()))
        );
    }

    @Test
    public void getUserRoleListNormalUserTest() {
        com.intuit.wasabi.authenticationobjects.UserInfo.Username username =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("test_user");
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> userRoleList = new ArrayList<>();
        userRoleList.add(
                UserRole.builder()
                        .appName("test")
                        .role(Role.ADMIN.toString())
                        .userId("test")
                        .build()
        );
        com.intuit.wasabi.authenticationobjects.UserInfo userInfo =
                new com.intuit.wasabi.authenticationobjects.UserInfo.Builder(username)
                        .withEmail("test@test.com")
                        .withUserId("test_id")
                        .withFirstName("test_fn")
                        .withLastName("test_ln")
                        .build();
        doReturn(new ArrayList<com.intuit.wasabi.repository.cassandra.pojo.UserRole>())
                .when(spyRepository).getUserRolesWithWildcardAppName(
                eq(username),
                eq(CassandraAuthorizationRepository.WILDCARD)
        );
        doReturn(userInfo).when(spyRepository).retrieveOrDefaultUser(username);
        doReturn(userRoleList).when(spyRepository).getUserRoleList(username, Optional.empty());
        UserRoleList result = spyRepository.getUserRoleList(username);
        assertThat(result.getRoleList().size(), is(1));
        result.getRoleList().forEach(
                v -> assertThat(v.getRole().getRolePermissions(), is(Role.ADMIN.getRolePermissions()))
        );

    }

    @Test
    public void setUserRoleTest() {
        Session mockSession = mock(Session.class);
        when(mappingMapager.getSession()).thenReturn(mockSession);
        com.intuit.wasabi.authorizationobjects.UserRole userRole = com.intuit.wasabi.authorizationobjects.UserRole
                .newInstance(Application.Name.valueOf("TestApp"), Role.ADMIN)
                .withUserID(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("testuser"))
                .withLastName("lastname")
                .withFirstName("firstname")
                .build();
        repository.setUserRole(userRole);
        verify(userRoleAccessor, times(1)).insertUserRoleStatement(eq("testuser"), eq("TestApp"), eq(Role.ADMIN.name()));
        verify(appRoleAccessor, times(1)).insertAppRoleStatement(eq("TestApp"), eq("testuser"), eq(Role.ADMIN.name()));
        verify(mockSession, times(1)).execute(any(Statement.class));
    }

    @Test
    public void deleteUserRoleTest() {
        Session mockSession = mock(Session.class);
        when(mappingMapager.getSession()).thenReturn(mockSession);
        com.intuit.wasabi.authorizationobjects.UserRole userRole = com.intuit.wasabi.authorizationobjects.UserRole
                .newInstance(Application.Name.valueOf("TestApp"), Role.ADMIN)
                .withUserID(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("testuser"))
                .withLastName("lastname")
                .withFirstName("firstname")
                .build();
        repository.deleteUserRole(userRole.getUserID(), userRole.getApplicationName());
        verify(userRoleAccessor, times(1)).deleteUserRoleStatement(eq("testuser"), eq("TestApp"));
        verify(appRoleAccessor, times(1)).deleteAppRoleStatement(eq("TestApp"), eq("testuser"));
        verify(mockSession, times(1)).execute(any(Statement.class));
    }

    @Test
    public void getAppSpecificPermissionTest() {
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> expected = new ArrayList<>();
        com.intuit.wasabi.authenticationobjects.UserInfo.Username testUser =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("testUser");
        Optional<Application.Name> testApp = Optional.ofNullable(Application.Name.valueOf("TestApp"));
        doReturn(expected).when(spyRepository).getUserRoleList(eq(testUser), eq(testApp));
        UserPermissions result = spyRepository.getAppSpecificPermission(testUser, testApp.get());
        assertThat(result, is(nullValue()));
    }

    @Test
    public void getAppSpecificPermissionSingleElementTest() {
        Optional<Application.Name> testApp = Optional.ofNullable(Application.Name.valueOf("TestApp"));
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> expected = new ArrayList<>();
        expected.add(UserRole.builder()
                .userId("user1")
                .role(Role.READONLY.name())
                .appName(testApp.get().toString())
                .build()
        );
        com.intuit.wasabi.authenticationobjects.UserInfo.Username testUser =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("testUser");
        doReturn(expected).when(spyRepository).getUserRoleList(eq(testUser), eq(testApp));
        UserPermissions result = spyRepository.getAppSpecificPermission(testUser, testApp.get());
        assertThat(result.getApplicationName().toString(), is(testApp.get().toString()));
        assertThat(result.getPermissions(), is(Role.READONLY.getRolePermissions()));
    }

    @Test(expected = AssertionError.class)
    public void getAppSpecificPermissionSingleElementNoRoleTest() {
        Optional<Application.Name> testApp = Optional.ofNullable(Application.Name.valueOf("TestApp"));
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> expected = new ArrayList<>();
        expected.add(UserRole.builder()
                .userId("user1")
                .appName(testApp.get().toString())
                .build()
        );
        com.intuit.wasabi.authenticationobjects.UserInfo.Username testUser =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("testUser");
        doReturn(expected).when(spyRepository).getUserRoleList(eq(testUser), eq(testApp));
        UserPermissions result = spyRepository.getAppSpecificPermission(testUser, testApp.get());
    }

    @Test(expected = AssertionError.class)
    public void getAppSpecificPermissionMultipleElementsTest() {
        Optional<Application.Name> testApp = Optional.ofNullable(Application.Name.valueOf("TestApp"));
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> expected = new ArrayList<>();
        expected.add(UserRole.builder()
                .userId("user1")
                .appName("app1")
                .build()
        );
        expected.add(UserRole.builder()
                .userId("user2")
                .appName("app2")
                .build()
        );
        com.intuit.wasabi.authenticationobjects.UserInfo.Username testUser =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("testUser");
        doReturn(expected).when(spyRepository).getUserRoleList(eq(testUser), eq(testApp));
        UserPermissions result = spyRepository.getAppSpecificPermission(testUser, testApp.get());
    }

    @Test
    public void getUserPermissionsWithSuperAdminTest() {
        Application.Name testApp = Application.Name.valueOf("TestApp");
        com.intuit.wasabi.authenticationobjects.UserInfo.Username testUser =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("testUser");
        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> expected = new ArrayList<>();
        expected.add(UserRole.builder()
                .userId(testUser.getUsername())
                .appName(testApp.toString())
                .role(Role.SUPERADMIN.name())
                .build()
        );
        doReturn(expected).when(spyRepository).getUserRolesWithWildcardAppName(eq(testUser), eq(testApp));
        UserPermissions result = spyRepository.getUserPermissions(testUser, testApp);
        assertThat(result.getApplicationName(), is(testApp));
        assertThat(result.getPermissions(), is(Role.SUPERADMIN.getRolePermissions()));
    }

    @Test
    public void convertAppRoleToUserRoleWithoutLookupTest() {
        Application.Name applicationName = Application.Name.valueOf("TestApp");
        AppRole appRole = AppRole.builder()
                .role(Role.SUPERADMIN.name())
                .appName(applicationName.toString())
                .userId("user1")
                .build();

        com.intuit.wasabi.authenticationobjects.UserInfo userInfo =
                new com.intuit.wasabi.authenticationobjects.UserInfo.Builder(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1")
                )
                        .withFirstName("firstname")
                        .withLastName("lastname")
                        .withUserId("user1")
                        .withEmail("test@test.com")
                        .build();
        doReturn(userInfo).when(spyRepository)
                .getUserInfo(eq(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1")));
        com.intuit.wasabi.authorizationobjects.UserRole result =
                spyRepository.convertAppRoleToUserRole(applicationName, appRole);
        assertThat(result.getRole(), is(Role.SUPERADMIN));
        assertThat(result.getApplicationName(), is(applicationName));
        assertThat(result.getLastName(), is(userInfo.getLastName()));
        assertThat(result.getFirstName(), is(userInfo.getFirstName()));
        assertThat(result.getUserEmail(), is(userInfo.getEmail()));
        assertThat(result.getUserID().getUsername(), is(userInfo.getUserId()));
    }

    @Test
    public void convertAppRoleToUserRoleWithLookupTest() {
        Application.Name applicationName = Application.Name.valueOf("TestApp");
        AppRole appRole = AppRole.builder()
                .role(Role.SUPERADMIN.name())
                .appName(applicationName.toString())
                .userId("user1")
                .build();

        com.intuit.wasabi.authenticationobjects.UserInfo userInfo =
                new com.intuit.wasabi.authenticationobjects.UserInfo.Builder(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1")
                )
                        .withFirstName("firstname")
                        .withLastName("lastname")
                        .withUserId("user1")
                        .withEmail("test@test.com")
                        .build();
        doReturn(null).when(spyRepository)
                .getUserInfo(eq(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1")));
        doReturn(userInfo).when(spyRepository)
                .lookupUser(eq(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1")));
        com.intuit.wasabi.authorizationobjects.UserRole result =
                spyRepository.convertAppRoleToUserRole(applicationName, appRole);
        assertThat(result.getRole(), is(Role.SUPERADMIN));
        assertThat(result.getApplicationName(), is(applicationName));
        assertThat(result.getLastName(), is(userInfo.getLastName()));
        assertThat(result.getFirstName(), is(userInfo.getFirstName()));
        assertThat(result.getUserEmail(), is(userInfo.getEmail()));
        assertThat(result.getUserID().getUsername(), is(userInfo.getUserId()));
    }

    @Test
    public void getAppRoleListSingleElement() {
        List<com.intuit.wasabi.repository.cassandra.pojo.AppRole> appRoleList = new ArrayList<>();
        appRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.AppRole.builder()
                .appName("testApp")
                .userId("role1")
                .userId("user1")
                .build()
        );
        when(appRoleAccessor.getAppRoleByAppName(eq("testApp"))).thenReturn(appRoleResult);
        when(appRoleResult.iterator()).thenReturn(appRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.AppRole> result =
                repository.getAppRoleList(Application.Name.valueOf("testApp"));
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(appRoleList.get(0)));
    }

    @Test
    public void getAppRoleListZeroElement() {
        List<com.intuit.wasabi.repository.cassandra.pojo.AppRole> appRoleList = new ArrayList<>();
        when(appRoleAccessor.getAppRoleByAppName(eq("testApp"))).thenReturn(appRoleResult);
        when(appRoleResult.iterator()).thenReturn(appRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.AppRole> result =
                repository.getAppRoleList(Application.Name.valueOf("testApp"));
        assertThat(result.size(), is(0));
    }

    @Test
    public void getAppRoleListMoreThanTwoElement() {
        List<com.intuit.wasabi.repository.cassandra.pojo.AppRole> appRoleList = new ArrayList<>();
        appRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.AppRole.builder()
                .appName("testApp")
                .userId("role1")
                .userId("user1")
                .build()
        );
        appRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.AppRole.builder()
                .appName("testApp")
                .userId("role2")
                .userId("user2")
                .build()
        );
        appRoleList.add(com.intuit.wasabi.repository.cassandra.pojo.AppRole.builder()
                .appName("testApp")
                .userId("role3")
                .userId("user3")
                .build()
        );
        when(appRoleAccessor.getAppRoleByAppName(eq("testApp"))).thenReturn(appRoleResult);
        when(appRoleResult.iterator()).thenReturn(appRoleList.iterator());
        List<com.intuit.wasabi.repository.cassandra.pojo.AppRole> result =
                repository.getAppRoleList(Application.Name.valueOf("testApp"));
        assertThat(result.size(), is(3));
        assertThat(result.get(0), is(appRoleList.get(0)));
        assertThat(result.get(1), is(appRoleList.get(1)));
        assertThat(result.get(2), is(appRoleList.get(2)));
    }

    @Test
    public void getApplicationUsersTest() {
        Application.Name applicaitonName = Application.Name.valueOf("TestApp");
        List<AppRole> appRoleList = new ArrayList<>();
        appRoleList.add(AppRole.builder()
                .appName(applicaitonName.toString())
                .userId("user1")
                .role(Role.ADMIN.name())
                .build()
        );
        com.intuit.wasabi.authenticationobjects.UserInfo userInfo =
                new com.intuit.wasabi.authenticationobjects.UserInfo.Builder(
                        com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1")
                )
                        .withFirstName("firstname")
                        .withLastName("lastname")
                        .withUserId("user1")
                        .withEmail("test@test.com")
                        .build();
        doReturn(userInfo).when(spyRepository)
                .getUserInfo(eq(com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1")));
        doReturn(appRoleList).when(spyRepository).getAppRoleList(eq(applicaitonName));
        UserRoleList result = spyRepository.getApplicationUsers(applicaitonName);
        assertThat(result.getRoleList().size(), is(1));
        com.intuit.wasabi.authorizationobjects.UserRole userRole = result.getRoleList().get(0);
        assertThat(userRole.getRole(), is(Role.ADMIN));
        assertThat(userRole.getUserID().getUsername(), is(userInfo.getUserId()));
        assertThat(userRole.getApplicationName(), is(applicaitonName));
        assertThat(userRole.getUserEmail(), is(userInfo.getEmail()));
        assertThat(userRole.getFirstName(), is(userInfo.getFirstName()));
        assertThat(userRole.getLastName(), is(userInfo.getLastName()));
    }

    @Test
    public void getUserPermissionsListAsSuperadminTest() {
        List<ApplicationList> allAppsNames = new ArrayList<>();
        allAppsNames.add(ApplicationList.builder().appName("testApp1").build());
        allAppsNames.add(ApplicationList.builder().appName("testApp2").build());
        com.intuit.wasabi.authenticationobjects.UserInfo.Username testUser =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1");
        UserPermissions expected = UserPermissions.newInstance(CassandraAuthorizationRepository.WILDCARD,
                Role.SUPERADMIN.getRolePermissions()).build();
        doReturn(Optional.ofNullable(expected)).when(spyRepository).getSuperAdminUserPermissions(
                eq(testUser),
                eq(CassandraAuthorizationRepository.WILDCARD)
        );
        when(applicationListAccessor.getUniqueAppName()).thenReturn(applicationListResult);
        when(applicationListResult.iterator()).thenReturn(allAppsNames.iterator());
        UserPermissionsList userPermissionsList = spyRepository.getUserPermissionsList(testUser);
        assertThat(userPermissionsList.getPermissionsList().size(), is(2));
        int i = 0;
        for (UserPermissions userPermissions : userPermissionsList.getPermissionsList()) {
            assertThat(userPermissions.getPermissions(), is(Role.SUPERADMIN.getRolePermissions()));
            assertThat(allAppsNames.get(i).getAppName(), is(userPermissions.getApplicationName().toString()));
            i++;
        }
    }

    @Test
    public void getUserPermissionsListAsNormalUserTest() {
        com.intuit.wasabi.authenticationobjects.UserInfo.Username testUser =
                com.intuit.wasabi.authenticationobjects.UserInfo.Username.valueOf("user1");
        doReturn(Optional.empty()).when(spyRepository).getSuperAdminUserPermissions(
                eq(testUser),
                eq(CassandraAuthorizationRepository.WILDCARD)
        );

        List<com.intuit.wasabi.repository.cassandra.pojo.UserRole> expectedUserRoles = new ArrayList<>();
        expectedUserRoles.add(
                com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                        .appName("app1")
                        .userId("user1")
                        .role(Role.READONLY.name())
                        .build()
        );
        expectedUserRoles.add(
                com.intuit.wasabi.repository.cassandra.pojo.UserRole.builder()
                        .appName("app2")
                        .userId("user1")
                        .role(Role.READWRITE.name())
                        .build()
        );
        doReturn(expectedUserRoles).when(spyRepository).getUserRoleList(
                eq(testUser),
                eq(Optional.empty())
        );

        UserPermissionsList userPermissionsList = spyRepository.getUserPermissionsList(testUser);
        assertThat(userPermissionsList.getPermissionsList().size(), is(2));
        assertThat(userPermissionsList.getPermissionsList().get(0).getPermissions(),
                is(Role.READONLY.getRolePermissions()));
        assertThat(userPermissionsList.getPermissionsList().get(0).getApplicationName(),
                is(Application.Name.valueOf("app1")));
        assertThat(userPermissionsList.getPermissionsList().get(1).getPermissions(),
                is(Role.READWRITE.getRolePermissions()));
        assertThat(userPermissionsList.getPermissionsList().get(1).getApplicationName(),
                is(Application.Name.valueOf("app2")));
    }
}