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
package com.intuit.wasabi.cassandra;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Inject;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.SSLConnectionContext;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.Slf4jConnectionPoolMonitorImpl;
import com.netflix.astyanax.ddl.SchemaChangeResult;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default implementation of {@link CassandraDriver}
 */
public class DefaultCassandraDriver implements CassandraDriver {

    private static final Logger LOGGER = getLogger(DefaultCassandraDriver.class);
    private static final String DEFAULT_CASSANDRA_VERSION = "2.0";
    private static final String DEFAULT_CQL_VERSION = "3.0.0";
    private AstyanaxContext<Keyspace> context;
    private CassandraDriver.Configuration configuration;
    private Keyspace keyspace;
    private boolean keyspaceInitialized;

    @Inject
    public DefaultCassandraDriver(CassandraDriver.Configuration config, HealthCheckRegistry healthChecks,
                                  String instanceName)
            throws IOException, ConnectionException {
        super();

        this.configuration = config;

        LOGGER.info("Initializing driver");
        initialize();
        // Register for health check
        healthChecks.register(instanceName, new DefaultCassandraHealthCheck());
    }

    private void initialize() throws IOException, ConnectionException {
        if (context == null) {
            synchronized (DefaultCassandraDriver.class) {


                String cassandraVersion = getConfiguration().getTargetVersion();
                if (cassandraVersion == null
                        || cassandraVersion.trim().isEmpty()) {
                    cassandraVersion = DEFAULT_CASSANDRA_VERSION;
                }

                String cqlVersion = getConfiguration().getCQLVersion();
                if (cqlVersion == null || cqlVersion.trim().isEmpty()) {
                    cqlVersion = DEFAULT_CQL_VERSION;
                }

                ConsistencyLevel readConsistency =
                        getConfiguration().getDefaultReadConsistency();
                if (readConsistency == null) {
                    readConsistency = ConsistencyLevel.CL_QUORUM;
                }

                ConsistencyLevel writeConsistency =
                        getConfiguration().getDefaultWriteConsistency();
                if (writeConsistency == null) {
                    writeConsistency = ConsistencyLevel.CL_QUORUM;
                }

                ConnectionPoolConfigurationImpl conf =
                        new ConnectionPoolConfigurationImpl(getConfiguration().getKeyspaceName() + "Pool")
                                .setPort(getConfiguration().getPort())
                                .setMaxConnsPerHost(
                                        getConfiguration().getMaxConnectionsPerHost())
                                .setSeeds(getConfiguration().getNodeHosts())
                                .setMaxOperationsPerConnection(10_000_000)
                                .setConnectionLimiterMaxPendingCount(20)
                                .setTimeoutWindow(120_000)
                                .setConnectionLimiterWindowSize(20_000)
                                .setMaxTimeoutCount(3)
                                .setConnectTimeout(20_000)
                                .setMaxFailoverCount(-1)
                                .setSocketTimeout(30_000)
                                .setMaxTimeoutWhenExhausted(10_000)
                      /*          .setMaxPendingConnectionsPerHost(20)
                                .setLatencyAwareBadnessThreshold(20)
                                .setLatencyAwareUpdateInterval(10_000) // 10000
                                .setLatencyAwareResetInterval(0)
                                .setLatencyAwareWindowSize(100) // 100
                                .setLatencyAwareSentinelCompare(100f)
                                .setInitConnsPerHost(10)*/;
                // SSL connection
                if (getConfiguration().useSSL()) {
                    conf.setSSLConnectionContext(new SSLConnectionContext(
                            getConfiguration().getSSLTrustStore(),
                            getConfiguration().getSSLTrustStorePassword()
                    ));
                }

                context = new AstyanaxContext.Builder()
//                  .forCluster("ClusterName") // Not sure why this is helpful
                        .forKeyspace(getConfiguration().getKeyspaceName())
                        .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                                        .setTargetCassandraVersion(cassandraVersion)
                                        .setCqlVersion(cqlVersion)
                                        .setDefaultReadConsistencyLevel(readConsistency)
                                        .setDefaultWriteConsistencyLevel(writeConsistency)
                                        .setDiscoveryType(configuration.getNodeDiscoveryType())
                                        .setConnectionPoolType(configuration.getConnectionPoolType())
                        )
                        .withConnectionPoolConfiguration(conf)
                                // TODO: Does this need to be externalized?
//                    .withConnectionPoolMonitor(
//                        new CountingConnectionPoolMonitor())
                                // This is useful for development
                        .withConnectionPoolMonitor(new Slf4jConnectionPoolMonitorImpl())
                        .buildKeyspace(ThriftFamilyFactory.getInstance());

                context.start();
                keyspace = context.getClient();

                // Try to get the definition to test if the keyspace exists
                try {
                    if (keyspace.describeKeyspace() != null) {
                        keyspaceInitialized = true;
                    }
                } catch (BadRequestException e) {
                    LOGGER.warn("Keyspace " + getConfiguration().getKeyspaceName() + " doesn't exist", e);
                    keyspaceInitialized = false;
                }
            }
        }
    }

    private CassandraDriver.Configuration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isKeyspaceInitialized() {
        synchronized (DefaultCassandraDriver.class) {
            return keyspaceInitialized;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Keyspace getKeyspace() {
        return keyspace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeKeyspace()
            throws ConnectionException {
        createKeyspace(getConfiguration());
    }

    private void createKeyspace(CassandraDriver.Configuration config)
            throws ConnectionException {

        synchronized (DefaultCassandraDriver.class) {
            if (!keyspaceInitialized) {

                LOGGER.info("Creating keyspace \"{}\"", keyspace.getKeyspaceName());

                Map<String, Object> keyspaceConfig = new HashMap<String, Object>();
                Map<String, Object> strategyOptions = new HashMap<String, Object>();

                String keyspaceReplicationStrategy = config.getKeyspaceStrategyClass();
                // Get the keyspaceReplicationStrategy and default it to SimpleStrategy
                if (keyspaceReplicationStrategy == null || keyspaceReplicationStrategy.trim().isEmpty()) {
                    keyspaceReplicationStrategy = "SimpleStrategy";
                }

                // If the replication strategy is NetworkTopologyStrategy we need to accommodate for
                // each of the data center and its associated replication factors
                if ("NetworkTopologyStrategy".equals(keyspaceReplicationStrategy)) {
                    String networkTopologyReplicationValues = config.getNetworkTopologyReplicationValues();
                    String[] replicationValues = networkTopologyReplicationValues.split(",");
                    for (String replicationValue : replicationValues) {
                        String[] datacenterReplicationValues = replicationValue.split(":");
                        strategyOptions.put(datacenterReplicationValues[0], datacenterReplicationValues[1]);
                    }
                } else if ("SimpleStrategy".equals(keyspaceReplicationStrategy)) {
                    int keyspaceReplicationFactor =
                            config.getKeyspaceReplicationFactor();
                    if (keyspaceReplicationFactor < 1) {
                        keyspaceReplicationFactor = 1;
                    }
                    strategyOptions.put("replication_factor", "" + keyspaceReplicationFactor);
                }

                keyspaceConfig.put("strategy_class", keyspaceReplicationStrategy);
                keyspaceConfig.put("strategy_options", strategyOptions);

                OperationResult<SchemaChangeResult> result =
                        keyspace.createKeyspace(Collections.unmodifiableMap(keyspaceConfig));

                // If we got this far, the keyspace was created
                keyspaceInitialized = true;

                LOGGER.info("Successfully created keyspace \"{}\"", keyspace.getKeyspaceName());
            }
        }
    }

    // Private class used for health check
    private class DefaultCassandraHealthCheck extends HealthCheck {

        /**
         * {@inheritDoc}
         */
        @Override
        public Result check() {
            boolean res = false;
            String msg = "";
            try {
                keyspace.prepareCqlStatement()
                        .withCql("SELECT now() FROM system.local")
                        .withConsistencyLevel(ConsistencyLevel.CL_QUORUM)
                        .execute();
                res = true;
            } catch (ConnectionException ex) {
                LOGGER.error("Unable to connect to cassandra", ex);
                msg = ex.getMessage();
            }
            return res ? Result.healthy() : Result.unhealthy(msg);
        }
    }
}
