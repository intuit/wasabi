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

import static com.intuit.wasabi.tests.library.util.Constants.USER_EMAIL;
import static com.intuit.wasabi.tests.library.util.Constants.USER_FIRST_NAME;
import static com.intuit.wasabi.tests.library.util.Constants.USER_LAST_NAME;
import static com.intuit.wasabi.tests.library.util.Constants.USER_READER;
import static com.intuit.wasabi.tests.library.util.Constants.USER_ROLE;
import static com.intuit.wasabi.tests.library.util.Constants.USER_USER_ID;
import static com.intuit.wasabi.tests.library.util.Constants.WASABI_EMAIL;
import static com.intuit.wasabi.tests.library.util.Constants.WASABI_FIRST_NAME;
import static com.intuit.wasabi.tests.library.util.Constants.WASABI_LAST_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tests the user authorization endpoint.
 */
public class IntegrationAuthorization extends TestBase {

    private static final String USER_ADMIN = "admin";
    private static final Logger LOGGER = getLogger(IntegrationAuthorization.class);
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
    public IntegrationAuthorization(@Optional("usernameXYZ123456") String username,
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
    public void t_createExperiments() {
        Experiment exp = postExperiment(experiment);
        Assert.assertNotNull(exp.creationTime, "Experiment creation failed (No creationTime).");
        Assert.assertNotNull(exp.modificationTime, "Experiment creation failed (No modificationTime).");
        Assert.assertNotNull(exp.state, "Experiment creation failed (No state).");
        experiment.update(exp);
    }

    /**
     * Test getting user permission
     */
    @Test(dependsOnMethods = {"t_createExperiments"})
    public void t_getUserPermissions() {
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
    @Test(dependsOnMethods = {"t_createExperiments"})
    public void t_getUserAppPermissions() {
        String uri = "authorization/users/" + userName + "/applications/" + experiment.applicationName + "/permissions";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, HttpStatus.SC_OK);
        @SuppressWarnings("rawtypes")
        HashMap jsonMap = (HashMap) response.jsonPath().get();
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
    @Test(dependsOnMethods = {"t_getUserPermissions"})
    public void t_getUserRoles() {

        String testRole = "superadmin";
        String uri = "authorization/roles/" + testRole + "/permissions";
        response = apiServerConnector.doGet(uri);

        assertReturnCode(response, HttpStatus.SC_OK);
        @SuppressWarnings("rawtypes")
        HashMap jsonMap = (HashMap) response.jsonPath().get();
        Assert.assertNotNull(jsonMap);
        Assert.assertTrue(jsonMap.toString().contains("SUPERADMIN"));
    }

    /**
     * Test getting application user roles
     */
    @Test(dependsOnMethods = {"t_getUserRoles"})
    public void t_getApplicationUsersByRole() {

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

//    /**
//     * Test deleting user role. This includes deleting of app_role as well.
//     */
//    @Test(dependsOnMethods = {"t_getApplicationUsersByRole"})
//    public void t_deleteUserRoles() {
//    	
//    	String uri = "authorization/applications/"+experiment.applicationName+"/users/"+username+"/roles";
//        response = apiServerConnector.doDelete(uri);
//        assertReturnCode(response, HttpStatus.SC_NO_CONTENT);
//    }  

    /**
     * Testing getting application user role again but this time it will return nothing because user role is deleted.
     */
    @Test(dependsOnMethods = {"t_getUserRoles"})
    public void t_getApplicationUsersByRoleAgain() {

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
    @Test(dependsOnMethods = {"t_getApplicationUsersByRoleAgain"})
    public void t_deleteExperiments() {
        deleteExperiment(experiment);
    }

    @Test(dependsOnGroups = {"ping"})
    public void t_addWasabiReaderToUserInfo() {
        addUserInfo("wasabi_reader", HttpStatus.SC_OK);
    }


    @Test(dependsOnMethods = {"t_addWasabiReaderToUserInfo"})
    public void t_addWasabiReaderAsSuperAdmin() {

        List<Map<String, Object>> superadminsBefore = getSuperAdmins(HttpStatus.SC_OK, apiServerConnector);

        boolean isWasabiSuperadmin = superadminsBefore.stream().map(map -> map.get(USER_USER_ID)).
                anyMatch(userId -> USER_READER.equals(userId.toString()));

        Assert.assertEquals(isWasabiSuperadmin, false);

        postSuperAdmin(USER_READER, HttpStatus.SC_NO_CONTENT, apiServerConnector);

        List<Map<String, Object>> superadminsAfterAdd = getSuperAdmins(HttpStatus.SC_OK, apiServerConnector);

        isWasabiSuperadmin = superadminsAfterAdd.stream().map(map -> map.get(USER_USER_ID)).
                anyMatch(userId -> USER_READER.equals(userId.toString()));

        Assert.assertEquals(isWasabiSuperadmin, true);

        Map<String, Object> wasabi = superadminsAfterAdd.stream().
                filter(map -> USER_READER.equals(map.get(USER_USER_ID))).findFirst().get();

        Assert.assertEquals(isWasabiSuperadmin, true);
        Assert.assertEquals(wasabi.get(USER_FIRST_NAME).toString(), WASABI_FIRST_NAME);
        Assert.assertEquals(wasabi.get(USER_LAST_NAME).toString(), WASABI_LAST_NAME);
        Assert.assertEquals(wasabi.get(USER_ROLE).toString(), "SUPERADMIN");
        Assert.assertEquals(wasabi.get(USER_EMAIL).toString(), WASABI_EMAIL);

        Assert.assertEquals(isWasabiSuperadmin, true);

    }

    @Test(dependsOnMethods = {"t_addWasabiReaderAsSuperAdmin"})
    public void t_removeWasabiReaderAsSuperAdmin() {

        List<Map<String, Object>> superadminsBefore = getSuperAdmins(HttpStatus.SC_OK, apiServerConnector);

        boolean isWasabiSuperadmin = superadminsBefore.stream().map(map -> map.get(USER_USER_ID)).
                anyMatch(userId -> USER_READER.equals(userId.toString()));

        Assert.assertEquals(isWasabiSuperadmin, true);
        deleteSuperAdmin(USER_READER, HttpStatus.SC_NO_CONTENT, apiServerConnector);

        List<Map<String, Object>> superadminsAfterDelete = getSuperAdmins(HttpStatus.SC_OK, apiServerConnector);

        isWasabiSuperadmin = superadminsAfterDelete.stream().map(map -> map.get(USER_USER_ID)).
                anyMatch(userId -> USER_READER.equals(userId.toString()));

        Assert.assertEquals(isWasabiSuperadmin, false);
    }

    /**
     * TODO: makes the assumption that "admin" is the last remaining superadmin. Integration tests need to be made
     *       independent of each other to produce this scenario consistently.
     */
    /**
    @Test(dependsOnMethods = {"t_removeWasabiReaderAsSuperAdmin"})
    public void t_removeLastSuperAdmin() {

        deleteSuperAdmin(USER_ADMIN, HttpStatus.SC_BAD_REQUEST, apiServerConnector);

    }*/
}
