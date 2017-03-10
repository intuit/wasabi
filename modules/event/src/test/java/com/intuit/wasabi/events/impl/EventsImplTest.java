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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Assignments assignments;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TransactionFactory transactionFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Provider<Envelope<EventEnvelopePayload, DatabaseExport>> eventEnvelopeProvider;

    EventsImpl eventsImpl;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    EventList events;

    @Mock
    Event event;


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Assignment assignment;

    Application.Name appName = Application.Name.valueOf("abcd");

    Experiment.Label label = Experiment.Label.valueOf("l1");

    User.ID userId = User.ID.valueOf("u1");

    Set<Context> contextSet;

    List<Event> eventsList;

    protected boolean createdEventEnvelope;

    private Context c1;

    @Mock
    EventIngestionExecutor mockEventIngestionExecutor;

    @Before
    public void setUp() {

        HashMap<String, EventIngestionExecutor> eventIngestioExecutors = new HashMap<String, EventIngestionExecutor>();
        eventIngestioExecutors.put("Mock", mockEventIngestionExecutor);
        eventsImpl = new EventsImpl(eventIngestioExecutors, 2, assignments, transactionFactory) {

            @Override
            protected EventsEnvelope makeEventEnvelope(Assignment assignment, Event event) {
                createdEventEnvelope = true;
                return super.makeEventEnvelope(assignment, event);
            }

        };
        contextSet = new HashSet<Context>();
        eventsList = new ArrayList<Event>();
        c1 = Context.valueOf("c1");
        createdEventEnvelope = false;
    }

    @After
    public void tearDown() {
        eventsImpl.shutdown();
    }

    @Test
    public void testQueueLength() {
        Map<String, Integer> queueLengthMap = new HashMap<String, Integer>();
        queueLengthMap.put(EventsImpl.MYSQL, new Integer(0));
        queueLengthMap.put("mock", new Integer(0));
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
        eventsImpl.recordEvents(appName, label
                , userId, events, contextSet);
        assertFalse("event envelope should not be created", createdEventEnvelope);
    }

    @Test
    public void testRecordEventsOneContextOneEvent() {
        contextSet.add(c1);
        eventsList.add(event);
        given(event.getContext()).willReturn(c1);
        given(assignments.getExistingAssignment(userId, appName, label, contextSet.iterator().next())).willReturn(assignment);
        given(events.getEvents()).willReturn(eventsList);
        eventsImpl.recordEvents(appName, label
                , userId, events, contextSet);

        assertTrue("event envelope should be created", createdEventEnvelope);
    }
}
