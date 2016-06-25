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

import static java.lang.Math.sqrt;

/**
 * Statistical functions for binomial distributions.
 *
 * Uses a normal approximation to the binomial distribution.
 * For some methods, symmetrizes over buckets using the pooled rate.
 */
public class NormalApproxSymMetric extends com.intuit.wasabi.analyticsobjects.metrics.NormalApproxMetric {

    public NormalApproxSymMetric(final double conf_level) {
        super(conf_level);
    }

    public NormalApproxSymMetric(final double conf_level, final double max_decisions) {
        super(conf_level, max_decisions);
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
        SymmetricInputValidation(number_impressions_1, number_actions_1, number_impressions_2, number_actions_2);
        double zval = DecisionAdjuster.scaledZ(confidenceLevel, maxDecisions);
        double pooled_rate = 1.0 * (number_actions_1 + number_actions_2) / (number_impressions_1 + number_impressions_2);
        double effect_size = zval * sqrt(pooled_rate * (1 - pooled_rate) * (1.0 / number_impressions_1 + 1.0 / number_impressions_2));

        return new DistinguishableEffectSize.Builder()
                .withNegativeEffectSize(-effect_size)
                .withPositiveEffectSize(effect_size)
                .build();

    }

    /**
     * For a given effect size, calculates the amount of data needed to confidently say that effect size, if measured, is nonzero.
     *
     * Symmetrizes over the two buckets by using the pooled rate.
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
        SymmetricInputValidation(number_impressions_1, number_actions_1, number_impressions_2, number_actions_2);
        if (effect_size < -1.0 || effect_size > 1.0) {
            throw new IllegalArgumentException("Effect size must be in the interval [-1, 1].");
        }

        double zval = DecisionAdjuster.scaledZ(confidenceLevel, maxDecisions);
        double pooled_rate = 1.0 * (number_actions_1 + number_actions_2) / (number_impressions_1 + number_impressions_2);

        return 1 / (pooled_rate * (1 - pooled_rate) * (1.0 / number_impressions_1 + 1.0 / number_impressions_2) * zval * zval / effect_size / effect_size);
    }

    void SymmetricInputValidation(long number_impressions_1, long number_actions_1, long number_impressions_2, long number_actions_2) {
        inputValidation(number_impressions_1, number_actions_1, number_impressions_2);
        if (number_actions_2 < 0) {
            throw new IllegalArgumentException("Number of unique actions must be nonnegative.");
        }
        if (number_actions_2 > number_impressions_2) {
            throw new IllegalArgumentException("Number of unique actions cannot exceed number of unique impressions.");
        }
    }


}
