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
package com.intuit.wasabi.cassandra.health;

import com.codahale.metrics.health.HealthCheck;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionAbortedException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.cql.CqlStatement;
import com.netflix.astyanax.model.ConsistencyLevel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Created on 3/2/16.
 */

@RunWith(MockitoJUnitRunner.class)
public class DefaultCassandraHealthCheckTest {

    @Mock private Keyspace keyspace;
    private DefaultCassandraHealthCheck defaultCassandraHealthCheck;

    @Before
    public void setup(){
        defaultCassandraHealthCheck = new DefaultCassandraHealthCheck(keyspace);
    }

    @Test
    public void checkTest() throws ConnectionException {
        HealthCheck.Result result = Mockito.mock(HealthCheck.Result.class);
        CqlStatement cqlStatment = Mockito.mock(CqlStatement.class);
        when(keyspace.prepareCqlStatement()).thenReturn(cqlStatment);
        when(cqlStatment.withCql(anyString())).thenReturn(cqlStatment);
        when(cqlStatment.withConsistencyLevel(eq(ConsistencyLevel.CL_QUORUM))).thenReturn(cqlStatment);

        assertThat(defaultCassandraHealthCheck.check(), is(HealthCheck.Result.healthy()));

        doThrow(new ConnectionAbortedException("mocked")).when(cqlStatment).execute();
        assertThat(defaultCassandraHealthCheck.check(),
                is(HealthCheck.Result.unhealthy("ConnectionAbortedException: " +
                        "[host=None(0.0.0.0):0, latency=0(0), attempts=0]mocked")));
    }

}
