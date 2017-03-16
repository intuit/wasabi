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
import com.intuit.wasabi.eventobjects.EventEnvelopePayload;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NoopDatabaseEventEnvelopeTest {

    private NoopDatabaseEventEnvelope envelope;
    private Application.Name applicationName = Application.Name.valueOf("testApp");
    private Experiment.Label experimentLabel = Experiment.Label.valueOf("testLabel");
    private Assignment assignment = Assignment.newInstance(Experiment.ID.newInstance()).build();
    private Event event = new Event();

    @Before
    public void setUp() throws Exception {
        envelope = new NoopDatabaseEventEnvelope();
    }

    @Test
    public void test() {
        assertEquals(envelope, envelope.withPayload(new EventEnvelopePayload(applicationName, experimentLabel, assignment, event)));
        envelope.run();
    }

}
