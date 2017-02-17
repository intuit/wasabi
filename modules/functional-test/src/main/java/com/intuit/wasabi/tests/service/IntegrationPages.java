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

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.Page;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.PageFactory;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Integration test for batch/page assignments.
 */
public class IntegrationPages extends TestBase {

    private static final String PREFIX_APPLICATION = "TestAppForPages_";
    private static final String PREFIX_EXPERIMENT = "exp_";
    private static final Logger LOGGER = getLogger(IntegrationPages.class);
    private static final double SAMPLING_PERCENT = 1.0;
    private static final double ALLOCATION_PERCENT = 1.0;
    private static final String TIMESTAMP_STR = "" + System.currentTimeMillis();

    private static Experiment exp_b;

    private void assertPageFalse(Page page, Experiment exp) {
        //Getting the list of pages for a single experiment
        List<Page> pages = getPages(exp);
        Assert.assertFalse(pages.contains(page));
        Assert.assertTrue(pages.size() == 0);
    }

    private void assertPageTrue(Page page, Experiment exp) {
        //Getting the list of pages for a single experiment
        List<Page> pages = getPages(exp);
        Assert.assertTrue(pages.contains(page));
        Assert.assertEquals(pages.get(0).name, page.name, "Invalid list of pages for experiment");
    }

    /**
     * This method queries list of experiments by application and page. Returns true if found.
     *
     * @param exp
     * @param page
     * @return
     */
    private boolean findExperimentByPage(Experiment exp, Page page) {
        boolean found = false;
        //Getting the list of pages for the application
        List<Experiment> experiments = getExperimentsByApplicationPage(new Application(exp.applicationName), page);
        for (Experiment e : experiments) {
            if (e.label.equals(exp.label)) {
                found = true;
            }
        }
        return found;
    }

    private Page testPostPage(Experiment exp, String pageName, boolean allowAssignment) {
        return testPostPage(exp, pageName, allowAssignment, true);
    }

    /**
     * This method tests posting page after setting experiment to the desired experimentState.
     *
     * @param exp
     * @param pageName
     * @param allowAssignment
     * @param experimentState
     */
    private Page testPostPage(Experiment exp, String pageName, boolean allowAssignment, boolean isDeletePage) {

        // try to post page for experiment
        Page page = PageFactory.createPage().setName(pageName).setAllowNewAssignment(allowAssignment);

        Response response = postPages(exp, page, HttpStatus.SC_CREATED);
        Assert.assertEquals(response.getStatusCode(), 201);

        boolean found = findExperimentByPage(exp, page);
        Assert.assertTrue(found);

        assertPageTrue(page, exp);

        if (isDeletePage) {
            response = deletePages(exp, page);
            Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_NO_CONTENT);

            found = findExperimentByPage(exp, page);
            Assert.assertFalse(found);

            assertPageFalse(page, exp);
        }

