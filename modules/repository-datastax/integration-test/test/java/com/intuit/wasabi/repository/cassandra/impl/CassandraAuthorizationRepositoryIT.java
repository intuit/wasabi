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

import com.google.inject.Inject;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.repository.AuthorizationRepository;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.data.AuthorizationRepositoryDataProvider;
import com.intuit.wasabi.repository.cassandra.impl.authorization.AuthorizationRepositorySetup;
import com.intuit.wasabi.userdirectory.UserDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

@Test
@Guice(modules = CassandraRepositoryModule.class)
public class CassandraAuthorizationRepositoryIT extends AuthorizationRepositorySetup {
    private final Logger logger = LoggerFactory.getLogger(CassandraAuthorizationRepositoryIT.class);
    @Inject
    AuthorizationRepository authorizationRepository;
    @Inject
    UserDirectory userDirectory;
    protected final Map<String, Set<String>> userApplicationMap = new HashMap<>();

    public CassandraAuthorizationRepositoryIT() {
        super();
        List<String> superadmins = new ArrayList<>();
        Set<String> allApps = new HashSet<>();
        for (Object[] values : AuthorizationRepositoryDataProvider.getExperimentTimes()) {
            String app = (String) values[0];
            String user = (String) values[1];
            if (!"*".equals(app)) {
                Set<String> apps = userApplicationMap.getOrDefault(user, new HashSet<>());
                apps.add(app);
                userApplicationMap.put(user, apps);
                allApps.add(app);
            } else {
                superadmins.add(user);
            }
        }

        for (String superadmin : superadmins) {
            userApplicationMap.put(superadmin, allApps);
        }
    }

    @Test(groups = {"prepare"},
            dataProvider = "AuthorizationDataProvider",
            dataProviderClass = AuthorizationRepositoryDataProvider.class)
    public void prepare(String appName, String username, String role) {
        super.setupDb(appName, username, role);
    }


    @Test(dependsOnGroups = "prepare", groups = "phase1")
    public void testGetApplicationUsers() {
        for (String app : apps) {
            UserRoleList userRoleList = authorizationRepository.getApplicationUsers(Application.Name.valueOf(app));
            Set<String> usernames = appUser.get(app);
            for (UserRole userRole : userRoleList.getRoleList()) {
                assertThat(usernames, hasItem(userRole.getUserID().getUsername()));
            }
        }
    }

    @Test(dependsOnGroups = "prepare", groups = " phase1")
    public void testGetUserPermissionsList() {
        UserPermissionsList userPermissionsList =
                authorizationRepository.getUserPermissionsList(UserInfo.Username.valueOf("wasabi_writer"));
        assertThat(userPermissionsList.getPermissionsList().size(), is(2));
        for (UserPermissions userPermissions : userPermissionsList.getPermissionsList()) {
            assertThat(apps, hasItem(userPermissions.getApplicationName().toString()));
            assertThat(userPermissions.getPermissions(), is(Role.READWRITE.getRolePermissions()));
        }

        userPermissionsList =
                authorizationRepository.getUserPermissionsList(UserInfo.Username.valueOf("wasabi_reader"));
        assertThat(userPermissionsList.getPermissionsList().size(), is(1));
        for (UserPermissions userPermissions : userPermissionsList.getPermissionsList()) {
            assertThat(userPermissions.getApplicationName().toString(), is("App1"));
            assertThat(userPermissions.getPermissions(), is(Role.READONLY.getRolePermissions()));
        }

        userPermissionsList =
                authorizationRepository.getUserPermissionsList(UserInfo.Username.valueOf("admin"));
        assertThat(userPermissionsList.getPermissionsList().size(), is(2));
        for (UserPermissions userPermissions : userPermissionsList.getPermissionsList()) {
            assertThat(apps, hasItem(userPermissions.getApplicationName().toString()));
            assertThat(userPermissions.getPermissions(), is(Role.SUPERADMIN.getRolePermissions()));
        }
    }

    @Test(dependsOnGroups = "prepare", groups = " phase1",
            dataProvider = "AuthorizationDataProvider",
            dataProviderClass = AuthorizationRepositoryDataProvider.class)
    public void testGetUserPermissions(String appName, String username, String role) {
        //Not testing * because it is not a valid application.name therefore not a valid test case
        if (!"*".equals(appName)) {
            UserPermissions userPermissions = authorizationRepository.getUserPermissions(
                    UserInfo.Username.valueOf(username),
                    Application.Name.valueOf(appName)
            );
            assertThat(userPermissions.getApplicationName().toString(), is(appName));
            assertThat(userPermissions.getPermissions(), is(Role.valueOf(role).getRolePermissions()));
        }
    }

