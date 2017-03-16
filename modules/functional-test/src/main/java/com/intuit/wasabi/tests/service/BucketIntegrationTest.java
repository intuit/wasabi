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
package com.intuit.wasabi.tests.service;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.intuit.wasabi.tests.library.util.Constants.BUCKET_STATE_CLOSED;
import static com.intuit.wasabi.tests.library.util.Constants.BUCKET_STATE_EMPTY;
import static com.intuit.wasabi.tests.library.util.Constants.BUCKET_STATE_OPEN;
import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_DELETED;
import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_RUNNING;
import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_TERMINATED;
import static com.intuit.wasabi.tests.library.util.Constants.NEW_ASSIGNMENT;
import static com.intuit.wasabi.tests.library.util.Constants.NO_OPEN_BUCKETS;
import static org.testng.Assert.assertEquals;

/**
 * Bucket integration tests
 */
public class BucketIntegrationTest extends TestBase {

    private Experiment experiment;
    private List<Bucket> buckets = new ArrayList<>();
    private String[] labels = {"blue", "red", "white"};
    private double[] allocations = {.40, .30, .30};
    private boolean[] control = {false, false, true};

    /**
     * Initializes a default experiment.
     */
    public BucketIntegrationTest() {
        setResponseLogLengthLimit(1000);

        experiment = ExperimentFactory.createExperiment();
        experiment.startTime = "2013-08-01T00:00:00+0000";
        experiment.endTime = "2013-08-08T00:00:00+0000";
        experiment.samplingPercent = 0.5;
        experiment.label = "experiment";
        experiment.applicationName = "qbo" + UUID.randomUUID();

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);

    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateThreeBuckets() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
        buckets = BucketFactory.createCompleteBuckets(experiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);

        Assert.assertEquals(buckets, resultBuckets);

