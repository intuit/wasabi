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
package com.intuit.wasabi.assignment;

import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBatch;
import com.intuit.wasabi.experimentobjects.Page;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The common interface for the Assignment Objects that measure the interaction of Users with
 * the Experiment.
 */
public interface Assignments {

    /**
     * Holds the length of queues stored in rule cache and ingestion executors.
     *
     * @return Map of number of elements each queue
     */
    @Deprecated
    Map<String, Integer> queuesLength();

    /**
     * @return Details of the queues in rule cache and ingestion executors.
     */
    Map<String, Object> queuesDetails();

    /**
     * Flush all active and queued messages in ThreadPoolExecutor to persistent store.
     */
    void flushMessages();

    /**
     * Get or create a single assignment for a given user to a given experiment.
     *
     * @param userID                the {@link com.intuit.wasabi.assignmentobjects.User.ID} of the person we want the assignment for
     * @param appLabel              the {@link com.intuit.wasabi.experimentobjects.Application.Name} the app we want the assignment for
     * @param experimentLabel       the {@link com.intuit.wasabi.experimentobjects.Experiment.Label} the experiment
     * @param context               the {@link Context} of the assignment call
     * @param createAssignment      <code>true</code> when a new Assignment should be created
     * @param ignoreSamplingPercent <code>true</code> if we want to have an assignment independent of the sampling rate
     * @param segmentationProfile   the {@link SegmentationProfile} to be used for the assignment
     * @param headers               the {@link HttpHeaders} that can be used by the segmentation
     * @return a brand new or old {@link Assignment}
     */
    Assignment doSingleAssignment(User.ID userID, Application.Name appLabel,
                                  Experiment.Label experimentLabel, Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                  SegmentationProfile segmentationProfile, HttpHeaders headers,boolean forceProfileCheck);

    /**
     * Get or create user assignments for a given user to given experiments.
     *
     * @param userID           the {@link com.intuit.wasabi.assignmentobjects.User.ID} of the person we want the assignment for
     * @param applicationName  the {@link com.intuit.wasabi.experimentobjects.Application.Name} the app we want the assignment for
     * @param context          the {@link Context} of the assignment call
     * @param createAssignment <code>true</code> when a new Assignment should be created
     * @param overwrite        <code>true</code> if the assignment should be forced for existing assignments
     * @param headers          the {@link HttpHeaders} that can be used by the segmentation
     * @param experimentBatch  the {@link ExperimentBatch} experiment batch for batch assignments
     * @return a brand new or old {@link Assignment}
     */
    List<Assignment> doBatchAssignments(User.ID userID, Application.Name applicationName, Context context,
                                        boolean createAssignment, boolean overwrite, HttpHeaders headers,
                                        ExperimentBatch experimentBatch, boolean forceProfileCheck);

    /**
     * Get or create user assignments for a given user to experiments associated to given application and page.
     *
     * @param applicationName       the {@link com.intuit.wasabi.experimentobjects.Application.Name} the app we want the assignment for
     * @param pageName              the {@link com.intuit.wasabi.experimentobjects.Page.Name} the page name for the assignment
     * @param userID                the {@link com.intuit.wasabi.assignmentobjects.User.ID} of the person we want the assignment for
     * @param context               the {@link Context} of the assignment call
     * @param createAssignment      <code>true</code> when a new Assignment should be created
     * @param ignoreSamplingPercent <code>true</code> if the assignment should be forced
     * @param headers               the {@link HttpHeaders} that can be used by the segmentation
     * @param segmentationProfile   {@link SegmentationProfile} for the Assignment resolution
     * @return a {@link List} of {@link HashMap}s for the generated assignments
     */
    List<Assignment> doPageAssignments(Application.Name applicationName, Page.Name pageName, User.ID userID,
                                       Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                       HttpHeaders headers, SegmentationProfile segmentationProfile, boolean forceProfileCheck);

