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
package com.intuit.wasabi.assignment.impl;

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.HttpHeaders;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class NoopDatabaseAssignmentEnvelopeTest {

    private NoopDatabaseAssignmentEnvelope envelope;

    @Before
    public void setUp() throws Exception {
        envelope = new NoopDatabaseAssignmentEnvelope();
    }

    @Test
    public void test() {
        assertEquals(envelope, envelope.withPayload(new AssignmentEnvelopePayload(User.ID.valueOf("user"),
                Context.valueOf("PROD"), true, true, true, SegmentationProfile.from(new HashMap<>()).build(),
                Assignment.Status.EXISTING_ASSIGNMENT, Bucket.Label.valueOf("A"), Page.Name.valueOf("Page"),
                Application.Name.valueOf("Application"), Experiment.Label.valueOf("Experiment"),
                Experiment.ID.newInstance(), new Date(), Mockito.mock(HttpHeaders.class))));

        envelope.run();
    }

}
