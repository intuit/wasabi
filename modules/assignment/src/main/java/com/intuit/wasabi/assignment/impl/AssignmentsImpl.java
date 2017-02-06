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
package com.intuit.wasabi.assignment.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.hyrule.Rule;
import com.intuit.hyrule.RuleBuilder;
import com.intuit.hyrule.exceptions.InvalidInputException;
import com.intuit.hyrule.exceptions.MissingInputException;
import com.intuit.hyrule.exceptions.TreeStructureException;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.assignment.AssignmentDecorator;
import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import com.intuit.wasabi.assignmentobjects.RuleCache;
import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.AssignmentExistsException;
import com.intuit.wasabi.exceptions.BucketDistributionNotFetchableException;
import com.intuit.wasabi.exceptions.BucketNotFoundException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBatch;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.cassandra.impl.ExperimentRuleCacheUpdateEnvelope;

/**
 * Assignments implementation
 *
 * @see Assignments
 */
public class AssignmentsImpl implements Assignments {

    protected static final String HOST_IP = "hostIP";
    protected static final String RULE_CACHE = "ruleCache";
    protected static final String QUEUE_SIZE = "queueSize";

    /**
     * Logger for the class
     */
    private static final Logger LOGGER = getLogger(AssignmentsImpl.class);
    /**
     * Experiment repo
     */
    protected final ExperimentRepository repository;

    /**
     * Mutex repo
     */
    protected final MutexRepository mutexRepository;

    /**
     * Assignments reo
     */
    protected final AssignmentsRepository assignmentsRepository;
    private final SecureRandom random;
    /**
     * Executors to ingest data to real time ingestion system.
     */
    protected Map<String, AssignmentIngestionExecutor> executors;
    protected AssignmentDecorator assignmentDecorator = null;
    protected ThreadPoolExecutor ruleCacheExecutor;
    // Rule-Cache queue set up
    protected RuleCache ruleCache;
    /**
     * Proxy related info
     */
    private Priorities priorities;
    private Pages pages;

    private EventLog eventLog;
    private String hostIP;

    /**
     * Helper for unit tests
     * @param assignmentRepository
     * @param mutRepository
     */
    AssignmentsImpl(AssignmentsRepository assignmentRepository, MutexRepository mutRepository) {
        // FIXME
//        eventLog = new NoopEventLogImpl();
        repository = null;
        mutexRepository = mutRepository;
        assignmentsRepository = assignmentRepository;
        random = null;
    }

    /**
     * Constructor
     *
     * @param repository                          CassandraRepository to connect to
     * @param assignmentsRepository               reference to AssignmentsRepository
     * @param mutexRepository                     reference to MutexRepository
     * @param ruleCache                           RuleCache which has cached segmentation rules
     * @param pages                               Pages for this experiment
     * @param priorities                          Priorities for the application
     * @param assignmentDecorator                 The assignmentDecorator to be used
     * @param ruleCacheExecutor                   The rule cache executor to be used

     * @param eventLog                            eventLog
     * @throws IOException         io exception
     * @throws ConnectionException connection exception
     */
    @Inject
    public AssignmentsImpl(Map<String, AssignmentIngestionExecutor> executors,
                           final @CassandraRepository ExperimentRepository repository,
                           final AssignmentsRepository assignmentsRepository,
                           final MutexRepository mutexRepository,
                           final RuleCache ruleCache, final Pages pages,
                           final Priorities priorities,
                           final AssignmentDecorator assignmentDecorator,
                           final @Named("ruleCache.threadPool") ThreadPoolExecutor ruleCacheExecutor,
                           final EventLog eventLog)
            throws IOException {
        super();
        try {
            hostIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            // ignore
        }
        this.executors = executors;
        this.repository = repository;
        // FIXME:
        this.random = new SecureRandom();
        this.ruleCache = ruleCache;
        this.pages = pages;
        this.priorities = priorities;
        this.assignmentDecorator = assignmentDecorator;
        this.eventLog = eventLog;
        this.ruleCacheExecutor = ruleCacheExecutor;
        this.assignmentsRepository = assignmentsRepository;
        this.mutexRepository = mutexRepository;
        this.eventLog = eventLog;
  
    }

