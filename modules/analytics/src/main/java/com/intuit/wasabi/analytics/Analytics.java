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
package com.intuit.wasabi.analytics;

import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCumulativeCounts;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentCumulativeStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;

/**
 * This interface specifies methods to compute statistics on an {@link Experiment}.
 */
public interface Analytics {

    /**
     * Queries the datastore and returns a number of summary counts for the specified experiment.
     *
     * @param experimentId the unique {@link com.intuit.wasabi.experimentobjects.Experiment.ID}
     * @param parameters   API parameters {@link Parameters}
     * @return ExperimentCounts
     */
    ExperimentCounts getExperimentCounts(Experiment.ID experimentId, Parameters parameters);

    /**
     * Queries the datastore and returns a number of summary counts for the specified experiment, by day.
     *
     * @param experimentId the unique experiment ID
     * @param parameters   API parameters
     * @return ExperimentCumulativeCounts
     */
    ExperimentCumulativeCounts getExperimentCountsDailies(Experiment.ID experimentId, Parameters parameters);

    /**
     * Returns a number of summary counts for the specified experiment, using rollups if possible.
     * <p>
     * First queries the rollup table to retrieve counts.  If no valid rollups exist,
     * calls {@link #getExperimentCounts} to do full queries for counts data.
     *
     * @param experimentId the unique {@link com.intuit.wasabi.experimentobjects.Experiment.ID}
     * @param parameters   API parameters {@link Parameters}
     * @return ExperimentCounts
     */
    ExperimentCounts getExperimentRollup(Experiment.ID experimentId, Parameters parameters);

    /**
     * Returns a number of summary counts for the specified experiment, by day, using rollups if possible.
     * <p>
     * First queries the rollup table to retrieve counts.  If no valid rollups exist,
     * calls {@link #getExperimentCountsDailies} to do full queries for counts data.
     *
     * @param experimentId the unique  {@link com.intuit.wasabi.experimentobjects.Experiment.ID}
     * @param parameters   API parameters {@link Parameters}
     * @return ExperimentCumulativeCounts
     */
    ExperimentCumulativeCounts getExperimentRollupDailies(Experiment.ID experimentId, Parameters parameters);

    /**
     * Queries the datastore and returns a number of summary counts and statistics for the specified experiment.
     * <p>
     * Statistics are calculated from unique counts.
     *
     * @param experimentId the unique experiment ID
     * @param parameters   API parameters
     * @return ExperimentStatistics
     */
    ExperimentStatistics getExperimentStatistics(Experiment.ID experimentId, Parameters parameters);

    /**
     * Queries the database and returns a number of summary counts and statistics for the specified experiment, by day.
     * Statistics are calculated from unique counts and comparison statistics are only calculated for cumulative counts.
     *
     * @param experimentId experiment id
     * @param parameters   parameters
     * @return ExperimentCumulativeStatistics
     */
    ExperimentCumulativeStatistics getExperimentStatisticsDailies(Experiment.ID experimentId, Parameters parameters);

    /**
     * Returns a summary of the assignments delivered for an experiment
     *
     * @param experimentID experiment id
     * @param context      context
     * @return AssignmentCounts
     */
    AssignmentCounts getAssignmentCounts(Experiment.ID experimentID, Context context);
}
