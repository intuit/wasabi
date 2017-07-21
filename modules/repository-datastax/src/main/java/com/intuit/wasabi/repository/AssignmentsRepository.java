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

import com.google.common.collect.Table;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBatch;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.core.StreamingOutput;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * Assign users to experiments in a batch
     *
     * @param assignments pair of experiment and assignment
     * @param date        Date of user assignment
     * @return Resulting assignment
     */
    void assignUsersInBatch(List<Pair<Experiment, Assignment>> assignments, Date date);

    /**
     * Get assignments
     *
     * @param userID        User Id
     * @param appLabel      Application Label
     * @param context       Environment context
     * @param experimentMap experiment map to fetch experiment label
     * @return Experiment id
     */
    List<Pair<Experiment, String>> getAssignments(User.ID userID, Application.Name appLabel, Context context,
                                                  Map<Experiment.ID, Experiment> experimentMap);

    /**
     * Get assignment for experiment and user
     *
     * @param userID
     * @param appName
     * @param experimentID
     * @param context
     * @return assignment object if assignment is present or NULL
     */
    Assignment getAssignment(User.ID userID, Application.Name appName, Experiment.ID experimentID, Context context);

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
     * Assign user to exports
     *
     * @param assignment Assignment object
     * @param date       Date of assignment
     */
    void assignUserToExports(Assignment assignment, Date date);

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
     * @param type      type of assignment to be staged
     * @param exception Exception
     * @param data      Assignment Data to be pushed to staging
     */
    void pushAssignmentToStaging(String type, String exception, String data);

    /**
     * Push assignment messages to staging in a BATCH
     *
     * @param type      type of assignment to be staged
     * @param exception Exception
     * @param data      Assignment messages to be pushed to staging
     */
    void pushAssignmentsToStaging(String type, String exception, Collection<String> data);

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

    /**
     * Gets the bucket assignmentCounts in parallel using asynchronous database queries.
     * @param experimentIds List of experiment ids
     * @return map of experiment id to assignment counts.
     */
    Map<Experiment.ID, AssignmentCounts> getBucketAssignmentCountsInParallel(List<Experiment.ID> experimentIds);

    /**
     * Populate experiment metadata asynchronously...
     *
     * @param userID
     * @param appName
     * @param context
     * @param allowAssignments
     * @param prioritizedExperimentList
     * @param experimentMap
     * @param bucketMap
     * @param exclusionMap
     */
    void populateAssignmentsMetadata(User.ID userID, Application.Name appName, Context context, ExperimentBatch experimentBatch, Optional<Map<Experiment.ID, Boolean>> allowAssignments,
                                     PrioritizedExperimentList prioritizedExperimentList,
                                     Map<Experiment.ID, Experiment> experimentMap,
                                     Map<Experiment.ID, BucketList> bucketMap,
                                     Map<Experiment.ID, List<Experiment.ID>> exclusionMap
    );

}