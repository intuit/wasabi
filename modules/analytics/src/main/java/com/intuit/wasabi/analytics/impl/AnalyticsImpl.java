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

import com.google.inject.Inject;
import com.intuit.wasabi.analytics.AnalysisTools;
import com.intuit.wasabi.analytics.Analytics;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.analyticsobjects.counts.DailyCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCumulativeCounts;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics.BinomialMetric;
import com.intuit.wasabi.analyticsobjects.statistics.BucketBasicStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.BucketComparison;
import com.intuit.wasabi.analyticsobjects.statistics.BucketStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.DailyStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentBasicStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentCumulativeStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.database.Transaction.Block;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AnalyticsRepository;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.slf4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TimeZone;

import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.wasabi.util.DateUtil.createCalendarMidnight;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation for {@link Analytics}
 */
public class AnalyticsImpl implements Analytics {

    public static final String PROPERTY_NAME = "/analytics.properties";
    private static final String ACTION = "action";
    private static final Logger LOGGER = getLogger(AnalyticsImpl.class);
    private final Experiments experiments;
    private final TransactionFactory transactionFactory;
    private final AnalyticsRepository analyticsRepository;
    private final AnalysisTools analysisTools;
    private final ExperimentRepository cassandraRepository;
    private final AssignmentsRepository assignmentRepository;

