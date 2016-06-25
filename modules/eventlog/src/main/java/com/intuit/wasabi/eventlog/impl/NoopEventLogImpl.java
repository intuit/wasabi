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

import com.google.inject.Inject;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.EventLogListener;
import com.intuit.wasabi.eventlog.events.EventLogEvent;

import java.util.List;

/**
 * The EventLogImpl can be used to log events. It is possible to subscribe to specific events and get notified
 * whenever an event occurs, to log events or handle them otherwise (for example to notify users of changed events).
 */
public class NoopEventLogImpl implements EventLog {

    /**
     * Creates an event log and initializes the private members.
     */
    @Inject
    public NoopEventLogImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(EventLogListener listener) {
        //This is specific override if no default eventlog, this one does nothing, left to user
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(EventLogListener listener, List<Class<? extends EventLogEvent>> events) {
        //This is specific override if no default eventlog, this one does nothing, left to user
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(EventLogListener listener, String... events) throws ClassNotFoundException {
        //This is specific override if no default eventlog, this one does nothing, left to user
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(EventLogEvent event) {
        //This is specific override if no default eventlog, this one does nothing, left to user
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        //This is specific override if no default eventlog, this one does nothing, left to user
    }
}
