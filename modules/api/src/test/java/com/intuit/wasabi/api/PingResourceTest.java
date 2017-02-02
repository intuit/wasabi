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
package com.intuit.wasabi.api;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PingResourceTest {

    @Mock
    private HealthCheckRegistry healthChecks;
    private PingResource resouce;
    private String version = "v1.0";

    @Before
    public void setup() {
        resouce = new PingResource(healthChecks, new HttpHeader(version, "600"));
    }

    @Test
    public void ping() throws Exception {
        healthChecks.register("component1", new HealthCheck() {

            @Override
            protected Result check() throws Exception {
                return HealthCheck.Result.healthy();
            }
        });
        healthChecks.register("component2", new HealthCheck() {

            @Override
            protected Result check() throws Exception {
                return HealthCheck.Result.unhealthy("something went wrong");
            }
        });
        resouce.ping();
    }

    @Test
    public void pingTest() throws Exception {
        SortedMap<String, HealthCheck.Result> result = new TreeMap<String, HealthCheck.Result>();
        result.put("test", HealthCheck.Result.unhealthy("test unhealthy"));
        when(healthChecks.runHealthChecks()).thenReturn(result);
        Response answer = resouce.ping();
        Response expected = Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("").build();
        assertThat(answer.getStatus(), is(expected.getStatus()));
    }
}
