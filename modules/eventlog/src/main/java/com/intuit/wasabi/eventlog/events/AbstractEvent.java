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
 * An abstract event without a description.
 */
public abstract class AbstractEvent implements EventLogEvent {

    private final Calendar time;
    private final UserInfo user;

    /**
     * Instantiates an AbstractEvent invoked by the specified user.
     * If the user is {@code null}, the {@link EventLog#SYSTEM_USER} is used.
     *
     * @param user the user
     */
    public AbstractEvent(UserInfo user) {
        this.time = Calendar.getInstance();
        this.user = user == null ? EventLog.SYSTEM_USER : user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Calendar getTime() {
        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfo getUser() {
        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventLogEventType getType() {
        return EventLogEventType.getType(this);
    }

}
