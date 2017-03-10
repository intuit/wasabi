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
import com.intuit.wasabi.repository.cassandra.pojo.Exclusion;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class MutexAccessorITest extends IntegrationTestBase {
    static ExclusionAccessor accessor;
    static String applicationName = "MyTestApplication_" + System.currentTimeMillis();

    @BeforeClass
    public static void setup() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = manager.createAccessor(ExclusionAccessor.class);

    }

    @Test
    public void testCreateOneAndGetExcusion() {
        UUID base = UUID.randomUUID();
        UUID pair = UUID.randomUUID();
        Result<Exclusion> exclusions = accessor.getExclusions(base);

        assertEquals("Size should be same", 0, exclusions.all().size());
        session.execute(accessor.createExclusion(base, pair));

        exclusions = accessor.getExclusions(base);

        List<Exclusion> exclusionsList = exclusions.all();

        assertEquals("Size should be same", 1, exclusionsList.size());
        assertEquals("ids should be same", exclusionsList.get(0).getBase(), base);
        assertEquals("ids should be same", exclusionsList.get(0).getPair(), pair);
    }

    @Test
    public void testCreateOneAndDeleteExcusion() {
        UUID base = UUID.randomUUID();
        UUID pair = UUID.randomUUID();

        session.execute(accessor.createExclusion(base, pair));

        Result<Exclusion> exclusions = accessor.getExclusions(base);
        List<Exclusion> exclusionsList = exclusions.all();
        assertEquals("Size should be same", 1, exclusionsList.size());
        assertEquals("ids should be same", exclusionsList.get(0).getBase(), base);
        assertEquals("ids should be same", exclusionsList.get(0).getPair(), pair);

        session.execute(accessor.deleteExclusion(base, pair));

        exclusions = accessor.getExclusions(base);
        exclusionsList = exclusions.all();
        assertEquals("Size should be same", 0, exclusionsList.size());
    }
}