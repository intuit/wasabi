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
package com.intuit.wasabi.email.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.email.EmailLinksList;
import com.intuit.wasabi.email.EmailService;
import com.intuit.wasabi.email.EmailTextProcessor;
import com.intuit.wasabi.experimentobjects.Application;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Add a noop implementation that can be used in the test setting where we do not want to sent out emails.
 */
@Singleton
public class NoopEmailImpl implements EmailService {

    private EmailTextProcessor emailTextProcessor;
    private final static Logger LOGGER = getLogger(NoopEmailImpl.class);

    /**
     * Sets the {@link EmailTextProcessor} for the service.
     *
     * @param emailTextProcessor the processor to be set
     */
    @Inject
    // FIXME: ?why inject a processor for a no-op impl?
    public void setEmailTextProcessor(EmailTextProcessor emailTextProcessor) {
        this.emailTextProcessor = emailTextProcessor;
    }

    @Override
    public void doSend(String subject, String msg, String... to) {
        LOGGER.info("SENDING EMAIL WITH SUBJECT \"" + subject + "\" AND MESSAGE: \"" + msg + "\" TO " + Arrays.toString(to));
    }

    @Override
    public void sendEmailForUserPermission(Application.Name appName, UserInfo.Username user, EmailLinksList links) {
        LOGGER.info("EMAIL IS GETTING SEND FOR USER: \"" + user + "\" FOR THE APPLICATION \" " + appName.toString() + "\"");

        try {
            String subject = emailTextProcessor.getSubject(appName);
            String msg = emailTextProcessor.getMessage(appName, user, links);
            Set<String> addressees = emailTextProcessor.getAddressees(appName);

            doSend(subject, msg, addressees.toArray(new String[addressees.size()]));
        } catch (NullPointerException npe) {
            LOGGER.info("No EmailTextProcessor set in NoopImplementation", npe);
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void setActive(boolean active) {
        //noop
    }
}
