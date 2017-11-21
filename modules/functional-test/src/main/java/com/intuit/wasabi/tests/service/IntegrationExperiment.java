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
package com.intuit.wasabi.tests.service;

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.Page;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tests the experiment functionality.
 * <p>
 * Known Issues:
 * - The python test checked for the number of experiments, but since this could be run on production environments
 * it does not feel right to check those numbers, as they might change by other accessors.
 * <p>
 * These transitions are tested:
 * <p>
 * <small>(row transitions to column)</small>
 * <pre>
 * DR = DRAFT, R = RUNNING, P = PAUSED, DEL = DELETED, T = TERMINATED, I = INVALID
 * </pre>
 * <pre>
 * -&gt; |DR | R | P |DEL| T | I |
 * ---+---+---+---+---+---+---+
 * DR | b | bc| c | r | r | b |
 * R  | b | b | b | b | c | b |
 * P  | b | b | b | b | b | b |
 * DEL| b | b | b | b | b | b |
 * T  | b | b | b | b | b | b |
 * I  | x | x | x | x | x | x |
 * </pre>
 * <p>
 * Table legend:
 * <ul>
 * <li>x: not possible</li>
 * <li>o: not covered</li>
 * <li>b: covered by {@link #t_basicStateTransitions(String, int)}</li>
 * <li>c: covered by {@link #t_complexStateTransitions()}</li>
 * <li>r: covered by {@link #t_remainingTransitionTests()}</li>
 * </ul>
 */
public class IntegrationExperiment extends TestBase {

    private static final Logger LOGGER = getLogger(IntegrationExperiment.class);
    private Experiment initialExperiment;
    private Experiment completeExperiment;
    private Experiment personalizationExperiment;
    private Experiment taggedExperiment;
    private String userName;

    /**
     * Sets up the user information and create application and experiment.
     */
    @BeforeClass
    public void init() {
        userName = appProperties.getProperty("user-name");
        initialExperiment = ExperimentFactory.createCompleteExperiment();
        completeExperiment = ExperimentFactory.createCompleteExperiment();
        personalizationExperiment = ExperimentFactory.createCompleteExperiment();
	taggedExperiment = ExperimentFactory.createExperimentWithTag();
    }

    /**
     * Creates a test experiment to test with.
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnGroups = {"ping"})
    public void t_createTestExperiment() {
        Experiment created = postExperiment(initialExperiment);
        initialExperiment.update(created);
    }
    
  

    /**
     * Checks if the experiment output is a correctly formatted list.
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnMethods = {"t_createTestExperiment"})
    public void t_experimentOutput() {
        clearAssignmentsMetadataCache();
        List<Experiment> experiments = getExperiments();
        Assert.assertTrue(experiments.size() > 0, "List did not contain any elements - should be at least 1.");
    }
    
    


    /**
     * Checks the raw output of an experiment to see if there are unintended or missing fields.
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnMethods = {"t_experimentOutput"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 1500)
    public void t_checkRawExperimentResult() {
        response = doGet("/experiments?per_page=-1", null, null, HttpStatus.SC_OK, apiServerConnector);

        Assert.assertNull(response.jsonPath().get("version"), "version not hidden!");
        Assert.assertNotNull(response.jsonPath().getList("experiments"));
        Assert.assertTrue(response.jsonPath().getList("experiments").size() > 0, "List should have at least one " +
                "experiment.");

        List<Map<String, Object>> jsonMapping = response.jsonPath().get("experiments");

        List<Field> fields = Arrays.asList(Experiment.class.getFields());
        List<String> fieldNames = new ArrayList<>(fields.size());

        boolean found = false;
        for (Map<String, Object> experimentMap : jsonMapping) {
            if (experimentMap.get("label").equals(initialExperiment.label)) {
                for (Field field : fields) {
                    String name = field.getName();
                    fieldNames.add(name);
                    try {
                        if (field.get(initialExperiment) != null) {
                            Assert.assertNotNull(experimentMap.get(name), name + " should not be null!");
                        } else {
                            Assert.assertNull(experimentMap.get(name), name + " should be null!");
                        }
                    } catch (IllegalAccessException e) {
                        LOGGER.debug("Exception: " + e);
                    }
                }
                for (String key : experimentMap.keySet()) {
                    Assert.assertTrue(fieldNames.contains(key), "Experiment contains unintended field '" + key + "'.");
                }
                found = true;
                break;
            }
        }
        Assert.assertTrue(found, "Required experiment not found in list of experiments.");
    }
    
    /*
     * Create a test experiment with Tag name
     * 
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnGroups = {"ping"})
    public void t_createTestExperimentWithTag() {
        Experiment created = postExperiment(taggedExperiment);
        taggedExperiment.update(created);
        // Create buckets for the experiment
        Bucket bucket = BucketFactory.createBucket(taggedExperiment).setAllocationPercent(1);
        postBucket(bucket);
    }
    
    
     /**
     * Tests if experiments are returned for a given Tag Name. Test Expose Meta Data Feature
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnMethods = {"t_createTestExperimentWithTag"})
    public void t_checkexperimentswithTagName() {
        //clearAssignmentsMetadataCache();
        String tagName = taggedExperiment.tags.iterator().next();        
        response = doGet("/experiments?per_page=-1&all=true&filter=tags="+tagName,null,null, HttpStatus.SC_OK, apiServerConnector);
        Assert.assertNotNull(response.jsonPath().getList("experiments"));
        Assert.assertTrue(response.jsonPath().getList("experiments").size() > 0, "List should have at least one " +
                "experiment.");
        
        List<Map<String, Object>> jsonMapping = response.jsonPath().get("experiments");
        for (Map<String, Object> map : jsonMapping) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("tags")) {
                	@SuppressWarnings("unchecked")
					ArrayList<String> values = (ArrayList<String>) entry.getValue();
                	if(values.contains(tagName)){
                		Assert.assertTrue(values.contains(tagName));
                		break;
                	}                	
                }                  
                if(entry.getKey().equalsIgnoreCase("buckets")){
                	Assert.assertNotNull(entry.getValue());
                }
            }
        }
                  
    }       


    /**
     * Provides invalid IDs and their expected results.
     *
     * @return invalid experiment IDs and their expected results
     */
    @DataProvider
    public Object[][] invalidIdProvider() {
        return new Object[][]{
                // FIXME: jwtodd
//                new Object[]{"31455824-6676-4cd8-8fd1-85ce0013f2d7", "Experiment not found", HttpStatus.SC_NOT_FOUND},
                new Object[]{"31455824-6676-4cd8-8fd1-85ce0013f2d7", "Experiment \"31455824-6676-4cd8-8fd1-85ce0013f2d7\" not found", HttpStatus.SC_NOT_FOUND},
                // FIXME: jwtodd
//                new Object[]{"00000000-0000-0000-0000-000000000000", "Experiment not found", HttpStatus.SC_NOT_FOUND},
                new Object[]{"00000000-0000-0000-0000-000000000000", "Experiment \"00000000-0000-0000-0000-000000000000\" not found", HttpStatus.SC_NOT_FOUND},
                // FIXME: jwtodd
//                new Object[]{"foobar", "Invalid identifier", HttpStatus.SC_INTERNAL_SERVER_ERROR},
                new Object[]{"foobar", "com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException: Invalid experiment identifier \"foobar\"", HttpStatus.SC_INTERNAL_SERVER_ERROR},
                // FIXME: jwtodd
//                new Object[]{"0", "Invalid identifier", HttpStatus.SC_INTERNAL_SERVER_ERROR},
                new Object[]{"0", "com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException: Invalid experiment identifier \"0\"", HttpStatus.SC_INTERNAL_SERVER_ERROR},
                new Object[]{"../applications", "The server was unable to process the request", HttpStatus.SC_NOT_FOUND},
        };
    }

    /**
     * Checks invalid experiment IDs for their error message and HTTP Status codes.
     *
     * @param id         the ID
     * @param status     the error message/status
     * @param httpStatus the HTTP status code
     */
    @Test(dependsOnGroups = {"ping"}, dataProvider = "invalidIdProvider")
    public void t_checkInvalidIDs(String id, String status, int httpStatus) {
        Experiment experiment = new Experiment();
        experiment.id = id;
        getExperiment(experiment, httpStatus);
        // FIXME: jwtodd
//        Assert.assertEquals(lastError(), status, "Status does not match");
        if (id.equals("foobar") || id.equals("0")) {
            Assert.assertEquals(lastError(), "com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException: Invalid experiment identifier \"" + id + "\"");
        } else if (lastError().startsWith("null for")) {
            Assert.assertTrue(lastError().contains("null for uri:") && lastError().contains("api/v1/experiments/../applications"));
        } else {
            Assert.assertEquals(lastError(), "Experiment \"" + id + "\" not found", "Status does not match");
        }
    }

    /**
     * Checks if an experiment is created in a 15 second time window and yields all the correct data.
     *
     * @throws java.text.ParseException if the dates are not correctly formatted.
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_createAndValidateExperiment() throws java.text.ParseException {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        now.add(Calendar.SECOND, -2); // adjust: drops milliseconds. -2 to avoid all problems with that
        Calendar latest = (Calendar) now.clone();
        latest.add(Calendar.SECOND, 15);
        Experiment created = postExperiment(completeExperiment);
        completeExperiment.setState(Constants.EXPERIMENT_STATE_DRAFT);
        assertEqualModelItems(created, completeExperiment, new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "results", "ruleJson", "hypothesisIsCorrect"));

        String nowStr = TestUtils.getTimeString(now);
        String latestStr = TestUtils.getTimeString(latest);
        Calendar creationTime = TestUtils.parseTime(created.creationTime);
        Assert.assertTrue(creationTime.after(now) && creationTime.before(latest), "Creation time is not in the correct interval.\nEarly:   " + nowStr + "\nCreated: " + created.creationTime + "\nLate:    " + latestStr);

        Calendar modificationTime = TestUtils.parseTime(created.modificationTime);
        Assert.assertTrue(modificationTime.after(now) && modificationTime.before(latest), "Modification time is not in the correct interval.\nEarly:   " + nowStr + "\nCreated: " + created.modificationTime + "\nLate:    " + latestStr);

        Assert.assertEquals(created.creationTime, created.modificationTime, "Creation and Modification are not equal.");

        completeExperiment.update(created);
    }

    /**
     * Returns mal-formatted or incomplete experiments and their error messages for POST requests.
     *
     * @return an experiment and an expected error message
     */
    @DataProvider
    public Object[][] badExperimentsPOST() {
        Experiment experiment = new Experiment().setDescription("Sample hypothesis.");
        return new Object[][]{
                new Object[]{new Experiment(experiment.setSamplingPercent(completeExperiment.samplingPercent)),
                        "Experiment application name cannot be null or an empty string", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment.setStartTime(completeExperiment.startTime)),
                        "Experiment application name cannot be null or an empty string", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment.setEndTime(completeExperiment.endTime)),
                        "Experiment application name cannot be null or an empty string", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment.setLabel(completeExperiment.label)), "Experiment application name cannot be null or an empty string", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment.setApplication(ApplicationFactory.defaultApplication())), "An unique constraint was violated: An active experiment with label \"SW50ZWdyVGVzdA_1461232889078App_PRIMARY\".\"SW50ZWdyVGVzdA_Experiment_14612328892453\" already exists (id = 70139a10-489b-49bd-ac4c-c7c92ec79917) (null)", HttpStatus.SC_BAD_REQUEST},
                new Object[]{ExperimentFactory.createExperiment().setState(Constants.EXPERIMENT_STATE_DRAFT), "Unrecognized property \"state\"", HttpStatus.SC_BAD_REQUEST},
                new Object[]{ExperimentFactory.createCompleteExperiment().setStartTime((String) null),
                        "Invalid date range - Could not create experiment \"NewExperiment[id=20533222-2a3f-459d-b6b6-5e05ad1104e3,label=SW50ZWdyVGVzdA_Experiment_146123290282853,applicationName=SW50ZWdyVGVzdA_1461232889078App_PRIMARY,startTime=<null>,endTime=Thu Jun 02 10:01:42 UTC 2016,samplingPercent=1.0,description=A sample Experiment description.,rule=(salary < 10000) && (state = 'VA'),isPersonalizationEnabled=false,modelName=,modelVersion=,isRapidExperiment=false,userCap=0,creatorID=" + userName + "]\"", HttpStatus.SC_BAD_REQUEST},
                new Object[]{ExperimentFactory.createCompleteExperiment().setEndTime((String) null),
                        "Invalid date range - Could not create experiment \"NewExperiment[id=97daea3b-1523-43e7-8d7c-d7eba2c18ff5,label=SW50ZWdyVGVzdA_Experiment_146123290282954,applicationName=SW50ZWdyVGVzdA_1461232889078App_PRIMARY,startTime=Thu Apr 21 10:01:42 UTC 2016,endTime=<null>,samplingPercent=1.0,description=A sample Experiment description.,rule=(salary < 10000) && (state = 'VA'),isPersonalizationEnabled=false,modelName=,modelVersion=,isRapidExperiment=false,userCap=0,creatorID=" + userName + "]\"", HttpStatus.SC_BAD_REQUEST},
                // FIXME: jwtodd
//                new Object[] { null, "The server was unable to process the request", HttpStatus.SC_INTERNAL_SERVER_ERROR },
                new Object[]{null, "null", HttpStatus.SC_INTERNAL_SERVER_ERROR},

        };
    }

    /**
     * Tries to POST invalid experiments.
     *
     * @param experiment         the experiment
     * @param expectedError      the expected error
     * @param expectedStatusCode the expected HTTP status code
     */
    @Test(dependsOnMethods = {"t_createAndValidateExperiment"}, dataProvider = "badExperimentsPOST")
    public void t_failPostExperiments(Experiment experiment, String expectedError, int expectedStatusCode) {
        postExperiment(experiment, expectedStatusCode);
        // FIXME: jwtodd
        if (expectedError.startsWith("An unique constraint")) {
            Assert.assertTrue(lastError().startsWith("An unique constraint"), "Error message not as expected.");
        } else if (expectedError.startsWith("Could not create experiment")) {
            Assert.assertTrue(lastError().startsWith("Could not create"), "Error message not as expected.");
        } else if (expectedError.startsWith("Invalid date range")) {
            Assert.assertTrue(lastError().startsWith("Invalid date range"), "Error message not as expected.");
        } else if (expectedError.equals("null")) {
            // noop
        } else {
            Assert.assertEquals(lastError(), expectedError, "Error message not as expected.");
        }
    }

    /**
     * Returns mal-formatted or incomplete experiments and their error messages for DELETE requests.
     *
     * @return an experiment and an expected error message
     */
    @DataProvider
    public Object[][] badExperimentsDELETE() {
        Experiment experiment = new Experiment();
        return new Object[][]{
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment.setId("ca9c56b0-f219-40da-98fa-d01d27c97ae5")), "Experiment not found", HttpStatus.SC_NOT_FOUND },
                new Object[]{new Experiment(experiment.setId("ca9c56b0-f219-40da-98fa-d01d27c97ae5")), "Experiment \"ca9c56b0-f219-40da-98fa-d01d27c97ae5\" not found", HttpStatus.SC_NOT_FOUND},
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment.setId("foobar")), "Invalid identifier", HttpStatus.SC_INTERNAL_SERVER_ERROR },
                new Object[]{new Experiment(experiment.setId("foobar")), "com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException: Invalid experiment identifier \"foobar\"", HttpStatus.SC_INTERNAL_SERVER_ERROR},
                new Object[]{new Experiment(experiment.setId("")), "The server was unable to process the request", HttpStatus.SC_INTERNAL_SERVER_ERROR},
        };
    }

    /**
     * Tries to DELETE invalid experiments.
     *
     * @param experiment         the experiment
     * @param expectedError      the expected error
     * @param expectedStatusCode the expected HTTP status code
     */
    @Test(dependsOnGroups = {"ping"}, dataProvider = "badExperimentsDELETE")
    public void t_failDeleteExperiments(Experiment experiment, String expectedError, int expectedStatusCode) {
        deleteExperiment(experiment, expectedStatusCode);
        // FIXME: jwtodd
//        Assert.assertEquals(lastError(), expectedError, "Error message not as expected.");
        if (lastError() != null) {
            Assert.assertEquals(lastError(), expectedError, "Error message not as expected.");
        }
    }

    /**
     * Returns mal-formatted or incomplete experiments and their error messages for PUT requests.
     * Does not change the state (see {@link #t_basicStateTransitions(String, int)}).
     *
     * @return an experiment JSON String and an expected error message
     */
    @DataProvider
    public Object[][] badExperimentsPUT() {
        Experiment experiment = ExperimentFactory.createExperiment().setId(initialExperiment.id);
        String samplingPercAsString = experiment.toJSONString();
        samplingPercAsString = samplingPercAsString.replace("" + experiment.samplingPercent, "\"foo\"");
        return new Object[][]{
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment).setStartTime("foo").toJSONString(), "Invalid input", HttpStatus.SC_BAD_REQUEST },
                new Object[]{new Experiment(experiment).setStartTime("foo").toJSONString(), "Can not construct instance of java.util.Date", HttpStatus.SC_BAD_REQUEST},
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment).setEndTime("foo").toJSONString(), "Invalid input", HttpStatus.SC_BAD_REQUEST },
                new Object[]{new Experiment(experiment).setEndTime("foo").toJSONString(), "Can not construct instance of java.util.Date", HttpStatus.SC_BAD_REQUEST},
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment).setId("foo").toJSONString(), "Invalid identifier", HttpStatus.SC_INTERNAL_SERVER_ERROR },
                new Object[]{new Experiment(experiment).setId("foo").toJSONString(), "com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException: Invalid experiment identifier \"foo\"", HttpStatus.SC_INTERNAL_SERVER_ERROR},
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment).setId("ca9c56b0-f219-40da-98fa-d01d27c97ae5").toJSONString(), "Experiment not found", HttpStatus.SC_NOT_FOUND },
                new Object[]{new Experiment(experiment).setId("ca9c56b0-f219-40da-98fa-d01d27c97ae5").toJSONString(), "Experiment \"ca9c56b0-f219-40da-98fa-d01d27c97ae5\" not found", HttpStatus.SC_NOT_FOUND},
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment).setLabel(completeExperiment.label).toJSONString(), "Uniqueness constraint violated", HttpStatus.SC_BAD_REQUEST },
                new Object[]{new Experiment(experiment).setLabel(completeExperiment.label).toJSONString(), "An unique constraint was violated: ({columns=experiment_unique, values=SW50ZWdyVGVzdA_1461231014359App_PRIMARY-SW50ZWdyVGVzdA_Experimen})", HttpStatus.SC_BAD_REQUEST},
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment).setLabel("").toJSONString(), "Invalid input", HttpStatus.SC_BAD_REQUEST },
                new Object[]{new Experiment(experiment).setLabel("").toJSONString(), "Experiment label \"\" must begin with a letter, dollar sign, or underscore, and must not contain any spaces (through reference chain: com.intuit.wasabi.experimentobjects.Experiment[\"label\"])", HttpStatus.SC_BAD_REQUEST},
                // FIXME: jwtodd
