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
package com.intuit.wasabi.repository.impl.cassandra;

import com.intuit.wasabi.cassandra.CassandraDriver;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolType;
import com.netflix.astyanax.model.ConsistencyLevel;

import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static com.netflix.astyanax.connectionpool.NodeDiscoveryType.*;
import static com.netflix.astyanax.connectionpool.impl.ConnectionPoolType.*;
import static com.netflix.astyanax.connectionpool.impl.ConnectionPoolType.TOKEN_AWARE;
import static com.netflix.astyanax.model.ConsistencyLevel.CL_QUORUM;
import static java.lang.Boolean.FALSE;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Property-driven CassandraDriver configuration
 */
public class DriverConfiguration implements CassandraDriver.Configuration {

    private Properties properties;

    /**
     * Create an instance bound to the property context
     *
     * @param propertyContext property context
     */
    public DriverConfiguration(final String propertyContext) {
        super();

        properties = create(checkNotNull(propertyContext));
    }

    /*
     * @see com.intuit.wasabi.cassandra.CassandraDriver.Configuration#getNodeHosts()
     */
    @Override
    public String getNodeHosts() {
        return getProperty("nodeHosts", properties);
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

    @Override
    public int getMaxConnectionsPerHost() {
        return parseInt(getProperty("maxConnectionsPerHost", properties, "10"));
    }

    @Override
    public String getTargetVersion() {
        return getProperty("targetVersion", properties);
    }

    @Override
    public String getCQLVersion() {
        return getProperty("cqlVersion", properties);
    }

    @Override
    public ConsistencyLevel getDefaultReadConsistency() {
        String value = getProperty("defaultReadConsistency", properties);

        return !isBlank(value) ? ConsistencyLevel.valueOf(value) : CL_QUORUM;
    }

    @Override
    public ConsistencyLevel getDefaultWriteConsistency() {
        String value = getProperty("defaultWriteConsistency", properties);

        return !isBlank(value) ? ConsistencyLevel.valueOf(value) : CL_QUORUM;
    }

    /**
     * Returns the string reflecting the values of the replication factor for each mentioned data center.
     * Format of the value is DataCenter1:ReplicationFactor,DataCenter2:ReplicationFactor,....
     */
    @Override
    public String getNetworkTopologyReplicationValues() {
        return getProperty("networkTopologyReplicationValues", properties);
    }

    /**
     * Returns the astyanax ConnectionPoolType
     *
     * @return ConnectionPoolType Default:TOKEN_AWARE
     */
    @Override
    public ConnectionPoolType getConnectionPoolType() {
        ConnectionPoolType result = TOKEN_AWARE;
        String value = getProperty("connectionPoolType", properties);

        if (! isBlank(value)) {
            if ("ROUND_ROBIN".equalsIgnoreCase(value)) {
                result = ROUND_ROBIN;
            } else if ("BAG".equalsIgnoreCase(value)) {
                result = BAG;
            }
        }

        return result;
    }

    /**
     * Returns the astyanax NodeDiscoveryType
     *
     * @return NodeDiscoveryType Default:RING_DESCRIBE
     */
    @Override
    public NodeDiscoveryType getNodeDiscoveryType() {
        NodeDiscoveryType result = RING_DESCRIBE;
        String value = getProperty("nodeDiscoveryType", properties);

        if (value != null && !value.trim().isEmpty()) {
            if ("DISCOVERY_SERVICE".equalsIgnoreCase(value)) {
                result = DISCOVERY_SERVICE;
            } else if ("TOKEN_AWARE".equalsIgnoreCase(value)) {
                result = NodeDiscoveryType.TOKEN_AWARE;
            } else if ("NONE".equalsIgnoreCase(value)) {
                result = NONE;
            }
        }

        return result;
    }
}
