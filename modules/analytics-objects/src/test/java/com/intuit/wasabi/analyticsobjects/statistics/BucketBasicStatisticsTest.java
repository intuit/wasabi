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
 * Tests the {@link BucketBasicStatistics}
 */
public class BucketBasicStatisticsTest {

    private Bucket.Label label;
    private Map<Event.Name, ActionCounts> actionCountsMap;
    private Map<Event.Name, ActionRate> actionRateMap;
    private Counts jointActionCounts;
    private Counts impressionCounts;
    private Estimate jointActionRate;
    private BucketBasicStatistics bucketBasicStatistics;

    @Before
    public void setup() {
        label = Bucket.Label.valueOf("TestWinner");
        actionCountsMap = new HashMap<>();
        actionRateMap = new HashMap<>();
        jointActionCounts = new Counts();
        impressionCounts = new Counts();
        jointActionRate = new Estimate();
        bucketBasicStatistics = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();
    }

    @Test
    public void testTwoObjectsEqual() {
        BucketBasicStatistics bucketBasicStatistics2 = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();

        assertThat(bucketBasicStatistics, equalTo(bucketBasicStatistics2));
    }

    @Test
    public void testTwoObjectsNotEqual() {
        Label label2 = Bucket.Label.valueOf("abcdef");
        Counts jointActionCounts2 = new Counts();
        jointActionCounts.addCount(new Counts.Builder().withEventCount(1).build());
        BucketBasicStatistics bucketBasicStatistics2 = new BucketBasicStatistics.Builder().withLabel(label2)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts2).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();
        assertThat(bucketBasicStatistics, not(equalTo(bucketBasicStatistics2)));
    }

    @Test
    public void testCloneEqual() {
        BucketBasicStatistics bucketBasicStatistics2 = new BucketBasicStatistics.Builder().withLabel(label)
                .withActionCounts(actionCountsMap).withActionRates(actionRateMap)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withJointActionRate(jointActionRate).build();

        assertThat(bucketBasicStatistics, equalTo(bucketBasicStatistics2));

        assertThat(bucketBasicStatistics.clone(), equalTo(bucketBasicStatistics));
        assertThat(bucketBasicStatistics2.clone(), equalTo(bucketBasicStatistics2));
    }

    @Test
    public void testBuilder() {
        assertThat(bucketBasicStatistics.getLabel(), equalTo(label));
        assertThat(bucketBasicStatistics.getActionCounts(), equalTo(actionCountsMap));
        assertThat(bucketBasicStatistics.getActionRates(), equalTo(actionRateMap));
        assertThat(bucketBasicStatistics.getJointActionCounts(), equalTo(jointActionCounts));
        assertThat(bucketBasicStatistics.getImpressionCounts(), equalTo(impressionCounts));
        assertThat(bucketBasicStatistics.getJointActionRate(), equalTo(jointActionRate));

        assertThat(bucketBasicStatistics, equalTo(bucketBasicStatistics.clone()));
        assertThat(bucketBasicStatistics.hashCode(), equalTo(bucketBasicStatistics.clone().hashCode()));

        String buckBasicStats = bucketBasicStatistics.toString();

        assertThat(buckBasicStats, containsString(label.toString()));
        assertThat(buckBasicStats, containsString(actionCountsMap.toString()));
        assertThat(buckBasicStats, containsString(actionRateMap.toString()));
        assertThat(buckBasicStats, containsString(jointActionCounts.toString()));
        assertThat(buckBasicStats, containsString(impressionCounts.toString()));
        assertThat(buckBasicStats, containsString(jointActionRate.toString()));

        assertThat(bucketBasicStatistics, equalTo(bucketBasicStatistics));
        assertThat(bucketBasicStatistics, notNullValue());
    }

    @Test
    public void testSettersAndGetters() {
        bucketBasicStatistics.setLabel(null);
        assertThat(bucketBasicStatistics.getLabel(), nullValue());
        assertThat(bucketBasicStatistics.toString(), equalTo(
                "BucketBasicStatistics[label=<null>,jointActionRate=Estimate[estimate=<null>,lowerBound=<null>," +
                        "upperBound=<null>],actionRates={},actionCounts={},impressionCounts=Counts[eventCount=0," +
                        "uniqueUserCount=0],jointActionCounts=Counts[eventCount=0,uniqueUserCount=0]]"));
    }


    @Test
    public void addToActionRateTest() {
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

    @Test
    public void equalsMethodTest() {
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

        assertThat(bucketBasicStatistics1, equalTo(bucketBasicStatistics2));
        assertThat(bucketBasicStatistics1, not(equalTo(bucketBasicStatistics3)));
        assertThat(bucketBasicStatistics2, not(equalTo(bucketBasicStatistics3)));
    }

}
