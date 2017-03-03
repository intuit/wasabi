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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.pojo.Application;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class PrioritiesAccessorITest extends IntegrationTestBase {
    static PrioritiesAccessor accessor;
    static String applicationName = "MyTestApplication_" + System.currentTimeMillis();

    @BeforeClass
    public static void setup() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = manager.createAccessor(PrioritiesAccessor.class);
        accessor.deletePriorities(applicationName);

        Result<Application> result = accessor.getPriorities(applicationName);
        assertEquals("Size should be same", 0, result.all().size());
    }

    @Test
    public void testCreateAndGetPrioritiesWithOneUUIDAndListLength() {
        List<UUID> ids = new ArrayList<>();
        ids.add(UUID.randomUUID());
        accessor.updatePriorities(ids, applicationName);

        Result<Application> result = accessor.getPriorities(applicationName);
        List<Application> application = result.all();
        assertEquals("Size should be same", 1, application.size());
        assertEquals("ids length should be same", 1, application.get(0).getPriorities().size());
        assertEquals("ids should be same", ids, application.get(0).getPriorities());
    }

    @Test
    public void testCreateAndGetPrioritiesWithTwoUUIDAndListLength() {
        List<UUID> ids = new ArrayList<>();
        ids.add(UUID.randomUUID());
        ids.add(UUID.randomUUID());
        accessor.updatePriorities(ids, applicationName);

        Result<Application> result = accessor.getPriorities(applicationName);
        List<Application> application = result.all();
        assertEquals("Size should be same", 1, application.size());
        assertEquals("ids length should be same", 2, application.get(0).getPriorities().size());
        assertEquals("ids should be same", ids, application.get(0).getPriorities());
    }

    @Test
    public void testCreateAndGetPrioritiesWithTwoUUIDAndDeleteListLength() {
        List<UUID> ids = new ArrayList<>();
        ids.add(UUID.randomUUID());
        ids.add(UUID.randomUUID());
        accessor.updatePriorities(ids, applicationName);

        Result<Application> result = accessor.getPriorities(applicationName);
        List<Application> application = result.all();
        assertEquals("Size should be same", 1, application.size());
        assertEquals("ids length should be same", 2, application.get(0).getPriorities().size());
        assertEquals("ids should be same", ids, application.get(0).getPriorities());

        accessor.deletePriorities(applicationName);

        result = accessor.getPriorities(applicationName);
        application = result.all();
        assertEquals("Size should be same", 0, application.size());
    }

    @Test
    public void testCreateAndGetPrioritiesWithOneAndOverrideUUIDAndListLength() {
        List<UUID> ids = new ArrayList<>();
        ids.add(UUID.randomUUID());
        accessor.updatePriorities(ids, applicationName);

        Result<Application> result = accessor.getPriorities(applicationName);
        List<Application> application = result.all();
        assertEquals("Size should be same", 1, application.size());
        assertEquals("ids length should be same", 1, application.get(0).getPriorities().size());
        assertEquals("ids should be same", ids, application.get(0).getPriorities());

        // Add 2nd id
        ids.add(UUID.randomUUID());
        accessor.updatePriorities(ids, applicationName);

        result = accessor.getPriorities(applicationName);
        application = result.all();
        assertEquals("Size should be same", 1, application.size());
        assertEquals("ids length should be same", 2, application.get(0).getPriorities().size());
        assertEquals("ids should be same", ids, application.get(0).getPriorities());
    }
}