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

import com.google.inject.AbstractModule;
import com.intuit.wasabi.export.rest.Driver;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

public class ExportModule extends AbstractModule {

    public static final String PROPERTY_NAME = "/export.properties";
    private static final Logger LOGGER = getLogger(ExportModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", ExportModule.class.getSimpleName());

        Properties properties = create(PROPERTY_NAME, ExportModule.class);

        bind(Integer.class).annotatedWith(named("export.rest.client.connectionTimeout"))
                .toInstance(Integer.valueOf(getProperty("export.rest.client.connectionTimeout", properties)));
        bind(Integer.class).annotatedWith(named("export.rest.client.socketTimeout"))
                .toInstance(Integer.valueOf(getProperty("export.rest.client.socketTimeout", properties)));
        bind(String.class).annotatedWith(named("export.http.proxy.host"))
                .toInstance(getProperty("http.proxy.host", properties));
        bind(Integer.class).annotatedWith(named("export.http.proxy.port"))
                .toInstance(Integer.valueOf(getProperty("http.proxy.port", properties, "80")));
        bind(Driver.class).to(DefaultRestDriver.class).in(SINGLETON);
        bind(Driver.Configuration.class).to(DefaultDriverConfiguration.class);

        LOGGER.debug("installed module: {}", ExportModule.class.getSimpleName());
    }
}
