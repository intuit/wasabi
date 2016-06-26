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
package com.intuit.wasabi.assignment.impl;

import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.intuit.data.autumn.client.HttpCall;
import com.intuit.data.autumn.client.impl.HttpCallImplWithConnectionPooling;
import com.intuit.hyrule.Rule;
import com.intuit.hyrule.RuleBuilder;
import com.intuit.hyrule.exceptions.InvalidInputException;
import com.intuit.hyrule.exceptions.MissingInputException;
import com.intuit.hyrule.exceptions.TreeStructureException;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.assignment.AssignmentDecorator;
import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.assignmentobjects.*;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.AssignmentExistsException;
import com.intuit.wasabi.exceptions.BucketNotFoundException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.*;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import com.intuit.wasabi.export.DatabaseExport;
import com.intuit.wasabi.export.Envelope;
import com.intuit.wasabi.export.WebExport;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.impl.cassandra.ExperimentRuleCacheUpdateEnvelope;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Assignments implementation
 *
 * @see Assignments
 */
public class AssignmentsImpl implements Assignments {

    protected static final String RULE_CACHE = "ruleCache";
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
    //TODO: instead of provider type, these needs to be factories
    protected Provider<Envelope<AssignmentEnvelopePayload, DatabaseExport>> assignmentDBEnvelopeProvider;
    //TODO: instead of provider type, these needs to be factories
    protected Provider<Envelope<AssignmentEnvelopePayload, WebExport>> assignmentWebEnvelopeProvider;
    // Rule-Cache queue set up
    protected RuleCache ruleCache;
    /**
     * Proxy related info
     */
    private HttpCall<PersonalizationEngineResponse> httpCall = new HttpCallImplWithConnectionPooling<>();
    private Priorities priorities;
    private Pages pages;
    private EventLog eventLog;

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
     * @param assignmentDBEnvelopeProvider        AssignmentDBEnvelopeProvider
     * @param assignmentWebEnvelopeProvider       AssignmentWebEnvelopeProvider
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
                           final Provider<Envelope<AssignmentEnvelopePayload, DatabaseExport>> assignmentDBEnvelopeProvider,
                           final Provider<Envelope<AssignmentEnvelopePayload, WebExport>> assignmentWebEnvelopeProvider,
                           final @Nullable AssignmentDecorator assignmentDecorator,
                           final @Named("ruleCache.threadPool") ThreadPoolExecutor ruleCacheExecutor,
                           final EventLog eventLog)
            throws IOException, ConnectionException {
        super();

        this.executors = executors;
        this.repository = repository;
        // FIXME:
        this.random = new SecureRandom();
        this.ruleCache = ruleCache;
        this.pages = pages;
        this.priorities = priorities;
        this.assignmentDBEnvelopeProvider = assignmentDBEnvelopeProvider;
        this.assignmentWebEnvelopeProvider = assignmentWebEnvelopeProvider;
        this.assignmentDecorator = assignmentDecorator;
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
        if (assignment == null) {
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
                    assignment = generateAssignment(experiment, userID, context, selectBucket, currentDate);
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
     * Return an existing assignment for a user, or potentially create a new
     * assignment if the user is assignable to this experiment. Includes a Page.Name to identify the page through which
     * the assignment was delivered.
     */
    @Override
    public Assignment getAssignment(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
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
        if (assignment == null) {
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
                    assignment = generateAssignment(experiment, userID, context, selectBucket, bucketList, currentDate);
                    assert assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT :
                            new StringBuilder("Assignment status should have been NEW_ASSIGNMENT for ")
                                    .append("userID = \"").append(userID).append("\", experiment = \"")
                                    .append(experiment).append("\"").toString();
                } else {
                    // Make a call to Rule cache update
                    ruleCacheExecutor.execute(new ExperimentRuleCacheUpdateEnvelope(experiment.getRule(),
                            ruleCache, experimentID));
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

        // Ingest data to real time data ingestion systems if executors exist
        for (String name : executors.keySet()) {
            executors.get(name).execute(new AssignmentEnvelopePayload(userID, context, createAssignment, false,
                    ignoreSamplingPercent, segmentationProfile, assignment != null ? assignment.getStatus() : null,
                    assignment != null ? assignment.getBucketLabel() : null, pageName, applicationName, experimentLabel,
                    experimentID, currentDate, headers));
        }

        // Updating rule cache.  This will cause future assignment calls, on this server, to
        // use the new version of the rule, if it has recently been changed.
        ruleCacheExecutor.execute(new ExperimentRuleCacheUpdateEnvelope(experiment.getRule(),
                ruleCache, experimentID));

        return assignment;
    }

    @Override
    public List<Map> doBatchAssignments(User.ID userID, Application.Name applicationName, Context context,
                                        boolean createAssignment, boolean forceInExperiment, HttpHeaders headers,
                                        ExperimentBatch experimentBatch, Page.Name pageName,
                                        Map<Experiment.ID, Boolean> allowAssignments) {

        // Get the metadata of all the experiments for this application
        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments = repository.getExperimentList(applicationName);

        List<Map> allAssignments = new ArrayList<>();

        // Get the assignments for userID across all experiments in applicationName for the context
        Table<Experiment.ID, Experiment.Label, String> userAssignments =
                assignmentsRepository.getAssignments(userID, applicationName, context, allExperiments);
        PrioritizedExperimentList appPriorities = priorities.getPriorities(applicationName, false);
        Set<Experiment.ID> experimentSet = allExperiments.rowKeySet();
        Map<Experiment.ID, BucketList> bucketList = getBucketList(experimentSet);
        Map<Experiment.ID, List<Experiment.ID>> exclusives = getExclusivesList(experimentSet);

        // iterate over all experiments in the application in priority order
        for (PrioritizedExperiment experiment : appPriorities.getPrioritizedExperiments()) {
            //check if the experiment was given in experimentBatch
            if (experimentBatch.getLabels().contains(experiment.getLabel())) {
                String labelStr = experiment.getLabel().toString();
                Map<String, Object> tempResult = new HashMap<>();
                // get the assignment for user in this experiment
                Experiment.Label label = Experiment.Label.valueOf(labelStr);
                tempResult.put("experimentLabel", label);
                SegmentationProfile segmentationProfile = SegmentationProfile.from(experimentBatch.getProfile())
                        .build();

                try {
                    Assignment assignment = getAssignment(userID, applicationName, label,
                            context, allowAssignments != null ? allowAssignments.get(experiment.getID()) : createAssignment,
                            forceInExperiment, segmentationProfile,
                            headers, pageName, allExperiments.get(experiment.getID(), experiment.getLabel()),
                            bucketList.get(experiment.getID()), userAssignments, exclusives);


                    // This wouldn't normally happen because we specified CREATE=true
                    if (assignment == null) {
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

                        if (assignment.getBucketLabel() != null) {
                            Bucket bucket = repository.getBucket(experiment.getID(), assignment.getBucketLabel());
                            tempResult.put("payload",
                                    bucket.getPayload() != null
                                            ? bucket.getPayload()
                                            : null);
                        }
                    }

                    tempResult.put("status", assignment.getStatus());

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
        return allAssignments;
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
            BucketList buckets = repository.getBuckets(experimentID);
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
            List<Experiment.ID> exclusiveIDs = exclusives.getExperiments().stream().filter(exp -> exp.getState() == Experiment.State.RUNNING ||
                    exp.getState() == Experiment.State.PAUSED).map(Experiment::getID).collect(Collectors.toList());

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
        return experimentIDList.stream().filter(experimentID -> !userAssignments.row(experimentID).values().isEmpty() &&
                !"null".equals(userAssignments.row(experimentID).values().iterator().next())).collect(Collectors.toSet());
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

        List<PageExperiment> pageExperimentList = pages.getExperiments(applicationName, pageName);
        Set<Experiment.Label> experimentLabels = new HashSet<>(pageExperimentList.size());
        Map<Experiment.ID, Boolean> allowAssignments = new HashMap<>(pageExperimentList.size());
        for (PageExperiment pageExperiment : pageExperimentList) {
            allowAssignments.put(pageExperiment.getId(), pageExperiment.getAllowNewAssignment());
            experimentLabels.add(pageExperiment.getLabel());
        }
        ExperimentBatch.Builder experimentBatchBuilder = ExperimentBatch.newInstance();
        experimentBatchBuilder.withLabels(experimentLabels);
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

    Assignment generateAssignment(Experiment experiment, User.ID userID, Context context,
                                  boolean selectBucket, Date date) {

        Assignment.Builder builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(userID)
                .withContext(context);

        if (selectBucket) {
            /*
            With the bucket state in effect, this part of the code could also result in
            the generation of a null assignment in cases where the selected bucket is CLOSED or EMPTY
            In CLOSED state, the current bucket assignments to the bucket are left as is and all the
            assignments to this bucket are made null going forward.

            In EMPTY state, the current bucket assignments are made null as well as all the future assignments
            to this bucket are made null.

            Retrieves buckets from Repository if personalization is not enabled and if skipBucketRetrieval is false
            Retrieves buckets from DE if personalization is enabled
            * */
            BucketList buckets = getBucketList(experiment, false);
            Bucket assignedBucket = selectBucket(buckets.getBuckets());

            //check that at least one bucket was open
            if (assignedBucket != null) {
                //create the bucket with bucketlabel
                builder.withBucketLabel(assignedBucket.getLabel());
            } else {
                return nullAssignment(userID, experiment.getApplicationName(), experiment.getID(),
                        Assignment.Status.NO_OPEN_BUCKETS);
            }

        } else {
            builder.withBucketLabel(null);
        }

        Assignment result = builder.build();
        return assignmentsRepository.assignUser(result, experiment, date);
    }

    private Assignment generateAssignment(Experiment experiment, User.ID userID, Context context, boolean selectBucket,
                                          BucketList buckets, Date date) {

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
            Retrieves buckets from DE if personalization is enabled
            */
            assignedBucket = selectBucket(buckets.getBuckets());
            //check that at least one bucket was open
            if (assignedBucket != null) {
                //create the bucket with bucketlabel
                builder.withBucketLabel(assignedBucket.getLabel());
            } else {
                return nullAssignment(userID, experiment.getApplicationName(), experiment.getID(),
                        Assignment.Status.NO_OPEN_BUCKETS);
            }

        } else {
            builder.withBucketLabel(null);
        }

        Assignment result = builder.build();
        return assignmentsRepository.assignUser(result, experiment, date);
    }

    /**
     * Returns a bucketList depending on whether it is a personalization experiment or not.
     * Retrieves buckets from DE if personalization is enabled
     * Retrieves buckets from Repository if personalization is not enabled and if skipBucketRetrieval is false
     *
     * @param experiment          Experiment
     * @param skipBucketRetrieval If false, retrieves buckets from Repository
     * @return BucketList
     */
    protected BucketList getBucketList(Experiment experiment, Boolean skipBucketRetrieval) {
        BucketList buckets = null;
        if (!skipBucketRetrieval) {
            buckets = repository.getBuckets(experiment.getID());
        }
        return buckets;
    }

    protected Double rollDie() {
        return random.nextDouble();
    }

    protected Bucket selectBucket(List<Bucket> buckets) {

        final double dieRoll = rollDie();

        // Sort the buckets consistently (by label)
        Collections.sort(buckets,
                (b1, b2) -> b1.getAllocationPercent().compareTo(
                        b2.getAllocationPercent()));

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

    /**
     * Merging response from personalization engine with segmentation profile.
     *
     * @param segmentationProfile           Segmentation Profile
     * @param personalizationEngineResponse Personalization Engine Response
     * @return SegmentationProfile the merged segmentation profile
     */
    SegmentationProfile mergePersonalizationResponseWithSegmentation(SegmentationProfile segmentationProfile, PersonalizationEngineResponse personalizationEngineResponse) {

        if (personalizationEngineResponse != null) {
            segmentationProfile.addAttribute("tid", personalizationEngineResponse.getTid());
            segmentationProfile.addAttribute("data", personalizationEngineResponse.getData());
            segmentationProfile.addAttribute("model", personalizationEngineResponse.getModel());
        }
        return segmentationProfile;
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
        Map<String, Integer> queueLengthMap = new HashMap<>();
        queueLengthMap.put(RULE_CACHE, this.ruleCacheExecutor.getQueue().size());
        for (String name : executors.keySet()) {
            queueLengthMap.put(name.toLowerCase(), executors.get(name).queueLength());
        }
        return queueLengthMap;
    }

}
