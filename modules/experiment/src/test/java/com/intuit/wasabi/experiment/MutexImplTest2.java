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

import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.experiment.impl.ExperimentsImpl;
import com.intuit.wasabi.experiment.impl.MutexImpl;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.repository.cassandra.impl.CassandraMutexRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willDoNothing;

@RunWith(MockitoJUnitRunner.class)
public class MutexImplTest2 {

    @Mock
    CassandraMutexRepository mutexRepository;

    @Mock
    private ExperimentsImpl experiments;

    @Mock
    private EventLog eventLog;

    @InjectMocks
    MutexImpl resource;

    private final static Application.Name testApp = Application.Name.valueOf("testApp");

    @Test
    public void testDeleteExclusionSuccessful() {
        Experiment.ID experimentID1 = Experiment.ID.newInstance();
        Experiment experiment_1 = Experiment.withID(experimentID1)
                .withApplicationName(testApp)
                .build();
        Experiment.ID experimentID2 = Experiment.ID.newInstance();
        Experiment experiment_2 = Experiment.withID(experimentID2)
                .withApplicationName(testApp)
                .withLabel(Label.valueOf("l2"))
                .build();

        given(experiments.getExperiment(experimentID1)).willReturn(experiment_1);
        given(experiments.getExperiment(experimentID2)).willReturn(experiment_2);
        willDoNothing().given(mutexRepository).deleteExclusion(experimentID1, experimentID2);
        willDoNothing().given(eventLog).postEvent(any(ExperimentChangeEvent.class));
        resource.deleteExclusion(experimentID1, experimentID2, null);

        verify(mutexRepository).deleteExclusion(experimentID1, experimentID2);
        verify(eventLog).postEvent(any(ExperimentChangeEvent.class));
    }

    @Test
    public void testGetExclusions() {
        ExperimentList exclusions = new ExperimentList();
        Experiment.ID experimentID = Experiment.ID.newInstance();
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .build();
        given(experiments.getExperiment(experimentID)).willReturn(experiment);
        given(mutexRepository.getExclusions(experimentID)).willReturn(exclusions);

        ExperimentList result = resource.getExclusions(experimentID);

        then(result).isEqualTo(exclusions);

        verify(experiments).getExperiment(experimentID);
        verify(mutexRepository).getExclusions(experimentID);
    }

    @Test
    public void testGetNotExclusions() {
        ExperimentList notExclusions = new ExperimentList();
        Experiment.ID experimentID = Experiment.ID.newInstance();
        given(mutexRepository.getNotExclusions(experimentID)).willReturn(notExclusions);

        ExperimentList result = resource.getNotExclusions(experimentID);

        then(result).isEqualTo(notExclusions);

        verify(mutexRepository).getNotExclusions(experimentID);
    }
}
