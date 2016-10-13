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
package com.intuit.wasabi.tests.service.experiment;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.slf4j.LoggerFactory.getLogger;

public class MetadataTest extends TestBase {

    private static final String PREFIX_EXPERIMENT = "metadataNullTest_";
    private static final Logger LOGGER = getLogger(MetadataTest.class);
    private int count = 0;

    /**
     * Creates a sample basic experiment
     *
     * @return sample experiment
     */
    private Experiment createExperiment() {
        return ExperimentFactory.createExperiment()
                .setDescription("Sample Description.")
                .setStartTime("2013-01-01T00:00:00+0000")
                .setEndTime("2014-01-01T00:00:00+0000")
                .setLabel(PREFIX_EXPERIMENT + System.currentTimeMillis() + count++)
                .setSamplingPercent(1.0)
                .setApplication(ApplicationFactory.defaultApplication());
    }

    /**
     * Tests if the default descriptions and payloads assigned
     * are empty strings when the "description" and "payload" are missing
     * from the json input.
     */
    @Test(dependsOnGroups = {"ping"})
    public void defaultDescriptionPayLoadEmpty() {
        // create experiment with description missing
        Experiment exp = postExperiment(createExperiment());

        // extract experiment parameters from the JSON out object
        Assert.assertNotNull(exp.id, "Experiment creation failed (No id).");
        Assert.assertNotNull(exp.applicationName, "Experiment creation failed (No applicationName).");
        Assert.assertNotNull(exp.label, "Experiment creation failed (No label).");

        // create a bucket, 100% allocation, description and payload missing
        Bucket bucket = postBucket(BucketFactory.createBucket(exp));
        LOGGER.info("Testing null default bucket description...");
        Assert.assertNull(bucket.description, "Default bucket description should be null");
        LOGGER.info("Testing null default bucket payload...");
        Assert.assertNull(bucket.payload, "default bucket payload should be null");

        // cleanup
        deleteExperiment(exp);
    }

    /**
     * Tests if the returned "description" and "payload" are
     * consistent with supplied strings for these fields
     */
    @Test(dependsOnGroups = {"ping"})
    public void descriptionPayLoadProvided() {
        // create Experiment with given description
        String description = "Non-null metadata";
        Experiment exp = postExperiment(createExperiment().setDescription(description));
        LOGGER.info("Testing non-default experiment description...");
        Assert.assertEquals(exp.description, description, "Experiment description does not match the supplied description");

        // create a bucket, 100% allocation, and explicitly supply description and payload
        String bucketDescription = "Non-null description";
        String bucketPayload = "Non-null payload";
        Bucket bucket = postBucket(BucketFactory.createBucket(exp).setDescription(bucketDescription).setPayload(bucketPayload));
        LOGGER.info("Testing non-default bucket description and payload...");
        Assert.assertEquals(bucket.description, bucketDescription, "Bucket description does not match the supplied description");
        Assert.assertEquals(bucket.payload, bucketPayload, "Bucket payload does not match the supplied payload string");

        // cleanup
        deleteExperiment(exp);
    }

    /**
     * Tests if the default assigned "description" and "payload"
     * are empty strings if the supplied "description" and "payload" are
     * explicitly set to empty string
     */
    @Test(dependsOnGroups = {"ping"})
    public void defaultDescriptionPayLoadNull() {
        // create an experiment with {"description":""}
        Experiment exp = postExperiment(createExperiment());

        // create a bucket with empty description and payload
        Bucket bucket = postBucket(BucketFactory.createBucket(exp).setDescription("").setPayload(""));
        LOGGER.info("Testing bucket description with empty string supplied");
        Assert.assertEquals(bucket.description, null, "Bucket description should be an empty string");
        LOGGER.info("Testing bucket payload with empty string supplied");
        Assert.assertEquals(bucket.payload, null, "Bucket payload should be an empty string");

        // cleanup
        deleteExperiment(exp);
    }

}
