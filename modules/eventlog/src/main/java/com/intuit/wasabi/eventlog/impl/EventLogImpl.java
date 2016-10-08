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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.EventLogListener;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.SimpleEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Class.forName;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * The EventLogImpl can be used to log events. It is possible to subscribe to specific events and get notified
 * whenever an event occurs, to log events or handle them otherwise (for example to notify users of changed events).
 */
public class EventLogImpl implements EventLog {

    private static final Logger LOGGER = getLogger(EventLogImpl.class);
    /**
     * The listener subscriptions.
     */
    final Map<EventLogListener, List<Class<? extends EventLogEvent>>> listeners;
    /**
     * the event Deque
     */
    final Deque<EventLogEvent> eventDeque;
    /**
     * Executes the {@link EventLogEventEnvelope}s.
     */
    private final ThreadPoolExecutor eventPostThreadPoolExecutor;

    /**
     * Creates the event pool executor. Should be called by Guice.
     *
     * @param threadPoolSizeCore named instance threadpoolsize.core
     * @param threadPoolSizeMax  named instance threadpoolsize.max
     */
    @Inject
    public EventLogImpl(@Named("eventlog.threadpoolsize.core") int threadPoolSizeCore,
                        @Named("eventlog.threadpoolsize.max") int threadPoolSizeMax) {
        listeners = new ConcurrentHashMap<>();
        eventDeque = new ConcurrentLinkedDeque<>();

        eventPostThreadPoolExecutor = new ThreadPoolExecutor(threadPoolSizeCore, threadPoolSizeMax, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(EventLogListener listener) {
        this.register(listener, Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(EventLogListener listener, List<Class<? extends EventLogEvent>> events) {
        if (events.isEmpty()) {
            events = Collections.singletonList(EventLogEvent.class);
        }
        listeners.put(listener, events);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void register(EventLogListener listener, String... events)
            throws ClassNotFoundException {
        String packagePrefix = "com.intuit.wasabi.eventlog.events.";

        List<Class<? extends EventLogEvent>> eventList = new ArrayList<>(events.length);
        for (String event : events) {
            Class<? extends EventLogEvent> eventLogClass;

            try {
                eventLogClass = (Class<? extends EventLogEvent>) forName(event);
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Event class " + event + " not found, trying " + packagePrefix + event + " !", e);
                eventLogClass = (Class<? extends EventLogEvent>) forName(packagePrefix + event);
            }
            eventList.add(eventLogClass);
        }
        this.register(listener, eventList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(EventLogEvent event) {
        if (Objects.isNull(event)) {
            LOGGER.warn("null-Event skipped.");
            return;
        }
        eventDeque.offerLast(event);
    }

    /**
     * Returns true if {@code listener} is registered for {@code event}.
     *
     * @param listener the listener to check
     * @param event    the event
     * @return true if the listener is registered for the event
     */
    boolean isSubscribed(EventLogListener listener, EventLogEvent event) {
        List<Class<? extends EventLogEvent>> subscriptions = listeners.get(listener);

        // class and super classes
        Class<?> eventClass = event.getClass();
        while (!eventClass.equals(Object.class)) {
            if (subscriptions.contains(eventClass)) {
                return true;
            }

            // interfaces
            for (Class<?> subscription : eventClass.getInterfaces()) {
                if (subscriptions.contains(subscription)) {
                    return true;
                }
            }
            eventClass = eventClass.getSuperclass();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            do {
                if (!eventDeque.isEmpty()) {
                    prepareEnvelope(eventDeque.pollFirst());
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Interrupted while sleeping.", e);
                        Thread.currentThread().interrupt();
                    }
                }
            } while (!Thread.currentThread().isInterrupted() && Thread.currentThread().isAlive());
        } finally {
            LOGGER.info("Shutting down event system, posting remaining events -- new events will not be processed.");
            if (!eventDeque.isEmpty()) {
                eventDeque.forEach(this::prepareEnvelope);
                eventDeque.clear();
            }
        }
    }

    /**
     * Prepares {@link EventLogEventEnvelope}s for all registered listeners and submits them to the ThreadPoolExecutor.
     *
     * @param event the vent to prepare
     */
    private void prepareEnvelope(final EventLogEvent event) {
        EventLogEvent realEvent = Optional.ofNullable(event).orElseGet(() -> new SimpleEvent("Someone sent a null event."));
        listeners.keySet().parallelStream()
                .filter(eventLogListener -> isSubscribed(eventLogListener, realEvent))
                .forEach(eventLogListener -> eventPostThreadPoolExecutor
                        .submit(new EventLogEventEnvelope(realEvent, eventLogListener)));
    }
}
