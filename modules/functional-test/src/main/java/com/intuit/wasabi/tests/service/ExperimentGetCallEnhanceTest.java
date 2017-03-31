/*******************************************************************************
 * Copyright 2017 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.tests.service;

import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;

/**
 * Created on 03/21/2017 This test class tests the enhancement made to the GET experiment API call that will now return
 * hypothesisIsCorrect and results fields that were not returned previously
 *
 */
public class ExperimentGetCallEnhanceTest extends TestBase {

    Experiment testExp = null;

    @BeforeClass
    public void setUp() {

        // create experiment
        testExp = ExperimentFactory.createExperiment("_" + UUID.randomUUID().toString(), -1);
        testExp = postExperiment(testExp);

        // create buckets for the test experiment
        List<Bucket> bucketList = BucketFactory.createBuckets(testExp, 5);
        postBuckets(bucketList);

        // below we are starting the experiment
        testExp.state = Constants.EXPERIMENT_STATE_RUNNING;
        putExperiment(testExp);
    }

    @Test
    public void testHypothesisFieldAndResults() {

        // lets set the results and hypothesisIsCorrect fields
        testExp.setResults("this is the result");
        testExp.setHypothesisIsCorrect("yes");
        putExperiment(testExp);

        // lets do a get and make sure that the above two fields are returned
        Experiment exp = getExperiment(testExp);
        Assert.assertNotNull(exp.hypothesisIsCorrect);
        Assert.assertNotNull(exp.results);
    }

    @AfterClass
    public void tearDown() {

        // pause the experiment
        testExp.state = Constants.EXPERIMENT_STATE_PAUSED;
        putExperiment(testExp);

        // delete the experiment
        testExp.state = Constants.EXPERIMENT_STATE_TERMINATED;
        putExperiment(testExp);

        // lets dereference the experiment and call the Garbage collector Daemon
        testExp = null;
        System.gc();
    }
}
