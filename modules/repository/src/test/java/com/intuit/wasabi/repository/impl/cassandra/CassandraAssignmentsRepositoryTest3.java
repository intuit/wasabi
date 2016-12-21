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
package com.intuit.wasabi.repository.impl.cassandra;

import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.OperationException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.query.CqlQuery;
import com.netflix.astyanax.query.PreparedCqlQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CassandraAssignmentsRepository}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CassandraAssignmentsRepositoryTest3 {
    @Mock
    private PreparedCqlQuery<?, ?> preparedCqlQuery;
    @Mock
    private CassandraAssignmentsRepository defaultCassandraAssignmentsRepository;
    @Mock
    private Rows<?, ?> rows;

    @Before
    public void setup() throws ConnectionException, IOException {
        CassandraDriver cassandraDriver = Mockito.mock(CassandraDriver.class);
        defaultCassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                Mockito.mock(CassandraExperimentRepository.class),
                Mockito.mock(ExperimentRepository.class),
                Mockito.mock(AssignmentsRepository.class),
                cassandraDriver,
                Mockito.mock(ExperimentsKeyspace.class),
                Mockito.mock(EventLog.class),
                5, true, true, true, true, "yyyy-mm-dd");
        Keyspace keyspace = Mockito.mock(Keyspace.class);
        doReturn(keyspace).when(cassandraDriver).getKeyspace();

        ColumnFamilyQuery<?, ?> columnFamilyQuery = Mockito.mock(ColumnFamilyQuery.class);
        doReturn(columnFamilyQuery).when(keyspace).prepareQuery(any());

        CqlQuery cqlQuery = Mockito.mock(CqlQuery.class);
        doReturn(cqlQuery).when(columnFamilyQuery).withCql(anyString());
        doReturn(cqlQuery).when(cqlQuery).useCompression();
        doReturn(preparedCqlQuery).when(cqlQuery).asPreparedStatement();
        doThrow(new IllegalStateException("You should always use asPreparedStatement()!")).when(cqlQuery).execute();

        doReturn(preparedCqlQuery).when(preparedCqlQuery).withBooleanValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withByteBufferValue(any(), any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withDoubleValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withFloatValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withIntegerValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withLongValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withShortValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withStringValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withUUIDValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withValue(any());
        doReturn(preparedCqlQuery).when(preparedCqlQuery).withValues(any());

        OperationResult<?> operationResult = Mockito.mock(OperationResult.class);
        doReturn(operationResult).when(preparedCqlQuery).execute();

        CqlResult<?, ?> cqlResult = Mockito.mock(CqlResult.class);
        doReturn(cqlResult).when(operationResult).getResult();
        doReturn(rows).when(cqlResult).getRows();
    }

    /**
     * Sets up the {@link PreparedCqlQuery} to throw on {@link PreparedCqlQuery#execute()}
     */
    private void doThrowOnExecute() {
        try {
            doThrow(new OperationException("A ConnectionException for unit tests.")).when(preparedCqlQuery).execute();
        } catch (ConnectionException exception) {
            Assert.fail(String.format("Something with the exception mocking is wrong, check doThrowOnExecute().\n" +
                    "Error was: %s", exception.getMessage()));
        }
    }

    @Test
    public void testInsertExperimentBucketAssignment() throws ConnectionException {
        defaultCassandraAssignmentsRepository.insertExperimentBucketAssignment(Experiment.ID.newInstance(), Instant.now(), true);
        defaultCassandraAssignmentsRepository.insertExperimentBucketAssignment(Experiment.ID.newInstance(), Instant.now(), false);

        doThrowOnExecute();
        defaultCassandraAssignmentsRepository.insertExperimentBucketAssignment(Experiment.ID.newInstance(), Instant.now(), true);
        defaultCassandraAssignmentsRepository.insertExperimentBucketAssignment(Experiment.ID.newInstance(), Instant.now(), false);

        verify(preparedCqlQuery, times(4)).execute();
    }

    @Test
    public void testGetExperimentBucketAssignmentRatioPerDay() throws ConnectionException {
        // Objects to probe on
        Experiment.ID experimentID = Experiment.ID.newInstance();
        OffsetDateTime fromDate = OffsetDateTime.of(2016, 9, 25, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime toDate = fromDate.plusDays(3);

        // Mock setup
        List<Row<Experiment.ID, String>> rowList = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            @SuppressWarnings("unchecked")
            Row<Experiment.ID, String> row = Mockito.mock(Row.class);
            @SuppressWarnings("unchecked")
            ColumnList<String> columnList = Mockito.mock(ColumnList.class);

            doReturn(columnList).when(row).getColumns();
            doReturn(i < 50).when(columnList).getBooleanValue("bucket_assignment", true);
            doReturn(i < 50).when(columnList).getBooleanValue("bucket_assignment", false);

            rowList.add(row);
        }
        doAnswer(invocation -> rowList.iterator()).when(rows).iterator();

        // Success test
        Map<OffsetDateTime, Double> result = defaultCassandraAssignmentsRepository.getExperimentBucketAssignmentRatioPerDay(experimentID, fromDate, toDate);

        Assert.assertEquals("Result should contain four elements.", 4, result.size());
        Assert.assertTrue("Result should contain the correct days.",
                result.keySet().containsAll(Arrays.asList(
                        fromDate, fromDate.plusDays(1), fromDate.plusDays(2), fromDate.plusDays(3)
                )));
        Assert.assertTrue("Result should always have 0.5 as the result Double.",
                result.values().stream().allMatch(d -> d.equals(0.5))
        );
        verify(preparedCqlQuery, times(4)).execute();

        // Throw test
        doThrowOnExecute();
        try {
            defaultCassandraAssignmentsRepository.getExperimentBucketAssignmentRatioPerDay(experimentID, fromDate, toDate);
            Assert.fail("Should throw RepositoryException on failure.");
        } catch (RepositoryException ignored) {
        }
        verify(preparedCqlQuery, times(5)).execute();

    }
}
