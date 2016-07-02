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

import com.intuit.wasabi.analyticsobjects.Event;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ActionCountsTest {
    Event.Name testEvent;
    long eventCount;
    long uniqueUserCount;
    Counts counter;
    ActionCounts actionCounter;

    @Before
    public void setup(){
        testEvent = Event.Name.valueOf("Test Event");
        eventCount = 500000;
        uniqueUserCount  = 500000;
        actionCounter = new ActionCounts.Builder().withActionName(testEvent)
                        .withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
    }

    @Test
    public void testBuilder(){
        assertEquals(actionCounter.getActionName(), testEvent);
        assertEquals(actionCounter.getEventCount(), eventCount);
        assertEquals(actionCounter.getUniqueUserCount(), uniqueUserCount);
        System.out.println(actionCounter.toString());
    }

    @Test
    public  void testBuildWithCountObject(){
        counter = new Counts.Builder().withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
        actionCounter = new ActionCounts.Builder().withCountObject(counter).build();
        actionCounter.setActionName(testEvent);
        assertEquals(actionCounter.getActionName(), testEvent);
        assertEquals(actionCounter.getEventCount(), eventCount);
        assertEquals(actionCounter.getUniqueUserCount(), uniqueUserCount);
    }

    @Test
    public  void testBuildWithNew(){
        ActionCounts newActionCounter = new ActionCounts(testEvent, eventCount, uniqueUserCount);
        assertEquals(newActionCounter.getActionName(), testEvent);
        assertEquals(newActionCounter.getEventCount(), eventCount);
        assertEquals(newActionCounter.getUniqueUserCount(), uniqueUserCount);
        assertTrue(actionCounter.equals(newActionCounter));
    }

}
