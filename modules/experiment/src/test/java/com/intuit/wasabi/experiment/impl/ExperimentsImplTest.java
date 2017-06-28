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
package com.intuit.wasabi.experiment.impl;

import com.intuit.wasabi.assignmentobjects.RuleCache;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.ExperimentCreateEvent;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Buckets;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentsImplTest {

    // All required params for an experiment
    private final static Application.Name testApp = Application.Name.valueOf("testApp");
    private final static Experiment.Label testLabel = Experiment.Label.valueOf("testLabel");
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private ExperimentRepository databaseRepository;
    @Mock
    private ExperimentRepository cassandraRepository;
    @Mock
    private ExperimentValidator validator;
    @Mock
    private RuleCache ruleCache;
    @Mock
    private Experiments experiments;
    @Mock
    private Buckets buckets;
    @Mock
    private Pages pages;
    @Mock
    private Priorities priorities;
    @Mock
    private EventLog eventLog;
    private Date startTime;
    private Date endTime;
    private Double samplingPercent;
    private Experiment.ID experimentID;
    private ExperimentsImpl expImpl;
    private String description;

    @Before
    public void setup() {
        experimentID = Experiment.ID.newInstance();
        startTime = new Date();
        endTime = new Date(startTime.getTime() + 60000);
        samplingPercent = 0.5;
        expImpl = new ExperimentsImpl(databaseRepository, cassandraRepository, experiments, buckets, pages, priorities, validator, ruleCache, eventLog);
        description = "Some description";
    }

    @Test(expected = InvalidIdentifierException.class)
    public void testCreateExperimentFailedValidator() {
        NewExperiment testExp = NewExperiment.withID(experimentID)
                .withAppName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withDescription(description).build();
        testExp.setApplicationName(Application.Name.valueOf(""));
        assertThat(testExp.getApplicationName(), is(Application.Name.valueOf("")));
        expImpl.createExperiment(testExp, UserInfo.from(UserInfo.Username.valueOf("user")).build());
    }

    @Test
    public void testCreateExperimentFailedCassandraCreation() {
        NewExperiment testExp = NewExperiment.withID(experimentID)
                .withAppName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withDescription(description).build();
        //Mock interactions
        doNothing().when(validator).validateNewExperiment(testExp);
        when(databaseRepository.createExperiment(testExp)).thenReturn(experimentID);
        doThrow(new RepositoryException()).when(cassandraRepository).createExperiment(testExp);

        //Make actual call
        try {
            expImpl.createExperiment(testExp, UserInfo.from(UserInfo.Username.valueOf("user")).build());
            fail("Expected RepositoryException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), RepositoryException.class);
        }

        //Verify execution
        verify(databaseRepository, times(1)).createExperiment(any(NewExperiment.class));
        verify(databaseRepository, times(1)).deleteExperiment(testExp);
        verify(eventLog, times(0)).postEvent(any(ExperimentCreateEvent.class));

    }

    @Test
    public void testCreateExperimentFailedDatabaseCreateExperiment() {
        NewExperiment testExp = NewExperiment.withID(experimentID)
                .withAppName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withDescription(description).build();
        //Mock interactions
        doNothing().when(validator).validateNewExperiment(testExp);
        doThrow(new RepositoryException()).when(databaseRepository).createExperiment(testExp);

        //Make actual call
        try {
            expImpl.createExperiment(testExp, UserInfo.from(UserInfo.Username.valueOf("user")).build());
            fail("Expected RepositoryException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), RepositoryException.class);
        }

        //Verify execution
        verify(cassandraRepository, times(0)).createExperiment(any(NewExperiment.class));
        verify(databaseRepository, times(0)).deleteExperiment(testExp);
        verify(eventLog, times(0)).postEvent(any(ExperimentCreateEvent.class));
    }


    @Test
    public void testCreateExperimentPass() {
        NewExperiment testExp = NewExperiment.withID(experimentID)
                .withAppName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withDescription(description).build();

        //Mock interactions
        doNothing().when(validator).validateNewExperiment(testExp);
        when(cassandraRepository.createExperiment(testExp)).thenReturn(experimentID);
        when(databaseRepository.createExperiment(testExp)).thenReturn(experimentID);

        //Make actual call
        expImpl.createExperiment(testExp, UserInfo.from(UserInfo.Username.valueOf("user")).build());

        //Verify execution
        verify(databaseRepository, times(1)).createExperiment(any(NewExperiment.class));
        verify(cassandraRepository, times(1)).createExperiment(any(NewExperiment.class));
        verify(databaseRepository, times(0)).deleteExperiment(testExp);
        verify(eventLog, times(1)).postEvent(any(ExperimentCreateEvent.class));
    }

    @Test
    public void testGetExperiment() throws Exception {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();

        // Test null case for single experiment
        when(cassandraRepository.getExperiment(experimentID)).thenReturn(null);
        Experiment experiment = expImpl.getExperiment(experimentID);
        assert experiment == null;

        experiment = expImpl.getExperiment(testApp, testLabel);
        assert experiment == null;

        // With ExperimentID
        when(cassandraRepository.getExperiment(experimentID)).thenReturn(testExp);
        experiment = expImpl.getExperiment(experimentID);
        assert experiment.getID() == experimentID;

        // With App Name and Label
        when(cassandraRepository.getExperiment(testApp, testLabel)).thenReturn(testExp);
        experiment = expImpl.getExperiment(testApp, testLabel);
        assert experiment.getID() == experimentID;

        List<Experiment> experiments = null;

        // Test null case for list of experiments
        when(cassandraRepository.getExperiments(testApp)).thenReturn(null);
        experiments = expImpl.getExperiments(testApp);
        assert experiments == null;

        when(cassandraRepository.getExperiments()).thenReturn(null);
        ExperimentList experimentIDs = expImpl.getExperiments();
        assert experimentIDs == null;
    }

    @Test
    public void testCheckStateTransition() {
        Experiment.State currentState = Experiment.State.RUNNING;
        Experiment.State desiredState = Experiment.State.RUNNING;
        verify(validator, never()).validateStateTransition(currentState, desiredState);
        expImpl.checkStateTransition(experimentID, currentState, desiredState);

        verify(validator, never()).validateStateTransition(currentState, desiredState);
        expImpl.checkStateTransition(experimentID, currentState, null);

        BucketList bucketList = mock(BucketList.class);
        when(buckets.getBuckets(experimentID, false)).thenReturn(bucketList);
        expImpl.checkStateTransition(experimentID, Experiment.State.DRAFT, Experiment.State.RUNNING);
        verify(validator, times(1)).validateStateTransition(Experiment.State.DRAFT, Experiment.State.RUNNING);
        verify(buckets, times(1)).getBuckets(experimentID, false);
        verify(validator, times(1)).validateExperimentBuckets(bucketList.getBuckets());

    }

    @Test
    public void testCheckForIllegalUpdate() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updateExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();

        // Attempt to modify experimentID
        updateExp.setID(Experiment.ID.newInstance());
        try {
            expImpl.checkForIllegalUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setID(experimentID);

        // Attempt to modify Creation Time
        Date tempCreationTime = updateExp.getCreationTime();
        updateExp.setCreationTime(new Date());
        try {
            expImpl.checkForIllegalUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setCreationTime(tempCreationTime);

        // Attempt to modify Modification Time
        updateExp.setModificationTime(new Date());
        try {
            expImpl.checkForIllegalUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
    }

    @Test
    public void testCheckForIllegalTerminatedUpdate() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withRule("rule1")
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updateExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withRule("rule2")
                .withEndTime(endTime).build();

        testExp.setState(Experiment.State.TERMINATED);

        // Attempt to change Application Name
        updateExp.setApplicationName(Application.Name.valueOf("ChangedAppName"));
        try {
            expImpl.checkForIllegalTerminatedUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setApplicationName(testApp);

        // Attempt to change label
        updateExp.setLabel(Experiment.Label.valueOf("ChangedLabel"));
        try {
            expImpl.checkForIllegalTerminatedUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setLabel(testLabel);

        // Attempt to change end time
        updateExp.setEndTime(new Date());
        try {
            expImpl.checkForIllegalTerminatedUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setEndTime(endTime);

        // Attempt to change sampling percent
        updateExp.setSamplingPercent(100.0);
        try {
            expImpl.checkForIllegalTerminatedUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setSamplingPercent(samplingPercent);

        // Attempt to check when Rules are different
        try {
            expImpl.checkForIllegalTerminatedUpdate(testExp, updateExp);
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
    }

    /**
     *
     */
    @Test
    public void testCheckForIllegalExperimentEndTime() {
        Experiment mockCurrentExperiment = mock(Experiment.class);
        Experiment mockUpdateExperiment = mock(Experiment.class);
        Date currentStartDate = mock(Date.class);
        Date updateStartDate = mock(Date.class);
        Date updateEndDate = mock(Date.class);
        //update.getEndTime().before(NOW)
        when(mockUpdateExperiment.getEndTime()).thenReturn(updateEndDate);
        when(updateEndDate.before(any(Date.class))).thenReturn(true);
        assertIllegalExperimentEndTime(mockCurrentExperiment, mockUpdateExperiment);
        when(updateEndDate.before(any(Date.class))).thenReturn(false);
        //current.getEndTime().before(NOW)
        Date currentEndDate = mock(Date.class);
        when(mockCurrentExperiment.getEndTime()).thenReturn(currentEndDate);
        when(currentEndDate.before(any(Date.class))).thenReturn(true);
        assertIllegalExperimentEndTime(mockCurrentExperiment, mockUpdateExperiment);
        when(currentEndDate.before(any(Date.class))).thenReturn(false);
        //update.getStartTime is null
        when(mockUpdateExperiment.getStartTime()).thenReturn(null);
        when(mockUpdateExperiment.getEndTime()).thenReturn(updateEndDate);
        when(mockCurrentExperiment.getStartTime()).thenReturn(currentStartDate);
        when(updateEndDate.before(currentStartDate)).thenReturn(true);
        assertIllegalExperimentEndTime(mockCurrentExperiment, mockUpdateExperiment);
        //update.getStartTime is not null
        when(mockUpdateExperiment.getStartTime()).thenReturn(updateStartDate);
        when(updateEndDate.before(updateStartDate)).thenReturn(true);
        assertIllegalExperimentEndTime(mockCurrentExperiment, mockUpdateExperiment);
    }

    void assertIllegalExperimentEndTime(Experiment mockCurrentExperiment, Experiment mockUpdateExperiment) {
        try {
            expImpl.checkForIllegalExperimentEndTime(mockCurrentExperiment, mockUpdateExperiment);
            fail();
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
    }

    @Test
    public void testCheckForIllegalExperimentStartTime() {
        Experiment mockCurrentExperiment = mock(Experiment.class);
        Experiment mockUpdateExperiment = mock(Experiment.class);
        Date updateEndDate = mock(Date.class);
        Date currentEndDate = mock(Date.class);
        Date updateStartDate = mock(Date.class);
        when(mockUpdateExperiment.getStartTime()).thenReturn(updateStartDate);
        when(updateStartDate.before(any(Date.class))).thenReturn(true);
        //update.getStartTime().before NOW
        assertIllegalExperimentStartTime(mockCurrentExperiment, mockUpdateExperiment);
        when(updateStartDate.before(any(Date.class))).thenReturn(false);
        //current.getStartTime().before NOW
        Date currentStartDate = mock(Date.class);
        when(mockCurrentExperiment.getStartTime()).thenReturn(currentStartDate);
        when(currentStartDate.before(any(Date.class))).thenReturn(true);
        assertIllegalExperimentStartTime(mockCurrentExperiment, mockUpdateExperiment);
        when(currentStartDate.before(any(Date.class))).thenReturn(false);
        //update.getEndTime is null
        when(mockUpdateExperiment.getEndTime()).thenReturn(null);
        when(mockCurrentExperiment.getEndTime()).thenReturn(currentEndDate);
        when(updateStartDate.after(currentEndDate)).thenReturn(true);
        assertIllegalExperimentStartTime(mockCurrentExperiment, mockUpdateExperiment);
        when(currentStartDate.before(any(Date.class))).thenReturn(false);
        //update.getEndTime is not null
        when(mockUpdateExperiment.getEndTime()).thenReturn(updateEndDate);
        when(updateStartDate.after(updateEndDate)).thenReturn(true);
        assertIllegalExperimentStartTime(mockCurrentExperiment, mockUpdateExperiment);
    }

    void assertIllegalExperimentStartTime(Experiment mockCurrentExperiment, Experiment mockUpdateExperiment) {
        try {
            expImpl.checkForIllegalExperimentStartTime(mockCurrentExperiment, mockUpdateExperiment);
            fail();
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
    }

    @Test
    public void testCheckForIllegalPausedUpdate() {
        ExperimentsImpl expImpl = spy(new ExperimentsImpl(databaseRepository, cassandraRepository, experiments,
                buckets, pages, priorities, validator, ruleCache, eventLog));

        Experiment mockCurrentExperiment = mock(Experiment.class);
        Experiment mockUpdateExperiment = mock(Experiment.class);
        doNothing().when(expImpl).checkForIllegalExperimentStartTime(mockCurrentExperiment, mockUpdateExperiment);
        doNothing().when(expImpl).checkForIllegalExperimentEndTime(mockCurrentExperiment, mockUpdateExperiment);
        when(mockCurrentExperiment.getState()).thenReturn(Experiment.State.RUNNING);
        //Test the Application Name different
        when(mockCurrentExperiment.getApplicationName()).thenReturn(Application.Name.valueOf("A"));
        when(mockUpdateExperiment.getApplicationName()).thenReturn(Application.Name.valueOf("B"));
        try {
            expImpl.checkForIllegalPausedRunningUpdate(mockCurrentExperiment, mockUpdateExperiment);
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            when(mockUpdateExperiment.getApplicationName()).thenReturn(Application.Name.valueOf("A"));
        }
        //Test the Label difference
        when(mockCurrentExperiment.getLabel()).thenReturn(Experiment.Label.valueOf("A"));
        when(mockUpdateExperiment.getLabel()).thenReturn(Experiment.Label.valueOf("B"));
        try {
            expImpl.checkForIllegalPausedRunningUpdate(mockCurrentExperiment, mockUpdateExperiment);
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            when(mockUpdateExperiment.getLabel()).thenReturn(Experiment.Label.valueOf("A"));
        }
        //Test the start time difference
        Date updateStartDate = mock(Date.class);
        Date currentStartDate = mock(Date.class);
        when(mockCurrentExperiment.getStartTime()).thenReturn(currentStartDate);
        when(mockUpdateExperiment.getStartTime()).thenReturn(updateStartDate);
        expImpl.checkForIllegalPausedRunningUpdate(mockCurrentExperiment, mockUpdateExperiment);
        verify(expImpl, times(1)).checkForIllegalExperimentStartTime(mockCurrentExperiment, mockUpdateExperiment);
        //Test the end time different
        when(mockUpdateExperiment.getStartTime()).thenReturn(currentStartDate);
        when(mockUpdateExperiment.getEndTime()).thenReturn(updateStartDate);
        when(mockCurrentExperiment.getEndTime()).thenReturn(currentStartDate);
        expImpl.checkForIllegalPausedRunningUpdate(mockCurrentExperiment, mockUpdateExperiment);
        verify(expImpl, times(1)).checkForIllegalExperimentEndTime(mockCurrentExperiment, mockUpdateExperiment);

    }

    // FIXME:
//    @Ignore("FIXME")
//    @Test
//    public void testUpdateSegmentationRule() {
//        Experiment testExp = Experiment.withID(experimentID)
//                .withApplicationName(testApp)
//                .withLabel(testLabel)
//                .withSamplingPercent(samplingPercent)
//                .withStartTime(startTime)
//                .withEndTime(endTime).build();
//        when(ruleCache.getRule(experimentID)).thenReturn(null);
//        expImpl.updateSegmentationRule(testExp, UserInfo.from(UserInfo.Username.valueOf("userinfo")).build());
//        verify(ruleCache, atLeastOnce()).clearRule(experimentID);
//
//        testExp.setRule("(mock == true)");
//        com.intuit.hyrule.Rule mockRule = mock(com.intuit.hyrule.Rule.class);
//        when(ruleCache.getRule(experimentID)).thenReturn(mockRule);
//        expImpl.updateSegmentationRule(testExp, UserInfo.from(UserInfo.Username.valueOf("userinfo")).build());
//        verify(ruleCache, atLeastOnce()).setRule(eq(experimentID), any(com.intuit.hyrule.Rule.class));
//    }

    @Test(expected = ExperimentNotFoundException.class)
    public void testUpdateExperimentNull() {
        Experiment updateExp = null;
        expImpl.updateExperiment(experimentID, updateExp, UserInfo.from(UserInfo.Username.valueOf("user")).build());
    }

    @Test
    public void testBuildUpdatedExperiment() {
        Experiment current = mock(Experiment.class);
        Experiment update = mock(Experiment.class);
        Experiment.Builder builder = mock(Experiment.Builder.class);
        List<Experiment.ExperimentAuditInfo> changeList = mock(List.class);
        boolean requiresUpdate = expImpl.buildUpdatedExperiment(current, update, builder, changeList);
        assertThat(requiresUpdate, is(false));

        when(current.getState()).thenReturn(Experiment.State.DRAFT);
        when(update.getState()).thenReturn(Experiment.State.RUNNING);

        when(current.getDescription()).thenReturn("A");
        when(update.getDescription()).thenReturn("B");

        when(current.getSamplingPercent()).thenReturn(1.0);
        when(update.getSamplingPercent()).thenReturn(2.0);

        when(current.getStartTime()).thenReturn(new Date(1));
        when(update.getStartTime()).thenReturn(new Date(2));

        when(current.getEndTime()).thenReturn(new Date(100));
        when(update.getEndTime()).thenReturn(new Date(200));

        when(current.getIsPersonalizationEnabled()).thenReturn(false);
        when(update.getIsPersonalizationEnabled()).thenReturn(true);

        when(current.getModelName()).thenReturn("A");
        when(update.getModelName()).thenReturn("B");
        when(current.getModelVersion()).thenReturn("V1");
        when(update.getModelVersion()).thenReturn("V2");

        when(current.getIsRapidExperiment()).thenReturn(false);
        when(update.getIsRapidExperiment()).thenReturn(true);

        when(current.getUserCap()).thenReturn(1);
        when(update.getUserCap()).thenReturn(2);

        when(current.getRule()).thenReturn("Rule-1");
        when(update.getRule()).thenReturn("Rule-2");

        when(current.getLabel()).thenReturn(Experiment.Label.valueOf("Lable-1"));
        when(update.getLabel()).thenReturn(Experiment.Label.valueOf("Label-2"));

        when(current.getApplicationName()).thenReturn(Application.Name.valueOf("App1"));
        when(update.getApplicationName()).thenReturn(Application.Name.valueOf("App2"));

        requiresUpdate = expImpl.buildUpdatedExperiment(current, update, builder, changeList);
        assertThat(requiresUpdate, is(true));
        verify(builder, times(1)).withState(Experiment.State.RUNNING);
        verify(builder, times(1)).withDescription("B");
        verify(builder, times(1)).withSamplingPercent(2.0);
        verify(builder, times(1)).withStartTime(new Date(2));
        verify(builder, times(1)).withEndTime(new Date(200));
        verify(builder, times(1)).withIsPersonalizationEnabled(true);
        verify(builder, times(1)).withModelName("B");
        verify(builder, times(1)).withModelVersion("V2");
        verify(builder, times(1)).withIsRapidExperiment(true);
        verify(builder, times(1)).withUserCap(2);
        verify(builder, times(1)).withRule("Rule-2");
        verify(builder, times(1)).withLabel(Experiment.Label.valueOf("Label-2")); //change to this does not trigger experiment audit info
        verify(builder, times(1)).withApplicationName(Application.Name.valueOf("App2")); //change to this does not trigger experiment audit info
        verify(changeList, times(11)).add(any(Experiment.ExperimentAuditInfo.class));

    }

    @Test
    public void testUpdateExperiment() {
        Experiment update = mock(Experiment.class);
        Experiment current = mock(Experiment.class);
        UserInfo user = mock(UserInfo.class);
        ExperimentsImpl expImpl = spy(new ExperimentsImpl(databaseRepository, cassandraRepository, experiments,
                buckets, pages, priorities, validator, ruleCache, eventLog));
        when(current.getID()).thenReturn(experimentID);
        doReturn(current).when(expImpl).getExperiment(experimentID);
        doReturn(false).when(expImpl).buildUpdatedExperiment(eq(current), eq(update),
                any(Experiment.Builder.class), any(List.class));
        Experiment result = expImpl.updateExperiment(experimentID, update, user);
        verify(experiments, times(1)).checkStateTransition(any(Experiment.ID.class), any(Experiment.State.class),
                any(Experiment.State.class));
        verify(experiments, times(1)).checkForIllegalUpdate(any(Experiment.class), any(Experiment.class));
        verify(experiments, times(1)).checkForIllegalTerminatedUpdate(any(Experiment.class), any(Experiment.class));
        verify(experiments, times(1)).checkForIllegalPausedRunningUpdate(any(Experiment.class), any(Experiment.class));

        assertThat(result, is(current));
        Experiment concretExperiment = spy(Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withRule("Rule-1")
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build());
        when(update.getState()).thenReturn(Experiment.State.DELETED);
        when(cassandraRepository.updateExperiment(any(Experiment.class))).thenReturn(concretExperiment);
        doReturn(concretExperiment).when(expImpl).getExperiment(experimentID);
        doReturn(true).when(expImpl).buildUpdatedExperiment(eq(concretExperiment), eq(update),
                any(Experiment.Builder.class), any(List.class));
        doReturn(Application.Name.valueOf("mock")).when(concretExperiment).getApplicationName();
        doReturn("mockedRules").when(concretExperiment).getRule();
        doReturn(Experiment.State.RUNNING).when(concretExperiment).getState();
        result = expImpl.updateExperiment(experimentID, update, user);
        assertThat(result.getState(), is(Experiment.State.DELETED));
        verify(validator, times(1)).validateExperiment(any(Experiment.class));
        verify(databaseRepository, times(1)).updateExperiment(any(Experiment.class));
        verify(priorities, times(1)).appendToPriorityList(any(Experiment.ID.class));
        verify(cassandraRepository, times(1)).logExperimentChanges(any(Experiment.ID.class), any(List.class));
        verify(priorities, times(2)).removeFromPriorityList(any(Application.Name.class), any(Experiment.ID.class));
        verify(pages, times(1)).erasePageData(any(Application.Name.class), any(Experiment.ID.class), any(UserInfo.class));
    }

}
