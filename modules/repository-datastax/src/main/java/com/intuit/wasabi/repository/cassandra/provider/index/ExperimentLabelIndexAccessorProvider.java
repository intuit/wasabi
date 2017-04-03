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
package com.intuit.wasabi.repository.cassandra.provider.index;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentLabelIndexAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentLabelIndexAccessorProvider implements Provider<ExperimentLabelIndexAccessor> {
    private final Logger logger = LoggerFactory.getLogger(ExperimentLabelIndexAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public ExperimentLabelIndexAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public ExperimentLabelIndexAccessor get() {
        return manager.createAccessor(ExperimentLabelIndexAccessor.class);
    }
}