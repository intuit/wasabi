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

import com.google.common.math.DoubleMath;
import com.intuit.wasabi.analytics.AnalysisTools;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics;
import com.intuit.wasabi.analyticsobjects.statistics.AbstractContainerStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ActionComparisonStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ActionProgress;
import com.intuit.wasabi.analyticsobjects.statistics.ActionRate;
import com.intuit.wasabi.analyticsobjects.statistics.BucketComparison;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ComparisonStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.DistinguishableEffectSize;
import com.intuit.wasabi.analyticsobjects.statistics.Estimate;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.Progress;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Double.NaN;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * AnalysisTools implementation
 */
public class AnalysisToolsImpl implements AnalysisTools {

    /**
     * Floating point difference epsilon
     */
    protected static final double FLOAT_POINT_DIFFERENCE_EPSILON = 0.00000001;

    /**
     * Logger for the class
     */
    private static final Logger LOGGER = getLogger(AnalysisToolsImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateRate(AbstractContainerStatistics container, BinomialMetrics.BinomialMetric metric) {
        long uniqueImpressions = container.getImpressionCounts().getUniqueUserCount();

        Estimate jointRate;
        try {
            jointRate = metric.estimateRate(uniqueImpressions, container.getJointActionCounts().getUniqueUserCount());
        } catch (IllegalArgumentException iae) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("BinomialMetric.estimateRate called with invalid arguments by " +
                        "AnalyticsService.generateRates: ", iae);

            jointRate = new Estimate(NaN, NaN, NaN);
        }
        container.setJointActionRate(jointRate);

        Map<Event.Name, ActionRate> actionRates = new HashMap<>();
        Map<Event.Name, ActionCounts> actionCounts = container.getActionCounts();
        for (ActionCounts action : actionCounts.values()) {
            Event.Name actionName = action.getActionName();
            Estimate rate;
            try {
                rate = metric.estimateRate(uniqueImpressions, action.getUniqueUserCount());
            } catch (IllegalArgumentException iae) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("BinomialMetric.estimateRate called with invalid arguments by " +
                            "AnalyticsService.generateRates: ", iae);

                rate = new Estimate(NaN, NaN, NaN);
            }
            ActionRate actionRate = new ActionRate.Builder().withActionName(actionName).withEstimate(rate).build();
            actionRates.put(actionName, actionRate);
        }