    /**
     * @param userID                the {@link com.intuit.wasabi.assignmentobjects.User.ID} of the person we want the assignment for
     * @param applicationName       the {@link com.intuit.wasabi.experimentobjects.Application.Name} application name
     * @param experimentLabel       the {@link com.intuit.wasabi.experimentobjects.Experiment.Label} the experiment
     * @param context               the {@link Context} of the assignment call
     * @param createAssignment      <code>true</code> when a new Assignment should be created
     * @param ignoreSamplingPercent <code>true</code> if we want to have an assignment independent of the sampling rate
     * @param segmentationProfile   the {@link SegmentationProfile} to be used for the assignment
     * @param headers               the {@link HttpHeaders} that can be used by the segmentation
     * @param pageName              the {@link com.intuit.wasabi.experimentobjects.Page.Name} the page name for the assignment
     * @return assignment assignments
     */
    @Override
    public Assignment getSingleAssignment(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                          Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                          SegmentationProfile segmentationProfile, HttpHeaders headers, Page.Name pageName) {

        final Date currentDate = new Date();
        final long currentTime = currentDate.getTime();

        Experiment experiment = repository.getExperiment(applicationName, experimentLabel);
        if (experiment == null) {
            return nullAssignment(userID, applicationName, null, Assignment.Status.EXPERIMENT_NOT_FOUND);
        }
        Experiment.ID experimentID = experiment.getID();

        assert experiment.getState() != Experiment.State.TERMINATED :
                "Should not be able to access terminated experiment \"" +
                        experimentID + "\" via label \"" + experimentLabel + "\"";

        if (experiment.getState() == Experiment.State.DRAFT) {
            return nullAssignment(userID, applicationName, experimentID,
                    Assignment.Status.EXPERIMENT_IN_DRAFT_STATE);
        }


        if (currentTime < experiment.getStartTime().getTime()) {
            return nullAssignment(userID, applicationName, experimentID,
                    Assignment.Status.EXPERIMENT_NOT_STARTED);
        }

        if (currentTime > experiment.getEndTime().getTime()) {
            return nullAssignment(userID, applicationName, experimentID,
                    Assignment.Status.EXPERIMENT_EXPIRED);
        }

        Assignment assignment = assignmentsRepository.getAssignment(experimentID, userID, context);
        if (assignment == null || assignment.isBucketEmpty()) {
            if (createAssignment) {
                if (experiment.getState() == Experiment.State.PAUSED) {
                    return nullAssignment(userID, applicationName, experimentID,
                            Assignment.Status.EXPERIMENT_PAUSED);
                }

                // Generate a new assignment
                double samplePercent = experiment.getSamplingPercent();
                assert samplePercent >= 0.0 && samplePercent <= 1.0 : "Sample percent must be between 0.0 and 1.0";

                boolean selectBucket;

                if (doesProfileMatch(experiment, segmentationProfile, headers, context)) {
                    selectBucket = checkMutex(experiment, userID, context) && (ignoreSamplingPercent || (rollDie() <
                            samplePercent));

                    if (segmentationProfile == null || segmentationProfile.getProfile() == null) {
                        Map profileMap = new HashMap();
                        segmentationProfile = new SegmentationProfile.Builder(profileMap).build();
                    }
                    // Generate the assignment; this always generates an assignment,
                    // which may or may not specify a bucket
                    assignment = generateAssignment(experiment, userID, context, selectBucket, null /* no bucket list*/, currentDate, segmentationProfile);
                    assert assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT :
                            "Assignment status should have been NEW_ASSIGNMENT for " +
                                    "userID = \"" + userID + "\", experiment = \"" + experiment + "\"";
                } else {
                    ruleCacheExecutor.execute(new ExperimentRuleCacheUpdateEnvelope(experiment.getRule(),
                            ruleCache, experimentID));

                    return nullAssignment(userID, applicationName, experiment.getID(),
                            Assignment.Status.NO_PROFILE_MATCH);
                }
            }
        } else {
            // Do nothing; user has an assignment, with or without a bucket
            assert assignment.getStatus() == Assignment.Status.EXISTING_ASSIGNMENT :
                    "Assignment status should have been EXISTING_ASSIGNMENT for " +
                            "userID = \"" + userID + "\", experiment = \"" +
                            experiment + "\"";
        }

        // Updating rule cache
        ruleCacheExecutor.execute(new ExperimentRuleCacheUpdateEnvelope(experiment.getRule(), ruleCache,
                experimentID));

        // Ingest data to real time data ingestion systems if executors exist
		for (String name : executors.keySet()) {
			executors.get(name).execute(new AssignmentEnvelopePayload(userID, context, createAssignment, false, ignoreSamplingPercent,
                    segmentationProfile, assignment != null ? assignment.getStatus() : null,
                    assignment != null ? assignment.getBucketLabel() : null, pageName, applicationName,
                    experimentLabel, experimentID,
                    currentDate, headers));
		}

		return assignment;
    }

    @Override
    public Assignment getAssignment(User.ID userID,
                                    Application.Name appName, Experiment.Label experimentLabel,
                                    Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                    SegmentationProfile segmentationProfile, HttpHeaders headers) {
        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments =
                repository.getExperimentList(appName);
        Experiment experiment = getExperimentFromTable(allExperiments, experimentLabel);

        if (experiment == null) {
            return nullAssignment(userID, appName, null, Assignment.Status.EXPERIMENT_NOT_FOUND);
        }

        BucketList bucketList = repository.getBucketList(experiment.getID());
        Table<Experiment.ID, Experiment.Label, String> userAssignments =
                assignmentsRepository.getAssignments(userID, experiment.getApplicationName(), context, allExperiments);
        Map<Experiment.ID, List<Experiment.ID>> exclusives = getExclusivesList(experiment.getID());

        return getAssignment(userID, appName, experimentLabel, context, createAssignment, ignoreSamplingPercent,
                segmentationProfile, headers, null, experiment, bucketList, userAssignments, exclusives);
    }

    protected Experiment getExperimentFromTable(Table<Experiment.ID, Experiment.Label, Experiment> allExperiments,
                                                Experiment.Label experimentLabel) {

        Collection<Experiment> experiments = allExperiments.column(experimentLabel).values();
        return experiments.isEmpty() ? null : experiments.iterator().next();
    }

    private Map<Experiment.ID, List<Experiment.ID>> getExclusivesList(Experiment.ID experimentID) {
        List<Experiment.ID> exclusions = mutexRepository.getExclusionList(experimentID);
        Map<Experiment.ID, List<Experiment.ID>> result = new HashMap<>(1);
        result.put(experimentID, exclusions);
        return result;
    }


