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
package com.intuit.wasabi.analyticsobjects.metrics;

import com.intuit.wasabi.analyticsobjects.statistics.DistinguishableEffectSize;
import com.intuit.wasabi.analyticsobjects.statistics.Estimate;
import com.intuit.wasabi.exceptions.AnalyticsException;

import java.lang.reflect.InvocationTargetException;

/**
 * Statistical functions for binomial distributions.
 */
public enum BinomialMetrics {

    NORMAL_APPROX(NormalApproxMetric.class),
    NORMAL_APPROX_SYM(NormalApproxSymMetric.class);
    private Class<BinomialMetric> binomialMetricClass;

    BinomialMetrics(Class binomialMetricClass) {
        this.binomialMetricClass = binomialMetricClass;
    }

    public BinomialMetric constructMetric(final double conf_level) {
        Class[] parameters = {double.class};

        try {
            return (BinomialMetric) binomialMetricClass.getConstructor(parameters).newInstance(conf_level);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                // Should never happen
                throw new AnalyticsException("BinomialMetric constructMetric InvocationTargetException", e);
            }
        } catch (ReflectiveOperationException e) {
            // Should never happen
            throw new AnalyticsException("BinomialMetric constructMetric ReflectiveOperationException", e);
        }
    }

    public BinomialMetric constructMetric(final double conf_level,
                                          final double max_decisions) {

        Class[] parameters = {double.class, double.class};
        try {
            return (BinomialMetric) binomialMetricClass.getConstructor(parameters).newInstance(conf_level, max_decisions);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                // Should never happen
                throw new AnalyticsException("BinomialMetric constructMetric InvocationTargetException", e);
            }
        } catch (ReflectiveOperationException e) {
            // Should never happen
            throw new AnalyticsException("BinomialMetric constructMetric ReflectiveOperationException", e);
        }
    }


    public abstract static class BinomialMetric {

        protected double confidenceLevel;
        protected double maxDecisions;

        public BinomialMetric(final double conf_level) {
            if (conf_level <= 0.0 || conf_level >= 1.0) {
                throw new IllegalArgumentException("Confidence level must be in the interval (0, 1).");
            }

            this.confidenceLevel = conf_level;
            this.maxDecisions = DecisionAdjuster.DEFAULT_MAX_DECISIONS;
        }

        public BinomialMetric(final double confidenceLevel, final double maxDecisions) {
            if (confidenceLevel <= 0.0 || confidenceLevel >= 1.0) {
                throw new IllegalArgumentException("Confidence level must be in the interval (0, 1).");
            }

            if (maxDecisions < 1.0) {
                throw new IllegalArgumentException("Maximum number of decision points must be at least 1.0.");
            }

            this.confidenceLevel = confidenceLevel;
            this.maxDecisions = maxDecisions;
        }

        public double getConfidenceLevel() {
            return this.confidenceLevel;
        }

        public double getMaxDecisions() {
            return this.maxDecisions;
        }

        /**
         * Calculates the estimate and confidence interval for the rate of a binomial distribution.
         *
         * @param number_impressions number of unique impressions
         * @param number_actions     number of unique actions
         * @return Estimate with [estimated rate, lower bound of confidence interval, upper bound of confidence interval]
         */
        public abstract Estimate estimateRate(final long number_impressions, final long number_actions);

        /**
         * Calculates the estimate and confidence interval for the difference in rates of two binomial distributions.
         *
         * @param number_impressions_1 number of unique impressions in bucket 1
         * @param number_actions_1     number of unique actions (of chosen type) in bucket 1
         * @param number_impressions_2 number of unique impressions in bucket 2
         * @param number_actions_2     number of unique actions (of chosen type) in bucket 2
         * @return Estimate with [estimated difference in rates, lower bound of confidence interval, upper bound of confidence interval]
         */
        public abstract Estimate estimateRateDifference(final long number_impressions_1, final long number_actions_1,
                                                        final long number_impressions_2, final long number_actions_2);

        /**
         * Calculates, for the current amount of data, the smallest (closest to 0) effect size that, if measured, could be confidently said to be nonzero.
         *
         * @param number_impressions_1 number of unique impressions in bucket 1
         * @param number_actions_1     number of unique actions (of chosen type) in bucket 1
         * @param number_impressions_2 number of unique impressions in bucket 2
         * @param number_actions_2     number of unique actions (of chosen type) in bucket 2
         * @return DistinguishableEffectSize with [lower boundary (negative), upper boundary (positive)]; Effect size is the difference in rates between the two buckets.
         */
        public abstract DistinguishableEffectSize distinguishableEffectSizes(final long number_impressions_1,
                                                                             final long number_actions_1,
                                                                             final long number_impressions_2,
                                                                             final long number_actions_2);

        /**
         * For a given effect size, calculates the amount of data needed to confidently say that effect size, if measured, is nonzero.
         *
         * @param number_impressions_1 number of unique impressions in bucket 1
         * @param number_actions_1     number of unique actions (of chosen type) in bucket 1
         * @param number_impressions_2 number of unique impressions in bucket 2
         * @param number_actions_2     number of unique actions (of chosen type) in bucket 2
         * @param effect_size          difference in rates between the two buckets, range: [-1, 1]
         * @return current amount of data/amount of data needed
         */
        public abstract Double fractionOfData(final long number_impressions_1, final long number_actions_1,
                                              final long number_impressions_2, final long number_actions_2,
                                              final double effect_size);

        /**
         * For a predicted action rate and effect size, calculates the estimated amount of data needed.
         *
         * @param rate                 predicted action rate, range [0, 1]
         * @param allocation_percent_1 allocation percentage for one bucket, range (0, 1)
         * @param allocation_percent_2 allocation percentage for the other bucket, range (0, 1)
         * @param effect_size          predicted difference in rates between the two buckets, range: [-1, 1]
         * @return total number of unique impressions needed in experiment (sum of unique impressions in *all* buckets)
         */
        public abstract Long predictedDataNeeded(final double rate, final double allocation_percent_1,
                                                 final double allocation_percent_2, final double effect_size);
    }
}
