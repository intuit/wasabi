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
package com.intuit.wasabi.api;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.intuit.wasabi.analytics.AnalyticsModule;
import com.intuit.wasabi.api.error.ExceptionJsonifier;
import com.intuit.wasabi.api.jackson.JacksonModule;
import com.intuit.wasabi.auditlog.AuditLogModule;
import com.intuit.wasabi.authorization.AuthorizationModule;
import com.intuit.wasabi.database.DatabaseModule;
import com.intuit.wasabi.email.EmailModule;
import com.intuit.wasabi.events.EventsModule;
import com.intuit.wasabi.experiment.ExperimentsModule;
import com.intuit.wasabi.feedback.FeedbackModule;
import com.intuit.wasabi.repository.impl.cassandra.CassandraExperimentRepositoryModule;
import com.intuit.wasabi.repository.impl.database.DatabaseExperimentRepositoryModule;
import com.intuit.wasabi.userdirectory.UserDirectoryModule;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

public class ApiModule extends AbstractModule {

    private static final String PROPERTY_NAME = "/api.properties";
    private static final Logger LOGGER = getLogger(ApiModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", ApiModule.class.getSimpleName());

        install(new com.intuit.autumn.api.ApiModule());
        install(new AnalyticsModule());
        install(new AuditLogModule());
        install(new AuthorizationModule());
        install(new CassandraExperimentRepositoryModule());
        install(new DatabaseModule());
        install(new DatabaseExperimentRepositoryModule());
        install(new EmailModule());
        install(new EventsModule());
        install(new ExperimentsModule());
        install(new FeedbackModule());
        install(new JacksonModule());
        install(new UserDirectoryModule());

        Properties properties = create(PROPERTY_NAME, ApiModule.class);

        bind(String.class).annotatedWith(named("application.id"))
                .toInstance(getProperty("application.id", properties));
        bind(String.class).annotatedWith(named("default.time.zone"))
                .toInstance(getProperty("default.time.zone", properties, "yyyy-MM-dd HH:mm:ss"));
        bind(String.class).annotatedWith(named("default.time.format"))
                .toInstance(getProperty("default.time.format", properties, "yyyy-MM-dd HH:mm:ss"));

        bind(AuthorizedExperimentGetter.class).in(SINGLETON);
        bind(HealthCheckRegistry.class).in(SINGLETON);
        bind(HttpHeader.class).in(SINGLETON);
        bind(ExceptionJsonifier.class).in(SINGLETON);

        // FIXME: might need to run against jersey 1.18.1 to get this to work
//        try {
//            bind(InstrumentedResourceMethodDispatchAdapter.class)
//                    .toConstructor(InstrumentedResourceMethodDispatchAdapter.class.getConstructor(MetricRegistry.class))
//                    .in(SINGLETON);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }

        LOGGER.debug("installed module: {}", ApiModule.class.getSimpleName());
    }
}
