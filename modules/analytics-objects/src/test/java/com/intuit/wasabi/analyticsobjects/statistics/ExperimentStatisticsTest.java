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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link ExperimentStatistics}
 */
public class ExperimentStatisticsTest {
    private ExperimentStatistics experimentStatistics;
    private ExperimentCounts experimentCounts;
    private Progress progress;
    private Progress jointProgress;
    private Map<Event.Name, ActionCounts> actionCountsMap;
    private Map<Event.Name, ActionRate> actionRateMap;
    private Map<Event.Name, ActionProgress> actionProgressMap;
    private Map<Bucket.Label, BucketStatistics> bucketStatisticsMap;
    private Counts impressionCounts;
    private Counts actionCounts;
    private Estimate estimate;

    @Before
    public void setup() {
        experimentCounts = Mockito.mock(ExperimentCounts.class);
        progress = Mockito.mock(Progress.class);
        // all Maps cannot be mocked because the clone method explicitly creates a new hashmap when it is not null
        actionCountsMap = new HashMap<>();
        actionRateMap = new HashMap<>();
        actionProgressMap = new HashMap<>();
        bucketStatisticsMap = new HashMap<>();
        impressionCounts = Mockito.mock(Counts.class);
        actionCounts = Mockito.mock(Counts.class);
        estimate = Mockito.mock(Estimate.class);
        jointProgress = Mockito.mock(Progress.class);
        experimentStatistics = new ExperimentStatistics.Builder()
                .withExperimentCounts(experimentCounts)
                .withExperimentProgress(progress)
                .withActionCounts(actionCountsMap)
                .withActionRates(actionRateMap)
                .withActionProgress(actionProgressMap)
                .withBuckets(bucketStatisticsMap)
                .withImpressionCounts(impressionCounts)
                .withJointActionCounts(actionCounts)
                .withJointActionRate(estimate)
                .withJointProgress(jointProgress)
                .build();
    }

    @Test
    public void testBuilder() {
        ExperimentStatistics experimentStatistics = new ExperimentStatistics.Builder()
                .withExperimentCounts(experimentCounts)
                .withExperimentProgress(progress)
                .withActionCounts(actionCountsMap)
                .withActionRates(actionRateMap)
                .withActionProgress(actionProgressMap)
                .withBuckets(bucketStatisticsMap)
                .withImpressionCounts(impressionCounts)
                .withJointActionCounts(actionCounts)
                .withJointActionRate(estimate)
                .withJointProgress(jointProgress)
                .build();
        assertThat(experimentStatistics.getActionRates(), is(actionRateMap));
        assertThat(experimentStatistics.getBuckets(), is(bucketStatisticsMap));
        assertThat(experimentStatistics.getExperimentProgress(), is(progress));
        assertThat(experimentStatistics.getJointProgress(), is(jointProgress));
        assertThat(experimentStatistics.getActionProgress(), is(actionProgressMap));
    }

    @Test
    public void testClone() {
        when(progress.clone()).thenReturn(progress);
        when(impressionCounts.clone()).thenReturn(impressionCounts);
        when(actionCounts.clone()).thenReturn(actionCounts);
        when(estimate.clone()).thenReturn(estimate);
        when(jointProgress.clone()).thenReturn(jointProgress);
        ExperimentStatistics clonedExperimentStatistics = experimentStatistics.clone();
        assertThat(clonedExperimentStatistics.equals(experimentStatistics), is(true));
    }

    @Test
    public void testPojoMutationMethods() {
        //TODO: when we refactor the pojo, remove those mutator from the object itself and move it to the builder.
        experimentStatistics.addToBucketStatistics(Bucket.Label.valueOf("test"), new BucketStatistics.Builder().build());
        experimentStatistics.addToActionProgress(Event.Name.valueOf("test"), new ActionProgress.Builder().build());

        assertThat(experimentStatistics.getBuckets().size(), is(1));
        assertThat(experimentStatistics.getActionProgress().size(), is(1));
    }

}
