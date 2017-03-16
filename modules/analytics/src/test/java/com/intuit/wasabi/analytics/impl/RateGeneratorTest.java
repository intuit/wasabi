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
package com.intuit.wasabi.analytics.impl;

import com.intuit.wasabi.analytics.AnalysisTools;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics.BinomialMetric;
import com.intuit.wasabi.analyticsobjects.metrics.NormalApproxSymMetric;
import com.intuit.wasabi.analyticsobjects.statistics.ActionRate;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Double.NaN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;

@RunWith(MockitoJUnitRunner.class)
public class RateGeneratorTest {

    private BinomialMetric metric95 = new NormalApproxSymMetric(0.95, 1.0);
    private double precision = 0.00001;
    private AnalysisTools analysisTools = new AnalysisToolsImpl();

    private Map<Event.Name, ActionCounts> actions1 = new HashMap<>();
    private Map<Event.Name, ActionCounts> actions2 = new HashMap<>();
    private Map<Event.Name, ActionCounts> actions3 = new HashMap<>();
    private Map<Event.Name, ActionCounts> actions4 = new HashMap<>();
    private Set<BucketCounts> bucketCounts = new HashSet<>();
    private BucketStatistics bucket1StatsOriginal;
    private BucketStatistics bucket3StatsOriginal;
    private Event.Name clickName = Event.Name.valueOf("click");
    private Event.Name purchaseName = Event.Name.valueOf("purchase");
    private Event.Name commentName = Event.Name.valueOf("comment");
    private Bucket.Label bucket1Label = Bucket.Label.valueOf("one");
    private Bucket.Label bucket2Label = Bucket.Label.valueOf("two");
    private Bucket.Label bucket3Label = Bucket.Label.valueOf("three");

    @Before
    public void before() {
        ActionCounts click1 = new ActionCounts.Builder().withActionName(clickName).withEventCount(7)
                .withUniqueUserCount(3).build();
        ActionCounts click2 = new ActionCounts.Builder().withActionName(clickName).withEventCount(9)
                .withUniqueUserCount(6).build();
        ActionCounts click3 = new ActionCounts.Builder().withActionName(clickName).withEventCount(5)
                .withUniqueUserCount(3).build();
        ActionCounts click4 = new ActionCounts.Builder().withActionName(clickName).withEventCount(9)
                .withUniqueUserCount(6).build();
        ActionCounts purchase1 = new ActionCounts.Builder().withActionName(purchaseName).withEventCount(3)
                .withUniqueUserCount(1).build();
        ActionCounts purchase2 = new ActionCounts.Builder().withActionName(purchaseName).withEventCount(3)
                .withUniqueUserCount(2).build();
        ActionCounts purchase4 = new ActionCounts.Builder().withActionName(purchaseName).withEventCount(10)
                .withUniqueUserCount(10).build();
        ActionCounts comment1 = new ActionCounts.Builder().withActionName(commentName).withEventCount(2)
                .withUniqueUserCount(2).build();
        actions1.put(clickName, click1);
        actions1.put(purchaseName, purchase1);
        actions1.put(commentName, comment1);
        actions2.put(clickName, click2);
        actions2.put(purchaseName, purchase2);
        actions3.put(clickName, click3);
        actions4.put(clickName, click4);
        actions4.put(purchaseName, purchase4);

        BucketCounts bucket1Counts = new BucketCounts.Builder().withLabel(bucket1Label)
                .withImpressionCounts(new Counts.Builder().withEventCount(15)
                        .withUniqueUserCount(11).build())
                .withJointActionCounts(new Counts.Builder().withEventCount(12)
                        .withUniqueUserCount(3).build())
                .withActionCounts(actions1).build();
        BucketCounts bucket2Counts = new BucketCounts.Builder().withLabel(bucket2Label)
                .withImpressionCounts(new Counts.Builder().withEventCount(19)
                        .withUniqueUserCount(8).build())
                .withJointActionCounts(new Counts.Builder().withEventCount(12)
                        .withUniqueUserCount(7).build())
                .withActionCounts(actions2).build();
        BucketCounts bucket3Counts = new BucketCounts.Builder().withLabel(bucket3Label)
                .withImpressionCounts(new Counts.Builder().withEventCount(0)
                        .withUniqueUserCount(0).build())
                .withJointActionCounts(new Counts.Builder().withEventCount(5)
                        .withUniqueUserCount(3).build())
                .withActionCounts(actions3).build();
        bucketCounts.add(bucket1Counts);
        bucketCounts.add(bucket2Counts);

        bucket1StatsOriginal = new BucketStatistics.Builder()
                .withLabel(bucket1Counts.getLabel())
                .withImpressionCounts(bucket1Counts.getImpressionCounts())
                .withJointActionCounts(bucket1Counts.getJointActionCounts())
                .withActionCounts(bucket1Counts.getActionCounts()).build();
        bucket3StatsOriginal = new BucketStatistics.Builder()
                .withLabel(bucket3Counts.getLabel())
                .withImpressionCounts(bucket3Counts.getImpressionCounts())
                .withJointActionCounts(bucket3Counts.getJointActionCounts())
                .withActionCounts(bucket3Counts.getActionCounts()).build();
    }

