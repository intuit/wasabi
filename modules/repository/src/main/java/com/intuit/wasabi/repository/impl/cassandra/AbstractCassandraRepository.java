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

import com.intuit.wasabi.cassandra.CassandraDriver;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.toddfast.mutagen.Plan;
import com.toddfast.mutagen.State;
import com.toddfast.mutagen.cassandra.CassandraMutagen;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract repository with Cassandra support
 *
 * @param <K> The repository keyspace definition class
 */
public class AbstractCassandraRepository<K extends RepositoryKeyspace> {

    /**
     * Logger for class
     */
    private static final Logger LOGGER = getLogger(AbstractCassandraRepository.class);
    /**
     * Mutagen helper
     */
    private CassandraMutagen mutagen;
    /**
     * Cassandra driver
     */
    private CassandraDriver driver;
    /**
     * Keyspace
     */
    private K keyspace;

    /**
     * Constructor
     *
     * @param mutagen  Cassandra Mutagen
     * @param driver   Cassandra Driver
     * @param keyspace Cassandra keyspace
     * @throws IOException         io exception
     * @throws ConnectionException connection exception
     */
    public AbstractCassandraRepository(CassandraMutagen mutagen,
                                       CassandraDriver driver, K keyspace)
            throws IOException, ConnectionException {

        super();
        this.mutagen = mutagen;
        this.keyspace = keyspace;
        this.driver = driver;

        initialize();
    }

    /**
     * Initialize the keyspace using Mutagen
     */
    private void initialize()
            throws IOException, ConnectionException {

        if (!getDriver().isKeyspaceInitialized()) {
            getDriver().initializeKeyspace();
        }

        LOGGER.info("Mutating schema");
        Plan.Result<Integer> result = getMutagen().mutate(getDriver().getKeyspace());
        State<Integer> state = result.getLastState();

        if (result.isMutationComplete()) {
            LOGGER.info("Schema mutation complete. Final state: {}", state != null
                    ? state.getID()
                    : "null");
        }

        if (result.getException() != null) {
            String message = "Exception mutating schema " +
                    "(last state: " + (state != null ? state.getID() : "null") +
                    ", completed mutations: \"" + result.getCompletedMutations() +
                    "\", remaining mutations: \"" + result.getRemainingMutations() +
                    "\")";

            LOGGER.error(message, result.getException());
            throw new IOException(message, result.getException());
        }
    }

    /**
     * @return The mutagen instance. Never null.
     */
    protected CassandraMutagen getMutagen() {
        return mutagen;
    }

    /**
     * @return The driver instance. Never null.
     */
    protected CassandraDriver getDriver() {
        return driver;
    }

    /**
     * @return The keyspace instance. Never null.
     */
    protected K getKeyspace() {
        return keyspace;
    }
}
