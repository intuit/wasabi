package com.intuit.wasabi.repository.cassandra.impl;

import com.codahale.metrics.annotation.Timed;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.UnavailableException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketAssignmentCount;
import com.intuit.wasabi.analyticsobjects.counts.TotalUsers;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.DateHour;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.*;
import com.intuit.wasabi.repository.cassandra.accessor.BucketAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.StagingAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.count.BucketAssignmentCountAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.export.UserAssignmentExportAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentUserIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.UserBucketIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.export.UserAssignmentExport;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentUserByUserIdContextAppNameExperimentId;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

//TODO: rewrite this class to use BatchStatement after moving to datastax
public class CassandraAssignmentsRepository implements AssignmentsRepository {
    public static final Bucket.Label NULL_LABEL = Bucket.Label.valueOf("NULL");
    private final Logger logger = LoggerFactory.getLogger(CassandraAssignmentsRepository.class);

    private final ExperimentRepository experimentRepository;
    private final ExperimentRepository dbRepository;
    private final EventLog eventLog;
    private final MappingManager mappingManager;
    private final boolean assignUserToExport;
    private final boolean assignBucketCount;
    private final String defaultTimeFormat;

    final ThreadPoolExecutor assignmentsCountExecutor;

    private LinkedBlockingQueue assignmentsCountQueue = new LinkedBlockingQueue<>();

    private ExperimentAccessor experimentAccessor;
    private ExperimentUserIndexAccessor experimentUserIndexAccessor;

    private UserAssignmentExportAccessor userAssignmentExportAccessor;

    private BucketAccessor bucketAccessor;
    private UserBucketIndexAccessor userBucketIndexAccessor;
    private BucketAssignmentCountAccessor bucketAssignmentCountAccessor;

    private StagingAccessor stagingAccessor;


    @Inject
    public CassandraAssignmentsRepository(
            @CassandraRepository ExperimentRepository experimentRepository,
            @DatabaseRepository ExperimentRepository dbRepository,
            EventLog eventLog,
            ExperimentAccessor experimentAccessor,
            ExperimentUserIndexAccessor experimentUserIndexAccessor,

            UserAssignmentExportAccessor userAssignmentExportAccessor,

            BucketAccessor bucketAccessor,
            UserBucketIndexAccessor userBucketIndexAccessor,
            BucketAssignmentCountAccessor bucketAssignmentCountAccessor,

            StagingAccessor stagingAccessor,
            MappingManager mappingManager,
            final @Named("export.pool.size") int assignmentsCountThreadPoolSize,
            final @Named("assign.user.to.export") boolean assignUserToExport,
            final @Named("assign.bucket.count") boolean assignBucketCount,
            final @Named("default.time.format") String defaultTimeFormat ){

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
        this.userBucketIndexAccessor = userBucketIndexAccessor;
        this.bucketAssignmentCountAccessor = bucketAssignmentCountAccessor;
        //Staging related accessor
        this.stagingAccessor = stagingAccessor;
        this.assignmentsCountExecutor =  new ThreadPoolExecutor(
                assignmentsCountThreadPoolSize,
                assignmentsCountThreadPoolSize,
                0L,
                MILLISECONDS,
                assignmentsCountQueue);
    }

