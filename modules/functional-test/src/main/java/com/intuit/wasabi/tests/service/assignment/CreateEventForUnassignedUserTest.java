/*
 * ******************************************************************************
 *  * Copyright 2016 Intuit
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package com.intuit.wasabi.tests.service.assignment;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intuit.wasabi.tests.data.AssignmentDataProvider;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;

/**
 * 
 * @created on 09/24/2016 as part of below issue
 * https://github.com/intuit/wasabi/issues/88
 * 
 * steps followed
 * beforetest: setup require data
 * test: do a post on the
 * aftertest: clean 
 */
public class CreateEventForUnassignedUserTest  extends TestBase{

	static final Logger LOGGER = LoggerFactory.getLogger(CreateEventForUnassignedUserTest.class);
	Experiment experiment;

	@BeforeClass
	public void t_setup()
	{
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
		String url = "/experiments/"+experiment.id+"/buckets";
		String bucketA = "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, " +
				"\"description\": \"Bucket red\",\"payload\": \"This is bucket red\"}";
		response = apiServerConnector.doPost(url, bucketA);
		assertReturnCode(response, HttpStatus.SC_CREATED);
		String bucketB = "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, " +
				"\"description\": \"Bucket blue\",\"payload\": \"This is bucket blue\"}";
		response = apiServerConnector.doPost(url, bucketB);


		//change the state of the experiment from DRAFT to RUNNING
		assertReturnCode(response, HttpStatus.SC_CREATED);
		response = apiServerConnector.doPut("experiments/"+experiment.id, "{\"state\": \"RUNNING\"}");
		assertReturnCode(response, HttpStatus.SC_OK);
	}


	@Test()
	public void t_replicateBug()
	{
		//create user
		User user = UserFactory.createUser();

		//create an event of type IMPRESSION
		String payload = "{\"events\":[{\"name\":\"IMPRESSION\"}]}";

		//build the url and post it to the event endpoint
		String url =  "/events/applications/"+experiment.applicationName+"/"+"experiments/"+experiment.label+"/users/"+user.userID;
		response = apiServerConnector.doPost(url, payload);
		
		//TO-DO: change asserting the error code to the new value after fix from dev
		Assert.assertTrue((response.getStatusCode()!=HttpStatus.SC_CREATED)," status code should not be 201" );
	}


	@AfterClass
	public void t_cleanUp(){
		response = apiServerConnector.doPut("experiments/"+experiment.id, "{\"state\": \"TERMINATED\"}");
		assertReturnCode(response, HttpStatus.SC_OK);
		response = apiServerConnector.doDelete("experiments/"+experiment.id);
		assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
	}

}
