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

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
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
import java.util.Objects;
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
 * <li>b: covered by {@link #basicStateTransitions(String, int)}</li>
 * <li>c: covered by {@link #complexStateTransitions()}</li>
 * <li>r: covered by {@link #remainingTransitionTests()}</li>
 * </ul>
 */
public class ExperimentTest extends TestBase {

    private static final Logger LOGGER = getLogger(ExperimentTest.class);
    private Experiment initialExperiment;
    private Experiment completeExperiment;
    private Experiment personalizationExperiment;

    /**
     * Sets up the user information and create application and experiment.
     */
    @BeforeClass
    public void init() {
        initialExperiment = ExperimentFactory.createCompleteExperiment();
        completeExperiment = ExperimentFactory.createCompleteExperiment();
        personalizationExperiment = ExperimentFactory.createCompleteExperiment();
    }

    /**
     * Creates a test experiment to test with.
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnGroups = {"ping"})
    public void createTestExperiment() {
        Experiment created = postExperiment(initialExperiment);
        initialExperiment.update(created);
    }

    /**
     * Checks if the experiment output is a correctly formatted list.
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnMethods = {"createTestExperiment"})
    public void experimentOutput() {
        List<Experiment> experiments = getExperiments();
        Assert.assertTrue(experiments.size() > 0, "List did not contain any elements - should be at least 1.");
    }

    /**
     * Checks the raw output of an experiment to see if there are unintended or missing fields.
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnMethods = {"experimentOutput"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 1500)
    public void checkRawExperimentResult() {
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
                        if (Objects.nonNull(field.get(initialExperiment))) {
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

    /**
     * Provides invalid IDs and their expected results.
     *
     * @return invalid experiment IDs and their expected results
     */
    @DataProvider
    public Object[][] invalidIdProvider() {
        return new Object[][]{
                new Object[]{"31455824-6676-4cd8-8fd1-85ce0013f2d7", "Experiment \"31455824-6676-4cd8-8fd1-85ce0013f2d7\" not found", HttpStatus.SC_NOT_FOUND},
                new Object[]{"00000000-0000-0000-0000-000000000000", "Experiment \"00000000-0000-0000-0000-000000000000\" not found", HttpStatus.SC_NOT_FOUND},
                new Object[]{"foobar", "Invalid experiment identifier \"foobar\"", HttpStatus.SC_NOT_FOUND},
                new Object[]{"0", "InvalidIdentifierException: Invalid experiment identifier \"0\"", HttpStatus.SC_NOT_FOUND},
                new Object[]{"../applications", "null for uri: ", HttpStatus.SC_NOT_FOUND},
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
    public void checkInvalidIDs(String id, String status, int httpStatus) {
        Experiment experiment = new Experiment();
        experiment.id = id;
        getExperiment(experiment, httpStatus);
        Assert.assertTrue(lastError().contains(status), "Error was \"" + lastError() + "\", did not contain \"" + status + "\"");
    }

    /**
     * Checks if an experiment is created in a 15 second time window and yields all the correct data.
     *
     * @throws java.text.ParseException if the dates are not correctly formatted.
     */
    @Test(dependsOnGroups = {"ping"})
    public void createAndValidateExperiment() throws java.text.ParseException {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        now.add(Calendar.SECOND, -2); // adjust: drops milliseconds. -2 to avoid all problems with that
        Calendar latest = (Calendar) now.clone();
        latest.add(Calendar.SECOND, 15);
        Experiment created = postExperiment(completeExperiment);
        completeExperiment.setState(Constants.EXPERIMENT_STATE_DRAFT);
        assertEqualModelItems(created, completeExperiment, new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson"));

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
                new Object[]{new Experiment(experiment.setSamplingPercent(completeExperiment.samplingPercent)), "Experiment application name cannot be null or an empty string", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment.setStartTime(completeExperiment.startTime)), "Experiment application name cannot be null or an empty string", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment.setEndTime(completeExperiment.endTime)), "Experiment application name cannot be null or an empty string", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment.setLabel(completeExperiment.label)), "Experiment application name cannot be null or an empty string", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment.setApplication(ApplicationFactory.defaultApplication())), "An unique constraint was violated: ", HttpStatus.SC_BAD_REQUEST},
                new Object[]{ExperimentFactory.createExperiment().setState(Constants.EXPERIMENT_STATE_DRAFT), "Unrecognized property \"state\"", HttpStatus.SC_BAD_REQUEST},
                new Object[]{ExperimentFactory.createCompleteExperiment().setStartTime((String) null), "Could not create experiment ", HttpStatus.SC_INTERNAL_SERVER_ERROR},
                new Object[]{ExperimentFactory.createCompleteExperiment().setEndTime((String) null), "Could not create experiment ", HttpStatus.SC_INTERNAL_SERVER_ERROR},
                new Object[]{null, "Something about your request was wrong.", HttpStatus.SC_INTERNAL_SERVER_ERROR},

        };
    }

    /**
     * Tries to POST invalid experiments.
     *
     * @param experiment         the experiment
     * @param expectedError      the expected error
     * @param expectedStatusCode the expected HTTP status code
     */
    @Test(dependsOnMethods = {"createAndValidateExperiment"}, dataProvider = "badExperimentsPOST")
    public void failPostExperiments(Experiment experiment, String expectedError, int expectedStatusCode) {
        postExperiment(experiment, expectedStatusCode);
        Assert.assertTrue(lastError().contains(expectedError), "Error message \"" + lastError()
                + "\" does not contain \"" + expectedError + "\"");
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
                new Object[]{new Experiment(experiment.setId("ca9c56b0-f219-40da-98fa-d01d27c97ae5")), "Experiment \"ca9c56b0-f219-40da-98fa-d01d27c97ae5\" not found", HttpStatus.SC_NOT_FOUND},
                new Object[]{new Experiment(experiment.setId("foobar")), "InvalidIdentifierException: Invalid experiment identifier \"foobar\"", HttpStatus.SC_NOT_FOUND},
                new Object[]{new Experiment(experiment.setId("")), "Maybe you used an unrecognized HTTP method?", HttpStatus.SC_INTERNAL_SERVER_ERROR},
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
    public void failDeleteExperiments(Experiment experiment, String expectedError, int expectedStatusCode) {
        deleteExperiment(experiment, expectedStatusCode);
        Assert.assertTrue(lastError().contains(expectedError), "Error message \"" + lastError()
                + "\" does not contain \"" + expectedError + "\"");
    }

    /**
     * Returns mal-formatted or incomplete experiments and their error messages for PUT requests.
     * Does not change the state (see {@link #basicStateTransitions(String, int)}).
     *
     * @return an experiment JSON String and an expected error message
     */
    @DataProvider
    public Object[][] badExperimentsPUT() {
        Experiment experiment = ExperimentFactory.createExperiment().setId(initialExperiment.id);
        String samplingPercAsString = experiment.toJSONString();
        samplingPercAsString = samplingPercAsString.replace("" + experiment.samplingPercent, "\"foo\"");
        return new Object[][]{
                new Object[]{new Experiment(experiment).setStartTime("foo").toJSONString(), "Can not construct instance of java.util.Date", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment).setEndTime("foo").toJSONString(), "Can not construct instance of java.util.Date", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment).setId("foo").toJSONString(), "InvalidIdentifierException: Invalid experiment identifier \"foo\"", HttpStatus.SC_NOT_FOUND},
                new Object[]{new Experiment(experiment).setId("ca9c56b0-f219-40da-98fa-d01d27c97ae5").toJSONString(), "Experiment \"ca9c56b0-f219-40da-98fa-d01d27c97ae5\" not found", HttpStatus.SC_NOT_FOUND},
                new Object[]{new Experiment(experiment).setLabel(completeExperiment.label).toJSONString(), "An unique constraint was violated:", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment).setLabel("").toJSONString(), "must begin with a letter, dollar sign, or underscore, and must not contain any spaces", HttpStatus.SC_BAD_REQUEST},
                new Object[]{new Experiment(experiment).setSamplingPercent(-0.4).toJSONString(), "Sampling percent must be between 0.0 and 1.0 inclusive", HttpStatus.SC_BAD_REQUEST},
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
    @Test(dependsOnMethods = {"createAndValidateExperiment"}, dataProvider = "badExperimentsPUT")
    public void failPutExperiments(String experiment, String expectedError, int expectedStatusCode) {
        Map<String, Object> mapping = new HashMap<>();
        mapping = new GsonBuilder().create().fromJson(experiment, mapping.getClass());
        doPut("experiments/" + mapping.get("id"), null, experiment, expectedStatusCode, apiServerConnector);
        Assert.assertTrue(lastError().contains(expectedError), "Error message \"" + lastError()
                + "\" does not contain \"" + expectedError + "\"");
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
                new Object[]{Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_BAD_REQUEST}, // P -> DR
                new Object[]{Constants.EXPERIMENT_STATE_RUNNING, HttpStatus.SC_OK}, // P -> R
                new Object[]{"OBVIOUSLY_INVALID_EXPERIMENT_STATE", HttpStatus.SC_BAD_REQUEST}, // R -> I
                new Object[]{Constants.EXPERIMENT_STATE_RUNNING, HttpStatus.SC_OK}, // R -> R
                new Object[]{Constants.EXPERIMENT_STATE_DELETED, HttpStatus.SC_BAD_REQUEST}, // R -> DEL
                new Object[]{Constants.EXPERIMENT_STATE_DRAFT, HttpStatus.SC_BAD_REQUEST}, // R -> DR
                new Object[]{Constants.EXPERIMENT_STATE_PAUSED, HttpStatus.SC_OK}, // R -> P
                new Object[]{Constants.EXPERIMENT_STATE_DELETED, HttpStatus.SC_BAD_REQUEST}, // P -> DEL
                new Object[]{Constants.EXPERIMENT_STATE_TERMINATED, HttpStatus.SC_OK}, // P -> T
                new Object[]{"OBVIOUSLY_INVALID_EXPERIMENT_STATE", HttpStatus.SC_BAD_REQUEST}, // T -> I
                new Object[]{Constants.EXPERIMENT_STATE_RUNNING, HttpStatus.SC_BAD_REQUEST}, // T -> R
                new Object[]{Constants.EXPERIMENT_STATE_TERMINATED, HttpStatus.SC_OK}, // T -> T
                new Object[]{Constants.EXPERIMENT_STATE_PAUSED, HttpStatus.SC_BAD_REQUEST}, // T -> P
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
    @Test(dependsOnMethods = {"failPutExperiments", "failPostExperiments", "failDeleteExperiments"})
    public void createBucket() {
        Bucket bucket = BucketFactory.createBucket(completeExperiment).setAllocationPercent(1);
        postBucket(bucket);
    }

    /**
     * Tests different experiment state transitions.
     *
     * @param state      the state to change to
     * @param statusCode the expected http status code
     */
    @Test(dependsOnMethods = {"createBucket"}, dataProvider = "state")
    public void basicStateTransitions(String state, int statusCode) {
        completeExperiment.setState(state);
        Experiment updated = putExperiment(completeExperiment, statusCode);
        if (lastError().equals("") && Objects.nonNull(updated.id)) {
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
    @Test(dependsOnMethods = {"failPutExperiments", "failPostExperiments", "failDeleteExperiments"})
    public void complexStateTransitions() {
        // rely on previous tests that this works
        Experiment experiment = postExperiment(ExperimentFactory.createExperiment());

        // start without a bucket
        experiment.setState(Constants.EXPERIMENT_STATE_RUNNING);
        putExperiment(experiment, HttpStatus.SC_BAD_REQUEST);
        Assert.assertTrue(lastError().contains("No experiment buckets specified"), "Error message \"" + lastError()
                + "\" does not contain \"No experiment buckets specified\"");

        List<Bucket> buckets = BucketFactory.createBuckets(experiment, 4);

        // too few allocation percentage
        postBuckets(buckets.subList(0, 3));
        putExperiment(experiment, HttpStatus.SC_BAD_REQUEST);
        Assert.assertTrue(lastError().contains("Total allocation must be"), "Error message \"" + lastError()
                + "\" does not contain \"Total allocation must be\"");

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
    @Test(dependsOnMethods = {"complexStateTransitions", "basicStateTransitions"}, alwaysRun = true)
    public void checkIfExperimentsAreDeletedProperly() {
        deleteExperiment(initialExperiment);
        List<Experiment> experiments = getExperiments();
        initialExperiment.setSerializationStrategy(new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson"));
        Assert.assertFalse(experiments.contains(initialExperiment), "experiment was not deleted");
        Assert.assertFalse(experiments.contains(completeExperiment), "experiment was not deleted");
    }


    /**
     * Recreates an experiment, that is an experiment with a label used before.
     */
    @Test(dependsOnMethods = {"checkIfExperimentsAreDeletedProperly"})
    public void recreateExperiment() {
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
    @Test(dependsOnMethods = {"recreateExperiment"})
    public void remainingTransitionTests() {
        // DR -> T -> DEL
        initialExperiment.setState(Constants.EXPERIMENT_STATE_TERMINATED);
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
    @Test(dependsOnMethods = {"remainingTransitionTests"}, dataProvider = "dates")
    public void validDateBehaviourOnTransitions(String identifier, String start, String end) throws ParseException {
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
                startCal.after(endCal) ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
        if (startCal.after(endCal)) {
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
                invalid ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
        if (invalid) {
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
                invalid ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK);
        if (invalid) {
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
        putExperiment(experimentTerminatedState, HttpStatus.SC_BAD_REQUEST);
        Assert.assertTrue(lastError().startsWith("Invalid "));
        toCleanUp.add(experimentTerminatedState);
    }

    /**
     * Creates a test personalization experiment to test with.
     * Checks modelName is not empty if personalization is enabled.
     */
    @Test(groups = {"basicExperimentTests"}, dependsOnGroups = {"ping"})
    public void createTestPersonalizationExperiment() {
        // Creates a new experiment.
        Experiment createdSimpleExperiment = postExperiment(personalizationExperiment);

        // Changes it into personalization experience without specifying the model. Should return a bad request status
        createdSimpleExperiment.isPersonalizationEnabled = true;
        putExperiment(createdSimpleExperiment, HttpStatus.SC_BAD_REQUEST);

        // Now enabling the correct change with model specification
        createdSimpleExperiment.isPersonalizationEnabled = true;
        createdSimpleExperiment.modelName = "model";
        Experiment successfulPersonalizationChange = putExperiment(createdSimpleExperiment, HttpStatus.SC_OK);
        toCleanUp.add(successfulPersonalizationChange);

        // Now retry a bad update with empty model on an existing personalization enabled experiment.
        // Should return bad request
        successfulPersonalizationChange.modelName = "";
        putExperiment(successfulPersonalizationChange, HttpStatus.SC_BAD_REQUEST);
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
