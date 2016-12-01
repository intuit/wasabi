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
package com.intuit.wasabi.authorizationobjects;

import com.intuit.wasabi.experimentobjects.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test for the {@link UserPermissionsList}
 */
@RunWith(MockitoJUnitRunner.class)
public class UserPermissionsListTest {

    private List<UserPermissions> permissionsList = new ArrayList<>();

    private UserPermissionsList userPermissionsList;

    @Before
    public void setUp() throws Exception {
        userPermissionsList = new UserPermissionsList();
    }

    @Test
    public void testUserPermissionsListSet() {
        userPermissionsList.setPermissionsList(permissionsList);
        assertEquals(permissionsList, userPermissionsList.getPermissionsList());
        List<Permission> permissions = new ArrayList<Permission>();
        permissions.add(Permission.CREATE);
        permissions.add(Permission.DELETE);
        userPermissionsList.addPermissions(UserPermissions.newInstance(Application.Name.valueOf("testApp"), permissions).build());
        assertEquals(1, userPermissionsList.getPermissionsList().size());
    }

    @Test
    public void testUserPermissionsListFromOther() {
        UserPermissionsList newUserPermissionsList = new UserPermissionsList(5);

        assertEquals(userPermissionsList, newUserPermissionsList);
        assertEquals(userPermissionsList.hashCode(), newUserPermissionsList.hashCode());
    }

}