    @Test(dependsOnGroups = "prepare", groups = "phase1")
    public void testCheckSuperAdminPermissions() {
        UserPermissions result = authorizationRepository.checkSuperAdminPermissions(UserInfo.Username.valueOf("admin"),
                Application.Name.valueOf("App1"));
        assertThat(result.getApplicationName().toString(), is("App1"));
        assertThat(result.getPermissions(), is(Role.SUPERADMIN.getRolePermissions()));

        //TODO: is this valid?
        result = authorizationRepository.checkSuperAdminPermissions(UserInfo.Username.valueOf("admin"),
                Application.Name.valueOf("NonExistingApp"));
        assertThat(result.getApplicationName().toString(), is("NonExistingApp"));
        assertThat(result.getPermissions(), is(Role.SUPERADMIN.getRolePermissions()));

        result = authorizationRepository.checkSuperAdminPermissions(UserInfo.Username.valueOf("superman"),
                Application.Name.valueOf("App1"));
        assertThat(result, is(nullValue()));
    }

    @Test(dependsOnGroups = "prepare", groups = "phase1",
            dataProvider = "AuthorizationDataProvider",
            dataProviderClass = AuthorizationRepositoryDataProvider.class)
    public void testGetUserInfo(String appName, String username, String role) {
        UserInfo userInfo = authorizationRepository.getUserInfo(UserInfo.Username.valueOf(username));
        UserInfo expected = userDirectory.lookupUser(UserInfo.Username.valueOf(username));
        assertThat(userInfo.getEmail(), is(expected.getEmail()));
        assertThat(userInfo.getUsername().getUsername(), is(username));
        assertThat(userInfo.getLastName(), is(expected.getLastName()));
        assertThat(userInfo.getFirstName(), is(expected.getFirstName()));
    }

    @Test(dependsOnGroups = "prepare", groups = "phase1")
    public void testGetuserInfoNonExistingUser() {
        UserInfo userInfo = authorizationRepository.getUserInfo(UserInfo.Username.valueOf("NoOne"));
        assertThat(userInfo, is(nullValue()));
    }

    @Test(dependsOnGroups = "prepare", groups = "phase1",
            dataProvider = "AuthorizationDataProvider",
            dataProviderClass = AuthorizationRepositoryDataProvider.class)
    public void testGetUserRoleList(String appName, String username, String role) {
        UserRoleList userRoleList = authorizationRepository.getUserRoleList(UserInfo.Username.valueOf(username));
        assertThat(userRoleList.getRoleList().size(), is(userApplicationMap.get(username).size()));
    }

    @Test(groups = "saveDb")
    public void testSetUerInfo() {
        UserInfo userInfo = UserInfo.newInstance(UserInfo.Username.valueOf("wa"))
                .withEmail("wa@sabi.com")
                .withUserId("wa")
                .withLastName("sabi")
                .withFirstName("wa")
                .build();
        authorizationRepository.setUserInfo(userInfo);
        UserInfo result = authorizationRepository.getUserInfo(UserInfo.Username.valueOf("wa"));
        assertThat(result.getFirstName(), is(userInfo.getFirstName()));
        assertThat(result.getLastName(), is(userInfo.getLastName()));
        assertThat(result.getUsername(), is(userInfo.getUsername()));
        assertThat(result.getEmail(), is(userInfo.getEmail()));
    }

    @Test(groups = "saveDb")
    public void testSetUserRole() {
        UserRole userRole = UserRole.newInstance(Application.Name.valueOf("AR"), Role.ADMIN)
                .withUserID(UserInfo.Username.valueOf("Billy"))
                //Since Billy is not in our userDirectory.properties, we need to set the following to empty
                .withUserEmail("")
                .withFirstName("")
                .withLastName("")
                .build();

        authorizationRepository.setUserRole(userRole);
        UserRoleList userRoleList = authorizationRepository.getUserRoleList(UserInfo.Username.valueOf("Billy"));
        assertThat(userRoleList.getRoleList().size(), is(1));
        UserRole expected = userRoleList.getRoleList().get(0);
        assertThat(userRole, is(expected));
    }

    @Test(groups = "saveDb", dependsOnMethods = "testSetUserRole")
    public void testDeleteUserRole() {
        authorizationRepository.deleteUserRole(UserInfo.Username.valueOf("Billy"), Application.Name.valueOf("AR"));
        UserRoleList userRoleList = authorizationRepository.getUserRoleList(UserInfo.Username.valueOf("Billy"));
        assertThat(userRoleList.getRoleList().size(), is(0));
    }


}