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
package com.intuit.wasabi.repository.database;

import com.google.inject.Inject;
import com.googlecode.flyway.core.Flyway;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AnalyticsRepository;
import com.intuit.wasabi.repository.RepositoryException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database analytics impl of analytics repo
 *
 * @see AnalyticsRepository
 */
public class DatabaseAnalytics implements AnalyticsRepository {

    private TransactionFactory transactionFactory;
    private Transaction transaction;

    /**
     * Constructor
     *
     * @param transactionFactory factory for transactions
     * @param flyway             Flyway
     */
    @Inject
    public DatabaseAnalytics(TransactionFactory transactionFactory, Flyway flyway) {
        super();

        this.transactionFactory = transactionFactory;
        this.transaction = transactionFactory.newTransaction();
        initialize(flyway);
    }

    void initialize(Flyway flyway) {
        flyway.setLocations("com/intuit/wasabi/repository/impl/mysql/migration");
        flyway.setDataSource(transactionFactory.getDataSource());
        flyway.migrate();
    }

    /*
     * @see com.intuit.wasabi.repository.AnalyticsRepository#getRollupRows(com.intuit.wasabi.experimentobjects.Experiment.ID, java.lang.String, com.intuit.wasabi.analyticsobjects.Parameters)
     */
    @Override
    public List<Map> getRollupRows(Experiment.ID experimentId, String rollupDate, Parameters parameters)
            throws RepositoryException {

        // TODO enable direct mapping of DateMidnight
        List rollupRows;
        try {
            //build and execute SQL queries for counts from rollups
            String sqlQuery = "select bucket_label as bid, action, impression_count as ic, impression_user_count as iuc, " +
                    "action_count as ac, action_user_count as auc from experiment_rollup " +
                    "where experiment_id = ? and cumulative = ? and day = ? and context = ?";

            rollupRows = transaction.select(sqlQuery, experimentId, true,
                    rollupDate, parameters.getContext().getContext());
            return rollupRows;

        } catch (Exception e) {
            throw new RepositoryException("error reading rollup rows from MySQL", e);
        }

    }

    /*
     * @see com.intuit.wasabi.repository.AnalyticsRepository#getActionsRows(com.intuit.wasabi.experimentobjects.Experiment.ID, com.intuit.wasabi.analyticsobjects.Parameters)
     */
    @Override
    public List<Map> getActionsRows(Experiment.ID experimentID, Parameters parameters)
            throws RepositoryException {

        try {
            //build and execute SQL queries for counts
            Date from_ts = parameters.getFromTime();
            Date to_ts = parameters.getToTime();
            String sqlBase = "bucket_label as bid, count(user_id) as c, count(distinct user_id) as cu";
            StringBuilder sqlParams = new StringBuilder(" where experiment_id = ? and context = ?");
            List params = new ArrayList();
            params.add(experimentID);
            params.add(parameters.getContext().getContext());


            if (from_ts != null) {
                params.add(from_ts);
                sqlParams.append(" and timestamp >= ?");
            }

            if (to_ts != null) {
                params.add(to_ts);
                sqlParams.append(" and timestamp <= ?");
            }

            addActionsToSql(parameters, sqlParams, params);

            Object[] bucketSqlData = new Object[params.size()];
            params.toArray(bucketSqlData);

            String sqlActions = "select action, " + sqlBase + " from event_action" +
                    sqlParams.toString() + " group by bucket_label, action";
            List<Map> actionsRows = transaction.select(sqlActions, bucketSqlData);


            return actionsRows;

        } catch (Exception e) {
            throw new RepositoryException("error reading actions rows from MySQL", e);
        }
    }

    /**
     * @param experimentID experimentID
     * @param parameters   parameters associated with this experiment
     * @return actions joint actions
     * @throws RepositoryException system exception
     */
    /*
     * @see com.intuit.wasabi.repository.AnalyticsRepository#getJointActions(com.intuit.wasabi.experimentobjects.Experiment.ID, com.intuit.wasabi.analyticsobjects.Parameters)
     */
    @Override
    public List<Map> getJointActions(Experiment.ID experimentID, Parameters parameters)
            throws RepositoryException {

        try {

            //build and execute SQL queries for counts
            Date from_ts = parameters.getFromTime();
            Date to_ts = parameters.getToTime();
            String sqlBase = "bucket_label as bid, count(user_id) as c, count(distinct user_id) as cu";
            StringBuilder sqlParams = new StringBuilder(" where experiment_id = ? and context = ?");
            List params = new ArrayList();
            params.add(experimentID);
            params.add(parameters.getContext().getContext());


            if (from_ts != null) {
                params.add(from_ts);
                sqlParams.append(" and timestamp >= ?");
            }

            if (to_ts != null) {
                params.add(to_ts);
                sqlParams.append(" and timestamp <= ?");
            }

            addActionsToSql(parameters, sqlParams, params);

            Object[] bucketSqlData = new Object[params.size()];
            params.toArray(bucketSqlData);

            String sqlJointActions = "select " + sqlBase + " from event_action" +
                    sqlParams + " group by bucket_label";
            List<Map> jointActionsRows = transaction.select(sqlJointActions, bucketSqlData);
            return jointActionsRows;

        } catch (Exception e) {
            throw new RepositoryException("error reading actions rows from MySQL", e);
        }
    }

