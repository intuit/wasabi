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

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Low-level access to Cassandra using Astyanax
 */
public interface CassandraDriver extends Closeable {

    /**
     * Check whether the keyspace has already been initialized
     *
     * @return True if the keyspace has already been intialized
     */
    boolean isKeyspaceInitialized();

    /**
     * Initialize the keyspace.
     *
     * @throws IOException when cannot connect to cassandra
     */
    void initializeKeyspace() throws IOException;

    /**
     * The session {@link Session} managed by this driver
     *
     * @return The Session instance. Never null.
     */
    Session getSession();

    ////////////////////////////////////////////////////////////////////////////
    // Types
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Configuration to be provided to driver implementations
     */
    interface Configuration {

        /**
         * Returns keyspace name
         * @return keyspace name
         */
        String getKeyspaceName();

        /**
         * Returns Cassandra username
         * @return username
         */
        String getUsername();

        /**
         * Returns Cassandra password
         * @return password
         */
        String getPassword();

        /**
         * Returns Cassandra port
         * @return port number
         */
        int getPort();

        /**
         * Are we using a SSL connection
         * @return true if using SSL
         */
        Boolean useSSL();

        /**
         * Absolute path for the SSL trust keystore file
         * @return the path to SSL key store file
         */
        String getSSLTrustStore();


        /**
         * Password for the SSL trust keystore
         * @return password for ssl trust keystore
         */
        String getSSLTrustStorePassword();

        /**
         * Returns the keyspace replication factor
         * @return replication factor of keyspace
         */
        int getKeyspaceReplicationFactor();

        /**
         * Returns the keyspace strategy class
         * @return Keyspace strategy class
         */
        String getKeyspaceStrategyClass();

        /**
         * Returns node hosts
         * @return Node hosts, comma-separated
         */
        List<String> getNodeHosts();

        /**
         * Returns max connections per host
         * @return Max connections per host
         */
        @Deprecated
        int getMaxConnectionsPerHost();

        /**
         * Returns max connections per local host
         * @return Max connections per local host
         */
        int getMaxConnectionsPerHostLocal();

        /**
         * Returns core/min connections per local host
         * @return core/min connections per local host
         */
        int getCoreConnectionsPerHostLocal();

        /**
         * Returns max requests per local host
         * @return Max requests per local host
         */
        int getMaxRequestPerConnectionLocal();

        /**
         * Returns new connection threshold per local host
         * @return New connection threshold per local host
         */
        int getNewConnectionThresholdLocal();

        /**
         * Returns pool timeouts in milliseconds
         * @return Pool timeouts in milliseconds
         */
        int getPoolTimeoutMillis();

        /**
         * Returns the defined connection timeout in milliseconds.
         * @return the defined connection timeout in milliseconds
         */
        int getConnectTimeoutMillis();

        /**
         * Returned the per-host read timeout in milliseconds from the configuration.
         * @return the defined read timeout in milliseconds from the configuration
         */
        int getReadTimeoutMillis();

        /**
         * Returns max connections per remote host
         * @return max connections per remote host
         */
        int getMaxConnectionsPerHostRemote();

        /**
         * Returns core/min connections per remote host
         * @return core/min connections per remote host
         */
        int getCoreConnectionsPerHostRemote();

        /**
         * Returns max requests per remote host
         * @return Max requests per remote host
         */
        int getMaxRequestPerConnectionRemote();

        /**
         * Returns new connection threshold per remote host
         * @return New connection threshold per remote host
         */
        int getNewConnectionThresholdRemote();

//        /**
//         * The target version of Cassandra
//         * @return target version number
//         */
//        String getTargetVersion();
//
//        /**
//         * Returns CQL version
//         * @return CQL version
//         */
//        String getCQLVersion();

        /**
         * Returns the default read consistency
         * @return Default read consistency
         */
        ConsistencyLevel getDefaultReadConsistency();

        /**
         * Returns the default write consistency
         * @return default write consistency
         */
        ConsistencyLevel getDefaultWriteConsistency();

        /**
         * Returns the string reflecting the values of the replication factor for each mentioned data center.
         * @return Format of the value is DataCenter1:ReplicationFactor,DataCenter2:ReplicationFactor,....
         */
        String getNetworkTopologyReplicationValues();

//        /**
//         * Returns the astyanax ConnectionPoolType
//         *
//         * @return ConnectionPoolType Default:TOKEN_AWARE
//         */
//        PoolingOptions getConnectionPoolType();

        /**
         *
         * @return an Optional value that contains the name of the local dc for load balancing
         */
        Optional<String> getTokenAwareLoadBalancingLocalDC();

        /**
         *
         * @return an Integer value of the number of remote host(s) to be used for load balancing
         */

        Integer getTokenAwareLoadBalancingUsedHostsPerRemoteDc();

        /**
         *
         * @return boolean true if slow query logging is enabled
         */
        boolean isSlowQueryLoggingEnabled();

        /**
         * This option is only usable when isSlowQueryLoggingEnabled is true
         * @return a long to indicate the number of milliseconds to consider the query to be slow.
         */
        long getSlowQueryLoggingThresholdMilli();

    }
}
