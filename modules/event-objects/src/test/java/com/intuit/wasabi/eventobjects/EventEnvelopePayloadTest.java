package com.intuit.wasabi.eventobjects;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.apache.cassandra.utils.UUIDGen;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class EventEnvelopePayloadTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Assignment assignment;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Event event;

    Application.Name name = Application.Name.valueOf("a1");

    Experiment.Label label = Experiment.Label.valueOf("l1");

    @Test
    public void testCreatingInstance() {
        final UUID uuid = UUIDGen.getTimeUUID();
        EventEnvelopePayload payload = new EventEnvelopePayload(
                name, label,
                assignment, event) {

            @Override
            protected UUID makeUUID() {
                return uuid;
            }

        };

        assertEquals(name, payload.getApplicationName());
        assertEquals(label, payload.getExperimentLabel());
        assertSame(event, payload.getEvent());
        assertSame(assignment, payload.getAssignment());

    }

    @Test
    public void testUUID() {
        EventEnvelopePayload payload = new EventEnvelopePayload(
                null, null,
                null, null);

        assertTrue(payload.makeUUID() instanceof UUID);
    }

    @Test
    public void testCreatingInstanceWithNull() {
        final UUID uuid = UUIDGen.getTimeUUID();
        EventEnvelopePayload payload = new EventEnvelopePayload(
                null, null,
                null, null) {

            @Override
            protected UUID makeUUID() {
                return uuid;
            }

        };

        payload.setApplicationName(name);
        payload.setAssignment(assignment);
        payload.setExperimentLabel(label);
        payload.setEvent(event);
        assertEquals(name, payload.getApplicationName());
        assertEquals(label, payload.getExperimentLabel());
        assertSame(event, payload.getEvent());
        assertSame(assignment, payload.getAssignment());

    }

    @Test
    public void testToJsonMethod() {
        EventEnvelopePayload payload = new EventEnvelopePayload(
                name, label,
                assignment, event) {

            @Override
            protected UUID makeUUID() {
                return UUID.fromString("d9f92dd0-05be-11e6-86e9-0bf850ef5299");
            }

        };
        String jsonpayload = payload.toJson();
        assertThat(jsonpayload, is("{\"experimentLabel\":\"l1\",\"eventPayload\":null,\"eventType\":\"null\",\"userID\":null,\"epochTimestamp\":0,\"messageType\":\"EVENT\",\"context\":null,\"eventName\":\"null\",\"experimentID\":null,\"time_uuid\":\"d9f92dd0-05be-11e6-86e9-0bf850ef5299\",\"value\":null,\"applicationName\":\"a1\",\"bucketLabel\":null}"));
    }
}