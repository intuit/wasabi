/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created on 2/25/16.
 */
public class ExperimentStatisticsTest {
    ExperimentStatistics experimentStatistics;
    ExperimentCounts experimentCounts;
    Progress progress;
    Progress jointProgress;
    Map<Event.Name, ActionCounts> actionCountsMap;
    Map<Event.Name, ActionRate> actionRateMap;
    Map<Event.Name, ActionProgress> actionProgressMap;
    Map<Bucket.Label, BucketStatistics> bucketStatisticsMap;
    Counts impressionCounts;
    Counts actionCounts;
    Estimate estimate;

    @Before
    public void setup(){
        experimentCounts = Mockito.mock(ExperimentCounts.class);
        progress = Mockito.mock(Progress.class);
        //all Maps cannot be mocked because the clone method explicitly create a new hashmap when it is not null
        actionCountsMap = new HashMap<>();
        actionRateMap = new HashMap<>();
        actionProgressMap =  new HashMap<>();
        bucketStatisticsMap =  new HashMap<>();
        impressionCounts =  Mockito.mock(Counts.class);
        actionCounts =  Mockito.mock(Counts.class);
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
    public void testBuilder(){
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
    public void testClone(){
        when(progress.clone()).thenReturn(progress);
        when(impressionCounts.clone()).thenReturn(impressionCounts);
        when(actionCounts.clone()).thenReturn(actionCounts);
        when(estimate.clone()).thenReturn(estimate);
        when(jointProgress.clone()).thenReturn(jointProgress);
        ExperimentStatistics clonedExperimentStatistics = experimentStatistics.clone();
        assertThat(clonedExperimentStatistics.equals(experimentStatistics), is(true));
    }

    @Test
    public void testPojoMutationMethods(){
        //TODO: when we refactor the pojo, remove those mutator from the object it self and move it to the bulider.
        experimentStatistics.addToBucketStatistics(Bucket.Label.valueOf("test"), new BucketStatistics.Builder().build());
        experimentStatistics.addToActionProgress(Event.Name.valueOf("test"), new ActionProgress.Builder().build());

        assertThat(experimentStatistics.getBuckets().size(), is(1));
        assertThat(experimentStatistics.getActionProgress().size(), is(1));
    }

}
