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

import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics.BinomialMetric;
import com.intuit.wasabi.analyticsobjects.statistics.DistinguishableEffectSize;
import com.intuit.wasabi.analyticsobjects.statistics.Estimate;

import static java.lang.Math.sqrt;


/**
 * Statistical functions for binomial distributions.
 *
 * Uses a normal approximation to the binomial distribution.
 */
public class NormalApproxMetric extends BinomialMetric {

    public NormalApproxMetric(final double conf_level) {
        super(conf_level);
    }

    public NormalApproxMetric(final double conf_level, final double max_decisions) {
        super(conf_level, max_decisions);
    }


    /**
     * Calculates the estimate and confidence interval for the rate of a binomial distribution.
     *
     * Uses the Agresti-Coull confidence interval.
     *
     * @param number_impressions number of unique impressions
     * @param number_actions     number of unique actions
     * @return Estimate with [estimated rate, lower bound of confidence interval, upper bound of confidence interval]
     * @see <a href="http://projecteuclid.org/DPubS?service=UI&version=1.0&verb=Display&handle=euclid.ss/1009213286">Interval Estimation for a Binomial Proportion</a>
     */
    @Override
    public Estimate estimateRate(final long number_impressions, final long number_actions) {
        if (number_impressions < 0) {
            throw new IllegalArgumentException("Number of unique impressions must be nonnegative.");
        }
        if (number_actions < 0) {
            throw new IllegalArgumentException("Number of unique actions must be nonnegative.");
        }
        if (number_actions > number_impressions) {
            throw new IllegalArgumentException("Number of unique actions cannot exceed number of unique impressions.");
        }

        double zval = DecisionAdjuster.scaledZ(confidenceLevel, maxDecisions);
        double mid = (number_actions + 0.5 * zval * zval) / (number_impressions + zval * zval);
        double confint = zval * sqrt(mid * (1 - mid) / (number_impressions + zval * zval));

        return new Estimate.Builder().withEstimate(1.0 * number_actions / number_impressions)
                .withLowerBound(mid - confint)
                .withUpperBound(mid + confint).build();
    }

    /**
     * Calculates the estimate and confidence interval for the difference in rates of two binomial distributions.
     *
     * Uses a two-proportion hypothesis test (unpooled).
     *
     * @param number_impressions_1 number of unique impressions in bucket 1
     * @param number_actions_1     number of unique actions (of chosen type) in bucket 1
     * @param number_impressions_2 number of unique impressions in bucket 2
     * @param number_actions_2     number of unique actions (of chosen type) in bucket 2
     * @return Estimate with [estimated difference in rates, lower bound of confidence interval, upper bound of confidence interval]
     */
    @Override
    public Estimate estimateRateDifference(final long number_impressions_1, final long number_actions_1,
                                           final long number_impressions_2, final long number_actions_2) {
        inputValidation(number_impressions_1, number_actions_1, number_impressions_2);
        if (number_actions_2 < 0) {
            throw new IllegalArgumentException("Number of unique actions must be nonnegative.");
        }
        if (number_actions_2 > number_impressions_2) {
            throw new IllegalArgumentException("Number of unique actions cannot exceed number of unique impressions.");
        }

        double zval = DecisionAdjuster.scaledZ(confidenceLevel, maxDecisions);
        double rate_1 = 1.0 * number_actions_1 / number_impressions_1;
        double rate_2 = 1.0 * number_actions_2 / number_impressions_2;
        double rate_diff = rate_1 - rate_2;
        double confint = zval * sqrt(rate_1 * (1 - rate_1) / number_impressions_1 + rate_2 * (1 - rate_2) / number_impressions_2);

        return new Estimate.Builder().withEstimate(rate_diff)
                .withLowerBound(rate_diff - confint)
                .withUpperBound(rate_diff + confint).build();
    }

    /**
     * Calculates, for the current amount of data, the smallest (closest to 0) effect size that, if measured, could be confidently said to be nonzero.
     *
     * This is NOT a symmetric function; it takes rate_1 as a constant and considers effect size as a function of rate_2.
     *
     * @param number_impressions_1 number of unique impressions in bucket 1
     * @param number_actions_1     number of unique actions (of chosen type) in bucket 1
     * @param number_impressions_2 number of unique impressions in bucket 2
     * @param number_actions_2     number of unique actions (of chosen type) in bucket 2
     * @return DistinguishableEffectSize with [lower boundary (negative), upper boundary (positive)]; Effect size is the difference in rates between the two buckets.
     */
    @Override
    public DistinguishableEffectSize distinguishableEffectSizes(final long number_impressions_1,
                                                                final long number_actions_1,
                                                                final long number_impressions_2,
                                                                final long number_actions_2) {
        inputValidation(number_impressions_1, number_actions_1, number_impressions_2);

        double zval = DecisionAdjuster.scaledZ(confidenceLevel, maxDecisions);
        double rate_1 = 1.0 * number_actions_1 / number_impressions_1;
        double scale_1 = zval * zval / number_impressions_1;
        double scale_2 = zval * zval / number_impressions_2;
        double a = 1 + scale_2;
        double b = -(2 * rate_1 + scale_2);
        double c = rate_1 * rate_1 * (1 + scale_1) - rate_1 * scale_1;

        return new DistinguishableEffectSize.Builder()
                .withNegativeEffectSize(rate_1 - (-b + sqrt(b * b - 4 * a * c)) / (2 * a))
                .withPositiveEffectSize(rate_1 - (-b - sqrt(b * b - 4 * a * c)) / (2 * a))
                .build();
    }

