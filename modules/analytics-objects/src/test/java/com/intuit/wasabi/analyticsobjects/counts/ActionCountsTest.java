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

import com.intuit.wasabi.analyticsobjects.Event;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

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
    public void setup() {
        testEvent = Event.Name.valueOf("Test Event");
        eventCount = 500000;
        uniqueUserCount = 500000;
        actionCounter = new ActionCounts.Builder().withActionName(testEvent)
                .withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
    }

    @Test
    public void testBuilder() {
        assertThat(actionCounter.getActionName(), equalTo(testEvent));
        assertThat(actionCounter.getEventCount(), equalTo(eventCount));
        assertThat(actionCounter.getUniqueUserCount(), equalTo(uniqueUserCount));
        assertThat(actionCounter.toString(), containsString("eventCount=500000"));
        assertThat(actionCounter.toString(), containsString("uniqueUserCount=500000"));
    }

    @Test
    public void testHashCode() {
        ActionCounts actionCountsOther = actionCounter.clone();
        assertThat(actionCountsOther.hashCode(), equalTo(actionCounter.hashCode()));
        actionCountsOther.setEventCount(42l);
        assertThat(actionCountsOther.hashCode(), not(actionCounter.hashCode()));
    }

    @Test
    public void testBuildWithCountObject() {
        counter = new Counts.Builder().withEventCount(eventCount).withUniqueUserCount(uniqueUserCount).build();
        actionCounter = new ActionCounts.Builder().withCountObject(counter).build();
        actionCounter.setActionName(testEvent);

        assertThat(actionCounter.getActionName(), equalTo(testEvent));
        assertThat(actionCounter.getEventCount(), equalTo(eventCount));
        assertThat(actionCounter.getUniqueUserCount(), equalTo(uniqueUserCount));
    }

    @Test
    public void testBuildWithNew() {
        ActionCounts newActionCounter = new ActionCounts(testEvent, eventCount, uniqueUserCount);

        assertThat(newActionCounter.getActionName(), equalTo(testEvent));
        assertThat(newActionCounter.getEventCount(), equalTo(eventCount));
        assertThat(newActionCounter.getUniqueUserCount(), equalTo(uniqueUserCount));
        assertThat(actionCounter, equalTo(newActionCounter));

        ActionCounts actionCounterClone = actionCounter.clone();

        assertThat(actionCounterClone, equalTo(actionCounter));
        assertThat(actionCounterClone.getActionName(), equalTo(actionCounter.getActionName()));
        assertThat(actionCounterClone.getEventCount(), equalTo(actionCounter.getEventCount()));
        assertThat(actionCounterClone.getUniqueUserCount(), equalTo(actionCounter.getUniqueUserCount()));
    }

}
