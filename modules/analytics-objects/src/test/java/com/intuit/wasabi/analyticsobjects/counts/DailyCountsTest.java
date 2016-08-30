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
import com.intuit.wasabi.experimentobjects.Bucket;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class DailyCountsTest {
    String date;
    ExperimentCounts perDay;
    ExperimentCounts cumulative;

    DailyCounts counter;

    @Before
    public void setup(){
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
    public void testBuilder(){
        assertEquals(counter.getDate(), date);
        assertEquals(counter.getCumulative(), cumulative);
        assertEquals(counter.getPerDay(), perDay);
    }

    @Test
    public void testClone(){
        DailyCounts clonedCounter = counter.clone();
        assertEquals(clonedCounter.getDate(), date);

        assertEquals(clonedCounter.getDate(), date);
        assertEquals(clonedCounter.getCumulative(), cumulative);
        assertEquals(clonedCounter.getPerDay(), perDay);

        assertTrue(counter.equals(clonedCounter));
    }

}
