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

import com.intuit.wasabi.api.pagination.PaginationHelper;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.authorizationobjects.Permission.READ;
import static com.intuit.wasabi.authorizationobjects.Permission.UPDATE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationsResourceTest {

    @Mock
    private List<Application.Name> applicationNames;
    @Mock
    private AuthorizedExperimentGetter authorizedExperimentGetter;
    @Mock
    private Experiments experiments;
    @Mock
    private Authorization authorization;
    @Mock
    private Pages pages;
    @Mock
    private Priorities priorities;
    @Mock
    private Experiment experiment;
    @Mock
    private Application.Name applicationName;
    @Mock
    private Experiment.Label experimentLabel;
    @Mock
    private ExperimentIDList experimentIDList;
    @Mock
    private PrioritizedExperimentList prioritizedExperimentList;
    @Mock
    private UserInfo.Username username;
    @Mock
    private Page.Name pageName;
    @Mock
    private HttpHeader httpHeader;
    @Mock
    private ResponseBuilder responseBuilder;
    @Mock
    private List<Experiment> experimentsByName;
    @Mock
    private List<Page> pagesByName;
    @Mock
    private List<PageExperiment> pageExperiments;
    @Mock
    private Response response;
    @Captor
    private ArgumentCaptor<Map<String, List<Page>>> pagesByNameCaptor;
    @Captor
    private ArgumentCaptor<Map<String, List<PageExperiment>>> pageExperimentsCaptor;
    private ApplicationsResource applicationsResource;
    @Mock
    private PaginationHelper<Experiment> experimentPaginationHelper;

    @Before
    public void setup() {
        applicationsResource = new ApplicationsResource(authorizedExperimentGetter, experiments, authorization, priorities,
                pages, httpHeader, experimentPaginationHelper);
    }

    @Test
    public void getApplications() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(experiments.getApplications()).thenReturn(applicationNames);
        whenHttpHeader(applicationNames);

        applicationsResource.getApplications("foo");

        verify(authorization).getUser("foo");
        verify(experiments).getApplications();
        verifyHttpHeader(applicationNames);
    }

    private void whenHttpHeader(final Object entity) {
        when(httpHeader.headers()).thenReturn(responseBuilder);
        when(responseBuilder.entity(entity)).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);
    }

    private void verifyHttpHeader(final Object entity) {
        verifyHttpHeader(entity, times(1));
    }

    private void verifyHttpHeader(final Object entity, VerificationMode verificationMode) {
        verify(httpHeader, verificationMode).headers();
        verify(responseBuilder, verificationMode).entity(entity);
        verify(responseBuilder, verificationMode).build();
    }

    @Test
    public void getApplicationsWithNullAuthorizationUser() throws Exception {
        when(authorization.getUser("foo")).thenReturn(null);

        try {
            applicationsResource.getApplications("foo");
        } catch (AuthenticationException ae) {
            assertThat(ae.getErrorCode().toString(), is("WASABI-4501"));
            assertThat(ae.getErrorCode().name(), is("AUTHENTICATION_FAILED"));
            assertThat(ae.getErrorCode().getResponseCode(), is(UNAUTHORIZED.getStatusCode()));
            assertThat(ae.getDetailMessage(), is("User is not authenticated"));
        }

        verify(authorization).getUser("foo");
        verify(experiments, times(0)).getApplications();
        verifyHttpHeader(applicationNames, times(0));
    }

    @Test
    public void getExperiment() throws Exception {
        when(authorizedExperimentGetter.getAuthorizedExperimentByName("foo", applicationName,
                experimentLabel)).thenReturn(experiment);
        whenHttpHeader(experiment);

        applicationsResource.getExperiment(applicationName, experimentLabel, "foo");

        verify(authorizedExperimentGetter).getAuthorizedExperimentByName("foo", applicationName,
                experimentLabel);
        verifyHttpHeader(experiment);
    }

    @Test
    public void getExperiments() throws Exception {
        when(authorizedExperimentGetter.getAuthorizedExperimentsByName("foo", applicationName))
                .thenReturn(experimentsByName);
        whenHttpHeader(experimentsByName);

        applicationsResource.getExperiments(applicationName, "foo", 0, -1, "", "", "");

        verify(authorizedExperimentGetter).getAuthorizedExperimentsByName("foo", applicationName);
        verifyHttpHeader(experimentsByName);
    }

    @Test
    public void createPriorities() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(httpHeader.headers(NO_CONTENT)).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        applicationsResource.createPriorities(applicationName, experimentIDList, "foo");

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, UPDATE);
        verify(priorities).createPriorities(applicationName, experimentIDList, true);
        verify(httpHeader).headers(NO_CONTENT);
        verify(responseBuilder, times(0)).entity(anyObject());
        verify(responseBuilder).build();
    }

    @Test
    public void getPriorities() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(priorities.getPriorities(applicationName, true)).thenReturn(prioritizedExperimentList);
        whenHttpHeader(prioritizedExperimentList);

        applicationsResource.getPriorities(applicationName, "foo");

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, READ);
        verifyHttpHeader(prioritizedExperimentList);
    }

    @Test
    public void getPagesForApplication() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(pages.getPageList(applicationName)).thenReturn(pagesByName);
//        whenHttpHeader(anyCollection());
        when(httpHeader.headers()).thenReturn(responseBuilder);
        when(responseBuilder.entity(anyCollection())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        applicationsResource.getPagesForApplication(applicationName, "foo");

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, READ);
        verify(pages).getPageList(applicationName);
//        verifyHttpHeader(anyCollection());
        verify(responseBuilder).entity(pagesByNameCaptor.capture());
        assertThat(pagesByNameCaptor.getValue().size(), is(1));
        assertThat(pagesByNameCaptor.getValue(), hasEntry("pages", pagesByName));
    }

    @Test
    public void getExperimentsByPages() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(pages.getExperiments(applicationName, pageName)).thenReturn(pageExperiments);
//        whenHttpHeader(anyCollection());
        when(httpHeader.headers()).thenReturn(responseBuilder);
        when(responseBuilder.entity(anyCollection())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        applicationsResource.getExperiments(applicationName, pageName, "foo");

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, READ);
//        verifyHttpHeader(anyCollection());
        verify(httpHeader).headers();
        verify(responseBuilder).entity(pageExperimentsCaptor.capture());
        verify(responseBuilder).build();
        assertThat(pageExperimentsCaptor.getValue().size(), is(1));
        assertThat(pageExperimentsCaptor.getValue(), hasEntry("experiments", pageExperiments));
    }
}
