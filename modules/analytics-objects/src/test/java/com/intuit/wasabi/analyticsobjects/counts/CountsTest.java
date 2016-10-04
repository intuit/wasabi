/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.analyticsobjects.counts;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class tests the {@link Counts}.
 */
public class CountsTest {

    private Counts counter;
    private long eventCount;
    private long uniqueUserCount;

    @Before
    public void setup(){
        eventCount = 500000;
        uniqueUserCount  = 500000;
        counter = new Counts.Builder().withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
    }

    @Test
    public void testBuilder(){
        assertEquals(counter.getEventCount(), eventCount);
        assertEquals(counter.getUniqueUserCount(), uniqueUserCount);

        String counterString = counter.toString();
        assertTrue(counterString.contains("eventCount=500000"));
        assertTrue(counterString.contains("uniqueUserCount=500000"));
    }
    @Test
    public void testCloneAndHashCode(){
        Counts countClone = counter.clone();
        assertEquals(counter.getUniqueUserCount(), countClone.getUniqueUserCount());
        assertEquals(counter.getEventCount(), countClone.getEventCount());

        assertEquals(counter.hashCode(), countClone.hashCode());
        countClone.setEventCount(42);
        assertNotEquals(counter.hashCode(), countClone.hashCode());
    }


    @Test
    public void testAddEventCount(){
        long addEventValue = 100000;
        counter.addEventCount(addEventValue);
        assertEquals(counter.getEventCount(), eventCount + addEventValue);
    }

    @Test
    public void testAddUniqueUserCount(){
        long addUserValue = 100000;
        counter.addUniqueUserCount(addUserValue);
        assertEquals(counter.getUniqueUserCount(), uniqueUserCount + addUserValue);
    }

    @Test
    public void testAddCount(){
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
