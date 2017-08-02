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
package com.intuit.wasabi.events;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.intuit.wasabi.assignment.AssignmentsModule;
import com.intuit.wasabi.eventobjects.EventEnvelopePayload;
import com.intuit.wasabi.events.impl.EventsExportImpl;
import com.intuit.wasabi.events.impl.EventsImpl;
import com.intuit.wasabi.events.impl.NoOpEventsIngestionExecutor;
import com.intuit.wasabi.exceptions.EventException;
import com.intuit.wasabi.export.DatabaseExport;
import com.intuit.wasabi.export.Envelope;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static com.intuit.wasabi.events.EventsAnnotations.EXECUTOR_THREADPOOL_SIZE;
import static java.lang.Class.forName;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Guice module for configuring events related objects
 */
public class EventsModule extends AbstractModule {

    public static final String PROPERTY_NAME = "/event.properties";
    private static final Logger LOGGER = getLogger(EventsModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", EventsModule.class.getSimpleName());

        install(new AssignmentsModule());
        install(new CassandraRepositoryModule());

        Properties properties = create(PROPERTY_NAME, EventsModule.class);

        bind(Integer.class).annotatedWith(named(EXECUTOR_THREADPOOL_SIZE))
                .toInstance(parseInt(getProperty("executor.threadpool.size", properties, "0")));
        bind(Events.class).to(EventsImpl.class).in(SINGLETON);
        bind(EventsExport.class).to(EventsExportImpl.class).asEagerSingleton();

        String eventDbEnvelopeClassName = getProperty("export.rest.event.db.class.name", properties,
                "com.intuit.wasabi.events.impl.NoopDatabaseEventEnvelope");

        try {
            @SuppressWarnings("unchecked")
            Class<Envelope<EventEnvelopePayload, DatabaseExport>> eventDbEnvelopeClass =
                    (Class<Envelope<EventEnvelopePayload, DatabaseExport>>) forName(eventDbEnvelopeClassName);

            bind(new TypeLiteral<Envelope<EventEnvelopePayload, DatabaseExport>>() {
            }).to(eventDbEnvelopeClass);
        } catch (ClassNotFoundException e) {
            LOGGER.error("unable to find class: {}", eventDbEnvelopeClassName, e);

            throw new EventException("unable to find class: " + eventDbEnvelopeClassName, e);
        }

        LOGGER.debug("installed module: {}", EventsModule.class.getSimpleName());
    }

    @Provides
    @Inject
    public Map<String, EventIngestionExecutor> ingestionExecutors() {
        Map<String, EventIngestionExecutor> eventIngestionExecutorMap =  new HashMap<>();
        eventIngestionExecutorMap.put(NoOpEventsIngestionExecutor.NAME, new NoOpEventsIngestionExecutor());
        return eventIngestionExecutorMap;
    }
}
