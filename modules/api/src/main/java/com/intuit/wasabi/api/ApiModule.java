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
import com.google.inject.TypeLiteral;
import com.intuit.wasabi.analytics.AnalyticsModule;
import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.api.error.ExceptionJsonifier;
import com.intuit.wasabi.api.jackson.JacksonModule;
import com.intuit.wasabi.api.pagination.comparators.PaginationComparator;
import com.intuit.wasabi.api.pagination.comparators.impl.AuditLogEntryComparator;
import com.intuit.wasabi.api.pagination.comparators.impl.ExperimentComparator;
import com.intuit.wasabi.api.pagination.comparators.impl.ExperimentDetailComparator;
import com.intuit.wasabi.api.pagination.filters.PaginationFilter;
import com.intuit.wasabi.api.pagination.filters.impl.AuditLogEntryFilter;
import com.intuit.wasabi.api.pagination.filters.impl.ExperimentDetailFilter;
import com.intuit.wasabi.api.pagination.filters.impl.ExperimentFilter;
import com.intuit.wasabi.auditlog.AuditLogModule;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authorization.AuthorizationModule;
import com.intuit.wasabi.database.DatabaseModule;
import com.intuit.wasabi.email.EmailModule;
import com.intuit.wasabi.events.EventsModule;
import com.intuit.wasabi.experiment.ExperimentsModule;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.feedback.FeedbackModule;
import com.intuit.wasabi.repository.database.DatabaseExperimentRepositoryModule;
import com.intuit.wasabi.userdirectory.UserDirectoryModule;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static com.intuit.wasabi.api.ApiAnnotations.*;
import static org.slf4j.LoggerFactory.getLogger;

public class ApiModule extends AbstractModule {

    private static final String PROPERTY_NAME = "/api.properties";
    private static final Logger LOGGER = getLogger(ApiModule.class);

    @Override
    protected void configure() {

        installModules();

        Properties properties = create(PROPERTY_NAME, ApiModule.class);

        bind(String.class).annotatedWith(named(APPLICATION_ID))
                .toInstance(getProperty("application.id", properties));
        bind(String.class).annotatedWith(named(DEFAULT_TIME_ZONE))
                .toInstance(getProperty("default.time.zone", properties, "UTC"));
        bind(String.class).annotatedWith(named(DEFAULT_TIME_FORMAT))
                .toInstance(getProperty("default.time.format", properties, "yyyy-MM-dd HH:mm:ss"));
        bind(String.class).annotatedWith(named(ACCESS_CONTROL_MAX_AGE_DELTA_SECONDS))
                .toInstance(getProperty("access.control.max.age.delta.seconds", properties));
        bind(Boolean.class).annotatedWith(named(RATE_LIMIT_ENABLED))
                .toInstance(Boolean.parseBoolean(getProperty("rate.limit.enabled", properties, "false")));
        bind(Integer.class).annotatedWith(named(RATE_HOURLY_LIMIT))
                .toInstance(Integer.parseInt(getProperty("rate.hourly.limit", properties, "1")));

        bind(AuthorizedExperimentGetter.class).in(SINGLETON);
        bind(HealthCheckRegistry.class).in(SINGLETON);
        bind(HttpHeader.class).in(SINGLETON);
        bind(ExceptionJsonifier.class).in(SINGLETON);

        // Bind comparators and filters for pagination
        bind(new TypeLiteral<PaginationComparator<AuditLogEntry>>() {
        }).to(new TypeLiteral<AuditLogEntryComparator>() {
        });
        bind(new TypeLiteral<PaginationFilter<AuditLogEntry>>() {
        }).to(new TypeLiteral<AuditLogEntryFilter>() {
        });
        bind(new TypeLiteral<PaginationComparator<Experiment>>() {
        }).to(new TypeLiteral<ExperimentComparator>() {
        });
        bind(new TypeLiteral<PaginationFilter<Experiment>>() {
        }).to(new TypeLiteral<ExperimentFilter>() {
        });
        bind(new TypeLiteral<PaginationFilter<ExperimentDetail>>() {
        }).to(new TypeLiteral<ExperimentDetailFilter>() {
        });
        bind(new TypeLiteral<PaginationComparator<ExperimentDetail>>() {
        }).to(new TypeLiteral<ExperimentDetailComparator>() {
        });

    }

    protected void installUserModule() {
        install(new UserDirectoryModule());
    }

    protected void installAuthModule() {
        install(new AuthorizationModule());
    }

    protected void installEventModule() {
        install(new EventsModule());
    }

    protected void installEmailModule() { install(new EmailModule());}

    private void installModules() {
        LOGGER.debug("installing module: {}", ApiModule.class.getCanonicalName());

        //these modules are either free of other dependencies or they are required by later modules
        install(new com.intuit.autumn.api.ApiModule());
        installUserModule();
        install(new DatabaseExperimentRepositoryModule());
        install(new DatabaseModule());
        install(new JacksonModule());
        install(new AuditLogModule());
        installAuthModule();

        //install(new EmailModule());
        installEmailModule();
        installEventModule();
        install(new ExperimentsModule());
        install(new FeedbackModule());
        install(new AnalyticsModule());

        LOGGER.debug("installed module: {}", ApiModule.class.getCanonicalName());
    }
}
