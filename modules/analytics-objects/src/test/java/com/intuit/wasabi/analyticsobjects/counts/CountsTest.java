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
package com.intuit.wasabi.analyticsobjects.counts;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CountsTest {
    Counts counter;
    long eventCount;
    long uniqueUserCount;

    @Before
    public void setup() {
        eventCount = 500000;
        uniqueUserCount = 500000;
        counter = new Counts.Builder().withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
    }

    @Test
    public void testBuilder() {
        assertEquals(counter.getEventCount(), eventCount);
        assertEquals(counter.getUniqueUserCount(), uniqueUserCount);
        assertNotNull(counter.clone());
        assertNotNull(counter.hashCode());
        assertNotNull(counter.toString());
    }

    @Test
    public void testAddEventCount() {
        long addEventValue = 100000;
        counter.addEventCount(addEventValue);
        assertEquals(counter.getEventCount(), eventCount + addEventValue);
    }

    @Test
    public void testAddUniqueUserCount() {
        long addUserValue = 100000;
        counter.addUniqueUserCount(addUserValue);
        assertEquals(counter.getUniqueUserCount(), uniqueUserCount + addUserValue);
    }

    @Test
    public void testAddCount() {
        // Reset to setup value due to previous tests
        counter.setEventCount(eventCount);
        counter.setUniqueUserCount(uniqueUserCount);
        Counts addCounter = new Counts.Builder().withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
        assertTrue(counter.equals(addCounter));
        long addEventValue = 100000;
        long addUserValue = 100000;
        addCounter = new Counts.Builder().withEventCount(addEventValue).withUniqueUserCount(addUserValue).build();
        counter.addCount(addCounter);
        assertEquals(counter.getEventCount(), eventCount + addEventValue);
        assertEquals(counter.getUniqueUserCount(), uniqueUserCount + addUserValue);
    }

}
