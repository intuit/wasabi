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

/**
 * A simple event.
 */
public class SimpleEvent extends AbstractEvent {

    private final String description;

    /**
     * Instantiates a SimpleEvent invoked by the {@link EventLog#SYSTEM_USER}.
     * @param description the event description
     */
    public SimpleEvent(String description) {
        this(null, description);
    }

    /**
     * Instantiates a SimpleEvent invoked by the specified user.
     * @param user the user
     * @param description the description
     */
    public SimpleEvent(UserInfo user, String description) {
        super(user);
        this.description = description;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultDescription() {
        return getUser().getUsername() + " invoked this event: " + description;
    }
}
