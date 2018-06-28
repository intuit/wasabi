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

import com.datastax.driver.core.exceptions.ConnectionException;
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
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.assignment.AssignmentDecorator;
import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
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
import com.intuit.wasabi.exceptions.InvalidAssignmentStateException;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBatch;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.cassandra.impl.ExperimentRuleCacheUpdateEnvelope;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_ENABLED;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.RULECACHE_THREADPOOL;
import static com.intuit.wasabi.assignmentobjects.Assignment.Status.ASSIGNMENT_FAILED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

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
    private Pages pages;

    private EventLog eventLog;
    private String hostIP;

    private Boolean metadataCacheEnabled;
    private AssignmentsMetadataCache metadataCache;

    private Experiments experimentUtil;


    /**
     * Helper for unit tests
     *
     * @param assignmentRepository
     */
    AssignmentsImpl(AssignmentsRepository assignmentRepository) {
        // FIXME
//        eventLog = new NoopEventLogImpl();
        repository = null;
        assignmentsRepository = assignmentRepository;
        random = null;
    }

    /**
     * Constructor
     *
     * @param repository            CassandraRepository to connect to
     * @param assignmentsRepository reference to AssignmentsRepository
     * @param ruleCache             RuleCache which has cached segmentation rules
     * @param pages                 Pages for this experiment
     * @param assignmentDecorator   The assignmentDecorator to be used
     * @param ruleCacheExecutor     The rule cache executor to be used
     * @param eventLog              eventLog
     * @param metadataCache         Assignments metadata cache
     * @throws IOException         io exception
     * @throws ConnectionException connection exception
     */
    @Inject
    public AssignmentsImpl(Map<String, AssignmentIngestionExecutor> executors,
                           final @CassandraRepository ExperimentRepository repository,
                           final AssignmentsRepository assignmentsRepository,
                           final RuleCache ruleCache, final Pages pages,
                           final AssignmentDecorator assignmentDecorator,
                           final @Named(RULECACHE_THREADPOOL)
                                   ThreadPoolExecutor ruleCacheExecutor,
                           final EventLog eventLog,
                           final @Named(ASSIGNMENTS_METADATA_CACHE_ENABLED)
                                   Boolean metadataCacheEnabled,
                           final AssignmentsMetadataCache metadataCache,
                           final Experiments experimentUtil)
            throws IOException, ConnectionException {
        super();
        try {
            hostIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            // ignore
        }
        this.executors = executors;
        this.repository = repository;
        this.random = new SecureRandom();
        this.ruleCache = ruleCache;
        this.pages = pages;
        this.assignmentDecorator = assignmentDecorator;
        this.eventLog = eventLog;
        this.ruleCacheExecutor = ruleCacheExecutor;
        this.assignmentsRepository = assignmentsRepository;
        this.eventLog = eventLog;
        this.metadataCacheEnabled = metadataCacheEnabled;
        this.metadataCache = metadataCache;
        this.experimentUtil = experimentUtil;
    }

    //-----------------------------------------------------------------------------------------------------------------
    //--------------------- Assignment API Methods --------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Assignment> doBatchAssignments(User.ID userID, Application.Name applicationName, Context context,
                                               boolean createAssignment, boolean overwrite, HttpHeaders headers,
                                               ExperimentBatch experimentBatch,boolean forceProfileCheck) {
        //Call the common doAssignment() method to either retrieve existing or create new assignments.
        //Here send null for page name & allowAssignments map
        Page.Name pageName = null;
        Map<Experiment.ID, Boolean> allowAssignments = null;
        boolean updateDownstreamSystems = true; //Do update down stream systems while getting existing assignment
        List<Assignment> assignments = doAssignments(userID, applicationName, context, createAssignment, overwrite,
                headers, experimentBatch, pageName, allowAssignments, updateDownstreamSystems,forceProfileCheck);

        return assignments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Assignment> doPageAssignments(Application.Name applicationName, Page.Name pageName, User.ID userID,
                                              Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                              HttpHeaders headers, SegmentationProfile segmentationProfile,boolean forceProfileCheck) {

        //Get the experiments (id & allowNewAssignment only) associated to the given application and page.
        List<PageExperiment> pageExperimentList = getExperiments(applicationName, pageName);

        //Prepare allowAssignments map
        Map<Experiment.ID, Boolean> allowAssignments = new HashMap<>(pageExperimentList.size());
        for (PageExperiment pageExperiment : pageExperimentList) {
            allowAssignments.put(pageExperiment.getId(), pageExperiment.getAllowNewAssignment());
        }

        //Prepare experiment batch
        ExperimentBatch experimentBatch = createExperimentBatch(segmentationProfile, null);

        //Call the common doAssignment() method to either retrieve existing and/or create new assignments.
        boolean updateDownstreamSystems = true; //Do update down stream systems while getting existing assignment
        List<Assignment> assignments = doAssignments(userID, applicationName, context,
                createAssignment, ignoreSamplingPercent, headers, experimentBatch, pageName, allowAssignments,
                updateDownstreamSystems,forceProfileCheck);

        return assignments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Assignment doSingleAssignment(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                         Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                         SegmentationProfile segmentationProfile, HttpHeaders headers, boolean forceProfileCheck) {

        //Prepare experiment batch
        ExperimentBatch experimentBatch = createExperimentBatch(segmentationProfile, newHashSet(experimentLabel));

        //Call the common doAssignment() method to either retrieve existing or create new assignment.
        Page.Name pageName = null;
        Map<Experiment.ID, Boolean> allowAssignments = null;
        boolean updateDownstreamSystems = true; //Do update down stream systems while getting existing assignment
        List<Assignment> assignments = doAssignments(userID, applicationName, context,
                createAssignment, ignoreSamplingPercent, headers, experimentBatch, pageName, allowAssignments,
                updateDownstreamSystems, forceProfileCheck);

        //Get the final single assignment
        Assignment assignment = null;
        if (nonNull(assignments) && assignments.size() > 0) {
            assignment = assignments.get(0);
        }

        return assignment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Assignment getExistingAssignment(User.ID userID,
                                            Application.Name appName, Experiment.Label experimentLabel,
                                            Context context) {
        boolean ignoreSamplingPercent = false;
        SegmentationProfile segmentationProfile = null;
        HttpHeaders headers = null;
        boolean createAssignment = false;
        Page.Name pageName = null;
        Map<Experiment.ID, Boolean> allowAssignments = null;

        //Prepare experiment batch
        ExperimentBatch experimentBatch = createExperimentBatch(segmentationProfile, newHashSet(experimentLabel));

        //Call the common doAssignment() method to either retrieve existing or create new assignment.
        boolean updateDownstreamSystems = false; //Do not update down stream systems while getting existing assignment
        List<Assignment> assignments = doAssignments(userID, appName, context,
                createAssignment, ignoreSamplingPercent, headers, experimentBatch, pageName, allowAssignments,
                updateDownstreamSystems,false);

        //Get the final single assignment
        Assignment assignment = null;
        if (nonNull(assignments) && assignments.size() > 0) {
            assignment = assignments.get(0);
        }

        return assignment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Assignment putAssignment(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                    Context context, Bucket.Label desiredBucketLabel, boolean overwrite) {

        //check that the experiment is in a valid state
        EnumSet<Experiment.State> validStates = EnumSet.of(Experiment.State.RUNNING,
                Experiment.State.PAUSED);

        Experiment experiment = getExperiment(applicationName, experimentLabel);
        if (isNull(experiment)) {
            throw new ExperimentNotFoundException(experimentLabel);
        }

        Experiment.ID experimentID = experiment.getID();

        if (!validStates.contains(experiment.getState())) {
            throw new InvalidExperimentStateException(experiment.getID(), validStates, experiment.getState());
        }

        //throw exception if assignment already exists for user unless overwrite == true
        Assignment currentAssignment = assignmentsRepository.getAssignment(userID, applicationName, experimentID, context);
        if (!overwrite && currentAssignment != null && !currentAssignment.isBucketEmpty()) {
            throw new AssignmentExistsException(userID, applicationName, experimentLabel);
        }

        //check that the desired bucket is valid
        if (desiredBucketLabel != null) {
            BucketList buckets = getBucketList(experimentID);
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
                .withExperimentLabel(getExperimentLabel(experimentID))
                .withApplicationName(applicationName)
                .withUserID(userID)
                .withContext(context);
        if (desiredBucketLabel != null) {
            builder.withBucketLabel(desiredBucketLabel);
            builder.withPayload(getBucketPayload(experimentID, desiredBucketLabel.toString()));
        } else {
            //first need to delete existing assignment, since otherwise cassandra
            //will not update the assignment
            if (currentAssignment != null && currentAssignment.getBucketLabel() != null) {
                assignmentsRepository.deleteAssignment(experiment, userID, context, applicationName, currentAssignment);
            }
            //then build one with no Bucket
            builder.withBucketLabel(null);
        }
        Date date = new Date();
        builder.withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(Optional.ofNullable(date).orElseGet(Date::new))
                .withCacheable(false);
        Assignment assignment = builder.build();

        // Ingest data to real time data ingestion systems if executors exist
        for (String name : executors.keySet()) {
            executors.get(name).execute(new AssignmentEnvelopePayload(userID, context, false, true, false, null,
                    Assignment.Status.NEW_ASSIGNMENT, assignment.getBucketLabel(), null, applicationName,
                    experimentLabel, experimentID,
                    date, null));
        }

        /**
         * Check whether rapid experiment user cap reached. Proceed with the assignment even if the cap is reached
         * as in case of PUT API, experiment state of "PAUSED" is also allowed for new assignments.
         *
         * Calling this method here ensures that the experiment state is set to "PAUSED" and the cache refreshed if
         * it was in "RUNNING" state and the cap is reached already.
         */
        isAssignmentValidForRapidExperiment(experiment, null);

        //Add new assignment in the database
        assignmentsRepository.assignUsersInBatch(newArrayList(new ImmutablePair<>(experiment, assignment)), date);

        //Convert new assignment object to response map and then return response map
        return assignment;
    }

    //-----------------------------------------------------------------------------------------------------------------
    //--------------------- Other utility methods ---------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------


    /**
     * Create/Retrieve assignments for a given user, application, context and
     * given experiments (experimentBatch/allowAssignments).
     * <p>
     * <p>
     * This method is called from:
     * 1. First directly through API call AssignmentsImpl.doBatchAssignments() => api:/v1/assignments/applications/{applicationName}/users/{userID}
     * In this case pageName and allowAssignments are NULL
     * And ExperimentBatch is provided with experiment labels
     * <p>
     * 2. Second from AssignmentsImpl.doPageAssignments() => api:/v1/assignments/applications/{applicationName}/pages/{pageName}/users/{userID}
     * In this case pageName and allowAssignments are provided.
     * And ExperimentBatch is empty
     * <p>
     * 3. Third from  AssignmentsImpl.doSingleAssignment() => api:/v1/assignments/applications/{applicationName}/experiments/{experiment}/users/{userID}
     * In this case pageName and allowAssignments are NULL
     * And ExperimentBatch is provided with a single experiment label
     * <p>
     * 4. Forth from  AssignmentsImpl.getExistingAssignment() => api:/v1/events/applications/{applicationName}/experiments/{experimentLabel}/users/{userID}
     * In this case pageName and allowAssignments are NULL
     * And ExperimentBatch is provided with a single experiment label
     *
     * @param userID                  the {@link com.intuit.wasabi.assignmentobjects.User.ID} of the person we want the assignment for
     * @param applicationName         the {@link com.intuit.wasabi.experimentobjects.Application.Name} the app we want the assignment for
     * @param context                 the {@link Context} of the assignment call
     * @param createAssignment        <code>true</code> when a new Assignment should be created
     * @param forceInExperiment
     * @param headers                 the {@link HttpHeaders} that can be used by the segmentation
     * @param experimentBatch         the {@link ExperimentBatch} experiment batch for batch assignments
     *                                Experiment labels are NOT PRESENT when called from AssignmentsImpl.doPageAssignments()
     * @param pageName                the {@link com.intuit.wasabi.experimentobjects.Page.Name} the page name for the assignment
     *                                NULL when called from AssignmentsResource.getBatchAssignments()
     *                                PRESENT when called from AssignmentsImpl.doPageAssignments()
     * @param allowAssignments        {@link HashMap} for each experiment whether the assignment is allowed
     *                                NULL when called from AssignmentsResource.getBatchAssignments()
     *                                PRESENT when called from AssignmentsImpl.doPageAssignments()
     * @param updateDownstreamSystems if TRUE then only send assignments events to the downstream systems.
     * @return List of assignment objects
     */
    protected List<Assignment> doAssignments(User.ID userID, Application.Name applicationName, Context context,
                                             boolean createAssignment, boolean forceInExperiment, HttpHeaders headers,
                                             ExperimentBatch experimentBatch, Page.Name pageName,
                                             Map<Experiment.ID, Boolean> allowAssignments, boolean updateDownstreamSystems,
                                             boolean forceProfileCheck) {

        //allowAssignments is NULL when called from AssignmentsResource.getBatchAssignments()
        Optional<Map<Experiment.ID, Boolean>> allowAssignmentsOptional = Optional.ofNullable(allowAssignments);
        PrioritizedExperimentList appPriorities = new PrioritizedExperimentList();
        Map<Experiment.ID, com.intuit.wasabi.experimentobjects.Experiment> experimentMap = new HashMap<>();
        Table<Experiment.ID, Experiment.Label, String> userAssignments = HashBasedTable.create();
        Map<Experiment.ID, BucketList> bucketMap = new HashMap<>();
        Map<Experiment.ID, List<Experiment.ID>> exclusionMap = new HashMap<>();

        //Populate required assignment metadata
        populateAssignmentsMetadata(userID, applicationName, context, experimentBatch, allowAssignmentsOptional, appPriorities, experimentMap, bucketMap, exclusionMap);

        //Now fetch existing user assignments for given user, application & context
        assignmentsRepository.getAssignments(userID, applicationName, context, experimentMap)
                .forEach(assignmentPair -> userAssignments.put(assignmentPair.getLeft().getID(), assignmentPair.getLeft().getLabel(), assignmentPair.getRight().toString()));
        LOGGER.debug("[DB] existingUserAssignments = {}", userAssignments);

        List<Pair<Experiment, Assignment>> assignmentPairs = new LinkedList<>();
        List<Assignment> allAssignments = new LinkedList<>();
        SegmentationProfile segmentationProfile = SegmentationProfile.from(experimentBatch.getProfile()).build();

        //Prepopulate bucket assignment counts for rapid experiments in running state in asynchronous mode
        List<Experiment.ID> experimentIds =
                appPriorities.getPrioritizedExperiments()
                        .stream()
                        .filter(experiment -> (null != experiment.getIsRapidExperiment() && experiment.getIsRapidExperiment())
                                && experiment.getState().equals(Experiment.State.RUNNING))
                        .map(experiment -> experiment.getID())
                        .collect(Collectors.toList());

        Map<Experiment.ID, AssignmentCounts> rapidExperimentToAssignmentCounts =
                assignmentsRepository.getBucketAssignmentCountsInParallel(experimentIds);

        // iterate over all experiments in the application in priority order
        for (PrioritizedExperiment experiment : appPriorities.getPrioritizedExperiments()) {
            LOGGER.debug("Now processing: {}", experiment);

            //check if the experiment was given in experimentBatch
            if (experimentBatch.getLabels().contains(experiment.getLabel())) {
                LOGGER.debug("Experiment ({}) is part of given ExperimentBatch....", experiment.getLabel());

                String labelStr = experiment.getLabel().toString();
                Experiment.Label label = Experiment.Label.valueOf(labelStr);
                Assignment assignment = null;
                try {
                    boolean experimentCreateAssignment = allowAssignmentsOptional.isPresent() ?
                            (allowAssignmentsOptional.get().get(experiment.getID())) : createAssignment;

                    //This method only gets assignment object, and doesn't create new assignments in a database (cassandra)
                    assignment = getAssignment(userID, applicationName, label,
                            context, experimentCreateAssignment,
                            forceInExperiment, segmentationProfile,
                            headers, experimentMap.get(experiment.getID()),
                            bucketMap.get(experiment.getID()), userAssignments, exclusionMap,
                            rapidExperimentToAssignmentCounts, forceProfileCheck);

                    // This wouldn't normally happen because we specified CREATE=true
                    if (isNull(assignment)) {
                        continue;
                    }

                    // Add the assignment to the global list of userAssignments of the user
                    if (assignment.getStatus() != Assignment.Status.EXPERIMENT_EXPIRED) {
                        userAssignments.put(experiment.getID(), experiment.getLabel(),
                                assignment.getBucketLabel() != null ? assignment.getBucketLabel().toString() : "null");
                    }

                    assignmentPairs.add(new ImmutablePair<Experiment, Assignment>(experimentMap.get(experiment.getID()), assignment));

                } catch (WasabiException ex) {
                    LOGGER.error("Exception happened while executing assignment business logic", ex);
                    assignment = nullAssignment(userID, applicationName, experiment.getID(), label, ASSIGNMENT_FAILED);
                }

                allAssignments.add(assignment);
                experimentBatch.getLabels().remove(experiment.getLabel());
            } else {
                LOGGER.debug("Experiment ({}) is NOT part of given ExperimentBatch....", experiment.getLabel());
            }
        }

        //Find if there are any experiment which is not part of application-experiment list
        List<Experiment.Label> pExpLables = appPriorities.getPrioritizedExperiments().stream().map(pExp -> pExp.getLabel()).collect(Collectors.toList());
        experimentBatch.getLabels().forEach(iExpLabel -> {
            if (!pExpLables.contains(iExpLabel)) {
                allAssignments.add(nullAssignment(userID, applicationName, null, Assignment.Status.EXPERIMENT_NOT_FOUND));
            }
        });

        LOGGER.debug("Finished Execute_Assignments_BL, AllAssignments: {} ", allAssignments);

        //Make new assignments in database (cassandra)
        final Date currentDate = new Date();
        Stream<Pair<Experiment, Assignment>> newAssignments = assignmentPairs.stream().filter(pair -> {
            return (nonNull(pair) && pair.getRight().getStatus() == Assignment.Status.NEW_ASSIGNMENT);
        });
        assignmentsRepository.assignUsersInBatch(newAssignments.collect(Collectors.toList()), currentDate);
        LOGGER.debug("Finished Create_Assignments_DB...");

        //Ingest data to real time data ingestion systems if executors exist & if asked to update downstream systems
        if (updateDownstreamSystems) {
            assignmentPairs.forEach(assignmentPair -> {
                Assignment assignment = assignmentPair.getRight();
                Experiment experiment = assignmentPair.getLeft();
                boolean experimentCreateAssignment = allowAssignmentsOptional.isPresent() ? (allowAssignmentsOptional.get().get(experiment.getID())) : createAssignment;
                for (String name : executors.keySet()) {
                    executors.get(name).execute(new AssignmentEnvelopePayload(userID, context, experimentCreateAssignment, false,
                            forceInExperiment, segmentationProfile, assignment != null ? assignment.getStatus() : null,
                            assignment != null ? assignment.getBucketLabel() : null, pageName, applicationName, experiment.getLabel(),
                            experiment.getID(), currentDate, headers));
                }
            });
            LOGGER.debug("Finished Ingest_to_downstream_systems...");
        }

        // Updating rule cache, This will cause future assignment calls, on this server, to
        // use the new version of the rule, if it has recently been changed.
        assignmentPairs.forEach(assignmentPair -> {
            Experiment experiment = assignmentPair.getLeft();
            ruleCacheExecutor.execute(new ExperimentRuleCacheUpdateEnvelope(experiment.getRule(), ruleCache, experiment.getID()));
        });
        LOGGER.debug("Finished update_rule_cache...");

        return allAssignments;
    }

    /**
     * Return an existing assignment for a user. For new assignment, only create a new assignment object,
     * and doesn't create new assignment in a database (cassandra).
     * <p>
     * It is responsibility of caller method to create assignment entries in a database.
     * <p>
     */
    protected Assignment getAssignment(User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                       Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                       SegmentationProfile segmentationProfile, HttpHeaders headers,
                                       Experiment experiment, BucketList bucketList,
                                       Table<Experiment.ID, Experiment.Label, String> userAssignments,
                                       Map<Experiment.ID, List<Experiment.ID>> exclusives,
                                       Map<Experiment.ID, AssignmentCounts> rapidExperimentToAssignmentCounts,
                                       boolean forceProfileCheck) {
        final Date currentDate = new Date();
        final long currentTime = currentDate.getTime();

        if (experiment == null) {
            return nullAssignment(userID, applicationName, null, Assignment.Status.EXPERIMENT_NOT_FOUND);
        }

        Experiment.ID experimentID = experiment.getID();

        if (experiment.getState() == Experiment.State.TERMINATED) {
            throw new InvalidExperimentStateException(new StringBuilder("Should not be able to access terminated experiment \"")
                    .append(experimentID).append("\" via label \"")
                    .append(experimentLabel).append("\"").toString());
        }

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

        if (forceProfileCheck) {
            return getAssignmentForceProfileCheck(experimentID, userID, applicationName, experimentLabel,
                    context, createAssignment, ignoreSamplingPercent, segmentationProfile,
                    headers, experiment, bucketList, userAssignments, exclusives,
                    rapidExperimentToAssignmentCounts);
        } else {
            return getAssignment(experimentID, userID, applicationName, experimentLabel,
                    context, createAssignment, ignoreSamplingPercent, segmentationProfile,
                    headers, experiment, bucketList, userAssignments, exclusives,
                    rapidExperimentToAssignmentCounts);
        }

    }

    private Assignment getAssignment(Experiment.ID experimentID, User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                     Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                     SegmentationProfile segmentationProfile, HttpHeaders headers,
                                     Experiment experiment, BucketList bucketList,
                                     Table<Experiment.ID, Experiment.Label, String> userAssignments,
                                     Map<Experiment.ID, List<Experiment.ID>> exclusives,
                                     Map<Experiment.ID, AssignmentCounts> rapidExperimentToAssignmentCounts) {
        final Date currentDate = new Date();

        Assignment assignment = getAssignment(experimentID, userID, context, userAssignments, bucketList);
        if (assignment == null || assignment.isBucketEmpty()) {
            if (createAssignment) {
                if (experiment.getState() == Experiment.State.PAUSED ||
                        !isAssignmentValidForRapidExperiment(experiment, rapidExperimentToAssignmentCounts)) {
                    return nullAssignment(userID, applicationName, experimentID, Assignment.Status.EXPERIMENT_PAUSED);
                }

                // Generate a new assignment
                double samplePercent = experiment.getSamplingPercent();
                if (!(samplePercent >= 0.0 && samplePercent <= 1.0)) {
                    throw new InvalidExperimentStateException(new StringBuilder("Sample percent must be between 0.0 and 1.0 for experiment \"")
                            .append(experimentID).append("\" via label \"")
                            .append(experimentLabel).append("\"").toString());
                }

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

                    //Create an assignment object with status as NEW_ASSIGNMENT / NO_OPEN_BUCKETS
                    assignment = createAssignmentObject(experiment, userID, context, selectBucket, bucketList, currentDate, segmentationProfile);

                    if (assignment.getStatus() != Assignment.Status.NEW_ASSIGNMENT && assignment.getStatus() != Assignment.Status.NO_OPEN_BUCKETS) {
                        throw new InvalidAssignmentStateException(userID, applicationName, experimentLabel, assignment.getStatus(), null);
                    }

                } else {
                    return nullAssignment(userID, applicationName, experimentID,
                            Assignment.Status.NO_PROFILE_MATCH);
                }
            }
        } else {
            // Do nothing; user has an assignment, with or without a bucket
            if (assignment.getStatus() != Assignment.Status.EXISTING_ASSIGNMENT) {
                throw new InvalidAssignmentStateException(userID, applicationName, experimentLabel, assignment.getStatus(), null);
            }
        }
        return assignment;
    }

    private Assignment getAssignmentForceProfileCheck(Experiment.ID experimentID, User.ID userID, Application.Name applicationName, Experiment.Label experimentLabel,
                                                      Context context, boolean createAssignment, boolean ignoreSamplingPercent,
                                                      SegmentationProfile segmentationProfile, HttpHeaders headers,
                                                      Experiment experiment, BucketList bucketList,
                                                      Table<Experiment.ID, Experiment.Label, String> userAssignments,
                                                      Map<Experiment.ID, List<Experiment.ID>> exclusives,
                                                      Map<Experiment.ID, AssignmentCounts> rapidExperimentToAssignmentCounts) {

        final Date currentDate = new Date();

        // Check if the current user is selected by the segmentation rule of this experiment
        // when their profile values (and the headers and context) are used in the evaluation.
        // NOTE: If available, this uses the parsed version of the rule for this experiment that
        // has been cached in memory on this system.  That means a recent change to the rule
        // won't be taken into account until the ruleCacheExecutor call below.  This is an optimization
        // to improve performance which can mean that several assignments will use the old version of
        // the rule (until all servers have been updated with the new version).
        if (doesProfileMatch(experiment, segmentationProfile, headers, context)) {
            Assignment assignment = getAssignment(experimentID, userID, context, userAssignments, bucketList);
            if (assignment == null || assignment.isBucketEmpty()) {
                if (createAssignment) {
                    if (experiment.getState() == Experiment.State.PAUSED ||
                            !isAssignmentValidForRapidExperiment(experiment, rapidExperimentToAssignmentCounts)) {
                        return nullAssignment(userID, applicationName, experimentID, Assignment.Status.EXPERIMENT_PAUSED);
                    }

                    // Generate a new assignment
                    double samplePercent = experiment.getSamplingPercent();
                    if (!(samplePercent >= 0.0 && samplePercent <= 1.0)) {
                        throw new InvalidExperimentStateException(new StringBuilder("Sample percent must be between 0.0 and 1.0 for experiment \"")
                                .append(experimentID).append("\" via label \"")
                                .append(experimentLabel).append("\"").toString());
                    }

                    boolean selectBucket = checkMutex(experiment, userAssignments, exclusives) &&
                            (ignoreSamplingPercent || (rollDie() < samplePercent));

                    if (segmentationProfile == null || segmentationProfile.getProfile() == null) {
                        Map profileMap = new HashMap();
                        segmentationProfile = new SegmentationProfile.Builder(profileMap).build();
                    }

                    //Create an assignment object with status as NEW_ASSIGNMENT / NO_OPEN_BUCKETS
                    assignment = createAssignmentObject(experiment, userID, context, selectBucket, bucketList, currentDate, segmentationProfile);

                    if (assignment.getStatus() != Assignment.Status.NEW_ASSIGNMENT && assignment.getStatus() != Assignment.Status.NO_OPEN_BUCKETS) {
                        throw new InvalidAssignmentStateException(userID, applicationName, experimentLabel, assignment.getStatus(), null);
                    }
                }
            } else {
                // Do nothing; user has an assignment, with or without a bucket
                if (assignment.getStatus() != Assignment.Status.EXISTING_ASSIGNMENT) {
                    throw new InvalidAssignmentStateException(userID, applicationName, experimentLabel, assignment.getStatus(), null);
                }
            }
            return assignment;
        } else {
            return nullAssignment(userID, applicationName, experimentID,
                    Assignment.Status.NO_PROFILE_MATCH);
        }

    }

    /**
     * If the experiment is a rapid experiment, it checks whether the user cap has already been achieved.
     * If yes, then it sets the experiment state to paused, refreshes the experiment metadata cache
     * and returns false i.e. assignment is not allowed in that case.
     */
    private boolean isAssignmentValidForRapidExperiment(Experiment experiment, Map<Experiment.ID,
            AssignmentCounts> prefetchedAssignmentCounts) {
        if (experiment.getIsRapidExperiment() != null && experiment.getIsRapidExperiment()) {
            int userCap = experiment.getUserCap();
            AssignmentCounts assignmentCounts = null;
            if (null != prefetchedAssignmentCounts) {
                assignmentCounts = prefetchedAssignmentCounts.get(experiment.getID());
            }
            if (null == assignmentCounts) {
                assignmentCounts = assignmentsRepository.getBucketAssignmentCount(experiment);
            }
            /**
             * The experiment state ideally should be in "RUNNING" state as far as the experiment metadata
             * cache is concerned in order to reach this method in the code flow.
             * We only do a hard load of the experiment state when the caller has still given a go ahead of making
             * the assignment after checking the experiment state in the cache.
             *
             * In case of the PUT API, the experiment might be in "PAUSED" state but assignments are allowed
             * in that scenario too. So, we do not want to update the state and refresh the cache in that scenario
             * as it is an expensive operation.
             */
            if (experiment.getState().equals(Experiment.State.RUNNING) &&
                    assignmentCounts.getTotalUsers().getBucketAssignments() >= userCap) {
                Experiment experimentInfoFromDB = experimentUtil.getExperiment(experiment.getID());
                if (experimentInfoFromDB.getState().equals(Experiment.State.RUNNING)) {
                    experimentUtil.updateExperimentState(experiment, Experiment.State.PAUSED);
                }

                metadataCache.refresh();
                return false;
            }
        }
        return true;
    }

    /**
     * Populate assignments metadata; use metadata cache if it is enabled or use repository to populate from DB
     * <p>
     * experimentIds is NULL when called from AssignmentsResource.getBatchAssignments() => api:/v1/assignments/applications/{applicationName}/users/{userID}
     * experimentBatch.labels are NULL when called from AssignmentsImpl.doPageAssignments() => api:/v1/assignments/applications/{applicationName}/pages/{pageName}/users/{userID}
     *
     * @param userID                    Input: Given user id
     * @param appName                   Input: Given application name
     * @param context                   Input: Given context
     * @param experimentBatch           Input/Output: Given experiment batch. This object will be modified and become one of the output; in the case of AssignmentsImpl.doPageAssignments()
     * @param allowAssignments          Input: Given batch experiment ids with allow assignment flag.
     * @param prioritizedExperimentList Output: prioritized experiment list of ALL the experiments for the given application.
     * @param experimentMap             Output: Map of 'experiment id TO experiment' of ALL the experiments for the given application.
     * @param bucketMap                 Output: Map of 'experiment id TO BucketList' of ONLY experiments which are associated to the given application and page.
     * @param exclusionMap              Output: Map of 'experiment id TO to its mutual experiment ids' of ONLY experiments which are associated to the given application and page.
     */
    private void populateAssignmentsMetadata(User.ID userID, Application.Name appName, Context context, ExperimentBatch experimentBatch, Optional<Map<Experiment.ID, Boolean>> allowAssignments,
                                             PrioritizedExperimentList prioritizedExperimentList,
                                             Map<Experiment.ID, Experiment> experimentMap,
                                             Map<Experiment.ID, BucketList> bucketMap,
                                             Map<Experiment.ID, List<Experiment.ID>> exclusionMap) {
        LOGGER.debug("populateAssignmentsMetadata - STARTED: userID={}, appName={}, context={}, experimentBatch={}, experimentIds={}", userID, appName, context, experimentBatch, allowAssignments);
        if (isNull(experimentBatch.getLabels()) && !allowAssignments.isPresent()) {
            LOGGER.error("Invalid input to AssignmentsImpl.populateAssignmentsMetadata(): Given input: userID={}, appName={}, context={}, experimentBatch={}, allowAssignments={}", userID, appName, context, experimentBatch, allowAssignments);
            return;
        }

        //IF metadata cache is enabled, THEN use metadata cache to populate assignments metadata ELSE use assignments repository to populate assignments metadata
        if (metadataCacheEnabled) {
            //Populate experiments map of all the experiments of given application
            metadataCache.getExperimentsByAppName(appName).forEach(exp -> experimentMap.put(exp.getID(), exp));
            LOGGER.debug("[cache] experimentMap = {}", experimentMap);

            //Populate prioritized experiments list of given application
            Optional<PrioritizedExperimentList> prioritizedExperimentListOptional = metadataCache.getPrioritizedExperimentListMap(appName);
            if (prioritizedExperimentListOptional.isPresent()) {
                prioritizedExperimentListOptional.get().getPrioritizedExperiments().forEach(exp -> prioritizedExperimentList.addPrioritizedExperiment(exp));
            } else {
                //TODO: 1/30/17  What to do if there are no experiments for given application
            }
            LOGGER.debug("[cache] prioritizedExperimentList = {}", prioritizedExperimentList.getPrioritizedExperiments());

            //Populate experiments ids of given batch
            Set<Experiment.ID> experimentIds = allowAssignments.isPresent() ? allowAssignments.get().keySet() : new HashSet<>();
            populateExperimentIdsAndExperimentBatch(allowAssignments, experimentMap, experimentBatch, experimentIds);

            //Based on given experiment ids, populate experiment buckets and exclusions..
            experimentIds.forEach(expId -> {
                bucketMap.put(expId, metadataCache.getBucketList(expId));
                exclusionMap.put(expId, metadataCache.getExclusionList(expId));
            });
            LOGGER.debug("[cache] bucketMap = {}", bucketMap);
            LOGGER.debug("[cache] exclusionMap = {}", exclusionMap);

        } else {
            assignmentsRepository.populateAssignmentsMetadata(userID, appName, context, experimentBatch, allowAssignments, prioritizedExperimentList, experimentMap, bucketMap, exclusionMap);
        }

        LOGGER.debug("populateAssignmentsMetadata - FINISHED...");
    }

    /**
     * Create experiment batch for given segmentation profile and experiment labels
     *
     * @param segmentationProfile
     * @param labels
     * @return ExperimentBatch
     */
    private ExperimentBatch createExperimentBatch(SegmentationProfile segmentationProfile, HashSet<Experiment.Label> labels) {
        ExperimentBatch.Builder experimentBatchBuilder = ExperimentBatch.newInstance();
        if (nonNull(segmentationProfile)) {
            experimentBatchBuilder.withProfile(segmentationProfile.getProfile());
        }
        if (nonNull(labels)) {
            experimentBatchBuilder.withLabels(labels);
        }
        return experimentBatchBuilder.build();
    }


    /**
     * Get list of page experiments associated to given application and page.
     * <p>
     * Use metadata cache if it is enabled or use repository to fetch from Database
     *
     * @param applicationName
     * @param pageName
     * @return
     */
    private List<PageExperiment> getExperiments(Application.Name applicationName, Page.Name pageName) {
        if (metadataCacheEnabled) {
            return metadataCache.getPageExperiments(applicationName, pageName);
        } else {
            return pages.getExperimentsWithoutLabels(applicationName, pageName);
        }
    }


    /**
     * This method is used to :
     * 1.   Populate experimentIds of the given batch only based on experiment labels (experimentBatch).
     * 2.   Populate experiment labels based on given batch experiment ids (allowAssignments).
     *
     * @param allowAssignments - INPUT: if present then it contains given batch experiment ids.
     * @param experimentMap    - INPUT:  Map of all the experiments of the given application.
     * @param experimentBatch  - INPUT/OUTPUT:  if allowAssignments is empty then this contains given batch experiment labels.
     * @param experimentIds    - OUTPUT: Final given batch experiment ids.
     */
    private void populateExperimentIdsAndExperimentBatch(Optional<Map<Experiment.ID, Boolean>> allowAssignments, Map<Experiment.ID, com.intuit.wasabi.experimentobjects.Experiment> experimentMap, ExperimentBatch experimentBatch, Set<Experiment.ID> experimentIds) {
        //allowAssignments is EMPTY means experimentBatch.labels are present.
        //Use experimentBatch.labels to populate experimentIds
        if (!allowAssignments.isPresent()) {
            for (Experiment exp : experimentMap.values()) {
                if (experimentBatch.getLabels().contains(exp.getLabel())) {
                    experimentIds.add(exp.getID());
                }
            }
            LOGGER.debug("Populated experimentIds from given experimentBatch: {}", experimentIds);
        } else {
            //If allowAssignments IS NOT EMPTY means experimentBatch.labels are NOT provided.
            //Use allowAssignments.experimentIds to populate experimentBatch.labels
            Set<Experiment.Label> expLabels = new HashSet<>();
            for (Experiment.ID expId : experimentIds) {
                Experiment exp = experimentMap.get(expId);
                if (nonNull(exp)) {
                    expLabels.add(exp.getLabel());
                }
            }
            experimentBatch.setLabels(expLabels);
            LOGGER.debug("Populated experimentBatch from given experimentIds: {}", experimentBatch);
        }
    }

    /**
     * Get the experiment object for given application & experiment label.
     * <p>
     * If metadata cache is enabled then fetch from cache else fetch from database.
     *
     * @param applicationName
     * @param experimentLabel
     * @return experiment object
     */
    protected Experiment getExperiment(Application.Name applicationName, Experiment.Label experimentLabel) {
        Experiment result = null;

        if (metadataCacheEnabled) {
            //First fetch experiment list sorted by priorities (contains non-deleted experiment-ids).
            //This experiment-priority list is the source of truth for ALL assignment flows while looking up experiment...
            Optional<PrioritizedExperimentList> prioritizedExperimentListOptional = metadataCache.getPrioritizedExperimentListMap(applicationName);
            if (prioritizedExperimentListOptional.isPresent()) {
                //Iterate as per experiment priority and look for the matching experiment by their label
                for (PrioritizedExperiment prioritizedExperiment : prioritizedExperimentListOptional.get().getPrioritizedExperiments()) {
                    if (experimentLabel.equals(prioritizedExperiment.getLabel())) {
                        //Upon match, get the complete experiment object from cache
                        result = metadataCache.getExperimentById(prioritizedExperiment.getID()).orElse(null);
                        break;
                    }
                }
            }
        } else {
            result = repository.getExperiment(applicationName, experimentLabel);
        }

        return result;
    }


    /**
     * Get the experiment label based on experiment ID
     * If metadata cache is enabled then fetch from cache else fetch from database.
     *
     * @param experimentID
     * @return
     */
    protected Experiment.Label getExperimentLabel(Experiment.ID experimentID) {
        if (isNull(experimentID)) return null;
        Experiment.Label label = null;
        Experiment exp = null;
        if (metadataCacheEnabled) {
            Optional<Experiment> expOptional = metadataCache.getExperimentById(experimentID);
            exp = expOptional.isPresent() ? expOptional.get() : null;
        } else {
            exp = repository.getExperiment(experimentID);
        }

        if (nonNull(exp)) {
            label = exp.getLabel();
        }
        return label;
    }

    /**
     * Get bucket list bsed on experiment id
     * If metadata cache is enabled then fetch from cache else fetch from database.
     *
     * @param experimentID
     * @return
     */
    protected BucketList getBucketList(Experiment.ID experimentID) {
        BucketList bucketList;
        if (metadataCacheEnabled) {
            bucketList = metadataCache.getBucketList(experimentID);
        } else {
            bucketList = repository.getBucketList(experimentID);
        }
        return bucketList;
    }

    /**
     * Get the bucket based on experiment id and bucket label
     * If metadata cache is enabled then fetch from cache else fetch from database.
     *
     * @param experimentID
     * @param bucketLabel
     * @return
     */
    protected Optional<Bucket> getBucketByLabel(Experiment.ID experimentID, Bucket.Label bucketLabel) {
        Optional<Bucket> rBucket = Optional.empty();
        BucketList bucketList = null;
        if (metadataCacheEnabled) {
            bucketList = metadataCache.getBucketList(experimentID);
        } else {
            bucketList = repository.getBucketList(experimentID);
        }

        if (isNull(bucketList)) return rBucket;
        for (Bucket bucket : bucketList.getBuckets()) {
            if (bucket.getLabel().equals(bucketLabel)) {
                return Optional.of(bucket);
            }
        }

        return rBucket;
    }

    /**
     * Get the bucket based on experiment id and bucket label
     *
     * @param experimentID
     * @param bucketLabel
     * @return
     */
    protected String getBucketPayload(Experiment.ID experimentID, String bucketLabel) {
        if (isNull(bucketLabel) || bucketLabel.trim().isEmpty()) {
            return null;
        }
        Optional<Bucket> rBucket = getBucketByLabel(experimentID, Bucket.Label.valueOf(bucketLabel));
        String payload = null;
        if (rBucket.isPresent()) {
            payload = rBucket.get().getPayload();
        }
        return payload;
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

    /**
     * Create an assignment object for given user, app, experiment & status with NULL bucket (so NULL assignment)
     *
     * @param userID
     * @param appName
     * @param experimentID
     * @param status
     * @return
     */
    protected Assignment nullAssignment(User.ID userID, Application.Name appName, Experiment.ID experimentID,
                                        Assignment.Status status) {
        return nullAssignment(userID, appName, experimentID, getExperimentLabel(experimentID), status);
    }

    /**
     * Create an assignment object for given user, app, experiment & status with NULL bucket (so NULL assignment)
     *
     * @param userID
     * @param appName
     * @param experimentID
     * @param experimentLabel
     * @param status
     * @return
     */
    protected Assignment nullAssignment(User.ID userID, Application.Name appName, Experiment.ID experimentID, Experiment.Label experimentLabel,
                                        Assignment.Status status) {

        return Assignment.newInstance(experimentID)
                .withExperimentLabel(experimentLabel)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withPayload(null)
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
    protected boolean doesProfileMatch(Experiment experiment, SegmentationProfile segmentationProfile,
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
    protected boolean doesProfileMatch(Experiment experiment, SegmentationProfile segmentationProfile,
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

    /**
     * Get new assignment object for a given experiment from the existing userAssignments table.
     * <p>
     * Returns NULL if there is no assignment in the existing user assignment table for given experiment.
     *
     * @param experimentID
     * @param userID
     * @param context
     * @param userAssignments
     * @param bucketList
     * @return new assignment object or NULL
     */
    protected Assignment getAssignment(final Experiment.ID experimentID,
                                       final User.ID userID, final Context context,
                                       final Table<Experiment.ID, Experiment.Label, String> userAssignments,
                                       final BucketList bucketList) {

        if (userAssignments != null && userAssignments.row(experimentID).isEmpty()) {
            return null;
        } else {
            boolean isBucketEmpty = false;
            String bucketLabel = userAssignments.row(experimentID).values().iterator().next();
            for (Bucket b : bucketList.getBuckets()) {
                if (bucketLabel.equals(b.getLabel().toString())) {
                    if (b.getState() == Bucket.State.EMPTY) {
                        bucketLabel = "null";
                        isBucketEmpty = true;
                        break;
                    } else {
                        break;
                    }
                }
            }
            return Assignment.newInstance(experimentID)
                    .withExperimentLabel(getExperimentLabel(experimentID))
                    .withBucketLabel("null".equals(bucketLabel)
                            ? null
                            : Bucket.Label.valueOf(bucketLabel))
                    .withBucketEmpty(isBucketEmpty)
                    .withPayload(getBucketPayload(experimentID, bucketLabel))
                    .withUserID(userID)
                    .withContext(context)
                    .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                    .build();
        }
    }

    /**
     * This method first create an assignment object for NEW_ASSIGNMENT and then make entries in to the database.
     *
     * @param experiment
     * @param userID
     * @param context
     * @param selectBucket
     * @param bucketList
     * @param date
     * @param segmentationProfile
     * @return new assignment object which is created in the database as well.
     */
    protected Assignment generateAssignment(Experiment experiment, User.ID userID, Context context, boolean selectBucket,
                                            BucketList bucketList, Date date, SegmentationProfile segmentationProfile) {
        Assignment result = createAssignmentObject(experiment, userID, context, selectBucket, bucketList, date, segmentationProfile);
        if (result.getStatus().equals(Assignment.Status.NEW_ASSIGNMENT)) {
            assignmentsRepository.assignUsersInBatch(newArrayList(new ImmutablePair<>(experiment, result)), date);
            return result;
        } else {
            return result;
        }
    }

    /**
     * This method creates an assignment object for NEW_ASSIGNMENT / NO_OPEN_BUCKETS.
     * <p>
     * Note: it does not create entries in the database.
     *
     * @param experiment
     * @param userID
     * @param context
     * @param selectBucket
     * @param bucketList
     * @param date
     * @param segmentationProfile
     * @return new assignment object with status of either NEW_ASSIGNMENT or NO_OPEN_BUCKETS
     */
    protected Assignment createAssignmentObject(Experiment experiment, User.ID userID, Context context, boolean selectBucket,
                                                BucketList bucketList, Date date, SegmentationProfile segmentationProfile) {

        Assignment.Builder builder = Assignment.newInstance(experiment.getID())
                .withExperimentLabel(getExperimentLabel(experiment.getID()))
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
                builder.withPayload(getBucketPayload(experiment.getID(), nonNull(assignedBucket.getLabel()) ? assignedBucket.getLabel().toString() : null));
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
                    bucketList = getBucketList(experiment.getID());
                }
            }
        } else {
            if (!skipBucketRetrieval) {
                bucketList = getBucketList(experiment.getID());
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


    //-----------------------------------------------------------------------------------------------------------------
    //--------------------------- Other API methods -------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamingOutput getAssignmentStream(Experiment.ID experimentID, Context context, Parameters parameters, Boolean ignoreNullBucket) {

        Experiment experiment = repository.getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }
        return assignmentsRepository.getAssignmentStream(experimentID, context, parameters, ignoreNullBucket);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushMessages() {
        for (String name : executors.keySet()) {
            executors.get(name).flushMessages();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearMetadataCache() {
        if (metadataCacheEnabled) {
            metadataCache.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> metadataCacheDetails() {
        Map<String, String> details = new HashMap<>();
        if (metadataCacheEnabled) {
            return metadataCache.getDetails();
        } else {
            details.put("Status", "Assignments metadata cache is not enabled...");
        }
        return details;
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

