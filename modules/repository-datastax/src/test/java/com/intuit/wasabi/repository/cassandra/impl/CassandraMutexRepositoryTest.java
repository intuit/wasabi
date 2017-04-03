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

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Result;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.ExclusionAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.Exclusion;
import io.codearte.catchexception.shade.mockito.Mockito;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static io.codearte.catchexception.shade.mockito.Mockito.mock;
import static java.util.Objects.nonNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CassandraMutexRepositoryTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ExclusionAccessor accessor;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ExperimentAccessor experimentAccessor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion> resultDatastax;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> resultExperimentDatastax;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    CassandraDriver driver;

    CassandraMutexRepository repository;

    Application.Name applicationName;

    List<Experiment.ID> priorityIds;

    private ID base;

    private ID pair;

    private List<Exclusion> exclusions;
    private List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experiments;

    private Session session;
    private com.intuit.wasabi.repository.cassandra.pojo.Experiment experiment;

    @Before
    public void setUp() throws Exception {
        accessor = mock(ExclusionAccessor.class);
        experimentAccessor = mock(ExperimentAccessor.class);
        resultDatastax = mock(Result.class);
        resultExperimentDatastax = mock(Result.class);
        driver = mock(CassandraDriver.class);
        session = mock(Session.class);
        Mockito.when(driver.getSession()).thenReturn(session);
        repository = new CassandraMutexRepository(experimentAccessor, accessor, driver);
        base = Experiment.ID.newInstance();
        pair = Experiment.ID.newInstance();
        exclusions = new ArrayList<>();
        experiments = new ArrayList<>();

        UUID experimentId = UUID.randomUUID();
        experiment =
                new com.intuit.wasabi.repository.cassandra.pojo.Experiment();
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

    @Test
    public void testGetNonExclusionsSuccess() {
        Exclusion exc = new Exclusion(base.getRawID(), pair.getRawID());
        exclusions.add(exc);

        experiments.add(experiment);

        Mockito.when(accessor.getExclusions(base.getRawID())).thenReturn(resultDatastax);
        Mockito.when(resultDatastax.all()).thenReturn(exclusions);

        Mockito.when(experimentAccessor.getExperimentById(Mockito.any())).thenReturn(resultExperimentDatastax);
        Mockito.when(resultExperimentDatastax.one())
                .thenReturn(experiment);
        Mockito.when(experimentAccessor.getExperimentByAppName(Mockito.anyString()))
                .thenReturn(resultExperimentDatastax);
        Mockito.when(resultExperimentDatastax.all()).thenReturn(experiments);

        ExperimentList result = repository.getNotExclusions(base);

        assertEquals("Value should be eq", 1, result.getExperiments().size());

    }

    @Test(expected = RepositoryException.class)
    public void testGetNonExclusionsThrowsException() {

        Mockito.when(accessor.getExclusions(base.getRawID())).thenThrow(new RuntimeException("runtimeexcp"));
        ExperimentList result = repository.getNotExclusions(base);
    }

    @Test
    public void testGetExclusionsSuccess() {
        Exclusion exc = new Exclusion(base.getRawID(), pair.getRawID());
        exclusions.add(exc);

        experiments.add(experiment);

        Mockito.when(accessor.getExclusions(base.getRawID())).thenReturn(resultDatastax);
        Mockito.when(resultDatastax.all()).thenReturn(exclusions);

        Mockito.when(experimentAccessor.getExperiments(Mockito.any())).thenReturn(resultExperimentDatastax);
        Mockito.when(resultExperimentDatastax.all())
                .thenReturn(experiments);
        ExperimentList result = repository.getExclusions(base);

        assertEquals("Value should be eq", 1, result.getExperiments().size());
        assertEquals("value should be same", experiment.getId(),
                result.getExperiments().get(0).getID().getRawID());

    }

    @Test(expected = RepositoryException.class)
    public void testGetExclusionsThrowsException() {

        Mockito.when(accessor.getExclusions(base.getRawID())).thenThrow(
                new RuntimeException("RuntimeException"));
        ExperimentList result = repository.getExclusions(base);
    }


    @Test
    public void testCreateExclusionSuccess() {
        repository.createExclusion(Experiment.ID.newInstance(), Experiment.ID.newInstance());
    }

    @Test(expected = RepositoryException.class)
    public void testCreateExclusionAccesorThrowsException() {
        Mockito.doThrow(new RuntimeException("RuntimeExcp")).when(accessor)
                .createExclusion(base.getRawID(), pair.getRawID());
        repository.createExclusion(base, pair);
    }

    @Test
    public void testDeleteExclusionSuccess() {
        repository.deleteExclusion(base, pair);
    }

    @Test(expected = RepositoryException.class)
    public void testDeleteExclusionAccesorThrowsException() {
        Mockito.doThrow(new RuntimeException("RuntimeExcp")).when(accessor)
                .deleteExclusion(base.getRawID(), pair.getRawID());
        repository.deleteExclusion(base, pair);
    }

    @Test
    public void testGetOneExclusionListSuccess() {
        Exclusion exc = new Exclusion(base.getRawID(), pair.getRawID());
        exclusions.add(exc);
        Mockito.when(accessor.getExclusions(base.getRawID())).thenReturn(resultDatastax);
        Mockito.when(resultDatastax.all()).thenReturn(exclusions);
        List<ID> result = repository.getExclusionList(base);

        assertEquals("value should be equal", pair, result.get(0));
    }

    @Test
    public void testGetOneExclusiveListSuccess() throws InterruptedException, ExecutionException {
        Exclusion exc = new Exclusion(base.getRawID(), pair.getRawID());
        exclusions.add(exc);
        ListenableFuture<Result<Exclusion>> mockListenableFuture = mock(ListenableFuture.class);
        Mockito.when(accessor.asyncGetExclusions(base.getRawID())).thenReturn(mockListenableFuture);
        Mockito.when(mockListenableFuture.get()).thenReturn(resultDatastax);
        Mockito.when(resultDatastax.all()).thenReturn(exclusions);

        ArrayList<Experiment.ID> ids = new ArrayList<>();
        ids.add(base);
        Map<ID, List<ID>> result = repository.getExclusivesList(ids);

        assertEquals("value should be equal", 1, result.size());
        assertEquals("value should be equal", 1, result.get(base).size());
        assertEquals("value should be equal", pair, result.get(base).get(0));
    }

    @Test(expected = RepositoryException.class)
    public void testGetOneExclusiveListThrowsException() {
        Exclusion exc = new Exclusion(base.getRawID(), pair.getRawID());
        exclusions.add(exc);
        Mockito.when(accessor.getExclusions(base.getRawID())).thenThrow(
                new RuntimeException("TestException"));

        ArrayList<Experiment.ID> ids = new ArrayList<>();
        ids.add(base);
        Map<ID, List<ID>> result = repository.getExclusivesList(ids);

    }

    @Test
    public void testGetZeroExclusionListSuccess() {
        Mockito.when(accessor.getExclusions(base.getRawID())).thenReturn(resultDatastax);
        Mockito.when(resultDatastax.all()).thenReturn(exclusions);
        List<ID> result = repository.getExclusionList(base);

        assertEquals("value should be equal", 0, result.size());
    }

    @Test(expected = RepositoryException.class)
    public void testGetOneExclusionListThrowsException() {
        Mockito.when(accessor.getExclusions(base.getRawID())).thenThrow(new RuntimeException("RTE"));
        List<ID> result = repository.getExclusionList(base);

    }

    @Test
    public void testGetExclusivesList() throws ExecutionException, InterruptedException {
        //------ Input --------
        Experiment.ID expId1 = Experiment.ID.newInstance();
        Experiment.ID expId2 = Experiment.ID.newInstance();
        Experiment.ID expId3 = Experiment.ID.newInstance();

        Set<Experiment.ID> expIdSet = new HashSet<>();
        expIdSet.add(expId1);

        //------ Mocking interacting calls
        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion>> experimentsFuture = Mockito.mock(ListenableFuture.class);
        Mockito.when(accessor.asyncGetExclusions(expId1.getRawID())).thenReturn(experimentsFuture);
        List<com.intuit.wasabi.repository.cassandra.pojo.Exclusion> expList = new ArrayList<>();
        com.intuit.wasabi.repository.cassandra.pojo.Exclusion exp1 = com.intuit.wasabi.repository.cassandra.pojo.Exclusion.builder()
                .base(expId1.getRawID())
                .pair(expId2.getRawID())
                .build();
        expList.add(exp1);

        com.intuit.wasabi.repository.cassandra.pojo.Exclusion exp2 = com.intuit.wasabi.repository.cassandra.pojo.Exclusion.builder()
                .base(expId1.getRawID())
                .pair(expId3.getRawID())
                .build();
        expList.add(exp2);

        Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion> expResult = Mockito.mock(Result.class);
        Mockito.when(expResult.all()).thenReturn(expList);
        Mockito.when(experimentsFuture.get()).thenReturn(expResult);

        //Make actual call
        Map<Experiment.ID, List<Experiment.ID>> mutuallyExclusiveExperiments = repository.getExclusivesList(expIdSet);

        //Verify result
        assertThat(mutuallyExclusiveExperiments.size(), is(1));
        assertThat(nonNull(mutuallyExclusiveExperiments.get(expId1)), is(true));
        assertThat(mutuallyExclusiveExperiments.get(expId1).size(), is(2));
        assertThat(mutuallyExclusiveExperiments.get(expId1).contains(expId2), is(true));
        assertThat(mutuallyExclusiveExperiments.get(expId1).contains(expId3), is(true));
    }
}
