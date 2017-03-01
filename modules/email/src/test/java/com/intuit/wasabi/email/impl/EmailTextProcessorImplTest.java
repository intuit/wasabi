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
package com.intuit.wasabi.email.impl;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorizationobjects.Role;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.email.EmailLinksList;
import com.intuit.wasabi.email.EmailTextProcessor;
import com.intuit.wasabi.eventlog.EventLogEventType;
import com.intuit.wasabi.eventlog.events.BucketChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketCreateEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.eventlog.events.ExperimentCreateEvent;
import com.intuit.wasabi.exceptions.WasabiEmailException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBase;
import com.intuit.wasabi.repository.AuthorizationRepository;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.intuit.wasabi.eventlog.EventLogEventType.BUCKET_CHANGED;
import static com.intuit.wasabi.eventlog.EventLogEventType.EXPERIMENT_CHANGED;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class tests the {@link com.intuit.wasabi.email.impl.EmailTextProcessorImpl}
 *
 */
public class EmailTextProcessorImplTest {

    private AuthorizationRepository repoMock = mock(AuthorizationRepository.class);

    private EmailTextProcessor textProcessor = new EmailTextProcessorImpl(repoMock);

    private Application.Name appName = Application.Name.valueOf("unitTest");

    private List<String> links = new ArrayList<>();

    @Test
    public void testMessageResolution() {

        Experiment.Label label = Experiment.Label.valueOf("expLabel");
        Experiment.ID id = Experiment.ID.newInstance();

        ExperimentBase exp = Mockito.mock(Experiment.class);

        when(exp.getApplicationName()).thenReturn(appName);
        when(exp.getLabel()).thenReturn(label);
        when(exp.getID()).thenReturn(id);

        EventLogEvent event = new ExperimentCreateEvent(exp);
        String emailMessage = textProcessor.getMessage(event);

        assertTrue(emailMessage.contains(appName.toString()));
        assertTrue(emailMessage.contains(label.toString()));
        assertTrue(emailMessage.contains(id.toString()));
    }