    Stream<ExperimentUserByUserIdContextAppNameExperimentId> getUserIndexStream(String userId,
                                                                                String appName,
                                                                                String context){
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

    @Override
    @Timed
    public Set<Experiment.ID> getUserAssignments(User.ID userID, Application.Name appLabel, Context context) {
        final Stream<ExperimentUserByUserIdContextAppNameExperimentId> experimentUserStream =
                getUserIndexStream(userID.toString(), appLabel.toString(), context.getContext());
        final Set<Experiment.ID> result = experimentUserStream
                .filter(t -> t.getBucket() != null)
                .filter(t -> t.getExperimentId() != null) //ID must be non null otherwise InvalidIdentifierException
                .map(t -> Experiment.ID.valueOf(t.getExperimentId()))
                .collect(Collectors.toSet());
        return result;
    }

    //TODO: why return the last field as String instead of Bucket.Lable?
    @Override
    @Timed
    public Table<Experiment.ID, Experiment.Label, String> getAssignments(User.ID userID,
                                                                         Application.Name appLabel,
                                                                         Context context,
                                                                         Table<Experiment.ID, Experiment.Label, Experiment> allExperiments) {
        final Stream<ExperimentUserByUserIdContextAppNameExperimentId> experimentUserStream =
                getUserIndexStream(userID.toString(), appLabel.toString(), context.getContext());
        final Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
        experimentUserStream.forEach(t -> {
            Experiment.ID experimentID = Experiment.ID.valueOf(t.getExperimentId());
            allExperiments.row(experimentID).values().stream()
                    .forEach(i -> {
                        result.put(
                                experimentID,
                                i.getLabel(), //expects this to be non-null
                                Optional.ofNullable(t.getBucket()).orElseGet(() ->"null")
                        );
                    });
        });
        return result;
    }

    @Timed
    Optional<Assignment> getAssignmentFromLookUp(Experiment.ID experimentID, Application.Name appName, User.ID userID, Context context) {
        Stream<ExperimentUserByUserIdContextAppNameExperimentId> resultStream;
        try {
            final Result<ExperimentUserByUserIdContextAppNameExperimentId> result =
                    this.experimentUserIndexAccessor.selectBy(
                            userID.toString(),
                            appName.toString(),
                            context.getContext(),
                            experimentID.getRawID()
                    );
            resultStream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(result.iterator(), Spliterator.ORDERED), false);
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not retrieve assignment for " +
                    "experimentID = \"" + experimentID + "\" userID = \"" + userID + "\"", e);
        }
        final Stream<Assignment.Builder> builderStream = resultStream.map(t ->{
            Assignment.Builder builder = Assignment.newInstance(Experiment.ID.valueOf(t.getExperimentId()))
                    .withUserID(User.ID.valueOf(t.getUserId()))
                    .withContext(Context.valueOf(t.getContext()));
            if(Objects.nonNull(t.getBucket())) {
                builder.withBucketLabel(Bucket.Label.valueOf(t.getBucket()));
            }
            return builder;
        });
        return getAssignmentFromStream(experimentID, userID, context, builderStream);
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
                   + experimentID + "\" userID = \"" + userID + " context=\""+context.getContext()+"\"");
        });
    }

    @Override
    @Timed
    public Assignment getAssignment(Experiment.ID experimentID, Application.Name appName, User.ID userID, Context context) {
        Assignment result = getAssignmentFromLookUp(experimentID, appName, userID, context).orElseGet(() -> null);
        return result;
    }


    @Override
    @Timed
    public Assignment assignUser(Assignment assignment, Experiment experiment, Date date) {
        //Writing assignment to the new table - experiment_user_index
        Assignment new_assignment = assignUserToLookUp(assignment, experiment.getApplicationName());

        //Updating the assignment bucket counts, user_assignment_export
        // in a asynchronous AssignmentCountEnvelope thread
        boolean countUp = true;

        assignmentsCountExecutor.execute(new AssignmentCountEnvelope(this, experimentRepository,
                dbRepository, experiment, assignment, countUp, eventLog, date, assignUserToExport, assignBucketCount));

        indexUserToBucket(assignment);
        indexExperimentsToUser(assignment);

        return new_assignment;
    }

    void indexExperimentsToUser(Assignment assignment) {
        try {
            if (Objects.isNull(assignment.getBucketLabel())) {
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
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e){
            throw new RepositoryException("Could not index experiment to user \"" + assignment + "\"", e);
        }
    }

    void indexUserToBucket(Assignment assignment) {
        try{
            if(Objects.isNull(assignment.getBucketLabel())) {
                userBucketIndexAccessor.insertBy(
                        assignment.getExperimentID().getRawID(),
                        assignment.getUserID().toString(),
                        assignment.getContext().getContext(),
                        assignment.getCreated(),
                        new String(new byte[0], StandardCharsets.UTF_8 ) //Needed because of compact storage
                );
            } else {
                userBucketIndexAccessor.insertBy(
                        assignment.getExperimentID().getRawID(),
                        assignment.getUserID().toString(),
                        assignment.getContext().getContext(),
                        assignment.getCreated(),
                        assignment.getBucketLabel().toString()
                );
            }
        }catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e){
            throw new RepositoryException("Could not index user to bucket \"" + assignment + "\"", e);
        }
    }

    /**
     * Adds an assignment associated with a new user
     *
     * @param assignment assignment of the new user
     * @param appName       application Name
     * @return user's assignment
     */
    @Timed
    protected Assignment assignUserToLookUp(Assignment assignment, Application.Name appName) {
        try {
            if (Objects.isNull(assignment.getBucketLabel())) {
                experimentUserIndexAccessor.insertBy(
                        assignment.getUserID().toString(),
                        assignment.getContext().getContext(),
                        appName.toString(),
                        assignment.getExperimentID().getRawID()
                        );
            } else {
                experimentUserIndexAccessor.insertBy(
                        assignment.getUserID().toString(),
                        assignment.getContext().getContext(),
                        appName.toString(),
                        assignment.getExperimentID().getRawID(),
                        assignment.getBucketLabel().toString());
            }
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not save user assignment \"" +
                    assignment + "\"", e);
        }
        return  (Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCacheable(false)
                .build());
    }

    @Deprecated
    @Override
    public Assignment assignUserToOld(Assignment assignment, Date date) {
        throw new NotImplementedException("Method is deprecated");
    }

    @Override
    public void assignUserToExports(Assignment assignment, Date date) {
        final DateHour dateHour = new DateHour();
        dateHour.setDateHour(date); //TODO: why is this not derived from assignment.getCreated() instead?
        final Date day_hour = dateHour.getDayHour();
        try {
            if (Objects.isNull(assignment.getBucketLabel())) {
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
        //TODO: make all assignment/delete part of batchstatement
        deleteUserFromLookUp(experiment.getID(), userID, context, appName);

        //Updating the assignment bucket counts by -1 in a asynchronous AssignmentCountEnvelope thread
        // false to subtract 1 from the count for the bucket
        boolean countUp = false;
        assignmentsCountExecutor.execute(new AssignmentCountEnvelope(this, experimentRepository,
                dbRepository, experiment, currentAssignment, countUp, eventLog, null, assignUserToExport,
                assignBucketCount));

        removeIndexUserToBucket(userID, experiment.getID(), context, currentAssignment.getBucketLabel());
        removeIndexExperimentsToUser(userID, experiment.getID(), context, appName);
    }

    /**
     * Deletes the existing Assignment between a User and an Experiment.
     *
     * @param experimentID experiment id
     * @param userID       user id
     * @param context      context
     * @param appName      application name
     */

    void deleteUserFromLookUp(Experiment.ID experimentID, User.ID userID, Context context, Application.Name appName) {
        try{
            experimentUserIndexAccessor.deleteBy(
                    userID.toString(),
                    experimentID.getRawID(),
                    context.getContext(),
                    appName.toString()
            );
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not delete user assignment for Experiment:" + experimentID +
                    " and User " + userID, e);
        }
    }

    @Override
    public void removeIndexUserToBucket(User.ID userID, Experiment.ID experimentID, Context context, Bucket.Label bucketLabel) {
        try {
            userBucketIndexAccessor.deleteBy(
                    experimentID.getRawID(),
                    userID.toString(),
                    context.getContext(),
                    bucketLabel.toString()
            );
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException(
                    "Could not remove from user_bucket_index for user: " + userID + " to experiment: " + experimentID, e);
        }
    }


    void removeIndexExperimentsToUser(User.ID userID, Experiment.ID experimentID, Context context,
                                      Application.Name appName) {
        try{
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

    List<Date> getUserAssignmentPartitions(Date fromTime, Date toTime){
        final LocalDateTime startTime = LocalDateTime.ofInstant(fromTime.toInstant(), ZoneId.systemDefault())
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        final LocalDateTime endTime =  LocalDateTime.ofInstant(toTime.toInstant(), ZoneId.systemDefault())
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        final long hours = Duration.between(startTime, endTime).toHours();
        return LongStream
                .rangeClosed(0, hours)
                .mapToObj(startTime::plusHours)
                .map(t-> Date.from(t.atZone(ZoneId.systemDefault()).toInstant()))
                .collect(Collectors.toList());
    }

    //TODO: this is super bad practice. should return a java stream and let the resources who is calling this method handling the stream
    @Override
    public StreamingOutput getAssignmentStream(Experiment.ID experimentID, Context context, Parameters parameters, Boolean ignoreNullBucket) {
        final List<Date> dateHours = getDateHourRangeList(experimentID, parameters);
        final String header = "experiment_id\tuser_id\tcontext\tbucket_label\tcreated\t"+ System.getProperty("line.separator");
        final DateFormat formatter = new SimpleDateFormat(defaultTimeFormat);
        formatter.setTimeZone(parameters.getTimeZone());
        final StringBuilder sb = new StringBuilder();
        return (os) -> {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(os, Charset.forName("UTF-8")))) {
                writer.write(header);
                for(Date dateHour : dateHours){
                    Result<UserAssignmentExport> result;
                    logger.debug("Query user assignment export for experimentID={}, at dateHour={}", experimentID.getRawID(), dateHour);
                    if(ignoreNullBucket) {
                        result = userAssignmentExportAccessor.selectBy(experimentID.getRawID(), dateHour, context.getContext(), false);
                    }else{
                        result = userAssignmentExportAccessor.selectBy(experimentID.getRawID(), dateHour, context.getContext());
                    }

                    for(UserAssignmentExport userAssignmentExport : result){
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
            }catch(ReadTimeoutException | UnavailableException | NoHostAvailableException e){
                throw new RepositoryException("Could not retrieve assignment for " +
                        "experimentID = \"" + experimentID, e);
            }catch(IllegalArgumentException | IOException e) {
                throw new RepositoryException("Could not write assignment to stream for " +
                        "experimentID = \"" + experimentID, e);
            }
        };
    }

    List<Date> getDateHourRangeList(Experiment.ID experimentID, Parameters parameters) {
        final Experiment id = experimentRepository.getExperiment(experimentID);
        if (Objects.isNull(id)) {
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
    public void pushAssignmentToStaging(String exception, String data) {
        try{
            stagingAccessor.insertBy(exception, data);
        } catch (WriteTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not push the assignment to staging", e);
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
        try{
             result = bucketAssignmentCountAccessor.selectBy(experiment.getID().getRawID());
        } catch (ReadTimeoutException | UnavailableException | NoHostAvailableException e) {
            throw new RepositoryException("Could not fetch the bucket assignment counts for experiment " + experiment.getID(), e);
        }
        List<BucketAssignmentCount> bucketAssignmentCountList = new ArrayList<>();
        AssignmentCounts.Builder assignmentCountsBuilder = new AssignmentCounts.Builder()
                .withBucketAssignmentCount(bucketAssignmentCountList)
                .withExperimentID(experiment.getID());

        if(Objects.isNull(result)) {
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
            for(com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount bucketAssignmentCount : result){
                final Bucket.Label label = Objects.isNull(bucketAssignmentCount.getBucketLabel()) ?
                        NULL_LABEL : Bucket.Label.valueOf(bucketAssignmentCount.getBucketLabel());
                totalAssignments += bucketAssignmentCount.getCount();
                final BucketAssignmentCount.Builder builder = new BucketAssignmentCount.Builder()
                        .withCount(bucketAssignmentCount.getCount());
                if(NULL_LABEL.equals(label)){
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
}