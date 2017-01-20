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

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.APIUser;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.UserRole;
import com.intuit.wasabi.tests.model.factory.APIUserFactory;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.jayway.restassured.path.json.JsonPath;
/**
 * This test class covers a scenario where a super-admin should be able to give permissions to
 * other users to access the app's and the user once he logins should be able to view all the 
 * app's assigned to him 
 */

public class UserPermissionsTest extends TestBase {

	Experiment experiment1,experiment2,experiment3,experiment4;
	String testUser = null;
	String testUserPassword = null;
	String testUserID = null;
	List<Application> applicationList = null;

	@BeforeClass()
	public void initializeTest()
	{
		testUser = appProperties.getProperty("test-user", Constants.DEFAULT_TEST_USER);
		//lets create applications first
		Application application1 = ApplicationFactory.createApplication().setName("testApplication_1");
		Application application2 = ApplicationFactory.createApplication().setName("testApplication_2");
		Application application3 = ApplicationFactory.createApplication().setName("testApplication_3");
		Application application4 = ApplicationFactory.createApplication().setName("testApplication_4");

		//store the applications in arrayList
		applicationList = new ArrayList<Application>();
		applicationList.add(application1);
		applicationList.add(application2);
		applicationList.add(application3);
		applicationList.add(application4);
		
		//create and assign experiments to the applications
		experiment1 = ExperimentFactory.createExperiment().setApplication(application1);
		experiment2 = ExperimentFactory.createExperiment().setApplication(application2);
		experiment3 = ExperimentFactory.createExperiment().setApplication(application3);
		experiment4 = ExperimentFactory.createExperiment().setApplication(application4);
		
		experiment1 = postExperiment(experiment1);
		experiment2 = postExperiment(experiment2);
		experiment3 = postExperiment(experiment3);
		experiment4 = postExperiment(experiment4);
		
		List<Bucket> buckets1 = BucketFactory.createBuckets(experiment1, 3);
		List<Bucket> buckets2 = BucketFactory.createBuckets(experiment2, 3);
		List<Bucket> buckets3 = BucketFactory.createBuckets(experiment3, 3);
		List<Bucket> buckets4 = BucketFactory.createBuckets(experiment4, 3);
		
		 postBuckets(buckets1);
		 postBuckets(buckets2);
		 postBuckets(buckets3);
		 postBuckets(buckets4);

		//lets change state of the experiment to running 
		experiment1.state = Constants.EXPERIMENT_STATE_RUNNING;
		experiment2.state = Constants.EXPERIMENT_STATE_RUNNING;
		experiment3.state = Constants.EXPERIMENT_STATE_RUNNING;
		experiment4.state = Constants.EXPERIMENT_STATE_RUNNING;

		//below we start the experiment
		experiment1 = putExperiment(experiment1);
		experiment2 = putExperiment(experiment2);
		experiment3 = putExperiment(experiment3);
		experiment4 = putExperiment(experiment4);
	}
	
	@Test
	public void testUserPermission()
	{
		//first lets authenticate the user
		APIUser validUser = APIUserFactory.createAPIUser().setEmail(testUser);
		APIUser user = getUserExists(validUser);
		testUserPassword = user.password;
		testUserID = user.userId;
		
		//secondly lets add the testApplications to the user
		postUserRolePermission(testUserID,applicationList,Constants.ROLE_READONLY);
		
		//lets verify that the applications are indeed assigned to the user 
		List<String> actualapplications = getUserApplications(user);
		List<String> expectedlapplications = new ArrayList<>();
		
		for(Application application: applicationList)
			expectedlapplications.add(application.name);
		
		Assert.assertTrue(actualapplications.containsAll(expectedlapplications));
		
	}



	@AfterClass()
	public void cleanUp()
	{
		List<Experiment> experimentsList = new ArrayList<Experiment>();
		experiment1.setState(Constants.EXPERIMENT_STATE_PAUSED);
		experiment2.setState(Constants.EXPERIMENT_STATE_PAUSED);
		experiment3.setState(Constants.EXPERIMENT_STATE_PAUSED);
		experiment4.setState(Constants.EXPERIMENT_STATE_PAUSED);
		
		experiment1 = putExperiment(experiment1);
		experiment2 = putExperiment(experiment2);
		experiment3 = putExperiment(experiment3);
		experiment4 = putExperiment(experiment4);
		
//		experimentsList.add(experiment1);
//		experimentsList.add(experiment2);
//		experimentsList.add(experiment3);
//		experimentsList.add(experiment4);
//		putExperimentList(experimentsList);
		
		experiment1.setState(Constants.EXPERIMENT_STATE_TERMINATED);
		experiment2.setState(Constants.EXPERIMENT_STATE_TERMINATED);
		experiment3.setState(Constants.EXPERIMENT_STATE_TERMINATED);
		experiment4.setState(Constants.EXPERIMENT_STATE_TERMINATED);
		experiment1 = putExperiment(experiment1);
		experiment2 = putExperiment(experiment2);
		experiment3 = putExperiment(experiment3);
		experiment4 = putExperiment(experiment4);
		deleteExperiments(experimentsList);
		
		//thirdly lets delete the user from the application
		for(Application application: applicationList)
			deleteUserRole(testUserID, application.name);

	}

}
