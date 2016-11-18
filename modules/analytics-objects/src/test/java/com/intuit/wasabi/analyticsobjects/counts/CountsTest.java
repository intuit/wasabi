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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * This class tests the {@link Counts}.
 */
public class CountsTest {

    private Counts counter;
    private long eventCount;
    private long uniqueUserCount;

    @Before
    public void setup() {
        eventCount = 500000;
        uniqueUserCount = 500000;
        counter = new Counts.Builder().withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
    }

    @Test
    public void testBuilder() {
        assertThat(counter.getEventCount(), equalTo(eventCount));
        assertThat(counter.getUniqueUserCount(), equalTo(uniqueUserCount));

        String counterString = counter.toString();
        assertThat(counterString, containsString("eventCount=500000"));
        assertThat(counterString, containsString("uniqueUserCount=500000"));
    }

    @Test
    public void testCloneAndHashCode() {
        Counts countClone = counter.clone();
        assertThat(counter.getUniqueUserCount(), equalTo(countClone.getUniqueUserCount()));
        assertThat(counter.getEventCount(), equalTo(countClone.getEventCount()));

        assertThat(counter.hashCode(), equalTo(countClone.hashCode()));
        countClone.setEventCount(42);
        assertThat(counter.hashCode(), not(equalTo(countClone.hashCode())));
    }


    @Test
    public void testAddEventCount() {
        long addEventValue = 100000;
        counter.addEventCount(addEventValue);
        assertThat(counter.getEventCount(), equalTo(eventCount + addEventValue));
    }

    @Test
    public void testAddUniqueUserCount() {
        long addUserValue = 100000;
        counter.addUniqueUserCount(addUserValue);
        assertThat(counter.getUniqueUserCount(), equalTo(uniqueUserCount + addUserValue));
    }

    @Test
    public void testAddCount() {
        Counts addCounter = new Counts.Builder().withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
        assertThat(counter, equalTo(addCounter));
        long addEventValue = 100000;
        long addUserValue = 100000;
        addCounter = new Counts.Builder().withEventCount(addEventValue).withUniqueUserCount(addUserValue).build();
        counter.addCount(addCounter);
        assertThat(counter.getEventCount(), equalTo(eventCount + addEventValue));
        assertThat(counter.getUniqueUserCount(), equalTo(uniqueUserCount + addUserValue));
    }

}
