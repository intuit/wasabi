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
package com.intuit.wasabi.repository;

import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for accessing analytic data
 *
 * @see Experiment
 * @see Parameters
 */
public interface AnalyticsRepository {

    /**
     * Get action rows for the arguments
     *
     * @param experimentID experimentID
     * @param parameters   parameters associated with this experiment
     * @return Map of action rows
     */
    List<Map> getActionsRows(Experiment.ID experimentID, Parameters parameters);

    /**
     * Get joint actions for arguments
     *
     * @param experimentID experimentID
     * @param parameters   parameters associated with this experiment
     * @return Map of actions
     */
    List<Map> getJointActions(Experiment.ID experimentID, Parameters parameters);

    /**
     * Get rollup rows for arguments
     *
     * @param experimentId experimentID
     * @param rollupDate   the dates to roll up to
     * @param parameters   parameters associated with this experiment
     * @return rollup rows map
     */
    List<Map> getRollupRows(Experiment.ID experimentId, String rollupDate, Parameters parameters)
    ;

    /**
     * Get impression rows for arguments
     *
     * @param experimentID experimentID
     * @param parameters   parameters associated with this experiment
     * @return impression rows map
     */
    List<Map> getImpressionRows(Experiment.ID experimentID, Parameters parameters);

    /**
     * Get empty buckets with their labels
     *
     * @param experimentID experimentID
     * @return map of buckets and count
     */
    Map<Bucket.Label, BucketCounts> getEmptyBuckets(Experiment.ID experimentID);

    /**
     * Get counts from roll ups
     *
     * @param experimentID experimentID
     * @param parameters   parameters associated with this experiment
     * @return map of counts for rollups
     */
    List<Map> getCountsFromRollups(Experiment.ID experimentID, Parameters parameters);

    /**
     * Check most recent rollup
     *
     * @param experiment experiment object
     * @param parameters parameters associated with this experiment
     * @param to         date
     * @return whether the check succeeded
     */
    boolean checkMostRecentRollup(Experiment experiment, Parameters parameters, Date to);
}
