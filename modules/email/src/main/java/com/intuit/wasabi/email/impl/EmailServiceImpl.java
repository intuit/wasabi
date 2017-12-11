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
import com.google.inject.name.Named;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.email.EmailLinksList;
import com.intuit.wasabi.email.EmailService;
import com.intuit.wasabi.email.EmailTextProcessor;
import com.intuit.wasabi.exceptions.WasabiEmailException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.intuit.wasabi.email.EmailAnnotations.*;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the {@link com.intuit.wasabi.email.EmailService}
 */
@Singleton
public class EmailServiceImpl implements EmailService {

    private final static Logger LOGGER = getLogger(EmailServiceImpl.class);
    private boolean enabled;
    private String host;
    private String from;
    private String subjectPrefix;
    private EmailValidator emailVal = EmailValidator.getInstance();
    private EmailTextProcessor emailTextProcessor;
    private String userName;
    private String password;
    private boolean authenticationEnabled;
    private boolean sslEnabled;

    @Inject
    public EmailServiceImpl(final @Named(EMAIL_SERVICE_ENABLED) boolean enabled,
                            final @Named(EMAIL_SERVICE_HOST) String host,
                            final @Named(EMAIL_SERVICE_FROM) String from,
                            final @Named(EMAIL_SERVICE_SUBJECT_PREFIX) String subjectPrefix,
                            final @Named(EMAIL_SERVICE_USERNAME) String userName,
                            final @Named(EMAIL_SERVICE_PASSWORD) String password,
                            final @Named(EMAIL_SERVICE_AUTHENTICATION_ENABLED) boolean authenticationEnabled,
                            final @Named(EMAIL_SERVICE_SSL_ENABLED) boolean sslEnabled,
                            final EmailTextProcessor emailTextProcessor) {
        this.enabled = enabled;

        setHost(host);

        setFrom(from);

        this.subjectPrefix = subjectPrefix;
        this.emailTextProcessor = emailTextProcessor;
        this.userName=userName;
        this.password=password;
        this.authenticationEnabled=authenticationEnabled;
        this.sslEnabled=sslEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActive(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public String getFrom() {
        return from;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doSend(String subject, String msg, String... to) {
        send(subject, msg, to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendEmailForUserPermission(Application.Name appName, UserInfo.Username user, EmailLinksList links) {
        LOGGER.debug("Sending an email to the administrators of " + appName + " for user " + user + "with links" + links.toString());
        String subject = emailTextProcessor.getSubject(appName);
        String msg = emailTextProcessor.getMessage(appName, user, links);
        Set<String> addressees = emailTextProcessor.getAddressees(appName);

        doSend(subject, msg, addressees.toArray(new String[addressees.size()]));
    }

    /**
     * Removed duplicated and wrong addresses from the given set.
     *
     * @param emails the emails to be checked
     * @return and cleaned up array
     */
    String[] removeInvalidEmails(String[] emails) {
        Set<String> cleanAddresses = new HashSet<>();
        for (String emailTo : emails) {
            if (emailVal.isValid(emailTo)) {
                cleanAddresses.add(emailTo);
            } else {
                LOGGER.warn("Remove email address: [" + emailTo + "] from email recipients, because it is not valid");
            }
        }
        return cleanAddresses.toArray(new String[cleanAddresses.size()]);
    }

    void send(String subject, String msg, String... to) {

        String[] clearTo = removeInvalidEmails(to);

        if (isActive()) {
            try {
                Email email = createSimpleMailService();
                email.setHostName(host);
                email.setFrom(from);
                email.setSubject(subjectPrefix + " " + subject);
                email.setMsg(msg);
                email.addTo(clearTo);
                if (this.authenticationEnabled) {
                    email.setAuthentication(this.userName, this.password);
                }
                email.setSSLOnConnect(sslEnabled);
                email.send();
            } catch (EmailException mailExcp) {
                LOGGER.error("Email could not be send because of " + mailExcp.getMessage(),mailExcp);
                throw new WasabiEmailException("Email: " + emailToString(subject, msg, to) + " could not be sent.", mailExcp);
            }
        } else {
            //if the service is not active log the email that would have been send and throw error
            LOGGER.info("EmailService would have sent: " + emailToString(subject, msg, to));
            throw new WasabiEmailException(ErrorCode.EMAIL_NOT_ACTIVE_ERROR, "The EmailService is not active.");
        }
    }

    Email createSimpleMailService() {
        return new SimpleEmail();
    }

    /**
     * Just a helper method which makes the logging of an email easier.
     *
     * @param subject the subject of the email
     * @param msg     the concrete message
     * @param to      the addressant
     * @return a string representation of that
     */
    private String emailToString(String subject, String msg, String... to) {
        return "[" + msg + "] to " + Arrays.asList(to) + " with subject [" + subject + "]";
    }

    /**
     * Set the host for email
     *
     * @param host
     * @throws IllegalArgumentException if host is blank
     */
    public void setHost(String host) {
        if (isBlank(host)) {
            throw new IllegalArgumentException("Host can not be empty or contain a space, check the configuration file");
        }

        this.host = host;
    }

    /**
     * Set from for email
     *
     * @param from - set default email if from argument is not valid
     */
    public void setFrom(String from) {
        if (!emailVal.isValid(from)) {
            LOGGER.warn("The from-value for the email service is set to the default value: wasabi-service@example.com");

            // FIXME: inject
            this.from = "wasabi-service@example.com";
        } else {
            this.from = from;
        }
    }

    /**
     * Set the prefix
     *
     * @param subjectPrefix the prefix for the email subject
     */
    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }


}
