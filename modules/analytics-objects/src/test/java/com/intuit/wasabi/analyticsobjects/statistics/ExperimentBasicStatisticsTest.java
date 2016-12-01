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
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for the {@link ExperimentBasicStatistics}
 */
public class ExperimentBasicStatisticsTest {

    @Test
    public void testObjectsEqual() {
        ExperimentBasicStatistics ebs1 = new ExperimentBasicStatistics.Builder()
                .withJointActionCounts(new Counts.Builder().withEventCount(1).build()).build();
        ExperimentBasicStatistics ebs2 = new ExperimentBasicStatistics.Builder()
                .withJointActionCounts(new Counts.Builder().withEventCount(1).build()).build();
        assertThat(ebs1, equalTo(ebs2));
    }

    @Test
    public void testCloneObjectsEqual() {
        ExperimentBasicStatistics ebs1 = new ExperimentBasicStatistics.Builder()
                .withJointActionCounts(new Counts.Builder().withEventCount(1).build()).build();
        ExperimentBasicStatistics ebs2 = ebs1.clone();
        assertThat(ebs1, equalTo(ebs2));
    }

    @Test
    public void testObjectsNotEqual() {
        ExperimentBasicStatistics ebs1 = new ExperimentBasicStatistics.Builder()
                .withJointActionCounts(new Counts.Builder().withEventCount(1).build()).build();
        ExperimentBasicStatistics ebs2 = new ExperimentBasicStatistics.Builder()
                .withJointActionCounts(new Counts.Builder().withEventCount(2).build()).build();
        assertThat(ebs1, not(equalTo(ebs2)));
    }


    @Test
    public void testBuilder() {
        ExperimentCounts experimentCounts = new ExperimentCounts.Builder().build();
        Map<Bucket.Label, BucketBasicStatistics> bucketBasicStatisticsMap = new HashMap<>();
        Map<Event.Name, ActionCounts> actionCountsMap = new HashMap<>();
        Map<Event.Name, ActionRate> actionRateMap = new HashMap<>();
        Estimate estimate = new Estimate.Builder().build();
        Counts counts = new Counts.Builder().build();
        ExperimentBasicStatistics experimentBasicStatistics = new ExperimentBasicStatistics.Builder()
                .withExperimentCounts(experimentCounts)
                .withBuckets(bucketBasicStatisticsMap)
                .withActionCounts(actionCountsMap)
                .withImpressionCounts(counts)
                .withJointActionCounts(counts)
                .withJointActionRate(estimate)
                .withActionRates(actionRateMap).build();

        assertThat(experimentBasicStatistics.getActionRates().size(), is(0));
        assertThat(experimentBasicStatistics.getActionCounts().size(), is(0));
        assertThat(experimentBasicStatistics.getBuckets().size(), is(0));
        assertThat(experimentBasicStatistics.getJointActionRate(), is(estimate));
        assertThat(experimentBasicStatistics.getImpressionCounts(), is(counts));
        assertThat(experimentBasicStatistics.getJointActionCounts(), is(counts));

        ExperimentBasicStatistics cloned = experimentBasicStatistics.clone();

        assertThat(cloned.equals(experimentBasicStatistics), is(true));
        assertThat(cloned.hashCode(), is(experimentBasicStatistics.hashCode()));
        assertThat(cloned.toString(), is(experimentBasicStatistics.toString()));
    }
}
