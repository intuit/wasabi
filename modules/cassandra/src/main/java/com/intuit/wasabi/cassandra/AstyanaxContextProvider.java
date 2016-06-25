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
package com.intuit.wasabi.cassandra;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.SSLConnectionContext;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.Slf4jConnectionPoolMonitorImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;
import org.apache.commons.lang.StringUtils;

import static com.intuit.wasabi.cassandra.DefaultCassandraConstant.DEFAULT_CASSANDRA_VERSION;
import static com.intuit.wasabi.cassandra.DefaultCassandraConstant.DEFAULT_CQL_VERSION;
import static com.netflix.astyanax.model.ConsistencyLevel.CL_QUORUM;

/**
 * Provider for cassandra astyanax driver
 */
public class AstyanaxContextProvider implements Provider<AstyanaxContext<Keyspace>> {

    final CassandraDriver.Configuration configuration;

    @Inject
    public AstyanaxContextProvider(CassandraDriver.Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public AstyanaxContext<Keyspace> get() {
        String cassandraVersion = configuration.getTargetVersion();

        if (StringUtils.isBlank(cassandraVersion)) {
            cassandraVersion = DEFAULT_CASSANDRA_VERSION;
        }

        String cqlVersion = configuration.getCQLVersion();

        if (StringUtils.isBlank(cqlVersion)) {
            cqlVersion = DEFAULT_CQL_VERSION;
        }

        ConsistencyLevel readConsistency = configuration.getDefaultReadConsistency();

        if (readConsistency == null) {
            readConsistency = CL_QUORUM;
        }

        ConsistencyLevel writeConsistency = configuration.getDefaultWriteConsistency();

        if (writeConsistency == null) {
            writeConsistency = CL_QUORUM;
        }

        ConnectionPoolConfigurationImpl conf = getConnectionPoolConfiguration(configuration.getKeyspaceName() + "Pool");

        return getKeyspaceAstyanaxContext(cassandraVersion, cqlVersion, readConsistency, writeConsistency, conf);
    }


    AstyanaxContext<Keyspace> getKeyspaceAstyanaxContext(String cassandraVersion, String cqlVersion,
                                                         ConsistencyLevel readConsistency,
                                                         ConsistencyLevel writeConsistency,
                                                         ConnectionPoolConfigurationImpl conf) {
        return new AstyanaxContext.Builder()
//                  .forCluster("ClusterName") // Not sure why this is helpful
                .forKeyspace(configuration.getKeyspaceName())
                .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                        .setTargetCassandraVersion(cassandraVersion)
                        .setCqlVersion(cqlVersion)
                        .setDefaultReadConsistencyLevel(readConsistency)
                        .setDefaultWriteConsistencyLevel(writeConsistency)
                        .setDiscoveryType(configuration.getNodeDiscoveryType())
                        .setConnectionPoolType(configuration.getConnectionPoolType())
                )
                .withConnectionPoolConfiguration(conf)
                .withConnectionPoolMonitor(new Slf4jConnectionPoolMonitorImpl())
                .buildKeyspace(ThriftFamilyFactory.getInstance());
    }


    //TODO: do we really need Priam?
    private String getNodeHosts() {
        return configuration.getNodeHosts();
    }

    ConnectionPoolConfigurationImpl getConnectionPoolConfiguration(String poolName) {
        ConnectionPoolConfigurationImpl conf =
                new ConnectionPoolConfigurationImpl(poolName)
                        .setPort(configuration.getPort())
                        .setMaxConnsPerHost(configuration.getMaxConnectionsPerHost())
                        .setSeeds(getNodeHosts())
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
        if (configuration.useSSL()) {
            conf.setSSLConnectionContext(new SSLConnectionContext(
                    configuration.getSSLTrustStore(),
                    configuration.getSSLTrustStorePassword()
            ));
        }

        return conf;
    }
}
