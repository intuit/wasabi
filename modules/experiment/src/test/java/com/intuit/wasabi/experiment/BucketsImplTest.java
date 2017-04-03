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
package com.intuit.wasabi.experiment;

import com.intuit.wasabi.assignmentobjects.RuleCache;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.exceptions.BucketNotFoundException;
import com.intuit.wasabi.exceptions.ConstraintViolationException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.impl.BucketsImpl;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.RepositoryException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BucketsImplTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private ExperimentRepository databaseRepository;
    @Mock
    private ExperimentRepository cassandraRepository;
    @Mock
    private MutexRepository mutexRepository;
    @Mock
    private ExperimentValidator validator;
    @Mock
    private RuleCache ruleCache;
    @Mock
    private Experiments experiments;
    @Mock
    private Buckets buckets;
    @Mock
    private EventLog eventLog;

    private final static Application.Name testApp = Application.Name.valueOf("testApp");
    private Experiment.ID experimentID;
    private Bucket.Label bucketLabel;
    private UserInfo changeUser = UserInfo.from(UserInfo.Username.valueOf("userinfo")).build();

    @Before
    public void setup() {
        experimentID = Experiment.ID.newInstance();
        bucketLabel = Bucket.Label.valueOf("aLabel");
    }

    @Test
    public void testCreateBucket() throws Exception {

        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository,
                experiments, buckets, validator, eventLog) {
            @Override
            public Bucket getBucket(Experiment.ID experimentID, Bucket.Label bucketLabel) {
                return Bucket.newInstance(experimentID, bucketLabel).withAllocationPercent(.3).build();
            }
        };

        final Bucket newBucket = Bucket.newInstance(experimentID, bucketLabel).withAllocationPercent(.3).build();
        Bucket bucket = Bucket.newInstance(experimentID, bucketLabel).withAllocationPercent(.3).build();

        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(Experiment.State.DELETED)
                .build();

        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        verifyException(bucketsImpl, ExperimentNotFoundException.class)
                .createBucket(experiment.getID(), newBucket, changeUser);

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        verifyException(bucketsImpl, InvalidExperimentStateException.class)
                .createBucket(experimentID, newBucket, changeUser);

        experiment.setState(Experiment.State.DRAFT);
        when(cassandraRepository.getBucket(newBucket.getExperimentID(), newBucket.getLabel())).thenReturn(bucket);
        verifyException(bucketsImpl, ConstraintViolationException.class)
                .createBucket(experimentID, newBucket, changeUser);

        when(cassandraRepository.getBucket(newBucket.getExperimentID(), newBucket.getLabel())).thenReturn(null);

        Bucket result = bucketsImpl.createBucket(experimentID, newBucket, changeUser);
        assert result.getLabel() == newBucket.getLabel();
        assert result.getExperimentID() == experimentID;

        experiment.setState(Experiment.State.RUNNING);
        result = bucketsImpl.createBucket(experimentID, newBucket, changeUser);
        assert result.getLabel() == newBucket.getLabel();
        assert result.getExperimentID() == experimentID;

        doThrow(RepositoryException.class).when(databaseRepository).createBucket(newBucket);
        verifyException(bucketsImpl, RepositoryException.class).createBucket(experimentID, newBucket, changeUser);

    }

    @Test
    public void testAdjustAllocationPercentages() {

        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository, experiments, buckets,
                validator, eventLog);

        Bucket newBucket = Bucket.newInstance(experimentID, bucketLabel).withAllocationPercent(.3).build();
        Bucket bucket = Bucket.newInstance(experimentID, Bucket.Label.valueOf("a")).withAllocationPercent(.4).build();
        Bucket bucket2 = Bucket.newInstance(experimentID, Bucket.Label.valueOf("b")).withAllocationPercent(.6).build();
        BucketList bucketList = new BucketList();
        bucketList.addBucket(bucket);
        bucketList.addBucket(bucket2);
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(Experiment.State.DRAFT)
                .build();

        when(buckets.getBuckets(experimentID, false)).thenReturn(bucketList);

        BucketList newBuckets = bucketsImpl.adjustAllocationPercentages(experiment, newBucket);
        bucket.setAllocationPercent(.42);
        bucket2.setAllocationPercent(.28);
        bucketList = new BucketList();
        bucketList.addBucket(bucket);
        bucketList.addBucket(bucket2);
        for (Bucket b1 : bucketList.getBuckets()) {
            for (Bucket b2 : bucketList.getBuckets()) {
                if (b1.getLabel().equals(b2.getLabel())) {
                    assertTrue(b1.equals(b2));
                }
            }
        }
        assertTrue(bucketList.getBuckets().size() == newBuckets.getBuckets().size());

    }

    @Test
    public void testValidateBucketChanges() throws Exception {

        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository,
                experiments, buckets, validator, eventLog);

        Bucket bucket = Bucket.newInstance(experimentID, Bucket.Label.valueOf("a")).withAllocationPercent(.3)
                .withState(Bucket.State.valueOf("OPEN")).build();
        Bucket bucket2 = Bucket.newInstance(Experiment.ID.newInstance(), Bucket.Label.valueOf("a"))
                .withAllocationPercent(.3).withState(Bucket.State.valueOf("CLOSED")).build();

        try {
            bucketsImpl.validateBucketChanges(bucket, bucket2);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        bucket2.setExperimentID(experimentID);
        bucket2.setLabel(Bucket.Label.valueOf("b"));
        try {
            bucketsImpl.validateBucketChanges(bucket, bucket2);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        bucket2.setExperimentID(experimentID);
        bucket2.setLabel(Bucket.Label.valueOf("a"));
        try {
            bucketsImpl.validateBucketChanges(bucket, bucket2);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        bucket2.setState(Bucket.State.valueOf("OPEN"));
        bucketsImpl.validateBucketChanges(bucket, bucket2);
    }

    @Test
    public void testGetBucketChangeList() throws Exception {

        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository,
                experiments, buckets, validator, eventLog);

        Bucket bucket = Bucket.newInstance(experimentID, bucketLabel)
                .withControl(true).withAllocationPercent(.5).withDescription("one").withPayload("pay1").build();
        Bucket bucket2 = Bucket.newInstance(Experiment.ID.newInstance(), bucketLabel)
                .withControl(false).withAllocationPercent(.6).withDescription("two").withPayload("pay2").build();
        Bucket.Builder builder = Bucket.newInstance(experimentID, bucketLabel);

        ArrayList<Bucket.BucketAuditInfo> changes = new ArrayList<>();
        Bucket.BucketAuditInfo changeData;
        changeData = new Bucket.BucketAuditInfo("is_control", bucket.isControl().toString(),
                bucket2.isControl().toString());
        changes.add(changeData);

        changeData = new Bucket.BucketAuditInfo("allocation", bucket.getAllocationPercent().toString(),
                bucket2.getAllocationPercent().toString());
        changes.add(changeData);

        changeData = new Bucket.BucketAuditInfo("description", bucket.getDescription(), bucket2.getDescription());
        changes.add(changeData);

        changeData = new Bucket.BucketAuditInfo("payload", bucket.getPayload(), bucket2.getPayload());
        changes.add(changeData);

        List<Bucket.BucketAuditInfo> returned = bucketsImpl.getBucketChangeList(bucket, bucket2, builder);
        assert returned.equals(changes);
    }

    @Test
    public void testUpdateBucket() throws Exception {
        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository,
                experiments, buckets, validator, eventLog);
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(Experiment.State.DRAFT)
                .build();
        Bucket bucket = Bucket.newInstance(experimentID, bucketLabel)
                .withControl(true).withAllocationPercent(.5).withDescription("one").withState(Bucket.State.OPEN).build();
        Bucket updates = Bucket.newInstance(experimentID, bucketLabel)
                .withControl(true).withAllocationPercent(.5).withDescription("one").build();

        when(experiments.getExperiment(experimentID)).thenReturn(null);
        verifyException(bucketsImpl, ExperimentNotFoundException.class)
                .updateBucket(experiment.getID(), bucketLabel, updates, changeUser);

        when(experiments.getExperiment(experimentID)).thenReturn(experiment);
        when(cassandraRepository.getBucket(experimentID, bucketLabel)).thenReturn(null);
        verifyException(bucketsImpl, BucketNotFoundException.class)
                .updateBucket(experiment.getID(), bucketLabel, updates, changeUser);

        updates.setAllocationPercent(0.7);
        List<Bucket.BucketAuditInfo> changeList = new ArrayList<>();
        Bucket.BucketAuditInfo changeData = new Bucket.BucketAuditInfo("allocation", bucket.getAllocationPercent().toString(),
                updates.getAllocationPercent().toString());
        changeList.add(changeData);

        when(cassandraRepository.getBucket(experimentID, bucketLabel)).thenReturn(bucket);

        // The method instantiates an object and sends it as an argument so we have to use matchers
        when(buckets.getBucketChangeList(Matchers.<Bucket>any(), Matchers.<Bucket>any(), Matchers.<Bucket.Builder>any())).thenReturn(changeList);
        when(cassandraRepository.updateBucket(bucket)).thenReturn(updates);

        Bucket result = bucketsImpl.updateBucket(experimentID, bucketLabel, updates, changeUser);
        assert result.getLabel().equals(updates.getLabel());
    }

    @Test
    public void testUpdateBucketBatch() throws Exception {

        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository,
                experiments, buckets, validator, eventLog);

        Bucket bucket = Bucket.newInstance(experimentID, bucketLabel)
                .withControl(true).withAllocationPercent(.5)
                .withDescription("one").build();
        bucket.setLabel(Bucket.Label.valueOf("a"));

        BucketList bucketList = new BucketList();
        bucketList.addBucket(bucket);

        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(Experiment.State.DRAFT)
                .build();

        when(experiments.getExperiment(experimentID)).thenReturn(null);
        try {
            bucketsImpl.updateBucketBatch(experimentID, bucketList, changeUser);
            fail();
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experimentID)).thenReturn(experiment);
        try {
            bucketsImpl.updateBucketBatch(experimentID, bucketList, changeUser);
            fail();
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void testCombineOldAndNewBuckets() throws Exception {

        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository,
                experiments, buckets, validator, eventLog);

        Bucket bucket = Bucket.newInstance(experimentID, Bucket.Label.valueOf("a"))
                .withControl(true).withAllocationPercent(.5).withDescription("one").build();
        Bucket bucket2 = Bucket.newInstance(experimentID, Bucket.Label.valueOf("b"))
                .withControl(false).withAllocationPercent(.5).withDescription("two").build();

        Bucket bucketNew = Bucket.newInstance(experimentID, Bucket.Label.valueOf("b"))
                .withControl(false).withAllocationPercent(.5).withDescription("three").build();

        BucketList oldBucketList = new BucketList();
        oldBucketList.addBucket(bucket);
        oldBucketList.addBucket(bucket2);

        BucketList newBucketList = new BucketList();
        newBucketList.addBucket(bucketNew);

        BucketList expected = new BucketList();
        expected.addBucket(bucket);
        expected.addBucket(bucketNew);

        BucketList returned = bucketsImpl.combineOldAndNewBuckets(oldBucketList, newBucketList);
        assert returned.equals(expected);
    }

    @Test
    public void testDeleteBucket() {

        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository,
                experiments, buckets, validator, eventLog);

        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(Experiment.State.DRAFT)
                .build();


        Bucket bucket = Bucket.newInstance(experimentID, Bucket.Label.valueOf("awesomeBucket"))
                .withControl(true).withAllocationPercent(1.0).withDescription("one").build();
        //verify that an not available experiment is not processed
        verifyException(bucketsImpl, ExperimentNotFoundException.class)
                .deleteBucket(experimentID, bucket.getLabel(), changeUser);

        when(experiments.getExperiment(experimentID)).thenReturn(experiment);
        when(cassandraRepository.getBucket(experimentID, bucket.getLabel())).thenReturn(bucket);

        bucketsImpl.deleteBucket(experimentID, bucket.getLabel(), changeUser);

        //verify that Bucket gets deleted in both repositories
        verify(cassandraRepository, times(1)).deleteBucket(experimentID, bucket.getLabel());
        verify(databaseRepository, times(1)).deleteBucket(experimentID, bucket.getLabel());
        verify(cassandraRepository, times(0)).createBucket(bucket);

        doThrow(new RepositoryException()).when(databaseRepository).deleteBucket(experimentID, bucket.getLabel());
        try {
            bucketsImpl.deleteBucket(experimentID, bucket.getLabel(), changeUser);
            fail("Delete Bucket should throw RepositoryException!"); //fail in case no exception is thrown!
        } catch (RepositoryException e) {
            //this exception is expected!
        }
        //verify that the bucket gets recreated
        verify(cassandraRepository, times(1)).createBucket(bucket);
    }

    @Test
    public void testGetBucketBuilder() {
        BucketsImpl bucketsImpl = new BucketsImpl(databaseRepository, cassandraRepository,
                experiments, buckets, validator, eventLog);
        Experiment experiment = Experiment.withID(experimentID)
                .withApplicationName(testApp)
                .withState(Experiment.State.DRAFT)
                .build();
        verifyException(bucketsImpl, BucketNotFoundException.class).getBucketBuilder(experiment.getID(), bucketLabel);

    }
}
