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
        int getMaxConnectionsPerHost();

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

    }
}