        container.setActionRates(actionRates);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void generateBucketComparison(Map<Bucket.Label, BucketStatistics> buckets,
                                         BinomialMetrics.BinomialMetric metric, double effectSize, Parameters.Mode mode) {
        //loop over the set of buckets twice to get the two buckets to compare
        for (BucketStatistics bucket : buckets.values()) {
            for (BucketStatistics otherBucket : buckets.values()) {
                //don't compare a bucket to itself
                if (bucket != otherBucket) {
                    long bucketImpressions = bucket.getImpressionCounts().getUniqueUserCount();
                    long otherBucketImpressions = otherBucket.getImpressionCounts().getUniqueUserCount();
                    //calculate the joint action comparison
                    long bucketUniqueCounts = bucket.getJointActionCounts().getUniqueUserCount();
                    long otherBucketUniqueCounts = otherBucket.getJointActionCounts().getUniqueUserCount();
                    Estimate rateDifference;

                    try {
                        rateDifference = metric.estimateRateDifference(bucketImpressions, bucketUniqueCounts,
                                otherBucketImpressions, otherBucketUniqueCounts);
                    } catch (IllegalArgumentException iae) {
                        LOGGER.warn("BinomialMetric.estimateRateDifference called with invalid arguments by AnalyticsService.generateBucketComparisons: ", iae);

                        rateDifference = new Estimate(NaN, NaN, NaN);
                    }

                    Double fractionData;

                    fractionData = computeFractionData(metric, effectSize, mode, bucketImpressions, otherBucketImpressions, bucketUniqueCounts, otherBucketUniqueCounts);

                    DistinguishableEffectSize effects;

                    try {
                        effects = metric.distinguishableEffectSizes(bucketImpressions, bucketUniqueCounts,
                                otherBucketImpressions, otherBucketUniqueCounts);
                    } catch (IllegalArgumentException iae) {
                        LOGGER.warn("BinomialMetric.distinguishableEffectSizes called with invalid arguments by AnalyticsService.generateBucketComparisons: ", iae);

                        effects = new DistinguishableEffectSize(NaN, NaN);
                    }

                    ComparisonStatistics jointComparison = new ComparisonStatistics.Builder()
                            .withActionRateDifference(rateDifference)
                            .withSmallestDistinguishableEffectSize(effects)
                            .withFractionDataCollected(fractionData)
                            .build();

                    jointComparison.setSufficientData(DoubleMath.fuzzyEquals(fractionData, 1.0, Math.ulp(1.0)));

                    computeClearComparisonWinner(bucket, otherBucket, rateDifference, jointComparison);

                    //loop over shared actions within the buckets to calculate comparisons
                    Map<Event.Name, ActionComparisonStatistics> actionsComparisons = new HashMap<>();

                    Map<Event.Name, ActionCounts> actionCounts = bucket.getActionCounts();
                    Map<Event.Name, ActionCounts> otherActionCounts = otherBucket.getActionCounts();
                    Set<Event.Name> sharedNames = new HashSet<>(actionCounts.keySet());
                    sharedNames.retainAll(otherActionCounts.keySet());
                    for (Event.Name actionName : sharedNames) {
                        ActionCounts action = actionCounts.get(actionName);
                        ActionCounts otherAction = otherActionCounts.get(actionName);
                        bucketUniqueCounts = action.getUniqueUserCount();
                        otherBucketUniqueCounts = otherAction.getUniqueUserCount();

                        try {
                            rateDifference = metric.estimateRateDifference(bucketImpressions,
                                    bucketUniqueCounts, otherBucketImpressions, otherBucketUniqueCounts);
                        } catch (IllegalArgumentException iae) {
                            LOGGER.warn("BinomialMetric.estimateRateDifference called with invalid arguments by AnalyticsService.generateBucketComparisons: ", iae);
                            rateDifference = new Estimate(NaN, NaN, NaN);
                        }

                        fractionData = computeFractionData(metric, effectSize, mode, bucketImpressions,
                                otherBucketImpressions, bucketUniqueCounts, otherBucketUniqueCounts);

                        try {
                            effects = metric.distinguishableEffectSizes(bucketImpressions,
                                    bucketUniqueCounts, otherBucketImpressions, otherBucketUniqueCounts);
                        } catch (IllegalArgumentException iae) {
                            LOGGER.warn("BinomialMetric.distinguishableEffectSizes called with invalid arguments by AnalyticsService.generateBucketComparisons: ", iae);

                            effects = new DistinguishableEffectSize(NaN, NaN);
                        }

                        ActionComparisonStatistics actionComparison = new ActionComparisonStatistics.Builder()
                                .withActionName(actionName)
                                .withActionRateDifference(rateDifference)
                                .withSmallestDistinguishableEffectSize(effects)
                                .withFractionDataCollected(fractionData)
                                .build();

                        actionComparison.setSufficientData(DoubleMath.fuzzyEquals(fractionData, 1.0, Math.ulp(1.0)));

                        computeClearComparisonWinner(bucket, otherBucket, rateDifference, actionComparison);

                        actionsComparisons.put(actionName, actionComparison);
                    }

                    Bucket.Label otherBucketLabel = otherBucket.getLabel();
                    BucketComparison comparison = new BucketComparison.Builder()
                            .withOtherLabel(otherBucketLabel)
                            .withJointActionComparison(jointComparison)
                            .withActionComparisons(actionsComparisons)
                            .build();

                    bucket.addToBucketComparisons(otherBucketLabel, comparison);
                }
            }
        }
    }

