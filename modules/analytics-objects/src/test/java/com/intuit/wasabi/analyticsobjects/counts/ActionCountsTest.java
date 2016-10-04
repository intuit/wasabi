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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class tests the {@link ActionCounts}.
 */
public class ActionCountsTest {

    private Event.Name testEvent;
    private long eventCount;
    private long uniqueUserCount;
    private Counts counter;
    private ActionCounts actionCounter;

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
        assertTrue(actionCounter.toString().contains("eventCount=500000"));
        assertTrue(actionCounter.toString().contains("uniqueUserCount=500000"));
    }

    @Test
    public void testHashCode(){
        ActionCounts actionCountsOther = actionCounter.clone();
        assertEquals(actionCountsOther.hashCode(), actionCounter.hashCode());
        actionCountsOther.setEventCount(42l);
        assertNotEquals(actionCountsOther.hashCode(), actionCounter.hashCode());
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

        ActionCounts actionCounterClone = actionCounter.clone();
        assertTrue(actionCounterClone.equals(actionCounter));
        assertEquals(actionCounterClone.getActionName(), actionCounter.getActionName());
        assertEquals(actionCounterClone.getEventCount(), actionCounter.getEventCount());
        assertEquals(actionCounterClone.getUniqueUserCount(), actionCounter.getUniqueUserCount());
    }

}
