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

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBatch;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.BucketAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExclusionAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.StagingAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.count.BucketAssignmentCountAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.export.UserAssignmentExportAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentUserIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.PageExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentUserByUserIdContextAppNameExperimentId;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CassandraAssignmentsRepositoryTest {
    private final Logger logger = LoggerFactory.getLogger(CassandraAssignmentsRepositoryTest.class);
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    ExperimentRepository experimentRepository;
    @Mock
    ExperimentRepository dbRepository;
    @Mock
    EventLog eventLog;

    @Mock
    ExperimentAccessor experimentAccessor;
    @Mock
    ExperimentUserIndexAccessor experimentUserIndexAccessor;

    @Mock
    UserAssignmentExportAccessor userAssignmentExportAccessor;

    @Mock
    BucketAccessor bucketAccessor;
    @Mock
    BucketAssignmentCountAccessor bucketAssignmentCountAccessor;

    @Mock
    StagingAccessor stagingAccessor;
    @Mock
    PrioritiesAccessor prioritiesAccessor;
    @Mock
    ExclusionAccessor exclusionAccessor;
    @Mock
    PageExperimentIndexAccessor pageExperimentIndexAccessor;

    @Mock
    CassandraDriver driver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MappingManager mappingManager;
    @Mock
    Result mockedResultMapping;

    @Mock
    ThreadPoolExecutor assignmentsCountExecutor;

    CassandraAssignmentsRepository repository;
    CassandraAssignmentsRepository spyRepository;
    UUID experimentId = UUID.fromString("4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8");

    public static final Application.Name APPLICATION_NAME = Application.Name.valueOf("testApp");

    @Before
    public void setUp() throws Exception {
        repository = new CassandraAssignmentsRepository(
                experimentRepository,
                dbRepository,
                eventLog,
                experimentAccessor,
                experimentUserIndexAccessor,
                userAssignmentExportAccessor,
                bucketAccessor,
                bucketAssignmentCountAccessor,
                stagingAccessor,
                prioritiesAccessor,
                exclusionAccessor,
                pageExperimentIndexAccessor,
                driver,
                mappingManager,
                assignmentsCountExecutor,
                true,
                true,
                "yyyy-MM-dd HH:mm:ss"
        );
        spyRepository = spy(repository);
    }

    @Test
    public void testGetAssignmentsMultiple() {
        Experiment.ID expId1 = Experiment.ID.newInstance();
        Experiment.ID expId2 = Experiment.ID.newInstance();
        Date endTime = new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000);

        Experiment exp1 = Experiment.withID(expId1).withEndTime(endTime)
                .withLabel(Experiment.Label.valueOf("Exp1")).build();
        Experiment exp2 = Experiment.withID(expId2).withEndTime(endTime)
                .withLabel(Experiment.Label.valueOf("Exp2")).build();

        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(expId1.getRawID())
                .context("test")
                .bucket("bucket1")
                .build()
        );
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(expId2.getRawID())
                .context("test")
                .bucket("bucket2")
                .build()
        );

        Map<Experiment.ID, Experiment> experimentMap = newHashMap();
        experimentMap.put(expId1, exp1);
        experimentMap.put(expId2, exp2);

        doReturn(mocked.stream()).when(spyRepository).getUserIndexStream(anyString(), anyString(), anyString());

        List<Pair<Experiment, String>> result = spyRepository.getAssignments(User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"),
                experimentMap
        );

        //Verif the result
        assertThat(result.size(), is(2));
        result.forEach(pair -> {
            assertThat(mocked.stream().map(t -> t.getExperimentId()).collect(Collectors.toList()), hasItems(pair.getLeft().getID().getRawID()));
        });

    }

    @Test
    public void testGetAssignmentsSingle() {
        Experiment.ID expId1 = Experiment.ID.newInstance();
        Date endTime = new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000);

        Experiment exp1 = Experiment.withID(expId1).withEndTime(endTime)
                .withLabel(Experiment.Label.valueOf("Exp1")).build();

        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(expId1.getRawID())
                .context("test")
                .bucket("bucket1")
                .build()
        );

        Map<Experiment.ID, Experiment> experimentMap = newHashMap();
        experimentMap.put(expId1, exp1);

        doReturn(mocked.stream()).when(spyRepository).getUserIndexStream(anyString(), anyString(), anyString());
        List<Pair<Experiment, String>> result = spyRepository.getAssignments(User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"),
                experimentMap
        );
        assertThat(result.size(), is(1));
        result.forEach(pair -> {
            assertThat(mocked.stream().map(t -> t.getExperimentId()).collect(Collectors.toList()), hasItems(pair.getLeft().getID().getRawID()));
        });
    }

    @Test
    public void testGetAssignmentsSingleNoMatch() {
        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(experimentId)
                .context("test")
                .bucket("bucket1")
                .build()
        );

        Map<Experiment.ID, Experiment> experimentMap = newHashMap();

        Table<Experiment.ID, Experiment.Label, Experiment> experimentTable = HashBasedTable.create();
        experimentTable.put(Experiment.ID.valueOf(UUID.randomUUID()),
                Experiment.Label.valueOf("test-" + experimentId.toString()),
                Experiment.withID(Experiment.ID.valueOf(experimentId))
                        .withLabel(Experiment.Label.valueOf("test-bucket")).build()
        );
        doReturn(mocked.stream()).when(spyRepository).getUserIndexStream(anyString(), anyString(), anyString());
        List<Pair<Experiment, String>> result = spyRepository.getAssignments(User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"),
                experimentMap
        );
        assertThat(result.size(), is(0));
    }

    @Test
    public void testGetAssignmentsEmptyResult() {
        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        Table<Experiment.ID, Experiment.Label, Experiment> experimentTable = HashBasedTable.create();
        Map<Experiment.ID, Experiment> experimentMap = newHashMap();

        doReturn(mocked.stream()).when(spyRepository).getUserIndexStream(anyString(), anyString(), anyString());
        List<Pair<Experiment, String>> result = spyRepository.getAssignments(User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"),
                experimentMap
        );
        assertThat(result.size(), is(0));
    }

    @Test
    public void testGetAssignmentFromStreamMultiple() {
        List<Assignment.Builder> mockedResult = new ArrayList<>();
        mockedResult.add(Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withBucketLabel(Bucket.Label.valueOf("label1"))
        );
        mockedResult.add(Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withBucketLabel(Bucket.Label.valueOf("label1"))
        );
        Bucket mockedBucket = Bucket.newInstance(Experiment.ID.valueOf(experimentId), Bucket.Label.valueOf("label1"))
                .withAllocationPercent(1.0)
                .withState(Bucket.State.EMPTY)
                .build();
        when(experimentRepository.getBucket(eq(Experiment.ID.valueOf(experimentId)), eq(Bucket.Label.valueOf("label1"))))
                .thenReturn(mockedBucket);
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Multiple element fetched from db for experimentId = \"4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8\" userID = \"testuser1 context=\"test\"");
        repository.getAssignmentFromStream(Experiment.ID.valueOf(experimentId), User.ID.valueOf("testuser1"), Context.valueOf("test"), mockedResult.stream());
    }

    @Test
    public void testGetAssignmentFromStreamSingleStateEmpty() {
        List<Assignment.Builder> mockedResult = new ArrayList<>();
        mockedResult.add(Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withBucketLabel(Bucket.Label.valueOf("label1"))
        );
        Bucket mockedBucket = Bucket.newInstance(Experiment.ID.valueOf(experimentId), Bucket.Label.valueOf("label1"))
                .withAllocationPercent(1.0)
                .withState(Bucket.State.EMPTY)
                .build();
        when(experimentRepository.getBucket(eq(Experiment.ID.valueOf(experimentId)), eq(Bucket.Label.valueOf("label1"))))
                .thenReturn(mockedBucket);
        Optional<Assignment> assignmentOptional = repository.getAssignmentFromStream(
                Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"),
                mockedResult.stream());
        assertThat(assignmentOptional.isPresent(), is(true));
        assertThat(assignmentOptional.get().getBucketLabel(), is(nullValue()));
        assertThat(assignmentOptional.get().isCacheable(), is(false));
        assertThat(assignmentOptional.get().isBucketEmpty(), is(true));
    }

    @Test
    public void testGetAssignmentFromStreamSingleStateNotEmpty() {
        List<Assignment.Builder> mockedResult = new ArrayList<>();
        mockedResult.add(Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withBucketLabel(Bucket.Label.valueOf("label1"))
        );
        Bucket mockedBucket = Bucket.newInstance(Experiment.ID.valueOf(experimentId), Bucket.Label.valueOf("label1"))
                .withAllocationPercent(1.0)
                .withState(Bucket.State.OPEN)
                .build();
        when(experimentRepository.getBucket(eq(Experiment.ID.valueOf(experimentId)), eq(Bucket.Label.valueOf("label1"))))
                .thenReturn(mockedBucket);
        Optional<Assignment> assignmentOptional = repository.getAssignmentFromStream(
                Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"),
                mockedResult.stream());
        assertThat(assignmentOptional.isPresent(), is(true));
        assertThat(assignmentOptional.get().getBucketLabel().toString(), is("label1"));
        assertThat(assignmentOptional.get().isCacheable(), is(false));
        assertThat(assignmentOptional.get().isBucketEmpty(), is(false));
    }

    @Test
    public void testGetAssignmentFromStreamEmpty() {
        List<Assignment.Builder> mockedResult = new ArrayList<>();
        Bucket mockedBucket = Bucket.newInstance(Experiment.ID.valueOf(experimentId), Bucket.Label.valueOf("label1"))
                .withAllocationPercent(1.0)
                .withState(Bucket.State.OPEN)
                .build();
        when(experimentRepository.getBucket(eq(Experiment.ID.valueOf(experimentId)), eq(Bucket.Label.valueOf("label1"))))
                .thenReturn(mockedBucket);
        Optional<Assignment> assignmentOptional = repository.getAssignmentFromStream(
                Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"),
                mockedResult.stream());
        assertThat(assignmentOptional.isPresent(), is(false));
    }

    @Test
    public void testAssignmentExportWriteException() {
        Assignment expected = Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withBucketLabel(Bucket.Label.valueOf("bucket-1"))
                .withContext(Context.valueOf("test"))
                .withCreated(new Date())
                .withUserID(User.ID.valueOf("testuser1"))
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCacheable(false)
                .build();
        doThrow(WriteTimeoutException.class)
                .when(userAssignmentExportAccessor)
                .insertBy(eq(expected.getExperimentID().getRawID()),
                        eq(expected.getUserID().toString()),
                        eq(expected.getContext().getContext()),
                        eq(expected.getCreated()),
                        any(Date.class),
                        any(String.class),
                        any(Boolean.class));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not save user assignment in user_assignment_export");
        repository.assignUserToExports(expected, expected.getCreated());
    }


    @Test
    public void testAssignExportWithLabel() {
        Assignment expected = Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withBucketLabel(Bucket.Label.valueOf("bucket-1"))
                .withContext(Context.valueOf("test"))
                .withCreated(new Date())
                .withUserID(User.ID.valueOf("testuser1"))
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCacheable(false)
                .build();
        repository.assignUserToExports(expected, expected.getCreated());
        verify(userAssignmentExportAccessor, times(1)).insertBy(
                eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()),
                any(Date.class),
                eq(expected.getBucketLabel().toString()),
                eq(false));
    }

    @Test
    public void testAssignExportWihtoutLabel() {
        Assignment expected = Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withContext(Context.valueOf("test"))
                .withCreated(new Date())
                .withUserID(User.ID.valueOf("testuser1"))
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCacheable(false)
                .build();
        repository.assignUserToExports(expected, expected.getCreated());
        verify(userAssignmentExportAccessor, times(1)).insertBy(
                eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()),
                any(Date.class),
                eq("NO_ASSIGNMENT"),
                eq(true));
    }

    @Test
    public void testGetUserAssignmentPartitions() {
        Date date1 = new Date(116, 7, 1);
        Date date2 = new Date(116, 7, 2, 1, 0);
        List<Date> result = repository.getUserAssignmentPartitions(date1, date2);
        assertThat(result.size(), is(26));
        result = repository.getUserAssignmentPartitions(date2, date1);
        assertThat(result.size(), is(0));
    }

    @Test
    public void testGetDateHourRangeListNoExperimentException() {
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Parameters parameters = mock(Parameters.class);
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(null);
        thrown.expect(ExperimentNotFoundException.class);
        thrown.expectMessage("Experiment \"4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8\" not found");
        repository.getDateHourRangeList(experimentId, parameters);
    }

    @Test
    public void testGetDateHourRangeListNoFromAndToDate() {
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Parameters parameters = mock(Parameters.class);
        Experiment experimentResult = mock(Experiment.class);
        when(parameters.getFromTime()).thenReturn(null);
        when(parameters.getToTime()).thenReturn(null);
        when(experimentResult.getCreationTime()).thenReturn(new Date());
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(experimentResult);
        List<Date> result = repository.getDateHourRangeList(experimentId, parameters);
        assertThat(result.size(), is(1));
    }

    @Test
    public void testGetDateHourRangeListWithFromAndToDate() {
        Date fromDate = new Date(1469084400000L);
        Date toDate = new Date(1469098800000L);
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Parameters parameters = mock(Parameters.class);
        Experiment experimentResult = mock(Experiment.class);
        when(parameters.getFromTime()).thenReturn(fromDate);
        when(parameters.getToTime()).thenReturn(toDate);
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(experimentResult);
        List<Date> result = repository.getDateHourRangeList(experimentId, parameters);
        assertThat(result.size(), is(5));
    }

    @Test
    public void testGetDateHourRangeListWithFromAndToDateReversed() {
        Date fromDate = new Date(1469084400000L);
        Date toDate = new Date(1469098800000L);
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Parameters parameters = mock(Parameters.class);
        Experiment experimentResult = mock(Experiment.class);
        when(parameters.getFromTime()).thenReturn(toDate);
        when(parameters.getToTime()).thenReturn(fromDate);
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(experimentResult);
        List<Date> result = repository.getDateHourRangeList(experimentId, parameters);
        assertThat(result.size(), is(0));
    }

    @Test
    public void testGetAssignmentStreamNoExperimentExceptionIgnoreNullBuckets() {
        List<Date> dateArrayList = new ArrayList<>();
        dateArrayList.add(new Date(1469084400000L));
        dateArrayList.add(new Date(1469088000000L));
        dateArrayList.add(new Date(1469091600000L));
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Context context = Context.valueOf("test");
        Parameters mockParameters = mock(Parameters.class);
        Experiment mockExperiment = mock(Experiment.class);
        doReturn(dateArrayList).when(spyRepository).getDateHourRangeList(eq(experimentId), eq(mockParameters));
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(mockExperiment);
        StreamingOutput result = spyRepository.getAssignmentStream(experimentId, context, mockParameters, false);
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void testRemoveIndexExperimentsToUser() {
        repository.removeIndexExperimentsToUser(User.ID.valueOf("testuser1"),
                Experiment.ID.valueOf(this.experimentId),
                Context.valueOf("test"),
                APPLICATION_NAME);
        verify(experimentUserIndexAccessor, times(1))
                .deleteBy(eq("testuser1"), eq(this.experimentId), eq("test"), eq(APPLICATION_NAME.toString()));
    }

    @Test
    public void testRemoveIndexExperimentsToUserWriteException() {
        doThrow(WriteTimeoutException.class).when(experimentUserIndexAccessor)
                .deleteBy(eq("testuser1"), eq(this.experimentId), eq("test"), eq(APPLICATION_NAME.toString()));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not delete index from experiment_user_index for user: testuser1to experiment: 4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8");
        repository.removeIndexExperimentsToUser(User.ID.valueOf("testuser1"),
                Experiment.ID.valueOf(this.experimentId),
                Context.valueOf("test"),
                APPLICATION_NAME);
    }

    @Test
    public void testGetBatchPayloadsFromStaging(){

        int i;
        String message = "message";
        String time = "time";
        List<Row> mockRows = new ArrayList<Row>();
        for(i=0;i<10;i++){
            mockRows.add(mock(Row.class,message+i));
            long ms = new Date().getTime();
            when(mockRows.get(i).get("time", UUID.class)).thenReturn(new UUID(ms+i, ms+i+1));
            when(mockRows.get(i).getString("msg")).thenReturn(message+i);
        }

        ResultSet resultSet = mock(ResultSet.class);
        when(stagingAccessor.batchSelectBy(anyInt())).thenReturn(resultSet);
        when(resultSet.all()).thenReturn(mockRows);

        Map<UUID, String> payloads = repository.getBatchPayloadsFromStaging(10);
        assertThat(payloads.keySet().size(), is(10));
    }

    @Test()
    public void testDeleteFromStaging(){

        when(stagingAccessor.deleteBy(any())).thenReturn(null);
        repository.deleteFromStaging(UUID.randomUUID());
    }

    @Test
    public void testPushAssignmentToStaging() {
        repository.pushAssignmentToStaging("type", "string1", "string2");
        verify(stagingAccessor, times(1))
                .insertBy(eq("type"), eq("string1"), eq("string2"));
    }

    @Test
    public void testPushAssignmentsToStaging() {
        //------ Input
        List<String> messages = newArrayList("Msg1", "Msg2", "Msg3");

        //------ Mocking interacting calls
        ResultSet genericResultSet = mock(ResultSet.class);
        when(driver.getSession()).thenReturn(mock(Session.class));
        when(driver.getSession().execute(any(BatchStatement.class))).thenReturn(genericResultSet);

        //----- Actual call
        repository.pushAssignmentsToStaging("type", "string1", messages);

        //----- Verify result
        verify(stagingAccessor, times(3)).batchInsertBy(any(UUID.class), eq("type"), eq("string1"), any(String.class));
    }

    @Test
    public void testPushAssignmentsToStagingWriteException() {
        //------ Input
        List<String> messages = newArrayList("Msg1", "Msg2", "Msg3");

        //------ Mocking interacting calls
        doThrow(WriteTimeoutException.class).when(stagingAccessor).batchInsertBy(any(UUID.class), eq("type"), eq("string1"), any(String.class));
        ResultSet genericResultSet = mock(ResultSet.class);
        when(driver.getSession()).thenReturn(mock(Session.class));
        when(driver.getSession().execute(any(BatchStatement.class))).thenReturn(genericResultSet);

        //----- Verify expected exception
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Error occurred while pushAssignmentsToStaging");

        //----- Actual call
        repository.pushAssignmentsToStaging("type", "string1", messages);
    }

    @Test
    public void testPushAssignmentToStagingrWriteException() {
        doThrow(WriteTimeoutException.class).when(stagingAccessor)
                .insertBy(eq("type"), eq("string1"), eq("string2"));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not push the assignment to staging");
        repository.pushAssignmentToStaging("type", "string1", "string2");
    }

    @Test
    public void testUpdateBucketAssignmentCountUp() {
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Experiment mockedExperiment = mock(Experiment.class);
        when(mockedExperiment.getID()).thenReturn(experimentId);
        Bucket.Label bucketLabel = Bucket.Label.valueOf("testBucket");
        Assignment mockedAssignment = mock(Assignment.class);
        when(mockedAssignment.getBucketLabel()).thenReturn(bucketLabel);
        repository.updateBucketAssignmentCount(mockedExperiment, mockedAssignment, true);
        verify(bucketAssignmentCountAccessor, times(1))
                .incrementCountBy(eq(this.experimentId), eq(bucketLabel.toString()));
        verify(bucketAssignmentCountAccessor, times(0))
                .decrementCountBy(eq(this.experimentId), eq(bucketLabel.toString()));
    }

    @Test
    public void testUpdateBucketAssignmentCountDown() {
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Experiment mockedExperiment = mock(Experiment.class);
        when(mockedExperiment.getID()).thenReturn(experimentId);
        Bucket.Label bucketLabel = Bucket.Label.valueOf("testBucket");
        Assignment mockedAssignment = mock(Assignment.class);
        when(mockedAssignment.getBucketLabel()).thenReturn(bucketLabel);
        repository.updateBucketAssignmentCount(mockedExperiment, mockedAssignment, false);
        verify(bucketAssignmentCountAccessor, times(0))
                .incrementCountBy(eq(this.experimentId), eq(bucketLabel.toString()));
        verify(bucketAssignmentCountAccessor, times(1))
                .decrementCountBy(eq(this.experimentId), eq(bucketLabel.toString()));
    }

    @Test
    public void testUpdateBucketAssignmentCountrWriteException() {
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Experiment mockedExperiment = mock(Experiment.class);
        when(mockedExperiment.getID()).thenReturn(experimentId);
        Assignment mockedAssignment = mock(Assignment.class);
        doThrow(WriteTimeoutException.class).when(bucketAssignmentCountAccessor)
                .decrementCountBy(any(UUID.class), any(String.class));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not update the bucket count for experiment 4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8 bucket NULL");
        repository.updateBucketAssignmentCount(mockedExperiment, mockedAssignment, false);
    }

    @Test
    public void testIndexExperimentToUserEmptyBucket() {
        Assignment assignment = Assignment.newInstance(Experiment.ID.valueOf(this.experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withContext(Context.valueOf("test"))
                .withUserID(User.ID.valueOf("testuser1"))
                .build();
        repository.indexExperimentsToUser(assignment);
        verify(experimentUserIndexAccessor, times(1))
                .insertBy(eq(assignment.getUserID().toString()),
                        eq(assignment.getContext().getContext()),
                        eq(APPLICATION_NAME.toString()),
                        eq(this.experimentId));
        verify(experimentUserIndexAccessor, times(0))
                .insertBy(eq(assignment.getUserID().toString()),
                        eq(assignment.getContext().getContext()),
                        eq(APPLICATION_NAME.toString()),
                        eq(this.experimentId),
                        any());
    }

    @Test
    public void testIndexExperimentToUserWithBucket() {
        Assignment assignment = Assignment.newInstance(Experiment.ID.valueOf(this.experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withContext(Context.valueOf("test"))
                .withUserID(User.ID.valueOf("testuser1"))
                .withBucketLabel(Bucket.Label.valueOf("bucket1"))
                .build();
        repository.indexExperimentsToUser(assignment);
        verify(experimentUserIndexAccessor, times(0))
                .insertBy(eq(assignment.getUserID().toString()),
                        eq(assignment.getContext().getContext()),
                        eq(APPLICATION_NAME.toString()),
                        eq(this.experimentId));
        verify(experimentUserIndexAccessor, times(1))
                .insertBy(eq(assignment.getUserID().toString()),
                        eq(assignment.getContext().getContext()),
                        eq(APPLICATION_NAME.toString()),
                        eq(this.experimentId),
                        eq(assignment.getBucketLabel().toString()));
    }

    @Test
    public void testIndexExperimentToUserWithBucketWriteException() {
        Assignment assignment = Assignment.newInstance(Experiment.ID.valueOf(this.experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withContext(Context.valueOf("test"))
                .withUserID(User.ID.valueOf("testuser1"))
                .withBucketLabel(Bucket.Label.valueOf("bucket1"))
                .build();
        doThrow(WriteTimeoutException.class).when(experimentUserIndexAccessor)
                .insertBy(any(String.class), any(String.class), any(String.class), any(UUID.class), any(String.class));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not index experiment to user");
        repository.indexExperimentsToUser(assignment);
    }

    @Test
    public void testDeleteAssignment() {
        Experiment experiment = Experiment.withID(Experiment.ID.valueOf(this.experimentId)).build();
        User.ID userID = User.ID.valueOf("testuser1");
        Context context = Context.valueOf("test");
        Assignment currentAssignment = Assignment.newInstance(experiment.getID())
                .withBucketLabel(Bucket.Label.valueOf("bucket-1"))
                .build();
        spyRepository.deleteAssignment(experiment, userID, context, APPLICATION_NAME, currentAssignment);
        verify(spyRepository, times(1)).removeIndexExperimentsToUser(
                eq(userID),
                eq(experiment.getID()),
                eq(context),
                eq(APPLICATION_NAME)
        );
    }

    @Test
    public void testBucketAssignmentCountReadException() {
        Experiment experiment = Experiment.withID(Experiment.ID.valueOf(this.experimentId))
                .withIsPersonalizationEnabled(false)
                .withIsRapidExperiment(false).build();
        doThrow(ReadTimeoutException.class).when(bucketAssignmentCountAccessor)
                .selectBy(eq(experiment.getID().getRawID()));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not fetch the bucket assignment counts for experiment");
        repository.getBucketAssignmentCount(experiment);
    }

    @Test
    public void testBucketAssignmentCountNullResult() {
        Experiment experiment = Experiment.withID(Experiment.ID.valueOf(this.experimentId))
                .withIsPersonalizationEnabled(false)
                .withIsRapidExperiment(false).build();
        when(bucketAssignmentCountAccessor.selectBy(eq(experiment.getID().getRawID())))
                .thenReturn(null);
        AssignmentCounts assignmentCounts = repository.getBucketAssignmentCount(experiment);
        assertThat(assignmentCounts.getExperimentID(), is(experiment.getID()));
        assertThat(assignmentCounts.getAssignments().size(), is(1));
        assertThat(assignmentCounts.getAssignments().get(0).getBucket(), is(nullValue()));
        assertThat(assignmentCounts.getAssignments().get(0).getCount(), is(0L));
        assertThat(assignmentCounts.getTotalUsers().getBucketAssignments(), is(0L));
        assertThat(assignmentCounts.getTotalUsers().getNullAssignments(), is(0L));
        assertThat(assignmentCounts.getTotalUsers().getTotal(), is(0L));
    }

    @Test
    public void testBucketAssignmentCount() {
        Experiment experiment = Experiment.withID(Experiment.ID.valueOf(this.experimentId))
                .withIsPersonalizationEnabled(false)
                .withIsRapidExperiment(false).build();
        List<BucketAssignmentCount> mockedList = new ArrayList<>();
        mockedList.add(BucketAssignmentCount.builder()
                .bucketLabel("bucket-1")
                .count(500L)
                .experimentId(this.experimentId)
                .build()
        );
        mockedList.add(BucketAssignmentCount.builder()
                .bucketLabel(null)
                .count(500L)
                .experimentId(this.experimentId)
                .build()
        );
        Result<BucketAssignmentCount> result = mock(Result.class);
        when(bucketAssignmentCountAccessor.selectBy(eq(experiment.getID().getRawID())))
                .thenReturn(result);
        when(result.iterator()).thenReturn(mockedList.iterator());
        AssignmentCounts assignmentCounts = repository.getBucketAssignmentCount(experiment);
        assertThat(assignmentCounts.getExperimentID(), is(experiment.getID()));
        assertThat(assignmentCounts.getAssignments().size(), is(mockedList.size()));
        for (int i = 0; i < mockedList.size(); i++) {
            String label = mockedList.get(i).getBucketLabel();
            if (Objects.isNull(label)) {
                assertThat(assignmentCounts.getAssignments().get(i).getBucket(),
                        is(mockedList.get(i).getBucketLabel()));
            } else {
                assertThat(assignmentCounts.getAssignments().get(i).getBucket(),
                        is(Bucket.Label.valueOf(mockedList.get(i).getBucketLabel())));
            }
            assertThat(assignmentCounts.getAssignments().get(i).getCount(), is(mockedList.get(i).getCount()));
        }
        assertThat(assignmentCounts.getTotalUsers().getBucketAssignments(), is(500L));
        assertThat(assignmentCounts.getTotalUsers().getNullAssignments(), is(500L));
        assertThat(assignmentCounts.getTotalUsers().getTotal(), is(1000L));
    }

    @Test
    public void testPopulateExperimentMetadataSuccessCase() throws ExecutionException, InterruptedException {

        //------ Input --------
        Experiment.ID expId1 = Experiment.ID.newInstance();
        Application.Name appName = Application.Name.valueOf("testApp1");
        User.ID userID = User.ID.valueOf("testUser1");
        Context context = Context.valueOf("TEST");
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        ExperimentBatch experimentBatch = ExperimentBatch.newInstance().withProfile(segmentationProfile.getProfile()).build();
        Map<Experiment.ID, Boolean> allowAssignments = new HashMap<>();
        allowAssignments.put(expId1, true);
        Optional<Map<Experiment.ID, Boolean>> allowAssignmentsOptional = Optional.of(allowAssignments);

        //----- Output -----------
        PrioritizedExperimentList appPriorities = new PrioritizedExperimentList();
        Map<Experiment.ID, com.intuit.wasabi.experimentobjects.Experiment> experimentMap = new HashMap<>();
        Map<Experiment.ID, BucketList> bucketMap = new HashMap<>();
        Map<Experiment.ID, List<Experiment.ID>> exclusionMap = new HashMap<>();

        //------ Mocking interacting calls

        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>> experimentsFuture = mock(ListenableFuture.class);
        when(experimentAccessor.asyncGetExperimentByAppName(appName.toString())).thenReturn(experimentsFuture);
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

        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Application>> applicationFuture = mock(ListenableFuture.class);
        when(prioritiesAccessor.asyncGetPriorities(appName.toString())).thenReturn(applicationFuture);
        List<com.intuit.wasabi.repository.cassandra.pojo.Application> applicationList = new ArrayList<>();
        com.intuit.wasabi.repository.cassandra.pojo.Application app1 = com.intuit.wasabi.repository.cassandra.pojo.Application.builder()
                .appName(appName.toString())
                .priority(expId1.getRawID())
                .build();
        applicationList.add(app1);
        Result<com.intuit.wasabi.repository.cassandra.pojo.Application> applicationResult = mock(Result.class);
        when(applicationResult.all()).thenReturn(applicationList);
        when(applicationFuture.get()).thenReturn(applicationResult);

        ListenableFuture<Result<ExperimentUserByUserIdContextAppNameExperimentId>> userAssignmentFuture = mock(ListenableFuture.class);
        Bucket.Label bucketLabel = Bucket.Label.valueOf("testBucket1");
        when(experimentUserIndexAccessor.asyncSelectBy(userID.toString(), appName.toString(), context.toString())).thenReturn(userAssignmentFuture);
        List<ExperimentUserByUserIdContextAppNameExperimentId> userAssignmentList = new ArrayList<>();
        ExperimentUserByUserIdContextAppNameExperimentId ua1 = ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .experimentId(expId1.getRawID())
                .appName(appName.toString())
                .context(context.toString())
                .bucket(bucketLabel.toString()).build();
        userAssignmentList.add(ua1);
        Result<ExperimentUserByUserIdContextAppNameExperimentId> uaResult = mock(Result.class);
        when(uaResult.all()).thenReturn(userAssignmentList);
        when(userAssignmentFuture.get()).thenReturn(uaResult);

        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket>> bucketFuture = mock(ListenableFuture.class);
        when(bucketAccessor.asyncGetBucketByExperimentId(expId1.getRawID())).thenReturn(bucketFuture);
        List<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketList = new ArrayList<>();
        com.intuit.wasabi.repository.cassandra.pojo.Bucket bucket1 = com.intuit.wasabi.repository.cassandra.pojo.Bucket.builder()
                .label(bucketLabel.toString())
                .state(Bucket.State.OPEN.toString())
                .experimentId(expId1.getRawID())
                .build();
        bucketList.add(bucket1);
        Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketResult = mock(Result.class);
        when(bucketResult.all()).thenReturn(bucketList);
        when(bucketFuture.get()).thenReturn(bucketResult);

        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion>> exclusionFuture = mock(ListenableFuture.class);
        when(exclusionAccessor.asyncGetExclusions(expId1.getRawID())).thenReturn(exclusionFuture);
        List<com.intuit.wasabi.repository.cassandra.pojo.Exclusion> exclusionList = new ArrayList<>();
        Experiment.ID exclusionExpId1 = Experiment.ID.newInstance();
        com.intuit.wasabi.repository.cassandra.pojo.Exclusion exclusion1 = com.intuit.wasabi.repository.cassandra.pojo.Exclusion.builder().base(expId1.getRawID()).pair(exclusionExpId1.getRawID()).build();
        exclusionList.add(exclusion1);
        Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion> exclusionResult = mock(Result.class);
        when(exclusionResult.all()).thenReturn(exclusionList);
        when(exclusionFuture.get()).thenReturn(exclusionResult);

        //------ Actual call ---------
        repository.populateAssignmentsMetadata(userID, appName, context, experimentBatch, allowAssignmentsOptional, appPriorities, experimentMap, bucketMap, exclusionMap);

        //------ Assert response output ---------
        assertThat(appPriorities.getPrioritizedExperiments().size(), is(1));
        assertThat(appPriorities.getPrioritizedExperiments().get(0).getID(), is(expId1));
        assertThat(experimentMap.get(expId1) != null, is(true));
        assertThat(bucketMap.size(), is(1));
        assertThat(exclusionMap.size(), is(1));
        assertThat(exclusionMap.get(expId1).get(0).getRawID(), is(exclusion1.getPair()));

    }

    @Test
    public void testPopulateExperimentMetadataInvalidInputCase() throws ExecutionException, InterruptedException {

        //------ Input --------
        Experiment.ID expId1 = Experiment.ID.newInstance();
        Application.Name appName = Application.Name.valueOf("testApp1");
        User.ID userID = User.ID.valueOf("testUser1");
        Context context = Context.valueOf("TEST");
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        ExperimentBatch experimentBatch = ExperimentBatch.newInstance().withProfile(segmentationProfile.getProfile()).build();
        Optional<Map<Experiment.ID, Boolean>> allowAssignmentsOptional = Optional.empty();

        //----- Output -----------
        PrioritizedExperimentList appPriorities = new PrioritizedExperimentList();
        Map<Experiment.ID, com.intuit.wasabi.experimentobjects.Experiment> experimentMap = new HashMap<>();
        Table<Experiment.ID, Experiment.Label, String> userAssignments = HashBasedTable.create();
        Map<Experiment.ID, BucketList> bucketMap = new HashMap<>();
        Map<Experiment.ID, List<Experiment.ID>> exclusionMap = new HashMap<>();

        //------ Actual call ---------
        repository.populateAssignmentsMetadata(userID, appName, context, experimentBatch, allowAssignmentsOptional, appPriorities, experimentMap, bucketMap, exclusionMap);

        //------ Assert response output ---------
        assertThat(appPriorities.getPrioritizedExperiments().size(), is(0));
        assertThat(experimentMap.get(expId1) != null, is(false));
        assertThat(bucketMap.size(), is(0));
        assertThat(exclusionMap.size(), is(0));
    }

    @Test
    public void testAssignUsersInBatchCalls() {

        //------ Input

        Experiment experiment = Experiment.withID(Experiment.ID.valueOf(this.experimentId))
                .withIsPersonalizationEnabled(false)
                .withIsRapidExperiment(false).build();
        User.ID userID1 = User.ID.valueOf("testuser1");
        User.ID userID2 = User.ID.valueOf("testuser2");
        Context context = Context.valueOf("test");
        Date date = new Date();
        String bucketLabel = "bucket-1";

        Assignment assignment1 = Assignment.newInstance(experiment.getID())
                .withBucketLabel(Bucket.Label.valueOf(bucketLabel))
                .withCreated(date)
                .withApplicationName(APPLICATION_NAME)
                .withContext(context)
                .withUserID(userID1)
                .build();

        Assignment assignment2 = Assignment.newInstance(experiment.getID())
                .withBucketLabel(null)
                .withCreated(date)
                .withApplicationName(APPLICATION_NAME)
                .withContext(context)
                .withUserID(userID2)
                .build();

        List<Pair<Experiment, Assignment>> assignmentPairs = new LinkedList<>();
        assignmentPairs.add(new ImmutablePair<>(experiment, assignment1));
        assignmentPairs.add(new ImmutablePair<>(experiment, assignment2));


        //------ Mocking interacting calls
        ResultSetFuture genericResultSetFuture = mock(ResultSetFuture.class);
        ResultSet genericResultSet = mock(ResultSet.class);
        when(genericResultSetFuture.getUninterruptibly()).thenReturn(genericResultSet);

        doNothing().when(assignmentsCountExecutor).execute(any());

        when(driver.getSession()).thenReturn(mock(Session.class));
        when(driver.getSession().execute(any(BatchStatement.class))).thenReturn(genericResultSet);

        //------ Make final call
        boolean success = true;
        try {
            repository.assignUsersInBatch(assignmentPairs, date);
        } catch (Exception e) {
            logger.error("Failed to execute assignUser test...", e);
            success = false;
        }
        assertThat(success, is(true));
    }

}