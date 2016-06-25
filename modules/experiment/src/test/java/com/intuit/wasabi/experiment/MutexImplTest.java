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

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.EndTimeHasPassedException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.impl.ExperimentsImpl;
import com.intuit.wasabi.experiment.impl.MutexImpl;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentIDList;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.CassandraMutexRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MutexImplTest {
    private final static Application.Name testApp = Application.Name.valueOf("testApp");
    private CassandraMutexRepository mutexRepository = mock(CassandraMutexRepository.class);
    private ExperimentsImpl experiments = mock(ExperimentsImpl.class);
    private EventLog eventLog = mock(EventLog.class);
    MutexImpl resource = new MutexImpl(mutexRepository, experiments, eventLog);

    @Test
    public void testGetExclusions() {
        Experiment.ID experimentID = Experiment.ID.newInstance();
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .build();
        experiment.setID(null);
        when(experiments.getExperiment(experimentID)).thenReturn(experiment);
        try {
            resource.getExclusions(experimentID);
            fail();
        } catch (ExperimentNotFoundException e) {
            // Expecting this exception so do nothing
        }
    }

    @Test
    public void testGetNotExclusions() {
        try {
            resource.getNotExclusions(null);
            fail();
        } catch (ExperimentNotFoundException e) {
            // Expecting this exception so do nothing
        }
    }

    @Test
    public void testDeleteExclusion() {
        Experiment.ID experimentID1 = Experiment.ID.newInstance();
        Experiment experiment_1 = Experiment.withID(experimentID1)
                .withApplicationName(testApp)
                .build();
        Experiment.ID experimentID2 = Experiment.ID.newInstance();
        Experiment experiment_2 = Experiment.withID(experimentID2)
                .withApplicationName(testApp)
                .build();

        UserInfo changeUser = UserInfo.from(UserInfo.Username.valueOf("user")).build();

        // Experiment 1 is null
        when(experiments.getExperiment(experimentID1)).thenReturn(null);
        when(experiments.getExperiment(experimentID2)).thenReturn(experiment_2);
        try {
            resource.deleteExclusion(experimentID1, experimentID2, changeUser);
            fail();
        } catch (ExperimentNotFoundException e) {
            // Expecting this exception so do nothing
        }

        // Experiment 1 and Experiment 2 are null
        when(experiments.getExperiment(experimentID1)).thenReturn(null);
        when(experiments.getExperiment(experimentID2)).thenReturn(null);
        try {
            resource.deleteExclusion(experimentID1, experimentID2, changeUser);
            fail();
        } catch (ExperimentNotFoundException e) {
            // Expecting this exception so do nothing
        }

        // Experiment 2 is null
        when(experiments.getExperiment(experimentID1)).thenReturn(experiment_1);
        when(experiments.getExperiment(experimentID2)).thenReturn(null);
        try {
            resource.deleteExclusion(experimentID1, experimentID2, changeUser);
            fail();
        } catch (ExperimentNotFoundException e) {
            // Expecting this exception so do nothing
        }
    }

    @Test
    public void testCreateExclusions() {

        Experiment.ID experimentID1 = Experiment.ID.newInstance();
        Experiment experiment_1 = Experiment.withID(experimentID1)
                .withApplicationName(testApp)
                .build();
        Experiment.ID experimentID2 = Experiment.ID.newInstance();
        Experiment experiment_2 = Experiment.withID(experimentID2)
                .withApplicationName(testApp)
                .build();

        UserInfo changeUser = UserInfo.from(UserInfo.Username.valueOf("user")).build();

        List<Experiment.ID> listIDs = new ArrayList<>();
        listIDs.add(experimentID2);
        ExperimentIDList experimentIDList = ExperimentIDList.newInstance().withExperimentIDs(listIDs).build();

        // Base experiment is null
        when(experiments.getExperiment(experimentID1)).thenReturn(null);
        try {
            resource.createExclusions(experimentID1, experimentIDList, changeUser);
            fail();
        } catch (ExperimentNotFoundException e) {
            // Expecting this exception so do nothing
        }

        // Base experiment is terminated
        experiment_1.setState(Experiment.State.TERMINATED);
        when(experiments.getExperiment(experimentID1)).thenReturn(experiment_1);
        try {
            resource.createExclusions(experimentID1, experimentIDList, changeUser);
            fail();
        } catch (InvalidExperimentStateException e) {
            // Expecting this exception so do nothing
        }

        // Base experiment is deleted
        experiment_1.setState(Experiment.State.DELETED);
        when(experiments.getExperiment(experimentID1)).thenReturn(experiment_1);
        try {
            resource.createExclusions(experimentID1, experimentIDList, changeUser);
            fail();
        } catch (InvalidExperimentStateException e) {
            // Expecting this exception so do nothing
        }

        // Base experiment end time has passed
        experiment_1.setState(Experiment.State.RUNNING);
        experiment_1.setEndTime(new Timestamp(System.currentTimeMillis() - 1000000));
        when(experiments.getExperiment(experimentID1)).thenReturn(experiment_1);
        try {
            resource.createExclusions(experimentID1, experimentIDList, changeUser);
            fail();
        } catch (EndTimeHasPassedException e) {
            // Expecting this exception so do nothing
        }

        // When pair experiment is not found
        experiment_1.setEndTime(new Timestamp(System.currentTimeMillis()));
        when(experiments.getExperiment(experimentID2)).thenReturn(null);
        List<Map> result = resource.createExclusions(experimentID1, experimentIDList, changeUser);
        assert result.get(0).containsValue("Experiment2 not found.");

        // When experiments are not in the same application
        experiment_2.setApplicationName(Application.Name.valueOf("testApp2"));
        when(experiments.getExperiment(experimentID2)).thenReturn(experiment_2);
        result = resource.createExclusions(experimentID1, experimentIDList, changeUser);
        assert result.get(0).containsValue("Experiments 1 and 2 are not in the same application. " +
                "Mutual exclusion rules can only be defined for experiments within the same application.");

        // When pair experiment is in deleted state
        experiment_2.setApplicationName(testApp);
        experiment_2.setState(Experiment.State.DELETED);
        when(experiments.getExperiment(experimentID2)).thenReturn(experiment_2);
        result = resource.createExclusions(experimentID1, experimentIDList, changeUser);
        assert result.get(0).containsValue("Experiment2 is in TERMINATED or DELETED state");

        // When pair experiment has invalid endtime
        experiment_2.setState(Experiment.State.RUNNING);
        experiment_2.setEndTime(new Timestamp(System.currentTimeMillis() - 1000000));
        when(experiments.getExperiment(experimentID2)).thenReturn(experiment_2);
        result = resource.createExclusions(experimentID1, experimentIDList, changeUser);
        assert result.get(0).containsValue("End time has passed for experiment2");

        // Throw Repository exception
        experiment_2.setEndTime(new Timestamp(System.currentTimeMillis()));
        doThrow(new RepositoryException()).when(mutexRepository).createExclusion(experimentID1, experimentID2);
        result = resource.createExclusions(experimentID1, experimentIDList, null);
        assert result.get(0).containsValue("Repository exception");

        // Happy path
        doNothing().when(mutexRepository).createExclusion(experimentID1, experimentID2);
        result = resource.createExclusions(experimentID1, experimentIDList, changeUser);
        assert result.get(0).containsValue("SUCCESS");
    }
}
