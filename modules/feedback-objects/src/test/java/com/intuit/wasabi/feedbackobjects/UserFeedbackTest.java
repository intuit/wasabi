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
package com.intuit.wasabi.feedbackobjects;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.intuit.wasabi.authenticationobjects.UserInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UserFeedbackTest {

    private UserInfo.Username username =  UserInfo.Username.valueOf("testUsername");
    private Date submitted = new Date();
    private int score = 1;
    private String comments = "test comments";
    private boolean contactOkay = false;
    private String email = "user@example.com";

    private UserFeedback userFeedback;

    @Before
    public void setUp() throws Exception {
        userFeedback = getUserFeedback();
    }

    private UserFeedback getUserFeedback() {
        return UserFeedback.newInstance(username)
                .withSubmitted(submitted)
                .withScore(score)
                .withComments(comments)
                .withContactOkay(contactOkay)
                .withEmail(email)
                .build();
    }

    @Test
    public void testUserFeedback() {
        assertNotNull(userFeedback.getUsername());
        assertNotNull(userFeedback.getSubmitted());
        assertNotNull(userFeedback.getScore());
        assertNotNull(userFeedback.getComments());
        assertNotNull(userFeedback.isContactOkay());
        assertNotNull(userFeedback.getEmail());
    }

    @Test
    public void testUserFeedbackSet() {
        userFeedback.setUsername(username);
        userFeedback.setSubmitted(submitted);
        userFeedback.setScore(score);
        userFeedback.setComments(comments);
        userFeedback.setContactOkay(contactOkay);
        userFeedback.setEmail(email);

        assertEquals(username, userFeedback.getUsername());
        assertEquals(submitted, userFeedback.getSubmitted());
        assertEquals(score, userFeedback.getScore());
        assertEquals(comments, userFeedback.getComments());
        assertEquals(contactOkay, userFeedback.isContactOkay());
        assertEquals(email, userFeedback.getEmail());
    }

    @Test
    public void testUserFeedbackFromOther() {
        UserFeedback other = UserFeedback.from(userFeedback).build();

        assertEquals(userFeedback, userFeedback);
        assertEquals(userFeedback, other);
        assertEquals(userFeedback.toString(), userFeedback.toString());
        assertEquals(userFeedback.hashCode(), userFeedback.hashCode());
    }
}
