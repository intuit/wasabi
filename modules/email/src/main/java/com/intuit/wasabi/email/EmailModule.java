/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.email;

import com.google.inject.AbstractModule;
import com.intuit.wasabi.email.impl.EmailEventLogListener;
import com.intuit.wasabi.email.impl.EmailTextProcessorImpl;
import com.intuit.wasabi.email.impl.NoopEmailImpl;
import com.intuit.wasabi.eventlog.EventLogModule;
import com.intuit.wasabi.authenticationobjects.exceptions.AuthenticationException;
import com.intuit.wasabi.repository.RepositoryModule;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static java.lang.Boolean.FALSE;
import static java.lang.Class.forName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This is the module which defines the implementation to be used by different
 * classes that would like to sent emails in the future.
 */
public class EmailModule extends AbstractModule {

    public static final String PROPERTY_NAME = "/email.properties";
    private static final Logger LOGGER = getLogger(EmailModule.class);

    /**
     * Changes here can result in the use of different email services, or the use of
     * the {@link NoopEmailImpl} that only logs emails, but does not
     * sent them.
     */
    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", EmailModule.class.getSimpleName());

        install(new EventLogModule());
        install(new RepositoryModule());

        Properties properties = create(PROPERTY_NAME, EmailModule.class);

        bind(Boolean.class).annotatedWith(named("email.service.enabled"))
                .toInstance(Boolean.valueOf(getProperty("email.service.enabled", properties, FALSE.toString())));
        bind(String.class).annotatedWith(named("email.service.host"))
                .toInstance(getProperty("email.service.host", properties, ""));
        bind(String.class).annotatedWith(named("email.service.from"))
                .toInstance(getProperty("email.service.from", properties, ""));
        bind(String.class).annotatedWith(named("email.service.subject.prefix"))
                .toInstance(getProperty("email.service.subject.prefix", properties, ""));

        bind(EmailTextProcessor.class).to(EmailTextProcessorImpl.class).in(SINGLETON);
        //create listener
        bind(EmailEventLogListener.class).asEagerSingleton();

        String emailServiceClassName = getProperty("email.service.class.name", properties,
                "com.intuit.wasabi.email.impl.NoopEmailImpl");

        try {
            @SuppressWarnings("unchecked")
            Class<EmailService> emailServiceClass = (Class<EmailService>) forName(emailServiceClassName);

            bind(EmailService.class).to(emailServiceClass).in(SINGLETON);
        } catch (ClassNotFoundException e) {
            LOGGER.error("unable to find class: {}", emailServiceClassName, e);

            throw new AuthenticationException("unable to find class: " + emailServiceClassName, e);
        }

        LOGGER.debug("installed module: {}", EmailModule.class.getSimpleName());
    }
}
