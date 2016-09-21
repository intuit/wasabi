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
import com.intuit.wasabi.analytics.ExperimentDetails;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.experiment.*;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.DatabaseRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ExperimentDetails}
 */
public class ExperimentDetailsImpl implements ExperimentDetails{

    private final ExperimentRepository databaseRepository;
    private final ExperimentRepository cassandraRepository;
    private final Buckets buckets;
    private final Analytics analytics;


    /**
     * Constructor of the ExperimentDetails.
     *
     * @param databaseRepository the mssql database used for analytic data
     * @param cassandraRepository repository for the experiment information
     * @param buckets access to the bucket information
     * @param analytics the analytics module that holds the methods to get bucket details and counts
     *                  for running experiments
     */
    @Inject
    public ExperimentDetailsImpl(@DatabaseRepository ExperimentRepository databaseRepository,
                                 @CassandraRepository ExperimentRepository cassandraRepository,
                                 Buckets buckets, Analytics analytics) {
        super();
        this.databaseRepository = databaseRepository;
        this.cassandraRepository = cassandraRepository;
        this.buckets = buckets;
        this.analytics = analytics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExperimentDetail> getExperimentDetailsBase() {
        List<Experiment.ID> experimentIDs = cassandraRepository.getExperiments();
        List<Experiment> exps = cassandraRepository.getExperiments(experimentIDs).getExperiments();

        List<ExperimentDetail> details = new ArrayList<>();
        exps.forEach(e -> details.add(new ExperimentDetail(e)));

        //add bucket information
        details.parallelStream().forEach(expd -> getBucketData(expd));

        return details;
    }

    /**
     * Queries the database to get additional information to the buckets for the provided
     * experiment, like the label and allocation percentage. For the analytics data per bucket
     * see {@link #getAnalyticData(List, Parameters)}.
     *
     * @param exp the experiment that should be enriched with bucket data
     * @return the same ExperimentDetail object but with additional information
     */
    private ExperimentDetail getBucketData(ExperimentDetail exp){
        List<Bucket> buckList = buckets.getBuckets(exp.getId()).getBuckets();
        exp.addBuckets(buckList);

        return exp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExperimentDetail> getAnalyticData(List<ExperimentDetail> details, Parameters params) {

        details.parallelStream().forEach(expd -> getAnalyticData(expd, params));

        return details;
    }

    /**
     * Retrieves the analytics data for the buckets and the experiment itself.
     *
     * @param experimentDetail the {@link ExperimentDetail} that needs to be enhanced with analytics data
     * @param params {@link Parameters} for the Analytics calls- containing the context for example
     * @return the same object with additional analytic information
     */
     /*test*/ ExperimentDetail getAnalyticData(ExperimentDetail experimentDetail, Parameters params){

        // analytics data is only necessary for running/paused/terminated experiments
        if(!experimentDetail.getState().equals(Experiment.State.DRAFT)) {

            //experiment level analytics
            AssignmentCounts assignmentCounts = analytics.getAssignmentCounts(experimentDetail.getId(),
                                                                                params.getContext());
            if (assignmentCounts != null) {
                long totalAssignments = assignmentCounts.getTotalUsers().getTotal();
                experimentDetail.setTotalNumberUsers(totalAssignments);
            }

            ExperimentStatistics expStats = analytics.getExperimentStatistics(experimentDetail.getId(), params);
            getBucketDetails(experimentDetail,expStats);

        }
        return experimentDetail;
    }

    /**
     * Encapsulates the AnalyticsData retrieval for the Buckets of an Experiment.
     *
     * @param experimentDetail the {@link ExperimentDetail} of which the Bucketinformation is retrieved
     * @param expStats the ExperimentStatistics belonging to this Experiment
     */
     /*test*/ void getBucketDetails(ExperimentDetail experimentDetail, ExperimentStatistics expStats){
        DateTime aWeekAgo = new DateTime().minusDays(7);
        //winner so far is only determined if the experiment ran at least a week
        boolean checkWinnerSoFar = experimentDetail.getStartTime().before(aWeekAgo.toDate());

        Map<Label, BucketStatistics> bucketAnalytics = expStats.getBuckets();

        for(ExperimentDetail.BucketDetail b : experimentDetail.getBuckets()){
            BucketStatistics bucketStat = bucketAnalytics.get(b.getLabel());

            if(bucketStat.getJointActionRate() != null) {
                b.setActionRate(bucketStat.getJointActionRate().getEstimate());
                b.setLowerBound(bucketStat.getJointActionRate().getLowerBound());
                b.setUpperBound(bucketStat.getJointActionRate().getUpperBound());
            }

            b.setUserCount(bucketStat.getImpressionCounts().getUniqueUserCount());

            if(checkWinnerSoFar){
                for(Bucket.Label winner : expStats.getJointProgress().getWinnersSoFar()){
                    if(b.getLabel().equals(winner)){
                        b.setWinnerSoFar(true);
                        break;
                    }
                }
            }
        }
    }
}
