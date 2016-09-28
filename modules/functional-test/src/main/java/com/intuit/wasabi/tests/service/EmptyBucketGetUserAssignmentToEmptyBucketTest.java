package com.intuit.wasabi.tests.service;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameInclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;

/**
 * A test to check if user can be assigned if the previous assignment bucket is empty
 */
public class EmptyBucketGetUserAssignmentToEmptyBucketTest extends TestBase {

    private Experiment experiment;
    private List<Bucket> buckets = new ArrayList<>();
    private User specialUser = UserFactory.createUser("SpecialForBucketTest");

    /**
     * Initializes a default experiment.
     */
    public EmptyBucketGetUserAssignmentToEmptyBucketTest() {
        setResponseLogLengthLimit(1000);

        experiment = ExperimentFactory.createExperiment();

        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);

    }

    /**
     * Test scenario where user is being reassigned from empty bucket to another empty bucket
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_AddUserToExperimentWith2BucektsAndEmptyBucketBothAndReassignToAnotherEmptyBucket() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed.");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
        buckets = BucketFactory.createBuckets(experiment, 2);
        postBuckets(buckets);
        
        experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
        Experiment exp2 = putExperiment(experiment);
        assertEqualModelItems(exp2, experiment);
        experiment.update(exp);
        
        // Assign special user to bucket 0
        Assignment assignment = AssignmentFactory.createAssignment()
                .setAssignment(buckets.get(0).label)
                .setExperimentLabel(experiment.label)
                .setOverwrite(true);
        Assignment putAssignment = putAssignment(experiment, assignment, specialUser);
        assertEqualModelItems(putAssignment, assignment, new DefaultNameInclusionStrategy("assignment"));
        
       // Empty both buckets
       List<Bucket> emptyBucket = new ArrayList<>();
       emptyBucket.add(buckets.get(0));
       emptyBucket.add(buckets.get(1));
       emptyBucket = putBucketsState(emptyBucket, Constants.BUCKET_STATE_EMPTY);
       
       // There should be no bucket available for user
       Assignment getAssignmentAfterAllEmptyBuckets = getAssignment(experiment, specialUser);
       Assert.assertEquals(getAssignmentAfterAllEmptyBuckets.status, Constants.NO_OPEN_BUCKETS);
    }
}
