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
import com.intuit.wasabi.analyticsobjects.statistics.ActionProgress;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.NaN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ProgressGeneratorTest {

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
    private BucketStatistics bucket4StatsOriginal;
    private Event.Name clickName = Event.Name.valueOf("click");
    private Event.Name purchaseName = Event.Name.valueOf("purchase");
    private Event.Name commentName = Event.Name.valueOf("comment");
    private Bucket.Label bucket1Label = Bucket.Label.valueOf("one");
    private Bucket.Label bucket2Label = Bucket.Label.valueOf("two");
    private Bucket.Label bucket3Label = Bucket.Label.valueOf("three");
    private Bucket.Label bucket4Label = Bucket.Label.valueOf("four");

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
        BucketCounts bucket4Counts = new BucketCounts.Builder().withLabel(bucket4Label)
                .withImpressionCounts(new Counts.Builder().withEventCount(15)
                        .withUniqueUserCount(11).build())
                // not a valid jointActionCounts, given the individual actions
                // however we do this just to test a specific case for generateProgress
                .withJointActionCounts(new Counts.Builder().withEventCount(12)
                        .withUniqueUserCount(10).build())
                .withActionCounts(actions4).build();
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
        bucket4StatsOriginal = new BucketStatistics.Builder()
                .withLabel(bucket4Counts.getLabel())
                .withImpressionCounts(bucket4Counts.getImpressionCounts())
                .withJointActionCounts(bucket4Counts.getJointActionCounts())
                .withActionCounts(bucket4Counts.getActionCounts()).build();
    }

    @Test
    public void generateProgress1() throws Exception {
        Map<Bucket.Label, BucketStatistics> bucketStatistics = new HashMap<>();
        bucketStatistics.put(bucket1Label, bucket1StatsOriginal.clone());
        bucketStatistics.put(bucket2Label, bucket2StatsOriginal.clone());
        bucketStatistics.put(bucket3Label, bucket3StatsOriginal.clone());

        analysisTools.generateBucketComparison(bucketStatistics, metric95, 0.4, Parameters.Mode.PRODUCTION);

        ExperimentStatistics experimentStats = new ExperimentStatistics.Builder()
                .withBuckets(bucketStatistics).build();

        analysisTools.generateProgress(experimentStats);

        Set<Bucket.Label> winners = new HashSet<>();
        winners.add(bucket2Label);
        winners.add(bucket3Label);
        Set<Bucket.Label> losers = new HashSet<>();
        losers.add(bucket1Label);

        assertThat("joint action winners",
                new HashSet<>(experimentStats.getJointProgress().getWinnersSoFar()), is(winners));
        assertThat("joint action losers",
                new HashSet<>(experimentStats.getJointProgress().getLosersSoFar()), is(losers));
        assertThat("fraction of data is NaN", experimentStats.getJointProgress().getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", experimentStats.getJointProgress().isHasSufficientData(), is(FALSE));
        assertThat("number of action progresses is 2", experimentStats.getActionProgress().keySet(), hasSize(2));

        ActionProgress actionProgress = experimentStats.getActionProgress().get(clickName);
        assertThat("click winners", new HashSet<>(actionProgress.getWinnersSoFar()), is(winners));
        assertThat("click losers", new HashSet<>(actionProgress.getLosersSoFar()), is(losers));
        assertThat("fraction of data is NaN", actionProgress.getFractionDataCollected(), is(NaN));
        assertThat("sufficient data is false", actionProgress.isHasSufficientData(), is(FALSE));

        actionProgress = experimentStats.getActionProgress().get(purchaseName);
        assertThat("purchase winners empty", actionProgress.getWinnersSoFar(), is(empty()));
        assertThat("purchase losers empty", actionProgress.getLosersSoFar(), is(empty()));
        assertThat("fraction of data is 1.0", actionProgress.getFractionDataCollected(), closeTo(1.0, precision));
        assertThat("sufficient data is true", actionProgress.isHasSufficientData(), is(TRUE));

        assertThat("experiment winners empty", experimentStats.getExperimentProgress().getWinnersSoFar(),
                is(empty()));
        assertThat("experiment losers empty", experimentStats.getExperimentProgress().getLosersSoFar(),
                is(empty()));
        assertThat("fraction of data is NaN", experimentStats.getExperimentProgress().getFractionDataCollected(),
                is(NaN));
        assertThat("sufficient data is false", experimentStats.getExperimentProgress().isHasSufficientData(), is(FALSE));
    }

    @Test
    public void generateProgress2() throws Exception {
        Map<Bucket.Label, BucketStatistics> bucketStatistics = new HashMap<>();
        bucketStatistics.put(bucket1Label, bucket1StatsOriginal.clone());
        bucketStatistics.put(bucket2Label, bucket2StatsOriginal.clone());
        bucketStatistics.put(bucket4Label, bucket4StatsOriginal.clone());

        analysisTools.generateBucketComparison(bucketStatistics, metric95, 0.05, Parameters.Mode.PRODUCTION);

        ExperimentStatistics experimentStats = new ExperimentStatistics.Builder()
                .withBuckets(bucketStatistics)
                .build();

        analysisTools.generateProgress(experimentStats);

        Set<Bucket.Label> winners = new HashSet<>();
        winners.add(bucket2Label);
        winners.add(bucket4Label);
        Set<Bucket.Label> losers = new HashSet<>();
        losers.add(bucket1Label);

        assertThat("joint action winners", new HashSet<>(experimentStats.getJointProgress().getWinnersSoFar()),
                is(winners));
        assertThat("joint action losers", new HashSet<>(experimentStats.getJointProgress().getLosersSoFar()),
                is(losers));
        assertThat("fraction of data is 0.01209031", experimentStats.getJointProgress().getFractionDataCollected(),
                closeTo(0.01209031, precision));
        assertThat("sufficient data is false", experimentStats.getJointProgress().isHasSufficientData(), is(FALSE));
        assertThat("number of action progresses is 2", experimentStats.getActionProgress().values(), hasSize(2));

        ActionProgress actionProgress = experimentStats.getActionProgress().get(clickName);
        assertThat("click winners", new HashSet<>(actionProgress.getWinnersSoFar()), is(winners));
        assertThat("click losers", new HashSet<>(actionProgress.getLosersSoFar()), is(losers));
        assertThat("fraction of data is 0.01209031", actionProgress.getFractionDataCollected(),
                closeTo(0.01209031, precision));
        assertThat("sufficient data is false", actionProgress.isHasSufficientData(), is(FALSE));

        actionProgress = experimentStats.getActionProgress().get(purchaseName);
        winners = new HashSet<>();
        winners.add(bucket4Label);
        losers = new HashSet<>();
        losers.add(bucket1Label);
        losers.add(bucket2Label);
        assertThat("purchase winners", new HashSet<>(actionProgress.getWinnersSoFar()), is(winners));
        assertThat("purchase losers", new HashSet<>(actionProgress.getLosersSoFar()), is(losers));
        assertThat("fraction of data is 0.01295391", actionProgress.getFractionDataCollected(),
                closeTo(0.01295391, precision));
        assertThat("sufficient data is false", actionProgress.isHasSufficientData(), is(FALSE));

        winners = new HashSet<>();
        winners.add(bucket4Label);
        losers = new HashSet<>();
        losers.add(bucket1Label);

        assertThat("experiment winners", new HashSet<>(experimentStats.getExperimentProgress().getWinnersSoFar()),
                is(winners));
        assertThat("experiment losers", new HashSet<>(experimentStats.getExperimentProgress().getLosersSoFar()),
                is(losers));
        assertThat("fraction of data is 0.01252211", experimentStats.getExperimentProgress().getFractionDataCollected(),
                closeTo(0.01252211, precision));
        assertThat("sufficient data is false", experimentStats.getExperimentProgress().isHasSufficientData(), is(FALSE));
    }
}
