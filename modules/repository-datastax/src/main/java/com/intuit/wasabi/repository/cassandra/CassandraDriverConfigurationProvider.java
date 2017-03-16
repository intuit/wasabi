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
package com.intuit.wasabi.repository.cassandra;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraDriverConfigurationProvider implements Provider<CassandraDriver.Configuration> {
    private final Logger logger = LoggerFactory.getLogger(CassandraDriverConfigurationProvider.class);
    private final ClientConfiguration config;

    @Inject
    public CassandraDriverConfigurationProvider(@Named("cassandraClientConfig") String path) {
        this.config = new ClientConfiguration(path);
    }


    @Override
    public CassandraDriver.Configuration get() {
        return config;
    }
}