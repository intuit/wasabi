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
package com.intuit.wasabi.authorizationobjects;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.intuit.wasabi.experimentobjects.Application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class UserPermissionsTest {

    private Application.Name applicationName = Application.Name.valueOf("testApp");
    private List<Permission> permissions;

    private UserPermissions userPerm;

    @Before
    public void setUp() throws Exception {
        userPerm = getUserPermissions();
    }

    /**
     *
     */
    private UserPermissions getUserPermissions() {
        permissions = new ArrayList<Permission>();
        permissions.add(Permission.CREATE);
        permissions.add(Permission.DELETE);
        return UserPermissions.newInstance(applicationName, permissions).build();
    }

    @Test
    public void testUserPermissionsSet() {
        userPerm.setApplicationName(applicationName);
        userPerm.setPermissions(permissions);
        assertEquals(applicationName, userPerm.getApplicationName());
        assertEquals(permissions, userPerm.getPermissions());
    }

    @Test
    public void testAssignmentFromOther() {
        UserPermissions newUserPerm = UserPermissions.from(userPerm).build();

        assertEquals(userPerm, newUserPerm);
    }

}
