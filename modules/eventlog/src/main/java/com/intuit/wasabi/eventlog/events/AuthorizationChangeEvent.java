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
import com.intuit.wasabi.experimentobjects.Application;
import org.apache.commons.lang3.StringUtils;

/**
 * This event denotes authorization (UserRole) changes. It assigns the affected user to the
 * "propertyName" and the new role to "after". If "after" is blank ({@link StringUtils#isBlank(CharSequence)}), the
 * event denotes the removal of a user role.
 */
public class AuthorizationChangeEvent extends AbstractChangeEvent implements ApplicationEvent {

    private final Application.Name applicationName;

    /**
     * Authorization changes invoked by the {@link EventLog#SYSTEM_USER}.
     * @param applicationName the application affected
     * @param user the user with changed permissions
     * @param oldRole the role before (if applicable)
     * @param newRole the role after (if applicable)
     */
    public AuthorizationChangeEvent(Application.Name applicationName, UserInfo user, String oldRole, String newRole) {
        this(null, applicationName, user, oldRole, newRole);
    }

    /**
     * Authorization changes invoked by the {@link EventLog#SYSTEM_USER}.
     * @param admin           the UserInfo of admin
     * @param applicationName the application affected
     * @param user the user with changed permissions
     * @param oldRole the role before (if applicable)
     * @param newRole the role after (if applicable)
     *
     */
    public AuthorizationChangeEvent(UserInfo admin, Application.Name applicationName, UserInfo user, String oldRole, String newRole) {
        super(admin, user.getFirstName() + " " + user.getLastName() + " (" + user.getUsername() + ")", oldRole, newRole);
        this.applicationName = applicationName;
    }

    /**
     * A default event description for non-specified event handlers.
     *
     * @return a simplified description
     */
    @Override
    public String getDefaultDescription() {
        if (!StringUtils.isBlank(getAfter())) {
            return getUser() + " changed the role of " + getPropertyName() + " in application " + getApplicationName() + " to " + getAfter();
        }
        return getUser() + " removed the role of " + getPropertyName() + " from application " + getApplicationName();
    }

    /**
     * Returns the Application.Name
     */
    @Override
    public Application.Name getApplicationName() {
        return applicationName;
    }
}
