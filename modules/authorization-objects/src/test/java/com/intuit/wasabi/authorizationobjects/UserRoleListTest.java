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

import com.intuit.wasabi.experimentobjects.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class UserRoleListTest {

    private List<UserRole> roleList = new ArrayList<>();

    private UserRoleList userRoleList;

    @Before
    public void setUp() throws Exception {
        userRoleList = new UserRoleList();
    }

    @Test
    public void testUserRoleListSet() {
        userRoleList.setRoleList(roleList);
        assertEquals(roleList, userRoleList.getRoleList());
        userRoleList.addRole(UserRole.newInstance(Application.Name.valueOf("testApp"), Role.ADMIN).build());
        assertEquals(1, userRoleList.getRoleList().size());
    }

    @Test
    public void testUserRoleListFromOther() {
        UserRoleList newUserRoleList = new UserRoleList(5);

        assertEquals(userRoleList, newUserRoleList);
    }


}
