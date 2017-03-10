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
import com.intuit.wasabi.repository.cassandra.pojo.ApplicationList;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * These tests are just make sure that the queries work
 */
public class ApplicationListAccessorITest extends IntegrationTestBase {
    static ApplicationListAccessor accessor;
    static String applicationName = "MyTestApplication_" + System.currentTimeMillis();

    @BeforeClass
    public static void setup() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = manager.createAccessor(ApplicationListAccessor.class);
    }

    @Test
    public void testCreateAndDelete() {
        Result<ApplicationList> appNames = accessor.getUniqueAppName();
        List<ApplicationList> listOfAppNames = appNames.all();
        int count = listOfAppNames.size();

        accessor.insert(applicationName);
        appNames = accessor.getUniqueAppName();
        listOfAppNames = appNames.all();
        assertEquals("Counnt should be equal", count + 1, listOfAppNames.size());

        accessor.deleteBy(applicationName);
        appNames = accessor.getUniqueAppName();
        listOfAppNames = appNames.all();
        assertEquals("Counnt should be equal", count, listOfAppNames.size());

    }

}