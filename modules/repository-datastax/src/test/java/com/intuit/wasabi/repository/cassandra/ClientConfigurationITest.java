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
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver.Configuration;
import com.intuit.wasabi.database.DatabaseModule;
import com.intuit.wasabi.eventlog.EventLogModule;
import com.intuit.wasabi.repository.database.DatabaseExperimentRepositoryModule;
import com.intuit.wasabi.userdirectory.UserDirectoryModule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClientConfigurationITest {

    @Test
    public void testDefaultProperties() {
        Injector injector = Guice.createInjector(
                new UserDirectoryModule(),
                new EventLogModule(),
                new DatabaseModule(),
                new DatabaseExperimentRepositoryModule(),
                new CassandraRepositoryModule());
        Configuration config = injector.getInstance(CassandraDriver.Configuration.class);
        assertEquals(ConsistencyLevel.LOCAL_ONE, config.getDefaultReadConsistency());
        assertEquals(ConsistencyLevel.LOCAL_ONE, config.getDefaultWriteConsistency());
        assertEquals("wasabi_experiments", config.getKeyspaceName());
        assertEquals(1, config.getKeyspaceReplicationFactor());
        assertEquals(10, config.getMaxConnectionsPerHost());
        assertEquals("datacenter1:1", config.getNetworkTopologyReplicationValues());
        assertEquals(Arrays.asList("localhost"), config.getNodeHosts());
        assertEquals(9042, config.getPort());
        assertNull(config.getSSLTrustStore());
        assertNull(config.getSSLTrustStorePassword());
        assertEquals(Optional.empty(), config.getTokenAwareLoadBalancingLocalDC());
        assertEquals((Integer) 2, config.getTokenAwareLoadBalancingUsedHostsPerRemoteDc());
        assertEquals("SimpleStrategy", config.getKeyspaceStrategyClass());
    }

}
