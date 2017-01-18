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
import com.google.inject.name.Named;
import com.intuit.wasabi.export.rest.Driver;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class DefaultRestDriver implements Driver {

    private CloseableHttpClient closeableHttpClient;
    private CloseableHttpClient closeableHttpClientWithProxy;
    private static Logger LOGGER = getLogger(DefaultRestDriver.class);

    @Inject
    public DefaultRestDriver(final Driver.Configuration configuration, final @Named("export.http.proxy.host") String proxyHost,
                             final @Named("export.http.proxy.port") Integer proxyPort) {
        super();

        HttpClientBuilder httpClientBuilder = createHttpClientBuilder(configuration);
        this.closeableHttpClient = createCloseableHttpClient(httpClientBuilder);
        this.closeableHttpClientWithProxy = createCloseableHttpClientWithProxy(httpClientBuilder, proxyHost, proxyPort);

        LOGGER.info("Initializing Http driver");
    }

    private HttpClientBuilder createHttpClientBuilder(final Driver.Configuration configuration) {
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
                new PoolingHttpClientConnectionManager();

        poolingHttpClientConnectionManager.setMaxTotal(200);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(50);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(configuration.getConnectionTimeout())
                .setSocketTimeout(configuration.getSocketTimeout())
                .build();

        return HttpClients.custom()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig);
    }

    private CloseableHttpClient createCloseableHttpClient(final HttpClientBuilder httpClientBuilder) {
        return httpClientBuilder.build();
    }

    private CloseableHttpClient createCloseableHttpClientWithProxy(final HttpClientBuilder httpClientBuilder,
                                                                   final String proxyHost, final Integer proxyPort) {
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        DefaultProxyRoutePlanner defaultProxyRoutePlanner = new DefaultProxyRoutePlanner(proxy);

        httpClientBuilder.setRoutePlanner(defaultProxyRoutePlanner);

        return httpClientBuilder.build();
    }

    @Override
    public CloseableHttpClient getCloseableHttpClient(boolean useProxy) {
        return useProxy ? closeableHttpClientWithProxy : closeableHttpClient;
    }
}