        return page;
    }

    private Experiment testExperimentChangeState(Experiment exp, String experimentState) {
        LOGGER.info("Changing the experiment exp " + exp.id + " state to " + experimentState);
        exp.setState(experimentState);
        Assert.assertEquals(exp.state, experimentState, "Experiment state is not changed to " + experimentState);

        Experiment retExp = putExperiment(exp.setState(experimentState));
        Assert.assertEquals(retExp.state, exp.state);

        return exp;
    }

    private Experiment testCreateNewExperimentWithBucket(String bucketLabel, String bucketDescription, String bucketPayload, String expName, int startTimeDay, int endTimeDay) {
        String pastStartTime = getDatePlusDays(startTimeDay);
        String futureEndTime = getDatePlusDays(endTimeDay);

        // create experiment with past start and future end times
        Experiment exp = postExperiment(createExperiment(pastStartTime, futureEndTime, expName));
        LOGGER.info("Creating a new experiment with past start and future end times");

        // extract experiment parameters from the JSON out object, Invalid experiment created
        Assert.assertNotNull(exp.id, "Experiment creation failed (No id).");
        Assert.assertNotNull(exp.applicationName, "Experiment creation failed (No applicationName).");
        Assert.assertNotNull(exp.label, "Experiment creation failed (No label).");

        // create a bucket, 100% allocation, and explicitly supply label, description and payload
        Bucket bucket = postBucket(BucketFactory.createBucket(exp).setLabel(bucketLabel).setDescription(bucketDescription).setPayload(bucketPayload).setAllocationPercent(ALLOCATION_PERCENT));
        LOGGER.info("Testing non-default bucket description and payload...");
        Assert.assertEquals(bucket.description, bucketDescription, "Bucket description does not match the supplied description");
        Assert.assertEquals(bucket.payload, bucketPayload, "Bucket payload does not match the supplied payload string");

        return exp;
    }

    private Experiment testCreateNewExperimentWithBucket(String bucketLabel, String bucketDescription, String bucketPayload, String expName) {

        return testCreateNewExperimentWithBucket(bucketLabel, bucketDescription, bucketPayload, expName, -1, 10);
    }

    /**
     * Creates the experiment with provided startTime , endTime and experiment Label
     *
     * @return experiment
     */
    private Experiment createExperiment(String startTime, String endTime, String expSuffix) {
        return ExperimentFactory.createExperiment()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLabel(PREFIX_EXPERIMENT + TIMESTAMP_STR + expSuffix)
                .setSamplingPercent(SAMPLING_PERCENT)
                .setApplication(ApplicationFactory.createApplication().setName(PREFIX_APPLICATION + TIMESTAMP_STR));
    }

    /**
     * This test case covers a scenario where we are
     * trying to get all the pages of an invalid app
     */
    @Test(dependsOnGroups = {"ping"})
    public void testInvalidAppPages() {
        List<Page> pagelist = getPages(new Application("junkapp"));
        Assert.assertEquals(pagelist.size(), 0);
    }

    /**
     * Tests posting page for expired experiment with past start and end times
     */
    @Test(dependsOnGroups = {"ping"})
    public void t_expiredExperimentPages() {

        String pastStartTime = getDatePlusDays(-10);
        String pastEndTime = getDatePlusDays(-5);

        // create experiment with past start and end times
        Experiment exp = postExperiment(createExperiment(pastStartTime, pastEndTime, "a"));
        LOGGER.info("Creating a new experiment with past start and end times to test for failures");

        // extract experiment parameters from the JSON out object
        Assert.assertNotNull(exp.id, "Experiment creation failed (No id).");
        Assert.assertNotNull(exp.applicationName, "Experiment creation failed (No applicationName).");
        Assert.assertNotNull(exp.label, "Experiment creation failed (No label).");

        // try to post page for expired experiment
        Page page = PageFactory.createPage().setName("testPage").setAllowNewAssignment(true);

        Response response = postPages(exp, page, HttpStatus.SC_BAD_REQUEST);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_BAD_REQUEST);

        deleteExperiment(exp);
    }

    /**
     * Tests posting bucket and page for draft experiment with past start and future end times
     */
    @Test(dependsOnMethods = {"t_expiredExperimentPages"})
    public void t_draftExperimentPages() {
        String pastStartTime = getDatePlusDays(-10);
        String futureEndTime = getDatePlusDays(10);

        // create experiment b with past start and future end times
        Experiment exp = postExperiment(createExperiment(pastStartTime, futureEndTime, "b"));
        LOGGER.info("Creating a new experiment with past start and future end times");

        // extract experiment parameters from the JSON out object, Invalid experiment created
        Assert.assertNotNull(exp.id, "Experiment creation failed (No id).");
        Assert.assertNotNull(exp.applicationName, "Experiment creation failed (No applicationName).");
        Assert.assertNotNull(exp.label, "Experiment creation failed (No label).");

        // create a bucket, 100% allocation, and explicitly supply label, description and payload
        String bucketLabel = "red";
        String bucketDescription = "red bucket";
        String bucketPayload = "HTML-JS-red";
        Bucket bucket = postBucket(BucketFactory.createBucket(exp).setLabel(bucketLabel).setDescription(bucketDescription).setPayload(bucketPayload));
        LOGGER.info("Testing non-default bucket description and payload...");
        Assert.assertEquals(bucket.description, bucketDescription, "Bucket description does not match the supplied description");
        Assert.assertEquals(bucket.payload, bucketPayload, "Bucket payload does not match the supplied payload string");

        LOGGER.info("Changing the experiment exp_b state to " + Constants.EXPERIMENT_STATE_DRAFT);
        exp.setState(Constants.EXPERIMENT_STATE_DRAFT);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_DRAFT, "Experiment state is not changed to Draft");

        // try to post page for draft experiment
        Page page = PageFactory.createPage().setName("home").setAllowNewAssignment(true);

        Response response = postPages(exp, page, HttpStatus.SC_CREATED);
        Assert.assertEquals(response.getStatusCode(), 201);

        // cleanup
        deleteExperiment(exp);
    }

    /**
     * Tests posting bucket and page for paused experiment with past start and future end times
     */
    @Test(dependsOnMethods = {"t_draftExperimentPages"})
    public void t_pausedExperimentPages() {
        String pastStartTime = getDatePlusDays(-10);
        String futureEndTime = getDatePlusDays(10);

        // create experiment c with past start and future end times
        Experiment exp = postExperiment(createExperiment(pastStartTime, futureEndTime, "c"));
        LOGGER.info("Creating a new experiment with past start and future end times");

        // extract experiment parameters from the JSON out object, Invalid experiment created
        Assert.assertNotNull(exp.id, "Experiment creation failed (No id).");
        Assert.assertNotNull(exp.applicationName, "Experiment creation failed (No applicationName).");
        Assert.assertNotNull(exp.label, "Experiment creation failed (No label).");

        // create a bucket, 100% allocation, and explicitly supply label, description and payload
        String bucketLabel = "blue";
        String bucketDescription = "blue bucket";
        String bucketPayload = "HTML-JS-blue";
        Bucket bucket = postBucket(BucketFactory.createBucket(exp).setLabel(bucketLabel).setDescription(bucketDescription).setPayload(bucketPayload));
        LOGGER.info("Testing non-default bucket description and payload...");
        Assert.assertEquals(bucket.description, bucketDescription, "Bucket description does not match the supplied description");
        Assert.assertEquals(bucket.payload, bucketPayload, "Bucket payload does not match the supplied payload string");

        LOGGER.info("Changing the experiment exp_b state to " + Constants.EXPERIMENT_STATE_PAUSED);
        exp.setState(Constants.EXPERIMENT_STATE_PAUSED);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_PAUSED, "Experiment state is not changed to Paused");

        // try to post page for expired experiment
        Page page = PageFactory.createPage().setName("purchasePage").setAllowNewAssignment(false);

        Response response = postPages(exp, page, HttpStatus.SC_CREATED);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_CREATED);

        boolean found = false;
        //Getting the list of pages for the application
        List<Experiment> experiments = getExperimentsByApplicationPage(new Application(exp.applicationName), page);
        for (Experiment e : experiments) {
            if (e.label.equals(exp.label)) found = true;
        }
        Assert.assertTrue(found);

        //Getting the list of pages for a single experiment
        List<Page> pages = getPages(exp);
        Assert.assertTrue(pages.contains(page));
        Assert.assertEquals(pages.get(0).name, page.name, "Invalid list of pages for experiment");

        // cleanup
        deleteExperiment(exp);
    }

    /**
     * Tests posting new pages for terminated experiment
     */
    @Test(dependsOnMethods = {"t_pausedExperimentPages"})
    public void t_terminatedExperimentPages() {
        String pastStartTime = getDatePlusDays(-10);
        String futureEndTime = getDatePlusDays(10);

        // create experiment c with past start and future end times
        Experiment exp = postExperiment(createExperiment(pastStartTime, futureEndTime, "b"));
        LOGGER.info("Creating a new experiment with past start and future end times");

        // extract experiment parameters from the JSON out object
        Assert.assertNotNull(exp.id, "Experiment creation failed (No id).");
        Assert.assertNotNull(exp.applicationName, "Experiment creation failed (No applicationName).");
        Assert.assertNotNull(exp.label, "Experiment creation failed (No label).");

        LOGGER.info("Changing the experiment exp_b state to " + Constants.EXPERIMENT_STATE_TERMINATED);
        exp.setState(Constants.EXPERIMENT_STATE_TERMINATED);
        Assert.assertEquals(exp.state, Constants.EXPERIMENT_STATE_TERMINATED, "Experiment state is not changed to Terminated");

        // try to post page for terminated experiment
        Page page = PageFactory.createPage().setName("home").setAllowNewAssignment(true);

        Response response = postPages(exp, page, HttpStatus.SC_CREATED);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_CREATED);

        // cleanup
        deleteExperiment(exp);
    }

    /**
     * Tests posting new pages for deleted experiment
     */
    @Test(dependsOnMethods = {"t_terminatedExperimentPages"})
    public void t_deletedExperimentPages() {
        String pastStartTime = getDatePlusDays(-10);
        String futureEndTime = getDatePlusDays(10);

        // create experiment b with past start and future end times
        Experiment exp = postExperiment(createExperiment(pastStartTime, futureEndTime, "b"));
        LOGGER.info("Creating a new experiment with past start and future end times");

        // extract experiment parameters from the JSON out object
        Assert.assertNotNull(exp.id, "Experiment creation failed (No id).");
        Assert.assertNotNull(exp.applicationName, "Experiment creation failed (No applicationName).");
        Assert.assertNotNull(exp.label, "Experiment creation failed (No label).");

        deleteExperiment(exp);

        //Clear assignments metadata after experiment deletion
        clearAssignmentsMetadataCache();

        List<Experiment> experiments = getExperiments();
        Assert.assertFalse(experiments.contains(exp), "experiment was not deleted");

        // try to post page for deleted experiment
        Page page = PageFactory.createPage().setName("home").setAllowNewAssignment(true);

        Response response = postPages(exp, page, HttpStatus.SC_NOT_FOUND);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_NOT_FOUND);
    }

    /**
     * create new experiment b
     */
    @Test(dependsOnMethods = {"t_deletedExperimentPages"})
    public void t_createNewExperiment_b() {
        exp_b = testCreateNewExperimentWithBucket("red", "red bucket", "HTML-JS-red", "b");
    }

    /**
     * Test posting page to experiment in different state.
     */
    @Test(dependsOnMethods = {"t_createNewExperiment_b"})
    public void t_postPagesByExperimentState_exp_b() {

        testPostPage(exp_b, "home", true);
        exp_b = testExperimentChangeState(exp_b, Constants.EXPERIMENT_STATE_RUNNING);
        testPostPage(exp_b, "landingPage", false);
        exp_b = testExperimentChangeState(exp_b, Constants.EXPERIMENT_STATE_PAUSED);
        testPostPage(exp_b, "shoppingCart", true);
    }

    @Test(dependsOnMethods = {"t_postPagesByExperimentState_exp_b"})
    public void t_retrievePagesAcrossExperiments() {

        LOGGER.info("Testing retrieving pages of applications across experiments");
        Experiment exp_c = testCreateNewExperimentWithBucket("blue", "blue bucket", "HTML-JS-blue", "c");
        LOGGER.info("Posting a page to experiment exp_c.id '" + exp_c.id + "' in application " + exp_c.applicationName);

        String newPage = "purchasePage";
        // try to post page for exp c
        Page page = PageFactory.createPage().setName(newPage).setAllowNewAssignment(false);

        Response response = postPages(exp_c, page, HttpStatus.SC_CREATED);
        Assert.assertEquals(response.getStatusCode(), 201);
        LOGGER.info("Verifying if page '" + newPage + "' is found by querying the application " + exp_c.applicationName);
        boolean found = findExperimentByPage(exp_c, page);
        Assert.assertTrue(found);

        LOGGER.info("Verifying if page '" + newPage + "' is found by querying the experiment " + exp_c.id);
        assertPageTrue(page, exp_c);

        // try to post page for exp b
        response = postPages(exp_b, page, HttpStatus.SC_CREATED);
        Assert.assertEquals(response.getStatusCode(), 201);

        LOGGER.info("Verifying if page '" + newPage + "' is found in both exp_b '" + exp_b.id + "' and exp_c '" + exp_c.id + "' by querying the application " + exp_c.applicationName);
        found = findExperimentByPage(exp_b, page);
        Assert.assertTrue(found);
        found = findExperimentByPage(exp_c, page);
        Assert.assertTrue(found);

        LOGGER.info("Deleting page " + page.name + " from exp_c " + exp_c.id);
        response = deletePages(exp_c, page);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_NO_CONTENT);
        LOGGER.info("Verifying page " + page.name + " is not in exp_c " + exp_c.id);
        found = findExperimentByPage(exp_c, page);
        Assert.assertFalse(found);

        LOGGER.info("Deleting page " + page.name + " from exp_b " + exp_b.id);
        response = deletePages(exp_b, page);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_NO_CONTENT);
        LOGGER.info("Verifying page " + page.name + " is not in exp_b " + exp_b.id);
        found = findExperimentByPage(exp_b, page);
        Assert.assertFalse(found);

        LOGGER.info("Verifying non of the experiments have " + page.name + ".");
        found = findExperimentByPage(exp_b, page);
        Assert.assertFalse(found);

        deleteExperiment(exp_c);
    }

    /**
     * Test posting page to experiment in different state.
     */
    @Test(dependsOnMethods = {"t_retrievePagesAcrossExperiments"})
    public void t_postPagesToExperment_TerminateState_exp_b() {
        exp_b = testExperimentChangeState(exp_b, Constants.EXPERIMENT_STATE_TERMINATED);

        // try to post page for exp b in terminated state
        Page page = PageFactory.createPage().setName("home").setAllowNewAssignment(true);
        Response response = postPages(exp_b, page, HttpStatus.SC_BAD_REQUEST);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_BAD_REQUEST);

        deleteExperiment(exp_b);
        // try to post page to deleted exp b 
        page = PageFactory.createPage().setName("home").setAllowNewAssignment(true);
        response = postPages(exp_b, page, HttpStatus.SC_NOT_FOUND);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_NOT_FOUND);

    }

    /**
     * Tests batch assignments
     */
    @Test(dependsOnMethods = {"t_postPagesToExperment_TerminateState_exp_b"})
    public void t_batchExperimentPages() {

        Experiment exp_X = testCreateNewExperimentWithBucket("red", "red bucket", "HTML-JS-red", "X");
        Experiment exp_Y = testCreateNewExperimentWithBucket("blue", "blue bucket", "HTML-JS-blue", "Y");
        Experiment exp_Z = testCreateNewExperimentWithBucket("green", "green bucket", "HTML-JS-green", "Z");

        exp_X = testExperimentChangeState(exp_X, Constants.EXPERIMENT_STATE_RUNNING);
        exp_Y = testExperimentChangeState(exp_Y, Constants.EXPERIMENT_STATE_RUNNING);
        exp_Z = testExperimentChangeState(exp_Z, Constants.EXPERIMENT_STATE_RUNNING);

        //Post pages to exp_X, exp_Y, exp_Z
        String pageName = "ConfirmationPage";
        LOGGER.info(String.format("Posting page %s to experiment %s, %s and %s", pageName, exp_X.id, exp_Y.id, exp_Z.id));
        Page page_allowAssignTrue = testPostPage(exp_X, pageName, true /* allowAssignment=true */, false);
        testPostPage(exp_Y, pageName, true, false);
        Page page_allowAssignFalse = testPostPage(exp_Z, pageName, false /* allowAssignment=false */, false);

        User testUser = new User("ironman");
        LOGGER.info(String.format("Generating assignments for a single user %s for the experiments associated to page %s", testUser.userID, page_allowAssignTrue.name));
        boolean foundX = false;
        boolean foundY = false;
        boolean foundZ = false;
        LOGGER.info(String.format("Exp X %s ", exp_X.id));
        LOGGER.info(String.format("Exp Y %s ", exp_Y.id));
        LOGGER.info(String.format("Exp Z %s ", exp_Z.id));

        List<Assignment> assignments = postAssignments(new Application(exp_X.applicationName), page_allowAssignTrue, testUser, null, null, true, true);
        for (Assignment a : assignments) {
            LOGGER.info(String.format("The assignment is %s while exp X %s exp Y %s exp Z %s", a.experimentLabel, exp_X.label, exp_Y.label, exp_Z.label));
            if (!foundX && a.experimentLabel.equals(exp_X.label)) foundX = true;
            if (!foundY && a.experimentLabel.equals(exp_Y.label)) foundY = true;
            if (!foundZ && a.experimentLabel.equals(exp_Z.label)) foundZ = true;
        }
        Assert.assertEquals(foundX, true, "Assignment to experiment X missing");
        Assert.assertEquals(foundY, true, "Assignment to experiment Y missing");
        LOGGER.info("Page allowAssignment is set to false for exp_Z. Assignment to exp_Z should be misssing!");
        Assert.assertEquals(foundZ, false, "Assignment to experiment Z missing");

        LOGGER.info("Generating an assignment for a user on experiment Z with a direct assignment call");
        Assignment assignment = getAssignment(exp_Z, testUser);
        Assert.assertEquals(assignment.status, "NEW_ASSIGNMENT", "Assignment status wrong.");
        Assert.assertTrue(assignment.cache, "Assignment.cache not true.");

        LOGGER.info(String.format("Generating assignments AGAIN for a single user %s for the experiments associated to page %s. This time assignment should be found in exp Z", testUser.userID, page_allowAssignTrue.name));
        assignments = postAssignments(new Application(exp_X.applicationName), page_allowAssignTrue, testUser, null, null, true, true);
        for (Assignment a : assignments) {
            LOGGER.info(String.format("The assignment is %s while exp X %s exp Y %s exp Z %s", a.experimentLabel, exp_X.label, exp_Y.label, exp_Z.label));
            if (!foundX && a.experimentLabel.equals(exp_X.label)) foundX = true;
            if (!foundY && a.experimentLabel.equals(exp_Y.label)) foundY = true;
            if (!foundZ && a.experimentLabel.equals(exp_Z.label)) foundZ = true;
        }
        Assert.assertEquals(foundX, true, "Assignment to experiment X missing");
        Assert.assertEquals(foundY, true, "Assignment to experiment Y missing");
        Assert.assertEquals(foundZ, true, "Assignment to experiment Z missing");

        deletePages(exp_X, page_allowAssignTrue);
        deletePages(exp_Y, page_allowAssignTrue);
        deletePages(exp_Z, page_allowAssignFalse);

        testExperimentChangeState(exp_X, Constants.EXPERIMENT_STATE_TERMINATED);
        deleteExperiment(exp_X);
        testExperimentChangeState(exp_Y, Constants.EXPERIMENT_STATE_TERMINATED);
        deleteExperiment(exp_Y);
        testExperimentChangeState(exp_Z, Constants.EXPERIMENT_STATE_TERMINATED);
        deleteExperiment(exp_Z);
    }

    /**
     * JBA-227: Search Pages API: Create 3 valid Experiment.Two experiments have page testPage1.Third has some other page.
     * The experiment list for page testPage1 will retrieve two correct experiments.
     */
    @Test(dependsOnMethods = {"t_batchExperimentPages"})
    public void t_batchExperimentPages_Issue_JBA_227() {

        LOGGER.info("Creating the second valid new experiment AA");
        Experiment exp_aa = testCreateNewExperimentWithBucket("red", "red bucket", "HTML-JS-red", "AA", 10, 20);
        LOGGER.info("Creating the second valid new experiment BB");
        Experiment exp_bb = testCreateNewExperimentWithBucket("blue", "blue bucket", "HTML-JS-blue", "BB", 11, 21);
        LOGGER.info("Creating the second valid new experiment CC");
        Experiment exp_cc = testCreateNewExperimentWithBucket("green", "green bucket", "HTML-JS-green", "CC", 13, 23);

        // Post pages to exp_AA, exp_BB, exp_CC
        String testPage1Name = "testPage1";
        LOGGER.info(String.format("Posting page %s to experiment %s, %s and %s", testPage1Name, exp_aa.id, exp_bb.id, exp_cc.id));
        Page testPage1 = testPostPage(exp_aa, testPage1Name, false /* allowAssignment=true */, false);
        testPostPage(exp_bb, testPage1Name, false/* allowAssignment=false */, false);
        String otherPageName = "otherPage";
        Page otherPage = testPostPage(exp_cc, otherPageName, false /* allowAssignment=false */, false);

        LOGGER.info("Verifying if page '" + testPage1Name + "' is found in both exp_aa '" + exp_aa.id + "' and exp_bb '" + exp_bb.id + "' by querying the application " + exp_aa.applicationName);
        boolean found = findExperimentByPage(exp_aa, testPage1);
        Assert.assertTrue(found);
        LOGGER.info("Found page " + testPage1Name + " in exp_aa " + exp_aa.id);
        found = findExperimentByPage(exp_bb, testPage1);
        Assert.assertTrue(found);
        LOGGER.info("Found page " + testPage1Name + " in exp_bb " + exp_bb.id);

        found = findExperimentByPage(exp_cc, testPage1);
        LOGGER.info("Should not find page " + testPage1Name + " in exp_cc " + exp_cc.id);
        Assert.assertFalse(found);

        deletePages(exp_aa, testPage1);
        deletePages(exp_bb, testPage1);
        deletePages(exp_cc, otherPage);

        deleteExperiment(exp_aa);
        deleteExperiment(exp_bb);
        deleteExperiment(exp_cc);
    }

    /**
     * Returns date n days later in UTC timezone as string
     *
     * @return string
     */
    private String getDatePlusDays(int days) {
        DateTime dt = new DateTime();
        dt = dt.plusDays(days);
        final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dt.toDate());
    }
}
