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
package com.intuit.wasabi.analytics;

import com.google.inject.AbstractModule;
import com.intuit.wasabi.analytics.impl.AnalysisToolsImpl;
import com.intuit.wasabi.analytics.impl.AnalyticsImpl;
import com.intuit.wasabi.analytics.impl.ExperimentDetailsImpl;
import com.intuit.wasabi.experiment.ExperimentsModule;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.database.DatabaseAnalyticsModule;
import org.slf4j.Logger;

import static com.google.inject.Scopes.SINGLETON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The guice module for the Analytics package.
 */
public class AnalyticsModule extends AbstractModule {

    private static final Logger LOGGER = getLogger(AnalyticsModule.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", AnalyticsModule.class.getSimpleName());

        install(new ExperimentsModule());
        install(new CassandraRepositoryModule());
        install(new DatabaseAnalyticsModule());

        bind(Analytics.class).to(AnalyticsImpl.class).in(SINGLETON);
        bind(AnalysisTools.class).to(AnalysisToolsImpl.class).in(SINGLETON);
        bind(ExperimentDetails.class).to(ExperimentDetailsImpl.class).in(SINGLETON);

        LOGGER.debug("installed module: {}", AnalyticsModule.class.getSimpleName());
    }
}
