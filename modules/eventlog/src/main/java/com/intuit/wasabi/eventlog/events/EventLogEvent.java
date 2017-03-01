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
package com.intuit.wasabi.eventlog.events;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.EventLogEventType;

import java.util.Calendar;

/**
 * An EventLogEvent stores all data needed to identify an event:
 * <ul>
 *     <li>the user</li>
 *     <li>the date</li>
 *     <li>possible additional fields</li>
 * </ul>
 *
 * An EventLogEvent is the base interface for all other Event interfaces.
 */
public interface EventLogEvent {

    /**
     * The time at which this event happened.
     *
     * @return the event time
     */
    Calendar getTime();

    /**
     * The user initiating the event. If the event was automatically initiated by the system,
     * the {@link EventLog#SYSTEM_USER} is returned.
     *
     * @return the user initiating this event
     */
    UserInfo getUser();

    /**
     * A default event description for non-specified event handlers.
     *
     * @return a simplified description
     */
    String getDefaultDescription();

    /**
     * Returns the type of the specific event.
     *
     * @return the {@link EventLogEventType} to this event
     */
    EventLogEventType getType();

}