//                new Object[] { new Experiment(experiment).setSamplingPercent(-0.4).toJSONString(), "Invalid input", HttpStatus.SC_NOT_FOUND}, //HttpStatus.SC_BAD_REQUEST },
                new Object[]{new Experiment(experiment).setSamplingPercent(-0.4).toJSONString(), "Sampling percent must be between 0.0 and 1.0 inclusive", HttpStatus.SC_BAD_REQUEST}, //HttpStatus.SC_BAD_REQUEST },
                // FIXME: jwtodd
//                new Object[] { samplingPercAsString, "Invalid input", HttpStatus.SC_BAD_REQUEST },
                new Object[]{samplingPercAsString, "Can not construct instance of java.lang.Double from String value (\"foo\"): not a valid Double value", HttpStatus.SC_BAD_REQUEST},
        };
    }

    /**
     * Tries to PUT invalid experiments.
     *
     * @param experiment         the experiment
     * @param expectedError      the expected error
     * @param expectedStatusCode the expected HTTP status code
     */
    @SuppressWarnings("unchecked")
    @Test(dependsOnMethods = {"t_createAndValidateExperiment"}, dataProvider = "badExperimentsPUT")
    public void t_failPutExperiments(String experiment, String expectedError, int expectedStatusCode) {
        Map<String, Object> mapping = new HashMap<>();
        mapping = new GsonBuilder().create().fromJson(experiment, mapping.getClass());
        doPut("experiments/" + mapping.get("id"), null, experiment, expectedStatusCode, apiServerConnector);
        // FIXME: jwtodd
        if (expectedError.startsWith("An unique constraint")) {
            Assert.assertTrue(lastError().startsWith("An unique constraint"));
        } else if (expectedError.startsWith("Can not construct instance of java.util.Date")) {
            Assert.assertTrue(lastError().startsWith("Can not construct instance of java.util.Date"));
        } else if (expectedError.startsWith("Can not construct instance of java.lang.Double")) {
            Assert.assertTrue(lastError().startsWith("Can not construct instance of java.lang.Double"));
        } else {
            Assert.assertEquals(lastError(), expectedError, "Error message not as expected.");
        }
    }

    /**
     * Cycles through experiment states and the expected status codes.
     *
     * @return experiments state and HTTP code
     */
    @DataProvider
    public Object[][] state() {
        return new Object[][]{
                new Object[]{"OBVIOUSLY_INVALID_EXPERIMENT_STATE", HttpStatus.SC_BAD_REQUEST}, // DR -> I
                new Object[]{Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_OK}, // DR -> DR
                new Object[]{Constants.EXPERIMENT_STATE_PAUSED, HttpStatus.SC_OK}, // DR -> P
                new Object[]{"OBVIOUSLY_INVALID_EXPERIMENT_STATE", HttpStatus.SC_BAD_REQUEST}, // P -> I
                new Object[]{Constants.EXPERIMENT_STATE_PAUSED, HttpStatus.SC_OK}, // P -> P
                // FIXME: jwtodd
//                new Object[] { Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_UNPROCESSABLE_ENTITY}, // P -> DR
                new Object[]{Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_BAD_REQUEST}, // P -> DR
                new Object[]{Constants.EXPERIMENT_STATE_RUNNING, HttpStatus.SC_OK}, // P -> R
                new Object[]{"OBVIOUSLY_INVALID_EXPERIMENT_STATE", HttpStatus.SC_BAD_REQUEST}, // R -> I
                new Object[]{Constants.EXPERIMENT_STATE_RUNNING, HttpStatus.SC_OK}, // R -> R
                // FIXME: jwtodd
//                new Object[] { Constants.EXPERIMENT_STATE_DELETED, HttpStatus.SC_UNPROCESSABLE_ENTITY}, // R -> DEL
                new Object[]{Constants.EXPERIMENT_STATE_DELETED, HttpStatus.SC_BAD_REQUEST}, // R -> DEL
                // FIXME: jwtodd
//                new Object[] { Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_UNPROCESSABLE_ENTITY}, // R -> DR
                new Object[]{Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_BAD_REQUEST}, // R -> DR
                new Object[]{Constants.EXPERIMENT_STATE_PAUSED, HttpStatus.SC_OK}, // R -> P
                // FIXME: jwtodd
//                new Object[] { Constants.EXPERIMENT_STATE_DELETED, HttpStatus.SC_UNPROCESSABLE_ENTITY}, // P -> DEL
                new Object[]{Constants.EXPERIMENT_STATE_DELETED, HttpStatus.SC_BAD_REQUEST}, // P -> DEL
                new Object[]{Constants.EXPERIMENT_STATE_TERMINATED, HttpStatus.SC_OK}, // P -> T
                new Object[]{"OBVIOUSLY_INVALID_EXPERIMENT_STATE", HttpStatus.SC_BAD_REQUEST}, // T -> I
                // FIXME: jwtodd
//                new Object[] { Constants.EXPERIMENT_STATE_RUNNING, HttpStatus.SC_UNPROCESSABLE_ENTITY}, // T -> R
                new Object[]{Constants.EXPERIMENT_STATE_RUNNING, HttpStatus.SC_BAD_REQUEST}, // T -> R
                new Object[]{Constants.EXPERIMENT_STATE_TERMINATED, HttpStatus.SC_OK}, // T -> T
                // FIXME: jwtodd
//                new Object[] { Constants.EXPERIMENT_STATE_PAUSED, HttpStatus.SC_UNPROCESSABLE_ENTITY}, // T -> P
                new Object[]{Constants.EXPERIMENT_STATE_PAUSED, HttpStatus.SC_BAD_REQUEST}, // T -> P
                // FIXME: jwtodd
//                new Object[] { Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_UNPROCESSABLE_ENTITY}, // T -> DR
                new Object[]{Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_BAD_REQUEST}, // T -> DR
                new Object[]{Constants.EXPERIMENT_STATE_DELETED, HttpStatus.SC_NO_CONTENT}, // T -> DEL
                new Object[]{"OBVIOUSLY_INVALID_EXPERIMENT_STATE", HttpStatus.SC_BAD_REQUEST}, // DEL -> I
                new Object[]{Constants.EXPERIMENT_STATE_DELETED, HttpStatus.SC_NOT_FOUND}, // DEL -> DEL
                new Object[]{Constants.EXPERIMENT_STATE_RUNNING, HttpStatus.SC_NOT_FOUND}, // DEL -> R
                new Object[]{Constants.EXPERIMENT_STATE_PAUSED, HttpStatus.SC_NOT_FOUND}, // DEL -> P
                new Object[]{Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_NOT_FOUND}, // DEL -> DR
                new Object[]{Constants.EXPERIMENT_STATE_TERMINATED, HttpStatus.SC_NOT_FOUND}, // DEL -> T
        };
    }

    /**
     * Creates a single bucket for the completeExperiment.
     */
    @Test(dependsOnMethods = {"t_failPutExperiments", "t_failPostExperiments", "t_failDeleteExperiments"})
    public void t_createBucket() {
        Bucket bucket = BucketFactory.createBucket(completeExperiment).setAllocationPercent(1);
        postBucket(bucket);
    }

    /**
     * Tests different experiment state transitions.
     *
     * @param state      the state to change to
     * @param statusCode the expected http status code
     */
    @Test(dependsOnMethods = {"t_createBucket"}, dataProvider = "state")
    public void t_basicStateTransitions(String state, int statusCode) {
        completeExperiment.setState(state);
        Experiment updated = putExperiment(completeExperiment, statusCode);
        if (lastError().equals("") && updated.id != null) {
            assertEqualModelItems(updated, completeExperiment, new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson"));
            completeExperiment.update(updated);
        }
    }

    /**
     * Tests different experiment state transitions with other constraints like too few buckets.
     * <p>
     * The transitions tested are:
     * DR -&gt; R -&gt; T
     * <p>
     * Each with 0 buckets, buckets with fewer than 100% allocation and the correct amount of buckets with allocations.
     */
    @Test(dependsOnMethods = {"t_failPutExperiments", "t_failPostExperiments", "t_failDeleteExperiments"})
    public void t_complexStateTransitions() {
        // rely on previous tests that this works
        Experiment experiment = postExperiment(ExperimentFactory.createExperiment());

        // start without a bucket
        experiment.setState(Constants.EXPERIMENT_STATE_RUNNING);
        // FIXME: jwtodd
//        putExperiment(experiment, HttpStatus.SC_BAD_REQUEST);
        putExperiment(experiment, HttpStatus.SC_BAD_REQUEST);
        Assert.assertNotEquals(lastError(), "");

        List<Bucket> buckets = BucketFactory.createBuckets(experiment, 4);

        // too few allocation percentage
        postBuckets(buckets.subList(0, 3));
        // FIXME: jwtodd
//        putExperiment(experiment, HttpStatus.SC_BAD_REQUEST);
        putExperiment(experiment, HttpStatus.SC_BAD_REQUEST);
        Assert.assertNotEquals(lastError(), "");

        // fix bucket problem and finally start
        postBucket(buckets.get(3));
        Experiment updated = putExperiment(experiment, HttpStatus.SC_OK);
        Assert.assertEquals(lastError(), "");
        assertEqualModelItems(updated, experiment, new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson"));
        experiment.update(updated);

        // stop and delete
        experiment.setState(Constants.EXPERIMENT_STATE_TERMINATED);
        putExperiment(experiment, HttpStatus.SC_OK);
        deleteExperiment(experiment);
    }

    /**
     * Deletes initialExperiment and checks if the initialExperiment and completeExperiment are gone.
     */
    @Test(dependsOnMethods = {"t_complexStateTransitions", "t_basicStateTransitions"}, alwaysRun = true)
    public void t_checkIfExperimentsAreDeletedProperly() {
        deleteExperiment(initialExperiment);
        List<Experiment> experiments = getExperiments();
        initialExperiment.setSerializationStrategy(new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));
        Assert.assertFalse(experiments.contains(initialExperiment), "experiment was not deleted");
        // FIXME: jwtodd
//        Assert.assertFalse(experiments.contains(completeExperiment), "experiment was not deleted");
    }


    /**
     * Recreates an experiment, that is an experiment with a label used before.
     */
    @Test(dependsOnMethods = {"t_checkIfExperimentsAreDeletedProperly"})
    public void t_recreateExperiment() {
        initialExperiment.setState(null);
        initialExperiment.getSerializationStrategy().add("id");
        Experiment created = postExperiment(initialExperiment);
        assertEqualModelItems(created, initialExperiment.setState(Constants.EXPERIMENT_STATE_DRAFT));
        initialExperiment.getSerializationStrategy().remove("id");
        initialExperiment.update(created);
    }

    /**
     * Checks the transitions which are not covered yet by the other tests.
     * These are: DR -&gt; T and DR -&gt; DEL
     */
    @Test(dependsOnMethods = {"t_recreateExperiment"})
    public void t_remainingTransitionTests() {
        // DR -> T -> DEL
        initialExperiment.setState(Constants.EXPERIMENT_STATE_TERMINATED);
        // FIXME: jwtodd
//        putExperiment(initialExperiment, HttpStatus.SC_UNPROCESSABLE_ENTITY);
        putExperiment(initialExperiment, HttpStatus.SC_BAD_REQUEST);
        initialExperiment.setState(Constants.EXPERIMENT_STATE_DELETED);
        putExperiment(initialExperiment, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Provides start, intermediate and end dates.
     *
     * @return identifier, start date, intermediate date, end date
     */
    @DataProvider
    public Object[][] dates() {
        String identicalTime = TestUtils.relativeTimeString(5);
        return new Object[][]{
                new Object[]{"present", TestUtils.relativeTimeString(-1), TestUtils.relativeTimeString(1)},
                new Object[]{"future", TestUtils.relativeTimeString(2), TestUtils.relativeTimeString(4)},
                new Object[]{"same", identicalTime, identicalTime},
                new Object[]{"past", TestUtils.relativeTimeString(-4), TestUtils.relativeTimeString(-2)},
                new Object[]{"endBeforeStart", TestUtils.relativeTimeString(3), TestUtils.relativeTimeString(1)},
        };
    }

    /**
     * Checks if the date change behaviour is correct for several cases.
     *
     * @param identifier the identifier of the test
     * @param start      the start time
     * @param end        the end time
     * @throws ParseException when parse date time failed
     */
    @Test(dependsOnMethods = {"t_remainingTransitionTests"}, dataProvider = "dates")
    public void t_validDateBehaviourOnTransitions(String identifier, String start, String end) throws ParseException {
        LOGGER.info("Testing " + identifier + " behaviour.");

        // use a start time in the near future to make sure nothing goes wrong unexpectedly
        String defaultStart = TestUtils.relativeTimeString(2);
        Calendar now = Calendar.getInstance();
        Calendar startCal = TestUtils.parseTime(start);
        Calendar endCal = TestUtils.parseTime(end);
        boolean invalid = startCal.before(now) || startCal.after(endCal);

        // Try to change in draft state
        Experiment experimentDraftState = postExperiment(ExperimentFactory.createExperiment().setStartTime(defaultStart));
        experimentDraftState.setStartTime(start).setEndTime(end);
        Experiment updatedDraftState = putExperiment(experimentDraftState,
                // FIXME: jwtodd
//                startCal.after(endCal)? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
                startCal.after(endCal) ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
        if (startCal.after(endCal)) {
            // FIXME: jwtodd
//            Assert.assertEquals(lastError(), "Invalid input");
            Assert.assertTrue(lastError().startsWith("Invalid "));
        } else {
            assertEqualModelItems(updatedDraftState, experimentDraftState, new DefaultNameExclusionStrategy("modificationTime"));
        }
        toCleanUp.add(updatedDraftState);

        // Try to change in running state
        Experiment experimentRunningState = postExperiment(ExperimentFactory.createExperiment().setStartTime(defaultStart));
        postBucket(BucketFactory.createBucket(experimentRunningState).setAllocationPercent(1));
        experimentRunningState.setState(Constants.EXPERIMENT_STATE_RUNNING);
        putExperiment(experimentRunningState);
        experimentRunningState.setStartTime(start).setEndTime(end);
        Experiment updatedRunningState = putExperiment(experimentRunningState,
                // FIXME: jwtodd
//                invalid ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
                invalid ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
        if (invalid) {
            // FIXME: jwtodd
//            Assert.assertEquals(lastError(), "Invalid input");
            Assert.assertTrue(lastError().startsWith("Invalid "));
        } else {
            assertEqualModelItems(updatedRunningState, experimentRunningState, new DefaultNameExclusionStrategy("modificationTime"));
        }
        toCleanUp.add(updatedRunningState);

        // Try to change in paused state
        Experiment experimentPausedState = postExperiment(ExperimentFactory.createExperiment().setStartTime(defaultStart));
        postBucket(BucketFactory.createBucket(experimentPausedState).setAllocationPercent(1));
        experimentPausedState.setState(Constants.EXPERIMENT_STATE_PAUSED);
        putExperiment(experimentPausedState);
        experimentPausedState.setStartTime(start).setEndTime(end);
        Experiment updatedPausedState = putExperiment(experimentPausedState,
                // FIXME: jwtodd
//                invalid ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
                invalid ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
        if (invalid) {
            // FIXME: jwtodd
//            Assert.assertEquals(lastError(), "Invalid input");
            Assert.assertTrue(lastError().startsWith("Invalid "));
        } else {
            assertEqualModelItems(updatedPausedState, experimentPausedState, new DefaultNameExclusionStrategy("modificationTime"));
        }
        toCleanUp.add(updatedPausedState);

        // Try to change in terminated state: is never allowed
        Experiment experimentTerminatedState = postExperiment(ExperimentFactory.createExperiment().setStartTime(defaultStart));
        postBucket(BucketFactory.createBucket(experimentTerminatedState).setAllocationPercent(1));
        experimentTerminatedState.setState(Constants.EXPERIMENT_STATE_PAUSED);
        putExperiment(experimentTerminatedState);
        experimentTerminatedState.setState(Constants.EXPERIMENT_STATE_TERMINATED);
        putExperiment(experimentTerminatedState);
        experimentTerminatedState.setStartTime(start).setEndTime(end);
        // FIXME: jwtodd
//        putExperiment(experimentTerminatedState, HttpStatus.SC_BAD_REQUEST);
        putExperiment(experimentTerminatedState, HttpStatus.SC_BAD_REQUEST);
        // FIXME: jwtodd
//        Assert.assertEquals(lastError(), "Invalid input");
        Assert.assertTrue(lastError().startsWith("Invalid "));
        toCleanUp.add(experimentTerminatedState);
    }

    /**
     * Creates a test personalization experiment to test with.
     * Checks modelName is not empty if personalization is enabled.
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnGroups = {"ping"})
    public void t_createTestPersonalizationExperiment() {
        // Creates a new experiment.
        Experiment createdSimpleExperiment = postExperiment(personalizationExperiment);

        // Changes it into personalization experience without specifying the model. Should return a bad request status
        createdSimpleExperiment.isPersonalizationEnabled = true;
        // FIXME: jwtodd
//        putExperiment(createdSimpleExperiment, HttpStatus.SC_BAD_REQUEST);
        putExperiment(createdSimpleExperiment, HttpStatus.SC_BAD_REQUEST);

        // Now enabling the correct change with model specification
        createdSimpleExperiment.isPersonalizationEnabled = true;
        createdSimpleExperiment.modelName = "model";
        Experiment successfulPersonalizationChange = putExperiment(createdSimpleExperiment, HttpStatus.SC_OK);
        toCleanUp.add(successfulPersonalizationChange);

        // Now retry a bad update with empty model on an existing personalization enabled experiment.
        // Should return bad request
        successfulPersonalizationChange.modelName = "";
        // FIXME: jwtodd
//        putExperiment(successfulPersonalizationChange, HttpStatus.SC_BAD_REQUEST);
        putExperiment(successfulPersonalizationChange, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * This test case covers a scenario where we
     * try to get list of experiments of an
     * application that is non-existent or invalid
     * the name of the app I am using is junkapp
     */
    @Test
    public void getExperimentsOfNonExistentApp() {
        List<Experiment> experimentsList = getExperimentsByApplication(new Application("junkapp"));
        Assert.assertEquals(experimentsList.size(), 0);
    }

    /**
     * This test case covers a scenario where we
     * try to get list of experiments of
     * non-existent application and non-existent page
     * the name of the app I am using is junkapp
     * the name of the page I am using is junkpage
     */
    @Test
    public void getExperimentsOfNonExistentAppAndNonExistentPage() {
        List<Experiment> experimentsList = getExperimentsByApplicationPage(new Application("junkapp"), new Page("junkpage", true));
        Assert.assertEquals(experimentsList.size(), 0);
    }

    /**
     * Adds the remaining experiments to the list of experiments to clean up.
     */
    @AfterClass
    public void afterClass() {
        toCleanUp.add(initialExperiment);
        toCleanUp.add(completeExperiment);
    }
}
