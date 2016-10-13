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
package com.intuit.wasabi.tests.service.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tests the user authorization endpoint.
 */
public class AuthorizationTest extends TestBase {

    private static final Logger LOGGER = getLogger(AuthorizationTest.class);
    private String userName;
    private String password;
    private String userEmail;
    private String userLastName;

    private Experiment experiment;


    /**
     * Reads parameters from TestNG configuration.
     * For username, password, and email it also tries to read them from the appProperties:
     * {@code user-name}, {@code password}, and {@code user-email}.
     * However, TestNG configurations take precedence, unless they match the defaults (see below).
     * <p>
     * <p>
     * {@code validTokenPattern} can be null (and will be null if not supplied via TestNG XML). If it
     * is null, the appProperties will be searched for "validTokenPattern". If those can not be found
     * either, all tests concerning the validTokenPattern will be ignored and counted as automatic
     * passes.
     *
     * @param username the user name, default: usernameXYZ123456
     * @param password the password, default: passwordXYZ123456
     * @param email    the email address, default: mail@example.org
     */
    @Parameters({"username", "password", "email", "firstName", "lastName", "validTokenPattern"})
    public AuthorizationTest(@Optional("usernameXYZ123456") String username,
                             @Optional("passwordXYZ123456") String password,
                             @Optional("mail@example.org") String email,
                             @Optional("John") String firstName,
                             @Optional("Doe") String lastName,
                             @Optional String validTokenPattern) {
        this.userName = username;
        this.password = password;
        this.userEmail = email;
        this.userLastName = lastName;
    }

    /**
     * Sets up the user information and create application and experiment.
     */
    @BeforeClass
    public void init() {
        if (userName.equals("usernameXYZ123456")) {
            userName = appProperties.getProperty("user-name");
        }
        if (password.equals("passwordXYZ123456")) {
            password = appProperties.getProperty("password");
        }
        if (userEmail.equals("mail@example.org")) {
            userEmail = appProperties.getProperty("user-email");
        }
        userLastName = appProperties.getProperty("user-lastname");
        experiment = ExperimentFactory.createExperiment();
    }

    /**
     * POSTs the experiment to the server and updates it with the returned values.
     */
    @Test(dependsOnGroups = {"ping"})
    public void createExperiments() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
    }

    /**
     * Test getting user permission
     */
    @Test(dependsOnMethods = {"createExperiments"})
    public void getUserPermissions() {
        String uri = "authorization/users/" + userName + "/permissions";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, HttpStatus.SC_OK);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("permissionsList");
        Assert.assertTrue(!jsonStrings.isEmpty());
        boolean foundApp = false;
        for (@SuppressWarnings("rawtypes") Map jsonMap : jsonStrings) {
            String permissionJStr = simpleGson.toJson(jsonMap);
            JsonElement jElement = new JsonParser().parse(permissionJStr);
            JsonObject jobject = jElement.getAsJsonObject();

            JsonElement jElement2 = jobject.get("permissions");
            Assert.assertTrue(jElement2.toString().contains("SUPERADMIN"));

            JsonElement jsonElement = jobject.get("applicationName");
            String applicationName = jsonElement.getAsString();
            if (applicationName.equals(experiment.applicationName)) {
                LOGGER.info("Found application=" + experiment.applicationName);
                foundApp = true;
                break;
            }
        }
        Assert.assertTrue(foundApp);
    }

    /**
     * Test getting user application permission
     */
    @Test(dependsOnMethods = {"createExperiments"})
    public void getUserAppPermissions() {
        String uri = "authorization/users/" + userName + "/applications/" + experiment.applicationName + "/permissions";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, HttpStatus.SC_OK);
        @SuppressWarnings("rawtypes")
        HashMap jsonMap = response.jsonPath().get();
        Assert.assertNotNull(jsonMap);
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) jsonMap.get("permissions");
        Assert.assertTrue(permissions.toString().contains("CREATE"));
        Assert.assertTrue(permissions.toString().contains("READ"));
        Assert.assertTrue(permissions.toString().contains("UPDATE"));
        Assert.assertTrue(permissions.toString().contains("DELETE"));
        Assert.assertTrue(permissions.toString().contains("ADMIN"));
        Assert.assertTrue(permissions.toString().contains("SUPERADMIN"));

        Assert.assertEquals(jsonMap.get("applicationName"), experiment.applicationName);
    }

    /**
     * Test getting user roles
     */
    @Test(dependsOnMethods = {"getUserPermissions"})
    public void getUserRoles() {

        String testRole = "superadmin";
        String uri = "authorization/roles/" + testRole + "/permissions";
        response = apiServerConnector.doGet(uri);

        assertReturnCode(response, HttpStatus.SC_OK);
        @SuppressWarnings("rawtypes")
        HashMap jsonMap = response.jsonPath().get();
        Assert.assertNotNull(jsonMap);
        Assert.assertTrue(jsonMap.toString().contains("SUPERADMIN"));
    }

    /**
     * Test getting application user roles
     */
    @Test(dependsOnMethods = {"getUserRoles"})
    public void getApplicationUsersByRole() {

        String uri = "authorization/applications/" + experiment.applicationName;
        response = apiServerConnector.doGet(uri);

        assertReturnCode(response, HttpStatus.SC_OK);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("roleList");
        Assert.assertTrue(!jsonStrings.isEmpty());
        Assert.assertTrue(jsonStrings.size() == 1);
        // UserRoleList
        for (@SuppressWarnings("rawtypes") Map jsonMap : jsonStrings) {
            String userRoleListJStr = simpleGson.toJson(jsonMap);
            JsonElement jElement = new JsonParser().parse(userRoleListJStr);
            JsonObject jobject = jElement.getAsJsonObject();

            JsonElement jsonElement = jobject.get("firstName");
            Assert.assertEquals(jsonElement.getAsString(), "Wasabi");
            jsonElement = jobject.get("lastName");
            Assert.assertEquals(jsonElement.getAsString(), userLastName);
            jsonElement = jobject.get("role");
            Assert.assertEquals(jsonElement.getAsString(), "ADMIN");
            jsonElement = jobject.get("userEmail");
            Assert.assertEquals(jsonElement.getAsString(), userEmail);
            jsonElement = jobject.get("userID");
            Assert.assertEquals(jsonElement.getAsString(), userName);
            jsonElement = jobject.get("applicationName");
            Assert.assertEquals(jsonElement.getAsString(), experiment.applicationName);
        }
    }

    /**
     * Testing getting application user role again but this time it will return nothing because user role is deleted.
     */
    @Test(dependsOnMethods = {"getUserRoles"})
    public void getApplicationUsersByRoleAgain() {

        // delete the user role
        String uri = "authorization/applications/" + experiment.applicationName + "/users/" + userName + "/roles";
        response = apiServerConnector.doDelete(uri);

        uri = "authorization/applications/" + experiment.applicationName;
        response = apiServerConnector.doGet(uri);

        assertReturnCode(response, HttpStatus.SC_OK);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("roleList");
        Assert.assertTrue(jsonStrings.isEmpty());
    }

    /**
     * Deletes experiments.
     */
    @Test(dependsOnMethods = {"getApplicationUsersByRoleAgain"})
    public void deleteExperiments() {
        deleteExperiment(experiment);
    }
}
