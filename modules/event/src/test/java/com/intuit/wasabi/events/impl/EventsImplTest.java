package com.intuit.wasabi.events.impl;

import com.google.inject.Provider;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.EventList;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.eventobjects.EventEnvelopePayload;
import com.intuit.wasabi.events.EventIngestionExecutor;
import com.intuit.wasabi.exceptions.AssignmentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.export.DatabaseExport;
import com.intuit.wasabi.export.Envelope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class EventsImplTest {

    private boolean createdEventEnvelope;
    private EventsImpl eventsImpl;
    private Application.Name appName = Application.Name.valueOf("abcd");
    private Experiment.Label label = Experiment.Label.valueOf("l1");
    private User.ID userId = User.ID.valueOf("u1");
    private Set<Context> contextSet;
    private List<Event> eventsList;
    private Context c1;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Assignments assignments;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private TransactionFactory transactionFactory;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Provider<Envelope<EventEnvelopePayload, DatabaseExport>> eventEnvelopeProvider;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EventList events;
    @Mock
    private Event event;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Assignment assignment;
    @Mock
    private EventIngestionExecutor mockEventIngestionExecutor;

    @Before
    public void setUp() {

        HashMap<String, EventIngestionExecutor> eventIngestioExecutors = new HashMap<>();
        eventIngestioExecutors.put("Mock", mockEventIngestionExecutor);
        eventsImpl = new EventsImpl(eventIngestioExecutors, 2, assignments, transactionFactory) {

            @Override
            protected EventsEnvelope makeEventEnvelope(Assignment assignment, Event event) {
                createdEventEnvelope = true;
                return super.makeEventEnvelope(assignment, event);
            }

        };
        contextSet = new HashSet<>();
        eventsList = new ArrayList<>();
        c1 = Context.valueOf("c1");
        createdEventEnvelope = false;
    }

    @After
    public void tearDown() {
        eventsImpl.shutdown();
    }

    @Test
    public void testQueueLength() {
        Map<String, Integer> queueLengthMap = new HashMap<>();
        queueLengthMap.put(EventsImpl.MYSQL, 0);
        queueLengthMap.put("mock", 0);
        assertThat(eventsImpl.queuesLength(), is(queueLengthMap));
    }

    @Test
    public void testRecordEventsNoContextNoEvents() {
        eventsImpl.recordEvents(appName, label
                , userId, events, contextSet);
        assertFalse("event envelope should not be created", createdEventEnvelope);
    }

    @Test
    public void testRecordEventsOneContextNoEvent() {
        contextSet.add(c1);
        eventsImpl.recordEvents(appName, label, userId, events, contextSet);
        assertFalse("event envelope should not be created", createdEventEnvelope);
    }

    @Test
    public void testRecordEventsOneContextOneEvent() {
        contextSet.add(c1);
        eventsList.add(event);
        given(event.getContext()).willReturn(c1);
        given(assignments.getAssignment(userId, appName, label, contextSet.iterator().next(),
                false, false, null, null)).willReturn(assignment);
        given(events.getEvents()).willReturn(eventsList);
        eventsImpl.recordEvents(appName, label, userId, events, contextSet);

        assertTrue("event envelope should be created", createdEventEnvelope);
    }

    @Test(expected = AssignmentNotFoundException.class)
    public void testRecordEventsNoContext() throws Exception {
        eventsList.add(event);
        given(event.getContext()).willReturn(c1);
        given(assignments.getAssignment(userId, appName, label, Context.valueOf("c2"),
                false, false, null, null)).willReturn(assignment);
        given(events.getEvents()).willReturn(eventsList);
        eventsImpl.recordEvents(appName, label, userId, events, contextSet);
        assertFalse("event envelope should not be created", createdEventEnvelope);
    }
}
