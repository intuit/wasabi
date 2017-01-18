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

import static cern.jet.stat.Probability.normalInverse;
import static java.lang.Math.log;

/**
 * Helper class to support scaling operations
 */
public class DecisionAdjuster {

    public static final long DEFAULT_MIN_IMPRESSIONS = 1;
    public static final long DEFAULT_MAX_IMPRESSIONS = (long) Math.pow(10, 10);
    public static final double DEFAULT_MAX_DECISIONS = calcMaxDecisions(DEFAULT_MIN_IMPRESSIONS, DEFAULT_MAX_IMPRESSIONS);

    private DecisionAdjuster() {
        // do nothing
    }

    /**
     * Calculates the scaled, two-sided "Z-value" for a given confidence level.
     *
     * For a normal distribution, the Z-value is the number of standard deviations from the mean corresponding to
     * the given confidence level.  A two-sided value distributes the confidence region equally around the mean of the
     * distribution.  The Z-value is scaled in order to account for multiple decision points throughout the experiment.
     *
     * @param confidence_level a value in the interval (0, 1); e.g. use 0.95 for 95%
     * @param max_decisions    the number of 'independent' decision points that might occur, a reasonable value is 23
     * @return scaled, two-sided Z-value
     */
    public static Double scaledZ(final double confidence_level, final double max_decisions) {
        if (confidence_level <= 0.0 || confidence_level >= 1.0) {
            throw new IllegalArgumentException("Confidence level must be in the interval (0, 1).");
        }

        if (max_decisions <= 0.0) {
            throw new IllegalArgumentException("Scale factor must be positive.");
        }

        double significance_level = 1.0 - confidence_level;

        return normalInverse(1.0 - significance_level / max_decisions / 2.);
    }

    /**
     * Calculates the scaled, two-sided "Z-value" for a given confidence level and the default number of decision points.
     *
     * For a normal distribution, the Z-value is the number of standard deviations from the mean corresponding to
     * the given confidence level.  A two-sided value distributes the confidence region equally around the mean of the
     * distribution.  The Z-value is scaled in order to account for multiple decision points throughout the experiment.
     *
     * @param confidence_level a value in the interval (0, 1); e.g. use 0.95 for 95%
     * @return scaled, two-sided Z-value
     */
    public static Double scaledZ(final double confidence_level) {
        if (confidence_level <= 0.0 || confidence_level >= 1.0) {
            throw new IllegalArgumentException("Confidence level must be in the interval (0, 1).");
        }

        double significance_level = 1.0 - confidence_level;

        return normalInverse(1.0 - significance_level / DEFAULT_MAX_DECISIONS / 2.);
    }

    /**
     * Calculates the scale factor to be used in adjusting for continuous monitoring of experiments.
     *
     * The scale factor can be thought of as the maximum number of 'independent' decision points that might exist during
     * the course of an experiment.
     *
     * @param min_impressions the maximum number of unique impressions the experiment might have, suggestion: 10^10
     * @param max_impressions the minimum number of unique impressions before a decision might be made, suggestion: 100
     * @return the scale factor, or number of 'independent' decision points
     */
    public static Double calcMaxDecisions(final long min_impressions, final long max_impressions) {
        if (min_impressions <= 0) {
            throw new IllegalArgumentException("Minimum number of unique impressions must be positive.");
        }

        if (max_impressions <= 0) {
            throw new IllegalArgumentException("Maximum number of unique impressions must be positive.");
        }

        return log(1.0 * max_impressions / min_impressions);
    }
}
