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
import com.google.inject.AbstractModule;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.DefaultCassandraDriver;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import java.io.IOException;

/**
 * Abstract Guice module with Cassandra support
 */
public abstract class AbstractCassandraModule extends AbstractModule {

    /**
     * Construct the Cassandra driver from the specified context
     *
     * @param propertyContext property context
     * @param registry        metrics registry
     * @param instanceName    name of the isntance
     * @return instance of cassandra driver
     * @throws IOException         io exception
     * @throws ConnectionException when connection failed
     */
    protected CassandraDriver newDriver(final String propertyContext, final HealthCheckRegistry registry,
                                        final String instanceName)
            throws IOException, ConnectionException {
        DriverConfiguration config = new DriverConfiguration(propertyContext);
        // note: not using AWS yet, so set to null for now
        CassandraDriver result = new DefaultCassandraDriver(config, registry, instanceName);

        if (!result.isKeyspaceInitialized()) {
            result.initializeKeyspace();
        }

        return result;
    }
}
