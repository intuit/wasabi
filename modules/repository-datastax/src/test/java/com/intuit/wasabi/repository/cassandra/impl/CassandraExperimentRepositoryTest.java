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
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Application.Name;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.State;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.BucketAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentTagAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.BucketAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.ExperimentAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentLabelIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.StateExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentTagsByApplication;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraExperimentRepositoryTest {

    CassandraExperimentRepository repository;

    private ID experimentID1;
    private ID experimentID2;

    private Bucket bucket1;
    private Bucket bucket2;

    private NewExperiment newExperiment1;
    private NewExperiment newExperiment2;

    private ExperimentLabelIndexAccessor mockExperimentLabelIndexAccessor;
    private ApplicationListAccessor mockApplicationListAccessor;

    private BucketAccessor mockBucketAccessor;

    private Context QA = Context.valueOf("qa");
    private Application.Name appname;

    private CassandraDriver mockDriver;

    private ExperimentAccessor mockExperimentAccessor;
    private ExperimentAuditLogAccessor mockExperimentAuditLogAccessor;
    private ExperimentTagAccessor mockExperimentTagAccessor;
    private PrioritiesAccessor prioritiesAccessor;

    private StateExperimentIndexAccessor mockStateExperimentIndexAccessor;

    private BucketAuditLogAccessor mockBucketAuditLogAccessor;

    @Before
    public void setUp() throws Exception {

        mockBucketAccessor = Mockito.mock(BucketAccessor.class);
        mockDriver = Mockito.mock(CassandraDriver.class);
        mockExperimentAccessor = Mockito.mock(ExperimentAccessor.class);
        mockStateExperimentIndexAccessor = Mockito.mock(StateExperimentIndexAccessor.class);
        mockBucketAuditLogAccessor = Mockito.mock(BucketAuditLogAccessor.class);
        mockExperimentAuditLogAccessor = Mockito.mock(ExperimentAuditLogAccessor.class);
        mockApplicationListAccessor = Mockito.mock(ApplicationListAccessor.class);
        mockExperimentLabelIndexAccessor = Mockito.mock(ExperimentLabelIndexAccessor.class);
        mockExperimentTagAccessor = Mockito.mock(ExperimentTagAccessor.class);
        prioritiesAccessor = Mockito.mock(PrioritiesAccessor.class);

        if (repository != null) return;

        appname = Application.Name.valueOf("app1" + System.currentTimeMillis());

        experimentID1 = Experiment.ID.valueOf(UUID.randomUUID());
        experimentID2 = Experiment.ID.valueOf(UUID.randomUUID());

        repository = new CassandraExperimentRepository(
                mockDriver, mockExperimentAccessor, mockExperimentLabelIndexAccessor,
                mockBucketAccessor, mockApplicationListAccessor,
                mockBucketAuditLogAccessor, mockExperimentAuditLogAccessor,
                mockStateExperimentIndexAccessor, new ExperimentValidator(),
                mockExperimentTagAccessor, prioritiesAccessor);
        bucket1 = Bucket.newInstance(experimentID1, Bucket.Label.valueOf("bl1")).withAllocationPercent(.23)
                .withControl(true)
                .withDescription("b1").withPayload("p1")
                .withState(State.OPEN).build();

        bucket2 = Bucket.newInstance(experimentID2, Bucket.Label.valueOf("bl2"))
                .withAllocationPercent(.24).withControl(false)
                .withDescription("b2").withPayload("p2")
                .withState(State.OPEN).build();

        newExperiment1 = new NewExperiment(experimentID1);
        newExperiment1.setApplicationName(appname);
        newExperiment1.setLabel(Experiment.Label.valueOf("el1" + System.currentTimeMillis()));
        newExperiment1.setCreatorID("c1");
        newExperiment1.setDescription("ed1");
        newExperiment1.setStartTime(new Date());
        newExperiment1.setEndTime(new Date());
        newExperiment1.setSamplingPercent(0.2);
        experimentID1 = newExperiment1.getId();

        newExperiment2 = new NewExperiment(experimentID2);
        newExperiment2.setApplicationName(appname);
        newExperiment2.setLabel(Experiment.Label.valueOf("el2" + System.currentTimeMillis()));
        newExperiment2.setCreatorID("c2");
        newExperiment2.setDescription("ed2");
        newExperiment2.setStartTime(new Date());
        newExperiment2.setEndTime(new Date());
        newExperiment2.setSamplingPercent(0.2);
        experimentID2 = newExperiment2.getId();

    }

    @Test(expected = RepositoryException.class)
    public void testCreateBatchBucketSessionNullThrowsException() {
        BucketList bucketList = new BucketList();
        bucketList.addBucket(bucket1);
        bucketList.addBucket(bucket2);
        repository.setDriver(mockDriver);
        repository.updateBucketBatch(experimentID1, bucketList);
    }

    @Test(expected = RepositoryException.class)
    public void testCreateBucketAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockBucketAccessor).insert(bucket1.getExperimentID().getRawID(),
                bucket1.getLabel().toString(), bucket1.getDescription(), bucket1.getAllocationPercent(),
                bucket1.isControl(), bucket1.getPayload(), Bucket.State.OPEN.name());
        repository.createBucket(bucket1);
    }

    @Test(expected = RepositoryException.class)
    public void testRemoveExperimentLabelIndexAccessorMockThrowsException() {
        repository.setExperimentLabelIndexAccessor(mockExperimentLabelIndexAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockExperimentLabelIndexAccessor).deleteBy(Mockito.any(), Mockito.any());
        repository.removeExperimentLabelIndex(appname, newExperiment1.getLabel());
    }

    @Test(expected = RepositoryException.class)
    public void testGetDeleteExperimentAccessorMockThrowsException() {
        repository.setExperimentAccessor(mockExperimentAccessor);
        Mockito.doThrow(new RuntimeException("test")).when(mockExperimentAccessor).deleteExperiment(Mockito.any());
        repository.deleteExperiment(newExperiment1);
    }

    @Test(expected = RepositoryException.class)
    public void testGetExperimentByAppAccessorMockThrowsException() {
        repository.setExperimentAccessor(mockExperimentAccessor);
        Mockito.doThrow(new RuntimeException("test")).when(mockExperimentAccessor).getExperimentByAppName(Mockito.anyString());
        Table<ID, Label, Experiment> experiments =
                repository.getExperimentList(newExperiment1.getApplicationName());
    }

    @Test(expected = RepositoryException.class)
    public void testCreateAppGetApplicationAccessorMockThrowsException() {
        int size = repository.getApplicationsList().size();
        Name app = Application.Name.valueOf("appx" + System.currentTimeMillis());
        repository.createApplication(app);

        repository.setApplicationListAccessor(mockApplicationListAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockApplicationListAccessor).getUniqueAppName();

        List<Name> apps = repository.getApplicationsList();
        assertEquals("Value should be equal", size + 1, apps.size());
        assertTrue("App should be in the list " + apps, apps.stream().anyMatch(application -> application.toString().equals(app.toString())));
    }

    @Test(expected = RepositoryException.class)
    public void testCreateAppApplicationListMockAccessorThrowsException() {

        repository.setApplicationListAccessor(mockApplicationListAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockApplicationListAccessor).insert(Mockito.anyString());
        repository.createApplication(appname);

    }


    @Test(expected = RepositoryException.class)
    public void testGetExperimentAccessorMockThrowsException() {
        repository.setExperimentAccessor(mockExperimentAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockExperimentAccessor)
                .getExperimentById(Mockito.any());

        Experiment experiment = repository.getExperiment(experimentID1);
    }

    @Test(expected = RepositoryException.class)
    public void testGetExperimentLabelIndexAccessorMockThrowsException() {
        repository.setExperimentLabelIndexAccessor(mockExperimentLabelIndexAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockExperimentLabelIndexAccessor)
                .getExperimentBy(Mockito.any(), Mockito.any());

        Experiment experiment = repository.getExperiment(appname, newExperiment1.getLabel());
    }

    @Test(expected = RepositoryException.class)
    public void testGetExperimentsAccessorMockThrowsException() {
        List<Experiment.ID> experimentIds = new ArrayList<>();
        experimentIds.add(experimentID1);
        experimentIds.add(experimentID2);
        repository.setExperimentAccessor(mockExperimentAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockExperimentAccessor)
                .getExperiments(Mockito.any());

        ExperimentList experiments = repository.getExperiments(experimentIds);
    }

    @Test(expected = RepositoryException.class)
    public void testGetExperimentsByAppNameAccessorMockThrowsException() {
        repository.setExperimentAccessor(mockExperimentAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockExperimentAccessor)
                .getExperimentByAppName(Mockito.any());

        List<Experiment> experiments = repository.getExperiments(appname);
    }

    @Test(expected = RepositoryException.class)
    public void testGetExperimentsStateExperimentAccesorMockSuccess() {
        repository.setStateExperimentIndexAccessor(mockStateExperimentIndexAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockStateExperimentIndexAccessor).selectByKey(Mockito.any());

        repository.getExperiments();
    }

    @Test(expected = RepositoryException.class)
    public void testGetOneBucketListWithBucketAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockBucketAccessor).getBucketByExperimentIdAndBucket(UUID.randomUUID(), "l1");

        Object buckets = repository.getBucketList((Experiment.ID) null);
    }

    @Test(expected = RepositoryException.class)
    public void testGetBucketWithBucketAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockBucketAccessor).getBucketByExperimentId(null);

        Bucket buckets = repository.getBucket(bucket1.getExperimentID(), bucket1.getLabel());
    }

    @Test(expected = RepositoryException.class)
    public void testGetOneBucketListBucketAccessorMock() {
        BucketList bucketList1 = new BucketList();
        bucketList1.addBucket(bucket1);

        repository.updateBucketBatch(experimentID1, bucketList1);

        List<Experiment.ID> experimentIds = new ArrayList<>();
        experimentIds.add(experimentID1);
        experimentIds.add(experimentID2);

        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockBucketAccessor).getBucketByExperimentId(null);

        Map<ID, BucketList> buckets = repository.getBucketList(experimentIds);
    }

    @Test(expected = RepositoryException.class)
    public void testUpdateBucketAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockBucketAccessor).getBucketByExperimentId(null);
        repository.updateBucket(bucket1);
    }

    @Test(expected = RepositoryException.class)
    public void testUpdateBucketNotControlAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockBucketAccessor).updateBucket(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());
        repository.updateBucket(bucket2);

    }

    @Test(expected = RepositoryException.class)
    public void testUpdateBucketAllocationAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockBucketAccessor).updateAllocation(0.01, bucket1.getExperimentID().getRawID(), bucket1.getLabel().toString());
        repository.updateBucketAllocationPercentage(bucket1, .01);
    }

    @Test(expected = RepositoryException.class)
    public void testUpdateExperimentAccessorMockThrowsException() {
        Experiment experiment = repository.getExperiment(experimentID1);
        repository.setExperimentAccessor(mockExperimentAccessor);
        repository.setExperimentLabelIndexAccessor(mockExperimentLabelIndexAccessor);
        Mockito.doThrow(new RuntimeException("test")).when(mockExperimentLabelIndexAccessor).insertOrUpdateBy(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any());

        repository.updateExperiment(experiment);
    }

    @Test(expected = RepositoryException.class)
    public void testUpdateExperimentStateAccessorMockThrowsException() {
        Experiment experiment = repository.getExperiment(experimentID1);
        repository.setExperimentAccessor(mockExperimentAccessor);
        Mockito.doThrow(new RuntimeException("test")).when(mockExperimentAccessor).updateExperiment(Mockito.any(), Mockito.any(), Mockito.any());

        repository.updateExperimentState(experiment, Experiment.State.PAUSED);
    }

    @Test(expected = RepositoryException.class)
    public void testUpdateExperimentExperimentAccessorThrowsException() {

        Experiment experiment = repository.getExperiment(experimentID1);
        String description = "newDescription" + System.currentTimeMillis();
        experiment.setDescription(description);
        repository.setExperimentAccessor(null);
        repository.updateExperiment(experiment);
    }

    @Test(expected = RepositoryException.class)
    public void testUpdateExperimentExperimentLabelIndexAccessorMockThrowsException() {

        Experiment experiment = repository.getExperiment(experimentID1);
        String description = "newDescription" + System.currentTimeMillis();
        experiment.setDescription(description);
        repository.setExperimentLabelIndexAccessor(mockExperimentLabelIndexAccessor);
        Mockito.doThrow(new RuntimeException("test")).when(mockExperimentLabelIndexAccessor).
                insertOrUpdateBy(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any());
        repository.updateExperiment(experiment);

    }

    @Test(expected = RepositoryException.class)
    public void testUpdateStateIndexAccessorMockThrowsException() {

        Experiment experiment = repository.getExperiment(experimentID1);
        assertEquals("Value should be eq", Experiment.State.DRAFT, experiment.getState());
        repository.setStateExperimentIndexAccessor(mockStateExperimentIndexAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockStateExperimentIndexAccessor)
                .insert(Mockito.any(), Mockito.any(), Mockito.any());
        repository.updateStateIndex(experiment);

    }

    @Test(expected = RepositoryException.class)
    public void testUpdateBucketStateAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockBucketAccessor)
                .updateState(Mockito.any(), Mockito.any(), Mockito.any());

        Bucket resultBucket = repository.updateBucketState(bucket1, Bucket.State.CLOSED);
    }

    @Test(expected = NullPointerException.class)
    public void testGetExperimentByAppNullThrowsException() {
        Experiment experiment = repository.getExperiment(null, newExperiment1.getLabel());
    }

    @Test(expected = NullPointerException.class)
    public void testGetExperimentByLabelNullThrowsException() {
        Experiment experiment = repository.getExperiment(newExperiment1.getApplicationName(), null);

    }

    @Test(expected = RepositoryException.class)
    public void testLogBucketAuditAccessorMockThrowsException() {
        String bucketLabel = "bkt" + System.currentTimeMillis();
        List<Bucket.BucketAuditInfo> auditLog = new ArrayList<>();
        Bucket.BucketAuditInfo log = new Bucket.BucketAuditInfo("attr1", "o1", "n1");
        auditLog.add(log);
        repository.setBucketAuditLogAccessor(mockBucketAuditLogAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockBucketAuditLogAccessor).insertBy(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        repository.logBucketChanges(experimentID1, Bucket.Label.valueOf(bucketLabel), auditLog);
    }

    @Test(expected = RepositoryException.class)
    public void testLogExperimentAuditMockAccessorThrowsException() {
        Experiment.ID expid = Experiment.ID.newInstance();

        List<Experiment.ExperimentAuditInfo> auditLog = new ArrayList<>();
        Experiment.ExperimentAuditInfo log = new Experiment.ExperimentAuditInfo("attr1", "o1", "n1");
        auditLog.add(log);
        repository.setExperimentAuditLogAccessor(mockExperimentAuditLogAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockExperimentAuditLogAccessor)
                .insertBy(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        repository.logExperimentChanges(expid, auditLog);

    }

    // NOTE - IMHO - The createIndicesForNewExperiment should not be in the interface
    @Test(expected = RepositoryException.class)
    public void testCreateIndexesForExperimentStateIndexAccessorMockThrowsException() {
        newExperiment1.setId(Experiment.ID.newInstance());
        repository.setStateExperimentIndexAccessor(mockStateExperimentIndexAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockStateExperimentIndexAccessor)
                .insert(Mockito.any(), Mockito.any(), Mockito.any());

        repository.createIndicesForNewExperiment(newExperiment1);
    }

    @Test(expected = RepositoryException.class)
    public void testGetBucketsAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        BucketList buckets = repository.getBuckets(experimentID1, false);
    }

    @Test(expected = RepositoryException.class)
    public void testDeleteBucketAccessorMockThrowsException() {
        repository.setBucketAccessor(mockBucketAccessor);
        Mockito.doThrow(new RuntimeException("testexception")).when(
                mockBucketAccessor).deleteByExperimentIdAndLabel(bucket1.getExperimentID().getRawID(),
                bucket1.getLabel().toString());
        repository.deleteBucket(experimentID1, bucket1.getLabel());
    }

    @Test(expected = RepositoryException.class)
    public void testCreateExperimentAccessorMockThrowsException() {
        newExperiment1.setId(Experiment.ID.newInstance());
        newExperiment1.setLabel(Experiment.Label.valueOf("lbl" + System.currentTimeMillis()));
        repository.setExperimentAccessor(mockExperimentAccessor);
        repository.setApplicationListAccessor(mockApplicationListAccessor);
        Mockito.doThrow(new RuntimeException("runtime")).when(mockApplicationListAccessor).insert(Mockito.anyString());
        ID experimentId = repository.createExperiment(newExperiment1);
    }

    @Test
    public void testGetterSetter() {
        assertNotNull("value should be not be null", repository.getApplicationListAccessor());
        assertNotNull("value should be not be null", repository.getBucketAuditLogAccessor());
        assertNotNull("value should be not be null", repository.getExperimentAccessor());
        assertNotNull("value should be not be null", repository.getExperimentAuditLogAccessor());
        assertNotNull("value should be not be null", repository.getStateExperimentIndexAccessor());
        assertNotNull("value should be not be null", repository.getExperimentAccessor());
        assertNotNull("value should be not be null", repository.getBucketAccessor());
        assertNotNull("value should be not be null", repository.getExperimentLabelIndexAccessor());
        assertNotNull("value should be not be null", repository.getDriver());

        repository.setApplicationListAccessor(null);
        ;
        assertEquals("Value should be eq", null, repository.getApplicationListAccessor());

        repository.setBucketAccessor(null);
        assertEquals("Value should be eq", null, repository.getBucketAccessor());

        repository.setExperimentAccessor(null);
        assertEquals("Value should be eq", null, repository.getExperimentAccessor());

        repository.setBucketAuditLogAccessor(null);
        assertEquals("Value should be eq", null, repository.getBucketAuditLogAccessor());

        repository.setExperimentAuditLogAccessor(null);
        assertEquals("Value should be eq", null, repository.getExperimentAuditLogAccessor());

        repository.setStateExperimentIndexAccessor(null);
        assertEquals("Value should be eq", null, repository.getStateExperimentIndexAccessor());

        repository.setExperimentLabelIndexAccessor(null);
        assertEquals("Value should be eq", null, repository.getExperimentLabelIndexAccessor());

        repository.setDriver(null);
        assertEquals("Value should be eq", null, repository.getDriver());
    }

    @Test(expected = RepositoryException.class)
    public void testCreateExperimentExperimentAccessorThrowsException() {
        repository.setExperimentAccessor(mockExperimentAccessor);
        newExperiment1.setId(Experiment.ID.newInstance());
        ID experimentId = repository.createExperiment(newExperiment1);
    }

    @Test
    public void testGetExperimentsForApps() throws ExecutionException, InterruptedException {
        //------ Input --------
        Experiment.ID expId1 = Experiment.ID.newInstance();
        Application.Name appName = Application.Name.valueOf("testApp1");
        Set<Name> appNameSet = new HashSet<>();
        appNameSet.add(appName);

        //------ Mocking interacting calls
        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>> experimentsFuture = mock(ListenableFuture.class);
        when(mockExperimentAccessor.asyncGetExperimentByAppName(appName.toString())).thenReturn(experimentsFuture);
        List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> expList = new ArrayList<>();
        com.intuit.wasabi.repository.cassandra.pojo.Experiment exp1 = com.intuit.wasabi.repository.cassandra.pojo.Experiment.builder()
                .id(expId1.getRawID())
                .appName(appName.toString())
                .startTime(new Date())
                .created(new Date())
                .endTime(new Date())
                .state(Experiment.State.RUNNING.toString())
                .label("testExp1")
                .modified(new Date())
                .samplePercent(90.00)
                .build();
        expList.add(exp1);
        Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> expResult = mock(Result.class);
        when(expResult.all()).thenReturn(expList);
        when(experimentsFuture.get()).thenReturn(expResult);

        //Make actual call
        Map<Application.Name, List<Experiment>> actualResultMap = repository.getExperimentsForApps(appNameSet);

        //Verify result
        assertThat(actualResultMap.get(appName).size(), is(1));
        assertThat(actualResultMap.get(appName).get(0).getID().getRawID(), is(exp1.getId()));
    }

    @Test
    public void testGetExperimentsMap() throws ExecutionException, InterruptedException {
        //------ Input --------
        Application.Name appName = Application.Name.valueOf("testApp1");
        Experiment.ID expId1 = Experiment.ID.newInstance();
        Experiment.ID expId2 = Experiment.ID.newInstance();

        Set<Experiment.ID> expIdSet = new HashSet<>();
        expIdSet.add(expId1);
        expIdSet.add(expId2);

        //------ Mocking interacting calls
        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>> experimentsFuture = mock(ListenableFuture.class);
        when(mockExperimentAccessor.asyncGetExperimentById(expId1.getRawID())).thenReturn(experimentsFuture);
        List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> expList = new ArrayList<>();
        com.intuit.wasabi.repository.cassandra.pojo.Experiment exp1 = com.intuit.wasabi.repository.cassandra.pojo.Experiment.builder()
                .id(expId1.getRawID())
                .appName(appName.toString())
                .startTime(new Date())
                .created(new Date())
                .endTime(new Date())
                .state(Experiment.State.RUNNING.toString())
                .label("testExp1")
                .modified(new Date())
                .samplePercent(90.00)
                .build();
        expList.add(exp1);
        Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> expResult = mock(Result.class);
        when(expResult.all()).thenReturn(expList);
        when(experimentsFuture.get()).thenReturn(expResult);


        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>> experimentsFuture2 = mock(ListenableFuture.class);
        when(mockExperimentAccessor.asyncGetExperimentById(expId2.getRawID())).thenReturn(experimentsFuture2);
        List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> expList2 = new ArrayList<>();
        com.intuit.wasabi.repository.cassandra.pojo.Experiment exp2 = com.intuit.wasabi.repository.cassandra.pojo.Experiment.builder()
                .id(expId2.getRawID())
                .appName(appName.toString())
                .startTime(new Date())
                .created(new Date())
                .endTime(new Date())
                .state(Experiment.State.RUNNING.toString())
                .label("testExp1")
                .modified(new Date())
                .samplePercent(90.00)
                .build();
        expList2.add(exp2);
        Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> expResult2 = mock(Result.class);
        when(expResult2.all()).thenReturn(expList2);
        when(experimentsFuture2.get()).thenReturn(expResult2);


        //Make actual call
        Map<Experiment.ID, Experiment> actualResultMap = repository.getExperimentsMap(expIdSet);

        //Verify result
        assertThat(actualResultMap.size(), is(2));
        assertThat(actualResultMap.get(expId1).getID().getRawID(), is(exp1.getId()));
        assertThat(actualResultMap.get(expId2).getID().getRawID(), is(exp2.getId()));
    }


    @Test
    public void testGetBucketList() throws ExecutionException, InterruptedException {
        //------ Input --------
        Experiment.ID expId1 = Experiment.ID.newInstance();
        Set<Experiment.ID> expIdSet = new HashSet<>();
        expIdSet.add(expId1);

        //------ Mocking interacting calls
        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket>> bucketsFuture = mock(ListenableFuture.class);
        when(mockBucketAccessor.asyncGetBucketByExperimentId(expId1.getRawID())).thenReturn(bucketsFuture);
        List<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketList = new ArrayList<>();
        com.intuit.wasabi.repository.cassandra.pojo.Bucket bucket1 = com.intuit.wasabi.repository.cassandra.pojo.Bucket.builder()
                .experimentId(expId1.getRawID())
                .state(State.OPEN.toString())
                .allocation(0.4d)
                .control(true)
                .label("Test-Bucket-1")
                .payload("Test-Bucket-1-Payload")
                .build();
        bucketList.add(bucket1);
        Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketResult = mock(Result.class);
        when(bucketResult.all()).thenReturn(bucketList);
        when(bucketsFuture.get()).thenReturn(bucketResult);

        //Make actual call
        Map<Experiment.ID, BucketList> actualResultMap = repository.getBucketList(expIdSet);

        //Verify result
        assertThat(actualResultMap.get(expId1) != null, is(true));
        assertThat(actualResultMap.get(expId1).getBuckets().size(), is(1));
        assertThat(actualResultMap.get(expId1).getBuckets().get(0).getLabel().toString(), is("Test-Bucket-1"));
    }

    @Test
    public void testUpdateTags() {
        //------ Input --------
        Application.Name appName = Application.Name.valueOf("AppName");

        Set<String> exp1 = new TreeSet<>(Arrays.asList("tag1", "tag3"));
        Set<String> exp2 = new TreeSet<>(Arrays.asList("tag4"));
        ExperimentTagsByApplication exp2Tags = ExperimentTagsByApplication.builder()
                .tags(exp2).appName(appName.toString()).build();
        ExperimentTagsByApplication exp1Tags = ExperimentTagsByApplication.builder()
                .tags(exp1).appName(appName.toString()).build();

        List<ExperimentTagsByApplication> dbResultList = Arrays.asList(exp1Tags, exp2Tags);

        //------ Mocking interacting calls
        Result<ExperimentTagsByApplication> dbResult = mock(Result.class);
        when(mockExperimentAccessor.getAllTags(appName.toString())).thenReturn(dbResult);
        when(dbResult.all()).thenReturn(dbResultList);

        //------ Make call
        repository.updateExperimentTags(appName, experimentID1, exp1);
        repository.updateExperimentTags(appName, experimentID2, exp2);

        //------ Verify result
        verify(mockExperimentTagAccessor).insert(appName.toString(), experimentID1.getRawID(),
                new TreeSet<>(Arrays.asList("tag1", "tag3")));
        verify(mockExperimentTagAccessor).insert(appName.toString(), experimentID2.getRawID(),
                new TreeSet<>(Arrays.asList("tag4")));
    }

    @Test
    public void testGetTagList() throws ExecutionException, InterruptedException {
        //------ Input --------
        Application.Name app01 = Application.Name.valueOf("app01");
        Application.Name app02 = Application.Name.valueOf("app02");
        List<Application.Name> appNames = Arrays.asList(app01, app02);

        Set<String> exp1 = new TreeSet<>(Arrays.asList("tag1", "tag3"));
        Set<String> exp2 = new TreeSet<>(Arrays.asList("tag4"));

        List<ExperimentTagsByApplication> exp1Tags = Arrays.asList(ExperimentTagsByApplication.builder()
                .tags(exp1).appName(app01.toString()).build());
        List<ExperimentTagsByApplication> exp2Tags = Arrays.asList(ExperimentTagsByApplication.builder()
                .tags(exp2).appName(app02.toString()).build());

        //------ Mocking interacting calls
        ListenableFuture<Result<ExperimentTagsByApplication>> dbResultFuture1 = mock(ListenableFuture.class);
        ListenableFuture<Result<ExperimentTagsByApplication>> dbResultFuture2 = mock(ListenableFuture.class);

        Result<ExperimentTagsByApplication> dbResult1 = mock(Result.class);
        Result<ExperimentTagsByApplication> dbResult2 = mock(Result.class);

        when(mockExperimentTagAccessor.getExperimentTagsAsync("app01")).thenReturn(dbResultFuture1);
        when(mockExperimentTagAccessor.getExperimentTagsAsync("app02")).thenReturn(dbResultFuture2);

        when(dbResultFuture1.get()).thenReturn(dbResult1);
        when(dbResultFuture2.get()).thenReturn(dbResult2);

        when(dbResult1.all()).thenReturn(exp1Tags);
        when(dbResult2.all()).thenReturn(exp2Tags);

        //------ Make call
        Map<Name, Set<String>> callResult = repository.getTagListForApplications(appNames);

        //------ Verify result
        assertEquals(2, callResult.size());
        assertArrayEquals(exp1.toArray(), callResult.get(app01).toArray());
        assertArrayEquals(exp2.toArray(), callResult.get(app02).toArray());
    }


    @Test
    public void testGetEmptyTagList() throws ExecutionException, InterruptedException {
        //------ Input --------
        Application.Name app01 = Application.Name.valueOf("app01");
        List<Application.Name> appNames = Arrays.asList(app01);

        List<ExperimentTagsByApplication> exp1Tags = Collections.emptyList();

        //------ Mocking interacting calls
        ListenableFuture<Result<ExperimentTagsByApplication>> dbResultFuture1 = mock(ListenableFuture.class);

        Result<ExperimentTagsByApplication> dbResult1 = mock(Result.class);

        when(mockExperimentTagAccessor.getExperimentTagsAsync("app01")).thenReturn(dbResultFuture1);

        when(dbResultFuture1.get()).thenReturn(dbResult1);

        when(dbResult1.all()).thenReturn(exp1Tags);

        //------ Make call
        Map<Name, Set<String>> callResult = repository.getTagListForApplications(appNames);

        //------ Verify result
        assertEquals(0, callResult.size()); // no results are returned without error
    }

}
