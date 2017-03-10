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
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.intuit.wasabi.authorizationobjects.Permission.READ;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizedExperimentGetterTest {

    @Mock
    private Authorization authorization;
    @Mock
    private Experiments experiments;
    @Mock
    private Experiment.ID experimentId;
    @Mock
    private UserInfo.Username username;
    @Mock
    private Experiment experiment;
    @Mock
    private Application.Name applicationName;
    @Mock
    private Experiment.Label experimentLabel;
    @Mock
    private List<Experiment> experimentList;
    private AuthorizedExperimentGetter authorizedExperimentGetter;

    @Before
    public void before() {
        authorizedExperimentGetter = new AuthorizedExperimentGetter(authorization, experiments);
    }

    @Test
    public void getAuthenticatedExperimentById() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(experiments.getExperiment(experimentId)).thenReturn(experiment);
        when(experiment.getApplicationName()).thenReturn(applicationName);

        authorizedExperimentGetter.getAuthorizedExperimentById("foo", experimentId);

        verify(authorization).getUser("foo");
        verify(experiments).getExperiment(experimentId);
        verify(authorization).checkUserPermissions(username, applicationName, READ);
    }

    @Test(expected = AuthenticationException.class)
    public void getAuthenticatedExperimentByIdWithBadUser() throws Exception {
        when(authorization.getUser("foo")).thenThrow(new AuthenticationException("bad auth"));

        authorizedExperimentGetter.getAuthorizedExperimentById("foo", experimentId);

        verify(authorization, times(0)).getUser("foo");
        verify(experiments, times(0)).getExperiment(experimentId);
        verify(authorization, times(0)).checkUserPermissions(username, applicationName, READ);
    }

    @Test(expected = ExperimentNotFoundException.class)
    public void getAuthenticatedExperimentByIdWithNonExistentExperimentId() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(experiments.getExperiment(experimentId)).thenReturn(null);

        authorizedExperimentGetter.getAuthorizedExperimentById("foo", experimentId);

        verify(authorization).getUser("foo");
        verify(experiments).getExperiment(experimentId);
        verify(authorization, times(0)).checkUserPermissions(username, applicationName, READ);
    }

    @Test
    public void getAuthenticatedExperimentByName() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(experiments.getExperiment(applicationName, experimentLabel)).thenReturn(experiment);

        authorizedExperimentGetter.getAuthorizedExperimentByName("foo", applicationName, experimentLabel);

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, READ);
        verify(experiments).getExperiment(applicationName, experimentLabel);
    }

    @Test(expected = AuthenticationException.class)
    public void getAuthenticatedExperimentByNameWithBadUser() throws Exception {
        when(authorization.getUser("foo")).thenThrow(new AuthenticationException("bad auth"));

        authorizedExperimentGetter.getAuthorizedExperimentByName("foo", applicationName, experimentLabel);

        verify(authorization, times(0)).getUser("foo");
        verify(authorization, times(0)).checkUserPermissions(username, applicationName, READ);
        verify(experiments, times(0)).getExperiment(applicationName, experimentLabel);
    }

    @Test(expected = AuthenticationException.class)
    public void getAuthenticatedExperimentByNameWithBadUserPermissions() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        doThrow(new AuthenticationException("bad perms")).when(authorization)
                .checkUserPermissions(username, applicationName, READ);

        authorizedExperimentGetter.getAuthorizedExperimentByName("foo", applicationName, experimentLabel);

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, READ);
        verify(experiments, times(0)).getExperiment(applicationName, experimentLabel);
    }

    @Test(expected = ExperimentNotFoundException.class)
    public void getAuthenticatedExperimentByNameWithNonExistentExperimentId() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(experiments.getExperiment(experimentId)).thenReturn(null);

        authorizedExperimentGetter.getAuthorizedExperimentByName("foo", applicationName, experimentLabel);

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, READ);
        verify(experiments, times(0)).getExperiment(applicationName, experimentLabel);
    }

    @Test
    public void getAuthenticatedExperimentsByName() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(experiments.getExperiments(applicationName)).thenReturn(experimentList);

        authorizedExperimentGetter.getAuthorizedExperimentsByName("foo", applicationName);

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, READ);
        verify(experiments).getExperiments(applicationName);
    }

    @Test(expected = AuthenticationException.class)
    public void getAuthenticatedExperimentsByNameWithBadUser() throws Exception {
        when(authorization.getUser("foo")).thenThrow(new AuthenticationException("bad auth"));

        authorizedExperimentGetter.getAuthorizedExperimentsByName("foo", applicationName);

        verify(authorization).getUser("foo");
        verify(authorization, times(0)).checkUserPermissions(username, applicationName, READ);
        verify(experiments, times(0)).getExperiment(applicationName, experimentLabel);
    }

    @Test(expected = AuthenticationException.class)
    public void getAuthenticatedExperimentsByNameWithBadUserPermissions() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        doThrow(new AuthenticationException("bad perms")).when(authorization)
                .checkUserPermissions(username, applicationName, READ);

        authorizedExperimentGetter.getAuthorizedExperimentsByName("foo", applicationName);

        verify(authorization).getUser("foo");
        verify(authorization).checkUserPermissions(username, applicationName, READ);
        verify(experiments, times(0)).getExperiment(applicationName, experimentLabel);
    }

    @Test(expected = ExperimentNotFoundException.class)
    public void getAuthenticatedExperimenstByNameWithNonExistentExperimentId() throws Exception {
        when(authorization.getUser("foo")).thenReturn(username);
        when(experiments.getExperiment(experimentId)).thenReturn(null);
        try {
            authorizedExperimentGetter.getAuthorizedExperimentByName("foo", applicationName, experimentLabel);
        } finally {
            verify(authorization).getUser("foo");
            verify(authorization).checkUserPermissions(username, applicationName, READ);
            verify(experiments, times(1)).getExperiment(applicationName, experimentLabel);
        }
    }
}
