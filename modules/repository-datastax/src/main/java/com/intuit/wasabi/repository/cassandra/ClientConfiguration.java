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
package com.intuit.wasabi.repository.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.SocketOptions;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static java.lang.Boolean.FALSE;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Property-driven CassandraDriver configuration
 */
public class ClientConfiguration implements CassandraDriver.Configuration {

    private Properties properties;

    /**
     * Create an instance bound to the property context
     *
     * @param propertyContext property context
     */
    public ClientConfiguration(final String propertyContext) {
        super();

        properties = create(checkNotNull(propertyContext));
    }

    /*
     * @see com.intuit.wasabi.cassandra.CassandraDriver.Configuration#getNodeHosts()
     */
    @Override
    public List<String> getNodeHosts() {
        return Arrays.stream(getProperty("nodeHosts", properties).split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return getProperty("username", properties, "");
    }

    @Override
    public String getPassword() {
        return getProperty("password", properties, "");
    }

    @Override
    public int getPort() {
        return parseInt(getProperty("port", properties, "9160"));
    }

    @Override
    public Boolean useSSL() {
        return Boolean.valueOf(getProperty("useSSL", properties, FALSE.toString()));
    }

    @Override
    public String getSSLTrustStore() {
        return getProperty("trustStore", properties);
    }

    @Override
    public String getSSLTrustStorePassword() {
        return getProperty("trustStorePassword", properties);
    }

    @Override
    public String getKeyspaceName() {
        return getProperty("keyspaceName", properties);
    }

    @Override
    public int getKeyspaceReplicationFactor() {
        return parseInt(getProperty("keyspaceReplicationFactor", properties));
    }

    @Override
    public String getKeyspaceStrategyClass() {
        return getProperty("keyspaceStrategyClass", properties);
    }

    // These are options used by the PoolingOptions for cassandra
    @Override
    public int getMaxConnectionsPerHost() {
        return parseInt(getProperty("maxConnectionsPerHost", properties, "10"));
    }

    @Override
    public int getMaxConnectionsPerHostLocal() {
        return parseInt(getProperty("maxConnectionsPerHostLocal", properties, "32"));
    }

    @Override
    public int getCoreConnectionsPerHostLocal() {
        return parseInt(getProperty("coreConnectionsPerHostLocal", properties, "8"));
    }

    @Override
    public int getMaxRequestPerConnectionLocal() {
        return parseInt(getProperty("maxRequestPerConnectionLocal", properties, "32768"));
    }

    @Override
    public int getNewConnectionThresholdLocal() {
        return parseInt(getProperty("newConnectionThresholdLocal", properties, "50"));
    }

    @Override
    public int getPoolTimeoutMillis() {
        return parseInt(getProperty("poolTimeoutMillis", properties, "0"));
    }

    @Override
    public int getConnectTimeoutMillis() {
        return parseInt(getProperty("connectTimeoutMillis", properties, String.valueOf(SocketOptions.DEFAULT_CONNECT_TIMEOUT_MILLIS)));
    }

    @Override
    public int getReadTimeoutMillis() {
        return parseInt(getProperty("readTimeoutMillis", properties, String.valueOf(SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS)));
    }

    @Override
    public int getMaxConnectionsPerHostRemote() {
        return parseInt(getProperty("maxConnectionsPerHostRemote", properties, "32"));
    }

    @Override
    public int getCoreConnectionsPerHostRemote() {
        return parseInt(getProperty("coreConnectionsPerHostRemote", properties, "8"));
    }

    @Override
    public int getMaxRequestPerConnectionRemote() {
        return parseInt(getProperty("maxRequestPerConnectionRemote", properties, "2000"));
    }

    @Override
    public int getNewConnectionThresholdRemote() {
        return parseInt(getProperty("newConnectionThresholdRemote", properties, "50"));
    }

    @Override
    public ConsistencyLevel getDefaultReadConsistency() {
        String value = getProperty("defaultReadConsistency", properties);

        return !isBlank(value) ? ConsistencyLevel.valueOf(value) : ConsistencyLevel.LOCAL_QUORUM;
    }

    @Override
    public ConsistencyLevel getDefaultWriteConsistency() {
        String value = getProperty("defaultWriteConsistency", properties);

        return !isBlank(value) ? ConsistencyLevel.valueOf(value) : ConsistencyLevel.LOCAL_QUORUM;
    }

    /**
     * Returns the string reflecting the values of the replication factor for each mentioned data center.
     * Format of the value is DataCenter1:ReplicationFactor,DataCenter2:ReplicationFactor,....
     */
    @Override
    public String getNetworkTopologyReplicationValues() {
        return getProperty("networkTopologyReplicationValues", properties);
    }

    public Optional<String> getTokenAwareLoadBalancingLocalDC() {
        return Optional.ofNullable(getProperty("tokenAwareLoadBalancingLocalDC", properties));
    }

    public Integer getTokenAwareLoadBalancingUsedHostsPerRemoteDc() {
        return Integer.valueOf(getProperty("tokenAwareLoadBalancingUsedHostsPerRemoteDc", properties, "1"));
    }

    @Override
    public boolean isSlowQueryLoggingEnabled() {
        return Boolean.valueOf(getProperty("isSlowQueryLoggingEnabled", properties, "False"));
    }

    @Override
    public long getSlowQueryLoggingThresholdMilli() {
        return Long.valueOf(getProperty("slowQueryLoggingThresholdMilli", properties, "100"));
    }
}