    private void computeClearComparisonWinner(BucketStatistics bucket, BucketStatistics otherBucket, Estimate rateDifference, ComparisonStatistics jointComparison) {
        if (rateDifference.getEstimate() > 0 && rateDifference.getLowerBound() > 0) {
            jointComparison.setClearComparisonWinner(bucket.getLabel());
        } else if (rateDifference.getEstimate() < 0 && rateDifference.getUpperBound() < 0) {
            jointComparison.setClearComparisonWinner(otherBucket.getLabel());
        } else {
            jointComparison.setClearComparisonWinner(null);
        }
    }

    private Double computeFractionData(BinomialMetrics.BinomialMetric metric, double effectSize, Parameters.Mode mode, long bucketImpressions, long otherBucketImpressions, long bucketUniqueCounts, long otherBucketUniqueCounts) {
        Double fractionData;
        try {
            if (mode == Parameters.Mode.PRODUCTION) {
                fractionData = min(1.0, metric.fractionOfData(bucketImpressions, bucketUniqueCounts,
                        otherBucketImpressions, otherBucketUniqueCounts, effectSize));
            } else if (mode == Parameters.Mode.TEST) {
                fractionData = metric.fractionOfData(bucketImpressions, bucketUniqueCounts,
                        otherBucketImpressions, otherBucketUniqueCounts, effectSize);
            } else {
                throw new IllegalArgumentException("incorrect mode specified: " + mode.toString());
            }
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("BinomialMetric.fractionOfData called with invalid arguments by AnalyticsService.generateBucketComparisons: ", iae);
            fractionData = NaN;
        }
        return fractionData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentCounts calculateExperimentCounts(Collection<BucketCounts> bucketsSet) {
        Map<Bucket.Label, BucketCounts> buckets = new HashMap<>();
        for (BucketCounts bucket : bucketsSet) {
            buckets.put(bucket.getLabel(), bucket);
        }

        Counts jointActionCounts = new Counts(0, 0);
        Counts impressionCounts = new Counts(0, 0);
        Map<Event.Name, ActionCounts> experimentActionCounts = new HashMap<>();

        for (BucketCounts bucket : bucketsSet) {
            jointActionCounts.addCount(bucket.getJointActionCounts());
            impressionCounts.addCount(bucket.getImpressionCounts());

            Map<Event.Name, ActionCounts> bucketActionCounts = bucket.getActionCounts();
            for (ActionCounts bucketCounts : bucketActionCounts.values()) {
                Event.Name actionName = bucketCounts.getActionName();
                ActionCounts currentExperimentCounts = experimentActionCounts.get(actionName);
                if (currentExperimentCounts == null) {
                    experimentActionCounts.put(actionName, bucketCounts.clone());
                } else {
                    currentExperimentCounts.addCount(bucketCounts);
                }
            }
        }

        return new ExperimentCounts.Builder().withActionCounts(experimentActionCounts).withBuckets(buckets)
                .withImpressionCounts(impressionCounts).withJointActionCounts(jointActionCounts)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateProgress(ExperimentStatistics experiment) {
        Map<Bucket.Label, BucketStatistics> buckets = experiment.getBuckets();
        Set<Event.Name> actionNames = new HashSet<>();
        //calculate joint action progress
        Set<Bucket.Label> winners = new HashSet<>();
        Set<Bucket.Label> losers = new HashSet<>();
        Set<Bucket.Label> winnersToAdd = new HashSet<>();
        Double fractionOfData = null;
        Map<Bucket.Label, Integer> bucketWins = new HashMap<>();

        for (BucketStatistics bucket : buckets.values()) {
            Bucket.Label bucketName = bucket.getLabel();

            if (bucket.getBucketComparisons() != null) {
                for (BucketComparison comparison : bucket.getBucketComparisons().values()) {
                    ComparisonStatistics jointComparison = comparison.getJointActionComparison();

                    if (comparison.getJointActionComparison().getClearComparisonWinner() != null) {
                        //count the number of times a bucket is clearly better than another (a "win")
                        CountComparisonWinnerOrAddBucketToLosers(losers, bucketWins, bucketName, jointComparison);
                    }

                    //keep the smallest fraction of data value
                    Double jointFraction = jointComparison.getFractionDataCollected();

                    if (jointFraction != null) {
                        if (fractionOfData == null) {
                            fractionOfData = jointFraction;
                        } else {
                            fractionOfData = min(fractionOfData, jointFraction);
                        }
                    }

                    //create set of all actions comparisons to be used later
                    if (comparison.getActionComparisons() != null) {
                        actionNames.addAll(comparison.getActionComparisons().keySet());
                    }
                }
            }
        }

        //calculate the maximum number of wins
        int maxWins = 0;
        for (Integer wins : bucketWins.values()) {
            maxWins = max(maxWins, wins);
        }

        //and add the buckets with that number to the set of winners
        if (maxWins > 0) {
            for (Map.Entry<Bucket.Label, Integer> entry : bucketWins.entrySet()) {
                if (entry.getValue() == maxWins) {
                    winners.add(entry.getKey());
                }
            }

            //also add any bucket with is not clearly worse than these buckets
            for (Bucket.Label bucketLabel : winners) {
                BucketStatistics bucket = buckets.get(bucketLabel);
                if (bucket.getBucketComparisons() != null) {
                    for (BucketComparison comparison : bucket.getBucketComparisons().values()) {
                        Bucket.Label winnerName = comparison.getJointActionComparison().getClearComparisonWinner();
                        Bucket.Label otherBucketName = comparison.getOtherLabel();

                        if (winnerName == null || winnerName.equals(otherBucketName)) {
                            winnersToAdd.add(otherBucketName);
                        }
                    }
                }
            }
        }
        winners.addAll(winnersToAdd);

        //convert sets to lists
        List<Bucket.Label> winnersList = new ArrayList<>();
        winnersList.addAll(winners);
        List<Bucket.Label> losersList = new ArrayList<>();
        losersList.addAll(losers);

        boolean sufficientData = false;
        if (fractionOfData != null) {
            sufficientData = DoubleMath.fuzzyEquals(fractionOfData, 1.0, Math.ulp(1.0));
        }

        Progress jointProgress = new Progress.Builder().withWinnersSoFar(winnersList)
                .withLosersSoFar(losersList)
                .withSufficientData(sufficientData)
                .withFractionDataCollected(fractionOfData)
                .build();
        experiment.setJointProgress(jointProgress);

        //loop over actions to calculate progress
        Map<Event.Name, ActionProgress> actionProgresses = new HashMap<>();

        for (Event.Name actionName : actionNames) {
            winners = new HashSet<>();
            losers = new HashSet<>();
            winnersToAdd = new HashSet<>();
            fractionOfData = null;
            bucketWins = new HashMap<>();

            for (BucketStatistics bucket : buckets.values()) {
                Bucket.Label bucketName = bucket.getLabel();

                if (bucket.getBucketComparisons() != null) {
                    for (BucketComparison comparison : bucket.getBucketComparisons().values()) {

                        ActionComparisonStatistics action = comparison.getActionComparisons().get(actionName);
                        if (action != null) {
                            if (action.getClearComparisonWinner() != null) {
                                //count the number of times a bucket is clearly better than another (a "win")
                                CountComparisonWinnerOrAddBucketToLosers(losers, bucketWins, bucketName, action);
                            }

                            //keep the smallest fraction of data value
                            Double actionFraction = action.getFractionDataCollected();

                            if (actionFraction != null) {
                                if (fractionOfData == null) {
                                    fractionOfData = actionFraction;
                                } else {
                                    fractionOfData = min(fractionOfData, actionFraction);
                                }
                            }
                        }
                    }
                }
            }

            //calculate the maximum number of wins
            maxWins = 0;
            for (Integer wins : bucketWins.values()) {
                maxWins = max(maxWins, wins);
            }

            //and add the buckets with that number to the set of winners
            if (maxWins > 0) {
                for (Map.Entry<Bucket.Label, Integer> entry : bucketWins.entrySet()) {
                    if (entry.getValue() == maxWins) {
                        winners.add(entry.getKey());
                    }
                }
                //also add any bucket with is not clearly worse than these buckets
                for (Bucket.Label bucketLabel : winners) {
                    BucketStatistics bucket = buckets.get(bucketLabel);
                    if (bucket.getBucketComparisons() != null) {
                        for (BucketComparison comparison : bucket.getBucketComparisons().values()) {
                            ActionComparisonStatistics action = comparison.getActionComparisons().get(actionName);
                            if (action != null) {
                                Bucket.Label winnerName = action.getClearComparisonWinner();
                                Bucket.Label otherBucketName = comparison.getOtherLabel();

                                if (winnerName == null || winnerName.equals(otherBucketName)) {
                                    winnersToAdd.add(otherBucketName);
                                }
                            }
                        }
                    }
                }
            }
            winners.addAll(winnersToAdd);

            //convert sets to lists
            winnersList = new ArrayList<>();
            winnersList.addAll(winners);
            losersList = new ArrayList<>();
            losersList.addAll(losers);

            sufficientData = false;
            if (fractionOfData != null) {
                sufficientData = DoubleMath.fuzzyEquals(fractionOfData, 1.0, Math.ulp(1.0));
            }

            ActionProgress actionProgress = new ActionProgress.Builder().withActionName(actionName)
                    .withWinnersSoFarList(winnersList)
                    .withLosersSoFarList(losersList)
                    .withSufficientData(sufficientData)
                    .withFractionDataCollected(fractionOfData)
                    .build();

            actionProgresses.put(actionName, actionProgress);
        }

        experiment.setActionProgress(actionProgresses);
        //calculate the experiment-level progress from the action-level progresses
        winners = null;
        losers = null;
        fractionOfData = null;
        int numberActions = 0;

        for (ActionProgress actionProgress : actionProgresses.values()) {
            //take the set intersection for winners and losers
            Set<Bucket.Label> winnersSet = new HashSet<>(actionProgress.getWinnersSoFar());
            if (winners == null) {
                winners = winnersSet;
            } else {
                winners.retainAll(winnersSet);
            }

            Set<Bucket.Label> losersSet = new HashSet<>(actionProgress.getLosersSoFar());
            if (losers == null) {
                losers = losersSet;
            } else {
                losers.retainAll(losersSet);
            }

            //average the fraction of data--here we keep the running sum
            Double actionFraction = actionProgress.getFractionDataCollected();
            if (actionFraction != null) {
                numberActions += 1;

                if (fractionOfData == null) {
                    fractionOfData = actionFraction;
                } else {
                    fractionOfData += actionFraction;
                }
            }
        }

        //now divide to get the average for fraction of data
        if (fractionOfData != null) {
            fractionOfData /= numberActions;
        }

        //convert sets to lists
        winnersList = new ArrayList<>();
        if (winners != null) {
            winnersList.addAll(winners);
        }
        losersList = new ArrayList<>();
        if (losers != null) {
            losersList.addAll(losers);
        }

        sufficientData = fractionOfData != null && (Math.abs(fractionOfData - 1.0f) < FLOAT_POINT_DIFFERENCE_EPSILON);

        experiment.setExperimentProgress(new Progress.Builder().withWinnersSoFar(winnersList)
                .withLosersSoFar(losersList)
                .withSufficientData(sufficientData)
                .withFractionDataCollected(fractionOfData)
                .build());
    }

    private void CountComparisonWinnerOrAddBucketToLosers(Set<Bucket.Label> losers, Map<Bucket.Label, Integer> bucketWins, Bucket.Label bucketName, ComparisonStatistics jointComparison) {
        if (jointComparison.getClearComparisonWinner().equals(bucketName)) {
            Integer currentWins = bucketWins.get(bucketName);
            if (currentWins == null) {
                bucketWins.put(bucketName, 1);
            } else {
                bucketWins.put(bucketName, currentWins + 1);
            }
        } else {
            //or, if a bucket is worse, add it to the set of losers
            losers.add(bucketName);
        }
    }
}
