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
package com.intuit.wasabi.assignment;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.cassandra.impl.CassandraMutexRepository;
import org.junit.Ignore;

import java.util.concurrent.ThreadPoolExecutor;

import static com.intuit.wasabi.assignment.AssignmentsAnnotations.RULECACHE_THREADPOOL;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created on 3/2/16.
 */
public class AssignmentsModuleTest {

    @Ignore
    public void testConfigure() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MutexRepository.class).toInstance(mock(CassandraMutexRepository.class));
            }
        }, new AssignmentsModule());
        injector.getInstance(Key.get(boolean.class, Names.named("decision.engine.use.proxy")));
        injector.getInstance(Key.get(int.class, Names.named("decision.engine.connection.timeout")));
        injector.getInstance(Key.get(int.class, Names.named("decision.engine.read.timeout")));
        injector.getInstance(Key.get(boolean.class, Names.named("decision.engine.use.connection.pooling")));
        injector.getInstance(Key.get(int.class, Names.named("decision.engine.max.connections.per.host")));
        injector.getInstance(Key.get(int.class, Names.named("assignment.http.proxy.portt")));
        injector.getInstance(Key.get(String.class, Names.named("assignment.http.proxy.host")));

        //RuleCacheEngine
        injector.getInstance(Key.get(ThreadPoolExecutor.class,
                Names.named(RULECACHE_THREADPOOL)));

        assertThat(injector.getInstance(Key.get(Assignments.class)), is(not(nullValue())));
    }
}