    void inputValidation(long number_impressions_1, long number_actions_1, long number_impressions_2) {
        if (number_impressions_1 < 0) {
            throw new IllegalArgumentException("Number of unique impressions must be nonnegative.");
        }
        if (number_actions_1 < 0) {
            throw new IllegalArgumentException("Number of unique actions must be nonnegative.");
        }
        if (number_actions_1 > number_impressions_1) {
            throw new IllegalArgumentException("Number of unique actions cannot exceed number of unique impressions.");
        }
        if (number_impressions_2 < 0) {
            throw new IllegalArgumentException("Number of unique impressions must be nonnegative.");
        }
    }

    /**
     * For a given effect size, calculates the amount of data needed to confidently say that effect size, if measured, is nonzero.
     *
     * This is NOT a symmetric function; it takes rate_1 as a constant and considers rate_2 as a function of effect_size.
     *
     * @param number_impressions_1 number of unique impressions in bucket 1
     * @param number_actions_1     number of unique actions (of chosen type) in bucket 1
     * @param number_impressions_2 number of unique impressions in bucket 2
     * @param number_actions_2     number of unique actions (of chosen type) in bucket 2
     * @param effect_size          difference in rates between the two buckets, range: [-1, 1]
     * @return current amount of data/amount of data needed
     */
    @Override
    public Double fractionOfData(final long number_impressions_1, final long number_actions_1,
                                 final long number_impressions_2, final long number_actions_2,
                                 final double effect_size) {
        inputValidation(number_impressions_1, number_actions_1, number_impressions_2);

        if (effect_size < -1.0 || effect_size > 1.0) {
            throw new IllegalArgumentException("Effect size must be in the interval [-1, 1].");
        }

        double zval = DecisionAdjuster.scaledZ(confidenceLevel, maxDecisions);
        double rate_1 = 1.0 * number_actions_1 / number_impressions_1;
        double rate_2 = rate_1 - effect_size;

        return 1 / ((rate_1 * (1 - rate_1) / number_impressions_1 + rate_2 * (1 - rate_2) / number_impressions_2) * zval * zval / effect_size / effect_size);
    }

    /**
     * For a predicted action rate and effect size, calculates the estimated amount of data needed.
     *
     * Uses the pooled action rate for two buckets, which is calculated as:
     *
     * pooled_rate = (number_actions_1 + number_actions_2) / (number_impressions_1 + number_impressions_2).
     *
     * For small effect sizes the pooled action rate is approximately equal to the action rate for either bucket.
     * For similar allocation percentages, the pooled action rate is approximately equal to the average of the action rates.
     *
     * @param rate                 predicted action rate, range [0, 1]
     * @param allocation_percent_1 allocation percentage for one bucket, range (0, 1)
     * @param allocation_percent_2 allocation percentage for the other bucket, range (0, 1)
     * @param effect_size          predicted difference in rates between the two buckets, range: [-1, 1]
     * @return total number of unique impressions needed in experiment (sum of unique impressions in *all* buckets)
     */
    @Override
    public Long predictedDataNeeded(final double rate, final double allocation_percent_1,
                                    final double allocation_percent_2, final double effect_size) {
        if (rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("Action rate must be in the interval [0, 1].");
        }
        if (allocation_percent_1 <= 0.0 || allocation_percent_1 >= 1.0) {
            throw new IllegalArgumentException("Allocation percent must be in the interval (0, 1).");
        }
        if (allocation_percent_2 <= 0.0 || allocation_percent_2 >= 1.0) {
            throw new IllegalArgumentException("Allocation percent must be in the interval (0, 1).");
        }
        if (effect_size < -1.0 || effect_size > 1.0) {
            throw new IllegalArgumentException("Effect size must be in the interval [-1, 1].");
        }
        if (Double.doubleToRawLongBits(effect_size) == 0L) {
            throw new IllegalArgumentException("Effect size must not equal 0");
        }

        double zval = DecisionAdjuster.scaledZ(confidenceLevel, maxDecisions);
        return Math.round(rate * (1 - rate) * (1.0 / allocation_percent_1 + 1.0 / allocation_percent_2) * zval * zval / effect_size / effect_size);
    }

}