    /**
     * Return an existing assignment for a user. For new assignment, only create a new assignment object,
     * and doesn't create new assignment in a database (cassandra).
     *
     * It is responsibility of caller method to create assignment entries in a database.
     *
     * Includes a Page.Name to identify the page through which the assignment was delivered.
     */
    protected Assignment getAssignment(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                    Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                    SegmentationProfile segmentationProfile, HttpHeaders headers, Page.Name pageName,
                                    Experiment experiment, BucketList bucketList,
                                    Table<Experiment.ID, Experiment.Label, String> userAssignments,
                                    Map<Experiment.ID, List<Experiment.ID>> exclusives) {
        final Date currentDate = new Date();
        final long currentTime = currentDate.getTime();

        if (experiment == null) {
            return nullAssignment(userID, applicationName, null, Assignment.Status.EXPERIMENT_NOT_FOUND);
        }

        Experiment.ID experimentID = experiment.getID();

        assert experiment.getState() != Experiment.State.TERMINATED :
                new StringBuilder("Should not be able to access terminated experiment \"")
                        .append(experimentID).append("\" via label \"")
                        .append(experimentLabel).append("\"");

        if (experiment.getState() == Experiment.State.DRAFT) {
            return nullAssignment(userID, applicationName, experimentID,
                    Assignment.Status.EXPERIMENT_IN_DRAFT_STATE);
        }

        if (currentTime < experiment.getStartTime().getTime()) {
            return nullAssignment(userID, applicationName, experimentID,
                    Assignment.Status.EXPERIMENT_NOT_STARTED);
        }

        if (currentTime > experiment.getEndTime().getTime()) {
            return nullAssignment(userID, applicationName, experimentID,
                    Assignment.Status.EXPERIMENT_EXPIRED);
        }

        Assignment assignment = getAssignment(experimentID, userID, context, userAssignments, bucketList);
        if (assignment == null || assignment.isBucketEmpty()) {
            if (createAssignment) {
                if (experiment.getState() == Experiment.State.PAUSED) {
                    return nullAssignment(userID, applicationName, experimentID, Assignment.Status.EXPERIMENT_PAUSED);
                }

                // Generate a new assignment
                double samplePercent = experiment.getSamplingPercent();
                assert samplePercent >= 0.0 && samplePercent <= 1.0 : "Sample percent must be between 0.0 and 1.0";

                boolean selectBucket;

                // Check if the current user is selected by the segmentation rule of this experiment
                // when their profile values (and the headers and context) are used in the evaluation.
                // NOTE: If available, this uses the parsed version of the rule for this experiment that
                // has been cached in memory on this system.  That means a recent change to the rule
                // won't be taken into account until the ruleCacheExecutor call below.  This is an optimization
                // to improve performance which can mean that several assignments will use the old version of
                // the rule (until all servers have been updated with the new version).
                if (doesProfileMatch(experiment, segmentationProfile, headers, context)) {
                    selectBucket = checkMutex(experiment, userAssignments, exclusives) &&
                            (ignoreSamplingPercent || (rollDie() < samplePercent));

                    if (segmentationProfile == null || segmentationProfile.getProfile() == null) {
                        Map profileMap = new HashMap();
                        segmentationProfile = new SegmentationProfile.Builder(profileMap).build();
                    }

                    // Generate the assignment; this always generates an assignment,
                    // which may or may not specify a bucket
                    //todo: change so this doesn't follow the 'read then write' Cassandra anti-pattern
                    assignment = createAssignmentObject(experiment, userID, context, selectBucket, bucketList, currentDate, segmentationProfile);
                    assert assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT :
                            new StringBuilder("Assignment status should have been NEW_ASSIGNMENT for ")
                                    .append("userID = \"").append(userID).append("\", experiment = \"")
                                    .append(experiment).append("\"").toString();
                } else {
                    return nullAssignment(userID, applicationName, experimentID,
                            Assignment.Status.NO_PROFILE_MATCH);
                }
            }
        } else {
            // Do nothing; user has an assignment, with or without a bucket
            assert assignment.getStatus() == Assignment.Status.EXISTING_ASSIGNMENT :
                    new StringBuilder("Assignment status should have been EXISTING_ASSIGNMENT for ")
                            .append("userID = \"").append(userID)
                            .append("\", experiment = \"").append(experiment).append("\"").toString();
        }

        return assignment;
    }

