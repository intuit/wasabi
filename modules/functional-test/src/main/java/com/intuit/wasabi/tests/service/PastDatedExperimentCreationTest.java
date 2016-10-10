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
package com.intuit.wasabi.tests.service;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;

/**
 * 
 * @created on 09/28/2016 as part of below issue
 * https://github.com/intuit/wasabi/issues/93
 * 
 * steps followed
 * beforetest: setup require data
 * test: do a post on the
 * aftertest: clean 
 */
public class PastDatedExperimentCreationTest extends TestBase{
    static final Logger LOGGER = LoggerFactory.getLogger(PastDatedExperimentCreationTest.class);
    Experiment experiment;

    /**
     * creates an experiment that is previous dated
     */
    @Test
    public void createPreviousDatedExperiment()
    {
        //create an experiment
        experiment = ExperimentFactory.createExperiment();
        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);
        
        //set start time and end time in previous
        experiment.startTime = TestUtils.relativeTimeString(-10);
        experiment.endTime = TestUtils.relativeTimeString(-5);
        Experiment exp = postExperiment(experiment);
        
        //do the required asserts
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
        
        
        //create buckets within the experiment
        List<Bucket> buckets = postBuckets(BucketFactory.createCompleteBuckets(experiment, 2));
        Assert.assertEquals(buckets.size(), 2);
        
        //change the state of experiment to running
        experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
        exp = putExperiment(experiment);
        assertEqualModelItems(exp, experiment);
    }
    
    @AfterClass
    public void testCleanUp(){
        experiment.state = Constants.EXPERIMENT_STATE_TERMINATED;
        Experiment exp = putExperiment(experiment);
        experiment.update(exp);
        deleteExperiment(experiment);
    }


}
