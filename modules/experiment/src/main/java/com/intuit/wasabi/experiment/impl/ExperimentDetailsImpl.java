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
package com.intuit.wasabi.experiment.impl;

import com.intuit.wasabi.analytics.Analytics;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.experiment.*;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentDetail;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.DatabaseRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@link ExperimentDetails}
 */
public class ExperimentDetailsImpl implements ExperimentDetails{

    private static final Logger LOGGER = getLogger(ExperimentDetailsImpl.class);

    private final ExperimentRepository databaseRepository;
    private final ExperimentRepository cassandraRepository;
    private final Buckets buckets;

    private final Analytics analytics;

    private final Experiments experiments;


    /**
     * Constructor of the ExperimentDetails.
     *
     * @param databaseRepository the mssql database used for analytic data
     * @param cassandraRepository repository for the experiment information
     * @param experiments access to the experiments
     * @param buckets access to the bucket information
     */
    @Inject
    public ExperimentDetailsImpl(@DatabaseRepository ExperimentRepository databaseRepository,
                                 @CassandraRepository ExperimentRepository cassandraRepository,
                                 Experiments experiments, Buckets buckets, Analytics analytics) {
        super();
        this.databaseRepository = databaseRepository;
        this.cassandraRepository = cassandraRepository;
        this.experiments = experiments;
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

        //add analytics information
        details.parallelStream().forEach(expd -> getAnalyticData(expd, params));

        return details;
    }

    /**
     * Retrieves the analytics data for the buckets and the experiment itself.
     *
     * @param experimentDetail the {@link ExperimentDetail} that needs to be enhanced with analytics data
     * @param params {@link Parameters} for the Analytics calls- containing the context for example
     * @return
     */
    private ExperimentDetail getAnalyticData(ExperimentDetail experimentDetail, Parameters params){


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
            Map<Label, BucketStatistics> bucketAnalytics = expStats.getBuckets();

            //bucket analytics
            for(ExperimentDetail.BucketDetail b : experimentDetail.getBuckets()){
                BucketStatistics bucketStat = bucketAnalytics.get(b.getLabel());
                b.setActionRate(bucketStat.getJointActionRate().getEstimate().doubleValue());
                
            }
        }
        return experimentDetail;
    }
}
