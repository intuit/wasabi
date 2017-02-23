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
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorizationobjects.Role;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.email.EmailLinksList;
import com.intuit.wasabi.email.EmailTextProcessor;
import com.intuit.wasabi.email.TextTemplates;
import com.intuit.wasabi.eventlog.EventLogEventType;
import com.intuit.wasabi.eventlog.events.BucketChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketCreateEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.eventlog.events.ExperimentCreateEvent;
import com.intuit.wasabi.eventlog.events.ExperimentEvent;
import com.intuit.wasabi.exceptions.EventLogException;
import com.intuit.wasabi.exceptions.WasabiEmailException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.ExperimentBase;
import com.intuit.wasabi.repository.AuthorizationRepository;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.stringtemplate.v4.ST;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.intuit.wasabi.email.TextTemplates.APP_ACCESS;
import static com.intuit.wasabi.email.TextTemplates.BUCKET_CHANGE;
import static com.intuit.wasabi.email.TextTemplates.BUCKET_CREATED;
import static com.intuit.wasabi.email.TextTemplates.EXPERIMENT_CHANGED;
import static com.intuit.wasabi.email.TextTemplates.EXPERIMENT_CREATED;
import static com.intuit.wasabi.email.WasabiEmailFields.APPLICATION_NAME;
import static com.intuit.wasabi.email.WasabiEmailFields.BUCKET_NAME;
import static com.intuit.wasabi.email.WasabiEmailFields.EMAIL_LINKS;
import static com.intuit.wasabi.email.WasabiEmailFields.EXPERIMENT_ID;
import static com.intuit.wasabi.email.WasabiEmailFields.EXPERIMENT_LABEL;
import static com.intuit.wasabi.email.WasabiEmailFields.FIELD_AFTER;
import static com.intuit.wasabi.email.WasabiEmailFields.FIELD_BEFORE;
import static com.intuit.wasabi.email.WasabiEmailFields.FIELD_NAME;
import static com.intuit.wasabi.email.WasabiEmailFields.USER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the {@link EmailTextProcessor}
 */
public class EmailTextProcessorImpl implements EmailTextProcessor {

    /* need this for getting the admins to an experiment */
    private AuthorizationRepository authorizationRepository;
    private final static Logger LOGGER = getLogger(EmailTextProcessorImpl.class);

