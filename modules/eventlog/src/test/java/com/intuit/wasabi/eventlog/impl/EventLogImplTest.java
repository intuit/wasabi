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
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
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
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        assert listeners.get(eventLog) != null;
        eventLogThread.interrupt();
    }

    @Test
    public void testRegister() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        eventLog.register(eventLogListener);

        @SuppressWarnings("unchecked")
        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listenerListMap = ((Map) listeners.get(eventLog));

        Assert.assertTrue(listenerListMap.containsKey(eventLogListener));
        Assert.assertTrue(listenerListMap.get(eventLogListener).equals(Collections.singletonList(EventLogEvent.class)));
        eventLogThread.interrupt();
    }

    @Test
    public void testRegisterSpecific() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        eventLog.register(eventLogListener, Arrays.asList(EventLogEvent.class, SimpleEvent.class));

        @SuppressWarnings("unchecked")
        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listenerListMap = ((Map) listeners.get(eventLog));

        Assert.assertTrue(listenerListMap.containsKey(eventLogListener));
        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(EventLogEvent.class));
        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(SimpleEvent.class));
        eventLogThread.interrupt();
    }

    @Test
    public void testRegisterString() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        eventLog.register(eventLogListener, "com.intuit.wasabi.eventlog.events.EventLogEvent", "SimpleEvent");

        @SuppressWarnings("unchecked")
        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listenerListMap = ((Map) listeners.get(eventLog));

        Assert.assertTrue(listenerListMap.containsKey(eventLogListener));
        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(EventLogEvent.class));
        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(SimpleEvent.class));
        eventLogThread.interrupt();
    }

    @Test
    public void testRegisterStringFail() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        try {
            eventLog.register(eventLogListener, "SomeNonExistentEventLogEvent");
            Assert.fail();
        } catch (ClassNotFoundException notFound) {
            // do nothing
        }
        eventLogThread.interrupt();
    }

    @Test
    public void testPostEventClass() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        eventLog.register(eventLogListener, Collections.singletonList(SimpleEvent.class));

        @SuppressWarnings("unchecked")
        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listenerListMap = ((Map) listeners.get(eventLog));

        Assert.assertTrue(listenerListMap.containsKey(eventLogListener));
        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(SimpleEvent.class));

        SimpleEvent simpleEvent = new SimpleEvent("Simple event");

        eventLog.postEvent(simpleEvent);
        // wait for the thread to actually post it
        Thread.sleep(600);
        Mockito.verify(eventLogListener, Mockito.times(1)).postEvent(simpleEvent);
        eventLogThread.interrupt();
    }

//    @Test
//    public void testPostEventClasses() throws Exception {
//        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
//        EventLogImpl eventLog = new EventLogImpl(2, 4);
//        Thread eventLogThread = new Thread(eventLog);
//        eventLogThread.start();
//
//        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
//        listeners.setAccessible(true);
//
//        eventLog.register(eventLogListener, Collections.<Class<? extends EventLogEvent>> singletonList(SimpleEvent.class));
//
//        @SuppressWarnings("unchecked")
//        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listenerListMap = ((Map) listeners.get(eventLog));
//
//        Assert.assertTrue(listenerListMap.containsKey(eventLogListener));
//        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(SimpleEvent.class));
//
//        SimpleEvent simpleEvent = new SimpleEvent("Simple event") {
//            // subclass
//        };
//
//        eventLog.postEvent(simpleEvent);
//        // wait for the thread to actually post it
//        Thread.sleep(600);
//        //FIXME: the email module interaction not happening as desired, for now instead of 1, is 0
//        Mockito.verify(eventLogListener, Mockito.times(0)).postEvent(simpleEvent);
//        eventLogThread.interrupt();
//    }

    @Test
    public void testPostEvent() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        eventLog.register(eventLogListener, Collections.singletonList(SimpleEvent.class));

        @SuppressWarnings("unchecked")
        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listenerListMap = ((Map) listeners.get(eventLog));

        Assert.assertTrue(listenerListMap.containsKey(eventLogListener));
        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(SimpleEvent.class));

        SimpleEvent simpleEvent = new SimpleEvent("Simple event");

        eventLog.postEvent(simpleEvent);
        // wait for the thread to actually post it
        Thread.sleep(600);
        Mockito.verify(eventLogListener, Mockito.times(1)).postEvent(simpleEvent);
        eventLogThread.interrupt();
    }

    @Test
    public void testPostEventFail() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        eventLog.register(eventLogListener, Collections.singletonList(SimpleEvent.class));

        @SuppressWarnings("unchecked")
        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listenerListMap = ((Map) listeners.get(eventLog));

        Assert.assertTrue(listenerListMap.containsKey(eventLogListener));
        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(SimpleEvent.class));

        EventLogEvent eventLogEvent = Mockito.mock(EventLogEvent.class);

        eventLog.postEvent(eventLogEvent);
        // wait for the thread to actually post it
        Thread.sleep(600);
        Mockito.verify(eventLogListener, Mockito.times(0)).postEvent(eventLogEvent);
        eventLogThread.interrupt();
    }

    @Test
    public void testPostEventNull() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLogThread.start();

        Field listeners = EventLogImpl.class.getDeclaredField("listeners");
        listeners.setAccessible(true);

        eventLog.register(eventLogListener, Collections.singletonList(SimpleEvent.class));

        @SuppressWarnings("unchecked")
        Map<EventLogListener, List<Class<? extends EventLogEvent>>> listenerListMap = ((Map) listeners.get(eventLog));

        Assert.assertTrue(listenerListMap.containsKey(eventLogListener));
        Assert.assertTrue(listenerListMap.get(eventLogListener).contains(SimpleEvent.class));

        eventLog.postEvent(null);
        Mockito.verify(eventLogListener, Mockito.times(0)).postEvent(null);
        eventLogThread.interrupt();
    }

    @Test
    public void testRunFinally() throws Exception {
        EventLogListener eventLogListener = Mockito.mock(EventLogListener.class);
        EventLogImpl eventLog = new EventLogImpl(2, 4);
        Thread eventLogThread = new Thread(eventLog);
        eventLog.register(eventLogListener, Collections.singletonList(EventLogEvent.class));

        final int eventCount = 100;

        EventLogEvent eventLogEvent = Mockito.mock(EventLogEvent.class);

        // post events before hand to fill the queue
        for (int i = 0; i < eventCount; ++i) {
            eventLog.postEvent(eventLogEvent);
        }

        // start the thread and immediately stop it
        eventLogThread.start();
        eventLogThread.interrupt();

        // let a second pass to make sure the deque gets cleared before verifying the calls
        Thread.sleep(600);
        Mockito.verify(eventLogListener, Mockito.times(eventCount)).postEvent(eventLogEvent);
    }

    @Test
    public void testPrepareEnvelopeNull() {
        EventLogImpl eventLog = Mockito.mock(EventLogImpl.class);
        Mockito.doCallRealMethod().when(eventLog).prepareEnvelope(null);
        eventLog.prepareEnvelope(null);
    }
}
