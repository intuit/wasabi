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
package com.intuit.wasabi.repository.database;

import com.googlecode.catchexception.apis.BDDCatchException;
import com.googlecode.flyway.core.Flyway;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.exceptions.BucketNotFoundException;
import com.intuit.wasabi.exceptions.DatabaseException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import com.intuit.wasabi.repository.RepositoryException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.googlecode.catchexception.CatchException.caughtException;
import static com.intuit.wasabi.experimentobjects.Experiment.State.DELETED;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class DatabaseExperimentRepositoryTest {

    TransactionFactory transactionFactory = Mockito.mock(TransactionFactory.class);
    Transaction transaction = Mockito.mock(Transaction.class);
    DataSource dataSource = Mockito.mock(DataSource.class);
    ExperimentValidator experimentValidator = Mockito.mock(ExperimentValidator.class);
    Flyway flyway = Mockito.mock(Flyway.class);
    DatabaseExperimentRepository repository;

    @Before
    public void setup() {
        when(transactionFactory.newTransaction()).thenReturn(transaction);
        when(transactionFactory.getDataSource()).thenReturn(dataSource);
        repository = new DatabaseExperimentRepository(transactionFactory, experimentValidator, flyway);
    }

    @Test
    public void testInitialize() {
        verify(transactionFactory, atLeastOnce()).getDataSource();
        verify(flyway, atLeastOnce()).setLocations("com/intuit/wasabi/repository/impl/mysql/migration");
        verify(flyway, atLeastOnce()).setDataSource(dataSource);
        verify(flyway, atLeastOnce()).migrate();
    }

    @Test
    public void testNewTransaction() {
        Transaction result = repository.newTransaction();
        assertThat(result, is(transaction));
    }

    @Test
    public void testGetExperiment() {
        Experiment.ID id = Experiment.ID.newInstance();
        List queryResult = mock(List.class);
        when(transaction.select(anyString(), eq(id),
                eq(Experiment.State.DELETED.toString()))).thenReturn(queryResult);
        when(queryResult.isEmpty()).thenReturn(true);
        Experiment result = repository.getExperiment(id);
        assertThat(result, is(nullValue()));
        //result size > 1
        when(queryResult.isEmpty()).thenReturn(false);
        when(queryResult.size()).thenReturn(2);
        BDDCatchException.when(repository).getExperiment(id);
        BDDCatchException.then(caughtException())
                .isInstanceOf(RepositoryException.class)
                .hasMessage("Could not retrieve experiment \"" + id + "\"")
                .hasRootCauseExactlyInstanceOf(IllegalStateException.class);
        //rest of the happy case
        when(queryResult.size()).thenReturn(1);
        Map map = mock(Map.class);
        when(queryResult.get(0)).thenReturn(map);
        Date date = new Date();
        when(map.get("label")).thenReturn("test");
        when(map.get("end_time")).thenReturn(date);
        when(map.get("state")).thenReturn("DRAFT");
        when(map.get("modification_time")).thenReturn(date);
        when(map.get("start_time")).thenReturn(date);
        when(map.get("creation_time")).thenReturn(date);
        when(map.get("app_name")).thenReturn("app");
        when(map.get("sampling_percent")).thenReturn(1.0);
        result = repository.getExperiment(id);
        assertThat(result.getID(), is(id));
        assertThat(result.getLabel().toString(), is("test"));
        assertThat(result.getApplicationName().toString(), is("app"));
        assertThat(result.getSamplingPercent(), is(1.0));
        assertThat(result.getState(), is(Experiment.State.DRAFT));
        assertThat(result.getStartTime(), is(date));
        assertThat(result.getEndTime(), is(date));
        assertThat(result.getCreationTime(), is(date));
        assertThat(result.getModificationTime(), is(date));
    }

    @Test
    public void testGetExperimentWithLabel() throws IOException {
        Application.Name appName = Application.Name.valueOf("testApp");
        Experiment.Label label = Experiment.Label.valueOf("testLabel");
        Experiment.ID id = Experiment.ID.newInstance();
        List queryResult = mock(List.class);
        when(transaction.select(anyString(), eq(appName.toString()), eq(label.toString()),
                eq(Experiment.State.DELETED.toString()))).thenReturn(queryResult);
        when(queryResult.isEmpty()).thenReturn(true);
        Experiment result = repository.getExperiment(appName, label);
        assertThat(result, is(nullValue()));
        //result size > 1
        when(queryResult.isEmpty()).thenReturn(false);
        when(queryResult.size()).thenReturn(2);
        given(transaction.select(anyString(), eq(appName.toString()), eq(label.toString()),
                eq(Experiment.State.DELETED.toString()))).willReturn(queryResult);
        BDDCatchException.when(repository).getExperiment(appName, label);
        BDDCatchException.then(caughtException())
                .isInstanceOf(RepositoryException.class)
                .hasMessage("Could not retrieve experiment \"" + label + "\"")
                .hasRootCauseExactlyInstanceOf(IllegalStateException.class);
        //rest of the happy case
        when(queryResult.size()).thenReturn(1);
        Map map = mock(Map.class);
        when(queryResult.get(0)).thenReturn(map);
        Date date = new Date();
        ByteArrayOutputStream ba = new ByteArrayOutputStream(16);
        DataOutputStream da = new DataOutputStream(ba);
        da.writeLong(id.getRawID().getMostSignificantBits());
        da.writeLong(id.getRawID().getLeastSignificantBits());
        when(map.get("id")).thenReturn(ba.toByteArray());
        when(map.get("label")).thenReturn("test");
        when(map.get("end_time")).thenReturn(date);
        when(map.get("state")).thenReturn("DRAFT");
        when(map.get("modification_time")).thenReturn(date);
        when(map.get("start_time")).thenReturn(date);
        when(map.get("creation_time")).thenReturn(date);
        when(map.get("app_name")).thenReturn("app");
        when(map.get("sampling_percent")).thenReturn(1.0);
        result = repository.getExperiment(appName, label);
        assertThat(result.getID(), is(id));
        assertThat(result.getLabel().toString(), is("test"));
        assertThat(result.getApplicationName().toString(), is("app"));
        assertThat(result.getSamplingPercent(), is(1.0));
        assertThat(result.getState(), is(Experiment.State.DRAFT));
        assertThat(result.getStartTime(), is(date));
        assertThat(result.getEndTime(), is(date));
        assertThat(result.getCreationTime(), is(date));
        assertThat(result.getModificationTime(), is(date));
    }

    @Test
    public void testGetExperiments() throws IOException {
        List queryResult = new ArrayList();
        Map map = mock(Map.class);
        queryResult.add(map);
        Experiment.ID id = Experiment.ID.newInstance();
        ByteArrayOutputStream ba = new ByteArrayOutputStream(16);
        DataOutputStream da = new DataOutputStream(ba);
        da.writeLong(id.getRawID().getMostSignificantBits());
        da.writeLong(id.getRawID().getLeastSignificantBits());
        when(map.get("id")).thenReturn(ba.toByteArray());
        when(transaction.select(anyString(), eq(Experiment.State.DELETED.toString()))).thenReturn(queryResult);
        List<Experiment.ID> result = repository.getExperiments();
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(id));

        doThrow(new DatabaseException("Test")).when(transaction).select(anyString(),
                eq(Experiment.State.DELETED.toString()));
        BDDCatchException.when(repository).getExperiments();
        BDDCatchException.then(caughtException())
                .isInstanceOf(WasabiException.class)
                .hasMessage("Test")
                .hasNoCause();

        doThrow(new RuntimeException("runtime exception")).when(transaction).select(anyString(),
                eq(Experiment.State.DELETED.toString()));
        BDDCatchException.when(repository).getExperiments();
        BDDCatchException.then(caughtException())
                .isInstanceOf(Exception.class)
                .hasMessage("Could not retrieve experiment IDs")
                .hasRootCauseExactlyInstanceOf(RuntimeException.class);

    }

    @Test
    public void testGetExperimentsWithListOfExperimentIDs() {
        DatabaseExperimentRepository repository = spy(new DatabaseExperimentRepository(transactionFactory,
                experimentValidator, flyway));
        List<Experiment.ID> list = Arrays.asList(Experiment.ID.newInstance());
        Experiment experiment = mock(Experiment.class);
        doReturn(experiment).when(repository).getExperiment(any(Experiment.ID.class));
        ExperimentList result = repository.getExperiments(list);
        assertThat(result.getExperiments().size(), is(1));
        assertThat(result.getExperiments().get(0), is(experiment));
    }

    @Test
    public void testUnsupportedMethods() throws ConnectionException {
        BDDCatchException.when(repository).getExperimentList(Application.Name.valueOf("TestApp"));
        BDDCatchException.then(caughtException())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported ")
                .hasNoCause();

        BDDCatchException.when(repository).getApplicationsList();
        BDDCatchException.then(caughtException())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported ")
                .hasNoCause();

        BDDCatchException.when(repository).getBucket(Experiment.ID.newInstance(), Bucket.Label.valueOf("TestLabel"));
        BDDCatchException.then(caughtException())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported yet.")
                .hasNoCause();

        BDDCatchException.when(repository).getExperiments(Application.Name.valueOf("TestApp"));
        BDDCatchException.then(caughtException())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported ")
                .hasNoCause();

//        BDDCatchException.when(repository).createIndicesForNewExperiment(
//                NewExperiment.withID(Experiment.ID.newInstance())
//                        .withSamplingPercent(0.5).build());
//        BDDCatchException.then(caughtException())
//                .isInstanceOf(UnsupportedOperationException.class)
//                .hasMessage("No support for sql - indices are only created in Cassandra")
//                .hasNoCause();

        BDDCatchException.when(repository).getBucketList(Collections.<Experiment.ID>emptyList());
        BDDCatchException.then(caughtException())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported ")
                .hasNoCause();

        BDDCatchException.when(repository).getBucketList(Experiment.ID.newInstance());
        BDDCatchException.then(caughtException())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported ")
                .hasNoCause();

        BDDCatchException.when(repository).updateStateIndex(null);
        BDDCatchException.then(caughtException())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported ")
                .hasNoCause();

//        BDDCatchException.when(repository).getExperimentRows(Application.Name.valueOf("App"));
//        BDDCatchException.then(caughtException())
//                .isInstanceOf(UnsupportedOperationException.class)
//                .hasMessage("Not supported ")
//                .hasNoCause();
    }

    @Test
    public void testCreateExperiment() throws Exception {
        Experiment.ID id = Experiment.ID.newInstance();
        NewExperiment mockedNewExperiment = NewExperiment.withID(id).withSamplingPercent(1.0)
                .withLabel(Experiment.Label.valueOf("testLabel"))
                .withStartTime(new Date())
                .withDescription("TEST")
                .withEndTime(new Date())
                .withAppName(Application.Name.valueOf("TestApp")).build();

        doNothing().when(transaction).insert(anyString(), Matchers.anyVararg());
        Experiment.ID result = repository.createExperiment(mockedNewExperiment);
        assertThat(result, is(id));

        doThrow(new DatabaseException("Test")).when(transaction).insert(anyString(), Matchers.anyVararg());
        BDDCatchException.when(repository).createExperiment(mockedNewExperiment);
        BDDCatchException.then(caughtException())
                .isInstanceOf(WasabiException.class)
                .hasMessage("Test")
                .hasNoCause();

        doThrow(new RuntimeException("RTE")).when(transaction).insert(anyString(), Matchers.anyVararg());
        BDDCatchException.when(repository).createExperiment(mockedNewExperiment);
        BDDCatchException.then(caughtException())
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Could not persist experiment in database")
                .hasRootCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void testUpdateExperiment() {
        Experiment mockedExperiment = mock(Experiment.class);
        when(mockedExperiment.getState()).thenReturn(Experiment.State.DELETED);
        when(mockedExperiment.getLabel()).thenReturn(Experiment.Label.valueOf("testLabel"));
        when(mockedExperiment.getApplicationName()).thenReturn(Application.Name.valueOf("TestApp"));
        doNothing().when(experimentValidator).validateExperiment(eq(mockedExperiment));
        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(1);
        Experiment result = repository.updateExperiment(mockedExperiment);
        assertThat(result, is(mockedExperiment));

        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(2);
        BDDCatchException.when(repository).updateExperiment(mockedExperiment);
        BDDCatchException.then(caughtException())
                .isInstanceOf(RepositoryException.class)
                .hasMessage("Concurrent updates; please retry")
                .hasNoCause();


        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(0);
        BDDCatchException.when(repository).updateExperiment(mockedExperiment);
        BDDCatchException.then(caughtException())
                .isInstanceOf(RepositoryException.class)
                .hasMessage("No rows were updated")
                .hasNoCause();
    }

    @Test
    public void testUpdateExperimentState() {
        Experiment mockedExperiment = mock(Experiment.class);
        Experiment.State state = Experiment.State.DRAFT;
        doNothing().when(experimentValidator).validateExperiment(eq(mockedExperiment));
        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(1);
        Experiment result = repository.updateExperimentState(mockedExperiment, state);
        assertThat(result, is(mockedExperiment));

        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(2);
        BDDCatchException.when(repository).updateExperimentState(mockedExperiment, state);
        BDDCatchException.then(caughtException())
                .isInstanceOf(RepositoryException.class)
                .hasMessage("Concurrent updates; please retry")
                .hasNoCause();


        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(0);
        BDDCatchException.when(repository).updateExperimentState(mockedExperiment, state);
        BDDCatchException.then(caughtException())
                .isInstanceOf(RepositoryException.class)
                .hasMessage("No rows were updated")
                .hasNoCause();
    }

    @Test
    public void testCreateBucket() throws Exception {
        Bucket bucket = mock(Bucket.class, RETURNS_DEEP_STUBS);
        doNothing().when(transaction).insert(anyString(), Matchers.anyVararg());
        repository.createBucket(bucket);
        verify(transaction, atLeastOnce()).insert(anyString(), Matchers.anyVararg());

        doThrow(new DatabaseException("db error")).when(transaction).insert(anyString(), Matchers.anyVararg());
        BDDCatchException.when(repository).createBucket(bucket);
        BDDCatchException.then(caughtException())
                .isInstanceOf(WasabiException.class)
                .hasMessage("db error")
                .hasNoCause();

        doThrow(new RuntimeException("RTE")).when(transaction).insert(anyString(), Matchers.anyVararg());
        BDDCatchException.when(repository).createBucket(bucket);
        BDDCatchException.then(caughtException())
                .isInstanceOf(WasabiException.class)
                .hasMessageContaining("Could not create bucket")
                .hasRootCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void testUpdateBucket() {
        Bucket bucket = mock(Bucket.class, RETURNS_DEEP_STUBS);
        given(bucket.getLabel()).willReturn(Label.valueOf("l1"));
        assertThat(bucket.getLabel().toString().trim().isEmpty(), is(not(nullValue())));

        when(bucket.getAllocationPercent()).thenReturn(0.5);
        when(transaction.update(anyString(), Matchers.<Object>anyVararg())).thenReturn(1);
        when(bucket.isControl()).thenReturn(false);

        Bucket result = repository.updateBucket(bucket);

        assertThat(result, is(bucket));

        verify(transaction, times(1)).update(anyString(), Matchers.<Object>anyVararg());
        when(bucket.isControl()).thenReturn(true);
        result = repository.updateBucket(bucket);
        assertThat(result, is(bucket));
        verify(transaction, times(3)).update(anyString(), Matchers.<Object>anyVararg());
    }

    @Test
    public void testUpdateBucketAllocationPercentage() {
        Bucket bucket = mock(Bucket.class, RETURNS_DEEP_STUBS);
        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(1);
        Bucket result = repository.updateBucketAllocationPercentage(bucket, 1.0);
        assertThat(result, is(bucket));
    }

    @Test
    public void testUpdateBucketState() {
        Bucket bucket = mock(Bucket.class, RETURNS_DEEP_STUBS);
        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(1);
        Bucket result = repository.updateBucketState(bucket, Bucket.State.OPEN);
        assertThat(result, is(bucket));
    }

    @Test
    public void testUpdateBucketBatch() {
        BucketList bucketList = mock(BucketList.class, RETURNS_DEEP_STUBS);
//        List<Bucket> list = mock(List.class, RETURNS_DEEP_STUBS);
        List<Bucket> list = new ArrayList<>();
        when(list.size()).thenReturn(1);
        Bucket bucket = mock(Bucket.class, RETURNS_DEEP_STUBS);
        list.add(bucket);

        when(bucket.getState()).thenReturn(Bucket.State.OPEN);
        when(bucket.getPayload()).thenReturn("payload");
        when(bucket.getDescription()).thenReturn("description");
//        when(list.get(0)).thenReturn(bucket);
        when(bucketList.getBuckets()).thenReturn(list);
        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(1);
        BucketList result = repository.updateBucketBatch(Experiment.ID.newInstance(), bucketList);
        assertThat(result, is(bucketList));
        verify(transaction, times(1)).update(anyString(), Matchers.anyVararg());
    }

    @Test
    public void testDeleteBucket() {
        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(0);
        BDDCatchException.when(repository).deleteBucket(Experiment.ID.newInstance(), Bucket.Label.valueOf("label"));
        BDDCatchException.then(caughtException())
                .isInstanceOf(BucketNotFoundException.class)
                .hasMessageContaining("Bucket \"label\" not found")
                .hasNoCause();
    }

    @Test
    public void testDeleteExperiment() {
        when(transaction.update(anyString(), Matchers.anyVararg())).thenReturn(0);
        BDDCatchException.when(repository).deleteExperiment(
                NewExperiment.withID(Experiment.ID.newInstance())
                        .withDescription("TEST")
                        .withStartTime(new Date()).withEndTime(new Date())
                        .withSamplingPercent(0.5).withLabel(Experiment.Label.valueOf("l1")).build());
        BDDCatchException.then(caughtException())
                .isInstanceOf(ExperimentNotFoundException.class)
                .hasMessageContaining("Experiment")
                .hasNoCause();
    }

    @Test
    public void testEmptyMethods() {
        repository.logBucketChanges(Experiment.ID.newInstance(), Bucket.Label.valueOf("lable"),
                new ArrayList<Bucket.BucketAuditInfo>());
        repository.logExperimentChanges(Experiment.ID.newInstance(), new ArrayList<Experiment.ExperimentAuditInfo>());
    }

    @Test
    public void testGetBuckets() {
        Experiment.ID id = Experiment.ID.newInstance();
        List firstQueryResult = mock(List.class);
        when(transaction.select(anyString(), eq(id), eq(DELETED.toString()))).thenReturn(firstQueryResult);
        when(firstQueryResult.size()).thenReturn(0);
        BDDCatchException.when(repository).getBuckets(id, false);
        BDDCatchException.then(caughtException())
                .isInstanceOf(ExperimentNotFoundException.class)
                .hasMessageContaining("Experiment")
                .hasNoCause();
        verify(transaction, times(1)).select(anyString(), Matchers.anyVararg());

        when(firstQueryResult.size()).thenReturn(1);
        List secondQueryResult = new ArrayList();
        Map queryMap = mock(Map.class, RETURNS_DEEP_STUBS);
        secondQueryResult.add(queryMap);
        when(queryMap.get("label")).thenReturn("label");
        when(queryMap.get("allocation_percent")).thenReturn(1.0);
        when(queryMap.get("description")).thenReturn("description");
        when(queryMap.get("is_control")).thenReturn(true);
        when(queryMap.get("payload")).thenReturn("payload");
        when(transaction.select(anyString(), eq(id))).thenReturn(secondQueryResult);
        BucketList result = repository.getBuckets(id, false);
        assertThat(result, is(not(nullValue())));
        assertThat(result.getBuckets().size(), is(1));
        assertThat(result.getBuckets().get(0).getExperimentID(), is(id));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedTagMethod() {
        repository.getTagListForApplications(Collections.EMPTY_LIST);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedApplicationMethod() {
        repository.createApplication(Application.Name.valueOf("NotSupported"));
    }
}
