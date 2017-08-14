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
package com.intuit.wasabi.assignment;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.intuit.wasabi.assignment.cache.AssignmentMetadataCacheTimeService;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import com.intuit.wasabi.assignment.cache.impl.AssignmentMetadataCacheTimeServiceImpl;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheHealthCheck;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheImpl;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheRefreshTask;
import com.intuit.wasabi.assignment.cache.impl.NoopAssignmentsMetadataCacheImpl;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import com.intuit.wasabi.exceptions.AssignmentException;
import com.intuit.wasabi.experiment.ExperimentsModule;
import com.intuit.wasabi.export.DatabaseExport;
import com.intuit.wasabi.export.Envelope;
import com.intuit.wasabi.export.WebExport;
import com.intuit.wasabi.export.rest.impl.ExportModule;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.base.Optional.fromNullable;
import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_ALLOWED_STALE_TIME;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_ENABLED;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_HEALTH_CHECK;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_REFRESH_CACHE_SERVICE;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_REFRESH_INTERVAL;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_REFRESH_TASK;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENT_DECORATOR_SERVICE;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.RULECACHE_THREADPOOL;
import static java.lang.Boolean.FALSE;
import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

public class AssignmentsModule extends AbstractModule {

    private static final String PROPERTY_NAME = "/assignment.properties";
    private static final Logger LOGGER = getLogger(AssignmentsModule.class);

    /**
     * Executors to ingest data to real time ingestion system. Add bindings to mapBinder in order for the executors
     * within this map to get executed at various points in AssignmentsImpl.java.
     */
    protected MapBinder<String, AssignmentIngestionExecutor> mapBinder;

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", AssignmentsModule.class.getSimpleName());

        install(new ExportModule());
        install(new CassandraRepositoryModule());
        install(new ExperimentsModule());

        Properties properties = create(PROPERTY_NAME, AssignmentsModule.class);

        bindAssignmentAndDecorator(properties);
        bindRuleCacheThreadPool(properties);
        bindMetadataCache(properties);

        String databaseAssignmentClassName = getProperty("export.rest.assignment.db.class.name", properties,
                "com.intuit.wasabi.assignment.impl.NoopDatabaseAssignmentEnvelope");
        String webAssignmentClassName = getProperty("export.rest.assignment.web.class.name", properties,
                "com.intuit.wasabi.assignment.impl.NoopWebAssignmentEnvelope");

        try {
            @SuppressWarnings("unchecked")
            Class<Envelope<AssignmentEnvelopePayload, DatabaseExport>> databaseAssignmentClass =
                    (Class<Envelope<AssignmentEnvelopePayload, DatabaseExport>>) Class.forName(databaseAssignmentClassName);

            bind(new TypeLiteral<Envelope<AssignmentEnvelopePayload, DatabaseExport>>() {
            }).to(databaseAssignmentClass);

            @SuppressWarnings("unchecked")
            Class<Envelope<AssignmentEnvelopePayload, WebExport>> webAssignmentWebEnvelopeImplClass =
                    (Class<Envelope<AssignmentEnvelopePayload, WebExport>>) Class.forName(webAssignmentClassName);

            bind(new TypeLiteral<Envelope<AssignmentEnvelopePayload, WebExport>>() {
            }).to(webAssignmentWebEnvelopeImplClass);
        } catch (ClassNotFoundException e) {
            LOGGER.error("unable to find class: {}", e.getMessage(), e);

            throw new AssignmentException("unable to find class: " + e.getMessage(), e);
        }

        mapBinder = MapBinder.newMapBinder(
                binder(),
                String.class,
                AssignmentIngestionExecutor.class
        );

