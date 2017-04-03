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
package com.intuit.wasabi.eventlog;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.events.EventLogEvent;

import java.util.List;

/**
 * Provides an event log to register {@link EventLogListener}s and post events.
 */
public interface EventLog extends Runnable {
    /**
     * Denotes the system user, used for automated events.
     */
    UserInfo SYSTEM_USER = new UserInfo.Builder(UserInfo.Username.valueOf("SYSTEM_USER"))
            .withFirstName("System")
            .withLastName("User")
            .withEmail("admin@example.com")
            .withUserId("SystemUser")
            .build();

    /**
     * Registers a listener for all events.
     *
     * @param listener the listener
     */
    void register(EventLogListener listener);

    /**
     * Registers a listener for a list of events.
     *
     * If no events are specified, the listener is registered for all events.
     *
     * @param listener the listener
     * @param events the events
     */
    void register(EventLogListener listener, List<Class<? extends EventLogEvent>> events);

    /**
     * Registers a listener for a list of events.
     *
     * If no events are specified, the listener is registered for all events.
     *
     * The class will first be tried to load as specified, and if that fails it will be tried as if it
     * was in the package {@code com.intuit.wasabi.eventlog.events}.
     *
     * @param listener the listener
     * @param events the event class names
     *
     * @throws ClassNotFoundException when listeners cannot be loaded in classpath
     */
    void register(EventLogListener listener, String... events) throws ClassNotFoundException;

    /**
     * Post an event to the event log. This invokes {@link EventLogListener#postEvent(EventLogEvent)} for
     * all listeners which are described to events of this type.
     *
     * @param event the event to notify the listeners about.
     */
    void postEvent(EventLogEvent event);
}
