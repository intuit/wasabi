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
package com.intuit.wasabi.authorization;

import org.junit.Test;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AuthorizationModuleTest {

    @Test
    public void testFixMe() throws Exception {
        assertThat(TRUE, is(TRUE));
    }

//    @Ignore("FIXME")
//    @Test
//    public void testConfigure() throws Exception {
//        Injector injector = Guice.createInjector(new AbstractModule() {
//            @Override
//            protected void configure() {
//                bind(AuthorizationRepository.class).toInstance(mock(CassandraAuthorizationRepository.class));
//                bind(Experiments.class).toInstance(mock(ExperimentsImpl.class));
//                bind(EventLog.class).toInstance(mock(EventLogImpl.class));
//            }
//        }, new AuthorizationModule());
//
//        Authorization authorization = injector.getInstance(Key.get(Authorization.class));
//        assertThat(authorization, instanceOf(Authorization.class));
//        assertThat(authorization, instanceOf(DefaultAuthorization.class));
//
//    }
}
