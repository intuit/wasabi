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
package com.intuit.wasabi.repository;

import com.google.inject.AbstractModule;
import com.googlecode.flyway.core.Flyway;
import com.intuit.wasabi.repository.impl.cassandra.CassandraAssignmentsRepository;
import com.intuit.wasabi.repository.impl.cassandra.CassandraAuditLogRepository;
import com.intuit.wasabi.repository.impl.cassandra.CassandraAuthorizationRepository;
import com.intuit.wasabi.repository.impl.cassandra.CassandraFeedbackRepository;
import com.intuit.wasabi.repository.impl.cassandra.CassandraMutexRepository;
import com.intuit.wasabi.repository.impl.cassandra.CassandraPagesRepository;
import com.intuit.wasabi.repository.impl.cassandra.CassandraPrioritiesRepository;
import com.intuit.wasabi.repository.impl.database.DatabaseExperimentRepository;
import com.intuit.wasabi.repository.impl.database.DatabaseFavoritesRepository;
import com.intuit.wasabi.repository.impl.database.DatabaseAnalytics;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static java.lang.Boolean.TRUE;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installs all repository modules
 */
public class RepositoryModule extends AbstractModule {

    private static final String PROPERTY_NAME = "/repository.properties";
    private static final Logger LOGGER = getLogger(RepositoryModule.class);

    /*
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", RepositoryModule.class.getSimpleName());

        Properties properties = create(PROPERTY_NAME, RepositoryModule.class);


        bind(String.class).annotatedWith(named("assign.user.to.export"))
                .toInstance(getProperty("assign.user.to.export", properties));
        bind(String.class).annotatedWith(named("assign.bucket.count"))
                .toInstance(getProperty("assign.bucket.count", properties));
        bind(Integer.class).annotatedWith(named("export.pool.size"))
                .toInstance(parseInt(getProperty("export.pool.size", properties, "5")));
        bind(Boolean.class).annotatedWith(named("assign.user.to.old"))
                .toInstance(Boolean.valueOf(getProperty("assign.user.to.old", properties, TRUE.toString())));
        bind(Boolean.class).annotatedWith(named("assign.user.to.new"))
                .toInstance(Boolean.valueOf(getProperty("assign.user.to.new", properties, TRUE.toString())));
        bind(String.class).annotatedWith(named("default.time.format"))
                .toInstance(getProperty("default.time.format", properties, "yyyy-MM-dd HH:mm:ss"));
        bind(String.class).annotatedWith(named("cassandra.mutagen.root.resource.path"))
                .toInstance(getProperty("cassandra.mutagen.root.resource.path", properties));
        bind(String.class).annotatedWith(named("mysql.mutagen.root.resource.path"))
                .toInstance(getProperty("mysql.mutagen.root.resource.path", properties));

        // MySQL bindings
        bind(Flyway.class).in(SINGLETON);
        bind(AnalyticsRepository.class).to(DatabaseAnalytics.class).in(SINGLETON);
        bind(ExperimentRepository.class).annotatedWith(DatabaseRepository.class).to(DatabaseExperimentRepository.class).in(SINGLETON);
        bind(FavoritesRepository.class).to(DatabaseFavoritesRepository.class).in(SINGLETON);

        // Cassandra bindings
        bind(AssignmentsRepository.class).to(CassandraAssignmentsRepository.class).in(SINGLETON);
        bind(MutexRepository.class).to(CassandraMutexRepository.class).in(SINGLETON);
        bind(PrioritiesRepository.class).to(CassandraPrioritiesRepository.class).in(SINGLETON);
        bind(PagesRepository.class).to(CassandraPagesRepository.class).in(SINGLETON);
        bind(AuthorizationRepository.class).to(CassandraAuthorizationRepository.class).in(SINGLETON);
        bind(FeedbackRepository.class).to(CassandraFeedbackRepository.class).in(SINGLETON);
        bind(AuditLogRepository.class).to(CassandraAuditLogRepository.class).in(SINGLETON);

        LOGGER.debug("installed module: {}", RepositoryModule.class.getSimpleName());
    }
}
