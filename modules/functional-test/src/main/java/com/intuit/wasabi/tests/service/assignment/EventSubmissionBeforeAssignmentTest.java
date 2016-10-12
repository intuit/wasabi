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

package com.intuit.wasabi.tests.service.assignment;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;

/**
 * Assures that event submissions fail, if the submitting user has no assignment yet.
 */
public class EventSubmissionBeforeAssignmentTest extends TestBase {

    private Experiment experiment;

    @BeforeClass
    public void testSetUp() {
        //create an experiment and populate the experiment POJO
        experiment = ExperimentFactory.createExperiment();
        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);

        //create buckets within the experiment
        List<Bucket> buckets = postBuckets(BucketFactory.createCompleteBuckets(experiment, 2));
        Assert.assertEquals(buckets.size(), 2);

        //change the state of the experiment from DRAFT to RUNNING
        experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
        exp = putExperiment(experiment);
        assertEqualModelItems(exp, experiment);
    }


    @Test
    public void createImpression() {
        //create user
        User user = UserFactory.createUser();

        //create an event of type IMPRESSION and post it to the event endpoint
        Event impression = EventFactory.createImpression();
        postEvent(impression, experiment, user, HttpStatus.SC_NOT_FOUND);
    }


    @AfterClass
    public void testCleanUp() {
        toCleanUp.add(experiment);
        cleanUpExperiments();
    }

}