        for (Bucket result : resultBuckets) {
            Bucket matching = null;
            for (Bucket cand : buckets) {
                if (cand.label.equals(result.label)) {
                    matching = cand;
                    break;
                }

            }
            assertEquals(result.label, matching.label);
            assertEquals(result.isControl, matching.isControl);
            assertEquals(result.allocationPercent, matching.allocationPercent);
            assertEquals(result.description, matching.description);
        }
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateThreeBucketsWithIsControlToggle() {
        Experiment experimentLocal = ExperimentFactory.createExperiment();
        experimentLocal = postExperiment(experimentLocal);

        String[] labels = {"blue", "red", "ping"};
        double[] allocations = {.40, .30, .30};
        boolean[] control = {false, true, false};
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(experimentLocal, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        for (Bucket bucket : resultBuckets)
            bucket.isControl = true;

        List<Bucket> resultAfterIsControl = putBuckets(resultBuckets);
        int isControl = 0;
        for (Bucket bucket : resultAfterIsControl)
            if (bucket.isControl)
                isControl++;

        assertEquals(isControl, 3);

        experimentLocal.state = EXPERIMENT_STATE_RUNNING;
        experimentLocal = putExperiment(experimentLocal);
    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_ChangeControl() {
        List<Bucket> resultBuckets = getBuckets(experiment);

        Assert.assertEquals(buckets, resultBuckets);

        for (Bucket result : resultBuckets) {
            Bucket matching = null;
            for (Bucket cand : buckets) {
                if (cand.label.equals(result.label)) {
                    matching = cand;
                    break;
                }

            }
            assertEquals(result.label, matching.label);
            assertEquals(result.isControl, matching.isControl);
            assertEquals(result.allocationPercent, matching.allocationPercent);
            assertEquals(result.description, matching.description);
            if (result.label.equals("white"))
                assertEquals(true, result.isControl);
            else
                assertEquals(false, result.isControl);
        }

        Bucket newIsControlBucket = resultBuckets.get(0);
        newIsControlBucket.isControl = true;
        Bucket resultOfPut = putBucket(newIsControlBucket);
        assertEquals(true, resultOfPut.isControl);

        List<Bucket> resultBucketsAfterControlChange = getBuckets(experiment);

        for (Bucket result : resultBucketsAfterControlChange) {
            Bucket matching = null;
            for (Bucket cand : buckets) {
                if (cand.label.equals(result.label)) {
                    matching = cand;
                    break;
                }

            }
            assertEquals(result.label, matching.label);
            assertEquals(result.allocationPercent, matching.allocationPercent);
            assertEquals(result.description, matching.description);
            if (result.label.equals(resultOfPut.label))
                assertEquals(true, result.isControl);
            else
                assertEquals(false, result.isControl);
        }
    }

    @Test(dependsOnMethods = {"t_ChangeControl"})
    public void t_ChangeDescription() {
        List<Bucket> resultBuckets = getBuckets(experiment);

        Bucket newUpdatedBucket = resultBuckets.get(0);
        newUpdatedBucket.description = "updated description";
        Bucket resultOfPut = putBucket(newUpdatedBucket);
        assertEquals("updated description", resultOfPut.description);

        List<Bucket> resultBucketsAfterChange = getBuckets(experiment);

        Bucket matching = null;
        for (Bucket result : resultBucketsAfterChange) {
            if (result.label.equals(resultOfPut.label)) {
                matching = newUpdatedBucket;
                break;
            }

        }
        assertEquals(resultOfPut.label, matching.label);
        assertEquals(resultOfPut.allocationPercent, matching.allocationPercent);
        assertEquals(resultOfPut.description, matching.description);
        assertEquals(resultOfPut.isControl, matching.isControl);
    }

    @Test(dependsOnMethods = {"t_ChangeDescription"})
    public void t_ChangePayload() {
        List<Bucket> resultBuckets = getBuckets(experiment);

        Bucket newUpdatedBucket = resultBuckets.get(0);
        newUpdatedBucket.payload = "updated payload";
        Bucket resultOfPut = putBucket(newUpdatedBucket);
        assertEquals("updated payload", resultOfPut.payload);

        List<Bucket> resultBucketsAfterChange = getBuckets(experiment);

        for (Bucket result : resultBucketsAfterChange) {
            Bucket matching = null;
            if (result.label.equals(resultOfPut.label)) {
                matching = newUpdatedBucket;
                break;
            }

        }
        assertEquals(resultOfPut.label, newUpdatedBucket.label);
        assertEquals(resultOfPut.allocationPercent, newUpdatedBucket.allocationPercent);
        assertEquals(resultOfPut.description, newUpdatedBucket.description);
        assertEquals(resultOfPut.isControl, newUpdatedBucket.isControl);
    }

    @Test(dependsOnMethods = {"t_ChangeControl"})
    public void t_CreateJunkBucketAndDelete() {
        Bucket junkBucket = BucketFactory.createBucket(experiment);
        junkBucket.setLabel("muwaahhahhhahhello").setAllocationPercent(.99).setControl(true).
                setDescription("junk").setPayload("junk payload");
        junkBucket.setState(BUCKET_STATE_OPEN);
        junkBucket.setPayload("testPayload");
        Bucket junkBucketResult = postBucket(junkBucket);

        List<Bucket> resultBucketsAfterJunk = getBuckets(experiment, HttpStatus.SC_OK);
        List<Bucket> originalAndJunk = new ArrayList<>(buckets);
        originalAndJunk.add(junkBucketResult);
        for (Bucket result : resultBucketsAfterJunk) {
            Bucket matching = null;
            for (Bucket cand : originalAndJunk) {
                if (cand.label.equals(result.label)) {
                    matching = cand;
                    break;
                }

            }

            assertEquals(result.label, matching.label);
            assertEquals(result.allocationPercent, matching.allocationPercent);
            assertEquals(result.payload, matching.payload);
            assertEquals(result.state, matching.state);
        }

        Bucket bucket = getBucket(junkBucket);
        assertEquals(bucket.allocationPercent, junkBucketResult.allocationPercent);
        assertEquals(bucket.label, junkBucketResult.label);
        assertEquals(bucket.payload, junkBucketResult.payload);
        assertEquals(bucket.state, junkBucketResult.state);

        Response resultOfDelete = deleteBucket(junkBucket);
        assertEquals(HttpStatus.SC_NO_CONTENT, resultOfDelete.getStatusCode());

        getBucket(junkBucket, HttpStatus.SC_NOT_FOUND);

    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_CreateDuplicateBucket() {
        Bucket junkBucket = BucketFactory.createBucket(experiment);
        junkBucket.setLabel("white").setAllocationPercent(.99).setControl(true).
                setDescription("junk").setPayload("junk payload");
        junkBucket.setState(BUCKET_STATE_OPEN);
        Bucket junkBucketResult = postBucket(junkBucket, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_CreateBadAllocationBucketLessThanZero() {
        Bucket junkBucket1 = BucketFactory.createBucket(experiment);
        junkBucket1.setLabel("orange1").setAllocationPercent(-.01).setControl(true).
                setDescription("junk").setPayload("junk payload");
        junkBucket1.setState(BUCKET_STATE_OPEN);
        Bucket junkBucketResult1 = postBucket(junkBucket1, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"t_CreateBadAllocationBucketLessThanZero"})
    public void t_CreateBadAllocationBucketGreaterThan1() {
        Bucket junkBucket2 = BucketFactory.createBucket(experiment);
        junkBucket2.setLabel("orange2").setAllocationPercent(1.01).setControl(true).
                setDescription("junk").setPayload("junk payload");
        junkBucket2.setState(BUCKET_STATE_OPEN);
        Bucket junkBucketResult = postBucket(junkBucket2, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_CreateBadBucketUsingJson() {
        String url = "/experiments/" + experiment.id + "/buckets";
        String data = "{}";
        response = apiServerConnector.doPost(url, data);
        System.out.println(response.asString());
        assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);

        data = "{\"allocationPercent\":\"abcd\"}";
        response = apiServerConnector.doPost(url, data);
        System.out.println(response.asString());
        assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);

        data = "{\"allocationPercent\":0.3}";
        response = apiServerConnector.doPost(url, data);
        System.out.println(response.asString());
        assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);

        data = "{\"allocationPercent\":0.3, \"label\": \"red\"}";
        response = apiServerConnector.doPost(url, data);
        System.out.println(response.asString());
        assertReturnCode(response, HttpStatus.SC_BAD_REQUEST);

    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_DeleteBadBucketUsingJson() {
        String url = "/experiments/" + "unknownexperiment" + "/buckets";
        response = apiServerConnector.doDelete(url);
        System.out.println(response.asString());
        assertReturnCode(response, HttpStatus.SC_INTERNAL_SERVER_ERROR);

        url = "/experiments/" + experiment.id + "/buckets/1234";
        response = apiServerConnector.doDelete(url);
        System.out.println(response.asString());
        assertReturnCode(response, HttpStatus.SC_INTERNAL_SERVER_ERROR);

        url = "/experiments/" + experiment.id + "/buckets/abcd";
        response = apiServerConnector.doDelete(url);
        System.out.println(response.asString());
        assertReturnCode(response, HttpStatus.SC_NOT_FOUND);

    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_UpdateBucketAfterExperimentRunning() {
        Bucket junkBucket1 = BucketFactory.createBucket(experiment);
        junkBucket1.setLabel("orange1").setAllocationPercent(-.01).setControl(true).
                setDescription("junk").setPayload("junk payload");
        junkBucket1.setState(BUCKET_STATE_OPEN);
        Bucket junkBucketResult1 = postBucket(junkBucket1, HttpStatus.SC_BAD_REQUEST);

        Bucket junkBucket2 = BucketFactory.createBucket(experiment);
        junkBucket2.setLabel("orange2").setAllocationPercent(1.01).setControl(true).
                setDescription("junk").setPayload("junk payload");
        junkBucket2.setState(BUCKET_STATE_OPEN);
        Bucket junkBucketResult = postBucket(junkBucket2, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_UpdateBucketsWithNoChange() {
        List<Bucket> bucketResults = putBuckets(buckets, HttpStatus.SC_OK);
        assertEquals(bucketResults.size(), 3);
        assertEquals(bucketResults, buckets);
    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_UpdateBadAllocationBucketLessThanZero() {
        Bucket junkBucket1 = BucketFactory.createBucket(experiment);
        junkBucket1.setLabel("blue").setAllocationPercent(-.01).setControl(true).
                setDescription("junk").setPayload("junk payload");
        junkBucket1.setState(BUCKET_STATE_OPEN);
        Bucket junkBucketResult1 = putBucket(junkBucket1, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_UpdateBadAllocationBucketGreateThan1() {
        Bucket junkBucket1 = BucketFactory.createBucket(experiment);
        junkBucket1.setLabel("blue").setAllocationPercent(1.01).setControl(true).
                setDescription("junk").setPayload("junk payload");
        junkBucket1.setState(BUCKET_STATE_OPEN);
        Bucket junkBucketResult1 = putBucket(junkBucket1, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_UpdateInvalidBucket() {
        Experiment exp = ExperimentFactory.createCompleteExperiment();
        postExperiment(exp);
        Bucket junkBucket = BucketFactory.createBucket(experiment);
        junkBucket.setLabel("0").setAllocationPercent(.5);
        Bucket junkBucketResult = postBucket(junkBucket, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"t_CreateThreeBuckets"})
    public void t_UpdateUnknownBucket() {
        Bucket junkBucket = BucketFactory.createBucket(experiment);
        junkBucket.setLabel("unknown" + UUID.randomUUID()).setAllocationPercent(-.01);
        Bucket junkBucketResult = postBucket(junkBucket, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnMethods = {"t_CreateJunkBucketAndDelete"})
    public void t_SetExperimentStateToRunning() {
        experiment.state = "RUNNING";
        experiment = putExperiment(experiment);
        assertEquals(EXPERIMENT_STATE_RUNNING, experiment.state);
    }

    @Test(dependsOnMethods = {"t_SetExperimentStateToRunning"})
    public void t_SetBucketStateToClosed() {
        List<Bucket> resultBuckets = getBuckets(experiment);

        Bucket newUpdatedBucket = resultBuckets.get(0);
        newUpdatedBucket.state = BUCKET_STATE_CLOSED;
        Bucket resultOfPut = putBucketState(newUpdatedBucket);
        assertEquals(BUCKET_STATE_CLOSED, resultOfPut.state);

        Bucket bucketAfterUpdate = getBucket(newUpdatedBucket);
        assertEquals(BUCKET_STATE_CLOSED, bucketAfterUpdate.state);
        assertEquals(newUpdatedBucket.description, bucketAfterUpdate.description);
        assertEquals(newUpdatedBucket.experimentID, bucketAfterUpdate.experimentID);
        assertEquals(newUpdatedBucket.experimentID, bucketAfterUpdate.experimentID);
        assertEquals(newUpdatedBucket.payload, bucketAfterUpdate.payload);
        assertEquals(newUpdatedBucket.isControl, bucketAfterUpdate.isControl);

        List<Bucket> resultBucketsAfterClosingOne = getBuckets(experiment);
        assertEquals(resultBucketsAfterClosingOne.size(), 3);

    }

    @Test(dependsOnMethods = {"t_SetBucketStateToClosed"})
    public void t_TerminateThreeBucketExperimentExperiment() {
        experiment.state = EXPERIMENT_STATE_TERMINATED;
        experiment = putExperiment(experiment);
        assertEquals(EXPERIMENT_STATE_TERMINATED, experiment.state);
        Experiment afterTermination = getExperiment(experiment);
        assertEquals(EXPERIMENT_STATE_TERMINATED, afterTermination.state);

    }

    @Test(dependsOnMethods = {"t_TerminateThreeBucketExperimentExperiment"})
    public void t_DeleteThreeBucketExperimentExperiment() {
        experiment.state = EXPERIMENT_STATE_DELETED;
        experiment = putExperiment(experiment, HttpStatus.SC_NO_CONTENT);
        assertEquals(experiment.state, null);
        assertEquals(experiment.id, null);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateExperimentAllocation1SuccessOnRunningExperiment() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        newExperiment = postExperiment(newExperiment);

        String[] labels = {"blue", "red"};
        double[] allocations = {.40, .30};
        boolean[] control = {false, true};
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(buckets.size(), resultBuckets.size());
        assertEquals(resultBuckets.size(), 2);

        Bucket bucket = BucketFactory.createBucket(newExperiment);
        bucket.allocationPercent = 0.3;
        bucket.label = "black";
        Bucket resultBucket = postBucket(bucket);
        assertEquals(resultBucket.allocationPercent, 0.3, .000000001);
        assertEquals(resultBucket.label, "black");
        resultBuckets = getBuckets(newExperiment);
        assertEquals(resultBuckets.size(), 3);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);
        deleteExperiment(newExperiment, HttpStatus.SC_BAD_REQUEST);
        newExperiment.state = EXPERIMENT_STATE_TERMINATED;
        putExperiment(newExperiment, HttpStatus.SC_OK);
        deleteExperiment(newExperiment, HttpStatus.SC_NO_CONTENT);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateExperimentWithAllAttributesOfBuckets() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        newExperiment = postExperiment(newExperiment);

        Bucket bucket = BucketFactory.createCompleteBucket(newExperiment);
        bucket.allocationPercent = 1.0;
        bucket.description = "d1";
        bucket.isControl = true;
        bucket.label = "l2";
        bucket.payload = "p1";
        bucket.state = BUCKET_STATE_OPEN;
        Bucket resultBucket = postBucket(bucket);
        Bucket bucketFromExpriment = getBucket(resultBucket);

        assertEquals(bucketFromExpriment.allocationPercent, 1.0, .0000001);
        assertEquals(bucketFromExpriment.description, "d1");
        assertEquals(bucketFromExpriment.state, BUCKET_STATE_OPEN);
        assertEquals(bucketFromExpriment.label, "l2");
        assertEquals(bucketFromExpriment.payload, "p1");
        assertEquals(bucketFromExpriment.isControl, true);

        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment);

        terminateAndDelete(newExperiment);
    }

    protected void terminateAndDelete(Experiment experiment) {
        experiment.state = EXPERIMENT_STATE_TERMINATED;
        putExperiment(experiment);
        experiment.state = EXPERIMENT_STATE_DELETED;
        putExperiment(experiment, HttpStatus.SC_NO_CONTENT);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateExperimentAllocationLessThan1FailureOnRunningExperiment() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        newExperiment = postExperiment(newExperiment);

        String[] labels = {"blue", "red"};
        double[] allocations = {.40, .30};
        boolean[] control = {false, true};
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(buckets.size(), resultBuckets.size());
        assertEquals(resultBuckets.size(), 2);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateExperimentAllocationGreaterThan1FailureOnRunningExperiment() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        newExperiment = postExperiment(newExperiment);

        String[] labels = {"blue", "red"};
        double[] allocations = {.40, .70};
        boolean[] control = {false, true};
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(buckets.size(), resultBuckets.size());
        assertEquals(resultBuckets.size(), 2);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_BAD_REQUEST);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateExperimentWithSingleBucketAndCloseState() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        newExperiment.samplingPercent = 1.0;
        newExperiment = postExperiment(newExperiment);

        String[] labels = {"blue"};
        double[] allocations = {1.0};
        boolean[] control = {true};
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(buckets.size(), resultBuckets.size());
        assertEquals(resultBuckets.size(), 1);

        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);

        User user = UserFactory.createUser("u1");
        Assignment assignment = getAssignment(newExperiment, user);

        List<Assignment> assignments = getAssignments(newExperiment);
        assertEquals(assignments.size(), 1);
        assertEquals(labels[0], assignments.get(0).bucket_label);
        Bucket bucket = buckets.get(0);
        bucket.state = BUCKET_STATE_CLOSED;
        putBucketState(bucket);

        List<Assignment> assignmentsAfterclose = getAssignments(newExperiment);
        assertEquals(assignmentsAfterclose.size(), 1);
        assertEquals(labels[0], assignmentsAfterclose.get(0).bucket_label);

        User user2 = UserFactory.createUser("u2");
        Assignment assignment2 = getAssignment(newExperiment, user2);
        assertEquals(assignment2.status, NO_OPEN_BUCKETS);
        assertEquals(assignment2.assignment, null);

        terminateAndDelete(newExperiment);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateExperimentWithSingleBucketAndEmptyState() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        String[] labels = {"blue"};
        double[] allocations = {1.0};
        boolean[] control = {true};
        newExperiment.samplingPercent = 1.0;
        newExperiment = postExperiment(newExperiment);
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(buckets.size(), resultBuckets.size());
        assertEquals(resultBuckets.size(), 1);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);
        User user = UserFactory.createUser("u1");
        Assignment assignment = getAssignment(newExperiment, user);

        List<Assignment> assignments = getAssignments(newExperiment);
        assertEquals(assignments.size(), 1);
        assertEquals(labels[0], assignments.get(0).bucket_label);
        Bucket bucket = buckets.get(0);
        bucket.state = BUCKET_STATE_EMPTY;
        putBucketState(bucket);

        List<Assignment> assignmentsAfrerclose = getAssignments(newExperiment);
        assertEquals(assignmentsAfrerclose.size(), 1);
        assertEquals(labels[0], assignmentsAfrerclose.get(0).bucket_label);

        User user2 = UserFactory.createUser("u2");
        Assignment assignment2 = getAssignment(newExperiment, user2);
        assertEquals(assignment2.status, NO_OPEN_BUCKETS);
        assertEquals(assignment2.assignment, null);

        terminateAndDelete(newExperiment);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_UpdateBucketDescriptionOnRunningExperimentAllowed() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        String[] labels = {"blue", "red", "black"};
        double[] allocations = {.40, .30, 0.30};
        boolean[] control = {false, true, false};
        newExperiment = postExperiment(newExperiment);
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(resultBuckets.size(), 3);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);

        Bucket changingBucket = buckets.get(0);
        changingBucket.description = "new description";
        changingBucket = putBucket(changingBucket);
        assertEquals(changingBucket.description, "new description");

        terminateAndDelete(newExperiment);

    }

    @Test(dependsOnGroups = {"ping"})
    public void t_UpdateMultipleBucketsUsingJson() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        String[] labels = {"blue", "red", "black"};
        double[] allocations = {.40, .30, 0.30};
        boolean[] control = {false, true, false};
        newExperiment = postExperiment(newExperiment);
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(resultBuckets.size(), 3);

        String url = "/experiments/" + newExperiment.id + "/buckets";
        String data = "{\"label\":\"" + labels[0] + "\", \"allocationPercent\": "
                + allocations[0] + ", \"isControl\": " + control[0] + "}";
        data += ",";
        data += "{\"label\":\"" + labels[2] + "\", \"allocationPercent\": "
                + allocations[2] + ", \"isControl\": " + control[2] + "}";

        data += ",";
        data += "{\"label\":\"" + labels[1] + "\", \"allocationPercent\": "
                + allocations[1] + ", \"isControl\": " + control[1] + "}";

        data = "{\"buckets\":[" + data + "]}";
        response = apiServerConnector.doPut(url, data);
        assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
        System.out.println(response.asString());
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);

        terminateAndDelete(newExperiment);

    }

    @Test(dependsOnGroups = {"ping"})
    public void t_GetAssignmentsForUser() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        String[] labels = {"blue", "red", "black"};
        double[] allocations = {.40, .30, 0.30};
        boolean[] control = {false, true, false};
        newExperiment = postExperiment(newExperiment);
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(resultBuckets.size(), 3);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);

        User user = UserFactory.createUser();
        Assignment assignment = getAssignment(newExperiment, user);
        assertEquals(assignment.status, NEW_ASSIGNMENT);
        assertEquals(assignment.cache, true);

        terminateAndDelete(newExperiment);

    }

    @Test(dependsOnGroups = {"ping"})
    public void t_GetAssignmentsForUserWithClosedBucket() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        String[] labels = {"blue"};
        double[] allocations = {1.0};
        boolean[] control = {false};
        newExperiment = postExperiment(newExperiment);
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        buckets.get(0).state = BUCKET_STATE_OPEN;
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(resultBuckets.size(), 1);

        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);

        buckets.get(0).state = BUCKET_STATE_CLOSED;
        putBucketState(buckets.get(0));

        User user = UserFactory.createUser();
        Assignment assignment = getAssignment(newExperiment, user);
        assertEquals(assignment.status, NO_OPEN_BUCKETS);

        terminateAndDelete(newExperiment);

    }

    @Test(dependsOnGroups = {"ping"})
    public void t_UpdateBucketPayloadOnRunningExperimentAllowed() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        String[] labels = {"blue", "red", "black"};
        double[] allocations = {.40, .30, 0.30};
        boolean[] control = {false, true, false};
        newExperiment = postExperiment(newExperiment);
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(resultBuckets.size(), 3);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);

        Bucket changingBucket = buckets.get(0);
        changingBucket.payload = "new payload";
        changingBucket = putBucket(changingBucket);
        assertEquals(changingBucket.payload, "new payload");

        terminateAndDelete(newExperiment);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_UpdateBucketStateOnRunningExperimentNotAllowed() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        String[] labels = {"blue", "red", "black"};
        double[] allocations = {.40, .30, 0.30};
        boolean[] control = {false, true, false};
        newExperiment = postExperiment(newExperiment);
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(resultBuckets.size(), 3);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);

