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
package com.intuit.wasabi.repository.cassandra.accessor.audit;

import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.pojo.audit.ExperimentAuditLog;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * These tests are just make sure that the queries work
 */
public class ExperimentAuditLogAccessorITest extends IntegrationTestBase {
    static ExperimentAuditLogAccessor accessor;
    static UUID experimentId;
    static Date date;

    @BeforeClass
    public static void setupClass() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = manager.createAccessor(ExperimentAuditLogAccessor.class);
        experimentId = UUID.randomUUID();
        date = new Date();
    }

    @Before
    public void setupTest() {
        session.execute("truncate wasabi_experiments.experiment_audit_log");
    }

    @Test
    public void testCreateAndDeleteBucketAuditLog() {
        Result<ExperimentAuditLog> result = accessor.selectBy(experimentId);
        assertEquals("Value should be eq", 0, result.all().size());

        accessor.insertBy(experimentId, date, "a1", "v1", "v2");

        result = accessor.selectBy(experimentId);
        List<ExperimentAuditLog> values = result.all();
        assertEquals("Value should be eq", 1, values.size());
        assertEquals("Value should be eq", "a1", values.get(0).getAttributeName());
        assertEquals("Value should be eq", "v1", values.get(0).getOldValue());
        assertEquals("Value should be eq", "v2", values.get(0).getNewValue());
        assertEquals("Value should be eq", date, values.get(0).getModified());
        assertEquals("Value should be eq", experimentId, values.get(0).getExperimentId());

        accessor.deleteBy(experimentId);

        result = accessor.selectBy(experimentId);
        assertEquals("Value should be eq", 0, result.all().size());
    }

}