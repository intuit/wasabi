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
package com.intuit.wasabi.export.rest;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * A generic driver interface
 */
public interface Driver {

    /**
     * Get http closeable client
     * @param useProxy
     * @return CloseableHttpClient
     */
    CloseableHttpClient getCloseableHttpClient(boolean useProxy);

    /**
     * Configuration parameters for connection
     */
    interface Configuration {

        /**
         * Get connection timeout
         * @return connection timeout
         */
        int getConnectionTimeout();

        /**
         * Get socket timeout
         * @return socket timeout
         */
        int getSocketTimeout();
    }
}
