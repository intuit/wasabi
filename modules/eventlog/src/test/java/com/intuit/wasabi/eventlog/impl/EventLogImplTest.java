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
package com.intuit.wasabi.eventlog.impl;

import com.intuit.wasabi.eventlog.EventLogListener;
import com.intuit.wasabi.eventlog.events.BucketChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.SimpleEvent;
import org.junit.Assert;
import org.junit.Before;
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

    private EventLogImpl eventLog;
    private EventLogListener eventLogListener;

    @Before
    public void setup() {
        eventLog = new EventLogImpl(2, 4);
        eventLogListener = Mockito.mock(EventLogListener.class);
    }

    @Test
    public void testRegister() {
        eventLog.register(eventLogListener);

        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listeners = eventLog.listeners;

        Assert.assertTrue(listeners.containsKey(eventLogListener));
        Assert.assertTrue(listeners.get(eventLogListener).equals(Collections.singletonList(EventLogEvent.class)));
    }

    @Test
    public void testRegisterSpecific() {
        eventLog.register(eventLogListener, Arrays.asList(EventLogEvent.class, SimpleEvent.class));

        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listeners = eventLog.listeners;

        Assert.assertTrue(listeners.containsKey(eventLogListener));
        Assert.assertTrue(listeners.get(eventLogListener).contains(EventLogEvent.class));
        Assert.assertTrue(listeners.get(eventLogListener).contains(SimpleEvent.class));
        Assert.assertEquals(2, listeners.get(eventLogListener).size());
    }

    @Test
    public void testRegisterString() throws Exception {
        eventLog.register(eventLogListener, "com.intuit.wasabi.eventlog.events.EventLogEvent", "SimpleEvent");

        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listeners = eventLog.listeners;

        Assert.assertTrue(listeners.containsKey(eventLogListener));
        Assert.assertTrue(listeners.get(eventLogListener).contains(EventLogEvent.class));
        Assert.assertTrue(listeners.get(eventLogListener).contains(SimpleEvent.class));
        Assert.assertEquals(2, listeners.get(eventLogListener).size());
    }

    @Test(expected = ClassNotFoundException.class)
    public void testRegisterStringFail() throws Exception {
        eventLog.register(eventLogListener, "SomeNonExistentEventLogEvent");
        Assert.fail("Should not be able to register SomeNonExistentEventLogEvent!");
    }

    @Test
    public void testPostEvent() {
        eventLog.register(eventLogListener, Collections.singletonList(SimpleEvent.class));

        SimpleEvent simpleEvent = new SimpleEvent("Simple event");
        eventLog.postEvent(simpleEvent);
        Assert.assertTrue("Deque doesn't contain correct event.", eventLog.eventDeque.contains(simpleEvent));

        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Mockito.verify(eventLogListener, Mockito.timeout(10000)).postEvent(simpleEvent);
    }

    @Test
    public void testPostEventNull() {
        eventLog.postEvent(null);
        Assert.assertFalse("Deque contains null.", eventLog.eventDeque.contains(null));
    }

    @Test
    public void testIsSubscribed() {
        eventLog.register(eventLogListener, Collections.singletonList(BucketEvent.class));

        Assert.assertTrue("Is not subscribed to BucketEvent but should be!",
                eventLog.isSubscribed(eventLogListener, Mockito.mock(BucketEvent.class)));

        Assert.assertTrue("Is not subscribed to BucketChangeEvent but should be!",
                eventLog.isSubscribed(eventLogListener, Mockito.mock(BucketChangeEvent.class)));

        Assert.assertFalse("Is subscribed to SimpleEvent but should not be!",
                eventLog.isSubscribed(eventLogListener, new SimpleEvent("Test Event")));
    }
}
