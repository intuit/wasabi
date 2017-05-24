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
package com.intuit.wasabi.export.rest.impl;

import com.google.inject.Inject;
import com.intuit.wasabi.export.rest.RestEndPoint;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class DefaultRestEndPoint implements RestEndPoint {

    private static final Logger LOGGER = getLogger(DefaultRestEndPoint.class);
    private final Configuration configuration;

    @Inject
    public DefaultRestEndPoint(RestEndPoint.Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public URI getRestEndPointURI() {
        URI assignmentURI = null;
        try {
            URIBuilder uriBuilder = new URIBuilder().setScheme(configuration.getScheme())
                    .setHost(configuration.getHost())
                    .setPath(configuration.getPath());
            int port = configuration.getPort();
            if (port != 0) {
                uriBuilder.setPort(port);
            }
            assignmentURI = uriBuilder.build();
        } catch (URISyntaxException e) {
            LOGGER.error("URL Syntax Error: ", e);
        }
        return assignmentURI;
    }

    @Override
    public Boolean useProxy() {
        return configuration.useProxy();
    }

    @Override
    public int getRetries() {
        return configuration.getRetries();
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(configuration.getRetries());
        if (configuration.isExponentialBackoffEnabled()) {
            retryPolicy = retryPolicy.withBackoff(configuration.getExponentialBackoffDelay(),
                    configuration.getExponentialBackoffMaxDelay(), TimeUnit.SECONDS);
        }
        return retryPolicy;
    }
}
