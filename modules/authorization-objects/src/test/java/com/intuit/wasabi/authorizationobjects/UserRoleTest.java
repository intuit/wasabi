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

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class UserRoleTest {

    private Application.Name applicationName = Application.Name.valueOf("testApp");
    private Role role = Role.ADMIN;
    private UserInfo.Username userID = UserInfo.Username.valueOf("testUserID");
    private String userEmail = "user@example.com";
    private String firstName = "testFirstName";
    private String lastName = "testLastName";

    private UserRole userRole;

    @Before
    public void setUp() throws Exception {
        userRole = getUserRole();
    }

    /**
     *
     */
    private UserRole getUserRole() {
        return UserRole.newInstance(applicationName, role)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withUserEmail(userEmail)
                .withUserID(userID)
                .build();
    }

    @Test
    public void testUserRole() {
        assertNotNull(userRole.getApplicationName());
        assertNotNull(userRole.getRole());
        assertNotNull(userRole.getUserID());
        assertNotNull(userRole.getUserEmail());
        assertNotNull(userRole.getFirstName());
        assertNotNull(userRole.getLastName());
        assertNotNull(userRole.toString());
    }

    @Test
    public void testUserRoleSet() {
        userRole.setApplicationName(applicationName);
        userRole.setFirstName(firstName);
        userRole.setLastName(lastName);
        userRole.setRole(role);
        userRole.setUserEmail(userEmail);
        userRole.setUserID(userID);
        assertEquals(applicationName, userRole.getApplicationName());
        assertEquals(firstName, userRole.getFirstName());
        assertEquals(lastName, userRole.getLastName());
        assertEquals(role, userRole.getRole());
        assertEquals(userEmail, userRole.getUserEmail());
        assertEquals(userID, userRole.getUserID());
    }

    @Test
    public void testAssignmentFromOther() {
        UserRole newUserRole = UserRole.from(userRole).build();
        assertNotNull(newUserRole.getApplicationName());
        assertNotNull(newUserRole.getRole());
        UserRole other = getUserRole();

        assertEquals(userRole, other);
        assertEquals(userRole, userRole);
        assertEquals(userRole.hashCode(), userRole.hashCode());
    }

}