    /**
     *
     * Create/Retrieve assignments for a given user, application, context and
     * given experiments (experimentBatch/allowAssignments).
     *
     * This method is called from two places:
     * 1. First directly through API call AssignmentsResource.getBatchAssignments() => api:/v1/assignments/applications/{applicationName}/users/{userID}
     *    In this case pageName and allowAssignments are NULL
     *
     * 2. Second from AssignmentsImpl.doPageAssignments() => api:/v1/assignments/applications/{applicationName}/pages/{pageName}/users/{userID}
     *    In this case pageName and allowAssignments are provided.
     *    To avoid performance degradation (duplicate calls to )
     *
     *
     * @param userID           the {@link com.intuit.wasabi.assignmentobjects.User.ID} of the person we want the assignment for
     * @param applicationName  the {@link com.intuit.wasabi.experimentobjects.Application.Name} the app we want the assignment for
     * @param context          the {@link Context} of the assignment call
     * @param createAssignment <code>true</code> when a new Assignment should be created
     * @param forceInExperiment
     * @param headers          the {@link HttpHeaders} that can be used by the segmentation
     * @param experimentBatch  the {@link ExperimentBatch} experiment batch for batch assignments
     *                         Experiment labels are NOT PRESENT when called from AssignmentsImpl.doPageAssignments()
     *
     * @param pageName         the {@link com.intuit.wasabi.experimentobjects.Page.Name} the page name for the assignment
     *                         NULL when called from AssignmentsResource.getBatchAssignments()
     *                         PRESENT when called from AssignmentsImpl.doPageAssignments()
     *
     * @param allowAssignments {@link HashMap} for each experiment whether the assignment is allowed
     *                         NULL when called from AssignmentsResource.getBatchAssignments()
     *                         PRESENT when called from AssignmentsImpl.doPageAssignments()
     *
     * @return
     */
    @Override
    public List<Map> doBatchAssignments(User.ID userID, Application.Name applicationName, Context context,
                                            boolean createAssignment, boolean forceInExperiment, HttpHeaders headers,
                                            ExperimentBatch experimentBatch, Page.Name pageName,
                                            Map<Experiment.ID, Boolean> allowAssignments) {

        //allowAssignments is NULL when called from AssignmentsResource.getBatchAssignments()
        Optional<Map<Experiment.ID, Boolean>> allowAssignmentsOptional = Optional.ofNullable(allowAssignments);
        PrioritizedExperimentList appPriorities = new PrioritizedExperimentList();
        Map<Experiment.ID, com.intuit.wasabi.experimentobjects.Experiment> experimentMap = new HashMap<>();
        Table<Experiment.ID, Experiment.Label, String> userAssignments = HashBasedTable.create();
        Map<Experiment.ID, BucketList> bucketMap = new HashMap<>();
        Map<Experiment.ID, List<Experiment.ID>> exclusionMap = new HashMap<>();

        //Populate required experiment metadata along with all the existing user assignments for the given application
        assignmentsRepository.populateExperimentMetadata(userID, applicationName, context, experimentBatch, allowAssignmentsOptional, appPriorities, experimentMap, userAssignments, bucketMap, exclusionMap);

        List<Pair<Experiment, Assignment>> assignmentPairs = new LinkedList<>();
        List<Map> allAssignments = new LinkedList<>();
        SegmentationProfile segmentationProfile = SegmentationProfile.from(experimentBatch.getProfile()).build();
        // iterate over all experiments in the application in priority order
        for (PrioritizedExperiment experiment : appPriorities.getPrioritizedExperiments()) {
            LOGGER.debug("Now processing: {}", experiment);

            //check if the experiment was given in experimentBatch
            if (experimentBatch.getLabels().contains(experiment.getLabel())) {
                LOGGER.debug("Experiment ({}) has given batch assignment....", experiment.getLabel());

                String labelStr = experiment.getLabel().toString();
                Map<String, Object> tempResult = new HashMap<>();
                // get the assignment for user in this experiment
                Experiment.Label label = Experiment.Label.valueOf(labelStr);
                tempResult.put("experimentLabel", label);

                try {
                    boolean experimentCreateAssignment = allowAssignmentsOptional.isPresent()?(allowAssignmentsOptional.get().get(experiment.getID())):createAssignment;

                    //This method only gets assignment object, and doesn't create new assignments in a database (cassandra)
                    Assignment assignment = getAssignment(userID, applicationName, label,
                            context, experimentCreateAssignment,
                            forceInExperiment, segmentationProfile,
                            headers, pageName, experimentMap.get(experiment.getID()),
                              bucketMap.get(experiment.getID()), userAssignments, exclusionMap);

                    // This wouldn't normally happen because we specified CREATE=true
                    if (isNull(assignment)) {
                        continue;
                    }
                    // Only include `assignment` property if there is a definitive
                    // assignment, either to a bucket or not
                    if (assignment.getStatus() != Assignment.Status.EXPERIMENT_EXPIRED) {
                        // Add the assignment to the global list of userAssignments of the user
                        userAssignments.put(experiment.getID(), experiment.getLabel(),
                                assignment.getBucketLabel() != null ? assignment.getBucketLabel().toString() : "null");
                        tempResult.put("assignment",
                                assignment.getBucketLabel() != null
                                        ? assignment.getBucketLabel().toString()
                                        : null);

                        if (nonNull(assignment.getBucketLabel())) {
                            Optional<Bucket> bucket = getBucketByLabel(bucketMap.get(experiment.getID()), assignment.getBucketLabel());
                            if(bucket.isPresent()) {
                                tempResult.put("payload",
                                        bucket.get().getPayload() != null
                                                ? bucket.get().getPayload()
                                                : null);
                            }
                        }
                    }

                    tempResult.put("status", assignment.getStatus());
                    LOGGER.debug("tempResult: {}", tempResult);

                    assignmentPairs.add(new ImmutablePair<Experiment, Assignment>(experimentMap.get(experiment.getID()), assignment));

                } catch (WasabiException ex) {
                    //FIXME: should not use exception as part of the flow control.
                    LOGGER.info("Using exception as flow control", ex);
                    tempResult.put("status", "assignment failed");
                    tempResult.put("exception", ex.toString());
                    tempResult.put("assignment", null);
                }

                allAssignments.add(tempResult);
                experimentBatch.getLabels().remove(experiment.getLabel());
            }
        }
        LOGGER.debug("allAssignments: {} ", allAssignments);

        //Make new assignments in database (cassandra)
        final Date currentDate = new Date();
        Stream<Pair<Experiment, Assignment>> newAssignments = assignmentPairs.stream().filter(pair -> {return (nonNull(pair) && pair.getRight().getStatus()==Assignment.Status.NEW_ASSIGNMENT);});
        assignmentsRepository.assignUsersInBatch(newAssignments.collect(Collectors.toList()), currentDate);
        LOGGER.debug("Finished Create_Assignments_DB...");

        //Ingest data to real time data ingestion systems if executors exist
        assignmentPairs.forEach(assignmentPair -> {
            Assignment assignment = assignmentPair.getRight();
            Experiment experiment = assignmentPair.getLeft();
            boolean experimentCreateAssignment = allowAssignmentsOptional.isPresent()?(allowAssignmentsOptional.get().get(experiment.getID())):createAssignment;
            for (String name : executors.keySet()) {
                executors.get(name).execute(new AssignmentEnvelopePayload(userID, context, experimentCreateAssignment, false,
                        forceInExperiment, segmentationProfile, assignment != null ? assignment.getStatus() : null,
                        assignment != null ? assignment.getBucketLabel() : null, pageName, applicationName, experiment.getLabel(),
                        experiment.getID(), currentDate, headers));
            }
        });

        // Updating rule cache, This will cause future assignment calls, on this server, to
        // use the new version of the rule, if it has recently been changed.
        assignmentPairs.forEach(assignmentPair -> {
            Experiment experiment = assignmentPair.getLeft();
                ruleCacheExecutor.execute(new ExperimentRuleCacheUpdateEnvelope(experiment.getRule(), ruleCache, experiment.getID()));
        });

        return allAssignments;
    }

    private Optional<Bucket> getBucketByLabel(BucketList bucketList, Bucket.Label bucketLabel) {
        Optional<Bucket> rBucket = Optional.empty();
        if(isNull(bucketList)) return rBucket;
        for(Bucket bucket:bucketList.getBuckets()) {
            if(bucket.getLabel().equals(bucketLabel)) {
                return Optional.of(bucket);
            }
        }
        return rBucket;
    }

    private Map<Experiment.ID, BucketList> getBucketList(Set<Experiment.ID> experimentIDSet) {
        return repository.getBucketList(experimentIDSet);
    }

    private Map<Experiment.ID, List<Experiment.ID>> getExclusivesList(Set<Experiment.ID> experimentIDSet) {
        return mutexRepository.getExclusivesList(experimentIDSet);
    }

