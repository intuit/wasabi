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
package com.intuit.wasabi.database;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.jolbox.bonecp.BoneCPConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatabaseModuleTest {

    @Test
    public void testProviderCP() {
        Injector injector = Guice.createInjector(new DatabaseModule());
        Provider<BoneCPConfig> provider = injector.getProvider(BoneCPConfig.class);

        assertEquals(BoneCPConfig.class, provider.get().getClass());
        assert (provider.get().getJdbcUrl().startsWith("jdbc:mysql"));
        assertEquals("readwrite", provider.get().getUser());
        assertEquals("readwrite", provider.get().getPassword());
        assertEquals(1, provider.get().getPartitionCount());
        assertEquals(10, provider.get().getMinConnectionsPerPartition());
        assertEquals(30, provider.get().getMaxConnectionsPerPartition());
    }

}
