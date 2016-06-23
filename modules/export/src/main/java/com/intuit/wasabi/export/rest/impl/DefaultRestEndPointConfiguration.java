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
package com.intuit.wasabi.export.rest.impl;

import com.intuit.wasabi.export.rest.RestEndPoint;

import java.util.Properties;

import static com.intuit.data.autumn.utils.PropertyFactory.create;

public class DefaultRestEndPointConfiguration implements RestEndPoint.Configuration {

    private Properties properties;

    /**
     * Constructor setup for testing or future injection purpose
     * @param properties the properties that contains the data
     */
    protected DefaultRestEndPointConfiguration(Properties properties){
        this.properties = properties;
    }

    /**
     * Returns the export rest endpoint scheme property.
     *
     * @return scheme String, can be http/https
     */
    @Override
    public String getScheme() {
        return (String) properties.get("export.rest.scheme");
    }

    /**
     * Returns the export rest endpoint hostname property.
     *
     * @return host String, is the restendpoint hostname to which the data is to be pushed to.
     */
    @Override
    public String getHost() {
        return (String) properties.get("export.rest.host");
    }

    /**
     * Returns the export rest endpoint port property.
     *
     * @return port number, is the restendpoint port to which the data is to be pushed to.
     */
    @Override
    public int getPort() {
        if (properties.get("export.rest.port") != null) {
            return Integer.parseInt((String) properties.get("export.rest.port"));
        }
        return 0;
    }

    /**
     * Returns the export rest endpoint path property.
     *
     * @return path String, is the restendpoint path to which the data is to be pushed to.
     */
    @Override
    public String getPath() {
        return (String) properties.get("export.rest.path");
    }

    /**
     * Returns the export rest endpoint proxy property.
     *
     * @return proxy boolean value, is if restendpoint uses proxy, can be true or false.
     */
    @Override
    public Boolean useProxy() {
        return Boolean.parseBoolean((String) properties.get("export.rest.useProxy"));
    }

    /**
     * Returns the export rest endpoint retries property.
     *
     * @return retry count, is the count of times a restendpoint would be retried.
     */
    @Override
    public int getRetries() {
        return Integer.parseInt((String) properties.get("export.rest.retries"));
    }
}
