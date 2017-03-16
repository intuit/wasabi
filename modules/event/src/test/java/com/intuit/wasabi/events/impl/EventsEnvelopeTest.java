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
package com.intuit.wasabi.events.impl;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.exceptions.ApplicationNotFoundException;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.slf4j.LoggerFactory.getLogger;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LoggerFactory.class)
public class EventsEnvelopeTest {

    private EventsEnvelope eventsEnvelope;
    @Mock
    private Transaction transaction;
    @Mock
    private Assignment assignment;
    @Mock
    private Event event;
    @Mock
    private Logger logger;

    @Before
    public void setup() {
        assignment = Mockito.mock(Assignment.class);
        transaction = Mockito.mock(Transaction.class);
        event = Mockito.mock(Event.class);
        event.setName(Event.Name.valueOf("someEventName"));
        event.setContext(Context.valueOf("PROD"));
        event.setValue("someEventValue");
        event.setPayload(Event.Payload.valueOf("someEventPayload"));

        eventsEnvelope = new EventsEnvelope(assignment, event, transaction);
    }

    @Test
    public void testRecordEvent() throws Exception {
        when(event.getName()).thenReturn(Event.Name.valueOf("ASSIGNMENT"));
        when(event.getType()).thenReturn(Event.Type.IMPRESSION);
        when(assignment.getUserID()).thenReturn(User.ID.valueOf("user-a"));
        Bucket.Label bucketLabel = Bucket.Label.valueOf("red");
        when(assignment.getBucketLabel()).thenReturn(bucketLabel);
        Experiment.ID experimentID = Experiment.ID.valueOf("27ec196b-e90d-4752-9baf-5c3f11d9f78a");
        when(assignment.getExperimentID()).thenReturn(experimentID);

        eventsEnvelope.run();

        verify(event, times(1)).getName();
        verify(assignment, times(1)).getBucketLabel();
        verify(transaction, times(1)).insert(any(String.class), any(String.class), any(Experiment.ID.class), any(Bucket.Label.class),
                any(String.class), any(Context.class), any(Date.class));
    }

    @Test
    public void testRecordEventThrowsWasabiClientException() throws Exception {
        mockStatic(LoggerFactory.class);

        given(event.getName()).willReturn(Event.Name.valueOf("ASSIGNMENT"));
        given(event.getType()).willReturn(Event.Type.IMPRESSION);
        given(assignment.getUserID()).willReturn(User.ID.valueOf("user-a"));
        Bucket.Label bucketLabel = Bucket.Label.valueOf("red");
        given(assignment.getBucketLabel()).willReturn(bucketLabel);
        Experiment.ID experimentID = Experiment.ID.valueOf("27ec196b-e90d-4752-9baf-5c3f11d9f78a");
        given(assignment.getExperimentID()).willReturn(experimentID);
        WasabiClientException jce = new ApplicationNotFoundException("");
        given(getLogger(any(Class.class))).willReturn(logger);
        logger.warn(any(String.class));
        BDDMockito.willThrow(jce).given(transaction).
                insert(any(String.class), any(String.class), any(Experiment.ID.class),
                        any(Bucket.Label.class), any(String.class), any(Date.class), any(String.class));
        eventsEnvelope.run();

        verify(event, times(1)).getName();
        verify(assignment, times(1)).getBucketLabel();
        verify(transaction, times(1)).insert(any(String.class), any(String.class), any(Experiment.ID.class),
                any(Bucket.Label.class), any(String.class), any(Date.class), any(String.class));
        // Find a way to validate log message
        verify(logger, times(1)).warn(any(String.class));
    }

    @Test
    public void testRecordEventWithNullBucketExperiment() throws Exception {
        when(event.getName()).thenReturn(null);
        when(event.getType()).thenReturn(Event.Type.IMPRESSION);
        when(assignment.getUserID()).thenReturn(null);
        when(assignment.getBucketLabel()).thenReturn(null);
        when(assignment.getExperimentID()).thenReturn(null);

        eventsEnvelope.run();

        verify(transaction, times(0)).insert(any(String.class), any(String.class), any(Experiment.ID.class), any(Bucket.Label.class),
                any(String.class), any(Context.class), any(Date.class));

    }

}

