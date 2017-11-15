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
package com.intuit.wasabi.experiment.impl;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.BucketChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketCreateEvent;
import com.intuit.wasabi.eventlog.events.BucketDeleteEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.exceptions.BucketNotFoundException;
import com.intuit.wasabi.exceptions.ConstraintViolationException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.Buckets;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.DatabaseRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT;
import static org.slf4j.LoggerFactory.getLogger;

public class BucketsImpl implements Buckets {

    private static final Logger LOGGER = getLogger(ExperimentsImpl.class);
    private final ExperimentRepository databaseRepository;
    private final ExperimentRepository cassandraRepository;
    private final Experiments experiments;
    private final Buckets buckets;
    private final EventLog eventLog;
    private final ExperimentValidator validator;

    @Inject
    public BucketsImpl(@DatabaseRepository final ExperimentRepository databaseRepository,
                       @CassandraRepository final ExperimentRepository cassandraRepository,
                       final Experiments experiments, final Buckets buckets, final ExperimentValidator validator,
                       final EventLog eventLog) {
        super();

        this.validator = validator;
        this.databaseRepository = databaseRepository;
        this.cassandraRepository = cassandraRepository;
        this.experiments = experiments;
        this.buckets = buckets;
        this.eventLog = eventLog;
    }

    private static Double roundToTwo(Double x) {
        return Math.round(x * 10000) / 10000.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList getBuckets(Experiment.ID experimentID, boolean checkExperiment) {
        return cassandraRepository.getBuckets(experimentID, checkExperiment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bucket getBucket(Experiment.ID experimentID, Bucket.Label bucketLabel) {
        return cassandraRepository.getBucket(experimentID, bucketLabel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bucket createBucket(Experiment.ID experimentID, Bucket newBucket, UserInfo user) {

        Experiment experiment = experiments.getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        validateExperimentState(experiment);
        checkBucketConstraint(experiment, newBucket);

        LOGGER.debug("Add Bucket: adding new bucket to running experiment" + experiment.getID());
        cassandraRepository.createBucket(newBucket);
        try {
            databaseRepository.createBucket(newBucket);
        } catch (RepositoryException e) {
            cassandraRepository.deleteBucket(newBucket.getExperimentID(), newBucket.getLabel());
            throw e;
        }

        //if we just created an experiment in a running experiment, update the remaining allocation percentages
        if (!Experiment.State.DRAFT.equals(experiment.getState())) {
            eventLog.postEvent(new BucketCreateEvent(user, experiment, newBucket));
            BucketList updates = buckets.adjustAllocationPercentages(experiment, newBucket);
            buckets.updateBucketAllocBatch(experimentID, updates);
        }
        return getBucket(experimentID, newBucket.getLabel());
    }

    private void validateExperimentState(Experiment experiment) {
        Experiment.State state = experiment.getState();

        if (!(state.equals(Experiment.State.PAUSED)
                || state.equals(Experiment.State.RUNNING)
                || state.equals(Experiment.State.DRAFT))) {

            Set<Experiment.State> desiredStates = new HashSet<>();
            desiredStates.add(Experiment.State.DRAFT);
            desiredStates.add(Experiment.State.PAUSED);
            desiredStates.add(Experiment.State.RUNNING);
            throw new InvalidExperimentStateException(experiment.getID(),
                    desiredStates, experiment.getState());
        }
    }

    private void checkBucketConstraint(Experiment experiment, Bucket newBucket) {
        Bucket test = cassandraRepository.getBucket(newBucket.getExperimentID(), newBucket.getLabel());
        if (test != null) {
            throw new ConstraintViolationException(ConstraintViolationException.Reason.UNIQUE_CONSTRAINT_VIOLATION,
                    "Bucket label must be unique within an experiment", null);
        }

        if (!experiment.getState().equals(Experiment.State.DRAFT) &&
                newBucket.isControl() != null && newBucket.isControl()) {
            throw new ConstraintViolationException(ConstraintViolationException.Reason.APPLICATION_CONSTRAINT_VIOLATION,
                    "Bucket added to a running experiment may not be a control bucket", null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList updateBucketAllocBatch(Experiment.ID experimentID, BucketList bucketList) {

        LOGGER.debug("Add Bucket: saving new allocation percentages for experiment" + experimentID);
        // Update both repositories
        cassandraRepository.updateBucketBatch(experimentID, bucketList);
        databaseRepository.updateBucketBatch(experimentID, bucketList);

        return buckets.getBuckets(experimentID, false /* don't check experiment again */);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList adjustAllocationPercentages(Experiment experiment, Bucket newBucket) {

        double remainingAlloc = 1. - newBucket.getAllocationPercent();
        BucketList bucketList = buckets.getBuckets(experiment.getID(), false /* don't check experiment again */);
        BucketList newBuckets = new BucketList();
        for (Bucket bucket : bucketList.getBuckets()) {
            if (bucket.getLabel().equals(newBucket.getLabel())) {
                continue;
            }
            double newAlloc = roundToTwo(remainingAlloc * bucket.getAllocationPercent());
            LOGGER.debug("Add Bucket: setting allocation percentage for bucket " + bucket.getLabel() +
                    " in experiment " + experiment.getID() + " to: " + newAlloc);
            Bucket.Builder builder = Bucket.from(bucket).withAllocationPercent(newAlloc);
            Bucket updatedBucket = builder.build();
            newBuckets.addBucket(updatedBucket);
            if (!Experiment.State.DRAFT.equals(experiment.getState())
                    && Double.compare(bucket.getAllocationPercent(), updatedBucket.getAllocationPercent()) != 0) {
                // this is a system event, so no user needed
                eventLog.postEvent(new BucketChangeEvent(experiment, updatedBucket, "allocation",
                        String.valueOf(bucket.getAllocationPercent()),
                        String.valueOf(updatedBucket.getAllocationPercent())));
            }
        }
        return newBuckets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bucket updateBucket(final Experiment.ID experimentID, final Bucket.Label bucketLabel,
                               final Bucket updates, UserInfo user) {

        Experiment experiment = experiments.getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        Bucket bucket = getBucket(experimentID, bucketLabel);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketLabel);
        }
        // Save the state of the bucket; used for reverting the cassandra changes
        Bucket oldBucket = bucket;

        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=UPDATING_BUCKET, applicationName={}, experimentName={}, configuration=[{}]",
                experiment.getApplicationName(), experiment.getLabel(), oldBucket);

        buckets.validateBucketChanges(bucket, updates);

        Bucket.Builder builder = getBucketBuilder(experimentID, bucketLabel);

        // check for new metadata to be updated
        List<Bucket.BucketAuditInfo> changeList = buckets.getBucketChangeList(bucket, updates, builder);

        if (!changeList.isEmpty()) {

            bucket = builder.build();

            // Update both repositories
            Bucket updatedBucket = cassandraRepository.updateBucket(bucket);
            try {
                databaseRepository.updateBucket(bucket);
            } catch (Exception e) {
                cassandraRepository.updateBucket(oldBucket);
                throw e;
            }
            bucket = updatedBucket;

            // Update the bucket audit log
            if (!Experiment.State.DRAFT.equals(experiment.getState())) { // Do not audit the changes that are performed in the experiment's DRAFT state
                cassandraRepository.logBucketChanges(experimentID, bucketLabel, changeList);

                for (Bucket.BucketAuditInfo bucketAuditInfo : changeList) {
                    eventLog.postEvent(new BucketChangeEvent(user, experiment, bucket,
                            bucketAuditInfo.getAttributeName(), bucketAuditInfo.getOldValue(),
                            bucketAuditInfo.getNewValue()));
                }
            }
        }

        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=BUCKET_UPDATED, applicationName={}, experimentName={}, configuration=[{}]",
                experiment.getApplicationName(), experiment.getLabel(), bucket);

        return bucket;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bucket updateBucketState(Experiment.ID experimentID, Bucket.Label bucketLabel,
                                    Bucket.State desiredState, UserInfo user) {

        Experiment experiment = experiments.getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        Bucket bucket = getBucket(experimentID, bucketLabel);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketLabel);
        }

        // Changes to state is not allowed in experiment's DRAFT state
        if (experiment.getState().equals(DRAFT)) {
            throw new InvalidExperimentStateException("Changes to a bucket's state is not allowed " +
                    "when the experiment is in DRAFT state");
        }

        validator.validateBucketStateTransition(bucket.getState(), desiredState);

        //if we are actually going to update a bucket
        if (!desiredState.equals(bucket.getState()) &&
                (desiredState == Bucket.State.CLOSED ||
                        desiredState == Bucket.State.EMPTY)) {

            BucketList bucketList = getBuckets(experimentID, false /* don't check experiment again */);
            //get starting allocation pct of bucket to be closed
            Double bucketB = roundToTwo(bucket.getAllocationPercent());

            //iterate over all buckets, resizing them
            Double newPct;
            Bucket.State newState;
            List<List<Bucket.BucketAuditInfo>> allChanges = new ArrayList<>();

            BucketList newBuckets = new BucketList();

            Integer nOpen = 0;
            Bucket lastBucket = null;
            for (Bucket buck : bucketList.getBuckets()) {
                if (!buck.getLabel().equals(bucket.getLabel()) &&
                        buck.getState() == Bucket.State.OPEN) {
                    nOpen++;
                    lastBucket = buck;
                }
            }

            Double totalAlloc = 0.;
            for (int i = 0; i < bucketList.getBuckets().size(); i++) {

                Bucket buck = bucketList.getBuckets().get(i);
                List<Bucket.BucketAuditInfo> changeList = new ArrayList<>();

                //check if this is the bucket to be closed
                if (buck.getLabel().equals(bucket.getLabel())) {
                    newPct = 0.;
                    newState = desiredState;

                    // Update the bucket audit log
                    Bucket.BucketAuditInfo changeData = new Bucket.BucketAuditInfo("state", buck.getState().toString(),
                            desiredState.toString());

                    //log the change
                    changeList.add(changeData);

                } else {
                    //any other bucket, get new allocation pct
                    if (buck.getState() == Bucket.State.OPEN) {
                        if (bucketB < 1.) {
                            Double A = buck.getAllocationPercent();
                            newPct = A + (A * bucketB / ((Double) 1. - bucketB));
                        } else {
                            newPct = bucketB / nOpen;
                        }
                    } else {
                        newPct = buck.getAllocationPercent();
                    }

                    if (buck.equals(lastBucket)) {
                        newPct = roundToTwo(1. - totalAlloc);
                    } else {
                        newPct = roundToTwo(newPct);
                        totalAlloc += newPct;
                    }

                    newState = buck.getState();
                }

                //log the change
                Bucket.BucketAuditInfo changeData = new Bucket.BucketAuditInfo("allocation",
                        buck.getAllocationPercent().toString(), newPct.toString());
                changeList.add(changeData);
                allChanges.add(changeList);

                //keep track of the new buckets for validation/writing to DB
                Bucket.Builder builder = getBucketBuilder(experimentID, buck.getLabel());
                builder.withAllocationPercent(newPct);
                builder.withState(newState);
                builder.withControl(buck.isControl());
                builder.withDescription(buck.getDescription());
                newBuckets.addBucket(builder.build());
            }

            //check that the new buckets are valid
            validator.validateExperimentBuckets(newBuckets.getBuckets());

            //do the cassandra update
            BucketList updates = cassandraRepository.updateBucketBatch(experimentID, newBuckets);
            try {
                databaseRepository.updateBucketBatch(experimentID, newBuckets);
            } catch (WasabiException ex) {
                //if there was an exception, undo the cassandra writes
                cassandraRepository.updateBucketBatch(experimentID, bucketList);
                throw ex;
            }

            //log bucket changes
            for (int i = 0; i < bucketList.getBuckets().size(); i++) {
                cassandraRepository.logBucketChanges(experimentID, bucketList.getBuckets().get(i).getLabel(),
                        allChanges.get(i));
                for (Bucket.BucketAuditInfo bucketAuditInfo : allChanges.get(i)) {
                    if (!Experiment.State.DRAFT.equals(experiment.getState())
                            && !(Objects.equals(bucketAuditInfo.getOldValue(), bucketAuditInfo.getNewValue()))) {
                        EventLogEvent event = new BucketChangeEvent(user, experiment, bucketList.getBuckets().get(i),
                                bucketAuditInfo.getAttributeName(), bucketAuditInfo.getOldValue(),
                                bucketAuditInfo.getNewValue());
                        eventLog.postEvent(event);
                    }
                }

            }

            //read back the buckets
            for (Bucket buck : updates.getBuckets()) {
                if (buck.getLabel().equals(bucket.getLabel())) {
                    return buck;
                }
            }
        }

        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=BUCKET_STATE_UPDATED, applicationName={}, experimentName={}, configuration=[oldState={}, newState={}]",
                experiment.getApplicationName(), experiment.getLabel(), bucket.getState(),desiredState);

        //return the updated closed bucket
        return bucket;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // Isolate the scope of this bucket retrieval
    public Bucket.Builder getBucketBuilder(Experiment.ID experimentID, Bucket.Label bucketLabel) {
        Bucket bucket = getBucket(experimentID, bucketLabel);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketLabel);
        }

        return Bucket.from(bucket);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBucket(final Experiment.ID experimentID, final Bucket.Label bucketLabel,
                             UserInfo user) {

        Experiment experiment = experiments.getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }
        validator.ensureStateIsDraft(experiment);

        // Save the bucket object before deleting it
        // To use it again to preserve data consistency
        Bucket bucket = cassandraRepository.getBucket(experimentID, bucketLabel);

        // Update both repositories
        cassandraRepository.deleteBucket(experimentID, bucketLabel);
        try {
            databaseRepository.deleteBucket(experimentID, bucketLabel);
            // do not log changes that are done in DRAFT
            if (!Experiment.State.DRAFT.equals(experiment.getState())) {
                eventLog.postEvent(new BucketDeleteEvent(user, experiment, bucket));
            }
        } catch (Exception e) {
            if (bucket != null) {
                Bucket newBucket = Bucket.newInstance(experimentID, bucketLabel)
                        .withAllocationPercent(bucket.getAllocationPercent())
                        .withControl(bucket.isControl())
                        .withDescription(bucket.getDescription())
                        .withPayload(bucket.getPayload())
                        .withState(bucket.getState())
                        .build();
                cassandraRepository.createBucket(newBucket);
            }
            throw e;
        }
        LOGGER.info("event=EXPERIMENT_METADATA_CHANGE, message=BUCKET_DELETED, applicationName={}, experimentName={}, configuration=[{}]",
                experiment.getApplicationName(), experiment.getLabel(), bucket);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateBucketChanges(Bucket bucket, Bucket updates) {

        if (updates.getExperimentID() != null && !updates.getExperimentID().equals(bucket.getExperimentID())) {
            throw new IllegalArgumentException("Invalid value for experimentID \"" + updates.getExperimentID() + " \"." +
                    " Cannot update ExperimentID");
        }
        if (updates.getLabel() != null && !updates.getLabel().equals(bucket.getLabel())) {
            throw new IllegalArgumentException("Invalid value for bucket label \"" + updates.getLabel() + " \". " +
                    "Cannot update bucket label");
        }
        if (updates.getState() != null && !updates.getState().equals(bucket.getState())) {
            throw new IllegalArgumentException("Cannot update the state of a bucket using this api");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bucket.BucketAuditInfo> getBucketChangeList(Bucket bucket, Bucket updates, Bucket.Builder builder) {

        // check for new metadata to be updated
        List<Bucket.BucketAuditInfo> changeList = new ArrayList<>();
        Bucket.BucketAuditInfo changeData;

        if (updates.isControl() != null && !updates.isControl().equals(bucket.isControl())) {
            builder.withControl(updates.isControl());
            changeData = new Bucket.BucketAuditInfo("is_control", bucket.isControl().toString(), updates.isControl().toString());
            changeList.add(changeData);
        }

        if (updates.getAllocationPercent() != null &&
                !updates.getAllocationPercent().equals(bucket.getAllocationPercent())) {
            builder.withAllocationPercent(updates.getAllocationPercent());
            changeData = new Bucket.BucketAuditInfo("allocation", bucket.getAllocationPercent().toString(),
                    updates.getAllocationPercent().toString());
            changeList.add(changeData);
        }

        if (updates.getDescription() != null && !updates.getDescription().equals(bucket.getDescription())) {
            builder.withDescription(updates.getDescription());
            changeData = new Bucket.BucketAuditInfo("description", bucket.getDescription(), updates.getDescription());
            changeList.add(changeData);
        }

        if (updates.getPayload() != null && !updates.getPayload().equals(bucket.getPayload())) {
            builder.withPayload(updates.getPayload());
            changeData = new Bucket.BucketAuditInfo("payload", bucket.getPayload(), updates.getPayload());
            changeList.add(changeData);
        }
        return changeList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList updateBucketBatch(Experiment.ID experimentID, BucketList bucketList, UserInfo user) {

        Experiment experiment = experiments.getExperiment(experimentID);
        if (experiment == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        BucketList changeBucketList = new BucketList();

        List<List<Bucket.BucketAuditInfo>> allChanges = new ArrayList<>();
        BucketList oldBuckets = buckets.getBuckets(experimentID, false /* don't check experiment again */);

        if (oldBuckets == null) {
            throw new IllegalStateException("No Buckets could be found for this experiment");
        }

        Bucket oldBucket;
        for (Bucket bucket : bucketList.getBuckets()) {

            if (bucket.getLabel() == null) {
                throw new IllegalArgumentException("error, bucket label was null");
            }

            oldBucket = null;
            for (Bucket b : oldBuckets.getBuckets()) {
                if (b.getLabel().equals(bucket.getLabel())) {
                    oldBucket = b;
                }
            }
            if (oldBucket == null) {
                throw new BucketNotFoundException(bucket.getLabel());
            }
            if (bucket.isControl() == null) {
                bucket.setControl(oldBucket.isControl());
            }
            buckets.validateBucketChanges(oldBucket, bucket);

            Bucket.Builder builder = buckets.getBucketBuilder(experimentID, bucket.getLabel());
            List<Bucket.BucketAuditInfo> changeList = buckets.getBucketChangeList(oldBucket, bucket, builder);

            if (!changeList.isEmpty()) {
                changeBucketList.addBucket(bucket);
                allChanges.add(changeList);
            }
        }

        BucketList allBuckets = new BucketList();
        if (!changeBucketList.getBuckets().isEmpty()) {
            allBuckets = buckets.combineOldAndNewBuckets(oldBuckets, changeBucketList);
            validator.validateExperimentBuckets(allBuckets.getBuckets());

            cassandraRepository.updateBucketBatch(experimentID, changeBucketList);
            for (int i = 0; i < allChanges.size(); i++) {
                cassandraRepository.logBucketChanges(experimentID, changeBucketList.getBuckets().get(i).getLabel(),
                        allChanges.get(i));
                // do not store changes that are done in DRAFT state
                if (!Experiment.State.DRAFT.equals(experiment.getState())) {
                    for (Bucket.BucketAuditInfo bucketAuditInfo : allChanges.get(i)) {
                        eventLog.postEvent(new BucketChangeEvent(user, experiment, changeBucketList.getBuckets().get(i),
                                bucketAuditInfo.getAttributeName(), bucketAuditInfo.getOldValue(),
                                bucketAuditInfo.getNewValue()));
                    }
                }
            }
        }
        return allBuckets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BucketList combineOldAndNewBuckets(BucketList oldBuckets, BucketList newBuckets) {

        BucketList allBuckets = new BucketList();
        for (Bucket b : oldBuckets.getBuckets()) {
            Boolean changed = false;
            for (Bucket bb : newBuckets.getBuckets()) {
                if (bb.getLabel().equals(b.getLabel())) {
                    allBuckets.addBucket(bb);
                    changed = true;
                    break;
                }
            }
            if (!changed) {
                allBuckets.addBucket(b);
            }
        }
        return allBuckets;
    }
}
