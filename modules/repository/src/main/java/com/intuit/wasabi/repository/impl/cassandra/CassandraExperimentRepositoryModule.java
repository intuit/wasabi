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
package com.intuit.wasabi.repository.impl.cassandra;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.ExperimentDriver;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.toddfast.mutagen.cassandra.CassandraMutagen;
import com.toddfast.mutagen.cassandra.impl.CassandraMutagenImpl;
import org.slf4j.Logger;

import java.io.IOException;

import static com.google.inject.Scopes.SINGLETON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Guice module for cassandra experiment repo
 */
public class CassandraExperimentRepositoryModule extends AbstractCassandraModule {

    public static final String PROPERTY_NAME = "/cassandra_experiments.properties";
    private static final Logger LOGGER = getLogger(CassandraExperimentRepositoryModule.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", CassandraExperimentRepositoryModule.class.getSimpleName());

        bind(ExperimentsKeyspace.class).to(ExperimentsKeyspaceImpl.class).in(SINGLETON);

        LOGGER.debug("installed module: {}", CassandraExperimentRepositoryModule.class.getSimpleName());
    }

    @Provides
    @Singleton
    @ExperimentDriver
    protected CassandraDriver provideExperimentDriver(HealthCheckRegistry registry)
            throws IOException, ConnectionException {
        LOGGER.debug("Providing Experiments CassandraDriver instance");

        CassandraDriver result = newDriver(PROPERTY_NAME, registry, "Experiments Cassandra");

        LOGGER.debug("Provided Experiments CassandraDriver instance");

        return result;
    }

    @Inject
    @Provides
    @Singleton
    @CassandraRepository
    protected ExperimentRepository provideExperimentRepository(@ExperimentDriver CassandraDriver driver,
                                                               ExperimentsKeyspace keyspace,
                                                               ExperimentValidator validator)
            throws IOException, ConnectionException {
        LOGGER.debug("Providing Cassandra experiment repository instance");

        CassandraMutagen mutagen = new CassandraMutagenImpl();
        String rootResourcePath = "com/intuit/wasabi/repository/impl/cassandra/experiments/mutation";

        mutagen.initialize(rootResourcePath);

        ExperimentRepository result = new CassandraExperimentRepository(mutagen, driver, keyspace, validator);

        LOGGER.debug("Provided Cassandra experiment repository instance");

        return result;
    }
}