    @Test
    public void generateRatesBucket1() throws Exception {
        BucketStatistics bucketStats = bucket1StatsOriginal.clone();
        analysisTools.generateRate(bucketStats, metric95);

        assertThat("joint action rate estimate = 0.2727273", bucketStats.getJointActionRate().getEstimate(),
                closeTo(0.2727273, precision));
        assertThat("joint action rate lower bound = 0.0920451", bucketStats.getJointActionRate().getLowerBound(),
                closeTo(0.0920451, precision));
        assertThat("joint action rate upper bound = 0.5710608", bucketStats.getJointActionRate().getUpperBound(),
                closeTo(0.5710608, precision));
        assertThat("number of actions is 3", bucketStats.getActionRates().keySet(), hasSize(3));

        ActionRate actionRate = bucketStats.getActionRates().get(clickName);
        assertThat("click rate estimate = 0.2727273", actionRate.getEstimate(), closeTo(0.2727273, precision));
        assertThat("click rate lower bound = 0.0920451", actionRate.getLowerBound(), closeTo(0.0920451, precision));
        assertThat("click rate upper bound = 0.5710608", actionRate.getUpperBound(), closeTo(0.5710608, precision));

        actionRate = bucketStats.getActionRates().get(purchaseName);
        assertThat("purchase rate estimate = 0.09090909", actionRate.getEstimate(),
                closeTo(0.09090909, precision));
        assertThat("purchase rate lower bound = -0.005474139", actionRate.getLowerBound(),
                closeTo(-0.005474139, precision));
        assertThat("purchase rate upper bound = 0.3990647", actionRate.getUpperBound(),
                closeTo(0.3990647, precision));

        actionRate = bucketStats.getActionRates().get(commentName);
        assertThat("comment rate estimate = 0.1818182", actionRate.getEstimate(), closeTo(0.1818182, precision));
        assertThat("comment rate lower bound = 0.03986731", actionRate.getLowerBound(),
                closeTo(0.03986731, precision));
        assertThat("comment rate upper bound = 0.4884809", actionRate.getUpperBound(),
                closeTo(0.4884809, precision));
    }

    @Test
    public void generateRatesBucket3() throws Exception {
        BucketStatistics bucketStats = bucket3StatsOriginal.clone();

        analysisTools.generateRate(bucketStats, metric95);

        assertThat("joint action rate estimate is NaN", bucketStats.getJointActionRate().getEstimate(), is(NaN));
        assertThat("joint action rate lower bound is NaN", bucketStats.getJointActionRate().getLowerBound(),
                is(NaN));
        assertThat("joint action rate upper bound is NaN", bucketStats.getJointActionRate().getUpperBound(),
                is(NaN));
        assertThat("number of actions is 1", bucketStats.getActionRates().keySet(), hasSize(1));

        ActionRate actionRate = bucketStats.getActionRates().get(clickName);
        assertThat("click rate estimate is NaN", actionRate.getEstimate(), is(NaN));
        assertThat("click rate lower bound is NaN", actionRate.getLowerBound(), is(NaN));
        assertThat("click rate upper bound is NaN", actionRate.getUpperBound(), is(NaN));
    }
}
