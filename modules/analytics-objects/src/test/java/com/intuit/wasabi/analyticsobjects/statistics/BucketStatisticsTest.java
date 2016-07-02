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
package com.intuit.wasabi.analyticsobjects.statistics;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BucketStatisticsTest {
    Bucket.Label label;
    Map<Event.Name, ActionCounts> actionCountsMap;
    Map<Event.Name, ActionRate> actionRateMap;
    Counts jointActionCounts;
    Counts impressionCounts;
    Estimate jointActionRate;
    Map<Bucket.Label, BucketComparison> bucketComparisons;
    BucketStatistics bucketStatistics;

    @Before
    public void setup(){
        label = Bucket.Label.valueOf("TestWinner");
        actionCountsMap = new HashMap<Event.Name, ActionCounts>();
        actionRateMap = new HashMap<Event.Name, ActionRate>();
        jointActionCounts = new Counts();
        impressionCounts = new Counts();
        jointActionRate = new Estimate();
        bucketComparisons = new HashMap<Bucket.Label, BucketComparison>();
        bucketStatistics = new BucketStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).withBucketComparisons(bucketComparisons).build();
    }

    @Test
    public void testTwoObjectsEqual(){
        BucketStatistics bucketStatistics2 = new BucketStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).withBucketComparisons(bucketComparisons).build();
        assertEquals(bucketStatistics, bucketStatistics2);
    }

    @Test
    public void testTwoObjectsNotEqual(){
        Label label2 = Bucket.Label.valueOf("TestWinner2");

        BucketStatistics bucketStatistics2 = new BucketStatistics.Builder().withLabel(label2)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).withBucketComparisons(bucketComparisons).build();
        assertFalse(bucketStatistics.equals(bucketStatistics2));
    }

    @Test
    public void testCloneObjectsEqual(){
        BucketStatistics bucketStatistics2 = new BucketStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).withBucketComparisons(bucketComparisons).build();
        assertEquals(bucketStatistics, bucketStatistics2.clone());
        assertEquals(bucketStatistics.clone(), bucketStatistics2);
    }

    @Test
    public void testBuilder(){
        assertEquals(bucketStatistics.getLabel(), label);
        assertEquals(bucketStatistics.getActionCounts(), actionCountsMap);
        assertEquals(bucketStatistics.getActionRates(), actionRateMap);
        assertEquals(bucketStatistics.getJointActionCounts(), jointActionCounts);
        assertEquals(bucketStatistics.getImpressionCounts(), impressionCounts);
        assertEquals(bucketStatistics.getJointActionRate(), jointActionRate);

        assertTrue(bucketStatistics.equals(bucketStatistics));
        assertFalse(bucketStatistics.equals(null));
        assertFalse(bucketStatistics.equals(bucketComparisons));
    }

    @Test
    public void testSettersAndGetters(){
        bucketStatistics.setLabel(null);
        assertEquals(bucketStatistics.getLabel(), null);

        bucketStatistics.setBucketComparisons(null);
        assertEquals(bucketStatistics.getBucketComparisons(), null);

        bucketStatistics.addToBucketComparisons(label, null);
        assertNotNull(bucketStatistics.getBucketComparisons());
    }
}
