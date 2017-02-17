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

import com.intuit.wasabi.analytics.AnalysisTools;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AnalyticsRepository;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created on 2/25/16.
 */

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsImplTest {

    @Mock
    Experiments experiments;
    @Mock
    AssignmentsRepository assignmentsRepository;
    @Mock
    TransactionFactory transactionFactory;
    @Mock
    AnalyticsRepository analyticsRepository;
    @Mock
    AnalysisTools analysisTools;
    @Mock
    ExperimentRepository experimentRepository;
    private AnalyticsImpl analyticsImpl;

    @Before
    public void setup() {

        analyticsImpl = new AnalyticsImpl(experiments, assignmentsRepository, transactionFactory,
                analyticsRepository, analysisTools, experimentRepository);
    }

    @Test(expected = ExperimentNotFoundException.class)
    public void getExperimentIfExistsTest() {
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class);
        when(experiments.getExperiment(eq(id))).thenReturn(experiment);
        Experiment result = this.analyticsImpl.getExperimentIfExists(id);
        assertThat(result, is(experiment));
        when(experiments.getExperiment(not(eq(id)))).thenReturn(null);
        result = this.analyticsImpl.getExperimentIfExists(Experiment.ID.newInstance());
        Assert.fail();
    }

    @Test
    public void circumventRollupTest() {
        Experiment experiment = mock(Experiment.class);
        Parameters parameters = mock(Parameters.class);
        Date date = mock(Date.class);
        Experiment.ID id = Experiment.ID.newInstance();
        when(experiments.getExperiment(eq(id))).thenReturn(experiment);
        when(parameters.getToTime()).thenReturn(date);
        //the if condition
        when(parameters.getFromTime()).thenReturn(null);
        boolean result = this.analyticsImpl.circumventRollup(id, parameters);
        assertThat(result, is(true));
        //else if (to != null) condition
        List<String> list = mock(List.class);
        when(parameters.getFromTime()).thenReturn(null);
        when(parameters.getActions()).thenReturn(null);
        when(analyticsRepository.checkMostRecentRollup(eq(experiment), eq(parameters), eq(date)))
                .thenReturn(false);
        result = this.analyticsImpl.circumventRollup(id, parameters);
        assertThat(result, is(false));
        //else condition
        when(parameters.getToTime()).thenReturn(null);
        result = this.analyticsImpl.circumventRollup(id, parameters);
        assertThat(result, is(false));
    }

    @Test
    public void calculateBucketStatisticsTest() {
        Map<Bucket.Label, BucketCounts> buckets = new HashMap<Bucket.Label, BucketCounts>();
        buckets.put(Bucket.Label.valueOf("Test"), new BucketCounts.Builder().build());
        BinomialMetrics.BinomialMetric binomialMetric = mock(BinomialMetrics.BinomialMetric.class);
        Map<Bucket.Label, BucketStatistics> result = this.analyticsImpl.calculateBucketStatistics(buckets,
                binomialMetric, 1.0, Parameters.Mode.TEST);
        assertThat(result.size(), is(1));

    }

    @Test
    public void getAssignmentCountsTest() {
        AssignmentCounts assignmentCounts = mock(AssignmentCounts.class);
        Experiment experiment = mock(Experiment.class);
        Experiment.ID id = Experiment.ID.newInstance();
        when(experimentRepository.getExperiment(eq(id))).thenReturn(experiment);
        when(assignmentsRepository.getBucketAssignmentCount(eq(experiment))).thenReturn(assignmentCounts);
        //test the else part
        Date date = new Date(1000); //some time in 1970
        when(experiment.getCreationTime()).thenReturn(date);
        AssignmentCounts result = this.analyticsImpl.getAssignmentCounts(id, null);
        assertThat(result, is(assignmentCounts));
        //test the if part
        date = new Date(); //always now
        when(experiment.getCreationTime()).thenReturn(date);
        result = this.analyticsImpl.getAssignmentCounts(id, null);
        assertThat(result, is(assignmentCounts));
    }

    @Ignore /* TO BE IMPLEMENTED */
    public void getExperimentStatisticsDailiesTest() {
        Assert.fail();
    }

    @Test
    public void calculateExperimentStatisticsTest() {
        ExperimentCounts experimentCounts = mock(ExperimentCounts.class);
        Map buckets = mock(Map.class);
        BinomialMetrics.BinomialMetric binomialMetric = mock(BinomialMetrics.BinomialMetric.class);
        when(experimentCounts.getBuckets()).thenReturn(buckets);
        ExperimentStatistics result = this.analyticsImpl.calculateExperimentStatistics(experimentCounts, binomialMetric, 1.0
                , Parameters.Mode.TEST);
        assertNotNull(result);
    }

    @Ignore /* TO BE IMPLEMENTED */
    public void getExperimentStatisticsTest() {
        AnalyticsImpl analyticsImpl = spy(new AnalyticsImpl(experiments, assignmentsRepository, transactionFactory,
                analyticsRepository, analysisTools, experimentRepository));
        Assert.fail();
    }

    @Ignore  /* TO BE IMPLEMENTED */
    public void getExperimentCountsDailiesTest() {
        Experiment experiment = mock(Experiment.class);
        Experiment.ID id = Experiment.ID.newInstance();
        when(experiments.getExperiment(eq(id))).thenReturn(experiment);
    }

    @Ignore
    public void getExperimentRollupDailiesTest() {

    }

    @Ignore
    public void getExperimentCountsTest() {

    }

    @Ignore
    public void getExperimentRollupTest() {

    }
}
