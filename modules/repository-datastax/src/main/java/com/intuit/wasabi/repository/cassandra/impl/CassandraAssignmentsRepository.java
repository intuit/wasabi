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
package com.intuit.wasabi.repository.cassandra.impl;

import com.codahale.metrics.annotation.Timed;
import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.UnavailableException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketAssignmentCount;
import com.intuit.wasabi.analyticsobjects.counts.TotalUsers;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.DateHour;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBatch;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.DatabaseRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.UninterruptibleUtil;
import com.intuit.wasabi.repository.cassandra.accessor.BucketAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExclusionAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.StagingAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.count.BucketAssignmentCountAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.export.UserAssignmentExportAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentUserIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.PageExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.export.UserAssignmentExport;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentUserByUserIdContextAppNameExperimentId;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class CassandraAssignmentsRepository implements AssignmentsRepository {
    public static final Bucket.Label NULL_LABEL = Bucket.Label.valueOf("NULL");
    private final Logger LOGGER = LoggerFactory.getLogger(CassandraAssignmentsRepository.class);

    private final ExperimentRepository experimentRepository;
    private final ExperimentRepository dbRepository;
    private final EventLog eventLog;
    private final MappingManager mappingManager;
    private final boolean assignUserToExport;
    private final boolean assignBucketCount;
    private final String defaultTimeFormat;

    final ThreadPoolExecutor assignmentsCountExecutor;

    private ExperimentAccessor experimentAccessor;
    private ExperimentUserIndexAccessor experimentUserIndexAccessor;

    private UserAssignmentExportAccessor userAssignmentExportAccessor;

    private BucketAccessor bucketAccessor;
    private BucketAssignmentCountAccessor bucketAssignmentCountAccessor;

    private StagingAccessor stagingAccessor;
    private PrioritiesAccessor prioritiesAccessor;
    private ExclusionAccessor exclusionAccessor;
    private CassandraDriver driver;

    @Inject
    public CassandraAssignmentsRepository(
            @CassandraRepository ExperimentRepository experimentRepository,
            @DatabaseRepository ExperimentRepository dbRepository,
            EventLog eventLog,
            ExperimentAccessor experimentAccessor,
            ExperimentUserIndexAccessor experimentUserIndexAccessor,

            UserAssignmentExportAccessor userAssignmentExportAccessor,

            BucketAccessor bucketAccessor,
            BucketAssignmentCountAccessor bucketAssignmentCountAccessor,

            StagingAccessor stagingAccessor,
            PrioritiesAccessor prioritiesAccessor,
            ExclusionAccessor exclusionAccessor,
            PageExperimentIndexAccessor pageExperimentIndexAccessor,
            CassandraDriver driver,
            MappingManager mappingManager,
            @Named("AssignmentsCountThreadPoolExecutor") ThreadPoolExecutor assignmentsCountExecutor,
            final @Named("assign.user.to.export") boolean assignUserToExport,
            final @Named("assign.bucket.count") boolean assignBucketCount,
            final @Named("default.time.format") String defaultTimeFormat) {

        this.experimentRepository = experimentRepository;
        this.dbRepository = dbRepository;
        this.eventLog = eventLog;
        this.mappingManager = mappingManager;
        this.assignUserToExport = assignUserToExport;
        this.assignBucketCount = assignBucketCount;
        this.defaultTimeFormat = defaultTimeFormat;
        //Experiment related accessors
        this.experimentAccessor = experimentAccessor;
        this.experimentUserIndexAccessor = experimentUserIndexAccessor;
        //UserAssignment related accessors
        this.userAssignmentExportAccessor = userAssignmentExportAccessor;
        //Bucket related accessors
        this.bucketAccessor = bucketAccessor;
        this.bucketAssignmentCountAccessor = bucketAssignmentCountAccessor;
        //Staging related accessor
        this.stagingAccessor = stagingAccessor;
        this.prioritiesAccessor = prioritiesAccessor;
        this.exclusionAccessor = exclusionAccessor;
        this.driver = driver;
        this.assignmentsCountExecutor = assignmentsCountExecutor;
    }

    Stream<ExperimentUserByUserIdContextAppNameExperimentId> getUserIndexStream(String userId,
                                                                                String appName,
                                                                                String context) {
        Stream<ExperimentUserByUserIdContextAppNameExperimentId> resultStream = Stream.empty();
        try {
            final Result<ExperimentUserByUserIdContextAppNameExperimentId> result =
                    experimentUserIndexAccessor.selectBy(userId, appName, context);
            resultStream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false);
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve assignments for " +
                    "experimentID = \"" + appName + "\" userID = \"" +
                    userId + "\" and context " + context, e);
        }
        return resultStream;
    }

    /**
     * Populate experiment metadata asynchronously at one place.
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
    @Override
    @Timed
    public void populateAssignmentsMetadata(User.ID userID, Application.Name appName, Context context, ExperimentBatch experimentBatch, Optional<Map<Experiment.ID, Boolean>> allowAssignments,
                                            PrioritizedExperimentList prioritizedExperimentList,
                                            Map<Experiment.ID, com.intuit.wasabi.experimentobjects.Experiment> experimentMap,
                                            Map<Experiment.ID, BucketList> bucketMap,
                                            Map<Experiment.ID, List<Experiment.ID>> exclusionMap
    ) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("populateExperimentMetadata - STARTED: userID={}, appName={}, context={}, experimentBatch={}, experimentIds={}", userID, appName, context, experimentBatch, allowAssignments);
        if (isNull(experimentBatch.getLabels()) && !allowAssignments.isPresent()) {
            LOGGER.error("Invalid input to CassandraAssignmentsRepository.populateExperimentMetadata(): Given input: userID={}, appName={}, context={}, experimentBatch={}, allowAssignments={}", userID, appName, context, experimentBatch, allowAssignments);
            return;
        }

        //Populate experiments map, prioritized experiments list and existing user assignments.
        populateExperimentApplicationAndUserAssignments(userID, appName, context, prioritizedExperimentList, experimentMap);

        //Populate experiments ids of given batch
        Set<Experiment.ID> experimentIds = allowAssignments.isPresent() ? allowAssignments.get().keySet() : new HashSet<>();
        populateExperimentIdsAndExperimentBatch(allowAssignments, experimentMap, experimentBatch, experimentIds);

        //Based on given experiment ids, populate experiment buckets and exclusions..
        populateBucketsAndExclusions(experimentIds, bucketMap, exclusionMap);

        if (LOGGER.isDebugEnabled()) LOGGER.debug("populateExperimentMetadata - FINISHED...");
    }

    /**
     * @param userID                    Input: Given user id
     * @param appName                   Input: Given application name
     * @param context                   Input: Given context
     * @param prioritizedExperimentList Output: prioritized experiment list of ALL the experiments for the given application.
     * @param experimentMap             Output: Map of 'experiment id TO experiment' of ALL the experiments for the given application.
     */
    private void populateExperimentApplicationAndUserAssignments(User.ID userID, Application.Name appName, Context context,
                                                                 PrioritizedExperimentList prioritizedExperimentList,
                                                                 Map<Experiment.ID, com.intuit.wasabi.experimentobjects.Experiment> experimentMap) {
        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Application>> applicationFuture = null;
        ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment>> experimentsFuture = null;
        ListenableFuture<Result<ExperimentUserByUserIdContextAppNameExperimentId>> userAssignmentsFuture = null;

        //Send calls asynchronously
        experimentsFuture = experimentAccessor.asyncGetExperimentByAppName(appName.toString());
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Sent experimentAccessor.asyncGetExperimentByAppName({})", appName);

        applicationFuture = prioritiesAccessor.asyncGetPriorities(appName.toString());
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Sent prioritiesAccessor.asyncGetPriorities({})", appName);

        userAssignmentsFuture = experimentUserIndexAccessor.asyncSelectBy(userID.toString(), appName.toString(), context.toString());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Sent experimentUserIndexAccessor.asyncSelectBy({}, {}, {})", userID, appName, context);

        //Process the Futures in the order that are expected to arrive earlier
        UninterruptibleUtil.getUninterruptibly(experimentsFuture).all().stream().forEach(expPojo -> {
            Experiment exp = ExperimentHelper.makeExperiment(expPojo);
            experimentMap.put(exp.getID(), exp);
        });
        if (LOGGER.isDebugEnabled()) LOGGER.debug("experimentMap=> {}", experimentMap);

        int priorityValue = 1;
        for (com.intuit.wasabi.repository.cassandra.pojo.Application priority : UninterruptibleUtil.getUninterruptibly(applicationFuture).all()) {
            for (UUID uuid : priority.getPriorities()) {
                Experiment exp = experimentMap.get(Experiment.ID.valueOf(uuid));
                prioritizedExperimentList.addPrioritizedExperiment(PrioritizedExperiment.from(exp, priorityValue).build());
                priorityValue += 1;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            for (PrioritizedExperiment exp : prioritizedExperimentList.getPrioritizedExperiments()) {
                LOGGER.debug("prioritizedExperiment=> {} ", exp);
            }
        }
    }

    /**
     * @param experimentIds INPUT: Given batch experiment ids
     * @param bucketMap     Output: Map of 'experiment id TO BucketList' of ONLY experiments which are associated to the given application and page.
     * @param exclusionMap  Output: Map of 'experiment id TO to its mutual experiment ids' of ONLY experiments which are associated to the given application and page.
     */
    private void populateBucketsAndExclusions(Set<Experiment.ID> experimentIds, Map<Experiment.ID, BucketList> bucketMap, Map<Experiment.ID, List<Experiment.ID>> exclusionMap) {
        Map<Experiment.ID, ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket>>> bucketFutureMap = new HashMap<>();
        Map<Experiment.ID, ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion>>> exclusionFutureMap = new HashMap<>();

        //Send calls asynchronously
        experimentIds.stream().forEach(experimentId -> {
            bucketFutureMap.put(experimentId, bucketAccessor.asyncGetBucketByExperimentId(experimentId.getRawID()));
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Sent bucketAccessor.asyncGetBucketByExperimentId ({})", experimentId.getRawID());

            exclusionFutureMap.put(experimentId, exclusionAccessor.asyncGetExclusions(experimentId.getRawID()));
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Sent exclusionAccessor.asyncGetExclusions ({})", experimentId.getRawID());
        });

        //Process the Futures in the order that are expected to arrive earlier
        for (Experiment.ID expId : bucketFutureMap.keySet()) {
            bucketMap.put(expId, new BucketList());
            ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket>> bucketFuture = bucketFutureMap.get(expId);
            UninterruptibleUtil.getUninterruptibly(bucketFuture).all().forEach(bucketPojo -> {
                        bucketMap.get(expId).addBucket(BucketHelper.makeBucket(bucketPojo));
                    }
            );
        }
        if (LOGGER.isDebugEnabled()) LOGGER.debug("bucketMap=> {} ", bucketMap);

        for (Experiment.ID expId : exclusionFutureMap.keySet()) {
            ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion>> exclusionFuture = exclusionFutureMap.get(expId);
            exclusionMap.put(expId, new ArrayList<>());
            UninterruptibleUtil.getUninterruptibly(exclusionFuture).all().forEach(exclusionPojo -> {
                        exclusionMap.get(expId).add(Experiment.ID.valueOf(exclusionPojo.getPair()));
                    }
            );
        }
        if (LOGGER.isDebugEnabled()) LOGGER.debug("exclusionMap=> {} ", exclusionMap);
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
    private void populateExperimentIdsAndExperimentBatch(Optional<Map<Experiment.ID, Boolean>> allowAssignments,
                                                         Map<Experiment.ID, com.intuit.wasabi.experimentobjects.Experiment> experimentMap,
                                                         ExperimentBatch experimentBatch, Set<Experiment.ID> experimentIds) {
        //allowAssignments is EMPTY means experimentBatch.labels are present.
        //Use experimentBatch.labels to populate experimentIds
        if (!allowAssignments.isPresent()) {
            for (Experiment exp : experimentMap.values()) {
                if (experimentBatch.getLabels().contains(exp.getLabel())) {
                    experimentIds.add(exp.getID());
                }
            }
            if (LOGGER.isDebugEnabled()) LOGGER.debug("experimentIds for given experiment labels ({})", experimentIds);
        } else {
            //If allowAssignments IS NOT EMPTY means experimentBatch.labels are NOT provided.
            //Use allowAssignments.experimentIds to populate experimentBatch.labels
            Set<Experiment.Label> expLabels = new HashSet<>();
            for (Experiment.ID expId : experimentIds) {
                Experiment exp = experimentMap.get(expId);
                if (exp != null) {
                    expLabels.add(exp.getLabel());
                }
            }
            experimentBatch.setLabels(expLabels);
            if (LOGGER.isDebugEnabled()) LOGGER.debug("experimentBatch after updating labels ({})", experimentBatch);
        }
    }

    /**
     * Populate existing user assignments for given user, application & context.
     * This method make use of provided experimentMap to eliminate the call to database to fetch experiment object.
     *
     * @param userID        User Id
     * @param appLabel      Application Label
     * @param context       Environment context
     * @param experimentMap experiment map to fetch experiment label
     * @return List of assignments in term of pair of Experiment & Bucket label.
     */
    @Override
    @Timed
    public List<Pair<Experiment, String>> getAssignments(User.ID userID,
                                                         Application.Name appLabel,
                                                         Context context,
                                                         Map<Experiment.ID, Experiment> experimentMap) {
        final Stream<ExperimentUserByUserIdContextAppNameExperimentId> experimentUserStream = getUserIndexStream(userID.toString(), appLabel.toString(), context.getContext());
        List<Pair<Experiment, String>> result = new ArrayList<>();
        experimentUserStream.forEach((ExperimentUserByUserIdContextAppNameExperimentId t) -> {
            Experiment exp = experimentMap.get(Experiment.ID.valueOf(t.getExperimentId()));
            if (nonNull(exp)) {
                result.add(new ImmutablePair<>(exp, Optional.ofNullable(t.getBucket()).orElseGet(() -> "null")));
            } else {
                LOGGER.debug("{} experiment id is not present in the experimentMap...", t.getExperimentId());
            }
        });
        return result;
    }

    Optional<Assignment> getAssignmentFromStream(Experiment.ID experimentID, User.ID userID, Context context, Stream<Assignment.Builder> resultStream) {
        return resultStream.map(t -> {
            /*
            * The requirement for EMPTY buckets is to make the existing AND future assignments null.
            * It is also a requirement to preserve the list of the users assigned to a bucket which has been designated
            * as EMPTY bucket. i.e. We should be able to retrieve the list of users who have been assigned to a bucket
            * before its state was made EMPTY.
            * We will be able to do that if we do not update the existing user assignments in the user_assignment
            * and user_bucket_index  column families.
            * However, the below hack will return a null assignment  even though the existing assignment
            * for a user who had been assigned to an EMPTY bucket is not null
            * */
            //TODO: this is a temporary hack to make the code behave like it was asytnax code.
            //TODO: the comment is intentional left inplace for us to revisit it asap!!!

//            final boolean isBucketEmpty = experimentRepository.getBucket(experimentID, t.getBucketLabel())
//                    .getState()
//                    .equals(Bucket.State.EMPTY);
            Bucket.Label bucketLabel = t.getBucketLabel();
            boolean isBucketEmpty = false;
            if (bucketLabel != null &&
                    experimentRepository.getBucket(experimentID, t.getBucketLabel()).getState().equals(Bucket.State.EMPTY)) {
                bucketLabel = null;
                isBucketEmpty = true;
            }
            t.withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                    .withCacheable(false)
                    .withBucketEmpty(isBucketEmpty)
                    .withBucketLabel(bucketLabel);
//            if (Objects.nonNull(t.getBucketLabel()) && isBucketEmpty ){
//                t.withBucketLabel(bucketLabel);
//            }

            return t.build();
        }).reduce((element, anotherElement) -> { //With reduce, we can detect if there is more than 1 elements in the stream
            throw new RepositoryException("Multiple element fetched from db for experimentId = \""
                    + experimentID + "\" userID = \"" + userID + " context=\"" + context.getContext() + "\"");
        });
    }

    @Override
    @Timed
    public Assignment getAssignment(User.ID userID, Application.Name appName, Experiment.ID experimentID, Context context) {
        ListenableFuture<Result<ExperimentUserByUserIdContextAppNameExperimentId>> resultFuture = experimentUserIndexAccessor.asyncSelectBy(userID.toString(), appName.toString(), experimentID.getRawID(), context.toString());
        Result<ExperimentUserByUserIdContextAppNameExperimentId> assignmentResult = UninterruptibleUtil.getUninterruptibly(resultFuture);

        Stream<ExperimentUserByUserIdContextAppNameExperimentId> assignmentResultStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(assignmentResult.iterator(), Spliterator.ORDERED), false);

        final Stream<Assignment.Builder> assignmentBuilderStream = assignmentResultStream.map(t -> {
            Assignment.Builder builder = Assignment.newInstance(Experiment.ID.valueOf(t.getExperimentId()))
                    .withUserID(User.ID.valueOf(t.getUserId()))
                    .withContext(Context.valueOf(t.getContext()));

            if (nonNull(t.getBucket()) && !t.getBucket().trim().isEmpty()) {
                builder.withBucketLabel(Bucket.Label.valueOf(t.getBucket()));
            }
            return builder;
        });

        Optional<Assignment> assignmentOptional = getAssignmentFromStream(experimentID, userID, context, assignmentBuilderStream);
        return assignmentOptional.isPresent() ? assignmentOptional.get() : null;
    }

    /**
     * Create use assignments in cassandra in a batch.
     *
     * @param assignments pair of experiment and assignment
     * @param date        Date of user assignment
     * @return
     */
    @Override
    @Timed
    public void assignUsersInBatch(List<Pair<Experiment, Assignment>> assignments, Date date) {

        // Submit tasks for each assignment to increment/decrement counts
        incrementCounts(assignments, date);

        // Make entries in experiment_user_index table
        indexExperimentsToUser(assignments);
    }

    /**
     * Submit tasks for each assignment to increment/decrement counts
     *
     * @param assignments
     * @param date
     */
    private void incrementCounts(List<Pair<Experiment, Assignment>> assignments, Date date) {
        boolean countUp = true;
        assignments.forEach(pair -> assignmentsCountExecutor.execute(new AssignmentCountEnvelope(
                this, experimentRepository, dbRepository, pair.getLeft(),
                pair.getRight(), countUp, eventLog, date, assignUserToExport, assignBucketCount)));
        LOGGER.debug("Finished assignmentsCountExecutor");
    }

    /**
     * Make entries in experiment_user_index table
     *
     * @param assignments
     */
    private void indexExperimentsToUser(List<Pair<Experiment, Assignment>> assignments) {
        try {
            Session session = driver.getSession();
            final BatchStatement batchStatement = new BatchStatement(BatchStatement.Type.UNLOGGED);
            assignments.forEach(pair -> {
                Assignment assignment = pair.getRight();
                LOGGER.debug("assignment={}", assignment);
                BoundStatement bs;
                if (isNull(assignment.getBucketLabel())) {
                    bs = experimentUserIndexAccessor.insertBoundStatement(assignment.getUserID().toString(), assignment.getContext().toString(), assignment.getApplicationName().toString(), assignment.getExperimentID().getRawID());
                } else {
                    bs = experimentUserIndexAccessor.insertBoundStatement(assignment.getUserID().toString(), assignment.getContext().toString(), assignment.getApplicationName().toString(), assignment.getExperimentID().getRawID(), assignment.getBucketLabel().toString());
                }
                batchStatement.add(bs);
            });
            session.execute(batchStatement);
            LOGGER.debug("Finished experiment_user_index");
        } catch (Exception e) {
            LOGGER.error("Error occurred while adding data in to experiment_user_index", e);
        }
    }

    void indexExperimentsToUser(Assignment assignment) {
        try {
            if (isNull(assignment.getBucketLabel())) {
                experimentUserIndexAccessor.insertBy(
                        assignment.getUserID().toString(),
                        assignment.getContext().getContext(),
                        assignment.getApplicationName().toString(),
                        assignment.getExperimentID().getRawID()
                );
            } else {
                experimentUserIndexAccessor.insertBy(
                        assignment.getUserID().toString(),
                        assignment.getContext().getContext(),
                        assignment.getApplicationName().toString(),
                        assignment.getExperimentID().getRawID(),
                        assignment.getBucketLabel().toString()
                );
            }
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not index experiment to user \"" + assignment + "\"", e);
        }
    }

    @Override
    public void assignUserToExports(Assignment assignment, Date date) {
        final DateHour dateHour = new DateHour();
        dateHour.setDateHour(date); //TODO: why is this not derived from assignment.getCreated() instead?
        final Date day_hour = dateHour.getDayHour();
        try {
            if (isNull(assignment.getBucketLabel())) {
                userAssignmentExportAccessor.insertBy(assignment.getExperimentID().getRawID(),
                        assignment.getUserID().toString(),
                        assignment.getContext().getContext(),
                        date,
                        day_hour,
                        "NO_ASSIGNMENT",
                        true);
            } else {
                userAssignmentExportAccessor.insertBy(assignment.getExperimentID().getRawID(),
                        assignment.getUserID().toString(),
                        assignment.getContext().getContext(),
                        date,
                        day_hour,
                        assignment.getBucketLabel().toString(),
                        false);
            }
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not save user assignment in user_assignment_export \"" +
                    assignment + "\"", e);
        }

    }

    @Override
    public void deleteAssignment(Experiment experiment, User.ID userID, Context context, Application.Name appName, Assignment currentAssignment) {
        // Deletes the assignment data across all the relevant tables in a consistent manner

        //Note: Only removing the use of user_assignment & user_assignment_bu_userid tables. A separate card is created to completely remove these tables.
        //deleteUserFromLookUp(experiment.getID(), userID, context);
        //deleteAssignmentOld(experiment.getID(), userID, context, appName, currentAssignment.getBucketLabel());

        //Updating the assignment bucket counts by -1 in a asynchronous AssignmentCountEnvelope thread
        // false to subtract 1 from the count for the bucket
        boolean countUp = false;
        assignmentsCountExecutor.execute(new AssignmentCountEnvelope(this, experimentRepository,
                dbRepository, experiment, currentAssignment, countUp, eventLog, null, assignUserToExport,
                assignBucketCount));

        removeIndexExperimentsToUser(userID, experiment.getID(), context, appName);
    }

    void removeIndexExperimentsToUser(User.ID userID, Experiment.ID experimentID, Context context,
                                      Application.Name appName) {
        try {
            experimentUserIndexAccessor.deleteBy(userID.toString(),
                    experimentID.getRawID(),
                    context.getContext(),
                    appName.toString());
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException(
                    "Could not delete index from experiment_user_index for user: " + userID + "to experiment: " +
                            experimentID, e);
        }
    }

    List<Date> getUserAssignmentPartitions(Date fromTime, Date toTime) {
        final LocalDateTime startTime = LocalDateTime.ofInstant(fromTime.toInstant(), ZoneId.systemDefault())
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        final LocalDateTime endTime = LocalDateTime.ofInstant(toTime.toInstant(), ZoneId.systemDefault())
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        final long hours = Duration.between(startTime, endTime).toHours();
        return LongStream
                .rangeClosed(0, hours)
                .mapToObj(startTime::plusHours)
                .map(t -> Date.from(t.atZone(ZoneId.systemDefault()).toInstant()))
                .collect(Collectors.toList());
    }

    //TODO: this is super bad practice. should return a java stream and let the resources who is calling this method handling the stream
    @Override
    public StreamingOutput getAssignmentStream(Experiment.ID experimentID, Context context, Parameters parameters, Boolean ignoreNullBucket) {
        final List<Date> dateHours = getDateHourRangeList(experimentID, parameters);
        final String header = "experiment_id\tuser_id\tcontext\tbucket_label\tcreated\t" + System.getProperty("line.separator");
        final DateFormat formatter = new SimpleDateFormat(defaultTimeFormat);
        formatter.setTimeZone(parameters.getTimeZone());
        final StringBuilder sb = new StringBuilder();
        return (os) -> {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(os, Charset.forName("UTF-8")))) {
                writer.write(header);
                for (Date dateHour : dateHours) {
                    Result<UserAssignmentExport> result;
                    LOGGER.debug("Query user assignment export for experimentID={}, at dateHour={}", experimentID.getRawID(), dateHour);
                    if (ignoreNullBucket) {
                        result = userAssignmentExportAccessor.selectBy(experimentID.getRawID(), dateHour, context.getContext(), false);
                    } else {
                        result = userAssignmentExportAccessor.selectBy(experimentID.getRawID(), dateHour, context.getContext());
                    }

                    for (UserAssignmentExport userAssignmentExport : result) {
                        sb.append(userAssignmentExport.getExperimentId()).append("\t")
                                .append(userAssignmentExport.getUserId()).append("\t")
                                .append(userAssignmentExport.getContext()).append("\t")
                                .append(userAssignmentExport.getBucketLabel()).append("\t")
                                .append(formatter.format(userAssignmentExport.getCreated()))
                                .append(System.getProperty("line.separator"));
                        writer.write(sb.toString());
                        sb.setLength(0);
                    }
                }
            } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
                throw new RepositoryException("Could not retrieve assignment for " +
                        "experimentID = \"" + experimentID, e);
            } catch (IllegalArgumentException | IOException e) {
                throw new RepositoryException("Could not write assignment to stream for " +
                        "experimentID = \"" + experimentID, e);
            }
        };
    }

    List<Date> getDateHourRangeList(Experiment.ID experimentID, Parameters parameters) {
        final Experiment id = experimentRepository.getExperiment(experimentID);
        if (isNull(id)) {
            throw new ExperimentNotFoundException(experimentID);
        }
        final Optional<Date> from_ts = Optional.ofNullable(parameters.getFromTime());
        final Optional<Date> to_ts = Optional.ofNullable(parameters.getToTime());

        // Fetches the relevant partitions for a given time window where the user assignments data resides.
        return getUserAssignmentPartitions(
                from_ts.orElseGet(id::getCreationTime),
                to_ts.orElseGet(Date::new)
        );
    }

    @Override
    @Timed
    public void pushAssignmentToStaging(String type, String exception, String data) {
        try {
            stagingAccessor.insertBy(type, exception, data);
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not push the assignment to staging", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pushAssignmentsToStaging(String type, String exception, Collection<String> data) {
        try {
            Session session = driver.getSession();
            final BatchStatement batchStatement = new BatchStatement(BatchStatement.Type.UNLOGGED);
            final UUID timeUUID = UUIDGen.getTimeUUID();
            data.forEach(message -> {
                LOGGER.debug("message={}", message);
                batchStatement.add(stagingAccessor.batchInsertBy(timeUUID, type, exception, message));
            });
            session.execute(batchStatement);
            LOGGER.debug("Finished pushAssignmentsToStaging");
        } catch (Exception e) {
            LOGGER.error("Error occurred while pushAssignmentsToStaging", e);
            throw new RepositoryException("Error occurred while pushAssignmentsToStaging", e);
        }
    }

    public Map<UUID, String> getBatchPayloadsFromStaging(int batchSize){

        Map<UUID, String> payloads = null;

        try {
            ResultSet result = stagingAccessor.batchSelectBy(batchSize);
            payloads = getPayloadsFromCassandraResult(result);
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e){
            throw new RepositoryException("Could not get the payloads from staging", e);
        }
        return payloads;
    }

    public void deleteFromStaging(UUID timeUUID){

        try{
            stagingAccessor.deleteBy(timeUUID);
        } catch (UnavailableException | NoHostAvailableException e){
            throw new RepositoryException("Could not delete the payloads from staging", e);
        }
    }


    @Override
    public void updateBucketAssignmentCount(Experiment experiment, Assignment assignment, boolean countUp) {
        Optional<Bucket.Label> labelOptional = Optional.ofNullable(assignment.getBucketLabel());
        try {
            if (countUp) {
                bucketAssignmentCountAccessor.incrementCountBy(experiment.getID().getRawID(),
                        labelOptional.orElseGet(() -> NULL_LABEL).toString()
                );
            } else {
                bucketAssignmentCountAccessor.decrementCountBy(experiment.getID().getRawID(),
                        labelOptional.orElseGet(() -> NULL_LABEL).toString()
                );
            }
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not update the bucket count for experiment " + experiment.getID()
                    + " bucket " + labelOptional.orElseGet(() -> NULL_LABEL).toString(), e);
        }
    }

    @Override
    public AssignmentCounts getBucketAssignmentCount(Experiment experiment) {
        Result<com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount> result;
        try {
            result = bucketAssignmentCountAccessor.selectBy(experiment.getID().getRawID());
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not fetch the bucket assignment counts for experiment " + experiment.getID(), e);
        }
        return getBucketAssignmentCountFromCassandraResult(experiment.getID(), result);
    }

    @Override
    public Map<Experiment.ID, AssignmentCounts> getBucketAssignmentCountsInParallel(List<Experiment.ID> experimentIds) {
        Map<Experiment.ID, ListenableFuture<Result<com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount>>>
                experimentToBucketAssignmentFutures = new HashMap<>();
        Map<Experiment.ID, AssignmentCounts> experimentToAssignmentCounts = new HashMap<>();
        experimentIds.forEach(experimentId -> experimentToBucketAssignmentFutures.put(experimentId,
                bucketAssignmentCountAccessor.selectByAsync(experimentId.getRawID())));
        experimentIds.forEach(experimentId ->
            experimentToAssignmentCounts.put(experimentId,
                    getBucketAssignmentCountFromCassandraResult(experimentId,
                            UninterruptibleUtil.getUninterruptibly(experimentToBucketAssignmentFutures.get(experimentId)))));
        return experimentToAssignmentCounts;
    }

    private AssignmentCounts getBucketAssignmentCountFromCassandraResult
            (Experiment.ID experimentId,
             Result<com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount> result) {
        List<BucketAssignmentCount> bucketAssignmentCountList = new ArrayList<>();
        AssignmentCounts.Builder assignmentCountsBuilder = new AssignmentCounts.Builder()
                .withBucketAssignmentCount(bucketAssignmentCountList)
                .withExperimentID(experimentId);

        if (isNull(result)) {
            bucketAssignmentCountList.add(new com.intuit.wasabi.analyticsobjects.counts.BucketAssignmentCount.Builder()
                    .withBucket(null)
                    .withCount(0)
                    .build());
            assignmentCountsBuilder
                    .withTotalUsers(new TotalUsers.Builder()
                            .withBucketAssignments(0)
                            .withNullAssignments(0)
                            .withTotal(0)
                            .build());
        } else {
            long totalAssignments = 0;
            long totalNullAssignments = 0;
            for (com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount bucketAssignmentCount : result) {
                final Bucket.Label label = isNull(bucketAssignmentCount.getBucketLabel()) ?
                        NULL_LABEL : Bucket.Label.valueOf(bucketAssignmentCount.getBucketLabel());
                totalAssignments += bucketAssignmentCount.getCount();
                final BucketAssignmentCount.Builder builder = new BucketAssignmentCount.Builder()
                        .withCount(bucketAssignmentCount.getCount());
                if (NULL_LABEL.equals(label)) {
                    totalNullAssignments += bucketAssignmentCount.getCount();
                    bucketAssignmentCountList.add(builder.withBucket(null).build());
                } else {
                    bucketAssignmentCountList.add(builder.withBucket(label).build());
                }
            }
            assignmentCountsBuilder
                    .withTotalUsers(new TotalUsers.Builder()
                            .withBucketAssignments(totalAssignments - totalNullAssignments)
                            .withNullAssignments(totalNullAssignments)
                            .withTotal(totalAssignments)
                            .build());
        }
        return assignmentCountsBuilder.build();
    }

    private Map<UUID, String> getPayloadsFromCassandraResult(ResultSet result){
        List<Row> resultRows = null;
        Map<UUID, String> payloads = new HashMap<>();
        if(result != null && !result.isExhausted()){
            resultRows = result.all();
            for(Row row : resultRows){
                UUID time = row.get("time", UUID.class);
                String payload = row.getString("msg");
                payloads.put(time, payload);
            }
        }
        return payloads;
    }
}