package com.intuit.wasabi.eventobjects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@RunWith(Parameterized.class)
public class EventEnvelopePayloadTest {
    private static final Logger LOG = LoggerFactory.getLogger(EventEnvelopePayloadTest.class);

    private final EventEnvelopePayload testPayload;

    public EventEnvelopePayloadTest(EventEnvelopePayload testPayload) {
        this.testPayload = testPayload;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> parameters() {
        List<Object[]> testCases = new ArrayList<>();

        Experiment experiment0 = Experiment.withID(Experiment.ID.newInstance())
                .withLabel(Experiment.Label.valueOf("Experiment-0"))
                .withApplicationName(Application.Name.valueOf("Application-0"))
                .build();
        Assignment assignment0 = Assignment.newInstance(experiment0.getID()).build();
        Event event0 = new Event();
        testCases.add(new Object[]{new EventEnvelopePayload(experiment0.getApplicationName(), experiment0.getLabel(),
                assignment0, event0)});

        Experiment experiment1 = Experiment.withID(Experiment.ID.newInstance())
                .withLabel(Experiment.Label.valueOf("Experiment-1"))
                .withApplicationName(Application.Name.valueOf("Application-1"))
                .build();
        Context context1 = Context.valueOf("PREPROD");
        Assignment assignment1 = Assignment.newInstance(experiment1.getID())
                .withApplicationName(experiment1.getApplicationName())
                .withBucketEmpty(true)
                .withCacheable(true)
                .withContext(context1)
                .withBucketLabel(Bucket.Label.valueOf("Bucket-1"))
                .withCreated(new Date(Instant.now().minusSeconds(120).toEpochMilli()))
                .withUserID(User.ID.valueOf("User-1"))
                .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                .build();
        Event event1 = new Event();
        event1.setName(Event.Name.valueOf("CLICKED_BUTTON"));
        event1.setContext(context1);
        event1.setTimestamp(new Date());
        event1.setPayload(Event.Payload.valueOf("{\"state\":\"CA\",\"visited\":[\"home\",\"about\"]}"));
        testCases.add(new Object[]{new EventEnvelopePayload(experiment1.getApplicationName(), experiment1.getLabel(),
                assignment1, event1)});

        return testCases;
    }

    @Test
    public void testDeAndSerialization() throws java.io.IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(testPayload);
        LOG.debug("Exported JSON: {}", json);
        EventEnvelopePayload payloadRestored = objectMapper.readValue(json, EventEnvelopePayload.class);
        Assert.assertEquals("Payload deserialization and serialization does not match.", payloadRestored, testPayload);
        LOG.debug("Passed.");
    }

}
