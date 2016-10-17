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
package com.intuit.wasabi.assignmentobjects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(Parameterized.class)
public class AssignmentEnvelopePayloadTest {
    private static final Logger LOG = LoggerFactory.getLogger(AssignmentEnvelopePayloadTest.class);

    private final AssignmentEnvelopePayload testPayload;

    public AssignmentEnvelopePayloadTest(AssignmentEnvelopePayload testPayload) {
        this.testPayload = testPayload;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> parameters() {
        List<Object[]> testCases = new ArrayList<>();

        Map<String, Object> segmentationProfile = new HashMap<>(0);
        testCases.add(new Object[]{new AssignmentEnvelopePayload(User.ID.valueOf("User 0"), Context.valueOf("PROD"),
                true, true, true, SegmentationProfile.from(segmentationProfile).build(), Assignment.Status.EXISTING_ASSIGNMENT,
                Bucket.Label.valueOf("Bucket-0"), Page.Name.valueOf("Page"), Application.Name.valueOf("Application-0"),
                Experiment.Label.valueOf("Experiment-0"), Experiment.ID.newInstance(), new Date())});

        Map<String, Object> segmentationProfile1 = new HashMap<>(3);
        segmentationProfile1.put("state", "CA");
        segmentationProfile1.put("salary", 30000);
        segmentationProfile1.put("visited", "home");
        testCases.add(new Object[]{new AssignmentEnvelopePayload(User.ID.valueOf("User 1"), Context.valueOf("PROD"),
                true, true, true, SegmentationProfile.from(segmentationProfile1).build(), Assignment.Status.EXISTING_ASSIGNMENT,
                Bucket.Label.valueOf("Bucket-1"), Page.Name.valueOf("Page"), Application.Name.valueOf("Application-1"),
                Experiment.Label.valueOf("Experiment-1"), Experiment.ID.newInstance(), new Date())});
        return testCases;
    }

    @Test
    public void testDeAndSerialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(testPayload);
        LOG.debug("Exported JSON: {}", json);
        AssignmentEnvelopePayload payloadRestored = objectMapper.readValue(json, AssignmentEnvelopePayload.class);
        Assert.assertEquals("Payload deserialization and serialization does not match.", payloadRestored, testPayload);
    }

}
