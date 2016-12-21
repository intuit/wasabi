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
package com.intuit.wasabi.authenticationobjects;

import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the {@link UserInfo}
 */
@RunWith(MockitoJUnitRunner.class)
public class UserInfoTest {

    private Username username = UserInfo.Username.valueOf("testUser");
    private String userId = "123456789";
    private String firstName = "testFirstName";
    private String lastName = "testLastName";
    private String email = "user@example.com";
    private String password = "pwd";

    private UserInfo userInfo;

    @Before
    public void setUp() throws Exception {
        userInfo = getUserInfo();
    }

    private UserInfo getUserInfo() {
        return UserInfo.from(username)
                .withEmail(email)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withUserId(userId)
                .withPassword(password)
                .build();
    }

    @Test
    public void testUserInfo() {
        assertEquals(userInfo.getEmail(), email);
        assertEquals(userInfo.getFirstName(), firstName);
        assertEquals(userInfo.getLastName(), lastName);
        assertEquals(userInfo.getUserId(), userId);
        assertEquals(userInfo.getUsername(), username);
        assertEquals(userInfo.getPassword(), password);

        assertTrue(userInfo.toString().contains(email));
        assertTrue(userInfo.toString().contains(firstName));
        assertTrue(userInfo.toString().contains(lastName));
        assertTrue(userInfo.toString().contains(userId));
        assertTrue(userInfo.toString().contains(username.toString()));
        assertFalse(userInfo.toString().contains(password));
    }

    @Test
    public void testUserInfoSet() {
        userInfo.setEmail(email);
        userInfo.setFirstName(firstName);
        userInfo.setLastName(lastName);
        userInfo.setUserId(userId);
        userInfo.setUsername(username);
        userInfo.setPassword("pw");
        assertEquals(email, userInfo.getEmail());
        assertEquals(firstName, userInfo.getFirstName());
        assertEquals(lastName, userInfo.getLastName());
        assertEquals(userId, userInfo.getUserId());
        assertEquals(username, userInfo.getUsername());
        assertEquals("pw", userInfo.getPassword());
    }

    @Test
    public void testUserInfoFromOther() {
        UserInfo otherUserInfo = getUserInfo();
        UserInfo otherUserInfo1 = UserInfo.newInstance(username).build();

        assertNotNull(otherUserInfo1.getUsername());

        assertEquals(userInfo, otherUserInfo);
        assertEquals(userInfo, otherUserInfo);
        assertEquals(userInfo.hashCode(), otherUserInfo.hashCode());
    }

    @Test
    public void testUsername() {
        assertEquals(userInfo.getUsername(), userInfo.getUsername());
        assertEquals(UserInfo.Username.valueOf("testUser"), userInfo.getUsername());

        userInfo.getUsername().setUsername("testUserNew");
        assertEquals("testUserNew", userInfo.getUsername().getUsername());
    }
}