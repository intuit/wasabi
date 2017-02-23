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
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * The auth test.
 */
public class AuthTest extends TestBase {

    private Experiment experiment;

    /**
     * Initializes the auth test.
     */
    public AuthTest() {
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

        return new Object[][]{
                {userName, password}
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

    ///////////////
    // The Tests //
    ///////////////

    // FIXME: comment this out as DefaultAuthentication is currently not checking for invalid auth provider.

//    /**
//     * Tries to create experiments with inalid authorizations.
//     *
//     * @param userName the user name
//     * @param password the user pass
//     */
//    @Test(dataProvider = "InvalidAuthProvider", dependsOnGroups = {"ping"})
//    public void createExperimentUnAuth(String userName, String password) {
//        APIServerConnector asc = apiServerConnector.clone();
//        asc.setUserNameAndPassword(userName, password);
//
//        postExperiment(ExperimentFactory.createExperiment(), HttpStatus.SC_UNAUTHORIZED, asc);
//
//        Assert.assertEquals(response.jsonPath().get("errors.error[0].message"), "Authentication failed", "The message");
//    }

    /**
     * Tries to create experiments with valid authorizations.
     *
     * @param userName the user name
     * @param password the user pass
     */
    @Test(dataProvider = "ValidAuthProvider", dependsOnGroups = {"ping"})
    public void createExperimentAuth(String userName, String password) {
        APIServerConnector asc = apiServerConnector.clone();
        asc.setUserNameAndPassword(userName, password);
        experiment = postExperiment(ExperimentFactory.createExperiment());
    }


    /**
     * Adds the experiment to the list of experiments to clean up.
     */
    @AfterClass
    public void afterClass() {
        toCleanUp.add(experiment);
    }
}
