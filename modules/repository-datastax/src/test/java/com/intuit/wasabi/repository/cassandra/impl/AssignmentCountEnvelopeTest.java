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

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.impl.NoopEventLogImpl;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AssignmentCountEnvelopeTest {

    private Experiment exp = mock(Experiment.class);
    private AssignmentsRepository ar = mock(AssignmentsRepository.class, RETURNS_DEEP_STUBS);
    private ExperimentRepository cass = mock(ExperimentRepository.class);
    private ExperimentRepository mysql = mock(ExperimentRepository.class);
    private Assignment assignment = mock(Assignment.class);
    private EventLog el = new NoopEventLogImpl();
    private Date date = mock(Date.class);

    @Test
    public void testErrorInUpdateBucket() {

        doThrow(new RuntimeException("exception")).when(ar).updateBucketAssignmentCount(exp, assignment, true);

        AssignmentCountEnvelope env = new AssignmentCountEnvelope(ar, cass, mysql, exp, assignment, true, el, date, true, true);
        env.run();
        // so since the two parts a separate code fragments the fail of the first
        // should not result in a fail of the other
        verify(exp, times(2)).getIsRapidExperiment();
    }

    @Test
    public void testErrorInAssignUserToExports() {

        doThrow(new RuntimeException("exception")).when(ar).assignUserToExports(Mockito.eq(assignment), Mockito.any());

        AssignmentCountEnvelope env = new AssignmentCountEnvelope(ar, cass, mysql, exp, assignment, true, el, date, true, true);
        env.run();
        // so since the two parts a separate code fragments the fail of the first
        // should not result in a fail of the other
        verify(exp, times(2)).getIsRapidExperiment();
    }

    /**
     * This tests what happens when the userCap is not reached yet.
     */
    @Test
    public void testRapidExperimentationLimitNotReached() {
//        doReturn(true).when(exp).getIsRapidExperiment();
//        doReturn(42).when(exp).getUserCap();
        when(exp.getIsRapidExperiment()).thenReturn(true);
        when(exp.getUserCap()).thenReturn(42);
        when(ar.getBucketAssignmentCount(exp).getTotalUsers().getBucketAssignments()).thenReturn(40l);

        AssignmentCountEnvelope env = new AssignmentCountEnvelope(ar, cass, mysql, exp, assignment, true, el, date, true, true);
        env.run();

        verifyZeroInteractions(cass);
        verifyZeroInteractions(mysql);
    }


    /**
     * This tests what happens if the update in Cassandra fails.
     */
    @Test
    public void testRapidExperimentationCassandraFail() {
        when(exp.getIsRapidExperiment()).thenReturn(true);
        when(exp.getUserCap()).thenReturn(7);
        when(ar.getBucketAssignmentCount(exp).getTotalUsers().getBucketAssignments()).thenReturn(41l);
        when(cass.updateExperimentState(exp, Experiment.State.PAUSED)).thenThrow(new RuntimeException("Cassandra failed"));

        AssignmentCountEnvelope env = new AssignmentCountEnvelope(ar, cass, mysql, exp, assignment, true, el, date, true, true);
        env.run();

        //if cassandra fails I don't want to call mysql!
        verify(cass).updateExperimentState(exp, Experiment.State.PAUSED);
        verifyZeroInteractions(mysql);
    }

    /**
     * This tests what happens if the update in Cassandra succeeds.
     */
    @Test
    public void testRapidExperimentationCassandraSuccess() {
        when(exp.getIsRapidExperiment()).thenReturn(true);
        when(exp.getUserCap()).thenReturn(41); //41 users already assigned
        when(ar.getBucketAssignmentCount(exp).getTotalUsers().getBucketAssignments()).thenReturn(41l);
        when(cass.updateExperimentState(exp, Experiment.State.PAUSED)).thenReturn(exp);
        when(mysql.updateExperimentState(exp, Experiment.State.PAUSED)).thenReturn(exp);

        AssignmentCountEnvelope env = new AssignmentCountEnvelope(ar, cass, mysql, exp, assignment, true, el, date, true, true);
        env.run();

        //if cassandra fails I don't want to call mysql!
        verify(cass).updateExperimentState(exp, Experiment.State.PAUSED);
        verify(mysql).updateExperimentState(exp, Experiment.State.PAUSED);

    }


}