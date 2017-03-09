/*******************************************************************************
 * Copyright 2017 Intuit
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
import com.intuit.wasabi.repository.cassandra.pojo.Experiment;
import com.intuit.wasabi.repository.cassandra.pojo.ExperimentAssignmentType;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ExperimentAssignmentTypeAccessorITest extends IntegrationTestBase {
    static ExperimentAssignmentTypeAccessor accessor;
    static UUID experimentId1 = UUID.randomUUID();
    static Date date1 = new Date();
    static Date date2 = new Date();

    @BeforeClass
    public static void setup() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = manager.createAccessor(ExperimentAssignmentTypeAccessor.class);
        date2.setDate(date1.getDate() + 1);
    }

    @Test
    public void insertOneExperimentAssignmentType() {
        accessor.insert(experimentId1,
        		date1, true);
        Result<ExperimentAssignmentType> experimentAssignmentType = 
        		accessor.selectBy(experimentId1, date1, date2);
        
        List<ExperimentAssignmentType> experimentResult = experimentAssignmentType.all();
        assertEquals("size should be same", 1, experimentResult.size());
        ExperimentAssignmentType expAssignmentType = experimentResult.get(0);
        assertEquals("Expid should be equal", experimentId1, expAssignmentType.getExperimentId());
        assertEquals("timestamp should be equal", date1, expAssignmentType.getTimestamp());
        assertEquals("bucket should be equal", true, expAssignmentType.isBucketAssignment());
        
    }

}