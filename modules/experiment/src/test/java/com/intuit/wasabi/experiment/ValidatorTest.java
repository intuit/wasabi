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

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.fail;

public class ValidatorTest {

    private final static UUID EXPERIMENT_UUID = UUID.randomUUID();
    private ExperimentValidator validator = new ExperimentValidator();

    @Before
    public void setup() {
    }

    private Bucket.Builder newBucketBuilder() {
        final Experiment.ID experimentID = Experiment.ID.valueOf(EXPERIMENT_UUID);
        return Bucket.newInstance(experimentID, Bucket.Label.valueOf("myBucket"))
                .withAllocationPercent(0.5)
                .withDescription("Bucket description")
                .withControl(true)
                .withPayload("");
    }

    private Experiment.Builder newExperimentBuilder() {
        final Experiment.ID experimentID = Experiment.ID.valueOf(EXPERIMENT_UUID);
        return Experiment.withID(experimentID)
                .withApplicationName(Application.Name.valueOf("test-exp"))
                .withCreationTime(new Date(0))
                .withDescription("test-exp")
                .withEndTime(new Date(0))
                .withLabel(Experiment.Label.valueOf("test-exp"))
                .withSamplingPercent(1.0)
                .withModificationTime(new Date(0))
                .withStartTime(new Date(0))
                .withState(Experiment.State.DRAFT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateBucketInputWithInvalidControl() throws Exception {
        // Bucket with invalid control flag
        newBucketBuilder().withControl(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateBucketBucketInputWithInvalidAllocation() throws Exception {
        // Bucket with invalid AllocationPercent
        newBucketBuilder().withAllocationPercent(null).build();
    }


    @Test
    public void assertDraftState() throws Exception {
        // Experiment in draft state
        Experiment inputExperiment = newExperimentBuilder().build();
        validator.ensureStateIsDraft(inputExperiment);
    }

    @Test
    public void assertDraftStateWithInvalidState() throws Exception {
        // Experiment not in draft state
        Experiment inputExperiment = newExperimentBuilder().withState(Experiment.State.RUNNING).build();

        try {
            validator.ensureStateIsDraft(inputExperiment);
            fail("no exception caught");
        } catch (InvalidExperimentStateException e) {
            // Pass
        }
    }

    @Test
    public void validateExperiment() throws Exception {
        // valid Experiment
        Experiment inputExperiment = newExperimentBuilder().build();

        validator.validateExperiment(inputExperiment);
    }

    @Test(expected = NullPointerException.class)
    public void validateExperimentInvalidState() throws Exception {
        //Experiment with invalid state
        Experiment inputExperiment = newExperimentBuilder().withState(null).build();

        validator.validateExperiment(inputExperiment);
    }


    @Test(expected = IllegalArgumentException.class)
    public void validateExperimentInvalidDateRange() throws Exception {
        Experiment inputExperiment = newExperimentBuilder().withStartTime(new Date(1)).build();
        validator.validateExperiment(inputExperiment);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateExperimentInvalidSamplingPercent() throws Exception {
        Experiment inputExperiment = newExperimentBuilder().withSamplingPercent(1.5).build();

        validator.validateExperiment(inputExperiment);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateExperimentMissingSamplingPercent() throws Exception {
        Experiment inputExperiment = newExperimentBuilder().withSamplingPercent(null).build();

        validator.validateExperiment(inputExperiment);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateExperimentBucketsNoBuckets() throws Exception {
        ArrayList<Bucket> input = new ArrayList<>();

        validator.validateExperimentBuckets(input);
    }

    @Test
    public void validateExperimentBuckets() throws Exception {
        ArrayList<Bucket> input = new ArrayList<>();
        Bucket inputBucket1 = newBucketBuilder().build();
        Bucket inputBucket2 = newBucketBuilder().withControl(false).build();

        input.add(inputBucket1);
        input.add(inputBucket2);

        validator.validateExperimentBuckets(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateExperimentBucketsTooMuchControl() throws Exception {
        ArrayList<Bucket> input = new ArrayList<>();
        Bucket inputBucket1 = newBucketBuilder().build();
        Bucket inputBucket2 = newBucketBuilder().build();

        input.add(inputBucket1);
        input.add(inputBucket2);

        validator.validateExperimentBuckets(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateExperimentBucketsNullControl() throws Exception {
        List<Bucket> input = new ArrayList<Bucket>() {{
            add(newBucketBuilder().build());
            add(newBucketBuilder().build());
        }};

        validator.validateExperimentBuckets(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateExperimentBucketsTooMuchAllocation() throws Exception {
        ArrayList<Bucket> input = new ArrayList<>();
        Bucket inputBucket1 = newBucketBuilder().build();
        Bucket inputBucket2 = newBucketBuilder().withAllocationPercent(1.5).build();

        input.add(inputBucket1);
        input.add(inputBucket2);

        validator.validateExperimentBuckets(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateExperimentBucketsTooFewAllocation() throws Exception {
        ArrayList<Bucket> input = new ArrayList<>();
        Bucket inputBucket1 = newBucketBuilder().build();
        Bucket inputBucket2 = newBucketBuilder().withAllocationPercent(0.05).build();

        input.add(inputBucket1);
        input.add(inputBucket2);

        validator.validateExperimentBuckets(input);
    }
}
