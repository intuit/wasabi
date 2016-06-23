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
package com.intuit.wasabi.repository.impl.cassandra;

import com.intuit.wasabi.assignmentobjects.DateHour;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CassandraAssignmentsRepositoryTest {
    @Mock
    private ExperimentRepository cassandraRepository;
    @Mock
    private CassandraDriver cassandraDriver;
    @Mock
    private ExperimentsKeyspace keyspace;
    @Mock
    private AssignmentsRepository assignmentsRepository;
    @Mock
    private ExperimentRepository dbRepository;
    @Mock
    private EventLog eventLog;

    //Testing the getUserAssignmentPartition method between two timestamps. Here set to now(), now() + 1 HOUR.
    @Test
    public void getUserAssignmentPartitions_test1() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, keyspace, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        Date to_time = cassandraAssignmentsRepository.addHoursMinutes(from_time, 1, 0);
        List<DateHour> expected = new ArrayList<DateHour>();
        DateHour dateHour1 = new DateHour();
        dateHour1.setDateHour(from_time);
        expected.add(dateHour1);
        DateHour dateHour2 = new DateHour();
        dateHour2.setDateHour(cassandraAssignmentsRepository.addHoursMinutes(from_time, 1, 0));
        expected.add(dateHour2);
        List<DateHour> result = cassandraAssignmentsRepository.getUserAssignmentPartitions(from_time, to_time);
        assert result.equals(expected);
    }

    //Testing the getUserAssignmentPartition method between two equal time stamps
    @Test
    public void getUserAssignmentPartitions_test2() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, keyspace, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        Date to_time = new Date();
        List<DateHour> expected = new ArrayList<DateHour>();
        DateHour dateHour1 = new DateHour();
        dateHour1.setDateHour(from_time);
        expected.add(dateHour1);
        List<DateHour> result = cassandraAssignmentsRepository.getUserAssignmentPartitions(from_time, to_time);
        assert result.equals(expected);
    }

    //Testing the getUserAssignmentPartition method when from_time greater than to_time.
    // The correct behavior here is to return null
    @Test
    public void getUserAssignmentPartitions_test3() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, keyspace, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        Date to_time = cassandraAssignmentsRepository.addHoursMinutes(from_time, 0, -1);
        List<DateHour> expected = new ArrayList<DateHour>();
        List<DateHour> result = cassandraAssignmentsRepository.getUserAssignmentPartitions(from_time, to_time);
        assert result.equals(expected);
    }
}
