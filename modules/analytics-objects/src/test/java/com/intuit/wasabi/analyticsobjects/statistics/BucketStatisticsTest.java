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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests the {@link BucketStatistics}
 */
public class BucketStatisticsTest {

    private Bucket.Label label;
    private Map<Event.Name, ActionCounts> actionCountsMap;
    private Map<Event.Name, ActionRate> actionRateMap;
    private Counts jointActionCounts;
    private Counts impressionCounts;
    private Estimate jointActionRate;
    private Map<Bucket.Label, BucketComparison> bucketComparisons;
    private BucketStatistics bucketStatistics;

    @Before
    public void setup() {
        label = Bucket.Label.valueOf("TestWinner");
        actionCountsMap = new HashMap<>();
        actionRateMap = new HashMap<>();
        jointActionCounts = new Counts();
        impressionCounts = new Counts();
        jointActionRate = new Estimate();
        bucketComparisons = new HashMap<>();
        bucketStatistics = new BucketStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).withBucketComparisons(bucketComparisons).build();
    }

    @Test
    public void testTwoObjectsEqual() {
        BucketStatistics bucketStatistics2 = new BucketStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).withBucketComparisons(bucketComparisons).build();
        assertThat(bucketStatistics, equalTo(bucketStatistics2));
    }

    @Test
    public void testTwoObjectsNotEqual() {
        Label label2 = Bucket.Label.valueOf("TestWinner2");

        BucketStatistics bucketStatistics2 = new BucketStatistics.Builder().withLabel(label2)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).withBucketComparisons(bucketComparisons).build();
        assertThat(bucketStatistics, not(equalTo(bucketStatistics2)));
    }

    @Test
    public void testCloneObjectsEqual() {
        BucketStatistics bucketStatistics2 = new BucketStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).withBucketComparisons(bucketComparisons).build();
        assertThat(bucketStatistics, equalTo(bucketStatistics2.clone()));
        assertThat(bucketStatistics.clone(), equalTo(bucketStatistics2));
    }

    @Test
    public void testBuilder() {
        assertThat(bucketStatistics.getLabel(), is(label));
        assertThat(bucketStatistics.getActionCounts(), is(actionCountsMap));
        assertThat(bucketStatistics.getActionRates(), is(actionRateMap));
        assertThat(bucketStatistics.getJointActionCounts(), is(jointActionCounts));
        assertThat(bucketStatistics.getImpressionCounts(), is(impressionCounts));
        assertThat(bucketStatistics.getJointActionRate(), is(jointActionRate));

        assertThat(bucketStatistics.hashCode(), is(bucketStatistics.clone().hashCode()));

        String bucketStats = bucketStatistics.toString();
        assertThat(bucketStats, containsString(label.toString()));
        assertThat(bucketStats, containsString(actionCountsMap.toString()));
        assertThat(bucketStats, containsString(actionRateMap.toString()));
        assertThat(bucketStats, containsString(jointActionCounts.toString()));
        assertThat(bucketStats, containsString(impressionCounts.toString()));
        assertThat(bucketStats, containsString(jointActionRate.toString()));

        assertThat(bucketStatistics, equalTo(bucketStatistics));
        assertThat(bucketStatistics, not(equalTo(null)));
        assertThat(bucketStatistics, not(equalTo(bucketComparisons)));
    }

    @Test
    public void testSettersAndGetters() {
        bucketStatistics.setLabel(null);
        assertThat(bucketStatistics.getLabel(), nullValue());

        bucketStatistics.setBucketComparisons(null);
        assertThat(bucketStatistics.getBucketComparisons(), nullValue());

        bucketStatistics.addToBucketComparisons(label, null);
        assertThat(bucketStatistics.getBucketComparisons(), notNullValue());
    }
}
