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

import com.intuit.wasabi.exceptions.ApplicationNotFoundException;
import com.intuit.wasabi.experiment.impl.PrioritiesImpl;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentIDList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.PrioritiesRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PrioritiesImplTest {

    private final static Application.Name testApp = Application.Name.valueOf("testApp");
    protected PrioritiesImpl prioritiesImpl;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private PrioritiesRepository prioritiesRepository;
    @Mock
    private Experiments experiments;
    @Mock
    private ExperimentValidator validator;

    @Before
    public void setUp() {
        prioritiesImpl = new PrioritiesImpl(prioritiesRepository, experiments);
    }

    @Test
    public void getPriorities_test() throws Exception {

        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp).build();
        experiment.setState(Experiment.State.DRAFT);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        List<Experiment> experimentList = new ArrayList<>();
        experimentList.add(experiment);
        List<Experiment.ID> priorityList = new ArrayList<>();
        priorityList.add(experiment.getID());
        PrioritizedExperiment prioritizedExperiment = PrioritizedExperiment.from(experiment, 0).build();
        PrioritizedExperimentList prioritizedExperimentList = new PrioritizedExperimentList();
        prioritizedExperimentList.addPrioritizedExperiment(prioritizedExperiment);

        when(experiments.getExperiments(testApp)).thenReturn(experimentList);
        when(prioritiesRepository.getPriorityList(testApp)).thenReturn(priorityList);
        when(prioritiesRepository.getPriorities(testApp)).thenReturn(prioritizedExperimentList);
        prioritiesImpl.getPriorities(testApp, true);
        // verify that the control doesnt enter into the createPriorities part
        verify(prioritiesRepository, never()).createPriorities(testApp, priorityList);

        // Creating a badPriorityList
        List<Experiment.ID> badPriorityList = new ArrayList<>();
        badPriorityList.add(experiment.getID()); // This makes it equal to the current priorityList

        // Adding a DELETED experiment
        Experiment deletedExperiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .build();
        deletedExperiment.setState(Experiment.State.DELETED);
        badPriorityList.add(deletedExperiment.getID());
        when(experiments.getExperiment(deletedExperiment.getID())).thenReturn(null);

        // Adding a TERMINATED experiment
        Experiment terminatedExperiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .build();
        terminatedExperiment.setState(Experiment.State.TERMINATED);
        badPriorityList.add(terminatedExperiment.getID());
        when(experiments.getExperiment(terminatedExperiment.getID())).thenReturn(terminatedExperiment);

        //Adding an experiment which belongs to a different application
        Experiment differentApplicationExperiment = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(Application.Name
                        .valueOf("differentApplication"))
                .build();
        differentApplicationExperiment.setState(Experiment.State.DRAFT);
        badPriorityList.add(differentApplicationExperiment.getID());
        when(experiments.getExperiment(differentApplicationExperiment.getID()))
                .thenReturn(differentApplicationExperiment);

        // Adding a duplicate valid experiment
        badPriorityList.add(experiment.getID());

        // Adding an experiment to the experiments list that should be in the priorityList
        Experiment missingExperiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .build();
        experiment.setState(Experiment.State.DRAFT);
        experimentList.add(missingExperiment);
        when(experiments.getExperiment(missingExperiment.getID())).thenReturn(missingExperiment);


        when(prioritiesRepository.getPriorityList(testApp)).thenReturn(badPriorityList);
        prioritiesImpl.getPriorities(testApp, true);
        priorityList.add(missingExperiment.getID()); // build the correct priorityList
        // Verify that the control gets into the createPriorities part with the corrected priorityList
        verify(prioritiesRepository, times(1)).createPriorities(testApp, priorityList);
    }

    @Test
    public void createPriorities_test() {

        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp).build();
        experiment.setState(Experiment.State.DRAFT);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        Experiment deletedExperiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .build();
        deletedExperiment.setState(Experiment.State.DELETED);
        when(experiments.getExperiment(deletedExperiment.getID())).thenReturn(null);

        List<Experiment> experimentsList = new ArrayList<>();
        experimentsList.add(experiment);
        when(experiments.getExperiments(testApp)).thenReturn(experimentsList);


        List<Experiment.ID> experimentIDs = new ArrayList<>();
        experimentIDs.add(experiment.getID());
        experimentIDs.add(deletedExperiment.getID());
        ExperimentIDList experimentIDList = ExperimentIDList.newInstance().withExperimentIDs(experimentIDs).build();

        List<Experiment.ID> priorityList = new ArrayList<>();
        priorityList.add(experiment.getID());

        // Verify that null applicationName delivers an ApplicationNotFoundException exception
        verifyException(prioritiesImpl, ApplicationNotFoundException.class)
                .createPriorities(null, experimentIDList, true);

        prioritiesImpl.createPriorities(testApp, experimentIDList, false);
        // Verify that the createPriorities method is invoked with the given experimentIDList
        // when verifyPriorityList is false
        verify(prioritiesRepository, times(1)).createPriorities(testApp, experimentIDList.getExperimentIDs());

        prioritiesImpl.createPriorities(testApp, experimentIDList, true);
        // Verify that the createPriorities method is invoked with the corrected priorityList
        // when verifyPriorityList is true
        verify(prioritiesRepository, times(1)).createPriorities(testApp, priorityList);
    }

    @Test
    public void cleanPriorityList_test() {
        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withState(Experiment.State.DRAFT).build();
        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(Application.Name.valueOf("newApp"))
                .withState(Experiment.State.DRAFT).build();
        Experiment experiment3 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withState(Experiment.State.DRAFT).build();

        List<Experiment.ID> experimentIDs = new ArrayList<>();
        experimentIDs.add(experiment.getID());
        experimentIDs.add(experiment2.getID());

        List<Experiment> experimentList = new ArrayList<>();
        experimentList.add(experiment3);

        when(experiments.getExperiments(testApp)).thenReturn(experimentList);
        List<Experiment.ID> cleanPriorityList = prioritiesImpl.cleanPriorityList(testApp, experimentIDs);
        assert cleanPriorityList.size() == 1;
        assert cleanPriorityList.get(0).getRawID() == experiment3.getID().getRawID();

        // Not adding ID of experiment3 to the priorityList
        experimentList.add(experiment);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        when(experiments.getExperiment(experiment2.getID())).thenReturn(experiment2);
        cleanPriorityList = prioritiesImpl.cleanPriorityList(testApp, experimentIDs);
        assert cleanPriorityList.size() == 2;
        assert cleanPriorityList.get(0) == experiment.getID();
        assert cleanPriorityList.get(1) == experiment3.getID();

        // Adding a duplicate experiment ID to the priorityList
        experimentIDs.add(experiment.getID());
        cleanPriorityList = prioritiesImpl.cleanPriorityList(testApp, experimentIDs);
        assert cleanPriorityList.size() == 2;
        assert cleanPriorityList.get(0) == experiment.getID();
        assert cleanPriorityList.get(1) == experiment3.getID();

        // Adding an experiment with a Terminated state to the priorityList
        Experiment terminatedExperiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withState(Experiment.State.TERMINATED).build();
        experimentIDs.add(terminatedExperiment.getID());
        cleanPriorityList = prioritiesImpl.cleanPriorityList(testApp, experimentIDs);
        assert cleanPriorityList.size() == 2;
        assert cleanPriorityList.get(0) == experiment.getID();
        assert cleanPriorityList.get(1) == experiment3.getID();
    }

    @Test
    public void setPriority_for_Experiment() {

        Experiment experiment = mock(Experiment.class);

        when(prioritiesRepository.getPriorityList(testApp)).thenReturn(null);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        when(experiment.getApplicationName()).thenReturn(testApp);
        when(experiment.getState()).thenReturn(Experiment.State.TERMINATED);
        when(experiment.getID()).thenReturn(Experiment.ID.newInstance());

        prioritiesImpl.setPriority(experiment.getID(), 42);

        //verify that he never got to the point where the priority is set
        verify(prioritiesRepository, never()).createPriorities(any(Application.Name.class), anyList());

        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        //verify that he never got to the point where the priority is set
        verify(prioritiesRepository, never()).createPriorities(any(Application.Name.class), anyList());
    }

    @Test
    public void setPriorityForExperimentWithPrioritySame() {

        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withState(Experiment.State.RUNNING).build();
        List<Experiment.ID> list = new ArrayList<>();
        list.add(experiment.getID());
        when(prioritiesRepository.getPriorityList(testApp)).thenReturn(list);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        prioritiesImpl.setPriority(experiment.getID(), 1);

        verify(prioritiesRepository, never()).createPriorities(any(Application.Name.class), anyList());
    }

    @Test
    public void setPriorityForExperimentWithPriorityNotSame() {

        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withState(Experiment.State.RUNNING).build();
        List<Experiment.ID> list = new ArrayList<>();
        list.add(experiment.getID());
        when(prioritiesRepository.getPriorityList(testApp)).thenReturn(list);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        prioritiesImpl.setPriority(experiment.getID(), 2);

        verify(prioritiesRepository, times(1)).createPriorities(any(Application.Name.class), anyList());
    }

    @Test
    public void setPriorityForExperimentWithPriorityZero() {

        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withState(Experiment.State.RUNNING).build();
        List<Experiment.ID> list = new ArrayList<>();
        list.add(experiment.getID());
        when(prioritiesRepository.getPriorityList(testApp)).thenReturn(list);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        prioritiesImpl.setPriority(experiment.getID(), 0);

        verify(prioritiesRepository, times(1)).createPriorities(any(Application.Name.class), anyList());
    }
}
