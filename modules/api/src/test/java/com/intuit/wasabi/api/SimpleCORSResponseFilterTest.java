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

import com.sun.jersey.core.header.OutBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SimpleCORSResponseFilterTest {

    @Mock
    private ContainerRequest containerRequest;
    @Mock
    private ContainerResponse containerResponse;

    private SimpleCORSResponseFilter filter;

    @Before
    public void setup() {
        filter = new SimpleCORSResponseFilter("name", "600");
    }

    @Test
    public void filter() {
        Response response = Response.ok().build();
        when(containerResponse.getResponse()).thenReturn(response);
        when(containerResponse.getStatus()).thenReturn(Status.OK.getStatusCode());
        when(containerResponse.getHttpHeaders()).thenReturn(new OutBoundHeaders());
        when(containerRequest.getMethod()).thenReturn("OPTIONS");
        when(containerRequest.getHeaderValue("Access-Control-Request-Headers")).thenReturn("foo");

        ContainerResponse returnedContainerResponse = filter.filter(containerRequest, containerResponse);
        assertNotNull(returnedContainerResponse);
        assertEquals(Status.OK.getStatusCode(), returnedContainerResponse.getStatus());

        response = Response.status(Status.NOT_FOUND.getStatusCode()).build();
        when(containerResponse.getStatus()).thenReturn(Status.NOT_FOUND.getStatusCode());
        returnedContainerResponse = filter.filter(containerRequest, containerResponse);
        assertEquals(Status.NOT_FOUND.getStatusCode(), returnedContainerResponse.getStatus());
    }

}
