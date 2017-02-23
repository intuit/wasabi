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

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.database.DatabaseModule;
import com.intuit.wasabi.eventlog.EventLogModule;
import com.intuit.wasabi.repository.database.DatabaseExperimentRepositoryModule;
import com.intuit.wasabi.userdirectory.UserDirectoryModule;

/**
 * A utility class for creating session/etc once
 */
public class IntegrationTestBase {
    protected static Session session;
    protected static MappingManager manager;
    protected static Injector injector;

    public static void setup() {
        if (injector != null)
            return;
        injector = Guice.createInjector(
                new UserDirectoryModule(),
                new CassandraRepositoryModule(),
                new DatabaseExperimentRepositoryModule(),
                new DatabaseModule(),
                new EventLogModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
    }
}
