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
import org.apache.commons.lang.StringUtils;

/**
 * An abstract event without a description.
 */
public abstract class AbstractChangeEvent extends AbstractEvent implements ChangeEvent {

    private final String propertyName;
    private final String before;
    private final String after;

    /**
     * Instantiates an AbstractChangeEvent invoked by the specified user.
     *
     * @param user the user (if {@code null}, the {@link EventLog#SYSTEM_USER} is used.
     * @param propertyName the property which changed
     * @param before the state before
     * @param after the state after
     */
    public AbstractChangeEvent(UserInfo user, String propertyName, String before, String after) {
        super(user);
        if (StringUtils.isBlank(propertyName)) {
            throw new IllegalArgumentException("PropertyName must not be blank or null.");
        }
        this.propertyName = propertyName;
        this.before = before;
        this.after = after;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBefore() {
        return before;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAfter() {
        return after;
    }
}
