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
package com.intuit.wasabi.experiment;

import com.google.inject.AbstractModule;
import com.intuit.wasabi.eventlog.EventLogModule;
import com.intuit.wasabi.experiment.impl.BucketsImpl;
import com.intuit.wasabi.experiment.impl.ExperimentsImpl;
import com.intuit.wasabi.experiment.impl.FavoritesImpl;
import com.intuit.wasabi.experiment.impl.MutexImpl;
import com.intuit.wasabi.experiment.impl.PagesImpl;
import com.intuit.wasabi.experiment.impl.PrioritiesImpl;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import org.slf4j.Logger;

import static com.google.inject.Scopes.SINGLETON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Guice module for providing experiments related objects
 */
public class ExperimentsModule extends AbstractModule {

    private static final Logger LOGGER = getLogger(ExperimentsModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", ExperimentsModule.class.getSimpleName());

        install(new EventLogModule());
        install(new CassandraRepositoryModule());

        bind(Experiments.class).to(ExperimentsImpl.class).in(SINGLETON);
        bind(Buckets.class).to(BucketsImpl.class).in(SINGLETON);
        bind(Mutex.class).to(MutexImpl.class).in(SINGLETON);
        bind(Pages.class).to(PagesImpl.class).in(SINGLETON);
        bind(Priorities.class).to(PrioritiesImpl.class).in(SINGLETON);
        bind(Favorites.class).to(FavoritesImpl.class).in(SINGLETON);
        bind(ExperimentValidator.class).in(SINGLETON);

        LOGGER.debug("installed module: {}", ExperimentsModule.class.getSimpleName());
    }
}