    @Test
    public void testMessageAddressors() {

        ExperimentBase exp = mock(Experiment.class);
        UserRoleList roleListMock = mock(UserRoleList.class);
        UserRole roleOk = mock(UserRole.class);
        UserRole roleOk2 = mock(UserRole.class);
        UserRole roleBad = mock(UserRole.class);

        //this is a lot of mocking ... it is basically representing
        //the call to get the list of app admins and getting their email addresses
        when(roleOk.getRole()).thenReturn(Role.ADMIN);
        when(roleOk.getUserEmail()).thenReturn("valid.email@you.org");
        when(roleOk2.getUserEmail()).thenReturn("valid.email@gmail.com");
        when(roleBad.getUserEmail()).thenReturn("alskdjuifa");
        when(roleListMock.getRoleList()).thenReturn(Arrays.asList(roleOk, roleOk2, roleBad));
        when(exp.getApplicationName()).thenReturn(appName);
        when(repoMock.getApplicationUsers(appName)).thenReturn(roleListMock);
        EventLogEvent event = new ExperimentCreateEvent(exp);

        Set<String> emailMessage = textProcessor.getAddressees(event);

        assertEquals(emailMessage.size(), 1);
        assertTrue(emailMessage.contains("valid.email@you.org"));
        //assertTrue(emailMessage.contains("valid.email@gmail.com"));
        assertFalse(emailMessage.contains("alskdjuifa"));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubjectExtraction() {

        EventLogEvent mockedEventLog = mock(EventLogEvent.class);
        assertThat(textProcessor.getSubject(appName), is("A User requests access to your Application"));
        when(mockedEventLog.getType()).thenReturn(BUCKET_CHANGED);
        String subject = textProcessor.getSubject(mockedEventLog);
        assertTrue(StringUtils.containsIgnoreCase(subject, "bucket"));

        when(mockedEventLog.getType()).thenReturn(EXPERIMENT_CHANGED);
        subject = textProcessor.getSubject(mockedEventLog);
        assertTrue(StringUtils.containsIgnoreCase(subject, "experiment"));

        when(mockedEventLog.getType()).thenReturn(EventLogEventType.EXPERIMENT_CREATED);
        subject = textProcessor.getSubject(mockedEventLog);
        assertThat(subject, is("Experiment Created"));

        when(mockedEventLog.getType()).thenReturn(EventLogEventType.UNKNOWN);
        subject = textProcessor.getSubject(mockedEventLog);
        fail();
        assertThat(subject, is("NOT_POSSIBLE"));
    }

    /**
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMessageExtraction() {
        BucketCreateEvent mockedEventLog = mock(BucketCreateEvent.class);
        ExperimentBase mockedExperimentBase = mock(ExperimentBase.class);
        Bucket mockedBucket = mock(Bucket.class);
        Experiment.Label experimentLabel = mock(Experiment.Label.class);
        Bucket.Label bucketLabel = mock(Bucket.Label.class);

        when(experimentLabel.toString()).thenReturn("mockedExperimentLabel");
        when(bucketLabel.toString()).thenReturn("mockedBucketLabel");
        when(mockedEventLog.getBucket()).thenReturn(mockedBucket);
        when(mockedBucket.getLabel()).thenReturn(bucketLabel);
        when(mockedExperimentBase.getLabel()).thenReturn(experimentLabel);
        when(mockedEventLog.getExperiment()).thenReturn(mockedExperimentBase);
        when(mockedEventLog.getType()).thenReturn(EventLogEventType.BUCKET_CREATED);

        String message = textProcessor.getMessage(mockedEventLog);

        assertThat(message, is(not(nullValue())));

        when(mockedEventLog.getType()).thenReturn(EventLogEventType.UNKNOWN);
        message = textProcessor.getMessage(mockedEventLog);

        fail();

    }

    @Test(expected = WasabiEmailException.class)
    public void testNoAdminsForApp() {

        UserRoleList roleListMock = mock(UserRoleList.class);
        UserRole roleBad = mock(UserRole.class);

        when(roleBad.getUserEmail()).thenReturn("really invalid email");
        when(roleListMock.getRoleList()).thenReturn(Arrays.asList(roleBad));
        when(repoMock.getApplicationUsers(appName)).thenReturn(roleListMock);

        textProcessor.getAddressees(appName); //this should throw IllArgumentExp
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAddresseesDefault() {
        EventLogEvent eventLogEvent = mock(EventLogEvent.class);
        when(eventLogEvent.getType()).thenReturn(EventLogEventType.UNKNOWN);
        textProcessor.getAddressees(eventLogEvent);
    }

    @Test
    public void testAccessTemplateForApp() {
        links.add("https://wasabi.you.org/");
        String msg = textProcessor.getMessage(appName, UserInfo.Username.valueOf("Jabb0r"), EmailLinksList.withEmailLinksList(links).build());
        assertTrue(StringUtils.containsIgnoreCase(msg, appName.toString()));
        assertTrue(StringUtils.containsIgnoreCase(msg, "Jabb0r"));
    }

    @Test
    public void testMessageResolutionForExpChange() {

        Experiment.Label label = Experiment.Label.valueOf("expChanged");
        Experiment.ID id = Experiment.ID.newInstance();

        ExperimentBase exp = Mockito.mock(Experiment.class);

        when(exp.getApplicationName()).thenReturn(appName);
        when(exp.getLabel()).thenReturn(label);
        when(exp.getID()).thenReturn(id);

        EventLogEvent event = new ExperimentChangeEvent(exp, "label", "newUiFeature", "NewUI");
        String emailMessage = textProcessor.getMessage(event);

        assertTrue(emailMessage.contains(appName.toString()));
        assertTrue(emailMessage.contains(label.toString()));
        assertTrue(emailMessage.contains(id.toString()));
        assertTrue(emailMessage.contains("label"));
        assertTrue(emailMessage.contains("newUiFeature"));
        assertTrue(emailMessage.contains("NewUI"));
    }

    @Test
    public void testMessageResolutionForBucketChange() {

        Experiment.Label label = Experiment.Label.valueOf("expChanged");
        Experiment.ID id = Experiment.ID.newInstance();

        ExperimentBase exp = Mockito.mock(Experiment.class);
        Bucket buck = Mockito.mock(Bucket.class);

        when(exp.getApplicationName()).thenReturn(appName);
        when(exp.getLabel()).thenReturn(label);
        when(exp.getID()).thenReturn(id);
        when(buck.getLabel()).thenReturn(Bucket.Label.valueOf("CaseA"));

        EventLogEvent event = new BucketChangeEvent(exp, buck, "label", "casea", "CaseA");
        String emailMessage = textProcessor.getMessage(event);

        assertTrue(emailMessage.contains(appName.toString()));
        assertTrue(emailMessage.contains(label.toString()));
        assertTrue(emailMessage.contains("label"));
        assertTrue(emailMessage.contains("casea"));
        assertTrue(emailMessage.contains("CaseA"));
    }

}
