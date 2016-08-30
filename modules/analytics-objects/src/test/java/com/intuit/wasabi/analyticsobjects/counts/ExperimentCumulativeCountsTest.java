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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExperimentCumulativeCountsTest {
    List<DailyCounts> days;
    ExperimentCumulativeCounts counter;

    @Before
    public void setup(){
        days = new ArrayList<DailyCounts>();
        counter = new ExperimentCumulativeCounts.Builder().withDays(days).build();
    }

    @Test
    public void testBuilder(){
        assertEquals(counter.getDays(), days);
    }

    @Test
    public void testAddSetDays(){
        DailyCounts day = new DailyCounts.Builder().build();
        List<DailyCounts> otherDays = new ArrayList<DailyCounts>();
        otherDays.add(day);
        counter.addDays(day);
        assertEquals(counter.getDays(), otherDays);
        counter.setDays(days);
        assertEquals(counter.getDays(), days);
    }

    @Test
    public void testClone(){
        ExperimentCumulativeCounts clonedCounter = counter.clone();
        assertEquals(clonedCounter.getDays(), days);
    }

    @Test
    public void testCloneEquals(){
        ExperimentCumulativeCounts clonedCounter = counter.clone();
        assertEquals(clonedCounter, counter);
    }

    @Test
    public void testEquals(){
        ExperimentCumulativeCounts counter1 = new ExperimentCumulativeCounts.Builder().withDays(days).build();
        ExperimentCumulativeCounts counter2 = new ExperimentCumulativeCounts.Builder().withDays(days).build();
        
        assertEquals(counter1, counter2);
    }

    @Test
    public void testNotEquals(){
        ExperimentCumulativeCounts counter1 = new ExperimentCumulativeCounts.Builder().withDays(days).build();
        ArrayList<DailyCounts> days2 = new ArrayList<DailyCounts>();
        days.add(new DailyCounts.Builder().setDate("2016-03-01 12:12:12z").build());
        ExperimentCumulativeCounts counter2 = new ExperimentCumulativeCounts.Builder().withDays(days2).build();
        
        assertFalse(counter1.equals(counter2));
    }
}
