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
package com.intuit.wasabi.experiment;

import com.googlecode.catchexception.apis.CatchExceptionBdd;
import com.intuit.wasabi.assignmentobjects.RuleCache;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.impl.ExperimentsImpl;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ExperimentAuditInfo;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.experimentobjects.Experiment.State;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @Mock
    private Experiment.Builder builder;

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
        expImpl.createExperiment(testExp, UserInfo.from(UserInfo.Username.valueOf("user")).build());
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
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updateExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
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
    }

    @Test
    public void testCheckForIllegalPausedUpdate() {
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

        testExp.setState(Experiment.State.RUNNING);

        // Attempt to change Application Name
        updateExp.setApplicationName(Application.Name.valueOf("ChangedAppName"));
        try {
            expImpl.checkForIllegalPausedRunningUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setApplicationName(testApp);

        // Attempt to change label
        updateExp.setLabel(Experiment.Label.valueOf("ChangedLabel"));
        try {
            expImpl.checkForIllegalPausedRunningUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setLabel(testLabel);

        // Attempt to change start time to a value in the past
        updateExp.setStartTime(new Date(startTime.getTime() - 1000));
        try {
            expImpl.checkForIllegalPausedRunningUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setStartTime(startTime);

        // Attempt to change end time to a value older than start time
        updateExp.setEndTime(new Date(startTime.getTime() - 1000));
        try {
            expImpl.checkForIllegalPausedRunningUpdate(testExp, updateExp);
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        updateExp.setEndTime(endTime);
    }

    @Test
    public void testUpdateSegmentationRule() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        when(ruleCache.getRule(experimentID)).thenReturn(null);
        expImpl.updateSegmentationRule(testExp, UserInfo.from(UserInfo.Username.valueOf("user")).build());
        verify(ruleCache, atLeastOnce()).clearRule(experimentID);
    }

    @Test(expected = ExperimentNotFoundException.class)
    public void testUpdateExperimentNull() {
        Experiment updateExp = null;
        expImpl.updateExperiment(experimentID, updateExp, UserInfo.from(UserInfo.Username.valueOf("user")).build());
    }

    @Test
    public void testBuildUpdateExperimentState() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DRAFT)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withState(State.DRAFT)).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
        verify(builder).withState(State.DRAFT);
    }

    @Test
    public void testBuildUpdateExperimentDescriptionAndState() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withDescription("abcd")
                .withState(State.DRAFT)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withState(State.DRAFT)).willReturn(builder);
        given(builder.withDescription("abcd")).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(2);
        then(result).isEqualTo(true);
        verify(builder).withState(State.DRAFT);
        verify(builder).withDescription("abcd");
    }

    @Test
    public void testBuildUpdateExperimentStateTime() {
        Date startTime2 = new Date(1, 1, 1);
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime2)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withStartTime(startTime2)).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
        verify(builder).withStartTime(startTime2);
    }

    @Test
    public void testBuildUpdateExperimentLabel() {
        Label l1 = Label.valueOf("l1");
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(l1)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withLabel(l1)).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(0);
        then(result).isEqualTo(true);
    }


    @Test
    public void testBuildUpdateExperimentSamplingPercentage() {
        Double sp = samplingPercent - 0.01;
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(sp)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withSamplingPercent(sp)).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
    }

    @Test
    public void testBuildUpdateExperimentUserCap() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withUserCap(5)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withUserCap(6)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withUserCap(6)).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
    }

    @Test
    public void testBuildUpdateExperimentApplicationName() {
        Application.Name name = Application.Name.valueOf("a1");
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(name)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withApplicationName(name)).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(0);
        then(result).isEqualTo(true);
    }

    @Test
    public void testBuildUpdateExperimentModelName() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withModelName("m1")
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withModelName("m2")
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withModelName("m2")).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
    }

    @Test
    public void testBuildUpdateExperimentModelVersion() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withModelVersion("v1")
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withModelVersion("v2")
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withModelVersion("v2")).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
    }

    @Test
    public void testBuildUpdateExperimentEndTime() {
        Date endTime2 = new Date();
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updatedExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime2).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();
        given(builder.withEndTime(endTime2)).willReturn(builder);

        boolean result = expImpl.buildUpdatedExperiment(testExp, updatedExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
        verify(builder).withEndTime(endTime2);
    }

    @Test
    public void testBuildUpdateExperimentNoChange() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();

        boolean result = expImpl.buildUpdatedExperiment(testExp, testExp, builder, changeList);
        then(changeList.size()).isEqualTo(0);
        then(result).isEqualTo(false);
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void testcheckForIllegalTerminatedUpdateStartTimeNotSame() {
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.TERMINATED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updates = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.TERMINATED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(new Date(1, 1, 1))
                .withEndTime(endTime).build();

        CatchExceptionBdd.when(expImpl).checkForIllegalTerminatedUpdate(experiment, updates);
        CatchExceptionBdd.thenThrown(IllegalArgumentException.class);
    }

    @Test
    public void testcheckForIllegalTerminatedUpdateRuleNotSame() {
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.TERMINATED)
                .withLabel(testLabel)
                .withRule("r1")
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updates = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.TERMINATED)
                .withLabel(testLabel)
                .withRule("r2")
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();

        CatchExceptionBdd.when(expImpl).checkForIllegalTerminatedUpdate(experiment, updates);
        CatchExceptionBdd.thenThrown(IllegalArgumentException.class);
    }

    @Test
    public void testBuildUpdateExperimentRule() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withRule("r1")
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment upExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withRule("r2")
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        List<ExperimentAuditInfo> changeList = new ArrayList<>();

        boolean result = expImpl.buildUpdatedExperiment(testExp, upExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
    }

    @Test
    public void testcheckForIllegalPausedUpdateStartTimeBeforeNow() {
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.PAUSED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(new Date(1, 1, 1))
                .withEndTime(endTime).build();
        Experiment updates = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.PAUSED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(new Date(startTime.getTime() + 6000))
                .withEndTime(endTime).build();

        CatchExceptionBdd.when(expImpl).checkForIllegalPausedRunningUpdate(experiment, updates);
        CatchExceptionBdd.thenThrown(IllegalArgumentException.class);
    }

    @Test
    public void testcheckForIllegalPausedUpdateEndDateBeforeStart() {
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.PAUSED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment updates = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.PAUSED)
                .withLabel(testLabel)
                .withSamplingPercent(samplingPercent)
                .withStartTime(new Date(2222, 1, 2))
                .withEndTime(endTime).build();
        CatchExceptionBdd.when(expImpl).checkForIllegalPausedRunningUpdate(experiment, updates);
        CatchExceptionBdd.thenThrown(IllegalArgumentException.class);
    }

    @Test
    public void testBuildUpdateExperimentIsPersonlization() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withIsPersonalizationEnabled(true)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment upExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withIsPersonalizationEnabled(false)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();

        List<ExperimentAuditInfo> changeList = new ArrayList<>();

        boolean result = expImpl.buildUpdatedExperiment(testExp, upExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
    }

    @Test
    public void testBuildUpdateExperimentIsRapid() {
        Experiment testExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withIsRapidExperiment(true)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();
        Experiment upExp = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(State.DELETED)
                .withLabel(testLabel)
                .withIsRapidExperiment(false)
                .withSamplingPercent(samplingPercent)
                .withStartTime(startTime)
                .withEndTime(endTime).build();

        List<ExperimentAuditInfo> changeList = new ArrayList<>();

        boolean result = expImpl.buildUpdatedExperiment(testExp, upExp, builder, changeList);
        then(changeList.size()).isEqualTo(1);
        then(result).isEqualTo(true);
    }

    @Test
    public void testGetTags() {
        when(cassandraRepository.getTagListForApplications(null)).thenReturn(null);
        expImpl.getTagsForApplications(null);
        verify(cassandraRepository).getTagListForApplications(Collections.emptySet());
    }
}
