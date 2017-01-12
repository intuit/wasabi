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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * This class tests the {@link ExperimentCumulativeCounts}.
 */
public class ExperimentCumulativeCountsTest {

    private List<DailyCounts> days;
    private ExperimentCumulativeCounts counter;

    @Before
    public void setup() {
        days = new ArrayList<>();
        counter = new ExperimentCumulativeCounts.Builder().withDays(days).build();
    }

    @Test
    public void testBuilder() {
        assertThat(counter.getDays(), equalTo(days));
        assertThat(counter.toString(), containsString(days.toString()));

        ExperimentCumulativeCounts countClone = counter.clone();
        assertThat(counter.hashCode(), is(countClone.hashCode()));
        countClone.addDays(new DailyCounts.Builder().build());
        assertThat(counter.hashCode(), not(countClone.hashCode()));
    }

    @Test
    public void testAddSetDays() {
        DailyCounts day = new DailyCounts.Builder().build();
        List<DailyCounts> otherDays = new ArrayList<DailyCounts>();
        otherDays.add(day);
        counter.addDays(day);
        assertThat(counter.getDays(), equalTo(otherDays));
        counter.setDays(days);
        assertThat(counter.getDays(), equalTo(days));
    }

    @Test
    public void testClone() {
        ExperimentCumulativeCounts clonedCounter = counter.clone();
        assertThat(clonedCounter.getDays(), equalTo(days));
    }

    @Test
    public void testCloneEquals() {
        ExperimentCumulativeCounts clonedCounter = counter.clone();
        assertThat(clonedCounter, equalTo(counter));
    }

    @Test
    public void testEquals() {
        ExperimentCumulativeCounts counter1 = new ExperimentCumulativeCounts.Builder().withDays(days).build();
        ExperimentCumulativeCounts counter2 = new ExperimentCumulativeCounts.Builder().withDays(days).build();

        assertThat(counter1, equalTo(counter2));
    }

    @Test
    public void testNotEquals() {
        ExperimentCumulativeCounts counter1 = new ExperimentCumulativeCounts.Builder().withDays(days).build();
        ArrayList<DailyCounts> days2 = new ArrayList<>();
        days.add(new DailyCounts.Builder().setDate("2016-03-01 12:12:12z").build());
        ExperimentCumulativeCounts counter2 = new ExperimentCumulativeCounts.Builder().withDays(days2).build();

        assertThat(counter1, not(equalTo(counter2)));
    }
}
