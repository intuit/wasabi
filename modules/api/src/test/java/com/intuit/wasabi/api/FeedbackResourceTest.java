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
package com.intuit.wasabi.api;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.feedback.Feedback;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FeedbackResourceTest {

    @Mock
    private Authorization authorization;
    @Mock
    private UserInfo.Username username;
    @Mock
    private Feedback feedback;
    @Mock
    private HttpHeader httpHeader;
    @Mock
    private UserFeedback userFeedback;
    @Mock
    private List<UserFeedback> userFeedbacks;
    @Mock
    private ResponseBuilder responseBuilder;
    @Mock
    private Response response;
    @Captor
    private ArgumentCaptor<Map<String, List<UserFeedback>>> userFeedbackCaptor;
    private FeedbackResource feedbackResource;

    @Before
    public void before() {
        feedbackResource = new FeedbackResource(authorization, feedback, httpHeader);
    }

    @Test
    public void getAllUserFeedback() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(feedback.getAllUserFeedback()).thenReturn(userFeedbacks);
//        whenHttpHeader(anyCollection());
        when(httpHeader.headers()).thenReturn(responseBuilder);
        when(responseBuilder.entity(anyCollection())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        feedbackResource.getAllUserFeedback("foo");

        verify(authorization).getUser("foo");
        verify(authorization).checkSuperAdmin(username);
        verify(feedback).getAllUserFeedback();
        verify(httpHeader).headers();
        verify(responseBuilder).entity(userFeedbackCaptor.capture());
        assertThat(userFeedbackCaptor.getValue().size(), is(1));
        assertThat(userFeedbackCaptor.getValue(), hasEntry("feedback", userFeedbacks));
        verify(responseBuilder).build();
    }

    @Test
    public void postFeedback() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(httpHeader.headers(CREATED)).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        feedbackResource.postFeedback(userFeedback, "foo");

        verify(authorization).getUser("foo");
        verify(userFeedback).setUsername(username);
        verify(feedback).createUserFeedback(userFeedback);
        verify(httpHeader).headers(CREATED);
        verify(responseBuilder, times(0)).entity(anyObject());
        verify(responseBuilder).build();
    }

    @Test
    public void getUserFeedback() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(feedback.getUserFeedback(username)).thenReturn(userFeedbacks);
        when(httpHeader.headers()).thenReturn(responseBuilder);
        when(responseBuilder.entity(anyCollection())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        feedbackResource.getUserFeedback(username, "foo");

        verify(authorization).getUser("foo");
        verify(authorization).checkSuperAdmin(username);
        verify(feedback).getUserFeedback(username);
        verify(httpHeader).headers();
        verify(responseBuilder).entity(userFeedbackCaptor.capture());
        assertThat(userFeedbackCaptor.getValue().size(), is(1));
        assertThat(userFeedbackCaptor.getValue(), hasEntry("feedbackList", userFeedbacks));
        verify(responseBuilder).build();
    }
}
