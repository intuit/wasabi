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

import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.UserFeedbackAccessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CassandraFeedbackRepositoryTest {

    @Mock
    UserFeedbackAccessor userFeedbackAccessor;

    @Mock
    Result<com.intuit.wasabi.repository.cassandra.pojo.UserFeedback> result;

    CassandraFeedbackRepository repository;

    @Before
    public void setUp() throws Exception {
        repository = new CassandraFeedbackRepository(userFeedbackAccessor);
    }

    @Test
    public void testCreateFeedbackSuccess() {

        UserFeedback userFeedback = new UserFeedback();
        userFeedback.setUsername(Username.valueOf("userId1"));
        userFeedback.setSubmitted(new Date());
        userFeedback.setScore(2);
        userFeedback.setComments("comments1");
        userFeedback.setContactOkay(true);
        userFeedback.setEmail("userId1@example.com");

        repository.createUserFeedback(userFeedback);

    }

    @Test(expected = RepositoryException.class)
    public void testCreateFeedbackThrowsException() {

        UserFeedback userFeedback = new UserFeedback();
        userFeedback.setUsername(Username.valueOf("userId1"));
        userFeedback.setSubmitted(new Date());
        userFeedback.setScore(2);
        userFeedback.setComments("comments1");
        userFeedback.setContactOkay(true);
        userFeedback.setEmail("userId1@example.com");

        doThrow(new RuntimeException("testException")).when(userFeedbackAccessor).createUserFeedback(
                userFeedback.getUsername().toString(), userFeedback.getSubmitted(),
                userFeedback.getScore(),
                userFeedback.getComments(), userFeedback.isContactOkay(),
                userFeedback.getEmail());

        repository.createUserFeedback(userFeedback);

    }

    @Test
    public void testGetUserFeedbackSuccess() {

        com.intuit.wasabi.repository.cassandra.pojo.UserFeedback userFeedback = new com.intuit.wasabi.repository.cassandra.pojo.UserFeedback();
        userFeedback.setUserId("userid1");
        userFeedback.setSubmitted(new Date());
        userFeedback.setScore(2);
        userFeedback.setComment("comments1");
        userFeedback.setContactOkay(true);
        userFeedback.setUserEmail("userId1@example.com");

        when(userFeedbackAccessor.getUserFeedback("userid1")).thenReturn(result);
        List<com.intuit.wasabi.repository.cassandra.pojo.UserFeedback> userFeedbacks = new ArrayList<>();
        userFeedbacks.add(userFeedback);

        when(result.all()).thenReturn(userFeedbacks);

        List<UserFeedback> response = repository.getUserFeedback(Username.valueOf("userid1"));

        assertEquals("Size should be eq", 1, response.size());
        UserFeedback feedback = response.get(0);
        assertEquals("user should be same", userFeedback.getUserId(), feedback.getUsername().getUsername());
        assertEquals("submitted should be same", userFeedback.getSubmitted(), feedback.getSubmitted());
        assertEquals("score should be same", userFeedback.getScore(), feedback.getScore());
        assertEquals("contactOk should be same", userFeedback.isContactOkay(), feedback.isContactOkay());
        assertEquals("email should be same", userFeedback.getUserEmail(), feedback.getEmail());
    }

    @Test
    public void testGetUserFeedbackEmptySuccess() {

        when(userFeedbackAccessor.getUserFeedback("userid1")).thenReturn(result);
        List<com.intuit.wasabi.repository.cassandra.pojo.UserFeedback> userFeedbacks = new ArrayList<>();
        when(result.all()).thenReturn(userFeedbacks);

        List<UserFeedback> response = repository.getUserFeedback(Username.valueOf("userid1"));

        assertEquals("Size should be eq", 0, response.size());
    }

    @Test(expected = RepositoryException.class)
    public void testGetUserFeedbackThrowsException() {

        doThrow(new RuntimeException("testException")).when(userFeedbackAccessor).getUserFeedback("userid1");

        List<UserFeedback> response = repository.getUserFeedback(Username.valueOf("userid1"));

    }

    @Test
    public void testGetAllUserFeedbackSuccess() {

        com.intuit.wasabi.repository.cassandra.pojo.UserFeedback userFeedback = new com.intuit.wasabi.repository.cassandra.pojo.UserFeedback();
        userFeedback.setUserId("userid1");
        userFeedback.setSubmitted(new Date());
        userFeedback.setScore(2);
        userFeedback.setComment("comments1");
        userFeedback.setContactOkay(true);
        userFeedback.setUserEmail("userId1@example.com");

        when(userFeedbackAccessor.getAllUserFeedback()).thenReturn(result);
        List<com.intuit.wasabi.repository.cassandra.pojo.UserFeedback> userFeedbacks = new ArrayList<>();
        userFeedbacks.add(userFeedback);

        when(result.all()).thenReturn(userFeedbacks);

        List<UserFeedback> response = repository.getAllUserFeedback();

        assertEquals("Size should be eq", 1, response.size());
        UserFeedback feedback = response.get(0);
        assertEquals("user should be same", userFeedback.getUserId(), feedback.getUsername().getUsername());
        assertEquals("submitted should be same", userFeedback.getSubmitted(), feedback.getSubmitted());
        assertEquals("score should be same", userFeedback.getScore(), feedback.getScore());
        assertEquals("contactOk should be same", userFeedback.isContactOkay(), feedback.isContactOkay());
        assertEquals("email should be same", userFeedback.getUserEmail(), feedback.getEmail());
    }

    @Test(expected = RepositoryException.class)
    public void testGetAllUserFeedbackThrowsException() {

        doThrow(new RuntimeException("testException")).when(userFeedbackAccessor).getAllUserFeedback();

        List<UserFeedback> response = repository.getAllUserFeedback();

    }
}
