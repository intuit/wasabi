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
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import io.codearte.catchexception.shade.mockito.Mockito;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPrioritiesRepositoryTest {

    @Mock
    PrioritiesAccessor accessor;

    @Mock
    ExperimentAccessor experimentAccessor;

    @Mock
    Result<com.intuit.wasabi.repository.cassandra.pojo.Application> resultDatastax;

    @Mock
    Result<com.intuit.wasabi.repository.cassandra.pojo.Application> result;

    @Mock
    Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentResult;

    CassandraPrioritiesRepository repository;

    Application.Name applicationName;

    List<Experiment.ID> priorityIds;
    List<UUID> priorityUUIDs;
    List<com.intuit.wasabi.repository.cassandra.pojo.Application> applications;
    com.intuit.wasabi.repository.cassandra.pojo.Application application;
    List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experiments;
    com.intuit.wasabi.repository.cassandra.pojo.Experiment experiment;
    UUID experimentId = UUID.randomUUID();


    @Before
    public void setUp() throws Exception {
        accessor = Mockito.mock(PrioritiesAccessor.class);
        experimentAccessor = Mockito.mock(ExperimentAccessor.class);
        resultDatastax = Mockito.mock(Result.class);
        result = Mockito.mock(Result.class);
        experimentResult = Mockito.mock(Result.class);

        repository = new CassandraPrioritiesRepository(accessor, experimentAccessor);
        applicationName = Application.Name.valueOf("TestApplicationName");
        priorityIds = new ArrayList<>();
        applications = new ArrayList<>();
        priorityUUIDs = new ArrayList<>();
        experiments = new ArrayList<>();
        application = new com.intuit.wasabi.repository.cassandra.pojo.Application();

        experiment = new com.intuit.wasabi.repository.cassandra.pojo.Experiment();
        experiment.setId(experimentId);
        experiment.setAppName("ap1");
        experiment.setCreated(new Date());
        experiment.setStartTime(new Date());
        experiment.setEndTime(new Date());
        experiment.setModified(new Date());
        experiment.setLabel("l1");
        experiment.setSamplePercent(.5d);
        experiment.setState(Experiment.State.DRAFT.name());

    }

    @Test(expected = RepositoryException.class)
    public void testGetPrioritiesThrowsRepositoryException() {
        repository.getPriorities(applicationName);
    }

    @Test
    public void testGetPrioritiesSuccess() {
        applications.add(application);
        priorityUUIDs.add(experimentId);
        application.setPriorities(priorityUUIDs);

//		experiments.add(experiment);
        Mockito.when(accessor.getPriorities(applicationName.toString())).thenReturn(result);
        Mockito.when(result.all()).thenReturn(applications);
        Mockito.when(experimentAccessor.getExperimentById(Mockito.any())).thenReturn(experimentResult);
        Mockito.when(experimentResult.one()).thenReturn(experiment);

        PrioritizedExperimentList priorities = repository.getPriorities(applicationName);

        assertEquals("Values should be equal", 1, priorities.getPrioritizedExperiments().size());
        assertEquals("Values should be equal", experimentId,
                priorities.getPrioritizedExperiments().get(0).getID().getRawID());
    }

    @Test
    public void testCreatePrioritiesSuccess() {
        priorityIds.add(Experiment.ID.newInstance());
        repository.createPriorities(applicationName, priorityIds);
        Mockito.verify(accessor).updatePriorities(Mockito.anyList(), Mockito.anyString());
    }

    @Test(expected = RepositoryException.class)
    public void testCreatePrioritiesDeletionThrowsException() {
        Mockito.doThrow(new RuntimeException("RuntimeException")).when(accessor).deletePriorities(Mockito.anyString());
        repository.createPriorities(applicationName, priorityIds);
        Mockito.verify(accessor, Mockito.atLeastOnce()).
                deletePriorities(applicationName.toString());
    }

    @Test(expected = RepositoryException.class)
    public void testCreatePrioritiesUpdateThrowsException() {
        priorityIds.add(Experiment.ID.newInstance());
        Mockito.doThrow(new RuntimeException("RuntimeException")).when(accessor).updatePriorities(Mockito.anyList(),
                Mockito.anyString());
        repository.createPriorities(applicationName, priorityIds);
        Mockito.verify(accessor, Mockito.atLeastOnce()).
                updatePriorities(Mockito.anyList(), Mockito.eq(applicationName.toString()));
    }

    @Test
    public void testGetPrioritiesListSuccess() {
        priorityIds.add(Experiment.ID.newInstance());
        ArrayList<UUID> ids = new ArrayList<>();
        ids.add(priorityIds.get(0).getRawID());
        com.intuit.wasabi.repository.cassandra.pojo.Application app =
                new com.intuit.wasabi.repository.cassandra.pojo.Application(applicationName.toString(), ids);
        List<com.intuit.wasabi.repository.cassandra.pojo.Application> allApps = new ArrayList<>();
        allApps.add(app);
        Mockito.doReturn(allApps).when(resultDatastax).all();

        Mockito.doReturn(resultDatastax).when(accessor).getPriorities(applicationName.toString());

        List<ID> priorities = repository.getPriorityList(applicationName);
        assertEquals("Size should be same", 1, priorities.size());
        assertEquals("Value should be equal",
                priorityIds.get(0), priorities.get(0));
        Mockito.verify(accessor).getPriorities(applicationName.toString());
    }

    @Test(expected = RepositoryException.class)
    public void testGetPrirotiesListThrowsException() {
        Mockito.doThrow(new RuntimeException("RuntimeException")).when(accessor).
                getPriorities(Mockito.anyString());
        repository.getPriorityList(applicationName);
    }

    @Test
    public void testGetPrioritiesListLengthSuccess() {
        priorityIds.add(Experiment.ID.newInstance());
        ArrayList<UUID> ids = new ArrayList<>();
        ids.add(priorityIds.get(0).getRawID());
        com.intuit.wasabi.repository.cassandra.pojo.Application app =
                new com.intuit.wasabi.repository.cassandra.pojo.Application(applicationName.toString(), ids);
        List<com.intuit.wasabi.repository.cassandra.pojo.Application> allApps = new ArrayList<>();
        allApps.add(app);
        Mockito.doReturn(allApps).when(resultDatastax).all();

        Mockito.doReturn(resultDatastax).when(accessor).getPriorities(applicationName.toString());

        int size = repository.getPriorityListLength(applicationName);
        assertEquals("Size should be same", 1, size);
        Mockito.verify(accessor).getPriorities(applicationName.toString());
    }

    @Test(expected = RepositoryException.class)
    public void testGetPrirotiesListLengthThrowsException() {
        Mockito.doThrow(new RuntimeException("RuntimeException")).when(accessor).
                getPriorities(Mockito.anyString());
        repository.getPriorityListLength(applicationName);
    }
}
