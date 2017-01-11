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
import com.intuit.wasabi.experimentobjects.Bucket;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * This class tests the {@link DailyCounts}.
 */
public class DailyCountsTest {

    private String date;
    private ExperimentCounts perDay;
    private ExperimentCounts cumulative;

    private DailyCounts counter;

    @Before
    public void setup() {
        Counts impressionCounts = new Counts.Builder().withEventCount(100).withUniqueUserCount(100).build();
        Counts jointActionCounts = new Counts.Builder().withEventCount(200).withUniqueUserCount(200).build();
        HashMap<Event.Name, ActionCounts> actionCounts = new HashMap<Event.Name, ActionCounts>();
        HashMap<Bucket.Label, BucketCounts> buckets = new HashMap<Bucket.Label, BucketCounts>();

        perDay = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts).withActionCounts(actionCounts)
                .withBuckets(buckets).build();
        cumulative = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts).withActionCounts(actionCounts)
                .withBuckets(buckets).build();
        date = new DateTime().toString();

        counter = new DailyCounts.Builder().setDate(date).withCumulative(cumulative)
                .withPerDay(perDay).build();
    }

    @Test
    public void testBuilder() {
        assertThat(counter.getDate(), equalTo(date));
        assertThat(counter.getCumulative(), equalTo(cumulative));
        assertThat(counter.getPerDay(), equalTo(perDay));

        String counterString = counter.toString();
        assertThat(counterString, containsString(date));
        assertThat(counterString, containsString(perDay.toString()));
        assertThat(counterString, containsString(cumulative.toString()));
    }

    @Test
    public void testClone() {
        DailyCounts clonedCounter = counter.clone();

        assertThat(clonedCounter.getDate(), equalTo(date));
        assertThat(clonedCounter.getCumulative(), equalTo(cumulative));
        assertThat(clonedCounter.getPerDay(), equalTo(perDay));
        assertThat(counter, equalTo(clonedCounter));
        assertThat(clonedCounter.hashCode(), equalTo(counter.hashCode()));
    }

}
