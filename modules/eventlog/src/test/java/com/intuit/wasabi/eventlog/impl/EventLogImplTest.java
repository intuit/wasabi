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
package com.intuit.wasabi.eventlog.impl;

import com.intuit.wasabi.eventlog.EventLogListener;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.SimpleEvent;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link EventLogImpl}.
 */
public class EventLogImplTest {

    @Test
    public void testConstructor() throws Exception {
        EventLogImpl eventLog = new EventLogImpl(2, 4);

        Assert.assertNotNull(eventLog.listeners);
    }

    @Test
    public void testRegister() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        eventLog.register(eventLogListener);

        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listeners = eventLog.listeners;
        Assert.assertTrue(listeners.containsKey(eventLogListener));
        Assert.assertTrue(listeners.get(eventLogListener).equals(Collections.singletonList(EventLogEvent.class)));
    }

    @Test
    public void testRegisterSpecific() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        eventLog.register(eventLogListener, Arrays.asList(EventLogEvent.class, SimpleEvent.class));

        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listeners = eventLog.listeners;
        Assert.assertTrue(listeners.containsKey(eventLogListener));
        Assert.assertTrue(listeners.get(eventLogListener).contains(EventLogEvent.class));
        Assert.assertTrue(listeners.get(eventLogListener).contains(SimpleEvent.class));
        Assert.assertEquals(2, listeners.get(eventLogListener).size());
    }

    @Test
    public void testRegisterString() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        eventLog.register(eventLogListener, "com.intuit.wasabi.eventlog.events.EventLogEvent", "SimpleEvent");

        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listeners = eventLog.listeners;
        Assert.assertTrue(listeners.containsKey(eventLogListener));
        Assert.assertTrue(listeners.get(eventLogListener).contains(EventLogEvent.class));
        Assert.assertTrue(listeners.get(eventLogListener).contains(SimpleEvent.class));
        Assert.assertEquals(2, listeners.get(eventLogListener).size());
    }

    @Test
    public void testRegisterStringFail() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);

        try {
            eventLog.register(eventLogListener, "SomeNonExistentEventLogEvent");
            Assert.fail("Should not be able to register SomeNonExistentEventLogEvent!");
        } catch (ClassNotFoundException notFound) {
            // do nothing
        }
    }

    @Test
    @Ignore
    public void testPostEvent() throws Exception {
        // FIXME: this test still relies on threads, we need to polish it
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        eventLog.register(eventLogListener, Collections.singletonList(SimpleEvent.class));

        SimpleEvent simpleEvent = new SimpleEvent("Simple event");
        eventLog.postEvent(simpleEvent);

        Assert.assertTrue("Deque doesn't contain correct event.", eventLog.eventDeque.contains(simpleEvent));

        Thread eventLogThread = runThread(eventLog);
        long time = 0;
        if (eventLog.eventDeque.size() > 0) {
            while (eventLog.eventDeque.size() > 0) {
                Thread.sleep(50);
                time += 50;
                if (time > 5000) {
                    break;
                }
            }
        }
        eventLogThread.interrupt();

        Mockito.verify(eventLogListener, Mockito.times(1)).postEvent(simpleEvent);
    }

    @Test
    public void testPostEventFail() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        eventLog.register(eventLogListener, Collections.singletonList(SimpleEvent.class));

        EventLogEvent eventLogEvent = Mockito.mock(EventLogEvent.class);
        Assert.assertFalse("Deque contains invalid Event.", eventLog.eventDeque.contains(eventLogEvent));
    }

    @Test
    public void testPostEventNull() throws Exception {
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        eventLog.postEvent(null);
        Assert.assertFalse("Deque contains null.", eventLog.eventDeque.contains(null));
    }

    @Ignore
    @Test
    public void testRunFinally() throws Exception {
        // FIXME: this test still relies on threads, we need to polish it
        EventLogImpl eventLog = new EventLogImpl(1, 1);
        EventLogEvent eventLogEvent = Mockito.mock(EventLogEvent.class);

        final int eventCount = 100;
        for (int i = 0; i < eventCount; ++i) {
            eventLog.postEvent(eventLogEvent);
        }
        Assert.assertEquals("Number of elements in deque is incorrect.", eventCount, eventLog.eventDeque.size());

        runThread(eventLog).interrupt();

        int time = 0;
        while (eventLog.eventDeque.size() > 0) {
            Thread.sleep(50);
            time += 50;
            if (time > 5000) {
                break;
            }
        }

        Assert.assertEquals("There are still elements in the deque.", 0, eventLog.eventDeque.size());
    }

    private Thread runThread(EventLogImpl eventLog) throws Exception {
        // start the thread and immediately stop it
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();
        return eventLogThread;
    }
}
