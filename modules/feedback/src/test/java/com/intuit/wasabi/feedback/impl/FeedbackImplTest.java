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
package com.intuit.wasabi.feedback.impl;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import com.intuit.wasabi.repository.FeedbackRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.userdirectory.UserDirectory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FeedbackImplTest {

    @Mock
    FeedbackRepository feedbackRepository;

    @Mock
    UserDirectory userDirectory;
    FeedbackImpl feedbackImpl;
    private static final UserInfo.Username USER = UserInfo.Username.valueOf("user@example.com");

    private UserFeedback userFeedback;

    @Before
    public void setup() {
        userFeedback = UserFeedback.newInstance(USER).build();
        feedbackImpl = new FeedbackImpl(feedbackRepository, userDirectory);
    }

    @Test
    public void testCreateUserFeedback() throws Exception {

        userFeedback.setUsername(null);
        verifyException(feedbackImpl, NullPointerException.class).createUserFeedback(userFeedback);

        userFeedback.setUsername(USER);
        userFeedback.setSubmitted(null);
        verifyException(feedbackImpl, NullPointerException.class).createUserFeedback(userFeedback);

        userFeedback.setSubmitted(new Date());
        verifyException(feedbackImpl, IllegalArgumentException.class).createUserFeedback(userFeedback);

        userFeedback.setContactOkay(true);
        UserInfo userInfo = UserInfo.newInstance(USER).build();
        when(userDirectory.lookupUser(USER)).thenReturn(userInfo);
        userFeedback.setScore(7);
        feedbackImpl.createUserFeedback(userFeedback);

        doThrow(RepositoryException.class).when(feedbackRepository).createUserFeedback(userFeedback);
        verifyException(feedbackImpl, RepositoryException.class).createUserFeedback(userFeedback);

        doThrow(AuthenticationException.class).when(userDirectory).lookupUser(USER);
        verifyException(feedbackImpl, AuthenticationException.class).createUserFeedback(userFeedback);
    }

    @Test
    public void testGetUserFeedback() throws Exception {

        FeedbackImpl feedbackImpl = new FeedbackImpl(feedbackRepository, userDirectory);
        verifyException(feedbackImpl, NullPointerException.class).getUserFeedback(null);

        UserInfo.Username username = UserInfo.Username.valueOf("name");
        List<UserFeedback> userFeedbackList = new ArrayList<>();
        userFeedbackList.add(userFeedback);

        when(feedbackRepository.getUserFeedback(username)).thenReturn(userFeedbackList);
        List<UserFeedback> result = feedbackImpl.getUserFeedback(username);
        assert (userFeedbackList.equals(result));

        doThrow(RepositoryException.class).when(feedbackRepository).getUserFeedback(username);
        verifyException(feedbackImpl, RepositoryException.class).getUserFeedback(username);
    }

    @Test
    public void testGetAllUserFeedback() throws Exception {

        FeedbackImpl feedbackImpl = new FeedbackImpl(feedbackRepository, userDirectory);

        List<UserFeedback> userFeedbackList = new ArrayList<>();
        userFeedbackList.add(userFeedback);

        when(feedbackRepository.getAllUserFeedback()).thenReturn(userFeedbackList);
        List<UserFeedback> returned = feedbackImpl.getAllUserFeedback();
        assert (returned.equals(userFeedbackList));

        doThrow(RepositoryException.class).when(feedbackRepository).getAllUserFeedback();
        verifyException(feedbackImpl, RepositoryException.class).getAllUserFeedback();
    }
}
