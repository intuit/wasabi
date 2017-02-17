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

import com.intuit.wasabi.tests.library.APIServerConnector;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.model.UserFeedback;
import com.intuit.wasabi.tests.model.factory.UserFeedbackFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * The Feedback test.
 */
public class FeedbackTest extends TestBase {

    private static String currentTime;
    private static String currentTimeIsContactOkFalse;

    /**
     * Initializes the auth test.
     */
    public FeedbackTest() {
        setResponseLogLengthLimit(500);
    }

    ////////////////////
    // Data Providers //
    ///////////////////

    /**
     * Provides supplied user credentials.
     *
     * @return a set of user data
     */
    @DataProvider
    public Object[][] ValidAuthProvider() {
        String userName = appProperties.getProperty("user-name");
        String password = appProperties.getProperty("password");
        String userEmail = appProperties.getProperty("user-email");

        return new Object[][]{
                {userName, password, userEmail}
        };
    }


    /**
     * Provides invalid user credentials.
     *
     * @return a set of user data
     */
    @DataProvider
    public Object[][] InvalidAuthProvider() {
        return new Object[][]{
                {"user@foo.com", ""},
                {"", "user01"},
                {"user@foo.com", "notMyPassword"},
                {"", ""}
        };
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
        }
    }

    /**
     * Tries to post Userfeedback with valid authorizations and valid inputs. ContactOkay set to true so email address will be looked up by by Corpid.
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void createUserFeedback(String userName, String password, String userEmail) {
        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        currentTime = TestUtils.currentTimeString();
        UserFeedback userFeedback1 = UserFeedbackFactory.createUserFeedback()
                .setComments("this is a test comment for test case createUserFeedback at time " + currentTime)
                .setContactOkay(true)
                .setEmail("integration-test" + currentTime + "@example.com")
                .setScore(9)
                .setSubmitted(currentTime);
        postFeedback(userFeedback1, HttpStatus.SC_CREATED, asc);
        sleep(1000);
    }

    /**
     * Tries to post Userfeedback with valid authorizations and valid inputs. ContactOkay set to false so email address provided will be persisted as is.
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void createUserFeedbackIsContactOkFalse(String userName, String password, String userEmail) {
        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        currentTimeIsContactOkFalse = TestUtils.currentTimeString();
        UserFeedback userFeedback1 = UserFeedbackFactory.createUserFeedback()
                .setComments("this is a test comment for test case createUserFeedbackIsContactOkFalse at time " + currentTimeIsContactOkFalse)
                .setContactOkay(false)
                .setEmail("integration-test" + currentTimeIsContactOkFalse + "@example.com")
                .setScore(9)
                .setSubmitted(currentTimeIsContactOkFalse);
        postFeedback(userFeedback1, HttpStatus.SC_CREATED, asc);
        sleep(1000);
    }

    /**
     * Tries to get all user feedbacks with valid authorizations.
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dependsOnMethods = {"createUserFeedback"}, dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void getUserFeedbacksAuth(String userName, String password, String userEmail) {

        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        List<UserFeedback> userFeedbackList = getFeedbacks(HttpStatus.SC_OK, asc);

        boolean found = false;
        for (UserFeedback userFeedback : userFeedbackList) {
            if (userFeedback.getComments().equalsIgnoreCase("this is a test comment for test case createUserFeedback at time " + currentTime)) {
                found = true;
                Assert.assertEquals(userFeedback.getEmail(), userEmail);
            }
        }
        if (!found)
            Assert.fail("did not find the UserFeedback created with timestamp=" + currentTime);
        Assert.assertNotNull(userFeedbackList);
        Assert.assertTrue(userFeedbackList.size() > 0);
    }

    /**
     * Tries to get all user feedbacks by userName with valid authorizations.
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dependsOnMethods = {"createUserFeedback"}, dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void getUserFeedbacksByUsernameAuth(String userName, String password, String userEmail) {

        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        List<UserFeedback> userFeedbackList = getFeedbacksByUsername(HttpStatus.SC_OK, asc, userName);

        boolean found = false;
        for (UserFeedback userFeedback : userFeedbackList) {
            if (userFeedback.getComments().equalsIgnoreCase("this is a test comment for test case createUserFeedback at time " + currentTime)) {
                found = true;
                Assert.assertEquals(userFeedback.getEmail(), userEmail);
                Assert.assertEquals(userFeedback.getScore(), 9);
                Assert.assertEquals(userFeedback.isContactOkay(), true);
                Assert.assertEquals(userFeedback.getUsername(), userName);
            }
        }
        if (!found)
            Assert.fail("did not find the UserFeedback created with timestamp=" + currentTime);
        Assert.assertNotNull(userFeedbackList);
        Assert.assertTrue(userFeedbackList.size() > 0);
    }

    /**
     * Tries to get all user feedbacks by userName with valid authorizations.
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dependsOnMethods = {"createUserFeedbackIsContactOkFalse"}, dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void getUserFeedbacksByUsernameAuthIsContactOkFalse(String userName, String password, String userEmail) {

        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        List<UserFeedback> userFeedbackList = getFeedbacksByUsername(HttpStatus.SC_OK, asc, userName);

        boolean found = false;
        for (UserFeedback userFeedback : userFeedbackList) {
            if (userFeedback.getComments().equalsIgnoreCase("this is a test comment for test case createUserFeedbackIsContactOkFalse at time " + currentTimeIsContactOkFalse)) {
                found = true;
                Assert.assertEquals(userFeedback.getEmail(), "integration-test" + currentTimeIsContactOkFalse + "@example.com");
                Assert.assertEquals(userFeedback.getScore(), 9);
                Assert.assertEquals(userFeedback.isContactOkay(), false);
                Assert.assertEquals(userFeedback.getUsername(), userName);
            }
        }
        if (!found)
            Assert.fail("did not find the UserFeedback created with timestamp=" + currentTimeIsContactOkFalse);
        Assert.assertNotNull(userFeedbackList);
        Assert.assertTrue(userFeedbackList.size() > 0);
    }

    /**
     * Tries to post Userfeedback with valid authorization and invalid inputs.
     * Score=0
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void createUserFeedbackHasScore0(String userName, String password, String userEmail) {
        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        UserFeedback userFeedback1 = UserFeedbackFactory.createUserFeedback()
                .setScore(0);

        postFeedback(userFeedback1, HttpStatus.SC_BAD_REQUEST, asc);
    }

    /**
     * Tries to post Userfeedback with valid authorization and invalid inputs.
     * Score=-1
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void createUserFeedbackHasScoreMinus1(String userName,
                                                 String password, String userEmail) {
        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        UserFeedback userFeedback1 = UserFeedbackFactory.createUserFeedback()
                .setScore(-1);

        postFeedback(userFeedback1, HttpStatus.SC_BAD_REQUEST, asc);
    }

    /**
     * Tries to post Userfeedback with valid authorization and invalid inputs.
     * Score &gt; 10
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void createUserFeedbackHasScore11(String userName, String password, String userEmail) {
        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        UserFeedback userFeedback1 = UserFeedbackFactory.createUserFeedback()
                .setScore(11);

        postFeedback(userFeedback1, HttpStatus.SC_BAD_REQUEST, asc);
    }

    /**
     * Tries to post Userfeedback with valid authorization and invalid inputs.
     * contact_ok = false
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void createUserFeedbackIsContactOkayFalse(String userName,
                                                     String password, String userEmail) {
        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        UserFeedback userFeedback1 = UserFeedbackFactory.createUserFeedback()
                .setContactOkay(false);

        postFeedback(userFeedback1, HttpStatus.SC_BAD_REQUEST, asc);
    }

    @AfterClass
    public void afterClass() {
    }
}
