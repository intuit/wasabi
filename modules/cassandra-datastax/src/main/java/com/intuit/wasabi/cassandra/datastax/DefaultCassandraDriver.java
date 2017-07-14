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
package com.intuit.wasabi.cassandra.datastax;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.QueryLogger;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.cassandra.datastax.health.DefaultCassandraHealthCheck;
import org.slf4j.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default implementation of {@link CassandraDriver}
 */
public class DefaultCassandraDriver implements CassandraDriver {

    private static final Logger LOGGER = getLogger(DefaultCassandraDriver.class);
    private static final String[] CIPHER_SUITES = {"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"};
    private CassandraDriver.Configuration configuration;
    private Cluster cluster;
    private Session session;
    private boolean keyspaceInitialized;

    @Inject
    public DefaultCassandraDriver(CassandraDriver.Configuration config,
                                  HealthCheckRegistry healthCheckRegistry,
                                  @Named("CassandraInstanceName") String instanceName)
            throws IOException {
        super();

        this.configuration = config;

        LOGGER.info("Initializing driver");
        initialize();
        // Register for health check
        healthCheckRegistry.register(instanceName, new DefaultCassandraHealthCheck(getSession()));
    }

    private void initialize() throws IOException {
        if (cluster == null) {
            synchronized (DefaultCassandraDriver.class) {
                Cluster.Builder builder = Cluster.builder();
                List<String> nodes = getConfiguration().getNodeHosts();
                builder.addContactPoints(nodes.toArray(new String[nodes.size()]))
                        .withRetryPolicy(DefaultRetryPolicy.INSTANCE);
                builder.withPort(getConfiguration().getPort());
                builder.withCredentials(
                        getConfiguration().getUsername(),
                        getConfiguration().getPassword()
                );

                if (getConfiguration().getTokenAwareLoadBalancingLocalDC().isPresent() &&
                        getConfiguration().getTokenAwareLoadBalancingUsedHostsPerRemoteDc() >= 0) {
                    builder.withLoadBalancingPolicy(
                            new TokenAwarePolicy(
                                    new DCAwareRoundRobinPolicy.Builder()
                                            .withLocalDc(getConfiguration()
                                                    .getTokenAwareLoadBalancingLocalDC().get())
                                            .withUsedHostsPerRemoteDc(getConfiguration().
                                                    getTokenAwareLoadBalancingUsedHostsPerRemoteDc())
                                            .allowRemoteDCsForLocalConsistencyLevel()
                                            .build()
                            )
                    );
                } else {
                    builder.withLoadBalancingPolicy(
                            new TokenAwarePolicy(
                                    new RoundRobinPolicy()
                            )
                    );
                }


                //Add default query options for consistency
                ConsistencyLevel writeConsistency = getConfiguration().getDefaultWriteConsistency();
                QueryOptions queryOptions = new QueryOptions().setConsistencyLevel(writeConsistency);
                builder.withQueryOptions(queryOptions);

                //Configure the connection pool's options
                PoolingOptions poolingOptions = new PoolingOptions()
                        .setPoolTimeoutMillis(getConfiguration().getPoolTimeoutMillis())
                        .setConnectionsPerHost(HostDistance.LOCAL,
                                getConfiguration().getCoreConnectionsPerHostLocal(),
                                getConfiguration().getMaxConnectionsPerHostLocal())
                        .setConnectionsPerHost(HostDistance.REMOTE,
                                getConfiguration().getCoreConnectionsPerHostRemote(),
                                getConfiguration().getMaxConnectionsPerHostRemote())
                        .setMaxRequestsPerConnection(HostDistance.LOCAL,
                                getConfiguration().getMaxRequestPerConnectionLocal()) //range  (0, 32768)
                        .setMaxRequestsPerConnection(HostDistance.REMOTE,
                                getConfiguration().getMaxRequestPerConnectionRemote())
                        .setNewConnectionThreshold(HostDistance.LOCAL,
                                getConfiguration().getNewConnectionThresholdLocal())
                        .setNewConnectionThreshold(HostDistance.REMOTE,
                                getConfiguration().getNewConnectionThresholdRemote());
                builder.withPoolingOptions(poolingOptions);

                //Configure to use compression
                builder.withCompression(ProtocolOptions.Compression.LZ4);


                //The connection timeout in milliseconds.
                //As the name implies, the connection timeout defines how long the driver waits to establish a new connection to a Cassandra node before giving up.
                //Default value is 5000ms
                builder.getConfiguration().getSocketOptions().setConnectTimeoutMillis(getConfiguration().getConnectTimeoutMillis());

                //The per-host read timeout in milliseconds.
                //This defines how long the driver will wait for a given Cassandra node to answer a query.
                //Default value is 12000ms
                builder.getConfiguration().getSocketOptions().setReadTimeoutMillis(getConfiguration().getReadTimeoutMillis());

                // SSL connection
                if (getConfiguration().useSSL()) {
                    try {
                        SSLContext context = getSSLContext(getConfiguration().getSSLTrustStore(),
                                getConfiguration().getSSLTrustStorePassword());

                        builder.withSSL(JdkSSLOptions.builder()
                                .withSSLContext(context)
                                .withCipherSuites(CIPHER_SUITES).build());
                    } catch (Exception ex) {
                        LOGGER.error("General exception while construct SSL Context: ", ex);
                        //TODO: should we fail fast if no ssl is configured correctly? I think yes, but it is open.
                    }
                }

                LOGGER.info("Connecting to nodes {}, on port {}", nodes, getConfiguration().getPort());
                this.cluster = builder.build();

                synchronized (this) {
                    try {
                        session = cluster.connect(getConfiguration().getKeyspaceName());
                    } catch (InvalidQueryException ex) { //This exception occurs when namesapce does not exists
                        session = cluster.connect(); // have to attach to the root keyspace first
                        initializeKeyspace();
                    } catch (Exception e) {
                        LOGGER.error("Exception occurred while connecting to the cluster...", e);
                        throw e;
                    }

                    // Try to get the definition to test if the keyspace exists
                    try {

                        if (session.getLoggedKeyspace() != null) {
                            keyspaceInitialized = true;
                        } else {
                            initializeKeyspace();
                            keyspaceInitialized = true;
                        }

                    } catch (DriverException e) {
                        LOGGER.warn("Keyspace " + getConfiguration().getKeyspaceName() + " doesn't exist", e);
                        keyspaceInitialized = false;
                    }
                }

                try {
                    Metadata metadata = cluster.getMetadata();
                    LOGGER.info("Connected to cluster: {}\n", metadata.getClusterName());
                    for (Host host : metadata.getAllHosts()) {
                        LOGGER.info("Datatacenter: {}; Host: {}; Rack: {}\n",
                                host.getDatacenter(),
                                host.getAddress(),
                                host.getRack());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to connect to cluster\n", e);
                }

                if (getConfiguration().isSlowQueryLoggingEnabled()) {

                    LOGGER.warn("Enabling slow query logging could have performance impact!!");
                    QueryLogger queryLogger = QueryLogger.builder()
                            .withConstantThreshold(getConfiguration().getSlowQueryLoggingThresholdMilli())
                            .build();
                    cluster.register(queryLogger);
                    LOGGER.warn("Slow query logging threshold is set to be {} milliseconds",
                            getConfiguration().getSlowQueryLoggingThresholdMilli());

                    poolingMonitoring(poolingOptions);
                }

                LOGGER.info("Connected to the {} keyspace", getConfiguration().getKeyspaceName());
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
    public void initializeKeyspace() {
        createKeyspace(getConfiguration());
    }

    @Override
    public Session getSession() {
        return session;
    }

    //we wouldn't been here if the keyspace already exists
    private void createKeyspace(CassandraDriver.Configuration config) {

        synchronized (DefaultCassandraDriver.class) {
            if (!keyspaceInitialized) {

                LOGGER.info("Creating keyspace \"{}\"", config.getKeyspaceName());
                StringBuilder sb = new StringBuilder("CREATE KEYSPACE "
                        + config.getKeyspaceName()
                        + " WITH replication = { 'class' : ");
                // Send all output to the Appendable object sb
                Formatter formatter = new Formatter(sb, Locale.US);

                Map<String, Object> keyspaceConfig = new HashMap<>();
                Map<String, Object> strategyOptions = new HashMap<>();

                String keyspaceReplicationStrategy = config.getKeyspaceStrategyClass();
                // Get the keyspaceReplicationStrategy and default it to SimpleStrategy
                if (keyspaceReplicationStrategy == null || keyspaceReplicationStrategy.trim().isEmpty()) {
                    keyspaceReplicationStrategy = "SimpleStrategy";
                }

                // If the replication strategy is NetworkTopologyStrategy we need to accommodate for
                // each of the data center and its associated replication factors
                if ("NetworkTopologyStrategy".equals(keyspaceReplicationStrategy)) {
                    sb.append("NetworkTopologyStrategy");
                    String networkTopologyReplicationValues = config.getNetworkTopologyReplicationValues();
                    String[] replicationValues = networkTopologyReplicationValues.split(",");
                    for (String replicationValue : replicationValues) {
                        String[] datacenterReplicationValues = replicationValue.split(":");
                        strategyOptions.put(datacenterReplicationValues[0], datacenterReplicationValues[1]);
                        sb.append(", '")
                                .append(datacenterReplicationValues[0])
                                .append("' : ").
                                append(datacenterReplicationValues[1]);
                    }
                    sb.append(" }");
                } else if ("SimpleStrategy".equals(keyspaceReplicationStrategy)) {
                    int keyspaceReplicationFactor =
                            config.getKeyspaceReplicationFactor();
                    if (keyspaceReplicationFactor < 1) {
                        keyspaceReplicationFactor = 1;
                    }
                    strategyOptions.put("replication_factor", "" + keyspaceReplicationFactor);
                    sb.append("'SimpleStrategy', 'replication_factor' : ").append(keyspaceReplicationFactor)
                            .append(" }");
                }

                keyspaceConfig.put("strategy_class", keyspaceReplicationStrategy);
                keyspaceConfig.put("strategy_options", strategyOptions);

                session.execute(sb.toString());

                LOGGER.info("Successfully created keyspace \"{}\"", getConfiguration().getKeyspaceName());
                //reset session to point to the keyspace
                session = cluster.connect(getConfiguration().getKeyspaceName());

                // If we got this far, the keyspace was created
                keyspaceInitialized = true;
            }
        }
    }

    private static SSLContext getSSLContext(String truststorePath,
                                            String truststorePassword) throws Exception {
        FileInputStream tsf = new FileInputStream(truststorePath);
        SSLContext ctx = SSLContext.getInstance("SSL");

        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(tsf, truststorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        tsf = new FileInputStream(truststorePath);
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(tsf, truststorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, truststorePassword.toCharArray());

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return ctx;
    }


    @Override
    public void close() {
        this.session.close();
        this.cluster.close();
    }

    private void poolingMonitoring(PoolingOptions poolingOptions) {
        final LoadBalancingPolicy loadBalancingPolicy =
                cluster.getConfiguration().getPolicies().getLoadBalancingPolicy();
        ScheduledExecutorService scheduled =
                Executors.newScheduledThreadPool(1);
        scheduled.scheduleAtFixedRate((Runnable) () -> {
            Session.State state = session.getState();
            for (Host host : state.getConnectedHosts()) {
                HostDistance distance = loadBalancingPolicy.distance(host);
                int connections = state.getOpenConnections(host);
                int inFlightQueries = state.getInFlightQueries(host);
                LOGGER.info("{} connections={}, current load={}, max load={}",
                        host, connections, inFlightQueries,
                        connections * poolingOptions.getMaxRequestsPerConnection(distance));
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
}