        LOGGER.debug("installed module: {}", AssignmentsModule.class.getSimpleName());
    }

    private void bindMetadataCache(final Properties properties) {
        Boolean metadataCacheEnabled =
                Boolean.parseBoolean(getProperty("metadata.cache.enabled", properties, "true"));
        bind(Boolean.class).annotatedWith(named(ASSIGNMENTS_METADATA_CACHE_ENABLED))
                .toInstance(metadataCacheEnabled);

        if (metadataCacheEnabled) {
            //This is a cache refresh interval, at this frequency cache will be refreshed.
            Integer metadataCacheRefreshIntervalInMinutes = Integer.parseInt(getProperty("metadata.cache.refresh.interval", properties, "5"));
            Integer metadataCacheNumberOfThreads = 1; //We want only single thread to refresh metadata cache.
            //This is allowed missed intervals in minutes. If cache hasn't been refreshed for more than allowed missed/buffer intervals then HealthCheck will be failed for this APP node.
            Integer allowedStaleTimeInMinutes = Integer.parseInt(getProperty("metadata.cache.allowed.stale.time", properties, "360"));

            //Create Scheduled Executor Service
            ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("AssignmentMetadataCache-%d").setDaemon(true).build();
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(metadataCacheNumberOfThreads, threadFactory);

            //Configure and bind CacheManager
            CacheManager cacheManager = CacheManager.create();
            bind(CacheManager.class).toInstance(cacheManager);

            //Bind time service
            bind(AssignmentMetadataCacheTimeService.class).to(AssignmentMetadataCacheTimeServiceImpl.class).in(SINGLETON);
            //Bind allowed stale time
            bind(Integer.class)
                    .annotatedWith(named(ASSIGNMENTS_METADATA_CACHE_ALLOWED_STALE_TIME))
                    .toInstance(allowedStaleTimeInMinutes);
            //Bind health check
            bind(HealthCheck.class)
                    .annotatedWith(named(ASSIGNMENTS_METADATA_CACHE_HEALTH_CHECK))
                    .to(AssignmentsMetadataCacheHealthCheck.class).in(SINGLETON);
            //Bind scheduled executor service
            bind(ScheduledExecutorService.class)
                    .annotatedWith(named(ASSIGNMENTS_METADATA_CACHE_REFRESH_CACHE_SERVICE))
                    .toInstance(scheduledExecutorService);
            //Bind refresh interval
            bind(Integer.class)
                    .annotatedWith(named(ASSIGNMENTS_METADATA_CACHE_REFRESH_INTERVAL))
                    .toInstance(metadataCacheRefreshIntervalInMinutes);
            //Bind actual cache here
            bind(AssignmentsMetadataCache.class).to(AssignmentsMetadataCacheImpl.class).in(SINGLETON);
            //Bind cache refresh task
            bind(Runnable.class)
                    .annotatedWith(named(ASSIGNMENTS_METADATA_CACHE_REFRESH_TASK))
                    .to(AssignmentsMetadataCacheRefreshTask.class).in(SINGLETON);

        } else {
            //Bind cache instance to NOOP Instance if cache is disabled.
            bind(AssignmentsMetadataCache.class).to(NoopAssignmentsMetadataCacheImpl.class).in(SINGLETON);
        }
    }

    private void bindAssignmentAndDecorator(final Properties properties) {
        boolean assignmentDecoratorEnabled = Boolean.parseBoolean(getProperty("assignment.decorator.enabled",
                properties, FALSE.toString()));
        String assignmentDecoratorClassName = getProperty("assignment.decorator.class.name", properties,
                "com.intuit.wasabi.assignment.impl.DefaultAssignmentDecorator");
        String assignmentsClassName = getProperty("assignments.class.name", properties,
                "com.intuit.wasabi.assignment.impl.AssignmentsImpl");

        if (assignmentDecoratorEnabled) {
            URI assignmentDecoratorUri = URI.create(getProperty("assignment.decorator.service",
                    properties));

            if (fromNullable(assignmentDecoratorUri).isPresent()) {
                bind(URI.class).annotatedWith(named(ASSIGNMENT_DECORATOR_SERVICE))
                        .toInstance(assignmentDecoratorUri);
            }
        }
        try {
            @SuppressWarnings("unchecked")
            Class assignmentDecoratorClass = Class.forName(assignmentDecoratorClassName);

            bind(AssignmentDecorator.class).to(assignmentDecoratorClass).in(SINGLETON);

            @SuppressWarnings("unchecked")
            Class assignmentsClass = Class.forName(assignmentsClassName);

            bind(Assignments.class).to(assignmentsClass).in(SINGLETON);
        } catch (ClassNotFoundException e) {
            LOGGER.error("unable to find class: {}", e.getMessage(), e);

            throw new AssignmentException("unable to find class: " + e.getMessage(), e);
        }
    }

    private void bindRuleCacheThreadPool(final Properties properties) {
        LinkedBlockingQueue<Runnable> ruleCacheQueue = new LinkedBlockingQueue<>();
        int ruleCacheThreadPoolSize = parseInt(getProperty("ruleCache.executor.pool.size", properties, "5"));
        ThreadPoolExecutor ruleCacheExecutor = new ThreadPoolExecutor(ruleCacheThreadPoolSize,
                ruleCacheThreadPoolSize, 0L, MILLISECONDS, ruleCacheQueue, new ThreadFactoryBuilder()
                .setNameFormat("RuleCache-%d")
                .setDaemon(true)
                .build());

        bind(ThreadPoolExecutor.class)
                .annotatedWith(named(RULECACHE_THREADPOOL))
                .toInstance(ruleCacheExecutor);
    }

}