    @Override
    public Assignment putAssignment(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                    Context context, Bucket.Label desiredBucketLabel, boolean overwrite) {

        //check that the experiment is in a valid state
        EnumSet<Experiment.State> validStates = EnumSet.of(Experiment.State.RUNNING,
                Experiment.State.PAUSED);

        Experiment experiment = repository.getExperiment(applicationName, experimentLabel);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentLabel);
        }
        Experiment.ID experimentID = experiment.getID();
        assert experiment.getState() != Experiment.State.TERMINATED :
                new StringBuilder("Should not be able to access terminated experiment \"")
                        .append(experimentID).append("\" via label \"")
                        .append(experimentLabel).append("\"").toString();
        if (!validStates.contains(experiment.getState())) {
            throw new InvalidExperimentStateException(experiment.getID(), validStates, Experiment.State.DRAFT);
        }

        //throw exception if assignment already exists for user unless overwrite == true
        Assignment currentAssignment = assignmentsRepository.getAssignment(experimentID, userID, context);
        if (!overwrite && currentAssignment != null && !currentAssignment.isBucketEmpty()) {
                throw new AssignmentExistsException(userID, applicationName, experimentLabel);
        }

        //check that the desired bucket is valid
        if (desiredBucketLabel != null) {
            BucketList buckets = repository.getBuckets(experimentID, false /* don't check experiment again */);
            Boolean bucketFound = false;
            for (Bucket bucket : buckets.getBuckets()) {
                if (bucket.getLabel().equals(desiredBucketLabel) && !bucket.getState().equals(Bucket.State.EMPTY)) {
                    bucketFound = true;
                    break;
                }
            }
            if (!bucketFound) {
                throw new BucketNotFoundException(desiredBucketLabel);
            }
        }

        //create a newAssignment and persist into Cassandra
        Assignment.Builder builder = Assignment.newInstance(experimentID)
                .withApplicationName(applicationName)
                .withUserID(userID)
                .withContext(context);
        if (desiredBucketLabel != null) {
            //before inserting the new data, we have to manually delete a corresponding row
            //in the user_bucket_index table, since the primary key spans over bucketlabel
            //as well and would therefore not be updated in the table
            if (currentAssignment != null && currentAssignment.getBucketLabel() != null) {
                assignmentsRepository.removeIndexUserToBucket(userID, experimentID, context, currentAssignment
                        .getBucketLabel());
            }
            builder.withBucketLabel(desiredBucketLabel);
        } else {
            //first need to delete existing assignment, since otherwise cassandra
            //will not update the assignment
            if (currentAssignment != null && currentAssignment.getBucketLabel() != null) {
                assignmentsRepository.deleteAssignment(experiment, userID, context, applicationName, currentAssignment);
            }
            //then build one with no Bucket
            builder.withBucketLabel(null);
        }
        Assignment assignment = builder.build();
        Date date = new Date();

        // Ingest data to real time data ingestion systems if executors exist
		for (String name : executors.keySet()) {
			executors.get(name).execute(new AssignmentEnvelopePayload(userID, context, false, true, false, null,
                    Assignment.Status.NEW_ASSIGNMENT, assignment.getBucketLabel(), null, applicationName,
                    experimentLabel, experimentID,
                    date, null));
		}        
        
        //write assignment after checking assignment in first step
        return assignmentsRepository.assignUser(assignment, experiment, date);
    }

    //a function to check if a user is in any experiments which are mutually exclusive with the
    // experiment described by applicationName and experimentLabel
    @Override
    public Boolean checkMutex(Experiment experiment, User.ID userID, Context context) {

        //if the experiment exists in the database and is in a valid MUTEX state
        if (experiment != null && (experiment.getState() == Experiment.State.RUNNING ||
                experiment.getState() == Experiment.State.PAUSED)) {
            //get experiments which are mutually exclusive
            ExperimentList exclusives = mutexRepository.getExclusions(experiment.getID());
            List<Experiment.ID> exclusiveIDs = new ArrayList<>();
            for (Experiment exp : exclusives.getExperiments()) {
                if (exp.getState() == Experiment.State.RUNNING ||
                        exp.getState() == Experiment.State.PAUSED) {
                    exclusiveIDs.add(exp.getID());
                }
            }

            //get all experiments to which this user is assigned
            Set<Experiment.ID> preAssign = assignmentsRepository.getUserAssignments(userID, experiment
                    .getApplicationName(), context);

            //iterate over the mutex experiments
            for (Experiment.ID exp : exclusiveIDs) {
                //check if the user is in the experiment
                if (preAssign.contains(exp)) {
                    //return a response that the user cannot be assigned
                    return false;
                }
            }
        }
        return true;
    }

	protected boolean checkMutex(Experiment experiment,
                               Table<Experiment.ID, Experiment.Label, String> userAssignments,
                               Map<Experiment.ID, List<Experiment.ID>> exclusivesList) {

        //if the experiment exists in the database and is in a valid MUTEX state
        if (experiment != null && (experiment.getState() == Experiment.State.RUNNING ||
                experiment.getState() == Experiment.State.PAUSED)) {
            //get experiments which are mutually exclusive
            List<Experiment.ID> exclusiveIDs = exclusivesList.get(experiment.getID());

            //get all experiments to which this user is assigned
            Set<Experiment.ID> preAssign = getNonNullUserAssignments(exclusivesList.get(experiment.getID()),
                    userAssignments);

            //iterate over the mutex experiments
            for (Experiment.ID exp : exclusiveIDs) {
                //check if the user is in the experiment
                if (preAssign.contains(exp)) {
                    //return a response that the user cannot be assigned
                    return false;
                }
            }
        }
        return true;
    }

    private Set<Experiment.ID> getNonNullUserAssignments(List<Experiment.ID> experimentIDList,
                              Table<Experiment.ID, Experiment.Label, String> userAssignments) {
        Set<Experiment.ID> result = new HashSet<>();
        for (Experiment.ID experimentID : experimentIDList) {
            if (!userAssignments.row(experimentID).values().isEmpty() &&
                    !"null".equals(userAssignments.row(experimentID).values().iterator().next())) {
                result.add(experimentID);
            }
        }
        return result;
    }

    @Override
    public StreamingOutput getAssignmentStream(Experiment.ID experimentID, Context context, Parameters parameters, Boolean ignoreNullBucket) {

        Experiment experiment = repository.getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }
        return assignmentsRepository.getAssignmentStream(experimentID, context, parameters, ignoreNullBucket);
    }

    @Override
    public List<Map> doPageAssignments(Application.Name applicationName, Page.Name pageName, User.ID userID,
                                           Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                           HttpHeaders headers, SegmentationProfile segmentationProfile) {

        //Get the experiments (id & allowNewAssignment only) associated to the given application and page.
        List<PageExperiment> pageExperimentList = pages.getExperimentsWithoutLabels(applicationName, pageName);

        //Prepare allowAssignments map
        Map<Experiment.ID, Boolean> allowAssignments = new HashMap<>(pageExperimentList.size());
        for (PageExperiment pageExperiment : pageExperimentList) {
            allowAssignments.put(pageExperiment.getId(), pageExperiment.getAllowNewAssignment());
        }

        //Prepare experiment batch
        ExperimentBatch.Builder experimentBatchBuilder = ExperimentBatch.newInstance();
        if (segmentationProfile != null) {
            experimentBatchBuilder.withProfile(segmentationProfile.getProfile());
        }
        ExperimentBatch experimentBatch = experimentBatchBuilder.build();

        return doBatchAssignments(userID, applicationName, context,
                createAssignment, ignoreSamplingPercent, headers, experimentBatch, pageName, allowAssignments);

    }

    protected Assignment nullAssignment(User.ID userID, Application.Name appName, Experiment.ID experimentID,
                                      Assignment.Status status) {

        return Assignment.newInstance(experimentID)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withUserID(userID)
                .withContext(null)
                .withStatus(status)
                .build();
    }

    /**
     * This method is called to check if the current user is selected by this experiment's segmentation
     * rule by checking if the user's segmentation profile values cause the rule to evaluate to true.
     * The headers and context are passed because they can be used in the rule automatically.
     * NOTE: This version will check the parsed version of the rule that is cached in memory on this
     * system, if available.
     *
     * @param experiment          Experiment
     * @param segmentationProfile Segmentation Profile
     * @param headers             Headers
     * @param context             Environment Context
     * @return boolean true if the current user is selected by the experiment segmentation rule
     */
    boolean doesProfileMatch(Experiment experiment, SegmentationProfile segmentationProfile,
                                     HttpHeaders headers, Context context) {
        return doesProfileMatch(experiment, segmentationProfile, headers, context, false);
    }

    /**
     * This version of this method is called if the caller needs this method to behave in the way necessary
     * when testing segmentation rules from the UI.  Those behaviors are that we need it to NOT pull in
     * header values and the context value, so those can be passed in by the user, and to force the rule
     * to be parsed from the experiment every time.  That is not as performant, but is necessary if
     * we need to use the latest saved version of the rule.  For the normal use, that is, during assignments,
     * we want to use the cache (and only update the cache after the latest evaluation, asynchronously).
     */
    private boolean doesProfileMatch(Experiment experiment, SegmentationProfile segmentationProfile,
                                     HttpHeaders headers, Context context, boolean testMode) {
        try {
            String ruleExpression = experiment.getRule();

            if (ruleExpression == null || ruleExpression.trim().isEmpty()) {
                return true;
            } else {
                Map<String, Object> profileAttrs;
                if (testMode) {
                    // So that the user can provide values for context and headers (like user-agent), we need
                    // to not pull those in automatically.
                    profileAttrs = segmentationProfile.getProfile();
                } else {
                    segmentationProfile = mergeHeaderAndContextWithProfile(segmentationProfile, headers, context);
                    if (segmentationProfile == null) {
                        profileAttrs = null;
                    } else {
                        profileAttrs = segmentationProfile.getProfile();
                    }
                }

                Rule ruleObject = null;
                if (testMode) {
                    // This is used by the API (doSegmentTest()) that allows a user interactively test the rule with different
                    // profile values.  That isn't as performance sensitive, so we can parse and evaluate
                    // the expression each time, because we need to take recent changes into account immediately.
                    ruleObject = new RuleBuilder().parseExpression(ruleExpression);
                    return ruleObject.evaluate((HashMap) profileAttrs); //cast for Hyrule method
                }

                // Note that we are using the in-memory cache on this server. The key to understand about
                // this is that if the rule has been changed recently, it will not be cached, and therefore,
                // not available for evaluation, until after the next time an assignment call is made.
                ruleObject = ruleCache.getRule(experiment.getID());

                if (ruleObject == null) {
                    // The rule for this experiment has never been cached on this system.  Parse it
                    // and save the result in the cache for future evaluations.
                    ruleObject = new RuleBuilder().parseExpression(ruleExpression);
                    ruleCache.setRule(experiment.getID(), ruleObject);
                    return ruleObject.evaluate((HashMap) profileAttrs); //cast for Hyrule method
                } else {
                    return ruleObject.evaluate((HashMap) profileAttrs); //cast for Hyrule method
                }
            }
        } catch (MissingInputException | InvalidInputException | TreeStructureException e) {
            LOGGER.warn("assignment: profile match exception " + e);
            return false;
        }
    }

    protected Assignment getAssignment(final Experiment.ID experimentID,
                                     final User.ID userID, final Context context,
                                     final Table<Experiment.ID, Experiment.Label, String> userAssignments,
                                     final BucketList bucketList) {

        if (userAssignments != null && userAssignments.row(experimentID).isEmpty()) {
            return null;
        } else {
            String bucketLabel = userAssignments.row(experimentID).values().iterator().next();
            for (Bucket b : bucketList.getBuckets()) {
                if (bucketLabel.equals(b.getLabel().toString())) {
                    if (b.getState() == Bucket.State.EMPTY) {
                        bucketLabel = "null";
                        break;
                    } else {
                        break;
                    }
                }
            }
            return Assignment.newInstance(experimentID)
                    .withBucketLabel("null".equals(bucketLabel)
                            ? null
                            : Bucket.Label.valueOf(bucketLabel))
                    .withUserID(userID)
                    .withContext(context)
                    .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                    .build();
        }
    }

    /**
     *
     * This method first create an assignment object for NEW_ASSIGNMENT and then make entries in to the database.
     *
     * @param experiment
     * @param userID
     * @param context
     * @param selectBucket
     * @param bucketList
     * @param date
     * @param segmentationProfile
     *
     * @return new assignment object which is created in the database as well.
     *
     */
    Assignment generateAssignment(Experiment experiment, User.ID userID, Context context, boolean selectBucket,
                                          BucketList bucketList, Date date, SegmentationProfile segmentationProfile) {
        Assignment result = createAssignmentObject(experiment, userID, context, selectBucket, bucketList, date, segmentationProfile);
        if(result.getStatus().equals(Assignment.Status.NEW_ASSIGNMENT)) {
            return assignmentsRepository.assignUser(result, experiment, date);
        } else {
            return result;
        }
    }

    /**
     *
     * This method creates an assignment object for NEW_ASSIGNMENT.
     *
     * Note: it does not create entries in the database.
     *
     * @param experiment
     * @param userID
     * @param context
     * @param selectBucket
     * @param bucketList
     * @param date
     * @param segmentationProfile
     *
     * @return new assignment object with status of either NEW_ASSIGNMENT or NO_OPEN_BUCKETS
     *
     */
    Assignment createAssignmentObject(Experiment experiment, User.ID userID, Context context, boolean selectBucket,
                                  BucketList bucketList, Date date, SegmentationProfile segmentationProfile) {

        Assignment.Builder builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(userID)
                .withContext(context);

        if (selectBucket) {
            Bucket assignedBucket;
            /*
            With the bucket state in effect, this part of the code could also result in
            the generation of a null assignment in cases where the selected bucket is CLOSED or EMPTY
            In CLOSED state, the current bucket assignments to the bucket are left as is and all the
            assignments to this bucket are made null going forward.

            In EMPTY state, the current bucket assignments are made null as well as all the future assignments
            to this bucket are made null.

            Retrieves buckets from Repository if personalization is not enabled and if skipBucketRetrieval is false
            Retrieves buckets from Assignment Decorator if personalization is enabled
            */
            BucketList bucketsExternal = getBucketList(experiment, userID, segmentationProfile,
                    !Objects.isNull(bucketList) /* do not skip retrieving bucket from repository if bucketList is null */);
            if (Objects.isNull(bucketsExternal) || bucketsExternal.getBuckets().isEmpty()) {
                // if bucketlist obtained from Assignment Decorator is null; use the bucketlist passed into the method
                assignedBucket = selectBucket(bucketList.getBuckets());
            } else {
                //else use the bucketExternal obtained from the Assignment Decorator
                assignedBucket = selectBucket(bucketsExternal.getBuckets());
            }
            //check that at least one bucket was open
            if (!Objects.isNull(assignedBucket)) {
                //create the bucket with bucketlabel
                builder.withBucketLabel(assignedBucket.getLabel());
            } else {
                return nullAssignment(userID, experiment.getApplicationName(), experiment.getID(),
                        Assignment.Status.NO_OPEN_BUCKETS);
            }

        } else {
            builder.withBucketLabel(null);
        }

        Assignment result = builder
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(Optional.ofNullable(date).orElseGet(Date::new))
                .withCacheable(false)
                .build();
        LOGGER.debug("result => {}", result);
        return result;
    }
    
    /**
     * Returns a bucketList depending on whether it is a personalization experiment or not.
     * Retrieves buckets from Assignment Decorator if personalization is enabled
     * Retrieves buckets from Repository if personalization is not enabled and if skipBucketRetrieval is false
     *
     * @param experiment          Experiment
     * @param userID              UserId
     * @param segmentationProfile The segmentation profile
     * @param skipBucketRetrieval If false, retrieves buckets from Repository
     * @return BucketList
     */
    protected BucketList getBucketList(Experiment experiment, User.ID userID, SegmentationProfile segmentationProfile, Boolean skipBucketRetrieval) {
        
        BucketList bucketList = null;
        Boolean isPersonalizationEnabled = experiment.getIsPersonalizationEnabled();
        if (isPersonalizationEnabled) {
            try {
                bucketList = assignmentDecorator.getBucketList(experiment, userID, segmentationProfile);
            } catch (BucketDistributionNotFetchableException e) {
                LOGGER.error("Error obtaining the Bucket List from {} for user_id {}, for experiment: {} due to {}",
                        assignmentDecorator.getClass().getSimpleName(), userID, experiment.getLabel().toString(), e);
            } finally {
                //TODO: add some metrics on how often this fails
                // Logic behind the below step is to ensure the following:
                // In the case of batch assignments, when response from Assignment Decorator fails, we skip doing a back end call to cassandra
                // This is consistent with the different manner in which generateAssignment method is used for single and batch assignment calls.
                if ((Objects.isNull(bucketList) || bucketList.getBuckets().isEmpty()) && !skipBucketRetrieval) {
                    bucketList = repository.getBuckets(experiment.getID(), false /* don't check experiment again */);
                }
            }
        } else {
            if (!skipBucketRetrieval) {
                bucketList = repository.getBuckets(experiment.getID(), false /* don't check experiment again */);
            }
        }
        return bucketList;
    }
    
    protected Double rollDie() {
        return random.nextDouble();
    }

    protected Bucket selectBucket(List<Bucket> buckets) {

        final double dieRoll = rollDie();

        // Sort the buckets consistently (by label)
        Collections.sort(buckets,
                new Comparator<Bucket>() {
                    @Override
                    public int compare(Bucket b1, Bucket b2) {
                        return b1.getAllocationPercent().compareTo(
                                b2.getAllocationPercent());
                    }
                });

        Double totalProbability = 0.0d;

        Bucket bucket = null;
        for (Bucket candidateBucket : buckets) {
            totalProbability += candidateBucket.getAllocationPercent();

            if (dieRoll < totalProbability) {
                bucket = candidateBucket;
                break;
            }
        }

        return bucket;
    }

    /**
     * Adds http header attributes to profile (does not override existing profile attributes)
     * We only use the first attribute if multiple attributes with same key are provided in header
     *
     * @param segmentationProfile segmentation profile {@link SegmentationProfile}
     * @param headers             the http headers to use
     * @return merged segmentation profile
     */
    protected SegmentationProfile mergeHeaderWithProfile(SegmentationProfile segmentationProfile, HttpHeaders headers) {
        if (segmentationProfile != null && segmentationProfile.getProfile() != null && headers != null) {
            for (String headerKey : headers.getRequestHeaders().keySet()) {
                if (!segmentationProfile.hasAttribute(headerKey)) {
                    segmentationProfile.addAttribute(headerKey, headers.getRequestHeader(headerKey).get(0));
                }
            }
        } else if ((segmentationProfile == null || segmentationProfile.getProfile() == null) && headers != null) {
            // create profile with headers only
            Map profileMap = new HashMap();
            segmentationProfile = new SegmentationProfile.Builder(profileMap).build();

            for (String headerKey : headers.getRequestHeaders().keySet()) {
                if (!segmentationProfile.hasAttribute(headerKey)) {
                    segmentationProfile.addAttribute(headerKey, headers.getRequestHeader(headerKey).get(0));
                }
            }
        }
        return segmentationProfile;
    }

    /**
     * Adds the context attribute to the profile. It also adds the header attributes to profile using a helper method.
     *
     * @param segmentationProfile The segmentation profile object containing various segments.
     * @param headers             The http headers that need to be merged into segmentation profile as new segments.
     * @param context             The context object containing the context, for example, PROD or QA.
     *
     * @return Returns The merged segmentation profile object containing new segments from headers and context objects.
     */
    protected SegmentationProfile mergeHeaderAndContextWithProfile(SegmentationProfile segmentationProfile, HttpHeaders headers, Context context) {
        SegmentationProfile updatedSegmentationProfile = mergeHeaderWithProfile(segmentationProfile, headers);

        if (updatedSegmentationProfile == null) {
            Map profileMap = new HashMap();
            updatedSegmentationProfile = new SegmentationProfile.Builder(profileMap).build();
        }

        updatedSegmentationProfile.addAttribute("context", context.getContext());
        return updatedSegmentationProfile;
    }

    @Override
    public Bucket getBucket(Experiment.ID experimentID, Bucket.Label bucketLabel) {

        Bucket bucket = null;
        if (bucketLabel != null) {
            bucket = repository.getBucket(experimentID, bucketLabel);
        }
        return bucket;
    }

    @Override
    public boolean doSegmentTest(Application.Name applicationName, Experiment.Label experimentLabel,
                                 Context context, SegmentationProfile segmentationProfile,
                                 HttpHeaders headers) {

        Experiment experiment = repository.getExperiment(applicationName, experimentLabel);
        if (experiment == null) {
            throw new IllegalArgumentException(new StringBuilder("Experiment not found for application \"")
                    .append(applicationName).append("\" and label \"").append(experimentLabel).append("\"").toString());
        }

        return doesProfileMatch(experiment, segmentationProfile, headers, context, true);
    }

    @Override
    public Map<String, Integer> queuesLength() {
        Map<String, Integer> queueLengthMap = new HashMap<String, Integer>();
        queueLengthMap.put(RULE_CACHE, new Integer(this.ruleCacheExecutor.getQueue().size()));
        for (String name : executors.keySet()) {
            queueLengthMap.put(name.toLowerCase(), new Integer(executors.get(name).queueLength()));
        }
        return queueLengthMap;
    }

    @Override
    public Map<String, Object> queuesDetails() {
        Map<String, Object> queueDetailsMap = new HashMap<String, Object>();
        queueDetailsMap.put(HOST_IP, hostIP);
        Map<String, Object> ruleCacheMap = new HashMap<String, Object>();
        ruleCacheMap.put(QUEUE_SIZE, new Integer(this.ruleCacheExecutor.getQueue().size()));
        queueDetailsMap.put(RULE_CACHE, ruleCacheMap);
        for (String name : executors.keySet()) {
            queueDetailsMap.put(name.toLowerCase(), executors.get(name).queueDetails());
        }
        return queueDetailsMap;
    }
    
    public void flushMessages() {
        for (String name : executors.keySet()) {
            executors.get(name).flushMessages();
        }
    }
    
    /**
     * Gets the experiment assignment ratios per day per experiment.
     *
     * @param experiments the list of experiments
     * @param fromDate    the first day to include
     * @param toDate      the last day to include
     * @return a map mapping experiment IDs to their daily values for each of the given days
     */
    /*test*/
    /*
    FIXME: Traffic Analyzer change commented for Datastax-driver-migration release...

    Map<Experiment.ID, Map<OffsetDateTime, Double>> getExperimentAssignmentRatioPerDay(List<Experiment> experiments, OffsetDateTime fromDate, OffsetDateTime toDate) {
        return experiments.parallelStream()
                .collect(Collectors.toMap(Experiment::getID,
                        experiment -> assignmentsRepository.getExperimentBucketAssignmentRatioPerDay(experiment.getID(), fromDate, toDate)));
    }
    */

    /**
     * {@inheritDoc}
     */
    /*
    FIXME: Traffic Analyzer change commented for Datastax-driver-migration release...

    @Override
    public ImmutableMap<String, ?> getExperimentAssignmentRatioPerDayTable(List<Experiment> experiments, Map<Experiment.ID, Integer> experimentPriorities, OffsetDateTime fromDate, OffsetDateTime toDate) {
        Map<Experiment.ID, Map<OffsetDateTime, Double>> assignmentRatios = getExperimentAssignmentRatioPerDay(experiments, fromDate, toDate);

        // Prepare table: fill with labels, priorities, and sampling percentages
        List<Experiment.Label> experimentLabelsList = new ArrayList<>(experiments.size());
        List<Integer> prioritiesList = new ArrayList<>(experiments.size());
        List<Double> samplingPercentagesList = new ArrayList<>(experiments.size());
        for (Experiment tempExperiment : experiments) {
            experimentLabelsList.add(tempExperiment.getLabel());
            prioritiesList.add(experimentPriorities.get(tempExperiment.getID()));
            samplingPercentagesList.add(tempExperiment.getSamplingPercent());
        }

        ImmutableMap.Builder<String, List<?>> assignmentRatioTableBuilder = ImmutableMap.builder();
        assignmentRatioTableBuilder.put("experiments", experimentLabelsList);
        assignmentRatioTableBuilder.put("priorities", prioritiesList);
        assignmentRatioTableBuilder.put("samplingPercentages", samplingPercentagesList);

        // fill table with data
        DateTimeFormatter uiFormat = DateTimeFormatter.ofPattern("M/d/y");
        int days = (int) Duration.between(fromDate, toDate.plusDays(1)).toDays();

        List<Map<String, Object>> assignmentRatioCells = new ArrayList<>(days);

        IntStream.range(0, days)
                .mapToObj(fromDate::plusDays)
                .forEach(date -> {
                            OffsetDateTime dateKey = date.equals(fromDate) ? date : date.truncatedTo(ChronoUnit.DAYS);
                            Map<String, Object> cell = new HashMap<>();
                            cell.put("date", uiFormat.format(date));
                            cell.put("values", experiments.stream()
                                    .map(e -> assignmentRatios.getOrDefault(e.getID(), Collections.emptyMap())
                                            .getOrDefault(dateKey, 0.0))
                                    .collect(Collectors.toList()));
                            assignmentRatioCells.add(cell);
                        }
                );
        assignmentRatioTableBuilder.put("assignmentRatios", assignmentRatioCells);
        return assignmentRatioTableBuilder.build();
    }
    */
}

