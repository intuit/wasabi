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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.pojo.Experiment;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class StagingAccessorITest extends IntegrationTestBase {
//    static StagingAccessor accessor;
//
//    @BeforeClass
//    public static void setup() {
//        IntegrationTestBase.setup();
//        if (accessor != null) return;
//        accessor = manager.createAccessor(StagingAccessor.class);
//    }
//
//
//    @Test
//    public void testBatchSelectBy(){
//
//        int batchSize = 20;
//        String message = "message";
//        int i;
//        for(i=0 ; i<100 ; i++){
//            accessor.insertBy("test","test", message+i);
//        }
//
//        ResultSet resultSet = accessor.batchSelectBy(batchSize);
//        assertEquals(batchSize, resultSet.all().size());
//    }
//
//    @Test
//    public void testDeleteBy(){
//        accessor.deleteBy(UUID.fromString("0f698e10-f45a-11e8-baac-779486c46c22"));
//    }
}