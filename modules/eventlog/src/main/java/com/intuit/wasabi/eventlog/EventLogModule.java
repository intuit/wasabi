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
package com.intuit.wasabi.eventlog;

import com.google.inject.AbstractModule;
import com.intuit.wasabi.exceptions.EventLogException;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static java.lang.Class.forName;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides an EventLog.
 */
public class EventLogModule extends AbstractModule {

    public static final String PROPERTY_NAME = "/eventlog.properties";
    private static final Logger LOGGER = getLogger(EventLogModule.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", EventLogModule.class.getSimpleName());

        Properties properties = create(PROPERTY_NAME, EventLogModule.class);

        bind(Integer.class).annotatedWith(named("eventlog.threadpoolsize.core"))
                .toInstance(parseInt(getProperty("eventlog.threadpoolsize.core", properties, "2")));
        bind(Integer.class).annotatedWith(named("eventlog.threadpoolsize.max"))
                .toInstance(parseInt(getProperty("eventlog.threadpoolsize.max", properties, "4")));

        String eventLogClassName = getProperty("eventlog.class.name", properties,
                "com.intuit.wasabi.eventlog.impl.NoopEventLogImpl");

        try {
            @SuppressWarnings("unchecked")
            Class<EventLog> eventLogClass = (Class<EventLog>) forName(eventLogClassName);

            bind(EventLog.class).to(eventLogClass).in(SINGLETON);
        } catch (ClassNotFoundException e) {
            throw new EventLogException("unable to find class: " + eventLogClassName, e);
        }

        bind(EventLogSystem.class).in(SINGLETON);

        LOGGER.debug("installed module: {}", EventLogModule.class.getSimpleName());
    }
}
