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
package com.intuit.wasabi.cassandra.datastax.health;

import com.codahale.metrics.health.HealthCheck;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created on 3/2/16.
 */

@RunWith(MockitoJUnitRunner.class)
public class DefaultCassandraHealthCheckTest {

    @Mock
    private Session session;
    private DefaultCassandraHealthCheck defaultCassandraHealthCheck;

    @Before
    public void setup() {
        defaultCassandraHealthCheck = spy(new DefaultCassandraHealthCheck(session));
    }

    @Test
    public void checkTest() {
        when(session.execute(any(Statement.class))).thenReturn(null);
        assertThat(defaultCassandraHealthCheck.check(), is(HealthCheck.Result.healthy()));

        doThrow(new NoHostAvailableException(new HashMap()))
                .when(session).execute(eq(DefaultCassandraHealthCheck.SELECT_NOW_FROM_SYSTEM_LOCAL));
        assertThat(defaultCassandraHealthCheck.check(),
                is(HealthCheck.Result.unhealthy("All host(s) tried for query failed (no host was tried)")));
    }

}
