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

import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.impl.EventLogImpl;

/**
 * Specifies EventLogListeners which can subscribe to certain events at the {@link EventLogImpl}. Whenever one of
 * these events is posted, the {@link EventLogImpl} will call {@link #postEvent(EventLogEvent)} for all listeners
 * which are subscribed to that type of event.
 */
public interface EventLogListener {

    /**
     * Will be called by the EventLogImpl with events which the listener registered for.
     *
     * @param event the event which occurred.
     */
    void postEvent(EventLogEvent event);
}