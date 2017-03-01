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
import com.intuit.wasabi.experimentobjects.Application;

/**
 * This is the interface that is used for the centralized sending of notification emails, when
 * for example the state of an experiment changes.
 */
public interface EmailService {

    /**
     * Sends the specified message to the recipients.
     *
     * @param subject the subject of the email
     * @param msg     the plain messsage to be send
     * @param to      the email addresses of the recipients
     */
    void doSend(String subject, String msg, String... to);

    /**
     * Sends an Email to the Administrators of the specified application and requests
     * access for the User.
     *
     * @param appName the {@link com.intuit.wasabi.experimentobjects.Application.Name} the user requests access to
     * @param user    the user who wants access
     * @param links   the email links
     */
    void sendEmailForUserPermission(Application.Name appName, UserInfo.Username user, EmailLinksList links);

    /**
     * This returns whether the EmailService implementation is activated at
     * the moment or not. The value can be changed while the service is running
     * with {@link #setActive(boolean)}
     *
     * @return <code>true</code> when emails are sent.
     */
    boolean isActive();

    /**
     * En- or disables the EmailService. The concrete implementation of an inactive service can
     * vary among different implementations.
     *
     * @param active set to <code>true</code> when EmailService should sent emails
     */
    void setActive(boolean active);

}
