/*******************************************************************************
 * Copyright 2016 Intuit
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
package com.intuit.wasabi.tests.perftest;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;

public class PerfTest extends TestBase {

    // lists that holds the experiments of test applications
    List<Experiment> experimentListOfCTG = new ArrayList<Experiment>();
    List<Experiment> experimentListOfQBO = new ArrayList<Experiment>();

    // create performance test Applications
    Application ctgApplication = ApplicationFactory.createApplication().setName("wasabiloadtest_CTG");
    Application qboApplication = ApplicationFactory.createApplication().setName("wasabiloadtest_QBO_Events");

    @BeforeClass
    public void cleanUp() {
        List<Experiment> experimentsListOfCTG = getApplicationExperiments(ctgApplication);

        for (Experiment exp : experimentsListOfCTG) {

            // pause the experiment
            exp.state = Constants.EXPERIMENT_STATE_PAUSED;
            putExperiment(exp);

            // terminate the experiment
            exp.state = Constants.EXPERIMENT_STATE_TERMINATED;
            putExperiment(exp);

            // delete the experiment
            deleteExperiment(exp);
        }

        List<Experiment> experimentsListOfQBO = getApplicationExperiments(qboApplication);

        for (Experiment exp : experimentsListOfQBO) {

            // pause the experiment
            exp.state = Constants.EXPERIMENT_STATE_PAUSED;
            putExperiment(exp);

            // terminate the experiment
            exp.state = Constants.EXPERIMENT_STATE_TERMINATED;
            putExperiment(exp);

            // delete the experiment
            deleteExperiment(exp);
        }

    }

    @Test
    public void prepareData() {

        // now lets create experiments for each applications
        for (int i = 1; i <= 15; i++) {
            Experiment exp = ExperimentFactory.createExperiment("wasabi-perftest-ctgapp-exp" + i)
                    .setApplication(ctgApplication);
            experimentListOfCTG.add(exp);
        }

        experimentListOfCTG = postExperiments(experimentListOfCTG);

        // now lets create experiments for each applications
        for (int i = 1; i <= 15; i++) {
            Experiment exp = ExperimentFactory.createExperiment("wasabi-perftest-qboapp-exp" + i)
                    .setApplication(qboApplication);
            experimentListOfQBO.add(exp);
        }

        // lets create experiment aaa that is used for single assignment
        Experiment exp = ExperimentFactory.createExperiment("aaa").setApplication(qboApplication);
        experimentListOfQBO.add(exp);
        experimentListOfQBO = postExperiments(experimentListOfQBO);

        for (int i = 1; i <= 15; i++) {
            List<Bucket> bucketList = BucketFactory.createBuckets(experimentListOfCTG.get(i - 1), 5);
            postBuckets(bucketList);
        }

        for (int i = 1; i <= 16; i++) {
            List<Bucket> bucketList = BucketFactory.createBuckets(experimentListOfQBO.get(i - 1), 5);
            postBuckets(bucketList);
        }

        // lets change state of the experiment to running
        for (int i = 1; i <= 15; i++) {
            Experiment experiment = experimentListOfCTG.get(i - 1);
            experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
            putExperiment(experiment);
        }

        // lets change state of the experiment to running
        for (int i = 1; i <= 16; i++) {
            Experiment experiment = experimentListOfQBO.get(i - 1);
            experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
            putExperiment(experiment);
        }
        
        System.out.println("experiment ID:" + experimentListOfQBO.get(0).id);
    }

}
