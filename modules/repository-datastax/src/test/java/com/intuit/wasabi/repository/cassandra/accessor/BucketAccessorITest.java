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
import com.intuit.wasabi.experimentobjects.Bucket.State;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.pojo.Bucket;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * These tests are just make sure that the queries work
 */
public class BucketAccessorITest extends IntegrationTestBase {
    static BucketAccessor accessor;
    static UUID experimentId;

    @BeforeClass
    public static void setup() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = manager.createAccessor(BucketAccessor.class);
        experimentId = UUID.randomUUID();
    }

    @Test
    public void testCreateAndDeleteOneByExperimentId() {
        Result<Bucket> buckets = accessor.getBucketByExperimentId(experimentId);
        List<Bucket> list = buckets.all();
        int count = list.size();
        assertEquals("Count should be eq", 0, count);

        accessor.insert(experimentId, "l1", "d1", 1.0d, true, "p1", State.OPEN.name());

        buckets = accessor.getBucketByExperimentId(experimentId);
        list = buckets.all();
        count = list.size();
        assertEquals("Count should be eq", 1, count);

        accessor.deleteByExperimentId(experimentId);

        buckets = accessor.getBucketByExperimentId(experimentId);
        list = buckets.all();
        count = list.size();
        assertEquals("Count should be eq", 0, count);
    }

    @Test
    public void testCreateAndDeleteTwoByExperimentIds() {
        List<UUID> experimentIds = new ArrayList<>();
        experimentIds.add(experimentId);
        Result<Bucket> buckets = accessor.getBucketByExperimentIds(experimentIds);
        List<Bucket> list = buckets.all();
        int count = list.size();
        assertEquals("Count should be eq", 0, count);

        accessor.insert(experimentId, "l1", "d1", 1.0d, true, "p1", State.OPEN.name());

        buckets = accessor.getBucketByExperimentIds(experimentIds);
        list = buckets.all();
        count = list.size();
        assertEquals("Count should be eq", 1, count);

        UUID experimentId2 = UUID.randomUUID();
        accessor.insert(experimentId2, "l1", "d1", 1.0d, true, "p1", State.OPEN.name());

        experimentIds.add(experimentId2);

        buckets = accessor.getBucketByExperimentIds(experimentIds);
        list = buckets.all();
        count = list.size();
        assertEquals("Count should be eq", 2, count);

        accessor.deleteByExperimentId(experimentId);

        buckets = accessor.getBucketByExperimentIds(experimentIds);
        list = buckets.all();
        count = list.size();
        assertEquals("Count should be eq", 1, count);

        accessor.deleteByExperimentId(experimentId2);

        buckets = accessor.getBucketByExperimentIds(experimentIds);
        list = buckets.all();
        count = list.size();
        assertEquals("Count should be eq", 0, count);
    }
}