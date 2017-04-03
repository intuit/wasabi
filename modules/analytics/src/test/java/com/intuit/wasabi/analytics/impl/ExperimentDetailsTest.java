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
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for the {@link ExperimentDetailsImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class ExperimentDetailsTest {

    @Mock
    private Buckets buckets;
    @Mock
    private Analytics analytics;
    @Mock
    private ExperimentRepository cassandraRepo;
    @Mock
    private ExperimentStatistics expStats;

    ExperimentDetailsImpl expDetails;
    Experiment exp;
    Bucket b1;
    Experiment.ID expId = Experiment.ID.newInstance();

    @Before
    public void setUp() {
        expDetails = new ExperimentDetailsImpl(cassandraRepo, buckets, analytics);

        exp = Experiment.withID(expId).withState(Experiment.State.DRAFT)
                .withLabel(Experiment.Label.valueOf("ExpLabel"))
                .withApplicationName(Application.Name.valueOf("AppName")).build();

        b1 = Bucket.newInstance(expId, Bucket.Label.valueOf("Bucket1")).withAllocationPercent(0.5)
                .withState(Bucket.State.OPEN).build();
    }

    @Test
    public void testDraftExperiment() {
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
    public void testBasicData() {

        List<Experiment.ID> expIds = new ArrayList<>();
        expIds.add(expId);

        List<Experiment> exps = new ArrayList<>();
        exps.add(exp);

        ExperimentList experimentList = mock(ExperimentList.class);

        when(cassandraRepo.getExperiments()).thenReturn(expIds);
        when(cassandraRepo.getExperiments(expIds)).thenReturn(experimentList);
        when(experimentList.getExperiments()).thenReturn(exps);

        BucketList buckList = new BucketList();
        buckList.addBucket(b1);

        when(buckets.getBuckets(expId, false)).thenReturn(buckList);

        List<ExperimentDetail> expDetail = expDetails.getExperimentDetailsBase();

        ExperimentDetail.BucketDetail bucketDetail = expDetail.get(0).getBuckets().get(0);

        assertThat(bucketDetail.getState(), is(Bucket.State.OPEN));
        assertThat(bucketDetail.getAllocationPercent(), is(0.5));
        assertThat(bucketDetail.getLabel(), is(Bucket.Label.valueOf("Bucket1")));
    }

    @Test
    public void testAnalyticsDataExperiment() {
        ExperimentDetail expDetail = mock(ExperimentDetail.class);
        when(expDetail.getState()).thenReturn(Experiment.State.RUNNING);
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        when(expDetail.getId()).thenReturn(expId);
        when(expDetail.getStartTime()).thenReturn(new Date());

        AssignmentCounts counts = mock(AssignmentCounts.class);
        TotalUsers totUsers = mock(TotalUsers.class);
        when(counts.getTotalUsers()).thenReturn(totUsers);
        when(counts.getTotalUsers().getBucketAssignments()).thenReturn(42l);
        when(analytics.getAssignmentCounts(any(), any(Context.class))).thenReturn(counts);

        when(analytics.getExperimentStatistics(eq(expId), any(Parameters.class))).thenReturn(expStats);

        List<ExperimentDetail> details = new ArrayList<>();
        details.add(expDetail);

        expDetails.getAnalyticData(details, mock(Parameters.class));
        verify(expDetail).setTotalNumberUsers(42l);
    }

    @Test
    public void testAnalyticsDataBuckets() {
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
        Set<Bucket.Label> winnerSoFar = new HashSet<>();
        winnerSoFar.add(b1Label);

        when(expStats.getJointProgress().getWinnersSoFar()).thenReturn(winnerSoFar);

        //check if winner so far works
        AssignmentCounts assignmentCounts = mock(AssignmentCounts.class);
        when(assignmentCounts.getAssignments()).thenReturn(new ArrayList<>());
        expDetails.getBucketDetails(expDetail, expStats, assignmentCounts);

        verify(bd1).setActionRate(0.42);
        verify(bd2).setActionRate(0.99);
        verify(bd1).setWinnerSoFar(true);
        verify(bd2, never()).setWinnerSoFar(true);

    }


}
