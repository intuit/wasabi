/*
# Copyright 2016 Intuit
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###############################################################################

#
# Integration tests (client-side) of timestamp
# Requires the full stack to be running: service, DB, Cassandra, log uploader
#
 */
package com.intuit.wasabi.tests.service;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.APIUser;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.APIUserFactory;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * This test class covers a scenario where a super-admin should be able to give permissions to
 * other users to access the app's and the user once he logins should be able to view all the
 * app's assigned to him
 */

public class UserPermissionsTest extends TestBase {

    Experiment experiment1, experiment2, experiment3, experiment4;
    String testUser = null;
    String testUserPassword = null;
    String testUserID = null;
    List<Application> applicationList = new ArrayList<>();
    List<Experiment> experimentList = new ArrayList<>();

    static int NUMBER_OF_APPLICATIONS = 4;

    @BeforeClass()
    public void initializeTest() {
        testUser = appProperties.getProperty("test-user", Constants.DEFAULT_TEST_USER);

        for (int i = 1; i <= NUMBER_OF_APPLICATIONS; i++) {
            Application application = ApplicationFactory.createApplication().setName("testApplication_" + i);
            applicationList.add(application);
        }


        //create and assign experiments to the applications
        for (int i = 1; i <= NUMBER_OF_APPLICATIONS; i++) {
            Experiment experiment = ExperimentFactory.createExperiment().setApplication(applicationList.get(i - 1));
            experimentList.add(experiment);
        }

        experimentList = postExperiments(experimentList);

        for (int i = 1; i <= NUMBER_OF_APPLICATIONS; i++) {
            List<Bucket> bucketList = BucketFactory.createBuckets(experimentList.get(i - 1), 3);
            postBuckets(bucketList);
        }

        //lets change state of the experiment to running
        for (int i = 1; i <= NUMBER_OF_APPLICATIONS; i++) {
            Experiment experiment = experimentList.get(i - 1);
            experiment.state = Constants.EXPERIMENT_STATE_RUNNING;
            putExperiment(experiment);
        }
    }

    @Test
    public void testUserPermission() {
        //first lets authenticate the user
        APIUser validUser = APIUserFactory.createAPIUser().setEmail(testUser);
        APIUser user = getUserExists(validUser);
        user.password = (user.password == null || user.password.length() == 0) ? appProperties.getProperty("password") : user.password;
        testUserPassword = user.password;
        testUserID = user.userId;

        //secondly lets add the testApplications to the user
        postUserRolePermission(testUserID, applicationList, Constants.ROLE_READONLY);

        //lets verify that the applications are indeed assigned to the user 
        List<String> actualapplications = getUserApplications(user);
        List<String> expectedlapplications = new ArrayList<>();

        for (Application application : applicationList)
            expectedlapplications.add(application.name);

        Assert.assertTrue(actualapplications.containsAll(expectedlapplications));

    }


    @AfterClass()
    public void cleanUp() {

        //lets pause the experiments and delete them
        for (int i = 1; i <= NUMBER_OF_APPLICATIONS; i++) {

            //pause the experiment
            Experiment experiment = experimentList.get(i - 1);
            experiment.state = Constants.EXPERIMENT_STATE_PAUSED;
            putExperiment(experiment);

            //delete the experiment
            experiment.state = Constants.EXPERIMENT_STATE_TERMINATED;
            putExperiment(experiment);
        }

        //here finally lets delete those experiments
        deleteExperiments(experimentList);

        //thirdly lets delete the user from the application so that t
        for (Application application : applicationList)
            deleteUserRole(testUserID, application.name);
    }

}
