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
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentState;
import com.intuit.wasabi.repository.cassandra.accessor.index.StateExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.index.StateExperimentIndex;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class StateExperimentIndexAccessorITest extends IntegrationTestBase {
    static StateExperimentIndexAccessor accessor;
    UUID experimentId1 = UUID.randomUUID();
    UUID experimentId2 = UUID.randomUUID();

    @BeforeClass
    public static void setup() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = manager.createAccessor(StateExperimentIndexAccessor.class);
        session.execute("truncate wasabi_experiments.state_experiment_index");

    }

    @Before
    public void tearDown() {
        session.execute("truncate wasabi_experiments.state_experiment_index");
    }

    @Test
    public void testCreateAndGetDeletedOne() {
        Result<StateExperimentIndex> result =
                accessor.selectByKey(ExperimentState.DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        result =
                accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        session.execute(accessor.insert(ExperimentState.DELETED.name(), experimentId1, ByteBuffer.wrap("".getBytes())));

        result =
                accessor.selectByKey(ExperimentState.DELETED.name());
        List<StateExperimentIndex> values = result.all();
        assertEquals("Values should be eq", 1, values.size());
        assertEquals("Values should be eq", experimentId1, values.get(0).getExperimentId());
        assertEquals("Values should be eq", ExperimentState.DELETED.name(), values.get(0).getIndexKey());

        result =
                accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());
    }

    @Test
    public void testCreateAndGetDeletedByExperimentIdOne() {
        Result<StateExperimentIndex> result =
                accessor.selectByKey(ExperimentState.DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        result =
                accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        session.execute(accessor.insert(ExperimentState.DELETED.name(), experimentId1, ByteBuffer.wrap("".getBytes())));

        result =
                accessor.selectByKeyAndExperimentId(ExperimentState.DELETED.name(), experimentId1);
        List<StateExperimentIndex> values = result.all();
        assertEquals("Values should be eq", 1, values.size());
        assertEquals("Values should be eq", experimentId1, values.get(0).getExperimentId());
        assertEquals("Values should be eq", ExperimentState.DELETED.name(), values.get(0).getIndexKey());

        result =
                accessor.selectByKeyAndExperimentId(ExperimentState.DELETED.name(), experimentId2);
        values = result.all();
        assertEquals("Values should be eq", 0, values.size());
    }

    @Test
    public void testCreateAndDeletOne() {
        Result<StateExperimentIndex> result =
                accessor.selectByKey(ExperimentState.DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        result = accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        session.execute(accessor.insert(ExperimentState.DELETED.name(), experimentId1, ByteBuffer.wrap("".getBytes())));
        accessor.deleteBy(ExperimentState.DELETED.name(), experimentId1);
        result = accessor.selectByKey(ExperimentState.DELETED.name());
        List<StateExperimentIndex> values = result.all();
        assertEquals("Values should be eq", 1, values.size());
        result = accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());
    }

    @Test
    public void testCreateAndGetDeletedTwo() {
        Result<StateExperimentIndex> result =
                accessor.selectByKey(ExperimentState.DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        result =
                accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        session.execute(accessor.insert(ExperimentState.DELETED.name(), experimentId1, ByteBuffer.wrap("".getBytes())));
        session.execute(accessor.insert(ExperimentState.DELETED.name(), experimentId2, ByteBuffer.wrap("".getBytes())));

        result =
                accessor.selectByKey(ExperimentState.DELETED.name());
        List<StateExperimentIndex> values = result.all();
        Collections.sort(values, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                StateExperimentIndex sei = (StateExperimentIndex) o1;
                if (sei.getExperimentId().equals(experimentId1))
                    return -1;
                else
                    return 1;
            }

        });
        assertEquals("Values should be eq", 2, values.size());
        assertEquals("Values should be eq", experimentId1, values.get(0).getExperimentId());
        assertEquals("Values should be eq", ExperimentState.DELETED.name(), values.get(0).getIndexKey());
        assertEquals("Values should be eq", experimentId2, values.get(1).getExperimentId());
        assertEquals("Values should be eq", ExperimentState.DELETED.name(), values.get(1).getIndexKey());

        result =
                accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());
    }

    @Test
    public void testCreateAndGetDeletedAndNot() {
        Result<StateExperimentIndex> result =
                accessor.selectByKey(ExperimentState.DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        result =
                accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        assertEquals("Values should be eq", 0, result.all().size());

        session.execute(accessor.insert(ExperimentState.DELETED.name(), experimentId1, ByteBuffer.wrap("".getBytes())));
        session.execute(accessor.insert(ExperimentState.NOT_DELETED.name(), experimentId2, ByteBuffer.wrap("".getBytes())));

        result =
                accessor.selectByKey(ExperimentState.DELETED.name());
        List<StateExperimentIndex> values = result.all();
        assertEquals("Values should be eq", 1, values.size());
        assertEquals("Values should be eq", experimentId1, values.get(0).getExperimentId());


        result =
                accessor.selectByKey(ExperimentState.NOT_DELETED.name());
        values = result.all();
        assertEquals("Values should be eq", 1, values.size());
        assertEquals("Values should be eq", experimentId2, values.get(0).getExperimentId());

    }
}