        Bucket changingBucket1 = buckets.get(1);
        changingBucket1.state = BUCKET_STATE_CLOSED;
        Bucket changingBucket1after = putBucket(changingBucket1, HttpStatus.SC_BAD_REQUEST);

        Bucket bucketAfterChange = getBucket(changingBucket1);
        assertEquals(bucketAfterChange.state, BUCKET_STATE_OPEN);

        terminateAndDelete(newExperiment);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_UpdateBucketLabelOnRunningExperimentNotAllowed() {
        Experiment newExperiment = ExperimentFactory.createExperiment();
        String[] labels = {"blue", "red", "black"};
        double[] allocations = {.40, .30, 0.30};
        boolean[] control = {false, true, false};
        newExperiment = postExperiment(newExperiment);
        List<Bucket> buckets = BucketFactory.createCompleteBuckets(newExperiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        assertEquals(resultBuckets.size(), 3);
        newExperiment.state = EXPERIMENT_STATE_RUNNING;
        putExperiment(newExperiment, HttpStatus.SC_OK);

        Bucket changingBucket1 = buckets.get(0);
        String changingBucket1LabelOriginal = changingBucket1.label;
        changingBucket1.label = "newlabel";
        Bucket changingBucket1after = putBucket(changingBucket1, HttpStatus.SC_NOT_FOUND);
        assertEquals(changingBucket1after.label, null);
        changingBucket1.label = changingBucket1LabelOriginal;
        Bucket bucketAfterChange = getBucket(changingBucket1);
        assertEquals(bucketAfterChange.label, changingBucket1LabelOriginal);

        terminateAndDelete(newExperiment);
    }
}
