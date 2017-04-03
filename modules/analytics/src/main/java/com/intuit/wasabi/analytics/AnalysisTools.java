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
package com.intuit.wasabi.analytics;

import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics.BinomialMetric;
import com.intuit.wasabi.analyticsobjects.statistics.AbstractContainerStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.experimentobjects.Bucket;

import java.util.Collection;
import java.util.Map;

/**
 * The AnalysisTools provide counts for different containers and other statistics related to
 * {@link Bucket}s and {@link com.intuit.wasabi.experimentobjects.Experiment}s.
 */
public interface AnalysisTools {

    /**
     * Creates an ExperimentCounts object from a collection of {@link BucketCounts} objects.
     * <p>
     * Calculates experiment-level counts by summing the over the set of BucketCounts.
     * Each bucket in the experiment must appear exactly once in the collection.
     *
     * @param bucketsSet a complete collection of {@link BucketCounts} for this experiment
     * @return {@link ExperimentCounts} for those Buckets
     */
    ExperimentCounts calculateExperimentCounts(Collection<BucketCounts> bucketsSet);

    /**
     * Updates a statistics object to include rate estimates.
     *
     * @param container the statistics object, already containing counts
     * @param metric    the binomial metric to use for statistics calculations
     */
    void generateRate(AbstractContainerStatistics container, BinomialMetric metric);

    /**
     * Updates a list of incomplete {@link BucketStatistics} objects to include bucket comparisons.
     *
     * @param buckets    a complete list of BucketStatistics objects for this experiment, each already containing counts
     * @param metric     the binomial metric to use for statistics calculations
     * @param effectSize the effect size of interest to use for statistics calculations
     * @param mode       the mode of the parameters
     */
    void generateBucketComparison(Map<Bucket.Label, BucketStatistics> buckets, BinomialMetric metric,
                                  double effectSize, Parameters.Mode mode);

    /**
     * Updates an ExperimentStatistics object to include all the progress metrics.
     *
     * @param experiment the {@link ExperimentStatistics} object, already full bucket statistics
     */
    void generateProgress(ExperimentStatistics experiment);
}
