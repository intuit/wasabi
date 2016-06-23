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
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class BucketBasicStatisticsTest {
    Bucket.Label label;
    Map<Event.Name, ActionCounts> actionCountsMap;
    Map<Event.Name, ActionRate> actionRateMap;
    Counts jointActionCounts;
    Counts impressionCounts;
    Estimate jointActionRate;
    BucketBasicStatistics bucketBasicStatistics;

    @Before
    public void setup(){
        label = Bucket.Label.valueOf("TestWinner");
        actionCountsMap = new HashMap<Event.Name, ActionCounts>();
        actionRateMap = new HashMap<Event.Name, ActionRate>();
        jointActionCounts = new Counts();
        impressionCounts = new Counts();
        jointActionRate = new Estimate();
        bucketBasicStatistics = new BucketBasicStatistics.Builder().withLabel(label)
                                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                                .withJointActionRate(jointActionRate).build();
    }

    @Test
    public void testTwoObjectsEqual(){
        BucketBasicStatistics bucketBasicStatistics2 = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();
        
        assertEquals(bucketBasicStatistics, bucketBasicStatistics2);
    }

    @Test
    public void testTwoObjectsNotEqual(){
    	Label label2 = Bucket.Label.valueOf("abcdef");
        Counts jointActionCounts2 = new Counts();
        jointActionCounts.addCount(new Counts.Builder().withEventCount(1).build());
        BucketBasicStatistics bucketBasicStatistics2 = new BucketBasicStatistics.Builder().withLabel(label2)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts2).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();
        System.out.println(bucketBasicStatistics);
        System.out.println(bucketBasicStatistics2);
        assertFalse(bucketBasicStatistics.equals(bucketBasicStatistics2));
    }

    @Test
    public void testCloneEqual(){
        BucketBasicStatistics bucketBasicStatistics2 = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();
        
        assertEquals(bucketBasicStatistics, bucketBasicStatistics2);
        
        assertEquals(bucketBasicStatistics.clone(), bucketBasicStatistics);
        assertEquals(bucketBasicStatistics2.clone(), bucketBasicStatistics2);
    }

    @Test
    public void testBuilder(){
        assertEquals(bucketBasicStatistics.getLabel(), label);
        assertEquals(bucketBasicStatistics.getActionCounts(), actionCountsMap);
        assertEquals(bucketBasicStatistics.getActionRates(), actionRateMap);
        assertEquals(bucketBasicStatistics.getJointActionCounts(), jointActionCounts);
        assertEquals(bucketBasicStatistics.getImpressionCounts(), impressionCounts);
        assertEquals(bucketBasicStatistics.getJointActionRate(), jointActionRate);

        assertNotNull(bucketBasicStatistics.hashCode());
        assertNotNull(bucketBasicStatistics.toString());
        assertNotNull(bucketBasicStatistics.clone());
        assertTrue(bucketBasicStatistics.equals(bucketBasicStatistics));
        assertFalse(bucketBasicStatistics.equals(null));
    }

    @Test
    public void testSettersAndGetters(){
        bucketBasicStatistics.setLabel(null);
        assertEquals(bucketBasicStatistics.getLabel(), null);
        assertThat(bucketBasicStatistics.toString(), is(
        		"BucketBasicStatistics[label=<null>,jointActionRate=Estimate[estimate=<null>,lowerBound=<null>,upperBound=<null>],actionRates={},actionCounts={},impressionCounts=Counts[eventCount=0,uniqueUserCount=0],jointActionCounts=Counts[eventCount=0,uniqueUserCount=0]]"));
    }


    @Test
    public void addToActionRateTest(){
        ActionRate actionRate = new ActionRate();
        bucketBasicStatistics.addToActionRate(Event.Name.valueOf("Test"), actionRate);
        assertThat(bucketBasicStatistics.getActionRates().size(), is(1));
        BucketBasicStatistics bucketBasicStatistics1 = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(null).withActionRates(null)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();
        bucketBasicStatistics1.addToActionRate(Event.Name.valueOf("Test"), actionRate);
        assertThat(bucketBasicStatistics1.getActionRates().size(), is(1));

    }

    public void equalsMethodTest(){
        BucketBasicStatistics bucketBasicStatistics1 = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();
        BucketBasicStatistics bucketBasicStatistics2 = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();
        BucketBasicStatistics bucketBasicStatistics3 = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(null).withActionRates(null)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();

        assertTrue(bucketBasicStatistics1.equals(bucketBasicStatistics2));
        assertNotEquals(bucketBasicStatistics1, bucketBasicStatistics3);
        assertNotEquals(bucketBasicStatistics2, bucketBasicStatistics3);
    }

}
