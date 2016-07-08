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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.testng.Assert;

import static org.testng.Assert.*;

import org.testng.annotations.*;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.analytics.AnalyticsParameters;
import com.intuit.wasabi.tests.model.analytics.ExperimentCounts;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import com.jayway.restassured.response.Response;

import static com.intuit.wasabi.tests.library.util.Constants.*;
/**
 * Bucket integration tests
 */
public class RollUpIntegrationTest extends TestBase {

	private static final String FROM_TIME = "fromTime";
	private static final String QBO = "qbo";
	protected static final String RED = "red";
	protected static final String BLUE = "blue";
	
	protected static final String PROD = "PROD";
	protected static final String QA = "QA";
	
	private String yesterday;
	private String today;
	private String tomorrow;
	
	private Experiment experiment;
    private List<Bucket> buckets = new ArrayList<>();
    private String [] labels = { BLUE, RED };
    private double [] allocations = { .50, .50, };
    private boolean [] control = { false, true };
    private User userBill = UserFactory.createUser("Bill");
    private User userJane = UserFactory.createUser("Jane");
    private User userTom = UserFactory.createUser("Tom");

    private User []  users = {  userBill, userJane, userTom };
    
    private String actionImpression = "IMPRESSION";
    private String actionClick = "click";
    private String actionLoveIt = "love it";
	private SimpleDateFormat dateFormat;
	private String tomorrowPlus3;
      
   
	private Connection connection; 
	
