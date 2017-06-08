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
import com.intuit.wasabi.email.EmailService;
import com.intuit.wasabi.email.EmailTextProcessor;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.EventLogListener;
import com.intuit.wasabi.eventlog.events.BucketEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.ExperimentEvent;
import com.intuit.wasabi.eventlog.impl.EventLogImpl;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This listener is looking out for events and processes new events to
 * a format that can be sent via mail using the {@link com.intuit.wasabi.email.EmailService}
 */
public class EmailEventLogListener implements EventLogListener {

    private static final Logger LOGGER = getLogger(EventLogImpl.class);

    protected final EventLog eventLog;
    private final EmailService emailService;
    private final EmailTextProcessor emailTextProcessor;

    /**
     * The constructor uses the EventLog to register for updates on certain
     * events (at the moment this only listens to changes in Buckets and Experiments)
     *
     * @param eventLog           the event log we want to sign up for
     * @param emailService       the email service
     * @param emailTextProcessor the email text processor
     */
    @Inject
    public EmailEventLogListener(final EventLog eventLog, final EmailService emailService,
                                 final EmailTextProcessor emailTextProcessor) {
        this.eventLog = eventLog;
        //so at the moment we are only interested in Bucket and Experiment Updates
        List<Class<? extends EventLogEvent>> abo = new ArrayList<>();
        abo.add(ExperimentEvent.class);
        abo.add(BucketEvent.class);

        this.eventLog.register(this, abo);

        this.emailTextProcessor = emailTextProcessor;
        this.emailService = emailService;
    }


    /**
     * This will be the default implementation that will listen to all the changes
     * and send emails accordingly.
     *
     * @param event the event which occurred.
     */
    @Override
    public void postEvent(EventLogEvent event) {

        String subject = emailTextProcessor.getSubject(event);
        String msg = emailTextProcessor.getMessage(event);
        Set<String> addressees = emailTextProcessor.getAddressees(event);

        LOGGER.info("Sending email with subject:{}, message:{}, addressees:{}", subject, msg, addressees);

        emailService.doSend(subject, msg, addressees.toArray(new String[addressees.size()]));

    }
}