    /**
     * Constructor
     *
     * @param experiments           experiments
     * @param assignmentRepository  assignment repository
     * @param dataTransactorFactory data transaction factory
     * @param analyticsRepository   analytics repository
     * @param analysisTools         analytics tools
     * @param cassandraRepository   cassandra repository
     */
    @Inject
    public AnalyticsImpl(final Experiments experiments, final AssignmentsRepository assignmentRepository,
                         final TransactionFactory dataTransactorFactory, final AnalyticsRepository analyticsRepository,
                         final AnalysisTools analysisTools,
                         final @CassandraRepository ExperimentRepository cassandraRepository) {
        this.experiments = experiments;
        this.transactionFactory = dataTransactorFactory;
        this.analyticsRepository = analyticsRepository;
        this.analysisTools = analysisTools;
        this.cassandraRepository = cassandraRepository;
        this.assignmentRepository = assignmentRepository;
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        // FIXME: inject
        Properties properties = create(PROPERTY_NAME, AnalyticsImpl.class);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentCounts getExperimentRollup(final Experiment.ID experimentID, final Parameters parameters) {

        return (ExperimentCounts) transactionFactory.transaction(new Block() {
            @Override
            public Object value(Transaction transaction) {

                //if from_ or to_ time or actions specified, calculate counts from actions and impressions tables directly
                if (circumventRollup(experimentID, parameters)) {
                    return getExperimentCounts(experimentID, parameters);
                }

                //check that experiment exists and fetch metadata
                Experiment exp = getExperimentIfExists(experimentID);

                Rollup rollup = new Rollup(exp, true, transaction);
                if (!rollup.isFreshEnough()) {
                    return getExperimentCounts(experimentID, parameters);
                }

                //get rolluprows from the database
                List<Map> rollupRows = analyticsRepository.getRollupRows(experimentID,
                        rollup.latestAvailableRollupDateAsString(),
                        parameters);

                //fetch list of buckets for experiment and use to create counts objects
                Map<Bucket.Label, BucketCounts> buckets = analyticsRepository.getEmptyBuckets(experimentID);

                //loop over rollup rows to fill BucketCounts objects with counts
                for (Map rollupRow : rollupRows) {
                    // fixme: ref rollup domain object
                    BucketCounts bucket = buckets.get(Bucket.Label.valueOf((String) rollupRow.get("bid")));

                    // fixme: ref rollup domain object
                    if ("".equals(rollupRow.get(ACTION))) {
                        bucket.setImpressionCounts(new Counts.Builder()
                                .withEventCount(Long.valueOf((Integer) rollupRow.get("ic")))
                                .withUniqueUserCount(Long.valueOf((Integer) rollupRow.get("iuc")))
                                .build());
                        bucket.setJointActionCounts(new Counts.Builder()
                                .withEventCount(Long.valueOf((Integer) rollupRow.get("ac")))
                                .withUniqueUserCount(Long.valueOf((Integer) rollupRow.get("auc")))
                                .build());
                    } else {
                        Event.Name actionName = Event.Name.valueOf((String) rollupRow.get(ACTION));
                        bucket.addActionCounts(actionName, new ActionCounts.Builder()
                                .withActionName(actionName)
                                .withEventCount(Long.valueOf((Integer) rollupRow.get("ac")))
                                .withUniqueUserCount(Long.valueOf((Integer) rollupRow.get("auc")))
                                .build());
                    }
                }

                return analysisTools.calculateExperimentCounts(buckets.values());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentCounts getExperimentCounts(final Experiment.ID experimentID, final Parameters parameters) {


        return (ExperimentCounts) transactionFactory.transaction(new Block() {
            @Override
            public Object value(Transaction transaction) {

                assertExperimentExists(experimentID);

                List<Map> impressionRows = analyticsRepository.getImpressionRows(experimentID, parameters);
                List<Map> actionsRows = analyticsRepository.getActionsRows(experimentID, parameters);
                List<Map> jointActionsRows = analyticsRepository.getJointActions(experimentID, parameters);
                //fetch list of buckets for experiment and use to create counts objects
                Map<Bucket.Label, BucketCounts> buckets = analyticsRepository.getEmptyBuckets(experimentID);

                //loop over each of the SQL results to fill BucketCounts objects with counts
                for (Map actionRow : actionsRows) {
                    BucketCounts bucket = buckets.get(Bucket.Label.valueOf((String) actionRow.get("bid")));
                    Event.Name actionName = Event.Name.valueOf((String) actionRow.get(ACTION));
                    bucket.addActionCounts(actionName, new ActionCounts.Builder()
                            .withActionName(actionName)
                            .withEventCount((Long) actionRow.get("c"))
                            .withUniqueUserCount((Long) actionRow.get("cu"))
                            .build());
                }

                for (Map impressionRow : impressionRows) {
                    BucketCounts bucket = buckets.get(Bucket.Label.valueOf((String) impressionRow.get("bid")));

                    bucket.setImpressionCounts(new Counts.Builder()
                            .withEventCount((Long) impressionRow.get("c"))
                            .withUniqueUserCount((Long) impressionRow.get("cu"))
                            .build());
                }

                for (Map jointActionsRow : jointActionsRows) {
                    BucketCounts bucket = buckets.get(Bucket.Label.valueOf((String) jointActionsRow.get("bid")));

                    bucket.setJointActionCounts(new Counts.Builder()
                            .withEventCount((Long) jointActionsRow.get("c"))
                            .withUniqueUserCount((Long) jointActionsRow.get("cu"))
                            .build());
                }

                return analysisTools.calculateExperimentCounts(buckets.values());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentCumulativeCounts getExperimentRollupDailies(final Experiment.ID experimentID,
                                                                 final Parameters parameters) {
        return (ExperimentCumulativeCounts) transactionFactory.transaction(new Block() {
            @Override
            public Object value(Transaction transaction) {

                //if from_ or to_ time or actions specified, calculate counts from actions and impressions tables directly
                if (circumventRollup(experimentID, parameters)) {
                    return getExperimentCountsDailies(experimentID, parameters);
                }

                Experiment exp = getExperimentIfExists(experimentID);

                Rollup rollup = new Rollup(exp, transaction);
                if (!rollup.isFreshEnough())
                    return getExperimentCountsDailies(experimentID, parameters);

                List<Map> rollupRows = analyticsRepository.getCountsFromRollups(experimentID, parameters);

                // fetch list of buckets for experiment and use to pre-populate buckets for perDay and cumulative
                Map<Bucket.Label, BucketCounts> buckets = analyticsRepository.getEmptyBuckets(experimentID);
                ExperimentCounts experiment = analysisTools.calculateExperimentCounts(buckets.values());

                //create calendars to hold the start and end day
                Date start_ts = exp.getStartTime();
                Calendar start_cal = createCalendarMidnight(start_ts);
                Date end_ts = exp.getEndTime();
                Calendar end_cal = createCalendarMidnight(end_ts);
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

                df.setTimeZone(TimeZone.getTimeZone("UTC"));

                //loop over days using calendars to create DailyCounts with zero counts for each day
                List<DailyCounts> days = new ArrayList<>();

                for (; start_cal.compareTo(end_cal) <= 0; start_cal.add(Calendar.DATE, 1)) {
                    String thisDate = df.format(start_cal.getTime());

                    days.add(new DailyCounts.Builder().setDate(thisDate).withPerDay(experiment.clone())
                            .withCumulative(experiment.clone()).build());
                }

                int numberDays = days.size();
                int currentDay = 0;
                //loop over rollup rows to update DailyCounts for each day
                Map<Bucket.Label, BucketCounts> perDayBuckets = new HashMap<>();
                Map<Bucket.Label, BucketCounts> cumulativeBuckets = new HashMap<>();

                for (Map.Entry<Bucket.Label, BucketCounts> bucketEntry : buckets.entrySet()) {
                    perDayBuckets.put(bucketEntry.getKey(), bucketEntry.getValue().clone());
                    cumulativeBuckets.put(bucketEntry.getKey(), bucketEntry.getValue().clone());
                }

                int numberRows = rollupRows.size();

                for (int n = 0; n < numberRows; n++) {
                    Map rollupRow = rollupRows.get(n);
                    Bucket.Label bucketLabel = Bucket.Label.valueOf((String) rollupRow.get("bid"));

                    if ("".equals(rollupRow.get(ACTION))) {
                        Counts bucketImpressions = new Counts.Builder()
                                .withEventCount(Long.valueOf((Integer) rollupRow.get("ic")))
                                .withUniqueUserCount(Long.valueOf((Integer) rollupRow.get("iuc")))
                                .build();
                        Counts bucketJointActions = new Counts.Builder()
                                .withEventCount(Long.valueOf((Integer) rollupRow.get("ac")))
                                .withUniqueUserCount(Long.valueOf((Integer) rollupRow.get("auc")))
                                .build();

                        if ((Boolean) rollupRow.get("c")) {
                            cumulativeBuckets.get(bucketLabel).setImpressionCounts(bucketImpressions);
                            cumulativeBuckets.get(bucketLabel).setJointActionCounts(bucketJointActions);
                        } else {
                            perDayBuckets.get(bucketLabel).setImpressionCounts(bucketImpressions);
                            perDayBuckets.get(bucketLabel).setJointActionCounts(bucketJointActions);
                        }
                    } else {
                        Event.Name actionName = Event.Name.valueOf((String) rollupRow.get(ACTION));
                        ActionCounts bucketAction = new ActionCounts.Builder()
                                .withActionName(actionName)
                                .withEventCount(Long.valueOf((Integer) rollupRow.get("ac")))
                                .withUniqueUserCount(Long.valueOf((Integer) rollupRow.get("auc")))
                                .build();

                        if ((Boolean) rollupRow.get("c")) {
                            cumulativeBuckets.get(bucketLabel).addActionCounts(actionName, bucketAction);
                        } else {
                            perDayBuckets.get(bucketLabel).addActionCounts(actionName, bucketAction);
                        }
                    }

                    //update the DailyCounts if this is the last row for this date
                    Date thisDay = (Date) rollupRow.get("day");

                    if ((n == numberRows - 1) || (!rollupRows.get(n + 1).get("day").equals(thisDay))) {
                        //calculate date string and create a new DailyCounts for this day
                        String thisDate = df.format(thisDay);

                        while (currentDay < numberDays) {
                            if (days.get(currentDay).getDate().equals(thisDate)) {
                                days.set(currentDay, new DailyCounts.Builder().setDate(thisDate)
                                        .withPerDay(analysisTools.calculateExperimentCounts(perDayBuckets.values()))
                                        .withCumulative(analysisTools.calculateExperimentCounts(cumulativeBuckets.values()))
                                        .build());

                                currentDay += 1;

                                break;
                            } else {
                                //carry over cumulative counts from previous day if there are no new counts
                                if (currentDay > 0) {
                                    DailyCounts currentDailyCounts = days.get(currentDay);
                                    DailyCounts missingDailyCounts = getPreviousDayDailyCountAsCurrentDailyCount(
                                            currentDailyCounts, days, currentDay);
                                    days.set(currentDay, missingDailyCounts);
                                }

                                currentDay += 1;
                            }
                        }

                        //reset the perDay and cumulative maps for the next day
                        perDayBuckets = new HashMap<>();
                        cumulativeBuckets = new HashMap<>();

                        for (Map.Entry<Bucket.Label, BucketCounts> bucketEntry : buckets.entrySet()) {
                            perDayBuckets.put(bucketEntry.getKey(), bucketEntry.getValue().clone());
                            cumulativeBuckets.put(bucketEntry.getKey(), bucketEntry.getValue().clone());
                        }
                    }
                }

                //finish filling the cumulative counts if not already done
                for (; currentDay < numberDays; currentDay += 1) {
                    DailyCounts thisDailyCounts = days.get(currentDay);
                    DailyCounts currentDailyCount = getPreviousDayDailyCountAsCurrentDailyCount(
                            thisDailyCounts, days, currentDay);

                    days.set(currentDay, currentDailyCount);
                }

                return new ExperimentCumulativeCounts.Builder().withDays(days).build();
            }
        });
    }

    DailyCounts getPreviousDayDailyCountAsCurrentDailyCount(DailyCounts currentDailyCount, List<DailyCounts> days, int currentDay) {
        DailyCounts.Builder dailyCountsBuilder = new DailyCounts.Builder()
                .setDate(currentDailyCount.getDate())
                .withPerDay(currentDailyCount.getPerDay())
                .withCumulative(days.get(currentDay - 1).getCumulative());
        return dailyCountsBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentCumulativeCounts getExperimentCountsDailies(final Experiment.ID experimentId, final Parameters parameters) {

        Experiment exp = getExperimentIfExists(experimentId);

        //set start and end timestamps and use to create calendars
        Date start_ts = parameters.getFromTime();

        if (start_ts == null) {
            start_ts = exp.getStartTime();
        }

        Calendar start_cal = createCalendarMidnight(start_ts);
        Date end_ts = parameters.getToTime();

        if (end_ts == null) {
            end_ts = exp.getEndTime();
        }

        Calendar end_cal = createCalendarMidnight(end_ts);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        List<DailyCounts> days = new ArrayList<>();

        //loop over days using calendars
        for (; start_cal.compareTo(end_cal) <= 0; start_cal.add(Calendar.DATE, 1)) {
            Calendar to_cal = (Calendar) start_cal.clone();

            to_cal.add(Calendar.DATE, 1);
            to_cal.add(Calendar.MILLISECOND, -1);

            String currentDate = df.format(start_cal.getTime());

            //fetch the counts for the current day only
            Parameters dailyParams = parameters.clone();
            dailyParams.setFromTime(new Date(start_cal.getTime().getTime()));
            dailyParams.setToTime(new Date(to_cal.getTime().getTime()));

            ExperimentCounts perDayCount = getExperimentCounts(experimentId, dailyParams);

            //fetch the counts from the beginning of the experiment up through the current day
            Parameters cumulativeParams = parameters.clone();
            cumulativeParams.setFromTime(null);
            cumulativeParams.setToTime(new Date(to_cal.getTime().getTime()));

            ExperimentCounts cumulativeCount = getExperimentCounts(experimentId, cumulativeParams);

            days.add(new DailyCounts.Builder().setDate(currentDate).withPerDay(perDayCount)
                    .withCumulative(cumulativeCount).build());

        }

        return new ExperimentCumulativeCounts.Builder().withDays(days).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentStatistics getExperimentStatistics(final Experiment.ID experimentId, final Parameters parameters) {
        return (ExperimentStatistics) transactionFactory.transaction(new Block() {
            @Override
            public Object value(Transaction transaction) {
                ExperimentCounts counts = getExperimentRollup(experimentId, parameters);

                return calculateExperimentStatistics(counts, parameters.getMetricImpl(), parameters.getEffectSize(),
                        parameters.getMode());
            }
        });
    }

    /**
     * Creates an ExperimentStatistics object from an ExperimentCounts object.
     *
     * @param counts     an ExperimentCounts object containing all the counts for this experiment
     * @param metric     the binomial metric object to use for statistics calculations
     * @param effectSize the effect size of interest to use for statistics calculations
     * @return ExperimentStatistics
     */
    ExperimentStatistics calculateExperimentStatistics(final ExperimentCounts counts, BinomialMetric metric,
                                                       double effectSize, Parameters.Mode mode) {
        ExperimentStatistics statistics = new ExperimentStatistics.Builder()
                .withExperimentCounts(counts)
                .withBuckets(calculateBucketStatistics(counts.getBuckets(), metric, effectSize, mode))
                .build();

        analysisTools.generateRate(statistics, metric);
        analysisTools.generateProgress(statistics);

        return statistics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentCumulativeStatistics getExperimentStatisticsDailies(final Experiment.ID experimentId,
                                                                         final Parameters parameters) {

        BinomialMetric metric = parameters.getMetricImpl();
        double effectSize = parameters.getEffectSize();
        //get counts for each day, using rollups if applicable
        ExperimentCumulativeCounts counts = getExperimentRollupDailies(experimentId, parameters);
        List<DailyStatistics> days = new ArrayList<>();

        for (DailyCounts day : counts.getDays()) {
            //fetch the counts for the current day only and create corresponding statistics objects
            ExperimentCounts perDayCount = day.getPerDay();
            Map<Bucket.Label, BucketBasicStatistics> bucketsWithStats = new HashMap<>();

            for (BucketCounts bucket : perDayCount.getBuckets().values()) {
                BucketBasicStatistics bucketWithStats = new BucketBasicStatistics.Builder()
                        .withBucketCounts(bucket)
                        .build();
                Bucket.Label bucketLabel = bucket.getLabel();
                analysisTools.generateRate(bucketWithStats, metric);
                bucketsWithStats.put(bucketLabel, bucketWithStats);
            }

            ExperimentBasicStatistics perDayStatistics = new ExperimentBasicStatistics.Builder()
                    .withExperimentCounts(perDayCount)
                    .withBuckets(bucketsWithStats)
                    .build();

            analysisTools.generateRate(perDayStatistics, metric);

            //fetch the counts from the beginning of the experiment up through the current day
            //and create corresponding statistics objects
            ExperimentCounts cumulativeCount = day.getCumulative();
            ExperimentStatistics cumulativeStatistics = new ExperimentStatistics.Builder()
                    .withExperimentCounts(cumulativeCount)
                    .withBuckets(calculateBucketStatistics(cumulativeCount.getBuckets(),
                            metric, effectSize, parameters.getMode()))
                    .build();

            analysisTools.generateRate(cumulativeStatistics, metric);
            analysisTools.generateProgress(cumulativeStatistics);

            days.add(new DailyStatistics.Builder().setDate(day.getDate()).withPerDay(perDayStatistics)
                    .withCumulative(cumulativeStatistics).build());
        }

        return new ExperimentCumulativeStatistics.Builder()
                .withDays(days)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssignmentCounts getAssignmentCounts(Experiment.ID experimentID, Context context) {

        // Uses counters
        Experiment experiment = cassandraRepository.getExperiment(experimentID);
        if (Objects.isNull(experiment)) {
            throw new ExperimentNotFoundException(experimentID);
        }
        return assignmentRepository.getBucketAssignmentCount(experiment);
    }

    /**
     * Creates a list of BucketStatistics objects from a list of BucketCounts objects.
     *
     * @param buckets    a complete list of BucketCounts for this experiment
     * @param metric     the binomial metric to use for statistics calculations
     * @param effectSize the effect size of interest to use for statistics calculations
     * @return Map of bucket label and bucket stats
     */
    Map<Bucket.Label, BucketStatistics> calculateBucketStatistics(Map<Bucket.Label, BucketCounts> buckets,
                                                                  BinomialMetric metric, double effectSize,
                                                                  Parameters.Mode mode) {
        Map<Bucket.Label, BucketStatistics> bucketsWithStats = new HashMap<>();

        for (BucketCounts bucket : buckets.values()) {
            Bucket.Label bucketLabel = bucket.getLabel();
            BucketStatistics bucketWithStats = new BucketStatistics.Builder()
                    .withBucketCounts(bucket)
                    .withBucketComparisons(new HashMap<Bucket.Label, BucketComparison>())
                    .build();

            analysisTools.generateRate(bucketWithStats, metric);
            bucketsWithStats.put(bucketLabel, bucketWithStats);
        }

        analysisTools.generateBucketComparison(bucketsWithStats, metric, effectSize, mode);

        return bucketsWithStats;
    }


    boolean circumventRollup(final Experiment.ID experimentID, final Parameters parameters) {
        Experiment exp = getExperimentIfExists(experimentID);
        Date to = parameters.getToTime();

        if (parameters.getFromTime() != null || parameters.getActions() != null) {
            return true;
        } else if (to != null) {

            // Get the date of the most recent rollup.  Check to make sure that the toTime specified is >= last rollup
            // Return true if the date of the most recent rollup is before the specified toTime
            return analyticsRepository.checkMostRecentRollup(exp, parameters, to);
        } else {
            return false;
        }
    }

    // TODO would be nice if experiment existence could be checked without hitting the db
    Experiment getExperimentIfExists(final Experiment.ID experimentID) {
        Experiment experiment = experiments.getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }
        return experiment;
    }

    private void assertExperimentExists(final Experiment.ID experimentID) {
        getExperimentIfExists(experimentID);
    }

}