    /**
     * Initializes a default experiment.
     */
    public RollUpIntegrationTest() throws Exception {
    	
        setResponseLogLengthLimit(1000);

        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);    
        yesterday =  dateFormat.format(cal.getTime());        
        yesterday += "T00:00:00+0000";
        
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 0);    
        today =  dateFormat.format(cal.getTime());        
        today += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);    
        tomorrow =  dateFormat.format(cal.getTime());        
        tomorrow += "T00:00:00+0000";

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 4);    
        tomorrowPlus3 =  dateFormat.format(cal.getTime());        
        tomorrowPlus3 += "T00:00:00+0000";

        experiment = ExperimentFactory.createExperiment();
        experiment.startTime = yesterday;
        experiment.endTime = tomorrowPlus3;
        experiment.samplingPercent = 1.0;
        experiment.label = "experiment";
        experiment.applicationName = QBO + UUID.randomUUID();
        		
        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);
        
    }

    @BeforeClass
    public void prepareDBConnection() throws Exception {
    	Class.forName("com.mysql.jdbc.Driver").newInstance();
    	connection =
		  DriverManager.getConnection(
				  appProperties.getProperty("database.url"),
				  appProperties.getProperty("database.username"),
				  appProperties.getProperty("database.password")
				  );
     }
    
    @AfterClass
    public void closeDBConnection() throws Exception {
    	connection.close();
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_CreateTwoBuckets() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
        buckets = BucketFactory.createCompleteBuckets(experiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);
        
        Assert.assertEquals(buckets, resultBuckets);
        
        for (Bucket result : resultBuckets) {
            Bucket matching = null;
            for (Bucket cand : buckets) {
           	 if (cand.label.equals(result.label)) {
           		 matching = cand;
           		 break;
           	 }
           		 
            }
        	 assertEquals(result.label, matching.label);
        	 assertEquals(result.isControl, matching.isControl);
        	 assertEquals(result.allocationPercent, matching.allocationPercent);
        	 assertEquals(result.description, matching.description);
        }        
        experiment.state = EXPERIMENT_STATE_RUNNING;
        experiment = putExperiment(experiment);

    }

    @Test(dependsOnMethods = {"t_CreateTwoBuckets"})
    public void t_CheckBasicCounts() throws Exception {
        Map<String,Object> parameters = new HashMap<>();
        parameters.put(FROM_TIME, "");
        List<Event> events = postEvents(experiment, 
        		parameters, true, 
        		HttpStatus.SC_OK, apiServerConnector);
        assertEquals(events.size(), 0);
        System.out.println("Events size" + events);

    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "") + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
    	if ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		int userCount = result.getInt("user_count");
    		int distinctUserCount = result.getInt("distinct_user_count");
    		fail("There should not be any rows but found with first bucket: " + bucket + " userCount: " + userCount + 
    				" distinct_user_count : " + distinctUserCount);
    	}    	
    }

    @Test(dependsOnMethods = {"t_CheckBasicCounts"})
    public void t_PostAssignments() {
    	
        for (User user : users) {
            Assignment result = postAssignment(experiment, user, PROD);
        	
        	assertEquals(result.status, NEW_ASSIGNMENT);
       }

       for (User user : users) {
            Assignment result = postAssignment(experiment, user, QA);
        	
        	assertEquals(result.status, NEW_ASSIGNMENT);
       }
        Map<String,Object> parameters = new HashMap<>();
       parameters.put(FROM_TIME, "");
       List<Event> events = postEvents(experiment, 
       		parameters, true, 
       		HttpStatus.SC_OK, apiServerConnector);
       assertEquals(events.size(), 0);
       for (Event event : events) {
        	assertEquals(event.name, actionImpression);
       }
    }
    
    @Test(dependsOnMethods = {"t_PostAssignments"})
    public void t_PostImpressionsYesterday() {
       for (User user : users) {
       	Event event = EventFactory.createEvent();
       	event.context = PROD;
       	event.name = actionImpression;
       	event.timestamp = yesterday;
       	Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
       	assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
       }
       Map<String,Object> parameters = new HashMap<>();
       parameters.put(FROM_TIME, "");
       parameters.put("context", PROD);
       List<Event> events = postEvents(experiment, 
       		parameters, true, 
       		HttpStatus.SC_OK, apiServerConnector);
       assertEquals(events.size(), 3);
       for (Event event : events) {
        	assertEquals(event.name, actionImpression);
       }

    }

    @Test(dependsOnMethods = {"t_PostImpressionsYesterday"})
    public void t_PostClickToday() {
       User [] users = { userBill, userJane, userTom };
       for (User user : users) {
       	Event event = EventFactory.createEvent();
       	event.context = PROD;
       	event.name = actionClick;
       	event.timestamp = today;
       	Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
       	assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
       }
       Map<String,Object> parameters = new HashMap<>();
       parameters.put(FROM_TIME, today);
       parameters.put("context", PROD);
       List<Event> events = postEvents(experiment, 
       		parameters, true, 
       		HttpStatus.SC_OK, apiServerConnector);
       
       assertEquals(events.size(), 3);
       for (Event event : events) {
        	assertEquals(event.name, actionClick);
       }

    }

    @Test(dependsOnMethods = {"t_PostClickToday"})
    public void t_PostLoveItTomorrow() {
       User [] users = { userJane, userTom };
       for (User user : users) {
       	Event event = EventFactory.createEvent();
       	event.context = PROD;
       	event.name = actionLoveIt;
       	event.timestamp = tomorrow;
       	Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
       	assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
       }
       Map<String,Object> parameters = new HashMap<>();
       parameters.put(FROM_TIME, tomorrow);
       parameters.put("context", PROD);

       List<Event> events = postEvents(experiment, 
       		parameters, true, 
       		HttpStatus.SC_OK, apiServerConnector);
       
       assertEquals(events.size(), 4);
       for (Event event : events) {
    	   if ( event.name.equals(actionLoveIt) || event.name.equals(actionImpression)) {
    		   // ok
    	   } else {
    		   fail("event not loveit or impression: " + event);
    	   }
       }

    }

    @Test(dependsOnMethods = {"t_PostClickToday"})
    public void t_PostImpressionTomorrowQA() {
       User [] users = { userTom, userBill };
       for (User user : users) {
       	Event event = EventFactory.createEvent();
       	event.context = QA;
       	event.name = actionImpression;
       	event.timestamp = tomorrow;
       	Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
       	assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
       }
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForYesterdayByBucketFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" + this.yesterday + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
    	if ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		int userCount = result.getInt("user_count");
    		int distinctUserCount = result.getInt("distinct_user_count");
    		fail("There should not be any rows but found with first bucket: " + bucket + " userCount: " + userCount + 
    				" distinct_user_count : " + distinctUserCount + " query: " + queryGroupByBucket);
    	}    	
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForYesterdayByBucketAndActionFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucketAndAction = "select bucket_label, action, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" + this.yesterday + "' group by bucket_label, action";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucketAndAction);
    	if ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		int userCount = result.getInt("user_count");
    		int distinctUserCount = result.getInt("distinct_user_count");
    		fail("There should not be any rows but found with first bucket: " + bucket + " userCount: " + userCount + 
    				" distinct_user_count : " + distinctUserCount + " query: " + queryGroupByBucketAndAction);
    	}    	
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForYesterdayByBucketFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_impression where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" + this.yesterday + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
		int userCount = 0;
		int distinctUserCount = 0;
		boolean atLeastOneRow = false;
        while ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		userCount += result.getInt("user_count");
    		distinctUserCount += result.getInt("distinct_user_count");
    		atLeastOneRow = true;
    	}
        if ( !atLeastOneRow ) {
        	fail("At least one row expected for query: " +queryGroupByBucket );
        }
        
        assertEquals(userCount,3);
        assertEquals(distinctUserCount,3);
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForYesterdayByBucketFromMySQLProdContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_impression where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and context = 'PROD' and timestamp = '" + this.yesterday + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
		int userCount = 0;
		int distinctUserCount = 0;
		boolean atLeastOneRow = false;
        while ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		userCount += result.getInt("user_count");
    		distinctUserCount += result.getInt("distinct_user_count");
    		atLeastOneRow = true;
    	}
        if ( !atLeastOneRow ) {
        	fail("At least one row expected for query: " +queryGroupByBucket );
        }
        
        assertEquals(userCount,3);
        assertEquals(distinctUserCount,3);
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForYesterdayByBucketFromMySQLQAContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_impression where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and context = 'QA' and timestamp = '" + this.yesterday + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
        if ( result.next()) {
        	fail("Should have no rows for query: " +queryGroupByBucket );
        }
        
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForTomorrowByBucketFromMySQLProdContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_impression where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and context = 'PROD' and timestamp = '" + this.tomorrow + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
        if ( result.next()) {
        	fail("Should have no rows for query: " +queryGroupByBucket );
        }
        
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTodayByBucketFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" + this.today + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
		int userCount = 0;
		int distinctUserCount = 0;
		boolean atLeastOneRow = false;
        while ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		userCount += result.getInt("user_count");
    		distinctUserCount += result.getInt("distinct_user_count");
    		atLeastOneRow = true;
    	}
        if ( !atLeastOneRow ) {
        	fail("At least one row expected for query: " +queryGroupByBucket );
        }
        assertEquals(userCount,3);
        assertEquals(distinctUserCount,3);
    	result.close();
    	statement.close();
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTodayByBucketAndActionFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucketAndAction = "select bucket_label, action, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" 
				+ this.today + "' group by bucket_label, action";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucketAndAction);
		int userCount = 0;
		int distinctUserCount = 0;
		boolean atLeastOneRow = false;
        while ( result.next()) {
        	String action = result.getString("action");
    		userCount += result.getInt("user_count");
    		distinctUserCount += result.getInt("distinct_user_count");
    		assertEquals(actionClick, action);
    		atLeastOneRow = true;
    	}
        if ( !atLeastOneRow ) {
        	fail("At least one row expected for query: " +queryGroupByBucketAndAction );
        }
        assertEquals(userCount,3);
        assertEquals(distinctUserCount,3);
    	result.close();
    	statement.close();
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTodayByBucketFromMySQLQAContext() throws Exception {
    	
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and context = 'QA' and timestamp = '" + this.today + "' group by bucket_label";
    	try (Statement statement = connection.createStatement();
    			ResultSet result = 
    			statement.executeQuery(queryGroupByBucket)){
    		if (result.next()) {
    			fail("There should be no row expected for query: " +queryGroupByBucket );
    		}
    	}
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTodayByBucketAndActionFromMySQLQAContext() throws Exception {
    	
    	String queryGroupByBucketAndAction = "select bucket_label, action, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and context = 'QA' and timestamp = '" + this.today 
				+ "' group by bucket_label, action";
    	try (Statement statement = connection.createStatement();
    			ResultSet result = 
    			statement.executeQuery(queryGroupByBucketAndAction)){
    		if (result.next()) {
    			fail("There should be no row expected for query: " + queryGroupByBucketAndAction );
    		}
    	}
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTomorrowByBucketFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" + this.tomorrow + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
		int userCount = 0;
		int distinctUserCount = 0;
		boolean atLeastOneRow = false;
        while ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		userCount += result.getInt("user_count");
    		distinctUserCount += result.getInt("distinct_user_count");
    		atLeastOneRow = true;
    	}
        if ( !atLeastOneRow ) {
        	fail("At least one row expected for query: " +queryGroupByBucket );
        }
        assertEquals(userCount,2);
        assertEquals(distinctUserCount,2);
    	
    	result.close();
    	statement.close();
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckEventsForTomorrowByBucketAndActionFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucketAndAction = "select bucket_label, action, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_action where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" 
				+ this.tomorrow + "' group by bucket_label, action";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucketAndAction);
		int userCount = 0;
		int distinctUserCount = 0;
		boolean atLeastOneRow = false;
        while ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		userCount += result.getInt("user_count");
    		distinctUserCount += result.getInt("distinct_user_count");
    		atLeastOneRow = true;
    		if ( RED.equals(bucket) || BLUE.equals(bucket) ) {
    			// ok
    		}
    		else {
    			fail("Bucket should be red or blue " + bucket);
    		}
    	}
        if ( !atLeastOneRow ) {
        	fail("At least one row expected for query: " +queryGroupByBucketAndAction );
        }
        assertEquals(userCount,2);
        assertEquals(distinctUserCount,2);
    	
    	result.close();
    	statement.close();
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForTodayByBucketFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_impression where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" + this.today + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
    	if ( result.next()) {
    			fail("should be no impressions for today " + queryGroupByBucket);
    	}
    }

    @Test(dependsOnMethods = {"t_PostImpressionTomorrowQA"})
    public void t_CheckImpressionForTomorrowByBucketFromMySQLAllContext() throws Exception {
    	
    	Statement statement = connection.createStatement();
    	String queryGroupByBucket = "select bucket_label, "
				+ "count(user_id) as user_count, count(distinct user_id) as distinct_user_count  "
				+ "from event_impression where hex(experiment_id) = '" + 
				experiment.id.replace("-", "").toUpperCase() + "' and timestamp = '" + this.tomorrow + "' group by bucket_label";
    	ResultSet result = 
    			statement.executeQuery(queryGroupByBucket);
    	int userCount = 0;
    	int distinctUserCount = 0;
    	boolean atLeastOneRow = false;
        while ( result.next()) {
    		String bucket = result.getString("bucket_label");
    		userCount += result.getInt("user_count");
    		distinctUserCount += result.getInt("distinct_user_count");
    		atLeastOneRow = true;
    	}
        if ( !atLeastOneRow ) {
        	fail("At least one row expected for query: " +queryGroupByBucket );
        }
        assertEquals(userCount,2);
        assertEquals(distinctUserCount,2);
    }

}
