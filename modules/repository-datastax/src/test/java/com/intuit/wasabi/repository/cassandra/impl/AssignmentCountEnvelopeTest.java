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
    }

    @Test
    public void testErrorInAssignUserToExports() {

        doThrow(new RuntimeException("exception")).when(ar).assignUserToExports(Mockito.eq(assignment), Mockito.any());

        AssignmentCountEnvelope env = new AssignmentCountEnvelope(ar, cass, mysql, exp, assignment, true, el, date, true, true);
        env.run();
    }
}