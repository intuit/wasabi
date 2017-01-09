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
package com.intuit.wasabi.repository;

import com.google.common.collect.Table;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;

import javax.ws.rs.core.StreamingOutput;
import java.util.Date;
import java.util.Set;

/**
 * Interface to support assignment requests
 *
 * @see User
 * @see Experiment
 * @see Application
 * @see Experiment
 * @see Context
 * @see Assignment
 * @see Bucket
 * @see Parameters
 */
public interface AssignmentsRepository {

    /**
     * Get user assignments
     *
     * @param userID   User ID
     * @param appLabel Application Label
     * @param context  Environment context
     * @return Set of experiments for user
     */
    Set<Experiment.ID> getUserAssignments(User.ID userID, Application.Name appLabel, Context context);

    /**
     * Assign a user to experiment
     *
     * @param assignment Assignment assignment
     * @param experiment For Experiment experiment
     * @param date       Date of user assignment
     * @return Resulting assignment
     */
    Assignment assignUser(Assignment assignment, Experiment experiment, Date date);

    /**
     * Get assignments
     *
     * @param userID         User Id
     * @param appLabel       Application Label
     * @param context        Environment context
     * @param allExperiments A table of all Experiments for this application
     * @return Table of assignments
     */
    Table<Experiment.ID, Experiment.Label, String> getAssignments(User.ID userID, Application.Name appLabel,
                                                                  Context context,
                                                                  Table<Experiment.ID, Experiment.Label,
                                                                          Experiment> allExperiments);

    /**
     * Get assignment for experiment and user
     *
     * @param experimentID A Experiment.ID, uuid identifier for Experiment
     * @param userID       User Id
     * @param context      Environment context
     * @return Assignment
     */
    Assignment getAssignment(Experiment.ID experimentID, Application.Name appName, User.ID userID, Context context);

    /**
     * Delete assignment for experiment, user and application
     *
     * @param experiment        Experiment for which assignment is to be deleted
     * @param userID            UserId for to be deleted assignment
     * @param context           Environment context
     * @param appName           Application name
     * @param currentAssignment Assignment to be deleted
     */
    void deleteAssignment(Experiment experiment, User.ID userID, Context context, Application.Name appName,
                          Assignment currentAssignment);

    /**
     * Assign user to the old user_assignment table.
     * This will be completely removed once new table user_assignment_lookup starts serving all assignments.
     *
     * @param assignment Assignment object
     * @param date       Date of assignment
     * @return Assignment
     */
    @Deprecated
    Assignment assignUserToOld(Assignment assignment, Date date);

    /**
     * Assign user to exports
     *
     * @param assignment Assignment object
     * @param date       Date of assignment
     */
    void assignUserToExports(Assignment assignment, Date date);

    /**
     * Remove index user to bucket
     *
     * @param userID       UserId
     * @param experimentID A Experiment.ID, uuid identifier for Experiment
     * @param context      Environment context
     * @param bucketLabel  Bucket Label
     */
    void removeIndexUserToBucket(User.ID userID, Experiment.ID experimentID, Context context, Bucket.Label bucketLabel);

    /**
     * Get assignments as a stream
     *
     * @param experimentID     A Experiment.ID, uuid identifier for Experiment
     * @param context          Environment context
     * @param parameters       Parameters object
     * @param ignoreNullBucket Boolean, ignore null Buckets
     * @return assignment stream
     */
    StreamingOutput getAssignmentStream(final Experiment.ID experimentID, final Context context, Parameters parameters,
                                        final Boolean ignoreNullBucket);

    /**
     * Push assignment to staging
     *
     * @param exception Exception
     * @param data      Assignment Data to be pushed to staging
     */
    void pushAssignmentToStaging(String exception, String data);

    /**
     * Increments the bucket assignments counter up by 1 if countUp is true
     *
     * @param experiment update bucket assignment count for the Experiment experiment
     * @param assignment update bucket assignment count for the Assignment assignment
     * @param countUp    Increments the bucket assignments counter up by 1 if countUp is true
     *                   Decrement if countUp is false
     */
    void updateBucketAssignmentCount(Experiment experiment, Assignment assignment, boolean countUp);

    /**
     * Gets the current counts of bucket assignments for the different buckets along with their total
     *
     * @param experiment Experiment for which the bucket assignment counts is needed.
     * @return AssignmentCounts
     */
    AssignmentCounts getBucketAssignmentCount(Experiment experiment);

}
