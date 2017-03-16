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
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.impl.CassandraMutexRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void testGetRecursiveMutualExclusions() {
        List<Experiment> mutexExperiments = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            mutexExperiments.add(Experiment.withID(Experiment.ID.newInstance()).build());

            Mockito.doReturn(mutexExperiments.get(i)).when(experiments).getExperiment(mutexExperiments.get(i).getID());
        }
        int number = mutexExperiments.size();

        // Set up the exclusions as follows:
        //   0, 1, 2    are A1, A2, A3
        //   3, 4       are B1, B2
        //   5, 6, 7, 8 are C1, C2, C3, C4
        // A1, A2, and A3 are mutex to each other.
        // A1, and B1 are mutex.
        // B1, and B2 are mutex.
        // C1, C2, C3, C4 are each mutex to their "neighbors", i.e. C1 is mutex to C2 and C4, C2 is mutex to C1 and C3, ...
        // This setup results in experiments 0-5 being mutex, as well as 5-9.

        List<ExperimentList> exclusions = new ArrayList<>(number);
        for (int i = 0; i < number; ++i) {
            exclusions.add(new ExperimentList());
            List<Experiment> exclusionList = exclusions.get(i).getExperiments();
            switch (i) {
                case 0:
                    exclusionList.add(mutexExperiments.get(3)); // A1 -> B1
                case 1:
                case 2:
                    exclusionList.addAll(mutexExperiments.subList(0, 3)); // A1/2/3
                    break;
                case 3:
                    exclusionList.add(mutexExperiments.get(0)); // B1 -> A1
                case 4:
                    exclusionList.addAll(mutexExperiments.subList(3, 5)); // B1/2
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                    exclusionList.addAll(mutexExperiments.subList(5, 9)); // C1/2/3/4
                    exclusionList.remove(mutexExperiments.get((i - 3) % 4 + 5)); // remove non-neighbor
                    break;
                default:
                    throw new IllegalArgumentException(String.format("The unit test seems to be set up improperly. " +
                            "Not all experiments are handled in the switch case (i was %d)", i));
            }
            exclusionList.remove(mutexExperiments.get(i)); // remove experiment itself

            Mockito.doReturn(exclusions.get(i)).when(mutexRepository).getExclusions(mutexExperiments.get(i).getID());
        }

        for (int i = 0; i < number; ++i) {
            List<Experiment> actual = resource.getRecursiveMutualExclusions(mutexExperiments.get(i));
            List<Experiment> expected = Collections.emptyList();
            if (i < 5) {
                expected = mutexExperiments.subList(0, 5);
                Assert.assertEquals(String.format("Setup failed for i = %d", i), 5, expected.size());
            } else if (i < 9) {
                expected = mutexExperiments.subList(5, 9);
                Assert.assertEquals(String.format("Setup failed for i = %d", i), 4, expected.size());
            }
            Assert.assertEquals("Sizes do not match.", expected.size(), actual.size());
            Assert.assertTrue(String.format("Result for i = %d is incomplete.", i),
                    actual.containsAll(expected));
        }
    }
}