    void addActionsToSql(Parameters parameters, StringBuilder sqlParams, List params) {
        List<String> actions = parameters.getActions();
        if (actions != null) {
            int num_actions = actions.size();
            if (num_actions >= 1) {
                sqlParams.append(" and action in (?");
                params.add(actions.get(0));
            }
            for (int num = 1; num < num_actions; num++) {
                sqlParams.append(",?");
                params.add(actions.get(num));
            }
            if (num_actions >= 1) {
                sqlParams.append(") ");
            }
        }
    }

    /*
     * @see com.intuit.wasabi.repository.AnalyticsRepository#getImpressionRows(com.intuit.wasabi.experimentobjects.Experiment.ID, com.intuit.wasabi.analyticsobjects.Parameters)
     */
    @Override
    public List<Map> getImpressionRows(Experiment.ID experimentID, Parameters parameters)
            throws RepositoryException {

        try {

            //build and execute SQL queries for counts
            Date from_ts = parameters.getFromTime();
            Date to_ts = parameters.getToTime();
            String sqlBase = "bucket_label as bid, count(user_id) as c, count(distinct user_id) as cu";
            String sqlParams = " where experiment_id = ? and context = ?";
            List params = new ArrayList();
            params.add(experimentID);
            params.add(parameters.getContext().getContext());


            if (from_ts != null) {
                params.add(from_ts);
                sqlParams += " and timestamp >= ?";
            }

            if (to_ts != null) {
                params.add(to_ts);
                sqlParams += " and timestamp <= ?";
            }

            Object[] bucketSqlData = new Object[params.size()];
            params.toArray(bucketSqlData);

            String sqlImpressions = "select " + sqlBase + " from event_impression" +
                    sqlParams + " group by bucket_label";
            List<Map> impressionRows = transaction.select(sqlImpressions, bucketSqlData);

            return impressionRows;
        } catch (Exception e) {
            throw new RepositoryException("error reading actions rows from MySQL", e);
        }
    }

    /*
     * @see com.intuit.wasabi.repository.AnalyticsRepository#getEmptyBuckets(com.intuit.wasabi.experimentobjects.Experiment.ID)
     */
    @Override
    public Map<Bucket.Label, BucketCounts> getEmptyBuckets(Experiment.ID experimentID)
            throws RepositoryException {

        try {
            List<Map> bucketRows = transaction.select("select label from bucket where experiment_id=?", experimentID);

            Counts impressions = new Counts.Builder().withEventCount(0).withUniqueUserCount(0).build();
            Counts jointActions = new Counts.Builder().withEventCount(0).withUniqueUserCount(0).build();
            Map<Event.Name, ActionCounts> perDayBucketActions = new HashMap<>();
            BucketCounts blankBucket = new BucketCounts.Builder().withImpressionCounts(impressions)
                    .withJointActionCounts(jointActions)
                    .withActionCounts(perDayBucketActions).build();
            Map<Bucket.Label, BucketCounts> buckets = new HashMap<>();

            for (Map bucketRow : bucketRows) {
                Bucket.Label externalLabel =
                        Bucket.Label.valueOf((String) bucketRow.get("label"));
                BucketCounts bucket = blankBucket.clone();

                bucket.setLabel(externalLabel);
                buckets.put(externalLabel, bucket);
            }
            return buckets;

        } catch (Exception e) {
            throw new RepositoryException("error reading actions rows from MySQL", e);
        }
    }

    /*
     * @see com.intuit.wasabi.repository.AnalyticsRepository#getCountsFromRollups(com.intuit.wasabi.experimentobjects.Experiment.ID, com.intuit.wasabi.analyticsobjects.Parameters)
     */
    @Override
    public List<Map> getCountsFromRollups(Experiment.ID experimentID, Parameters parameters)
            throws RepositoryException {

        try {

            //build and execute SQL queries for counts from rollups
            String sqlQuery = "select day, bucket_label as bid, cumulative as c, action, impression_count as ic, " +
                    "impression_user_count as iuc, action_count as ac, action_user_count as auc " +
                    "from experiment_rollup where experiment_id = ? and context = ? order by day asc";

            return transaction.select(sqlQuery, experimentID, parameters.getContext().getContext());

        } catch (Exception e) {
            throw new RepositoryException("error reading counts from MySQL rollups", e);
        }
    }

    /**
     * Get the date of the most recent rollup.  Check to make sure that the toTime specified is &gt;= last rollup
     * Return true if the date of the most recent rollup is before the specified toTime
     *
     * @param experiment experiment object
     * @param parameters parameter object
     * @param to         date
     * @return boolean
     * @throws RepositoryException exception
     */
    @Override
    public boolean checkMostRecentRollup(Experiment experiment, Parameters parameters, Date to)
            throws RepositoryException {

        try {
            Timestamp toTime = new Timestamp(to.getTime());


            final String SQL_SELECT_ID = "SELECT day FROM experiment_rollup " +
                    "WHERE experiment_id=? AND context=? ORDER BY day";

            List result = transaction.select(SQL_SELECT_ID,
                    experiment.getID(),
                    parameters.getContext().getContext());

            if (result.isEmpty()) {
                return true;
            } else {
                Map row = (Map) result.get(0);
                Date maxDay = (Date) row.get("day");
                Timestamp maxStamp = new Timestamp(maxDay.getTime());
                return maxStamp.after(toTime);
            }
        } catch (Exception e) {
            throw new RepositoryException("error reading counts from MySQL rollups", e);
        }
    }

}
