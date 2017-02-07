/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
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
import com.intuit.wasabi.experimentobjects.*;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.*;
import com.intuit.wasabi.repository.cassandra.accessor.count.BucketAssignmentCountAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.export.UserAssignmentExportAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentUserIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.PageExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.UserAssignmentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.*;
import com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentUserByUserIdContextAppNameExperimentId;
import com.intuit.wasabi.repository.cassandra.pojo.index.UserAssignmentByUserId;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CassandraAssignmentsRepositoryTest {
    private final Logger logger = LoggerFactory.getLogger(CassandraAssignmentsRepositoryTest.class);
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock ExperimentRepository experimentRepository;
    @Mock ExperimentRepository dbRepository;
    @Mock EventLog eventLog;

    @Mock ExperimentAccessor experimentAccessor;
    @Mock ExperimentUserIndexAccessor experimentUserIndexAccessor;

    @Mock UserAssignmentAccessor userAssignmentAccessor;
    @Mock UserAssignmentIndexAccessor userAssignmentIndexAccessor;
    @Mock UserAssignmentExportAccessor userAssignmentExportAccessor;

    @Mock BucketAccessor bucketAccessor;
    @Mock BucketAssignmentCountAccessor bucketAssignmentCountAccessor;

    @Mock StagingAccessor stagingAccessor;
    @Mock PrioritiesAccessor prioritiesAccessor;
    @Mock ExclusionAccessor exclusionAccessor;
    @Mock PageExperimentIndexAccessor pageExperimentIndexAccessor;

    @Mock CassandraDriver driver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MappingManager mappingManager;
    @Mock Result mockedResultMapping;

    @Mock ThreadPoolExecutor assignmentsCountExecutor;

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
                userAssignmentAccessor,
                userAssignmentIndexAccessor,
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
                false,
                true,
                true,
                "yyyy-MM-dd HH:mm:ss"
                );
        spyRepository = spy(repository);
    }

    @Test
    public void testGetUserAssignments(){
        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(UUID.randomUUID())
                .context("test")
                .bucket("bucket1")
                .build()
        );
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(null)
                .bucket("bucket1")
                .context("test")
                .build()
        );
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(UUID.randomUUID())
                .context("test")
                .bucket(null)
                .build()
        );
        when(experimentUserIndexAccessor.selectBy(
                anyString(),
                eq(APPLICATION_NAME.toString()),
                anyString()
                )).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mocked.iterator());
        Set<Experiment.ID> result = repository.getUserAssignments(
                User.ID.valueOf("user1"),
                APPLICATION_NAME,
                Context.valueOf("test"));
        assertThat(result.size(), is(1));
        Experiment.ID resultObject = result.iterator().next();
        assertThat(resultObject.getRawID(), is(mocked.get(0).getExperimentId()));
    }

    @Test
    public void testGetUserAssignmentsException(){
        doThrow(ReadTimeoutException.class).when(experimentUserIndexAccessor)
                .selectBy(eq("testUser"),
                        eq(APPLICATION_NAME.toString()),
                        eq("test") );
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not retrieve assignments for experimentID = \"testApp\" userID = \"testUser\" and context test");
        repository.getUserAssignments(
                User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"));
    }

    @Test
    public void testGetAssignmentsMultiple(){
        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(UUID.randomUUID())
                .context("test")
                .bucket("bucket1")
                .build()
        );
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(UUID.randomUUID())
                .context("test")
                .bucket("bucket2")
                .build()
        );

        Table<Experiment.ID, Experiment.Label, Experiment> experimentTable = HashBasedTable.create();
        UUID random = UUID.randomUUID();
        experimentTable.put(Experiment.ID.valueOf(random),
                Experiment.Label.valueOf("random-"+random.toString()),
                Experiment.withID(Experiment.ID.valueOf(random)).build()
        );
        for(ExperimentUserByUserIdContextAppNameExperimentId item : mocked) {
            experimentTable.put(Experiment.ID.valueOf(item.getExperimentId()),
                    Experiment.Label.valueOf("test-"+item.getBucket()),
                    Experiment.withID(Experiment.ID.valueOf(item.getExperimentId()))
                            .withLabel(Experiment.Label.valueOf("test-"+item.getBucket())).build()
                    );
        }
        
        doReturn(mocked.stream()).when(spyRepository).getUserIndexStream(anyString(), anyString(), anyString());
        Table<Experiment.ID, Experiment.Label, String> result = spyRepository.getAssignments(User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"),
                experimentTable
                );
        assertThat(result.size(), is(2));
        for(Experiment.ID id : result.rowKeySet()){
            assertThat(mocked.stream().map(t-> t.getExperimentId()).collect(Collectors.toList()), hasItems(id.getRawID()));
        }
    }

    @Test
    public void testGetAssignmentsSingle(){
        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(experimentId)
                .context("test")
                .bucket("bucket1")
                .build()
        );

        Table<Experiment.ID, Experiment.Label, Experiment> experimentTable = HashBasedTable.create();
        experimentTable.put(Experiment.ID.valueOf(experimentId),
                Experiment.Label.valueOf("test-"+experimentId.toString()),
                Experiment.withID(Experiment.ID.valueOf(experimentId))
                        .withLabel(Experiment.Label.valueOf("test-bucket")).build()
        );
        doReturn(mocked.stream()).when(spyRepository).getUserIndexStream(anyString(), anyString(), anyString());
        Table<Experiment.ID, Experiment.Label, String> result = spyRepository.getAssignments(User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"),
                experimentTable
        );
        assertThat(result.size(), is(1));
        for(Experiment.ID id : result.rowKeySet()){
            assertThat(mocked.stream().map(t-> t.getExperimentId()).collect(Collectors.toList()), hasItems(id.getRawID()));
        }
    }

    @Test
    public void testGetAssignmentsSingleNoMatch(){
        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        mocked.add(ExperimentUserByUserIdContextAppNameExperimentId.builder()
                .appName(APPLICATION_NAME.toString())
                .experimentId(experimentId)
                .context("test")
                .bucket("bucket1")
                .build()
        );

        Table<Experiment.ID, Experiment.Label, Experiment> experimentTable = HashBasedTable.create();
        experimentTable.put(Experiment.ID.valueOf(UUID.randomUUID()),
                Experiment.Label.valueOf("test-"+experimentId.toString()),
                Experiment.withID(Experiment.ID.valueOf(experimentId))
                        .withLabel(Experiment.Label.valueOf("test-bucket")).build()
        );
        doReturn(mocked.stream()).when(spyRepository).getUserIndexStream(anyString(), anyString(), anyString());
        Table<Experiment.ID, Experiment.Label, String> result = spyRepository.getAssignments(User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"),
                experimentTable
        );
        assertThat(result.size(), is(0));
    }

    @Test
    public void testGetAssignmentsEmptyResult(){
        List<ExperimentUserByUserIdContextAppNameExperimentId> mocked = new ArrayList<>();
        Table<Experiment.ID, Experiment.Label, Experiment> experimentTable = HashBasedTable.create();
        experimentTable.put(Experiment.ID.valueOf(UUID.randomUUID()),
                Experiment.Label.valueOf("test-"+experimentId.toString()),
                Experiment.withID(Experiment.ID.valueOf(experimentId))
                        .withLabel(Experiment.Label.valueOf("test-bucket")).build()
        );
        doReturn(mocked.stream()).when(spyRepository).getUserIndexStream(anyString(), anyString(), anyString());
        Table<Experiment.ID, Experiment.Label, String> result = spyRepository.getAssignments(User.ID.valueOf("testUser"),
                APPLICATION_NAME,
                Context.valueOf("test"),
                experimentTable
        );
        assertThat(result.size(), is(0));
    }

    @Test
    public void testGetAssignmentFromStreamMultiple(){
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
    public void testGetAssignmentFromStreamSingleStateEmpty(){
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
    public void testGetAssignmentFromStreamSingleStateNotEmpty(){
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
    public void testGetAssignmentFromStreamEmpty(){
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
    public void testGetAssignmentFromLookUp(){
        List<UserAssignmentByUserId> mockedResult = new ArrayList<>();
        mockedResult.add(UserAssignmentByUserId.builder()
                .experimentId(experimentId)
                .context("test")
                .bucketLabel("bucket-1")
                .created(new Date())
                .userId("testuser1")
                .build()
        );
        when(userAssignmentIndexAccessor.selectBy(eq(experimentId), eq("testuser1"), eq("test")))
                .thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        Bucket mockedBucket = Bucket.newInstance(Experiment.ID.valueOf(experimentId), Bucket.Label.valueOf("bucket-1"))
                .withAllocationPercent(1.0)
                .withState(Bucket.State.OPEN)
                .build();
        when(experimentRepository.getBucket(eq(Experiment.ID.valueOf(experimentId)), eq(Bucket.Label.valueOf("bucket-1"))))
                .thenReturn(mockedBucket);
        Optional<Assignment> assignment = repository.getAssignmentFromLookUp(
                Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
        assertThat(assignment.isPresent(), is(true));
        Assignment result = assignment.get();
        assertThat(result.getBucketLabel(), is(Bucket.Label.valueOf("bucket-1")));
        assertThat(result.getExperimentID().getRawID(), is(experimentId));
        assertThat(result.getContext().getContext(), is("test"));
        assertThat(result.getStatus(), is(Assignment.Status.EXISTING_ASSIGNMENT));
        assertThat(result.isBucketEmpty(), is(false));
        assertThat(result.isCacheable(), is(false));
    }

    @Test
    public void testGetAssignmentFromLookUpReadException(){
        doThrow(ReadTimeoutException.class)
                .when(userAssignmentIndexAccessor)
                .selectBy(eq(experimentId), eq("testuser1"), eq("test"));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not retrieve assignment for experimentID = \"4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8\" userID = \"testuser1\"");
        repository.getAssignmentFromLookUp(Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test")
        );
    }

    @Test
    public void testGetAssignmentFromLookUpBucketIsNull(){
        List<UserAssignmentByUserId> mockedResult = new ArrayList<>();
        mockedResult.add(UserAssignmentByUserId.builder()
                .experimentId(experimentId)
                .context("test")
                .bucketLabel(null)
                .created(new Date())
                .userId("testuser1")
                .build()
        );
        when(userAssignmentIndexAccessor.selectBy(eq(experimentId), eq("testuser1"), eq("test")))
                .thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        Bucket mockedBucket = Bucket.newInstance(Experiment.ID.valueOf(experimentId), Bucket.Label.valueOf("bucket-1"))
                .withAllocationPercent(1.0)
                .withState(Bucket.State.OPEN)
                .build();
        when(experimentRepository.getBucket(eq(Experiment.ID.valueOf(experimentId)), isNull(Bucket.Label.class)))
                .thenReturn(mockedBucket);
        Optional<Assignment> assignment = repository.getAssignmentFromLookUp(
                Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
        assertThat(assignment.isPresent(), is(true));
        Assignment result = assignment.get();
        assertThat(result.getBucketLabel(), is(nullValue()));
        assertThat(result.getExperimentID().getRawID(), is(experimentId));
        assertThat(result.getContext().getContext(), is("test"));
        assertThat(result.getStatus(), is(Assignment.Status.EXISTING_ASSIGNMENT));
        assertThat(result.isBucketEmpty(), is(false));
        assertThat(result.isCacheable(), is(false));
    }

    @Test
    public void testGetAssignmentOldReadException(){
        doThrow(ReadTimeoutException.class)
                .when(userAssignmentAccessor)
                .selectBy(eq(experimentId), eq("testuser1"), eq("test"));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not retrieve assignment for experimentID = \"4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8\" userID = \"testuser1\"");
        repository.getAssignmentOld(Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
    }

    @Test
    public void testGetAssignmentOld(){
        List<UserAssignment> mockedResult = new ArrayList<>();
        mockedResult.add(UserAssignment.builder()
                .experimentId(experimentId)
                .context("test")
                .bucketLabel("bucket-1")
                .created(new Date())
                .userId("testuser1")
                .build()
        );
        when(userAssignmentAccessor.selectBy(eq(experimentId), eq("testuser1"), eq("test")))
                .thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        Bucket mockedBucket = Bucket.newInstance(Experiment.ID.valueOf(experimentId), Bucket.Label.valueOf("bucket-1"))
                .withAllocationPercent(1.0)
                .withState(Bucket.State.OPEN)
                .build();
        when(experimentRepository.getBucket(eq(Experiment.ID.valueOf(experimentId)), eq(Bucket.Label.valueOf("bucket-1"))))
                .thenReturn(mockedBucket);
        Optional<Assignment> assignment = repository.getAssignmentOld(
                Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
        assertThat(assignment.isPresent(), is(true));
        Assignment result = assignment.get();
        assertThat(result.getBucketLabel(), is(Bucket.Label.valueOf("bucket-1")));
        assertThat(result.getExperimentID().getRawID(), is(experimentId));
        assertThat(result.getContext().getContext(), is("test"));
        assertThat(result.getStatus(), is(Assignment.Status.EXISTING_ASSIGNMENT));
        assertThat(result.isBucketEmpty(), is(false));
        assertThat(result.isCacheable(), is(false));
    }

    @Test
    public void testGetAssignmentOldEmptyQueryResult(){
        List<UserAssignment> mockedResult = new ArrayList<>();
        when(userAssignmentAccessor.selectBy(eq(experimentId), eq("testuser1"), eq("test")))
                .thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        Optional<Assignment> assignment = repository.getAssignmentOld(
                Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
        assertThat(assignment.isPresent(), is(false));
    }

    @Test
    public void testGetAssignmentAssignToOldIsTrue(){
        Optional<Assignment> mocked = Optional.ofNullable(mock(Assignment.class));
        doReturn(Optional.empty()).when(spyRepository).getAssignmentFromLookUp(eq(Experiment.ID.valueOf(experimentId)),
                eq(User.ID.valueOf("testuser1")),
                eq(Context.valueOf("test"))
        );
        doReturn(mocked).when(spyRepository).getAssignmentOld(eq(Experiment.ID.valueOf(experimentId)),
                eq(User.ID.valueOf("testuser1")),
                eq(Context.valueOf("test"))
        );
        Assignment result = spyRepository.getAssignment(Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
        assertThat(mocked.get(), is(result));
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void testGetAssignmentAssignToNewIsTrue(){
        CassandraAssignmentsRepository assignmentsRepository = spy(new CassandraAssignmentsRepository(
                experimentRepository,
                dbRepository,
                eventLog,
                experimentAccessor,
                experimentUserIndexAccessor,
                userAssignmentAccessor,
                userAssignmentIndexAccessor,
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
                false,
                true,
                true,
                true,
                "yyyy-MM-dd HH:mm:ss"
        ));
        Optional<Assignment> mocked = Optional.ofNullable(mock(Assignment.class));
        doReturn(mocked).when(assignmentsRepository).getAssignmentFromLookUp(eq(Experiment.ID.valueOf(experimentId)),
                eq(User.ID.valueOf("testuser1")),
                eq(Context.valueOf("test"))
        );
        Assignment result = assignmentsRepository.getAssignment(Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
        assertThat(mocked.get(), is(result));
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void testGetAssignmentBothFalse(){
        CassandraAssignmentsRepository assignmentsRepository = spy(new CassandraAssignmentsRepository(
                experimentRepository,
                dbRepository,
                eventLog,
                experimentAccessor,
                experimentUserIndexAccessor,
                userAssignmentAccessor,
                userAssignmentIndexAccessor,
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
                false,
                false,
                true,
                true,
                "yyyy-MM-dd HH:mm:ss"
        ));
        Optional<Assignment> mocked1 = Optional.ofNullable(mock(Assignment.class));
        doReturn(mocked1).when(assignmentsRepository).getAssignmentFromLookUp(eq(Experiment.ID.valueOf(experimentId)),
                eq(User.ID.valueOf("testuser1")),
                eq(Context.valueOf("test"))
        );
        Optional<Assignment> mocked2 = Optional.ofNullable(mock(Assignment.class));
        doReturn(mocked2).when(spyRepository).getAssignmentFromLookUp(eq(Experiment.ID.valueOf(experimentId)),
                eq(User.ID.valueOf("testuser1")),
                eq(Context.valueOf("test"))
        );
        Assignment result = assignmentsRepository.getAssignment(Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
        assertThat(result, is(nullValue()));
    }


    @Test
    public void testGetAssignmentBothTrue(){
        CassandraAssignmentsRepository assignmentsRepository = spy(new CassandraAssignmentsRepository(
                experimentRepository,
                dbRepository,
                eventLog,
                experimentAccessor,
                experimentUserIndexAccessor,
                userAssignmentAccessor,
                userAssignmentIndexAccessor,
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
                true,
                true,
                "yyyy-MM-dd HH:mm:ss"
        ));
        Optional<Assignment> mocked1 = Optional.ofNullable(mock(Assignment.class));
        doReturn(mocked1).when(assignmentsRepository).getAssignmentFromLookUp(eq(Experiment.ID.valueOf(experimentId)),
                eq(User.ID.valueOf("testuser1")),
                eq(Context.valueOf("test"))
        );
        Optional<Assignment> mocked2 = Optional.ofNullable(mock(Assignment.class));
        doReturn(mocked2).when(spyRepository).getAssignmentFromLookUp(eq(Experiment.ID.valueOf(experimentId)),
                eq(User.ID.valueOf("testuser1")),
                eq(Context.valueOf("test"))
        );
        Assignment result = assignmentsRepository.getAssignment(Experiment.ID.valueOf(experimentId),
                User.ID.valueOf("testuser1"),
                Context.valueOf("test"));
        assertThat(result, is(notNullValue()));
        assertThat(result, is(mocked1.get()));
    }
    @Test
    public void testAssignUserToLookUpWriteException(){
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
                .when(userAssignmentIndexAccessor)
                .insertBy(eq(expected.getExperimentID().getRawID()),
                        eq(expected.getUserID().toString()),
                        eq(expected.getContext().getContext()),
                        eq(expected.getCreated()),
                        eq(expected.getBucketLabel().toString()));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not save user assignment");
        repository.assignUserToLookUp(expected, expected.getCreated());
    }


    @Test
    public void testAssignUserToLookUpFullParameter(){
        Assignment expected = Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withBucketLabel(Bucket.Label.valueOf("bucket-1"))
                .withContext(Context.valueOf("test"))
                .withCreated(new Date())
                .withUserID(User.ID.valueOf("testuser1"))
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCacheable(false)
                .build();
        Assignment result = repository.assignUserToLookUp(expected, expected.getCreated());
        verify(userAssignmentIndexAccessor, times(0)).insertBy(eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()));
        verify(userAssignmentIndexAccessor, times(1)).insertBy(eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()),
                eq(expected.getBucketLabel().toString()));
        assertThat(result.getBucketLabel(), is(expected.getBucketLabel()));
        assertThat(result.getStatus(), is(expected.getStatus()));
        assertThat(result.getContext(), is(expected.getContext()));
        assertThat(result.getCreated(), is(expected.getCreated()));
        assertThat(result.getUserID(), is(expected.getUserID()));
        assertThat(result.getExperimentID(), is(expected.getExperimentID()));
        assertThat(result.getApplicationName(), is(nullValue()));
    }

    @Test
    public void testAssignUserToLookUpWihtoutLabel(){
        Assignment expected = Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withContext(Context.valueOf("test"))
                .withCreated(new Date())
                .withUserID(User.ID.valueOf("testuser1"))
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCacheable(false)
                .build();
        Assignment result = repository.assignUserToLookUp(expected, expected.getCreated());
        verify(userAssignmentIndexAccessor, times(1)).insertBy(eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()));
        verify(userAssignmentIndexAccessor, times(0)).insertBy(eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()),
                any());
        assertThat(result.getBucketLabel(), is(nullValue()));
        assertThat(result.getStatus(), is(expected.getStatus()));
        assertThat(result.getContext(), is(expected.getContext()));
        assertThat(result.getCreated(), is(expected.getCreated()));
        assertThat(result.getUserID(), is(expected.getUserID()));
        assertThat(result.getExperimentID(), is(expected.getExperimentID()));
        assertThat(result.getApplicationName(), is(nullValue()));
    }

    @Test
    public void testAssignUserToOldWriteException(){
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
                .when(userAssignmentAccessor)
                .insertBy(eq(expected.getExperimentID().getRawID()),
                        eq(expected.getUserID().toString()),
                        eq(expected.getContext().getContext()),
                        eq(expected.getCreated()),
                        eq(expected.getBucketLabel().toString()));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not save user to the old table user assignment ");
        repository.assignUserToOld(expected, expected.getCreated());
    }


    @Test
    public void testAssignUserToOldFullParameter(){
        Assignment expected = Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withBucketLabel(Bucket.Label.valueOf("bucket-1"))
                .withContext(Context.valueOf("test"))
                .withCreated(new Date())
                .withUserID(User.ID.valueOf("testuser1"))
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCacheable(false)
                .build();
        Assignment result = repository.assignUserToOld(expected, expected.getCreated());
        verify(userAssignmentAccessor, times(0)).insertBy(eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()));
        verify(userAssignmentAccessor, times(1)).insertBy(eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()),
                eq(expected.getBucketLabel().toString()));
        assertThat(result.getBucketLabel(), is(expected.getBucketLabel()));
        assertThat(result.getStatus(), is(expected.getStatus()));
        assertThat(result.getContext(), is(expected.getContext()));
        assertThat(result.getCreated(), is(expected.getCreated()));
        assertThat(result.getUserID(), is(expected.getUserID()));
        assertThat(result.getExperimentID(), is(expected.getExperimentID()));
        assertThat(result.getApplicationName(), is(nullValue()));
    }

    @Test
    public void testAssignUserToOldWihtoutLabel(){
        Assignment expected = Assignment.newInstance(Experiment.ID.valueOf(experimentId))
                .withApplicationName(APPLICATION_NAME)
                .withContext(Context.valueOf("test"))
                .withCreated(new Date())
                .withUserID(User.ID.valueOf("testuser1"))
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCacheable(false)
                .build();
        Assignment result = repository.assignUserToOld(expected, expected.getCreated());
        verify(userAssignmentAccessor, times(1)).insertBy(eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()));
        verify(userAssignmentAccessor, times(0)).insertBy(eq(expected.getExperimentID().getRawID()),
                eq(expected.getUserID().toString()),
                eq(expected.getContext().getContext()),
                eq(expected.getCreated()),
                any());
        assertThat(result.getBucketLabel(), is(nullValue()));
        assertThat(result.getStatus(), is(expected.getStatus()));
        assertThat(result.getContext(), is(expected.getContext()));
        assertThat(result.getCreated(), is(expected.getCreated()));
        assertThat(result.getUserID(), is(expected.getUserID()));
        assertThat(result.getExperimentID(), is(expected.getExperimentID()));
        assertThat(result.getApplicationName(), is(nullValue()));
    }

    @Test
    public void testAssignmentExportWriteException(){
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
    public void testAssignExportWithLabel(){
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
    public void testAssignExportWihtoutLabel(){
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
    public void testGetUserAssignmentPartitions(){
        Date date1 = new Date(116,7,1);
        Date date2 = new Date(116,7,2,1,0);
        List<Date> result = repository.getUserAssignmentPartitions(date1, date2);
        assertThat(result.size(), is(26));
        result = repository.getUserAssignmentPartitions(date2, date1);
        assertThat(result.size(), is(0));
    }

    @Test
    public void testGetDateHourRangeListNoExperimentException(){
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Parameters parameters = mock(Parameters.class);
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(null);
        thrown.expect(ExperimentNotFoundException.class);
        thrown.expectMessage("Experiment \"4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8\" not found");
        repository.getDateHourRangeList(experimentId,  parameters);
    }

    @Test
    public void testGetDateHourRangeListNoFromAndToDate(){
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Parameters parameters = mock(Parameters.class);
        Experiment experimentResult = mock(Experiment.class);
        when(parameters.getFromTime()).thenReturn(null);
        when(parameters.getToTime()).thenReturn(null);
        when(experimentResult.getCreationTime()).thenReturn(new Date());
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(experimentResult);
        List<Date> result = repository.getDateHourRangeList(experimentId,  parameters);
        assertThat(result.size(), is(1));
    }

    @Test
    public void testGetDateHourRangeListWithFromAndToDate(){
        Date fromDate = new Date(1469084400000L);
        Date toDate = new Date(1469098800000L);
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Parameters parameters = mock(Parameters.class);
        Experiment experimentResult = mock(Experiment.class);
        when(parameters.getFromTime()).thenReturn(fromDate);
        when(parameters.getToTime()).thenReturn(toDate);
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(experimentResult);
        List<Date> result = repository.getDateHourRangeList(experimentId,  parameters);
        assertThat(result.size(), is(5));
    }

    @Test
    public void testGetDateHourRangeListWithFromAndToDateReversed(){
        Date fromDate = new Date(1469084400000L);
        Date toDate = new Date(1469098800000L);
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        Parameters parameters = mock(Parameters.class);
        Experiment experimentResult = mock(Experiment.class);
        when(parameters.getFromTime()).thenReturn(toDate);
        when(parameters.getToTime()).thenReturn(fromDate);
        when(experimentRepository.getExperiment(eq(experimentId))).thenReturn(experimentResult);
        List<Date> result = repository.getDateHourRangeList(experimentId,  parameters);
        assertThat(result.size(), is(0));
    }

    @Test
    public void testGetAssignmentStreamNoExperimentExceptionIgnoreNullBuckets(){
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
    public void testRemoveIndexExperimentsToUser(){
        repository.removeIndexExperimentsToUser(User.ID.valueOf("testuser1"),
                Experiment.ID.valueOf(this.experimentId),
                Context.valueOf("test"),
                APPLICATION_NAME);
        verify(experimentUserIndexAccessor, times(1))
                .deleteBy(eq("testuser1"), eq(this.experimentId), eq("test"), eq(APPLICATION_NAME.toString()));
    }

    @Test
    public void testRemoveIndexExperimentsToUserWriteException(){
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
    public void testPushAssignmentToStaging(){
        repository.pushAssignmentToStaging("type", "string1", "string2");
        verify(stagingAccessor, times(1))
                .insertBy(eq("type"), eq("string1"), eq("string2"));
    }

    @Test
    public void testPushAssignmentToStagingrWriteException(){
        doThrow(WriteTimeoutException.class).when(stagingAccessor)
                .insertBy(eq("type"), eq("string1"), eq("string2"));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not push the assignment to staging");
        repository.pushAssignmentToStaging("type", "string1", "string2");
    }

    @Test
    public void testUpdateBucketAssignmentCountUp(){
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
    public void testUpdateBucketAssignmentCountDown(){
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
    public void testUpdateBucketAssignmentCountrWriteException(){
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
    public void testIndexExperimentToUserEmptyBucket(){
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
    public void testIndexExperimentToUserWithBucket(){
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
    public void testIndexExperimentToUserWithBucketWriteException(){
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
    public void testDeleteAssignmentOld(){
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        User.ID userID = User.ID.valueOf("testuser1");
        Context context = Context.valueOf("test");
        repository.deleteAssignmentOld(experimentId, userID, context, null, null);
        verify(userAssignmentAccessor, times(1)).deleteBy(
                eq(this.experimentId),
                eq(userID.toString()),
                eq(context.getContext())
        );
    }

    @Test
    public void testDeleteAssignmentOldWriteException(){
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        User.ID userID = User.ID.valueOf("testuser1");
        Context context = Context.valueOf("test");
        doThrow(WriteTimeoutException.class).when(userAssignmentAccessor)
                .deleteBy(any(UUID.class), any(String.class), any(String.class));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not delete user assignment for Experiment:4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8 and User testuser1");
        repository.deleteAssignmentOld(experimentId, userID, context, null, null);
    }

    @Test
    public void testDeleteUserFromLookUp(){
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        User.ID userID = User.ID.valueOf("testuser1");
        Context context = Context.valueOf("test");
        repository.deleteUserFromLookUp(experimentId, userID, context);
        verify(userAssignmentIndexAccessor, times(1)).deleteBy(
                eq(userID.toString()),
                eq(context.getContext()),
                eq(this.experimentId)
        );
    }

    @Test
    public void testDeleteUserFromLookUpException(){
        Experiment.ID experimentId = Experiment.ID.valueOf(this.experimentId);
        User.ID userID = User.ID.valueOf("testuser1");
        Context context = Context.valueOf("test");
        doThrow(WriteTimeoutException.class).when(userAssignmentIndexAccessor)
                .deleteBy(any(String.class), any(String.class), any(UUID.class));
        thrown.expect(RepositoryException.class);
        thrown.expectMessage("Could not delete user assignment for Experiment:4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8 and User testuser1");
        repository.deleteUserFromLookUp(experimentId, userID, context);
    }

    @Test
    public void testDeleteAssignment(){
        Experiment experiment = Experiment.withID(Experiment.ID.valueOf(this.experimentId)).build();
        User.ID userID = User.ID.valueOf("testuser1");
        Context context = Context.valueOf("test");
        Assignment currentAssignment = Assignment.newInstance(experiment.getID())
                .withBucketLabel(Bucket.Label.valueOf("bucket-1"))
                .build();
        spyRepository.deleteAssignment(experiment, userID, context, APPLICATION_NAME, currentAssignment);
        verify(spyRepository, times(1)).deleteUserFromLookUp(eq(experiment.getID()), eq(userID), eq(context));
        verify(spyRepository, times(1)).deleteAssignmentOld(
                eq(experiment.getID()),
                eq(userID),
                eq(context),
                eq(APPLICATION_NAME),
                eq(currentAssignment.getBucketLabel())
        );
        verify(spyRepository, times(1)).removeIndexExperimentsToUser(
                eq(userID),
                eq(experiment.getID()),
                eq(context),
                eq(APPLICATION_NAME)
        );
    }


    @Test
    public void testAssignmentUserOld(){
        Experiment experiment = Experiment.withID(Experiment.ID.valueOf(this.experimentId))
                .withIsPersonalizationEnabled(false)
                .withIsRapidExperiment(false).build();
        User.ID userID = User.ID.valueOf("testuser1");
        Context context = Context.valueOf("test");
        Date date = new Date();
        Assignment currentAssignment = Assignment.newInstance(experiment.getID())
                .withBucketLabel(Bucket.Label.valueOf("bucket-1"))
                .withCreated(date)
                .withApplicationName(APPLICATION_NAME)
                .withContext(context)
                .withUserID(userID)
                .build();
        spyRepository.assignUser(currentAssignment, experiment, date);
        verify(spyRepository, times(1)).assignUserToOld(eq(currentAssignment), eq(date));
        verify(spyRepository, times(0)).assignUserToLookUp(eq(currentAssignment), eq(date));
        verify(spyRepository, times(1)).indexExperimentsToUser(eq(currentAssignment));
    }

    @Test
    public void testAssignmentUserNew(){
        CassandraAssignmentsRepository spyRepository = spy(new CassandraAssignmentsRepository(
                experimentRepository,
                dbRepository,
                eventLog,
                experimentAccessor,
                experimentUserIndexAccessor,
                userAssignmentAccessor,
                userAssignmentIndexAccessor,
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
                false,
                true,
                true,
                true,
                "yyyy-MM-dd HH:mm:ss"
        ));
        Experiment experiment = Experiment.withID(Experiment.ID.valueOf(this.experimentId))
                .withIsPersonalizationEnabled(false)
                .withIsRapidExperiment(false).build();
        User.ID userID = User.ID.valueOf("testuser1");
        Context context = Context.valueOf("test");
        Date date = new Date();
        Assignment currentAssignment = Assignment.newInstance(experiment.getID())
                .withBucketLabel(Bucket.Label.valueOf("bucket-1"))
                .withCreated(date)
                .withApplicationName(APPLICATION_NAME)
                .withContext(context)
                .withUserID(userID)
                .build();
        spyRepository.assignUser(currentAssignment, experiment, date);
        verify(spyRepository, times(0)).assignUserToOld(eq(currentAssignment), eq(date));
        verify(spyRepository, times(1)).assignUserToLookUp(eq(currentAssignment), eq(date));
        verify(spyRepository, times(1)).indexExperimentsToUser(eq(currentAssignment));
    }

    @Test
    public void testBucketAssignmentCountReadException(){
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
    public void testBucketAssignmentCountNullResult(){
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
    public void testBucketAssignmentCount(){
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
        for(int i =0; i < mockedList.size(); i++) {
            String label = mockedList.get(i).getBucketLabel();
            if(Objects.isNull(label)) {
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
        Table<Experiment.ID, Experiment.Label, String> userAssignments = HashBasedTable.create();
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
        repository.populateExperimentMetadata(userID, appName, context, experimentBatch, allowAssignmentsOptional, appPriorities, experimentMap, userAssignments, bucketMap, exclusionMap);

        //------ Assert response output ---------
        assertThat(appPriorities.getPrioritizedExperiments().size(), is(1));
        assertThat(appPriorities.getPrioritizedExperiments().get(0).getID(), is(expId1));
        assertThat(experimentMap.get(expId1)!=null, is(true));
        assertThat(userAssignments.size(), is(1));
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
        repository.populateExperimentMetadata(userID, appName, context, experimentBatch, allowAssignmentsOptional, appPriorities, experimentMap, userAssignments, bucketMap, exclusionMap);

        //------ Assert response output ---------
        assertThat(appPriorities.getPrioritizedExperiments().size(), is(0));
        assertThat(experimentMap.get(expId1)!=null, is(false));
        assertThat(userAssignments.size(), is(0));
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

        when(userAssignmentIndexAccessor.asyncInsertBy(experiment.getID().getRawID(), userID1.toString(), context.getContext(), date)).thenReturn(genericResultSetFuture);
        when(userAssignmentIndexAccessor.asyncInsertBy(experiment.getID().getRawID(), userID2.toString(), context.getContext(), date)).thenReturn(genericResultSetFuture);
        when(userAssignmentIndexAccessor.asyncInsertBy(experiment.getID().getRawID(), userID1.toString(), context.getContext(), date, bucketLabel)).thenReturn(genericResultSetFuture);
        when(userAssignmentIndexAccessor.asyncInsertBy(experiment.getID().getRawID(), userID2.toString(), context.getContext(), date, bucketLabel)).thenReturn(genericResultSetFuture);

        when(userAssignmentAccessor.asyncInsertBy(experiment.getID().getRawID(), userID1.toString(), context.getContext(), date)).thenReturn(genericResultSetFuture);
        when(userAssignmentAccessor.asyncInsertBy(experiment.getID().getRawID(), userID2.toString(), context.getContext(), date)).thenReturn(genericResultSetFuture);
        when(userAssignmentAccessor.asyncInsertBy(experiment.getID().getRawID(), userID1.toString(), context.getContext(), date, bucketLabel)).thenReturn(genericResultSetFuture);
        when(userAssignmentAccessor.asyncInsertBy(experiment.getID().getRawID(), userID2.toString(), context.getContext(), date, bucketLabel)).thenReturn(genericResultSetFuture);

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