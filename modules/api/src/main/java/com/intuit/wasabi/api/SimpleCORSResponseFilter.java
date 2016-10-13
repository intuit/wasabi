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

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

public class SimpleCORSResponseFilter implements ContainerResponseFilter {

    private final static Logger LOGGER = getLogger(SimpleCORSResponseFilter.class);

    public SimpleCORSResponseFilter() {
        LOGGER.info("Instantiated response filter {}", getClass().getName());
    }

    @Override
    public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse containerResponse) {
        LOGGER.trace("CORS filter called for request: {}", containerRequest);

        Response.ResponseBuilder response = Response.fromResponse(containerResponse.getResponse());

        if ("OPTIONS".equals(containerRequest.getMethod())) {

            response.status(204)
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                    .header("Content-Type", "application/json")
                    .entity("");

            String requestHeader = containerRequest.getHeaderValue("Access-Control-Request-Headers");

            if (Objects.nonNull(requestHeader)) {
                response.header("Access-Control-Allow-Headers", requestHeader);
            }
        }

        containerResponse.setResponse(response.build());

        return containerResponse;
    }


}
