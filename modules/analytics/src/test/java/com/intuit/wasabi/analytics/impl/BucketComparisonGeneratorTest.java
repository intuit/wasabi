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
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics.BinomialMetric;
import com.intuit.wasabi.analyticsobjects.metrics.NormalApproxSymMetric;
import com.intuit.wasabi.analyticsobjects.statistics.ActionComparisonStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.BucketComparison;
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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.NaN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;

@RunWith(MockitoJUnitRunner.class)
public class BucketComparisonGeneratorTest {

    private BinomialMetric metric95 = new NormalApproxSymMetric(0.95, 1.0);
    private double precision = 0.00001;
    private AnalysisTools analysisTools = new AnalysisToolsImpl();

    private Map<Event.Name, ActionCounts> actions1 = new HashMap<>();
    private Map<Event.Name, ActionCounts> actions2 = new HashMap<>();
    private Map<Event.Name, ActionCounts> actions3 = new HashMap<>();
    private Map<Event.Name, ActionCounts> actions4 = new HashMap<>();
    private Set<BucketCounts> bucketCounts = new HashSet<>();
    private BucketStatistics bucket1StatsOriginal;
    private BucketStatistics bucket2StatsOriginal;
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
        bucket2StatsOriginal = new BucketStatistics.Builder()
                .withLabel(bucket2Counts.getLabel())
                .withImpressionCounts(bucket2Counts.getImpressionCounts())
                .withJointActionCounts(bucket2Counts.getJointActionCounts())
                .withActionCounts(bucket2Counts.getActionCounts()).build();
        bucket3StatsOriginal = new BucketStatistics.Builder()
                .withLabel(bucket3Counts.getLabel())
                .withImpressionCounts(bucket3Counts.getImpressionCounts())
                .withJointActionCounts(bucket3Counts.getJointActionCounts())
                .withActionCounts(bucket3Counts.getActionCounts()).build();
    }

    @Test
    public void generateBucketComparisons() throws Exception {
        Map<Bucket.Label, BucketStatistics> bucketStatistics = new HashMap<>();
        bucketStatistics.put(bucket1Label, bucket1StatsOriginal.clone());
        bucketStatistics.put(bucket2Label, bucket2StatsOriginal.clone());
        bucketStatistics.put(bucket3Label, bucket3StatsOriginal.clone());

        analysisTools.generateBucketComparison(bucketStatistics, metric95, 0.4, Parameters.Mode.PRODUCTION);

        assertThat("number of buckets is 3", bucketStatistics.keySet(), hasSize(3));


        //check comparisons in bucket 1
        BucketStatistics bucketStats = bucketStatistics.get(bucket1Label);
        assertThat("number of comparisons is 2", bucketStats.getBucketComparisons().keySet(), hasSize(2));

        //check comparison to bucket 2
        BucketComparison comparison = bucketStats.getBucketComparisons().get(bucket2Label);
        assertThat("joint action rate difference estimate is -0.6022727",
                comparison.getJointActionComparison().getActionRateDifference().getEstimate(),
                closeTo(-0.6022727, precision));
        assertThat("joint action rate difference lower bound is -0.9512533",
                comparison.getJointActionComparison().getActionRateDifference().getLowerBound(),
                closeTo(-0.9512533, precision));
        assertThat("joint action rate difference upper bound is -0.2532922",
                comparison.getJointActionComparison().getActionRateDifference().getUpperBound(),
                closeTo(-0.2532922, precision));
        assertThat("joint action winner is bucket 2",
                comparison.getJointActionComparison().getClearComparisonWinner(), is(bucket2Label));
        assertThat("negative effect size is -0.4547275",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                closeTo(-0.4547275, precision));
        assertThat("positive effect size is 0.4547275",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                closeTo(0.4547275, precision));
        assertThat("fraction of data is 0.7737801", comparison.getJointActionComparison().getFractionDataCollected(),
                closeTo(0.7737801, precision));
        assertThat("sufficient data is false", comparison.getJointActionComparison().isSufficientData(), is(FALSE));
        assertThat("number of action comparisons is 2", comparison.getActionComparisons().keySet(), hasSize(2));

        //check click action in bucket 2
        ActionComparisonStatistics actionComparison = comparison.getActionComparisons().get(clickName);
        assertThat("click rate difference estimate is -0.4772727",
                actionComparison.getActionRateDifference().getEstimate(), closeTo(-0.4772727, precision));
        assertThat("click rate difference lower bound is -0.8763989",
                actionComparison.getActionRateDifference().getLowerBound(), closeTo(-0.8763989, precision));
        assertThat("click rate difference upper bound is -0.0781466",
                actionComparison.getActionRateDifference().getUpperBound(), closeTo(-0.0781466, precision));
        assertThat("click winner is bucket 2", actionComparison.getClearComparisonWinner(), is(bucket2Label));
        assertThat("negative effect size is -0.4547275",
                actionComparison.getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                closeTo(-0.4547275, precision));
        assertThat("positive effect size is 0.4547275",
                actionComparison.getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                closeTo(0.4547275, precision));
        assertThat("fraction of data is 0.7737801",
                actionComparison.getFractionDataCollected(), closeTo(0.7737801, precision));
        assertThat("sufficient data is false", actionComparison.isSufficientData(), is(FALSE));

        //check purchase action in bucket 2
        actionComparison = comparison.getActionComparisons().get(purchaseName);
        assertThat("purchase rate difference estimate is -0.1590909",
                actionComparison.getActionRateDifference().getEstimate(), closeTo(-0.1590909, precision));
        assertThat("purchase rate difference lower bound is -0.5039034",
                actionComparison.getActionRateDifference().getLowerBound(), closeTo(-0.5039034, precision));
        assertThat("purchase rate difference upper bound is 0.1857216",
                actionComparison.getActionRateDifference().getUpperBound(), closeTo(0.1857216, precision));
        assertThat("purchase winner is null", actionComparison.getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is -0.332086",
                actionComparison.getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                closeTo(-0.332086, precision));
        assertThat("positive effect size is 0.332086",
                actionComparison.getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                closeTo(0.332086, precision));
        assertThat("fraction of data is 1.0", actionComparison.getFractionDataCollected(), closeTo(1.0, precision));
        assertThat("sufficient data is true", actionComparison.isSufficientData(), is(TRUE));


        //check comparison to bucket 3
        comparison = bucketStats.getBucketComparisons().get(bucket3Label);
        assertThat("joint action rate difference estimate is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getEstimate(), is(NaN));
        assertThat("joint action rate difference lower bound is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getLowerBound(), is(NaN));
        assertThat("joint action rate difference upper bound is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getUpperBound(), is(NaN));
        assertThat("joint action winner is null",
                comparison.getJointActionComparison().getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is NaN",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                is(NaN));
        assertThat("positive effect size is NaN",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                is(NaN));
        assertThat("fraction of data is NaN",
                comparison.getJointActionComparison().getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", comparison.getJointActionComparison().isSufficientData(), is(FALSE));
        assertThat("number of action comparisons is 1", comparison.getActionComparisons().keySet(), hasSize(1));

        //check click action in bucket 3
        actionComparison = comparison.getActionComparisons().get(clickName);
        assertThat("click rate difference estimate is NaN",
                actionComparison.getActionRateDifference().getEstimate(), is(NaN));
        assertThat("click rate difference lower bound is NaN",
                actionComparison.getActionRateDifference().getLowerBound(), is(NaN));
        assertThat("click rate difference upper bound is NaN",
                actionComparison.getActionRateDifference().getUpperBound(), is(NaN));
        assertThat("click winner is null",
                actionComparison.getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is NaN",
                actionComparison.getSmallestDistinguishableEffectSize().getNegativeEffectSize(), is(NaN));
        assertThat("positive effect size is NaN",
                actionComparison.getSmallestDistinguishableEffectSize().getPositiveEffectSize(), is(NaN));
        assertThat("fraction of data is NaN", actionComparison.getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", actionComparison.isSufficientData(), is(FALSE));


        //check comparisons in bucket 2
        bucketStats = bucketStatistics.get(bucket2Label);
        assertThat("number of comparisons is 2", bucketStats.getBucketComparisons().keySet(), hasSize(2));

        //check comparison to bucket 1
        comparison = bucketStats.getBucketComparisons().get(bucket1Label);
        assertThat("joint action rate difference estimate is 0.6022727",
                comparison.getJointActionComparison().getActionRateDifference().getEstimate(),
                closeTo(0.6022727, precision));
        assertThat("joint action rate difference lower bound is 0.2532922",
                comparison.getJointActionComparison().getActionRateDifference().getLowerBound(),
                closeTo(0.2532922, precision));
        assertThat("joint action rate difference upper bound is 0.9512533",
                comparison.getJointActionComparison().getActionRateDifference().getUpperBound(),
                closeTo(0.9512533, precision));
        assertThat("joint action winner is bucket 2",
                comparison.getJointActionComparison().getClearComparisonWinner(), is(bucket2Label));
        assertThat("negative effect size is -0.4547275",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                closeTo(-0.4547275, precision));
        assertThat("positive effect size is 0.4547275",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                closeTo(0.4547275, precision));
        assertThat("fraction of data is 0.7737801",
                comparison.getJointActionComparison().getFractionDataCollected(), closeTo(0.7737801, precision));
        assertThat("sufficient data is false", comparison.getJointActionComparison().isSufficientData(), is(FALSE));
        assertThat("number of action comparisons is 2", comparison.getActionComparisons().keySet(), hasSize(2));

        //check click action in bucket 1
        actionComparison = comparison.getActionComparisons().get(clickName);
        assertThat("click rate difference estimate is 0.4772727",
                actionComparison.getActionRateDifference().getEstimate(), closeTo(0.4772727, precision));
        assertThat("click rate difference lower bound is 0.0781466",
                actionComparison.getActionRateDifference().getLowerBound(), closeTo(0.0781466, precision));
        assertThat("click rate difference upper bound is 0.8763989",
                actionComparison.getActionRateDifference().getUpperBound(), closeTo(0.8763989, precision));
        assertThat("click winner is bucket 2", actionComparison.getClearComparisonWinner(), is(bucket2Label));
        assertThat("negative effect size is -0.4547275",
                actionComparison.getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                closeTo(-0.4547275, precision));
        assertThat("positive effect size is 0.4547275",
                actionComparison.getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                closeTo(0.4547275, precision));
        assertThat("fraction of data is 0.7737801", actionComparison.getFractionDataCollected(),
                closeTo(0.7737801, precision));
        assertThat("sufficient data is false", actionComparison.isSufficientData(), is(FALSE));

        //check purchase action in bucket 1
        actionComparison = comparison.getActionComparisons().get(purchaseName);
        assertThat("purchase rate difference estimate is 0.1590909",
                actionComparison.getActionRateDifference().getEstimate(), closeTo(0.1590909, precision));
        assertThat("purchase rate difference lower bound is -0.1857216",
                actionComparison.getActionRateDifference().getLowerBound(), closeTo(-0.1857216, precision));
        assertThat("purchase rate difference upper bound is 0.5039034",
                actionComparison.getActionRateDifference().getUpperBound(), closeTo(0.5039034, precision));
        assertThat("purchase winner is null", actionComparison.getClearComparisonWinner(),
                is(nullValue()));
        assertThat("negative effect size is -0.332086",
                actionComparison.getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                closeTo(-0.332086, precision));
        assertThat("positive effect size is 0.332086",
                actionComparison.getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                closeTo(0.332086, precision));
        assertThat("fraction of data is 1.0", actionComparison.getFractionDataCollected(), closeTo(1.0, precision));
        assertThat("sufficient data is true", actionComparison.isSufficientData(), is(TRUE));


        //check comparison to bucket 3
        comparison = bucketStats.getBucketComparisons().get(bucket3Label);
        assertThat("joint action rate difference estimate is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getEstimate(), is(NaN));
        assertThat("joint action rate difference lower bound is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getLowerBound(), is(NaN));
        assertThat("joint action rate difference upper bound is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getUpperBound(), is(NaN));
        assertThat("joint action winner is null",
                comparison.getJointActionComparison().getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is NaN",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                is(NaN));
        assertThat("positive effect size is NaN",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                is(NaN));
        assertThat("fraction of data is NaN",
                comparison.getJointActionComparison().getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", comparison.getJointActionComparison().isSufficientData(), is(FALSE));
        assertThat("number of action comparisons is 1", comparison.getActionComparisons().keySet(), hasSize(1));

        //check click action in bucket 3
        actionComparison = comparison.getActionComparisons().get(clickName);
        assertThat("click rate difference estimate is NaN",
                actionComparison.getActionRateDifference().getEstimate(), is(NaN));
        assertThat("click rate difference lower bound is NaN",
                actionComparison.getActionRateDifference().getLowerBound(), is(NaN));
        assertThat("click rate difference upper bound is NaN",
                actionComparison.getActionRateDifference().getUpperBound(), is(NaN));
        assertThat("click winner is null", actionComparison.getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is NaN",
                actionComparison.getSmallestDistinguishableEffectSize().getNegativeEffectSize(), is(NaN));
        assertThat("positive effect size is NaN",
                actionComparison.getSmallestDistinguishableEffectSize().getPositiveEffectSize(), is(NaN));
        assertThat("fraction of data is NaN", actionComparison.getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", actionComparison.isSufficientData(), is(FALSE));


        //check comparisons in bucket 3
        bucketStats = bucketStatistics.get(bucket3Label);
        assertThat("number of comparisons is 2", bucketStats.getBucketComparisons().keySet(), hasSize(2));

        //check comparison to bucket 1
        comparison = bucketStats.getBucketComparisons().get(bucket1Label);
        assertThat("joint action rate difference estimate is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getEstimate(), is(NaN));
        assertThat("joint action rate difference lower bound is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getLowerBound(), is(NaN));
        assertThat("joint action rate difference upper bound is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getUpperBound(), is(NaN));
        assertThat("joint action winner is null",
                comparison.getJointActionComparison().getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is NaN",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                is(NaN));
        assertThat("positive effect size is NaN",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                is(NaN));
        assertThat("fraction of data is NaN",
                comparison.getJointActionComparison().getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", comparison.getJointActionComparison().isSufficientData(), is(FALSE));
        assertThat("number of action comparisons is 1", comparison.getActionComparisons().keySet(), hasSize(1));

        //check click action in bucket 1
        actionComparison = comparison.getActionComparisons().get(clickName);
        assertThat("click rate difference estimate is NaN",
                actionComparison.getActionRateDifference().getEstimate(), is(NaN));
        assertThat("click rate difference lower bound is NaN",
                actionComparison.getActionRateDifference().getLowerBound(), is(NaN));
        assertThat("click rate difference upper bound is NaN",
                actionComparison.getActionRateDifference().getUpperBound(), is(NaN));
        assertThat("click winner is null", actionComparison.getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is NaN",
                actionComparison.getSmallestDistinguishableEffectSize().getNegativeEffectSize(), is(NaN));
        assertThat("positive effect size is NaN",
                actionComparison.getSmallestDistinguishableEffectSize().getPositiveEffectSize(), is(NaN));
        assertThat("fraction of data is NaN", actionComparison.getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", actionComparison.isSufficientData(), is(FALSE));


        //check comparison to bucket 2
        comparison = bucketStats.getBucketComparisons().get(bucket2Label);
        assertThat("joint action rate difference estimate is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getEstimate(), is(NaN));
        assertThat("joint action rate difference lower bound is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getLowerBound(), is(NaN));
        assertThat("joint action rate difference upper bound is NaN",
                comparison.getJointActionComparison().getActionRateDifference().getUpperBound(), is(NaN));
        assertThat("joint action winner is null",
                comparison.getJointActionComparison().getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is NaN",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getNegativeEffectSize(),
                is(NaN));
        assertThat("positive effect size is NaN",
                comparison.getJointActionComparison().getSmallestDistinguishableEffectSize().getPositiveEffectSize(),
                is(NaN));
        assertThat("fraction of data is NaN",
                comparison.getJointActionComparison().getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", comparison.getJointActionComparison().isSufficientData(), is(FALSE));
        assertThat("number of action comparisons is 1", comparison.getActionComparisons().keySet(), hasSize(1));

        //check click action in bucket 2
        actionComparison = comparison.getActionComparisons().get(clickName);
        assertThat("click rate difference estimate is NaN",
                actionComparison.getActionRateDifference().getEstimate(), is(NaN));
        assertThat("click rate difference lower bound is NaN",
                actionComparison.getActionRateDifference().getLowerBound(), is(NaN));
        assertThat("click rate difference upper bound is NaN",
                actionComparison.getActionRateDifference().getUpperBound(), is(NaN));
        assertThat("click winner is null", actionComparison.getClearComparisonWinner(), is(nullValue()));
        assertThat("negative effect size is NaN",
                actionComparison.getSmallestDistinguishableEffectSize().getNegativeEffectSize(), is(NaN));
        assertThat("positive effect size is NaN",
                actionComparison.getSmallestDistinguishableEffectSize().getPositiveEffectSize(), is(NaN));
        assertThat("fraction of data is NaN", actionComparison.getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", actionComparison.isSufficientData(), is(FALSE));
    }
}