    /**
     * Return an existing assignment of a given user to the given experiment.
     * This method does not create a NEW ASSIGNMENT and returns NULL if assignment does not exists.
     *
     * @param userID          the {@link com.intuit.wasabi.assignmentobjects.User.ID} of the person we want the assignment for
     * @param appLabel        the {@link com.intuit.wasabi.experimentobjects.Application.Name} the app we want the assignment for
     * @param experimentLabel the {@link com.intuit.wasabi.experimentobjects.Experiment.Label} the experiment
     * @param context         the {@link Context} of the assignment call
     * @return Existing {@link Assignment} or NULL
     */
    Assignment getExistingAssignment(User.ID userID, Application.Name appLabel, Experiment.Label experimentLabel, Context context);

    /**
     * Create or update an assignment of a given user to a given experiment.
     *
     * @param userID             the {@link com.intuit.wasabi.assignmentobjects.User.ID} of the person we want the assignment for
     * @param applicationName    the {@link com.intuit.wasabi.experimentobjects.Application.Name} the app we want the assignment for
     * @param experimentLabel    the {@link com.intuit.wasabi.experimentobjects.Experiment.Label} the experiment
     * @param context            the {@link Context} of the assignment call
     * @param desiredBucketLabel the {@link com.intuit.wasabi.experimentobjects.Bucket.Label} the assignment should go to
     * @param overwrite          <code>true</code> if the assignment should be forced for existing assignments
     * @return a brand new or old {@link Assignment}
     */
    Assignment putAssignment(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                             Context context, Bucket.Label desiredBucketLabel, boolean overwrite);

    /**
     * Export assignments data for a given experiment ID.
     *
     * @param experimentID     the {@link com.intuit.wasabi.experimentobjects.Experiment.ID}
     * @param context          the {@link Context}
     * @param parameters       the parameters {@link Parameters}
     * @param ignoreNullBucket the boolean flag of whether to ignroe null bucket
     * @return a {@link StreamingOutput} for the Assignment Data
     */
    StreamingOutput getAssignmentStream(Experiment.ID experimentID, Context context, Parameters parameters, Boolean ignoreNullBucket);

    /**
     * This method returns true if the input segmentation profile matches the rule in the experiment.
     *
     * @param applicationName     the {@link com.intuit.wasabi.experimentobjects.Application.Name} the app we want the assignment for
     * @param experimentLabel     the {@link com.intuit.wasabi.experimentobjects.Experiment.Label} the experiment
     * @param context             the {@link Context} of the assignment call
     * @param segmentationProfile the {@link SegmentationProfile} to be used for the assignment
     * @param headers             the {@link HttpHeaders} that can be used by the segmentation
     * @return true if either the experiment doesn't have a segmentation rule or if the rule evaluates to true given the profile input, false otherwise (there is a rule and it evaluates to false).
     */
    boolean doSegmentTest(Application.Name applicationName, Experiment.Label experimentLabel,
                          Context context, SegmentationProfile segmentationProfile,
                          HttpHeaders headers);

    /**
     * This method is used to clear assignments metadata cache.
     *
     * @return True if cache is cleared successfully
     */
    void clearMetadataCache();

    /**
     * This method is used to get details about metadata cache.
     *
     * @return Map of metadata cache details
     */
    Map<String, String> metadataCacheDetails();


    /**
     * Gets bucket assignment ratios per day for a list of experiments. Also contains meta information about the
     * experiments such as sampling percentages and priorities. The data is in rows by date and ordered by priority
     * per row.
     *
     * @param experiments          the list of experiments
     * @param experimentPriorities a look up map of priorities
     * @param fromDate             the date to start reporting from
     * @param toDate               the date to report to
     * @return bucket assignment ratios per day and meta
     */
    /*
    FIXME: Traffic Analyzer change commented for Datastax-driver-migration release...
    ImmutableMap<String, ?> getExperimentAssignmentRatioPerDayTable(List<Experiment> experiments, Map<Experiment.ID, Integer> experimentPriorities, OffsetDateTime fromDate, OffsetDateTime toDate);
    */
}