    @Inject
    public EmailTextProcessorImpl(final AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAddressees(EventLogEvent event) {
        Set<String> addressants = new HashSet<>();
        EventLogEventType type = event.getType();

        switch (type) {
            case BUCKET_CHANGED:
            case BUCKET_CREATED:
            case EXPERIMENT_CHANGED:
            case EXPERIMENT_CREATED:
                addressants.addAll(getExperimentAddressor((ExperimentEvent) event));
                break;
            case SIMPLE_EVENT:
            default:
                throw new IllegalArgumentException("Can not handle EventType: " + type.toString());
        }

        return addressants;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAddressees(Application.Name app) {
        return getAdminEmails(app);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage(Application.Name app, UserInfo.Username username, EmailLinksList links) {
        ST appAccess = new ST(APP_ACCESS);
        Map<String, String> variables = new HashMap<>();

        put(variables, APPLICATION_NAME, String.valueOf(app));
        put(variables, USER_NAME, String.valueOf(username));
        put(variables, EMAIL_LINKS, links.toString());

        return replaceVariablesInTemplate(variables, appAccess);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSubject(Application.Name app) {
        return "A User requests access to your Application";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSubject(EventLogEvent event) {
        EventLogEventType type = event.getType();
        String subject;

        switch (type) {

            case BUCKET_CHANGED:
            case BUCKET_CREATED:
                subject = "Experiment Bucket Update";
                break;
            case EXPERIMENT_CHANGED:
                subject = "Experiment Changed";
                break;
            case EXPERIMENT_CREATED:
                subject = "Experiment Created";
                break;
            case SIMPLE_EVENT:
            default:
                throw new IllegalArgumentException("Can not handle EventType: " + type.toString());
        }

        return subject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage(EventLogEvent event) {
        EventLogEventType type = event.getType();
        String message = "";
        switch (type) {
            case BUCKET_CHANGED:
                message = getBucketChangedMessage((BucketChangeEvent) event);
                break;
            case BUCKET_CREATED:
                message = getBucketCreatedMessage((BucketCreateEvent) event);
                break;
            case EXPERIMENT_CHANGED:
                message = getExperimentChangedMessage((ExperimentChangeEvent) event);
                break;
            case EXPERIMENT_CREATED:
                message = getExperimentCreatedMessage((ExperimentCreateEvent) event);
                break;
            case SIMPLE_EVENT:
            default:
                throw new IllegalArgumentException("Can not handle EventType: " + type.toString());
        }
        return message;
    }

    /**
     * The addressors for a given experiment are the administrators of the application the
     * experiment belongs to.
     *
     * @param event the {@link ExperimentEvent} to be processed
     * @return a list of the email-addresses of the addressors
     */
    private Collection<? extends String> getExperimentAddressor(ExperimentEvent event) {
        Set<String> adressors;

        ExperimentBase exp = event.getExperiment();
        if (exp != null) {
            adressors = getAdminEmails(exp.getApplicationName());
        } else {
            throw new EventLogException("An ExperimentEvent was recorded without having an Experiment!");
        }

        return adressors;
    }

    /**
     * Returns the email adresses of the experiment admins.
     *
     * @param appName the application we want the admins from
     * @return a set of their valid email addresses
     */
    private Set<String> getAdminEmails(Application.Name appName) {
        Set<String> adressors = new HashSet<>();
        UserRoleList usersRoles = authorizationRepository.getApplicationUsers(appName);

        for (UserRole user : usersRoles.getRoleList()) {
            if (user.getRole() == Role.ADMIN) {
                String email = user.getUserEmail();
                if (EmailValidator.getInstance().isValid(email)) {
                    adressors.add(email);
                } else {
                    LOGGER.warn("\"" + email + "\" is not a valid email address for one of the administrators of " + appName);
                }
            }
        }

        //no admins, no email!
        if (adressors.isEmpty()) {
            throw new WasabiEmailException("No Admins with an valid email registered for this Application");
        }

        return adressors;
    }

    /**
     * This method finds the right attributes to set. The set fields can be found
     * in the {@link TextTemplates}.
     *
     * @param expEvent an {@link ExperimentChangeEvent} that represents the changes
     * @return the message for the email
     */
    private String getExperimentChangedMessage(ExperimentChangeEvent expEvent) {
        ST template = new ST(EXPERIMENT_CHANGED);
        Map<String, String> variables = new HashMap<>();

        ExperimentBase exp = expEvent.getExperiment();

        put(variables, EXPERIMENT_LABEL, exp.getLabel());
        put(variables, EXPERIMENT_ID, String.valueOf(exp.getID()));
        put(variables, APPLICATION_NAME, String.valueOf(exp.getApplicationName()));
        put(variables, USER_NAME, getUserRepresentation(expEvent.getUser()));
        put(variables, FIELD_NAME, expEvent.getPropertyName());
        if (expEvent.getPropertyName() != null && "sampling_percent".equalsIgnoreCase(expEvent.getPropertyName())) {
            put(variables, FIELD_BEFORE, String.valueOf(((double) Math.round(Double.parseDouble(expEvent.getBefore()) * 10000d) / 10000d) * 100).concat("%"));
            put(variables, FIELD_AFTER, String.valueOf(((double) Math.round(Double.parseDouble(expEvent.getAfter()) * 10000d) / 10000d) * 100).concat("%"));
        } else {
            put(variables, FIELD_BEFORE, expEvent.getBefore());
            put(variables, FIELD_AFTER, expEvent.getAfter());
        }
        return replaceVariablesInTemplate(variables, template);
    }

    /**
     * This method finds the right attributes to set. The set fields can be found
     * in the {@link TextTemplates}.
     *
     * @param expEvent an {@link ExperimentCreateEvent} that represents the created experiment
     * @return the message for the email
     */
    private String getExperimentCreatedMessage(ExperimentCreateEvent expEvent) {
        ST template = new ST(EXPERIMENT_CREATED);
        Map<String, String> variables = new HashMap<>();

        ExperimentBase exp = expEvent.getExperiment();

        put(variables, EXPERIMENT_LABEL, exp.getLabel());
        put(variables, EXPERIMENT_ID, String.valueOf(exp.getID()));
        put(variables, APPLICATION_NAME, String.valueOf(exp.getApplicationName()));
        put(variables, USER_NAME, getUserRepresentation(expEvent.getUser()));

        return replaceVariablesInTemplate(variables, template);
    }

    /**
     * This method finds the right attributes to set. The set fields can be found
     * in the {@link TextTemplates}.
     *
     * @param buckEvent an {@link BucketCreateEvent} that represents the changes
     * @return the message for the email
     */
    private String getBucketCreatedMessage(BucketCreateEvent buckEvent) {
        ST template = new ST(BUCKET_CREATED);
        Map<String, String> variables = new HashMap<>();

        Bucket buck = buckEvent.getBucket();
        ExperimentBase exp = buckEvent.getExperiment();

        put(variables, EXPERIMENT_LABEL, exp.getLabel());
        put(variables, BUCKET_NAME, buck.getLabel().toString());
        put(variables, APPLICATION_NAME, String.valueOf(exp.getApplicationName()));
        put(variables, USER_NAME, getUserRepresentation(buckEvent.getUser()));

        return replaceVariablesInTemplate(variables, template);
    }

    /**
     * This method finds the right attributes to set. The set fields can be found
     * in the {@link TextTemplates}.
     *
     * @param buckEvent an {@link BucketChangeEvent} that represents the changes
     * @return the message for the email
     */
    private String getBucketChangedMessage(BucketChangeEvent buckEvent) {
        ST template = new ST(BUCKET_CHANGE);
        Map<String, String> variables = new HashMap<>();

        Bucket buck = buckEvent.getBucket();
        ExperimentBase exp = buckEvent.getExperiment();

        put(variables, EXPERIMENT_LABEL, exp.getLabel());
        put(variables, BUCKET_NAME, buck.getLabel().toString());
        put(variables, APPLICATION_NAME, String.valueOf(exp.getApplicationName()));
        put(variables, USER_NAME, getUserRepresentation(buckEvent.getUser()));
        put(variables, FIELD_NAME, buckEvent.getPropertyName());
        if (buckEvent.getPropertyName() != null && "allocation".equalsIgnoreCase(buckEvent.getPropertyName())) {
            put(variables, FIELD_BEFORE, String.valueOf(((double) Math.round(Double.parseDouble(buckEvent.getBefore()) * 10000d) / 10000d) * 100).concat("%"));
            put(variables, FIELD_AFTER, String.valueOf(((double) Math.round(Double.parseDouble(buckEvent.getAfter()) * 10000d) / 10000d) * 100).concat("%"));
        } else {
            put(variables, FIELD_BEFORE, buckEvent.getBefore());
            put(variables, FIELD_AFTER, buckEvent.getAfter());
        }

        return replaceVariablesInTemplate(variables, template);
    }

    /**
     * Just a little helper method that safes me from writing to many String.valueOf() ..
     *
     * @param variables parameter set for the email template
     * @param key       the enum value
     * @param value     the value to that field
     */
    private void put(Map<String, String> variables, Object key, Object value) {
        variables.put(String.valueOf(key), String.valueOf(value));
    }

    /**
     * Gives the user representation as a {@link String} that is shown in the email.
     *
     * @param user the user we want the representation for
     * @return the users full name and email
     */
    private String getUserRepresentation(UserInfo user) {
        String userRep = "nobody";
        if (user == null) {
            LOGGER.debug("User was null for a given email - setting her to " + userRep);
        } else {
            userRep = user.getFirstName() + " " + user.getLastName() + " <" + user.getEmail() + ">";
        }

        return userRep;
    }

    /**
     * Replaces the values in a template to a complete message.
     *
     * @param variables values to be replaced
     * @param template  template for message generation
     * @return the complete message
     */
    private String replaceVariablesInTemplate(Map<String, String> variables, ST template) {
        if (variables != null) {
            for (String key : variables.keySet()) {
                String representation = variables.get(key) == null ? "null" : variables.get(key);
                template.add(key, representation);
            }
        }
        return template.render();
    }


}
