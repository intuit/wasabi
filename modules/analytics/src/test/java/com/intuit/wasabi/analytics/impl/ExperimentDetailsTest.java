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
package com.intuit.wasabi.analytics.impl;

import com.intuit.wasabi.analytics.Analytics;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.analyticsobjects.counts.TotalUsers;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.Estimate;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.Progress;
import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.experiment.Buckets;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Mockito.*;

/**
 * Test class for the {@link ExperimentDetailsImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class ExperimentDetailsTest{

    @Mock
    Buckets buckets;
    @Mock
    Analytics analytics;
    @Mock
    ExperimentRepository cassandraRepo;
    @Mock
    ExperimentStatistics expStats;

    ExperimentDetailsImpl expDetails;

    @Before
    public void setUp(){
        expDetails = new ExperimentDetailsImpl(cassandraRepo,buckets,analytics);
    }

    @Test
    public void testDraftExperiment(){
        ExperimentDetail expDetail = mock(ExperimentDetail.class);
        when(expDetail.getState()).thenReturn(Experiment.State.DRAFT);
        List<ExperimentDetail> details = new ArrayList<>();
        details.add(expDetail);

        expDetails.getAnalyticData(details, mock(Parameters.class));
        verify(expDetail).getState();
        //when the experiment is in draft state no information is added
        verifyNoMoreInteractions(expDetail);
    }

    @Test
    public void testAnalyticsDataExperiment(){
        ExperimentDetail expDetail = mock(ExperimentDetail.class);
        when(expDetail.getState()).thenReturn(Experiment.State.RUNNING);
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        when(expDetail.getId()).thenReturn(expId);
        when(expDetail.getStartTime()).thenReturn(new Date());

        AssignmentCounts counts = mock(AssignmentCounts.class);
        TotalUsers totUsers = mock(TotalUsers.class);
        when(counts.getTotalUsers()).thenReturn(totUsers);
        when(counts.getTotalUsers().getTotal()).thenReturn(42l);
        when(analytics.getAssignmentCounts(any(), any(Context.class))).thenReturn(counts);

        when(analytics.getExperimentStatistics(eq(expId), any(Parameters.class))).thenReturn(expStats);

        List<ExperimentDetail> details = new ArrayList<>();
        details.add(expDetail);

        expDetails.getAnalyticData(details, mock(Parameters.class));
        verify(expDetail).setTotalNumberUsers(42l);
    }

    @Test
    public void testAnalyticsDataBuckets(){
        ExperimentDetail expDetail = mock(ExperimentDetail.class);

        when(expDetail.getStartTime()).thenReturn(new DateTime().minusDays(8).toDate());
        Bucket.Label b1Label = Bucket.Label.valueOf("BucketLabel1");
        Bucket.Label b2Label = Bucket.Label.valueOf("BucketLabel2");

        //create buckets
        ExperimentDetail.BucketDetail bd1 = mock(ExperimentDetail.BucketDetail.class);
        when(bd1.getLabel()).thenReturn(b1Label);
        ExperimentDetail.BucketDetail bd2 = mock(ExperimentDetail.BucketDetail.class);
        when(bd2.getLabel()).thenReturn(b2Label);

        List<ExperimentDetail.BucketDetail> bucketDetails = new ArrayList<>();
        bucketDetails.add(bd1);
        bucketDetails.add(bd2);

        BucketStatistics bs1 = mock(BucketStatistics.class);
        when(bs1.getLabel()).thenReturn(b1Label);
        when(bs1.getJointActionRate()).thenReturn(mock(Estimate.class));
        when(bs1.getJointActionRate().getEstimate()).thenReturn(0.42);
        when(bs1.getImpressionCounts()).thenReturn(mock(Counts.class));

        BucketStatistics bs2 = mock(BucketStatistics.class);
        when(bs2.getLabel()).thenReturn(b2Label);
        when(bs2.getJointActionRate()).thenReturn(mock(Estimate.class));
        when(bs2.getJointActionRate().getEstimate()).thenReturn(0.99);
        when(bs2.getImpressionCounts()).thenReturn(mock(Counts.class));

        Map<Bucket.Label, BucketStatistics> bucketAnalytics = new HashMap<>();
        bucketAnalytics.put(b1Label, bs1);
        bucketAnalytics.put(b2Label, bs2);

        when(expDetail.getBuckets()).thenReturn(bucketDetails);
        when(expStats.getBuckets()).thenReturn(bucketAnalytics);
        when(expStats.getJointProgress()).thenReturn(mock(Progress.class));
        List<Bucket.Label> winnerSoFar = new ArrayList<>();
        winnerSoFar.add(b1Label);

        when(expStats.getJointProgress().getWinnersSoFar()).thenReturn(winnerSoFar);

        //check if winner so far works
        expDetails.getBucketDetails(expDetail, expStats);

        verify(bd1).setActionRate(0.42);
        verify(bd2).setActionRate(0.99);
        verify(bd1).setWinnerSoFar(true);
        verify(bd2, never()).setWinnerSoFar(true);

    }



}
