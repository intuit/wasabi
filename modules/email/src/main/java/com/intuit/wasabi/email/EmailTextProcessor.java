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
package com.intuit.wasabi.email;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.experimentobjects.Application;

import java.util.Set;

/**
 * Interface that handles the text creation from a specific event.
 */
public interface EmailTextProcessor {

    /**
     * The subject for the email. Containing basic information of what this email
     * is about.
     *
     * @param itemEvent the event from which the email should be generated
     * @return the subject
     */
    String getSubject(EventLogEvent itemEvent);

    /**
     * The message body for the Email. This has already all fields replaced etc.
     *
     * @param itemEvent the event from which the email should be send
     * @return the message body as a string
     */
    String getMessage(EventLogEvent itemEvent);

    /**
     * Gets the Email-Addresses of the users that are concerned with changes
     * for a given object.
     *
     * @param itemEvent the event that influences the concerned people
     * @return the addressees email addresses
     */
    Set<String> getAddressees(EventLogEvent itemEvent);

    /**
     * Gets the Email-Addresses for the admins of a specific {@link Application}.
     *
     * @param app the application we want the admins for
     * @return a set with the email addresses for the addressees
     */
    Set<String> getAddressees(Application.Name app);

    /**
     * Get's the message for permission access to a certain {@link Application}.
     *
     * @param app      the app the user wants access to
     * @param username the user who is requesting access
     * @param links    the list of links to email
     * @return the message for the permission email
     */
    String getMessage(Application.Name app, UserInfo.Username username, EmailLinksList links);

    /**
     * Get's the subject for permission access to a certain {@link Application}.
     *
     * @param app the app the user wants access to
     * @return the subject for the permission email
     */
    String getSubject(Application.Name app);

}
