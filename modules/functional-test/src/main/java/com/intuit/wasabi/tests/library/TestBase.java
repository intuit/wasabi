/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.tests.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryListener;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import com.intuit.wasabi.tests.model.APIUser;
import com.intuit.wasabi.tests.model.AccessToken;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.AssignmentStatus;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.Page;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.UserFeedback;
import com.intuit.wasabi.tests.model.UserRole;
import com.intuit.wasabi.tests.model.analytics.AnalyticsParameters;
import com.intuit.wasabi.tests.model.analytics.ExperimentCounts;
import com.intuit.wasabi.tests.model.analytics.ExperimentCumulativeCounts;
import com.intuit.wasabi.tests.model.analytics.ExperimentCumulativeStatistics;
import com.intuit.wasabi.tests.model.analytics.ExperimentStatistics;
import com.intuit.wasabi.tests.model.factory.APIUserFactory;
import com.intuit.wasabi.tests.model.factory.AccessTokenFactory;
import com.intuit.wasabi.tests.model.factory.AssignmentFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.PageFactory;
import com.intuit.wasabi.tests.model.factory.UserFeedbackFactory;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.exception.JsonPathException;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A TestBase for new test sets.
 */
@Listeners({ RetryListener.class })
public class TestBase extends ServiceTestBase {

    private static final Pattern PROPERTY_HEADERS_PATTERN = Pattern.compile("header\\.(.+)");

    private static final Logger LOGGER = getLogger(TestBase.class);
    private static boolean pingSuccess = false;
    protected Gson simpleGson;
    protected Properties appProperties;
    protected List<Experiment> toCleanUp = new ArrayList<>();

    /**
     * Creates a TestBase.
     */
    public TestBase() {
        init();
        simpleGson = new GsonBuilder().create();
    }

    /**
     * Initializes the TestBase.
     */
    private void init() {
        LOGGER.debug("Initializing TestBase");
        TestBase.pingSuccess = false;
    }

    /**
     * Will be called before any tests of a class are invoked. Creates an APIServerConnector and tries to ping the
     * service.
     *
     * @param configFile the configuration file
     * @throws IOException if the configfile can not be read.
     */
    @BeforeClass
    @Parameters({ "configFile" })
    protected void beforeClassTestWrapper(@Optional(Constants.DEFAULT_CONFIG_FILE) String configFile)
            throws IOException {
        LOGGER.debug(this.getClass().getName() + "@BeforeClass");

        loadProperties(configFile);

        createAPIServerConnector();

    }

    /**
     * Loads properties from the configfile, and after that from the System properties (also known as VM arguments).
     * That means the VM arguments take precedence.
     *
     * @param configFile the configuration file
     * @throws IOException if the file can not be read
     */
    private void loadProperties(String configFile) throws IOException {
        try {
            Properties properties = new Properties();
            properties.load(new BufferedReader(
                    new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(configFile))));
            appProperties = properties;
            LOGGER.debug("Properties loaded: " + appProperties.toString());
        } catch (IOException e) {
            LOGGER.error("Can not read config file " + configFile, e);
            throw e;
        }

        // System properties over config.properties
        setPropertyFromSystemProperty("api.server.protocol", "api-server-protocol");
        setPropertyFromSystemProperty("api.server.name", "api-server-name");
        setPropertyFromSystemProperty("api.version.string", "api-version-string");
        setPropertyFromSystemProperty("node.count", "node-count");
        // TODO It appears that the build system has user.name and pwd set to something different from what it should be
        // for the environment. Commented next two lines out for now.
        // setPropertyFromSystemProperty ("user.name","user-name");
        // setPropertyFromSystemProperty ("user.password","password");
        setPropertyFromSystemProperty("database-url", "database.url");
        setPropertyFromSystemProperty("database-username", "database.username");
        setPropertyFromSystemProperty("database-password", "database.password");

        setPropertyFromSystemProperty("user-name", "user-name");
        setPropertyFromSystemProperty("password", "password");
        setPropertyFromSystemProperty("user-lastname", "user-lastname");
        setPropertyFromSystemProperty("validTokenPattern", "validTokenPattern");
        setPropertyFromSystemProperty("user-email", "user-email");

        setPropertyFromSystemProperty("application.name", "application-name");
        setPropertyFromSystemProperty("experiment.prefix", "experiment-prefix");
        setPropertyFromSystemProperty("bucket.prefix", "bucket-prefix");

        setPropertyFromSystemProperty("test-user", "test-user");
    }

    /**
     * Will be called after all tests of a class are finished.
     */
    @AfterClass
    protected void afterClassTestWrapper() {
        LOGGER.debug(this.getClass().getName() + "@AfterClass");
    }

    /**
     * Creates an APIServerConnector.
     */
    private void createAPIServerConnector() {
        LOGGER.info("Creating APIServerConnector");

        // get values, resort to defaults if needed.
        String userName = appProperties.getProperty("user-name");
        String password = appProperties.getProperty("password");
        String apiServerProtocol = appProperties.getProperty("api-server-protocol",
                Constants.DEFAULT_CONFIG_SERVER_PROTOCOL);
        String apiServerName = appProperties.getProperty("api-server-name", Constants.DEFAULT_CONFIG_SERVER_NAME);
        String apiVersionString = appProperties.getProperty("api-version-string",
                Constants.DEFAULT_CONFIG_API_VERSION_STRING);

        Map<String, String> headerMap = new HashMap<>();
        //Any additional headers
        for (Enumeration<?> e = appProperties.propertyNames(); e.hasMoreElements(); ) {
            String propertyName = e.nextElement().toString();
            Matcher matcher = PROPERTY_HEADERS_PATTERN.matcher(propertyName);
            if (matcher.find() && matcher.groupCount() == 1) {
                String headerName = matcher.group(1);
                headerMap.put(headerName, appProperties.getProperty(propertyName));
            }
        }

        String baseUri = apiServerProtocol + "://" + apiServerName;
        String basePath = "/api/" + apiVersionString + "/";
        LOGGER.info("API base: " + baseUri + basePath);

        apiServerConnector = new APIServerConnector(baseUri, basePath, userName, password, headerMap);
    }

    /**
     * Sets an appProperty with the key {@code internalPropKey} to the value of the system property with the key
     * {@code sysPropKey}. If the system property is null or the empty string no action is done.
     *
     * @param sysPropKey the system property key
     * @param internalPropKey the appProperty key
     */
    protected void setPropertyFromSystemProperty(String sysPropKey, String internalPropKey) {
        String systemValueStr = System.getProperty(sysPropKey);
        if (systemValueStr != null && !systemValueStr.isEmpty()) {
            LOGGER.info("Setting property '" + internalPropKey + "' to: '" + systemValueStr
                    + "' based on system property '" + sysPropKey + "'");
            appProperties.setProperty(internalPropKey, systemValueStr);
        }
    }

    ///////////////////
    // ping Endpoint //
    ///////////////////

    /**
     * Pings the API server and asserts that all components in the received message are healthy, thus the system
     * running.
     * <p>
     * This will be run as a test to be able to use this as a dependency.
     */
    @Test(sequential = true, retryAnalyzer = RetryAnalyzer.class, groups = { "ping" })
    @RetryTest(maxTries = 5, warmup = 1000)
    public void assertPingAPIServer() {
        assertPingAPIServer(HttpStatus.SC_OK);
    }

    /**
     * Pings the API server and asserts that all components in the received message are healthy, thus the system
     * running.
     *
     * @param expectedStatus the expected HTTP status code
     */
    public void assertPingAPIServer(int expectedStatus) {
        assertPingAPIServer(expectedStatus, apiServerConnector);
    }

    /**
     * Pings the API server and asserts that all components in the received message are healthy, thus the system
     * running.
     *
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     */
    public void assertPingAPIServer(int expectedStatus, APIServerConnector apiServerConnector) {
        if (!TestBase.pingSuccess) {
            response = getPing(expectedStatus, apiServerConnector);

            // Right now ping is reporting on two components
            List<Map<String, Object>> list = response.jsonPath().get("componentHealths");
            Assert.assertTrue(list.size() >= 2,
                    "Health is reported for at least 2 components. In all: " + Integer.toString(list.size()));
            for (Map temp : list) {
                String k = (String) temp.get("componentName");
                Boolean v = (Boolean) temp.get("healthy");
                LOGGER.info("Ping reports: " + k + ": " + v);
                Assert.assertTrue(v, "Component " + k + "'s healthy value");
            }
            TestBase.pingSuccess = true;
        }
    }

    /**
     * Sends a GET request to ping the Server. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @return the response
     */
    public Response getPing() {
        return getPing(HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to ping the Server. The response must contain HTTP {@code expectedStatus}.
     *
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response getPing(int expectedStatus) {
        return getPing(expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to ping the Server. The response must contain HTTP {@code expectedStatus}.
     *
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response getPing(int expectedStatus, APIServerConnector apiServerConnector) {
        String url = "ping";
        response = apiServerConnector.doGet(url);
        assertReturnCode(response, expectedStatus);
        return response;
    }

    //////////////////////////
    // experiments Endpoint //
    //////////////////////////

    /**
     * Sends a POST request to create an experiment. The response must contain {@link HttpStatus#SC_CREATED}.
     * <p>
     * Sets createNewApplication to {@code true}.
     *
     * @param experiments the list of experiments to POST
     * @return the list of new experiments
     */
    public List<Experiment> postExperiments(List<Experiment> experiments) {
        List<Experiment> experimentsList = new ArrayList<Experiment>();
        for (Experiment exp : experiments)
            experimentsList.add(postExperiment(exp));
        return experimentsList;
    }

    /**
     * Sends a POST request to create an experiment. The response must contain {@link HttpStatus#SC_CREATED}.
     * <p>
     * Sets createNewApplication to {@code true}.
     *
     * @param experiment the experiment to POST
     * @return the new experiment
     */
    public Experiment postExperiment(Experiment experiment) {
        return postExperiment(experiment, HttpStatus.SC_CREATED);
    }

    /**
     * Sends a POST request to create an experiment. The response must contain {@link HttpStatus#SC_CREATED}.
     * <p>
     * Sets createNewApplication to {@code true}.
     *
     * @param experiment the experiment to POST
     * @param createNewApplication allow to create a new application
     * @return the new experiment
     */
    public Experiment postExperiment(Experiment experiment, boolean createNewApplication) {
        return postExperiment(experiment, createNewApplication, HttpStatus.SC_CREATED);
    }

    /**
     * Sends a POST request to create an experiment. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * Sets createNewApplication to {@code true}.
     *
     * @param experiment the experiment to POST
     * @param expectedStatus the expected HTTP status code
     * @return the new experiment
     */
    public Experiment postExperiment(Experiment experiment, int expectedStatus) {
        return postExperiment(experiment, true, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to create an experiment. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment to POST
     * @param createNewApplication allow to create a new application
     * @param expectedStatus the expected HTTP status code
     * @return the new experiment
     */
    public Experiment postExperiment(Experiment experiment, boolean createNewApplication, int expectedStatus) {
        return postExperiment(experiment, createNewApplication, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to create an experiment. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * Sets createNewApplication to {@code true}.
     *
     * @param experiment the experiment to POST
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the new experiment
     */
    public Experiment postExperiment(Experiment experiment, int expectedStatus, APIServerConnector apiServerConnector) {
        return postExperiment(experiment, true, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to create an experiment. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment to POST
     * @param createNewApplication allow to create a new application
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the new experiment
     */
    public Experiment postExperiment(Experiment experiment, boolean createNewApplication, int expectedStatus,
            APIServerConnector apiServerConnector) {
        response = apiServerConnector.doPost("experiments?createNewApplication=" + createNewApplication,
                experiment == null ? null : experiment.toJSONString());
        // FIXME: jwtodd
        assertReturnCode(response, response.getStatusCode() == 500 ? 500 : expectedStatus);
        return ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a PUT request to update the experiment. The response must contain {@link HttpStatus#SC_OK}.
     * <p>
     * Excludes the experiment creation and modification times.
     * <p>
     * If this call was used to delete an experiment (by changing the state to DELETED) it will return a new, empty
     * experiment.
     *
     * @param experiment the experiment to PUT
     * @return the new changed experiment
     */
    public Experiment putExperiment(Experiment experiment) {
        return putExperiment(experiment, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update the experiment. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * Excludes the experiment creation and modification times.
     * <p>
     * If this call was used to delete an experiment (by changing the state to DELETED) it will return a new, empty
     * experiment.
     *
     * @param experiment the experiment to PUT
     * @param expectedStatus the expected HTTP status code
     * @return the new changed experiment
     */
    public Experiment putExperiment(Experiment experiment, int expectedStatus) {
        return putExperiment(experiment, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update the experiment. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * Always excludes the experiment creation and modification times.
     * <p>
     * If this call was used to delete an experiment (by changing the state to DELETED) it will return a new, empty
     * experiment.
     *
     * @param experiment the experiment to PUT
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the new changed experiment
     */
    public Experiment putExperiment(Experiment experiment, int expectedStatus, APIServerConnector apiServerConnector) {
        String caseModification = "NO_ACTION";
        if (!experiment.getSerializationStrategy().exclude("modificationTime")) {
            if (experiment.getSerializationStrategy().contains("modificationTime")) { // whitelist strategy: remove it
                experiment.getSerializationStrategy().remove("modificationTime");
                caseModification = "WHITELIST_REMOVED";
            } else { // blacklist strategy: add it
                experiment.getSerializationStrategy().add("modificationTime");
                caseModification = "BLACKLIST_ADDED";
            }
        }
        String caseCreation = "NO_ACTION";
        if (!experiment.getSerializationStrategy().exclude("creationTime")) {
            if (experiment.getSerializationStrategy().contains("creationTime")) { // whitelist strategy: remove it
                experiment.getSerializationStrategy().remove("creationTime");
                caseCreation = "WHITELIST_REMOVED";
            } else { // blacklist strategy: add it
                experiment.getSerializationStrategy().add("creationTime");
                caseCreation = "BLACKLIST_ADDED";
            }
        }

        response = apiServerConnector.doPut("experiments/" + experiment.id, experiment.toJSONString());

        switch (caseModification) {
            case "WHITELIST_REMOVED":
                experiment.getSerializationStrategy().add("modificationTime");
                break;
            case "BLACKLIST_ADDED":
                experiment.getSerializationStrategy().remove("modificationTime");
                break;
        }
        switch (caseCreation) {
            case "WHITELIST_REMOVED":
                experiment.getSerializationStrategy().add("creationTime");
                break;
            case "BLACKLIST_ADDED":
                experiment.getSerializationStrategy().remove("creationTIme");
                break;
        }
        assertReturnCode(response, expectedStatus);
        if (expectedStatus == HttpStatus.SC_NO_CONTENT) {
            return new Experiment();
        }
        return ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
    }

    public void postSuperAdmin(String userId, int expectedStatus, APIServerConnector apiServerConnector) {

        response = apiServerConnector.doPost("authorization/superadmins/" + userId);
        assertReturnCode(response, expectedStatus);
    }

    public void addUserInfo(String userId, int expectedStatus) {

        response = apiServerConnector.doGet("authorization/users/" + userId + "/roles");
        assertReturnCode(response, expectedStatus);
    }

    public void deleteSuperAdmin(String userId, int expectedStatus, APIServerConnector apiServerConnector) {

        response = apiServerConnector.doDelete("authorization/superadmins/" + userId);
        assertReturnCode(response, expectedStatus);
    }

    public List<Map<String, Object>> getSuperAdmins(int expectedStatus, APIServerConnector apiServerConnector) {
        response = apiServerConnector.doGet("authorization/superadmins");

        LOGGER.debug("Response body: " + response.print());

        List<Map<String, Object>> superadmins = response.jsonPath().getList("");

        System.out.println(superadmins.size());

        assertReturnCode(response, expectedStatus);

        return superadmins;
    }

    /**
     * Sends a GET request to get all experiments. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @return a list of experiments
     */
    public List<Experiment> getExperiments() {
        return getExperiments(HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to get all experiments. The response must contain HTTP {@code expectedStatus}.
     *
     * @param expectedStatus the expected HTTP status code
     * @return a list of experiments
     */
    public List<Experiment> getExperiments(int expectedStatus) {
        return getExperiments(expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to get all experiments. The response must contain HTTP {@code expectedStatus}.
     *
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of experiments
     */
    public List<Experiment> getExperiments(int expectedStatus, APIServerConnector apiServerConnector) {
        response = apiServerConnector.doGet("experiments?per_page=-1");
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("experiments");
        List<Experiment> expList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            expList.add(ExperimentFactory.createFromJSONString(jsonString));
        }
        return expList;
    }

    /**
     * Sends a GET request to get an experiment with the supplied ID. The response must contain
     * {@link HttpStatus#SC_OK}.
     * <p>
     * Asserts that the experiment has an ID.
     *
     * @param experiment an experiment with an ID
     * @return a new experiment instance constructed from the response
     */
    public Experiment getExperiment(Experiment experiment) {
        return getExperiment(experiment, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to get an experiment with the supplied ID. The response must contain
     * {@link HttpStatus#SC_OK}.
     * <p>
     * Asserts that the experiment has an ID.
     *
     * @param experiment an experiment with an ID
     * @param expectedStatus the expected HTTP status code
     * @return a new experiment instance constructed from the response
     */
    public Experiment getExperiment(Experiment experiment, int expectedStatus) {
        return getExperiment(experiment, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to get an experiment with the supplied ID. The response must contain
     * {@link HttpStatus#SC_OK}.
     * <p>
     * Asserts that the experiment has an ID.
     *
     * @param experiment an experiment with an ID
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a new experiment instance constructed from the response
     */
    public Experiment getExperiment(Experiment experiment, int expectedStatus, APIServerConnector apiServerConnector) {
        Assert.assertNotNull(experiment.id);
        response = apiServerConnector.doGet("experiments/" + experiment.id);
        assertReturnCode(response, expectedStatus);
        return ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a DELETE request for the experiment with the experiment's ID. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     * <p>
     * Asserts that the experiment has an ID.
     *
     * @param experiment an experiment with an ID
     * @return the response
     */
    public Response deleteExperiment(Experiment experiment) {
        return deleteExperiment(experiment, HttpStatus.SC_NO_CONTENT);
    }

    public void deleteExperiments(List<Experiment> experiments) {
        for (Experiment exp : experiments) {
            deleteExperiment(exp);
        }
    }

    /**
     * Sends a DELETE request for the experiment with the experiment's ID. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment an experiment with an ID
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response deleteExperiment(Experiment experiment, int expectedStatus) {
        return deleteExperiment(experiment, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a DELETE request for the experiment with the experiment's ID. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment an experiment with an ID
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response deleteExperiment(Experiment experiment, int expectedStatus, APIServerConnector apiServerConnector) {
        Assert.assertNotNull(experiment.id, "The passed experiment has no ID.");
        response = apiServerConnector.doDelete("experiments/" + experiment.id);
        assertReturnCode(response, expectedStatus);
        return response;
    }

    ///////////////////////////////////////
    // experiments/<id>/buckets endpoint //
    ///////////////////////////////////////

    /**
     * Sends a POST request to create a bucket. The response must contain {@link HttpStatus#SC_CREATED}.
     *
     * @param bucket the new bucket
     * @return the retrieved bucket
     */
    public Bucket postBucket(Bucket bucket) {
        return postBucket(bucket, HttpStatus.SC_CREATED);
    }

    /**
     * Sends a POST request to create a bucket. The response must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the new bucket
     * @param expectedStatus the expected HTTP status code
     * @return the retrieved bucket
     */
    public Bucket postBucket(Bucket bucket, int expectedStatus) {
        return postBucket(bucket, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to create a bucket. The response must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the new bucket
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the retrieved bucket
     */
    public Bucket postBucket(Bucket bucket, int expectedStatus, APIServerConnector apiServerConnector) {
        Assert.assertNotNull(bucket.experimentID, "The experiment ID must not be 'null'.");
        String uri = "experiments/" + bucket.experimentID + "/buckets";
        response = apiServerConnector.doPost(uri, bucket.toJSONString());
        assertReturnCode(response, expectedStatus);
        return BucketFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends multiple POST requests to create multiple buckets. The response must contain {@link HttpStatus#SC_CREATED}.
     *
     * @param buckets a list of new buckets
     * @return a list of retrieved buckets
     */
    public List<Bucket> postBuckets(List<Bucket> buckets) {
        return postBuckets(buckets, HttpStatus.SC_CREATED);
    }

    /**
     * Sends multiple POST requests to create multiple buckets. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param buckets a list of new buckets
     * @param expectedStatus the expected HTTP status code
     * @return a list of retrieved buckets
     */
    public List<Bucket> postBuckets(List<Bucket> buckets, int expectedStatus) {
        return postBuckets(buckets, expectedStatus, apiServerConnector);
    }

    /**
     * Sends multiple POST requests to create multiple buckets. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param buckets a list of new buckets
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of retrieved buckets
     */
    public List<Bucket> postBuckets(List<Bucket> buckets, int expectedStatus, APIServerConnector apiServerConnector) {
        List<Bucket> newBuckets = new ArrayList<>(buckets.size());
        for (Bucket bucket : buckets) {
            newBuckets.add(postBucket(bucket, expectedStatus, apiServerConnector));
        }
        return newBuckets;
    }

    /**
     * Sends a PUT request to update a bucket. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param bucket the bucket to update
     * @return the retrieved bucket
     */
    public Bucket putBucket(Bucket bucket) {
        return putBucket(bucket, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update a bucket. The response must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the bucket to update
     * @param expectedStatus the expected HTTP status code
     * @return the retrieved bucket
     */
    public Bucket putBucket(Bucket bucket, int expectedStatus) {
        return putBucket(bucket, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update a bucket. The response must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the bucket to update
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the retrieved bucket
     */
    public Bucket putBucket(Bucket bucket, int expectedStatus, APIServerConnector apiServerConnector) {
        Assert.assertNotNull(bucket.experimentID, "The experiment ID must not be 'null'.");
        Assert.assertNotNull(bucket.label, "The bucket label must not be 'null'.");
        String uri = "experiments/" + bucket.experimentID + "/buckets/" + bucket.label;
        response = apiServerConnector.doPut(uri, bucket.toJSONString());
        assertReturnCode(response, expectedStatus);
        return BucketFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a PUT request to update a list of buckets. The responses must contain {@link HttpStatus#SC_OK}.
     *
     * @param buckets the buckets to update
     * @return the retrieved buckets
     */
    public List<Bucket> putBuckets(List<Bucket> buckets) {
        return putBuckets(buckets, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update a list of buckets. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param buckets the buckets to update
     * @param expectedStatus the expected HTTP status code
     * @return the retrieved buckets
     */
    public List<Bucket> putBuckets(List<Bucket> buckets, int expectedStatus) {
        return putBuckets(buckets, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update a list of buckets. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param buckets the buckets to update
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the retrieved buckets
     */
    public List<Bucket> putBuckets(List<Bucket> buckets, int expectedStatus, APIServerConnector apiServerConnector) {
        List<Bucket> newBuckets = new ArrayList<>(buckets.size());
        for (Bucket bucket : buckets) {
            newBuckets.add(putBucket(bucket, expectedStatus, apiServerConnector));
        }
        return newBuckets;
    }

    /**
     * Sends a PUT request to update the state of a bucket. The responses must contain {@link HttpStatus#SC_OK}.
     * <p>
     * Uses the bucket's state.
     *
     * @param bucket the buckets to update
     * @return the bucket returned by the response
     */
    public Bucket putBucketState(Bucket bucket) {
        return putBucketState(bucket, bucket.state);
    }

    /**
     * Sends a PUT request to update the state of a bucket. The responses must contain {@link HttpStatus#SC_OK}.
     *
     * @param bucket the buckets to update
     * @param state the new state
     * @return the bucket returned by the response
     */
    public Bucket putBucketState(Bucket bucket, String state) {
        return putBucketState(bucket, state, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update the state of a bucket. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the buckets to update
     * @param state the new state
     * @param expectedStatus the expected HTTP status code
     * @return the bucket returned by the response
     */
    public Bucket putBucketState(Bucket bucket, String state, int expectedStatus) {
        return putBucketState(bucket, state, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update the state of a bucket. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the buckets to update
     * @param state the new state
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the bucket returned by the response
     */
    public Bucket putBucketState(Bucket bucket, String state, int expectedStatus,
            APIServerConnector apiServerConnector) {
        Assert.assertNotNull(bucket.experimentID, "The experiment ID must not be 'null'.");
        Assert.assertNotNull(bucket.label, "The bucket label must not be 'null'.");
        String uri = "experiments/" + bucket.experimentID + "/buckets/" + bucket.label + "/state/" + state;
        response = apiServerConnector.doPut(uri);
        assertReturnCode(response, expectedStatus);
        String jsonString = response.asString();
        return BucketFactory.createFromJSONString(jsonString);
    }

    /**
     * Sends a PUT request to update the states of a list of buckets. The responses must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param buckets the buckets to update
     * @return a list retrieved buckets
     */
    public List<Bucket> putBucketsStates(List<Bucket> buckets) {
        return putBucketsStates(buckets, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update the states of a list of buckets. The responses must contain HTTP
     * {@code expectedStatus}.
     *
     * @param buckets the buckets to update
     * @param expectedStatus the expected HTTP status code
     * @return a list retrieved buckets
     */
    public List<Bucket> putBucketsStates(List<Bucket> buckets, int expectedStatus) {
        return putBucketsStates(buckets, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update the states of a list of buckets. The responses must contain HTTP
     * {@code expectedStatus}.
     *
     * @param buckets the buckets to update
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list retrieved buckets
     */
    public List<Bucket> putBucketsStates(List<Bucket> buckets, int expectedStatus,
            APIServerConnector apiServerConnector) {
        List<String> bucketStates = new ArrayList<>(buckets.size());
        for (Bucket bucket : buckets) {
            bucketStates.add(bucket.state);
        }
        return putBucketsStates(buckets, bucketStates, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update the states of a list of buckets. The responses must contain
     * {@link HttpStatus#SC_OK}. All states are set to the same supplied value.
     *
     * @param buckets the buckets to update
     * @param state the states to be set, one for all buckets
     * @return a list retrieved buckets
     */
    public List<Bucket> putBucketsState(List<Bucket> buckets, String state) {
        return putBucketsState(buckets, state, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update the states of a list of buckets. The responses must contain HTTP
     * {@code expectedStatus}. All states are set to the same supplied value.
     *
     * @param buckets the buckets to update
     * @param state the states to be set, one for all buckets
     * @param expectedStatus the expected http status code
     * @return a list retrieved buckets
     */
    public List<Bucket> putBucketsState(List<Bucket> buckets, String state, int expectedStatus) {
        String[] bucketStates = new String[buckets.size()];
        Arrays.fill(bucketStates, state);
        return putBucketsStates(buckets, Arrays.asList(bucketStates), expectedStatus);
    }

    /**
     * Sends a PUT request to update the states of a list of buckets. The responses must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param buckets the buckets to update
     * @param bucketStates the states to be set, one for each bucket
     * @return a list retrieved buckets
     */
    public List<Bucket> putBucketsStates(List<Bucket> buckets, List<String> bucketStates) {
        return putBucketsStates(buckets, bucketStates, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update the states of a list of buckets. The responses must contain HTTP
     * {@code expectedStatus}. All states are set to the same value.
     *
     * @param buckets the buckets to update
     * @param bucketStates the states to be set, one for each bucket
     * @param expectedStatus the expected HTTP status code
     * @return a list retrieved buckets
     */
    public List<Bucket> putBucketsStates(List<Bucket> buckets, List<String> bucketStates, int expectedStatus) {
        return putBucketsStates(buckets, bucketStates, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update the states of a list of buckets. The responses must contain HTTP
     * {@code expectedStatus}. All states are set to the same value.
     *
     * @param buckets the buckets to update
     * @param bucketStates the states to be set, one for each bucket
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list retrieved buckets
     */
    public List<Bucket> putBucketsStates(List<Bucket> buckets, List<String> bucketStates, int expectedStatus,
            APIServerConnector apiServerConnector) {
        Assert.assertEquals(buckets.size(), bucketStates.size(),
                buckets.size() + " buckets, but " + bucketStates.size() + " states.");
        List<Bucket> newBuckets = new ArrayList<>(buckets.size());
        for (int i = 0; i < buckets.size(); ++i) {
            newBuckets.add(putBucketState(buckets.get(i), bucketStates.get(i), expectedStatus, apiServerConnector));
        }
        return newBuckets;
    }

    /**
     * Sends a DELETE request for a bucket. The response must contain {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param bucket the bucket to delete
     * @return the response
     */
    public Response deleteBucket(Bucket bucket) {
        return deleteBucket(bucket, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a DELETE request for a bucket. The response must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the bucket to delete
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response deleteBucket(Bucket bucket, int expectedStatus) {
        return deleteBucket(bucket, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a DELETE request for a bucket. The response must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the bucket to delete
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response deleteBucket(Bucket bucket, int expectedStatus, APIServerConnector apiServerConnector) {
        Assert.assertNotNull(bucket.experimentID, "The experiment ID must not be 'null'.");
        Assert.assertNotNull(bucket.label, "The bucket label must not be 'null'.");
        String uri = "experiments/" + bucket.experimentID + "/buckets/" + bucket.label;
        response = apiServerConnector.doDelete(uri);
        assertReturnCode(response, expectedStatus);
        return response;
    }

    /**
     * Sends DELETE requests for a list of buckets. The responses must contain {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param buckets the buckets to delete
     * @return the response
     */
    public Response deleteBuckets(List<Bucket> buckets) {
        return deleteBuckets(buckets, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends DELETE requests for a list of buckets. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param buckets the buckets to delete
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response deleteBuckets(List<Bucket> buckets, int expectedStatus) {
        return deleteBuckets(buckets, expectedStatus, apiServerConnector);
    }

    /**
     * Sends DELETE requests for a list of buckets. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param buckets the buckets to delete
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response deleteBuckets(List<Bucket> buckets, int expectedStatus, APIServerConnector apiServerConnector) {
        for (Bucket bucket : buckets) {
            response = deleteBucket(bucket, expectedStatus, apiServerConnector);
        }
        return response;
    }

    /**
     * Sends a GET request to retrieve the buckets of the specified experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment providing the id
     * @return the response
     */
    public List<Bucket> getBuckets(Experiment experiment) {
        return getBuckets(experiment, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve the buckets of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment providing the id
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public List<Bucket> getBuckets(Experiment experiment, int expectedStatus) {
        return getBuckets(experiment, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve the buckets of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment providing the id
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public List<Bucket> getBuckets(Experiment experiment, int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/buckets";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("buckets");
        List<Bucket> bucketList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            bucketList.add(BucketFactory.createFromJSONString(jsonString));
        }
        return bucketList;
    }

    /**
     * Sends a GET request to retrieve the specified bucket. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param bucket the bucket providing a label and an experiment id
     * @return the bucket
     */
    public Bucket getBucket(Bucket bucket) {
        return getBucket(bucket, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve the specified bucket. The response must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the bucket providing a label and an experiment id
     * @param expectedStatus the expected HTTP status code
     * @return the bucket
     */
    public Bucket getBucket(Bucket bucket, int expectedStatus) {
        return getBucket(bucket, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve the specified bucket. The response must contain HTTP {@code expectedStatus}.
     *
     * @param bucket the bucket providing a label and an experiment id
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the bucket
     */
    public Bucket getBucket(Bucket bucket, int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "experiments/" + bucket.experimentID + "/buckets/" + bucket.label;
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        Map<String, Object> jsonMap = response.jsonPath().get("");
        String jsonString = simpleGson.toJson(jsonMap);
        return BucketFactory.createFromJSONString(jsonString);
    }

    /**
     * Sends GET requests to retrieve the specified buckets. The responses must contain {@link HttpStatus#SC_OK}.
     *
     * @param buckets the buckets list
     * @return the new list
     */
    public List<Bucket> getBuckets(List<Bucket> buckets) {
        return getBuckets(buckets, HttpStatus.SC_OK);
    }

    /**
     * Sends GET requests to retrieve the specified buckets. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param buckets the reference bucket list
     * @param expectedStatus the expected HTTP status code
     * @return the new bucket list
     */
    public List<Bucket> getBuckets(List<Bucket> buckets, int expectedStatus) {
        return getBuckets(buckets, expectedStatus, apiServerConnector);
    }

    /**
     * Sends GET requests to retrieve the specified buckets. The responses must contain HTTP {@code expectedStatus}.
     *
     * @param buckets the reference bucket list
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the new bucket list
     */
    public List<Bucket> getBuckets(List<Bucket> buckets, int expectedStatus, APIServerConnector apiServerConnector) {
        List<Bucket> bucketList = new ArrayList<>(buckets.size());
        for (Bucket bucket : buckets) {
            bucketList.add(getBucket(bucket, expectedStatus, apiServerConnector));
        }
        return bucketList;
    }

    //////////////////////////////////////
    // experiments/<id>/events Endpoint //
    //////////////////////////////////////

    /**
     * Sends a GET request to retrieve the events of the specified experiment. The response must contain
     * {@link HttpStatus#SC_OK}. Note that the GET request is limited to the most recent 75 events. For more events, see
     * {@link #postEvents(Experiment)} which is more flexible.
     *
     * @param experiment the reference experiment
     * @return the event list
     */
    public List<Event> getEvents(Experiment experiment) {
        return getEvents(experiment, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve the events of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}. Note that the GET request is limited to the most recent 75 events. For more events, see
     * {@link #postEvents(Experiment, int)} which is more flexible.
     *
     * @param experiment the reference experiment
     * @param expectedStatus the expected HTTP status code
     * @return the event list
     */
    public List<Event> getEvents(Experiment experiment, int expectedStatus) {
        return getEvents(experiment, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve the events of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}. Note that the GET request is limited to the most recent 75 events. For more events, see
     * {@link #postEvents(Experiment, int, APIServerConnector)} which is more flexible.
     *
     * @param experiment the reference experiment
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the event list
     */
    @SuppressWarnings("unchecked")
    public List<Event> getEvents(Experiment experiment, int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/events";

        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);

        String json = TestUtils.csvToJsonArray(response.body().asString(), Constants.TAB);
        List<Map<String, Object>> eventMapList = simpleGson.fromJson(json, List.class);
        List<Event> eventList = new ArrayList<>(eventMapList.size());
        for (Map m : eventMapList) {
            eventList.add(EventFactory.createFromJSONString(simpleGson.toJson(m)));
        }
        return eventList;

    }

    /**
     * Sends a POST request to retrieve the events of the specified experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the reference experiment
     * @return the event list
     */
    public List<Event> postEvents(Experiment experiment) {
        return postEvents(experiment, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to retrieve the events of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the reference experiment
     * @param expectedStatus the expected HTTP status code
     * @return the event list
     */
    public List<Event> postEvents(Experiment experiment, int expectedStatus) {
        return postEvents(experiment, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to retrieve the events of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the reference experiment
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the event list
     */
    public List<Event> postEvents(Experiment experiment, int expectedStatus, APIServerConnector apiServerConnector) {
        return postEvents(experiment, null, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to retrieve the events of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * The parameter keys should be some of
     * {@code fromTime, toTime, confidenceLevel, effectSize, actions, singleShot, metric, mode} and {@code context}. For
     * more information see
     * {@link #postEvents(Experiment, String, String, double, double, ArrayList, boolean, String, String, String, int, APIServerConnector)}.
     * <p>
     * Invalid keys are removed from the parameters map.
     *
     * @param experiment the reference experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the event list
     */
    public List<Event> postEvents(Experiment experiment, Map<String, Object> parameters, int expectedStatus,
            APIServerConnector apiServerConnector) {
        return postEvents(experiment, parameters, false, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to retrieve the events of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * The parameter keys should be some of
     * {@code fromTime, toTime, confidenceLevel, effectSize, actions, singleShot, metric, mode} and {@code context}. For
     * more information see
     * {@link #postEvents(Experiment, String, String, double, double, ArrayList, boolean, String, String, String, int, APIServerConnector)}.
     *
     * @param experiment the reference experiment
     * @param parameters the parameters for the request body, can be null
     * @param keepInvalidKeys if true, invalid keys are also transmitted; ignored if parameters == null
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the event list
     */
    @SuppressWarnings("unchecked")
    public List<Event> postEvents(Experiment experiment, Map<String, Object> parameters, boolean keepInvalidKeys,
            int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/events";

        if (parameters != null) {

            Map<String, String> keyMapping = new HashMap<>();
            keyMapping.put("confidenceLevel", "DOUBLE");
            keyMapping.put("effectSize", "DOUBLE");
            keyMapping.put("fromTime", "STRING");
            keyMapping.put("toTime", "STRING");
            keyMapping.put("metric", "STRING");
            keyMapping.put("mode", "STRING");
            keyMapping.put("context", "STRING");
            keyMapping.put("actions", "LIST");
            keyMapping.put("singleShot", "BOOLEAN");

            for (String key : parameters.keySet()) {
                switch (keyMapping.get(key) == null ? "INVALID" : keyMapping.get(key)) {
                    case "DOUBLE":
                        if (!(parameters.get(key) instanceof Double) || (parameters.get(key)).equals(Double.NaN)) {
                            parameters.remove(key);
                        }
                        break;
                    case "STRING":
                        if (parameters.get(key) == null) {
                            parameters.remove(key);
                        }
                        break;
                    case "LIST":
                        try {
                            if (parameters.get(key) == null || !(parameters.get(key) instanceof List)
                                    || ((List) parameters.get(key)).size() == 0) {
                                parameters.remove(key);
                            }
                        } catch (ClassCastException ex) { // should never happen...
                            LOGGER.error("Invalid value, object of key " + key + " is no LIST. Removing it.", ex);
                            parameters.remove(key);
                        }
                        break;
                    case "BOOLEAN":
                        if (!(parameters.get(key) instanceof Boolean) || !(Boolean) parameters.get(key)) {
                            parameters.remove(key);
                        }
                        break;
                    case "INVALID":
                        if (!keepInvalidKeys) {
                            parameters.remove(key);
                        }
                        break;
                }
            }
            String parameterJson = simpleGson.toJson(parameters);
            response = apiServerConnector.doPost(uri, parameterJson);

        } else {
            // needs an empty JSON object as a request body, maybe it would be a good idea to remove that dependency?
            response = apiServerConnector.doPost(uri, "{}");

        }

        assertReturnCode(response, expectedStatus);

        String json = TestUtils.csvToJsonArray(response.body().asString(), Constants.TAB);
        List<Map<String, Object>> eventMapList = simpleGson.fromJson(json, List.class);
        List<Event> eventList = new ArrayList<>(eventMapList.size());
        for (Map m : eventMapList) {
            eventList.add(EventFactory.createFromJSONString(simpleGson.toJson(m)));
        }
        return eventList;
    }

    /**
     * Sends a POST request to retrieve the events of the specified experiment. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * The parameters {@code fromTime, toTime, confidenceLevel, effectSize, actions, singleShot, metric, mode} and
     * {@code context} are optional. If they are {@code null} (Strings) or {@link Double#NaN} (doubles), they will be
     * ignored (note that {@code singleShot} is {@code boolean}, it will be ignored if false).
     * <p>
     * Legal values are:
     * <p>
     * <dl>
     * <dt>{@code fromTime}</dt>
     * <dd>{@code null} or String, see {@link TestUtils#getTimeString(Calendar)}</dd>
     * <dt>{@code toTime}</dt>
     * <dd>{@code null} or String, see {@link TestUtils#getTimeString(Calendar)}</dd>
     * <dt>{@code confidenceLevel}</dt>
     * <dd>{@code 0 < confidenceLevel < 1}</dd>
     * <dt>{@code effectSize}</dt>
     * <dd>{@code -1 &le; effectSize &le; 1}</dd>
     * <dt>{@code actions}</dt>
     * <dd>{@code null} or an empty list or Action identifiers, will be ignored on {@code null} and empty</dd>
     * <dt>{@code singleShot}</dt>
     * <dd>{@code true/false}, ignored on {@code false}</dd>
     * <dt>{@code metric}</dt>
     * <dd>{@code NORMAL_APPROX} or {@code NORMAL_APPROX_SYM}</dd>
     * <dt>{@code mode}</dt>
     * <dd>{@code PRODUCTION} or {@code TEST}, should not be used according to swagger</dd>
     * <dt>{@code context}</dt>
     * <dd>any value, ignored on {@code null}</dd>
     * </dl>
     *
     * @param experiment the reference experiment
     * @param fromTime the start time
     * @param toTime the end time
     * @param confidenceLevel the confidence level
     * @param effectSize the effect size
     * @param actions the actions to be fetched
     * @param singleShot single shot value
     * @param metric the metric to be used
     * @param mode the mode
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the event list
     */
    public List<Event> postEvents(Experiment experiment, String fromTime, String toTime, double confidenceLevel,
            double effectSize, ArrayList<String> actions, boolean singleShot, String metric, String mode,
            String context, int expectedStatus, APIServerConnector apiServerConnector) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fromTime", fromTime);
        parameters.put("toTime", toTime);
        parameters.put("confidenceLevel", confidenceLevel);
        parameters.put("effectSize", effectSize);
        parameters.put("actions", actions);
        parameters.put("singleShot", singleShot);
        parameters.put("metric", metric);
        parameters.put("mode", mode);
        parameters.put("context", context);
        return postEvents(experiment, parameters, expectedStatus, apiServerConnector);
    }

    //////////////////////////////////////////
    // experiments/<id>/exclusions Endpoint //
    //////////////////////////////////////////

    /**
     * Sends a POST request to mutually exclude experiments from each other. The response must contain
     * {@link HttpStatus#SC_CREATED}.
     *
     * @param experiment the reference experiment
     * @param excludedExperiments the list of mutual exclusive elements
     * @return the response
     */
    public Response postExclusions(Experiment experiment, List<Experiment> excludedExperiments) {
        return postExclusions(experiment, excludedExperiments, HttpStatus.SC_CREATED);
    }

    /**
     * Sends a POST request to mutually exclude experiments from each other. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the reference experiment
     * @param excludedExperiments the list of mutual exclusive elements
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postExclusions(Experiment experiment, List<Experiment> excludedExperiments, int expectedStatus) {
        return postExclusions(experiment, excludedExperiments, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to mutually exclude experiments from the reference experiment. The response must contain
     * HTTP {@code expectedStatus}.
     * <p>
     * Lets assume you would pass experiment 1 as the experiment and 2 and 3 as excluded experiments. Then 1 and 2 would
     * be mutually exclusive as well as 1 and 3, however 2 and 3 would not.
     * <p>
     * To making all experiments mutual exclusive with each other please use {@link #postExclusions(List)}.
     *
     * @param experiment the reference experiment
     * @param excludedExperiments the list of mutual exclusive elements
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response postExclusions(Experiment experiment, List<Experiment> excludedExperiments, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/exclusions";
        List<String> excludeIds = new ArrayList<>(excludedExperiments.size());
        excludeIds.addAll(excludedExperiments.stream().map(exp -> exp.id).collect(Collectors.toList()));
        response = apiServerConnector.doPost(uri,
                TestUtils.wrapJsonIntoObject(simpleGson.toJson(excludeIds), "experimentIDs"));
        assertReturnCode(response, expectedStatus);
        return response;
    }

    /**
     * Sends a POST request to mutually exclude experiments from each other. The response must contain
     * {@link HttpStatus#SC_CREATED}.
     * <p>
     * Note that only the last response is returned, but all are checked.
     *
     * @param excludedExperiments the list of mutual exclusive elements
     * @return the response
     */
    public Response postExclusions(List<Experiment> excludedExperiments) {
        return postExclusions(excludedExperiments, HttpStatus.SC_CREATED);
    }

    /**
     * Sends a POST request to mutually exclude experiments from each other. The response must contain
     * {@link HttpStatus#SC_CREATED}.
     * <p>
     * Note that only the last response is returned, but all are checked.
     *
     * @param excludedExperiments the list of mutual exclusive elements
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postExclusions(List<Experiment> excludedExperiments, int expectedStatus) {
        return postExclusions(excludedExperiments, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to mutually exclude experiments from each other. The response must contain
     * {@link HttpStatus#SC_CREATED}.
     * <p>
     * Note that only the last response is returned, but all are checked.
     *
     * @param excludedExperiments the list of mutual exclusive elements
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response postExclusions(List<Experiment> excludedExperiments, int expectedStatus,
            APIServerConnector apiServerConnector) {
        for (int i = 0; i < excludedExperiments.size() - 1; ++i) {
            response = postExclusions(excludedExperiments.get(i),
                    excludedExperiments.subList(i + 1, excludedExperiments.size()), expectedStatus, apiServerConnector);
        }
        return response;
    }

    /**
     * Sends a GET request to retrieve experiments mutually excluded from the supplied experiment. The response must
     * contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the reference experiment
     * @return a list of experiments mutually exclusive to the supplied one
     */
    public List<Experiment> getExclusions(Experiment experiment) {
        return getExclusions(experiment, true);
    }

    /**
     * Sends a GET request to retrieve experiments mutually excluded from the supplied experiment. The response must
     * contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the reference experiment
     * @param showAll sets the showAll parameter, default: true
     * @return a list of experiments mutually exclusive to the supplied one
     */
    public List<Experiment> getExclusions(Experiment experiment, boolean showAll) {
        return getExclusions(experiment, showAll, true);
    }

    /**
     * Sends a GET request to retrieve experiments mutually excluded from the supplied experiment. The response must
     * contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the reference experiment
     * @param showAll sets the showAll parameter, default: true
     * @param exclusive sets the excluse parameter, default: true
     * @return a list of experiments mutually exclusive to the supplied one
     */
    public List<Experiment> getExclusions(Experiment experiment, boolean showAll, boolean exclusive) {
        return getExclusions(experiment, showAll, exclusive, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve experiments mutually excluded from the supplied experiment. The response must
     * contain HTTP {@code expectedStatus}.
     *
     * @param experiment the reference experiment
     * @param showAll sets the showAll parameter, default: true
     * @param exclusive sets the excluse parameter, default: true
     * @param expectedStatus the expected HTTP status code
     * @return a list of experiments mutually exclusive to the supplied one
     */
    public List<Experiment> getExclusions(Experiment experiment, boolean showAll, boolean exclusive,
            int expectedStatus) {
        return getExclusions(experiment, showAll, exclusive, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve experiments mutually excluded from the supplied experiment. The response must
     * contain HTTP {@code expectedStatus}.
     *
     * @param experiment the reference experiment
     * @param showAll sets the showAll parameter, default: true
     * @param exclusive sets the excluse parameter, default: true
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of experiments mutually exclusive to the supplied one
     */
    public List<Experiment> getExclusions(Experiment experiment, boolean showAll, boolean exclusive, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/exclusions";
        if (!showAll || !exclusive) {
            uri += "?";
            if (!showAll) {
                uri += "showAll=false&";
            }
            if (!exclusive) {
                uri += "exclusive=false&";
            }
            uri = uri.substring(0, uri.length() - 1);
        }
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonResult = response.jsonPath().getList("experiments");
        List<Experiment> experiments = new ArrayList<>();
        for (Map jsonMap : jsonResult) {
            experiments.add(ExperimentFactory.createFromJSONString(simpleGson.toJson(jsonMap)));
        }
        return experiments;
    }

    /**
     * Sends a DELETE request to remove the mutual exclusion of two experiments. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param experiment one experiment
     * @param experimentOther another experiment
     * @return the response
     */
    public Response deleteExclusion(Experiment experiment, Experiment experimentOther) {
        return deleteExclusion(experiment, experimentOther, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a DELETE request to remove the mutual exclusion of two experiments. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment one experiment
     * @param experimentOther another experiment
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response deleteExclusion(Experiment experiment, Experiment experimentOther, int expectedStatus) {
        return deleteExclusion(experiment, experimentOther, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a DELETE request to remove the mutual exclusion of two experiments. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment one experiment
     * @param experimentOther another experiment
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response deleteExclusion(Experiment experiment, Experiment experimentOther, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/exclusions/experiment1/" + experiment.id + "/experiment2/" + experimentOther.id;
        response = apiServerConnector.doDelete(uri);
        assertReturnCode(response, expectedStatus);
        return response;
    }

    ////////////////////////////////////////
    // experiments/<id>/priority Endpoint //
    ////////////////////////////////////////

    /**
     * Sends a POST request to set the experiment's priority. The response must contain {@link HttpStatus#SC_CREATED}.
     *
     * @param experiment the experiment
     * @param priority the experiment priority
     * @return the response
     */
    public Response postExperimentPriority(Experiment experiment, int priority) {
        return postExperimentPriority(experiment, priority, HttpStatus.SC_CREATED);
    }

    /**
     * Sends a POST request to set the experiment's priority. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param priority the experiment priority
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postExperimentPriority(Experiment experiment, int priority, int expectedStatus) {
        return postExperimentPriority(experiment, priority, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to set the experiment's priority. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param priority the experiment priority
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response postExperimentPriority(Experiment experiment, int priority, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/priority/" + priority;
        response = apiServerConnector.doPost(uri);
        assertReturnCode(response, expectedStatus);
        return response;
    }

    ///////////////////////////////////////////
    // experiments/<id>/assignments Endpoint //
    ///////////////////////////////////////////

    /**
     * Sends a GET request to retrieve the experiment's assignments. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @return a list of assignments
     */
    public List<Assignment> getAssignments(Experiment experiment) {
        return getAssignments(experiment, null);
    }

    /**
     * Sends a GET request to retrieve the experiment's assignments. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param expectedStatus the expected HTTP status code
     * @return a list of assignments
     */
    public List<Assignment> getAssignments(Experiment experiment, int expectedStatus) {
        return getAssignments(experiment, null, expectedStatus);
    }

    /**
     * Sends a GET request to retrieve the experiment's assignments. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param context the experiment context (can be null)
     * @return a list of assignments
     */
    public List<Assignment> getAssignments(Experiment experiment, String context) {
        return getAssignments(experiment, context, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve the experiment's assignments. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the experiment context (can be null)
     * @param expectedStatus the expected HTTP status code
     * @return a list of assignments
     */
    public List<Assignment> getAssignments(Experiment experiment, String context, int expectedStatus) {
        return getAssignments(experiment, context, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve the experiment's assignments. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the experiment context (can be null)
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of assignments
     */
    public List<Assignment> getAssignments(Experiment experiment, String context, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/assignments";
        if (context != null) {
            context = TestUtils.wrapJsonIntoObject(context, "context");
        }
        response = apiServerConnector.doGet(uri, context);
        assertReturnCode(response, expectedStatus);

        if (expectedStatus == HttpStatus.SC_NOT_FOUND)
            return new ArrayList<Assignment>();

        String jsonArray = TestUtils.csvToJsonArray(response.body().asString(), Constants.TAB);
        String[] elements = jsonArray.substring(2, jsonArray.length() - 2).split("\\},\\{");
        List<Assignment> assignments = new ArrayList<>();
        for (String json : elements) {
            assignments.add(simpleGson.fromJson("{" + json + "}", Assignment.class));
        }
        return assignments;
    }

    /**
     * Sends a GET request to retrieve the experiment's assignment traffic. The response must contain
     * {@link HttpStatus#SC_OK}.
     * <p>
     * URLEncodes the generated Strings generated from the dates which are passed as the URL.
     *
     * @param experiment the experiment
     * @param from the first day to retrieve
     * @param to the last day to retrieve
     * @return a map of lists (= table) containing meta and assignment traffic data
     */
    protected Map<String, List<?>> getTraffic(Experiment experiment, LocalDateTime from, LocalDateTime to) {
        return getTraffic(experiment, from, to, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve the experiment's assignment traffic. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * URLEncodes the generated Strings generated from the dates which are passed as the URL.
     *
     * @param experiment the experiment
     * @param from the first day to retrieve
     * @param to the last day to retrieve
     * @return a map of lists (= table) containing meta and assignment traffic data
     */
    protected Map<String, List<?>> getTraffic(Experiment experiment, LocalDateTime from, LocalDateTime to,
            int expectedStatus) {
        RestAssured.urlEncodingEnabled = false;
        APIServerConnector apiServerConnectorNoURLEncoding = apiServerConnector.clone();
        RestAssured.urlEncodingEnabled = true;
        return getTraffic(experiment, from, to, expectedStatus, apiServerConnectorNoURLEncoding);
    }

    /**
     * Sends a GET request to retrieve the experiment's assignment traffic. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * URLEncodes the generated Strings generated from the dates which are passed as the URL.
     *
     * @param experiment the experiment
     * @param from the first day to retrieve
     * @param to the last day to retrieve
     * @return a map of lists (= table) containing meta and assignment traffic data
     */
    protected Map<String, List<?>> getTraffic(Experiment experiment, LocalDateTime from, LocalDateTime to,
            int expectedStatus, APIServerConnector apiServerConnector) {
        try {
            return getTraffic(experiment, URLEncoder.encode(TestUtils.formatDateForUI(from), "utf8"),
                    URLEncoder.encode(TestUtils.formatDateForUI(to), "utf8"), expectedStatus, apiServerConnector);
        } catch (UnsupportedEncodingException e) {
            Assert.fail("Failed to urlencode the date in TestBase. Should not have happened! " + e.getMessage());
            return null;
        }
    }

    /**
     * Sends a GET request to retrieve the experiment's assignment traffic. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param from the first day to retrieve
     * @param to the last day to retrieve
     * @return a map of lists (= table) containing meta and assignment traffic data
     */
    protected Map<String, List<?>> getTraffic(Experiment experiment, String from, String to) {
        return getTraffic(experiment, from, to, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve the experiment's assignment traffic. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param from the first day to retrieve
     * @param to the last day to retrieve
     * @param expectedStatus the expected HTTP status code
     * @return a map of lists (= table) containing meta and assignment traffic data
     */
    protected Map<String, List<?>> getTraffic(Experiment experiment, String from, String to, int expectedStatus) {
        return getTraffic(experiment, from, to, expectedStatus, apiServerConnector);

    }

    /**
     * Sends a GET request to retrieve the experiment's assignment traffic. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param from the first day to retrieve
     * @param to the last day to retrieve
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a map of lists (= table) containing meta and assignment traffic data
     */
    protected Map<String, List<?>> getTraffic(Experiment experiment, String from, String to, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/assignments/traffic/" + from + "/" + to;
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        Map<String, List<?>> resultMap = new HashMap<>(4);
        resultMap.put("priorities", response.jsonPath().<Integer>getList("priorities"));
        resultMap.put("assignmentRatios", response.jsonPath().<Map<String, Object>>getList("assignmentRatios"));
        resultMap.put("experiments", response.jsonPath().<String>getList("experiments"));
        resultMap.put("samplingPercentages", response.jsonPath().<Float>getList("samplingPercentages"));
        return resultMap;
    }

    /////////////////////////////////////
    // experiments/<id>/pages Endpoint //
    /////////////////////////////////////

    /**
     * Sends a GET request to retrieve the pages the experiment is assigned to. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @return a list of pages
     */
    public List<Page> getPages(Experiment experiment) {
        return getPages(experiment, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve the pages the experiment is assigned to. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param expectedStatus the expected HTTP status code
     * @return a list of pages
     */
    public List<Page> getPages(Experiment experiment, int expectedStatus) {
        return getPages(experiment, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve the pages the experiment is assigned to. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of pages
     */
    public List<Page> getPages(Experiment experiment, int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/pages";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonMapping = response.jsonPath().getList("pages");
        List<Page> pageList = new ArrayList<>(jsonMapping.size());
        for (Map jsonMap : jsonMapping) {
            String jsonString = simpleGson.toJson(jsonMap);
            pageList.add(PageFactory.createFromJSONString(jsonString));
        }
        return pageList;
    }

    /**
     * Sends a POST request to add an experiment to a page. The response must contain {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param experiment the experiment
     * @param page the page
     * @return the response
     */
    public Response postPages(Experiment experiment, Page page) {
        return postPages(experiment, Collections.singletonList(page));
    }

    /**
     * Sends a POST request to add an experiment to a page. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param page the page
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postPages(Experiment experiment, Page page, int expectedStatus) {
        return postPages(experiment, page, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to add an experiment to a page. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param page the page
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response postPages(Experiment experiment, Page page, int expectedStatus,
            APIServerConnector apiServerConnector) {
        return postPages(experiment, Collections.singletonList(page), expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to add an experiment to several pages. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param experiment the experiment
     * @param pages the pages
     * @return the response
     */
    public Response postPages(Experiment experiment, List<Page> pages) {
        return postPages(experiment, pages, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a POST request to add an experiment to several pages. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param pages the pages
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postPages(Experiment experiment, List<Page> pages, int expectedStatus) {
        return postPages(experiment, pages, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to add an experiment to several pages. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param pages the pages
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response postPages(Experiment experiment, List<Page> pages, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/pages";
        response = apiServerConnector.doPost(uri, TestUtils.wrapJsonIntoObject(simpleGson.toJson(pages), "pages"));
        assertReturnCode(response, expectedStatus);
        return response;
    }

    /**
     * Sends a POST request to add list of experiments to a page. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param experimentsList the list of experiments we want to add to a page
     * @param page the page
     * @return the response
     */
    public Response postPages(List<Experiment> experimentsList, Page page) {
        return postPages(experimentsList, Collections.singletonList(page));
    }

    /**
     * Sends a POST request to add list of experiments to a page. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experimentsList the list of experiments we want to add to a page
     * @param page the page
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postPages(List<Experiment> experimentsList, Page page, int expectedStatus) {
        return postPages(experimentsList, page, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to add list of experiments to a page. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experimentsList the list of experiments we want to add to a page
     * @param page the page
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response postPages(List<Experiment> experimentsList, Page page, int expectedStatus,
            APIServerConnector apiServerConnector) {
        return postPages(experimentsList, Collections.singletonList(page), expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to add list of experiments to a page. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param experimentsList the list of experiments
     * @param pages the pages
     * @return the response
     */
    public Response postPages(List<Experiment> experimentsList, List<Page> pages) {
        return postPages(experimentsList, pages, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a POST request to add list of experiments to several pages. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experimentsList the list of experiments
     * @param pages the pages
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postPages(List<Experiment> experimentsList, List<Page> pages, int expectedStatus) {
        return postPages(experimentsList, pages, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to add an experiment to several pages. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param pages the pages
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response postPages(List<Experiment> experimentsList, List<Page> pages, int expectedStatus,
            APIServerConnector apiServerConnector) {
        for (Experiment experiment : experimentsList) {
            String uri = "experiments/" + experiment.id + "/pages";
            response = apiServerConnector.doPost(uri, TestUtils.wrapJsonIntoObject(simpleGson.toJson(pages), "pages"));
            assertReturnCode(response, expectedStatus);
        }
        return response;
    }

    /**
     * Sends a DELETE request to delete an experiment from a page. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param experiment the experiment
     * @param page the page
     * @return the response
     */
    public Response deletePages(Experiment experiment, Page page) {
        return deletePages(experiment, page, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a DELETE request to delete an experiment from a page. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param page the page
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response deletePages(Experiment experiment, Page page, int expectedStatus) {
        return deletePages(experiment, page, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a DELETE request to delete an experiment from a page. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param page the page
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response deletePages(Experiment experiment, Page page, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/" + experiment.id + "/pages/" + page.name;
        response = apiServerConnector.doDelete(uri);
        assertReturnCode(response, expectedStatus);
        return response;
    }

    //////////////////////////////////////////////////////////////////
    // applications/<appName>/pages/ endpoint //
    //////////////////////////////////////////////////////////////////

    /**
     * Sends a GET request to retrieve pages assigned to the application. The response must contain HTTP
     * {@link HttpStatus#SC_OK}.
     *
     * @param application the application
     * @return a list of pages
     */
    public List<Page> getPages(Application application) {
        return getPages(application, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve pages assigned to the application. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application
     * @param expectedStatus the expected HTTP status code
     * @return a list of pages
     */
    public List<Page> getPages(Application application, int expectedStatus) {
        return getPages(application, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve pages assigned to the application. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of pages
     */
    public List<Page> getPages(Application application, int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "applications/" + application.name + "/pages/";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonMapping = response.jsonPath().getList("pages");
        List<Page> pageList = new ArrayList<>(jsonMapping.size());
        for (Map jsonMap : jsonMapping) {
            String jsonString = simpleGson.toJson(jsonMap);
            pageList.add(PageFactory.createFromJSONString(jsonString));
        }
        return pageList;
    }

    //////////////////////////////////////////////////////////////////
    // experiments/applications/<appName>/pages/<pagename> endpoint //
    //////////////////////////////////////////////////////////////////

    /**
     * Sends a GET request to retrieve pages assigned to the experiments of this application. The response must contain
     * HTTP {@link HttpStatus#SC_OK}.
     *
     * @param application the application
     * @param page the page
     * @return a list of pages
     */
    public List<Page> getPages(Application application, Page page) {
        return getPages(application, page, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve pages assigned to the experiments of this application. The response must contain
     * HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param page the page
     * @param expectedStatus the expected HTTP status code
     * @return a list of pages
     */
    public List<Page> getPages(Application application, Page page, int expectedStatus) {
        return getPages(application, page, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve pages assigned to the experiments of this application. The response must contain
     * HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param page the page
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of pages
     */
    public List<Page> getPages(Application application, Page page, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "experiments/applications/" + application.name + "/pages/" + page.name;
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonMapping = response.jsonPath().getList("pages");
        List<Page> pageList = new ArrayList<>(jsonMapping.size());
        for (Map jsonMap : jsonMapping) {
            String jsonString = simpleGson.toJson(jsonMap);
            pageList.add(PageFactory.createFromJSONString(jsonString));
        }
        return pageList;
    }

    //////////////////////////
    // assignments Endpoint //
    //////////////////////////

    /**
     * Sends a GET request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @return an assignment
     */
    public Assignment getAssignment(Experiment experiment, User user) {
        return getAssignment(experiment, user, null);
    }

    /**
     * Sends a GET request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @return an assignment
     */
    public Assignment getAssignment(Experiment experiment, User user, String context) {
        return getAssignment(experiment, user, context, true);
    }

    /**
     * Sends a GET request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status
     * @return an assignment
     */
    public Assignment getAssignment(Experiment experiment, User user, String context, boolean createAssignment) {
        return getAssignment(experiment, user, context, createAssignment, false);
    }

    /**
     * Sends a GET request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status
     * @param ignoreSamplingPercent indicates whether sampling percentages shall be ignored
     * @return an assignment
     */
    public Assignment getAssignment(Experiment experiment, User user, String context, boolean createAssignment,
            boolean ignoreSamplingPercent) {
        return getAssignment(experiment, user, context, createAssignment, ignoreSamplingPercent, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to get the assignment of the user for the experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status
     * @param ignoreSamplingPercent indicates whether sampling percentages shall be ignored
     * @param expectedStatus the expected HTTP status code
     * @return an assignment
     */
    public Assignment getAssignment(Experiment experiment, User user, String context, boolean createAssignment,
            boolean ignoreSamplingPercent, int expectedStatus) {
        return getAssignment(experiment, user, context, createAssignment, ignoreSamplingPercent, expectedStatus,
                apiServerConnector);
    }

    /**
     * Sends a GET request to get the assignment of the user for the experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status
     * @param ignoreSamplingPercent indicates whether sampling percentages shall be ignored
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return an assignment
     */
    public Assignment getAssignment(Experiment experiment, User user, String context, boolean createAssignment,
            boolean ignoreSamplingPercent, int expectedStatus, APIServerConnector apiServerConnector) {
        // Clear cache before assignment call
        clearAssignmentsMetadataCache();

        String uri = "assignments/applications/" + experiment.applicationName + "/experiments/" + experiment.label
                + "/users/" + user.userID;
        if (context != null || !createAssignment || ignoreSamplingPercent) {
            uri += "?";
            if (context != null) {
                uri += "context=" + context + "&";
            }
            if (!createAssignment) {
                uri += "createAssignment=false&";
            }
            if (ignoreSamplingPercent) {
                uri += "ignoreSamplingPercent=true&";
            }
            uri = uri.substring(0, uri.length() - 1);
        }
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        return AssignmentFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a POST request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @return an assignment
     */
    public Assignment postAssignment(Experiment experiment, User user) {
        return postAssignment(experiment, user, null);
    }

    /**
     * Sends a POST request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @return an assignment
     */
    public Assignment postAssignment(Experiment experiment, User user, String context) {
        return postAssignment(experiment, user, context, true);
    }

    /**
     * Sends a POST request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status, default: true
     * @return an assignment
     */
    public Assignment postAssignment(Experiment experiment, User user, String context, boolean createAssignment) {
        return postAssignment(experiment, user, context, createAssignment, false);
    }

    /**
     * Sends a POST request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status, default: true
     * @param ignoreSamplingPercent indicates whether sampling percentages shall be ignored, default: false
     * @return an assignment
     */
    public Assignment postAssignment(Experiment experiment, User user, String context, boolean createAssignment,
            boolean ignoreSamplingPercent) {
        return postAssignment(experiment, user, context, createAssignment, ignoreSamplingPercent, null);
    }

    /**
     * Sends a POST request to get the assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status, default: true
     * @param ignoreSamplingPercent indicates whether sampling percentages shall be ignored, default: false
     * @param profile a key-value map for the user profile
     * @return an assignment
     */
    public Assignment postAssignment(Experiment experiment, User user, String context, boolean createAssignment,
            boolean ignoreSamplingPercent, Map<String, Object> profile) {
        return postAssignment(experiment, user, context, createAssignment, ignoreSamplingPercent, profile,
                HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to get the assignment of the user for the experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status, default: true
     * @param ignoreSamplingPercent indicates whether sampling percentages shall be ignored, default: false
     * @param profile a key-value map for the user profile
     * @param expectedStatus the expected HTTP status code
     * @return an assignment
     */
    public Assignment postAssignment(Experiment experiment, User user, String context, boolean createAssignment,
            boolean ignoreSamplingPercent, Map<String, Object> profile, int expectedStatus) {
        return postAssignment(experiment, user, context, createAssignment, ignoreSamplingPercent, profile,
                expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to get the assignment of the user for the experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param user the user
     * @param context the context
     * @param createAssignment gets the new assignment allowance status, default: true
     * @param ignoreSamplingPercent indicates whether sampling percentages shall be ignored, default: false
     * @param profile a key-value map for the user profile
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return an assignment
     */
    public Assignment postAssignment(Experiment experiment, User user, String context, boolean createAssignment,
            boolean ignoreSamplingPercent, Map<String, Object> profile, int expectedStatus,
            APIServerConnector apiServerConnector) {
        clearAssignmentsMetadataCache();

        String uri = "assignments/applications/" + experiment.applicationName + "/experiments/" + experiment.label
                + "/users/" + user.userID;
        if (context != null || !createAssignment || ignoreSamplingPercent) {
            uri += "?";
            if (context != null) {
                uri += "context=" + context + "&";
            }
            if (!createAssignment) {
                uri += "createAssignment=false&";
            }
            if (ignoreSamplingPercent) {
                uri += "ignoreSamplingPercent=true&";
            }
            uri = uri.substring(0, uri.length() - 1);
        }
        String jsonBody = TestUtils.wrapJsonIntoObject(simpleGson.toJson(profile), "profile");
        response = apiServerConnector.doPost(uri, jsonBody);
        assertReturnCode(response, expectedStatus);
        return AssignmentFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a PUT request to update an assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     * <p>
     * Ignores {@link Assignment#payload}, {@link Assignment#context} (see below for notes), {@link Assignment#cache},
     * {@link Assignment#experimentLabel}, and {@link Assignment#status}. If the current
     * {@link Assignment#serializationStrategy} excludes {@link Assignment#assignment} and/or
     * {@link Assignment#overwrite} the respective field is excluded as well.
     * <p>
     * Note on {@link Assignment#context}: To implicitly include the context as a parameter, set the boolean flag of
     * this method to true.
     *
     * @param experiment the experiment
     * @param assignment the assignment to be changed
     * @param user the user
     * @param useAssignmentContext determines whether the assignments context shall be used as a parameter for the
     *            request
     * @return a new assignment reflecting the update
     */
    public Assignment putAssignment(Experiment experiment, Assignment assignment, User user,
            boolean useAssignmentContext) {
        return putAssignment(experiment, assignment, user, useAssignmentContext, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update an assignment of the user for the experiment. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * Ignores {@link Assignment#payload}, {@link Assignment#context} (see below for notes), {@link Assignment#cache},
     * {@link Assignment#experimentLabel}, and {@link Assignment#status}. If the current
     * {@link Assignment#serializationStrategy} excludes {@link Assignment#assignment} and/or
     * {@link Assignment#overwrite} the respective field is excluded as well.
     * <p>
     * Note on {@link Assignment#context}: To implicitly include the context as a parameter, set the boolean flag of
     * this method to true.
     *
     * @param experiment the experiment
     * @param assignment the assignment to be changed
     * @param user the user
     * @param useAssignmentContext determines whether the assignments context shall be used as a parameter for the
     *            request
     * @param expectedStatus the expected HTTP status code
     * @return a new assignment reflecting the update
     */
    public Assignment putAssignment(Experiment experiment, Assignment assignment, User user,
            boolean useAssignmentContext, int expectedStatus) {
        return putAssignment(experiment, assignment, user, useAssignmentContext, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update an assignment of the user for the experiment. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * Ignores {@link Assignment#payload}, {@link Assignment#context} (see below for notes), {@link Assignment#cache},
     * {@link Assignment#experimentLabel}, and {@link Assignment#status}. If the current
     * {@link Assignment#serializationStrategy} excludes {@link Assignment#assignment} and/or
     * {@link Assignment#overwrite} the respective field is excluded as well.
     * <p>
     * Note on {@link Assignment#context}: To implicitly include the context as a parameter, set the boolean flag of
     * this method to true.
     *
     * @param experiment the experiment
     * @param assignment the assignment to be changed
     * @param user the user
     * @param useAssignmentContext determines whether the assignments context shall be used as a parameter for the
     *            request
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a new assignment reflecting the update
     */
    public Assignment putAssignment(Experiment experiment, Assignment assignment, User user,
            boolean useAssignmentContext, int expectedStatus, APIServerConnector apiServerConnector) {
        return putAssignment(experiment, assignment, user, useAssignmentContext ? assignment.context : null,
                expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update an assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     * <p>
     * Ignores {@link Assignment#payload}, {@link Assignment#context} (see below for notes), {@link Assignment#cache},
     * {@link Assignment#experimentLabel}, and {@link Assignment#status}. If the current
     * {@link Assignment#serializationStrategy} excludes {@link Assignment#assignment} and/or
     * {@link Assignment#overwrite} the respective field is excluded as well.
     * <p>
     * Note on {@link Assignment#context}: To implicitly include the context as a parameter, use
     * {@link #putAssignment(Experiment, Assignment, User, boolean, int, APIServerConnector)} or any of its derivatives
     * with the fourth parameter set to {@code true}. Alternatively supply the context yourself.
     *
     * @param experiment the experiment
     * @param assignment the assignment to be changed
     * @param user the user
     * @return a new assignment reflecting the update
     */
    public Assignment putAssignment(Experiment experiment, Assignment assignment, User user) {
        return putAssignment(experiment, assignment, user, null);
    }

    /**
     * Sends a PUT request to update an assignment of the user for the experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     * <p>
     * Ignores {@link Assignment#payload}, {@link Assignment#context} (see below for notes), {@link Assignment#cache},
     * {@link Assignment#experimentLabel}, and {@link Assignment#status}. If the current
     * {@link Assignment#serializationStrategy} excludes {@link Assignment#assignment} and/or
     * {@link Assignment#overwrite} the respective field is excluded as well.
     * <p>
     * Note on {@link Assignment#context}: To implicitly include the context as a parameter, use
     * {@link #putAssignment(Experiment, Assignment, User, boolean, int, APIServerConnector)} or any of its derivatives
     * with the fourth parameter set to {@code true}. Alternatively supply the context yourself.
     *
     * @param experiment the experiment
     * @param assignment the assignment to be changed
     * @param user the user
     * @param context the context
     * @return a new assignment reflecting the update
     */
    public Assignment putAssignment(Experiment experiment, Assignment assignment, User user, String context) {
        return putAssignment(experiment, assignment, user, context, HttpStatus.SC_OK);
    }

    /**
     * Sends a PUT request to update an assignment of the user for the experiment. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * Ignores {@link Assignment#payload}, {@link Assignment#context} (see below for notes), {@link Assignment#cache},
     * {@link Assignment#experimentLabel}, and {@link Assignment#status}. If the current
     * {@link Assignment#serializationStrategy} excludes {@link Assignment#assignment} and/or
     * {@link Assignment#overwrite} the respective field is excluded as well.
     * <p>
     * Note on {@link Assignment#context}: To implicitly include the context as a parameter, use
     * {@link #putAssignment(Experiment, Assignment, User, boolean, int, APIServerConnector)} or any of its derivatives
     * with the fourth parameter set to {@code true}. Alternatively supply the context yourself.
     *
     * @param experiment the experiment
     * @param assignment the assignment to be changed
     * @param user the user
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @return a new assignment reflecting the update
     */
    public Assignment putAssignment(Experiment experiment, Assignment assignment, User user, String context,
            int expectedStatus) {
        return putAssignment(experiment, assignment, user, context, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to update an assignment of the user for the experiment. The response must contain HTTP
     * {@code expectedStatus}.
     * <p>
     * Ignores {@link Assignment#payload}, {@link Assignment#context} (see below for notes), {@link Assignment#cache},
     * {@link Assignment#experimentLabel}, and {@link Assignment#status}. If the current
     * {@link Assignment#serializationStrategy} excludes {@link Assignment#assignment} and/or
     * {@link Assignment#overwrite} the respective field is excluded as well.
     * <p>
     * Note on {@link Assignment#context}: To implicitly include the context as a parameter, use
     * {@link #putAssignment(Experiment, Assignment, User, boolean, int, APIServerConnector)} or any of its derivatives
     * with the fourth parameter set to {@code true}. Alternatively supply the context yourself.
     *
     * @param experiment the experiment
     * @param assignment the assignment to be changed
     * @param user the user
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a new assignment reflecting the update
     */
    public Assignment putAssignment(Experiment experiment, Assignment assignment, User user, String context,
            int expectedStatus, APIServerConnector apiServerConnector) {
        clearAssignmentsMetadataCache();

        String uri = "assignments/applications/" + experiment.applicationName + "/experiments/" + experiment.label
                + "/users/" + user.userID + (context != null ? ("?context=" + context) : "");

        SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy("payload", "context", "cache",
                "experimentLabel", "status");
        SerializationStrategy tempStrategy = assignment.getSerializationStrategy();
        if (tempStrategy.exclude("assignment")) {
            serializationStrategy.add("assignment");
        }
        if (tempStrategy.exclude("overwrite")) {
            serializationStrategy.add("overwrite");
        }

        assignment.setSerializationStrategy(serializationStrategy);
        response = apiServerConnector.doPut(uri, assignment.toJSONString());
        assignment.setSerializationStrategy(tempStrategy);
        assertReturnCode(response, expectedStatus);

        return AssignmentFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a POST request to assign the user to the experiments of the application if {@code create} is true. The
     * response must contain {@link HttpStatus#SC_OK}.
     *
     * @param application the application
     * @param user the user
     * @param experiments the experiments to assign the user to
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, User user, List<Experiment> experiments) {
        return postAssignments(application, user, experiments, null);
    }

    /**
     * Sends a POST request to assign the user to the experiments of the application if {@code create} is true. The
     * response must contain {@link HttpStatus#SC_OK}.
     *
     * @param application the application
     * @param user the user
     * @param experiments the experiments to assign the user to
     * @param context the context for the assignments
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, User user, List<Experiment> experiments,
            String context) {
        return postAssignments(application, user, experiments, context, true);
    }

    /**
     * Sends a POST request to assign the user to the experiments of the application if {@code create} is true. The
     * response must contain {@link HttpStatus#SC_OK}.
     *
     * @param application the application
     * @param user the user
     * @param experiments the experiments to assign the user to
     * @param context the context for the assignments
     * @param create if true the assignments are created
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, User user, List<Experiment> experiments,
            String context, boolean create) {
        return postAssignments(application, user, experiments, context, create, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to assign the user to the experiments of the application if {@code create} is true. The
     * response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param experiments the experiments to assign the user to
     * @param context the context for the assignments
     * @param create if true the assignments are created
     * @param expectedStatus the exptected HTTP status code
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, User user, List<Experiment> experiments,
            String context, boolean create, int expectedStatus) {
        return postAssignments(application, user, experiments, context, create, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to assign the user to the experiments of the application if {@code create} is true. The
     * response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param experiments the experiments to assign the user to
     * @param context the context for the assignments
     * @param create if true the assignments are created
     * @param expectedStatus the exptected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, User user, List<Experiment> experiments,
            String context, boolean create, int expectedStatus, APIServerConnector apiServerConnector) {
        clearAssignmentsMetadataCache();
        String uri = "assignments/applications/" + application.name + "/users/" + user.userID;
        if ((context != null) || !create) {
            uri += "?";
            if (context != null) {
                uri += "context=" + context + "&";
            }
            if (!create) {
                uri += "create=false&";
            }
            uri = uri.substring(0, uri.length() - 1);
        }

        HashSet<String> experimentLabels = new HashSet<>(experiments.size());
        for (Experiment experiment : experiments) {
            experimentLabels.add(experiment.label);
        }
        String json = TestUtils.wrapJsonIntoObject(simpleGson.toJson(experimentLabels), "labels");
        response = apiServerConnector.doPost(uri, json);
        assertReturnCode(response, expectedStatus);

        List<Map<String, Object>> assignmentMappings = response.jsonPath().getList("assignments");
        List<Assignment> assignments = new ArrayList<>(assignmentMappings.size());
        for (Map assignmentMapping : assignmentMappings) {
            assignments.add(AssignmentFactory.createFromJSONString(simpleGson.toJson(assignmentMapping)));
        }
        return assignments;
    }

    /**
     * Sends a GET request to assign the user to the experiments of the application's page if {@code createAssignment}
     * is true. The response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @return the created assignments, can be 0
     */
    public List<Assignment> getAssignments(Application application, Page page, User user) {
        return getAssignments(application, page, user, null);
    }

    /**
     * Sends a GET request to assign the user to the experiments of the application's page if {@code createAssignment}
     * is true. The response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param context the context for the assignments, default: null
     * @return the created assignments, can be 0
     */
    public List<Assignment> getAssignments(Application application, Page page, User user, String context) {
        return getAssignments(application, page, user, context, true);
    }

    /**
     * Sends a GET request to assign the user to the experiments of the application's page if {@code createAssignment}
     * is true. The response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param context the context for the assignments, default: null
     * @param createAssignment if true the assignments are created, default: true
     * @return the created assignments, can be 0
     */
    public List<Assignment> getAssignments(Application application, Page page, User user, String context,
            boolean createAssignment) {
        return getAssignments(application, page, user, context, createAssignment, false);
    }

    /**
     * Sends a GET request to assign the user to the experiments of the application's page if {@code createAssignment}
     * is true. The response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param context the context for the assignments, default: null
     * @param createAssignment if true the assignments are created, default: true
     * @param ignoreSamplingPercent ignores the sampling percentage, default: false
     * @return the created assignments, can be 0
     */
    public List<Assignment> getAssignments(Application application, Page page, User user, String context,
            boolean createAssignment, boolean ignoreSamplingPercent) {
        return getAssignments(application, page, user, context, createAssignment, ignoreSamplingPercent,
                HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to assign the user to the experiments of the application's page if {@code createAssignment}
     * is true. The response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param context the context for the assignments, default: null
     * @param createAssignment if true the assignments are created, default: true
     * @param ignoreSamplingPercent ignores the sampling percentage, default: false
     * @param expectedStatus the exptected HTTP status code
     * @return the created assignments, can be 0
     */
    public List<Assignment> getAssignments(Application application, Page page, User user, String context,
            boolean createAssignment, boolean ignoreSamplingPercent, int expectedStatus) {
        return getAssignments(application, page, user, context, createAssignment, ignoreSamplingPercent, expectedStatus,
                apiServerConnector);
    }

    /**
     * Sends a GET request to assign the user to the experiments of the application's page if {@code createAssignment}
     * is true. The response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param context the context for the assignments, default: null
     * @param createAssignment if true the assignments are created, default: true
     * @param ignoreSamplingPercent ignores the sampling percentage, default: false
     * @param expectedStatus the exptected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the created assignments, can be 0
     */
    public List<Assignment> getAssignments(Application application, Page page, User user, String context,
            boolean createAssignment, boolean ignoreSamplingPercent, int expectedStatus,
            APIServerConnector apiServerConnector) {
        clearAssignmentsMetadataCache();
        String uri = "assignments/applications/" + application.name + "/pages/" + page.name + "/users/" + user.userID;
        if (context != null || !createAssignment || ignoreSamplingPercent) {
            uri += "?";
            if (context != null) {
                uri += "context=" + context + "&";
            }
            if (!createAssignment) {
                uri += "createAssignment=false&";
            }
            if (ignoreSamplingPercent) {
                uri += "ignoreSamplingPercent=true&";
            }
            uri = uri.substring(0, uri.length() - 1);
        }
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> assignmentMappings = response.jsonPath().getList("assignments");
        List<Assignment> assignments = new ArrayList<>(assignmentMappings.size());
        for (Map assignmentMapping : assignmentMappings) {
            assignments.add(AssignmentFactory.createFromJSONString(simpleGson.toJson(assignmentMapping)));
        }
        return assignments;
    }

    /**
     * Sends a POST request to assign a user to experiments of the specified page. The response must contain
     * {@link HttpStatus#SC_CREATED}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, Page page, User user) {
        return postAssignments(application, page, user, null);
    }

    /**
     * Sends a POST request to assign a user to experiments of the specified page. Applies the segmentation rules given
     * in the {@code segmentationProfile}. The response must contain {@link HttpStatus#SC_CREATED}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param segmentationProfile the segmantation profile, will be wrapped into the correct JSON object
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, Page page, User user,
            Map<String, Object> segmentationProfile) {
        return postAssignments(application, page, user, segmentationProfile, null);
    }

    /**
     * Sends a POST request to assign a user to experiments of the specified page. Applies the segmentation rules given
     * in the {@code segmentationProfile}. The response must contain {@link HttpStatus#SC_CREATED}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param segmentationProfile the segmantation profile, will be wrapped into the correct JSON object
     * @param context the context for the assignments, default: null
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, Page page, User user,
            Map<String, Object> segmentationProfile, String context) {
        return postAssignments(application, page, user, segmentationProfile, context, true);
    }

    /**
     * Sends a POST request to assign a user to experiments of the specified page. Applies the segmentation rules given
     * in the {@code segmentationProfile}. The response must contain {@link HttpStatus#SC_CREATED}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param segmentationProfile the segmantation profile, will be wrapped into the correct JSON object
     * @param context the context for the assignments, default: null
     * @param createAssignment if true the assignments are created, default: true
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, Page page, User user,
            Map<String, Object> segmentationProfile, String context, boolean createAssignment) {
        return postAssignments(application, page, user, segmentationProfile, context, createAssignment, false);
    }

    /**
     * Sends a POST request to assign a user to experiments of the specified page. Applies the segmentation rules given
     * in the {@code segmentationProfile}. The response must contain {@link HttpStatus#SC_CREATED}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param segmentationProfile the segmantation profile, will be wrapped into the correct JSON object
     * @param context the context for the assignments, default: null
     * @param createAssignment if true the assignments are created, default: true
     * @param ignoreSamplingPercent ignores the sampling percentage, default: false
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, Page page, User user,
            Map<String, Object> segmentationProfile, String context, boolean createAssignment,
            boolean ignoreSamplingPercent) {
        return postAssignments(application, page, user, segmentationProfile, context, createAssignment,
                ignoreSamplingPercent, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to assign a user to experiments of the specified page. Applies the segmentation rules given
     * in the {@code segmentationProfile}. The response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param segmentationProfile the segmantation profile, will be wrapped into the correct JSON object
     * @param context the context for the assignments, default: null
     * @param createAssignment if true the assignments are created, default: true
     * @param ignoreSamplingPercent ignores the sampling percentage, default: false
     * @param expectedStatus the exptected HTTP status code
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, Page page, User user,
            Map<String, Object> segmentationProfile, String context, boolean createAssignment,
            boolean ignoreSamplingPercent, int expectedStatus) {
        return postAssignments(application, page, user, segmentationProfile, context, createAssignment,
                ignoreSamplingPercent, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to assign a user to experiments of the specified page. Applies the segmentation rules given
     * in the {@code segmentationProfile}. The response must contain HTTP {@code expectedStatus}.
     *
     * @param application the application
     * @param user the user
     * @param page the page
     * @param segmentationProfile the segmantation profile, will be wrapped into the correct JSON object
     * @param context the context for the assignments, default: null
     * @param createAssignment if true the assignments are created, default: true
     * @param ignoreSamplingPercent ignores the sampling percentage, default: false
     * @param expectedStatus the exptected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the created assignments, can be 0
     */
    public List<Assignment> postAssignments(Application application, Page page, User user,
            Map<String, Object> segmentationProfile, String context, boolean createAssignment,
            boolean ignoreSamplingPercent, int expectedStatus, APIServerConnector apiServerConnector) {
        clearAssignmentsMetadataCache();
        String uri = "assignments/applications/" + application.name + "/pages/" + page.name + "/users/" + user.userID;
        if (context != null || !createAssignment || ignoreSamplingPercent) {
            uri += "?";
            if (context != null) {
                uri += "context=" + context + "&";
            }
            if (!createAssignment) {
                uri += "createAssignment=false&";
            }
            if (ignoreSamplingPercent) {
                uri += "ignoreSamplingPercent=true&";
            }
            uri = uri.substring(0, uri.length() - 1);
        }

        if (segmentationProfile != null) {
            String segmentationProfileJSON = TestUtils.wrapJsonIntoObject(simpleGson.toJson(segmentationProfile),
                    "profile");
            response = apiServerConnector.doPost(uri, segmentationProfileJSON);
        } else {
            response = apiServerConnector.doPost(uri);
        }
        assertReturnCode(response, expectedStatus);

        List<Map<String, Object>> assignmentMappings = response.jsonPath().getList("assignments");
        List<Assignment> assignments = new ArrayList<>(assignmentMappings.size());
        for (Map assignmentMapping : assignmentMappings) {
            assignments.add(AssignmentFactory.createFromJSONString(simpleGson.toJson(assignmentMapping)));
        }
        return assignments;
    }

    /////////////////////
    // events Endpoint //
    /////////////////////

    /**
     * Sends a POST request to send a single event for a specific user for an experiment. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param event the event list to send
     * @param experiment the experiment
     * @param user the user
     * @return the response
     */
    public Response postEvent(Event event, Experiment experiment, User user) {
        return postEvents(Collections.singletonList(event), experiment, user, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a POST request to send a single event for a specific user for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param event the event list to send
     * @param experiment the experiment
     * @param user the user
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postEvent(Event event, Experiment experiment, User user, int expectedStatus) {
        return postEvents(Collections.singletonList(event), experiment, user, expectedStatus);
    }

    /**
     * Sends a POST request to send a list of events for a specific user for an experiment. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param events the event list to send
     * @param experiment the experiment
     * @param user the user
     * @return the response
     */
    public Response postEvents(List<Event> events, Experiment experiment, User user) {
        return postEvents(events, experiment, user, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a POST request to send a list of events for a specific user for an experiment. The response must contain
     * HTTP {@code expectedStatus}.
     *
     * @param events the event list to send
     * @param experiment the experiment
     * @param user the user
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response postEvents(List<Event> events, Experiment experiment, User user, int expectedStatus) {
        return postEvents(events, experiment, user, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to send a list of events for a specific user for an experiment. The response must contain
     * HTTP {@code expectedStatus}.
     *
     * @param events the event list to send
     * @param experiment the experiment
     * @param user the user
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response postEvents(List<Event> events, Experiment experiment, User user, int expectedStatus,
            APIServerConnector apiServerConnector) {
        clearAssignmentsMetadataCache();

        String uri = "events/applications/" + experiment.applicationName + "/experiments/" + experiment.label
                + "/users/" + user.userID;
        String json = TestUtils.wrapJsonIntoObject(simpleGson.toJson(events.toArray()), "events");

        response = apiServerConnector.doPost(uri, json);
        assertReturnCode(response, expectedStatus);

        return response;
    }

    //////////////////////////////////////
    // applications/<appName>/ endpoint //
    //////////////////////////////////////

    /**
     * Sends a GET request to retrieve an experiment for an application. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param application the application
     * @param experiment the experiment to supply the id
     * @return an experiment
     */
    public Experiment getApplicationExperiment(Application application, Experiment experiment) {
        return getApplicationExperiment(application, experiment, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve an experiment for an application. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application
     * @param experiment the experiment to supply the id
     * @param expectedStatus the expected HTTP status code
     * @return an experiment
     */
    public Experiment getApplicationExperiment(Application application, Experiment experiment, int expectedStatus) {
        return getApplicationExperiment(application, experiment, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve an experiment for an application. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application
     * @param experiment the experiment to supply the id
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return an experiment
     */
    public Experiment getApplicationExperiment(Application application, Experiment experiment, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "applications/" + application.name + "/experiments/" + experiment.label;
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        return ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a GET request to retrieve the experiments for an application. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param application the application for which the experiments are
     * @return a list of experiments
     */
    public List<Experiment> getApplicationExperiments(Application application) {
        return getApplicationExperiments(application, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to retrieve the experiments for an application. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param expectedStatus the expected HTTP status code
     * @return a list of experiments
     */
    public List<Experiment> getApplicationExperiments(Application application, int expectedStatus) {
        return getApplicationExperiments(application, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to retrieve the experiments for an application. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of experiments
     */
    public List<Experiment> getApplicationExperiments(Application application, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "applications/" + application.name + "/experiments";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("");
        List<Experiment> expList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            expList.add(ExperimentFactory.createFromJSONString(jsonString));
        }
        return expList;
    }

    /**
     * Sends a PUT request to change the priorities of experiments. The response must contain
     * {@link HttpStatus#SC_NO_CONTENT}.
     *
     * @param application the application for which the experiments are
     * @param experiments the experiments
     * @return the response
     */
    public Response putApplicationPriorities(Application application, List<Experiment> experiments) {
        return putApplicationPriorities(application, experiments, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a PUT request to change the priorities of experiments. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param experiments the experiments
     * @param expectedStatus the expected HTTP status code
     * @return the response
     */
    public Response putApplicationPriorities(Application application, List<Experiment> experiments,
            int expectedStatus) {
        return putApplicationPriorities(application, experiments, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a PUT request to change the priorities of experiments. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param experiments the experiments
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response putApplicationPriorities(Application application, List<Experiment> experiments, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "applications/" + application.name + "/priorities";
        List<String> experimentIDs = new ArrayList<>(experiments.size());
        for (Experiment experiment : experiments) {
            experimentIDs.add(experiment.id);
        }
        String jsonString = TestUtils.wrapJsonIntoObject(simpleGson.toJson(experimentIDs), "experimentIDs");
        response = apiServerConnector.doPut(uri, jsonString);
        assertReturnCode(response, expectedStatus);
        return response;
    }

    /**
     * Sends a GET request to receive a list of experiments ordered by their priority. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param application the application for which the experiments are
     * @return a list of experiments, ordered by priority
     */
    public List<Experiment> getApplicationPriorities(Application application) {
        return getApplicationPriorities(application, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive a list of experiments ordered by their priority. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param expectedStatus the expected HTTP status code
     * @return a list of experiments, ordered by priority
     */
    public List<Experiment> getApplicationPriorities(Application application, int expectedStatus) {
        return getApplicationPriorities(application, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to receive a list of experiments ordered by their priority. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of experiments, ordered by priority
     */
    public List<Experiment> getApplicationPriorities(Application application, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "applications/" + application.name + "/priorities";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("prioritizedExperiments");
        List<Experiment> expList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            expList.add(ExperimentFactory.createFromJSONString(jsonString));
        }
        return expList;
    }

    /**
     * Sends a GET request to receive a list of pages associated with an application. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param application the application for which the experiments are
     * @return a list of pages
     */
    public List<Page> getApplicationPages(Application application) {
        return getApplicationPages(application, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive a list of pages associated with an application. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param expectedStatus the expected HTTP status code
     * @return a list of pages
     */
    public List<Page> getApplicationPages(Application application, int expectedStatus) {
        return getApplicationPages(application, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to receive a list of pages associated with an application. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of pages
     */
    public List<Page> getApplicationPages(Application application, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "applications/" + application.name + "/pages";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonMapping = response.jsonPath().getList("pages");
        List<Page> pageList = new ArrayList<>(jsonMapping.size());
        for (Map jsonMap : jsonMapping) {
            String jsonString = simpleGson.toJson(jsonMap);
            pageList.add(PageFactory.createFromJSONString(jsonString));
        }
        return pageList;

    }

    /**
     * Sends a GET request to receive a list of experiments for an application The response must contain HTTP
     * {@link HttpStatus#SC_OK}
     *
     * @param application the application for which the experiments are
     * @return a list of experiments
     */
    public List<Experiment> getExperimentsByApplication(Application application) {
        return getExperimentsByApplication(application, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive a list of experiments for an application The response must contain HTTP
     * {@link HttpStatus#SC_OK}
     *
     * @param application the application for which the experiments are
     * @param expectedStatus the expected HTTP status code
     * @return a list of experiments
     */
    public List<Experiment> getExperimentsByApplication(Application application, int expectedStatus) {
        return getExperimentsByApplication(application, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to receive a list of experiments for an application The response must contain HTTP
     * {@link HttpStatus#SC_OK}
     *
     * @param application the application for which the experiments are
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of experiments
     */
    public List<Experiment> getExperimentsByApplication(Application application, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "applications/" + application.name + "/experiments";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("experiments");
        List<Experiment> expList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            expList.add(ExperimentFactory.createFromJSONString(jsonString));
        }
        return expList;
    }

    /**
     * Sends a GET request to receive a list of experiments for an application and page. The response must contain HTTP
     * {@link HttpStatus#SC_OK}.
     *
     * @param application the application for which the experiments are
     * @param page the experiment page
     * @return a list of experiments
     */
    public List<Experiment> getExperimentsByApplicationPage(Application application, Page page) {
        return getExperimentsByApplicationPage(application, page, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive a list of experiments for an application and page. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param page the experiment page
     * @param expectedStatus the expected HTTP status code
     * @return a list of experiments
     */
    public List<Experiment> getExperimentsByApplicationPage(Application application, Page page, int expectedStatus) {
        return getExperimentsByApplicationPage(application, page, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to receive a list of experiments for an application and page. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param application the application for which the experiments are
     * @param page the experiment page
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of experiments
     */
    public List<Experiment> getExperimentsByApplicationPage(Application application, Page page, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "applications/" + application.name + "/pages/" + page.name + "/experiments";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("experiments");
        List<Experiment> expList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            expList.add(ExperimentFactory.createFromJSONString(jsonString));
        }
        return expList;
    }

    ////////////////////////
    // analytics endpoint //
    ////////////////////////

    /**
     * Sends a POST request to receive counts for an experiment. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @return the experiment counts
     */
    public ExperimentCounts postExperimentCounts(Experiment experiment, AnalyticsParameters parameters) {
        return postExperimentCounts(experiment, parameters, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to receive counts for an experiment. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @return the experiment counts
     */
    public ExperimentCounts postExperimentCounts(Experiment experiment, AnalyticsParameters parameters,
            int expectedStatus) {
        return postExperimentCounts(experiment, parameters, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to receive counts for an experiment. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the experiment counts
     */
    public ExperimentCounts postExperimentCounts(Experiment experiment, AnalyticsParameters parameters,
            int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "analytics/experiments/" + experiment.id + "/counts";

        response = apiServerConnector.doPost(uri, simpleGson.toJson(parameters));
        assertReturnCode(response, expectedStatus);

        return simpleGson.fromJson(response.jsonPath().prettify(), ExperimentCounts.class);
    }

    /**
     * Sends a GET request to receive counts for an experiment. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @return the experiment counts
     */
    public ExperimentCounts getExperimentCounts(Experiment experiment) {
        return getExperimentCounts(experiment, null);
    }

    /**
     * Sends a GET request to receive counts for an experiment. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param context the context
     * @return the experiment counts
     */
    public ExperimentCounts getExperimentCounts(Experiment experiment, String context) {
        return getExperimentCounts(experiment, context, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive counts for an experiment. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @return the experiment counts
     */
    public ExperimentCounts getExperimentCounts(Experiment experiment, String context, int expectedStatus) {
        return getExperimentCounts(experiment, context, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to receive counts for an experiment. The response must contain HTTP {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the experiment counts
     */
    public ExperimentCounts getExperimentCounts(Experiment experiment, String context, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "analytics/experiments/" + experiment.id + "/counts";
        if (context != null) {
            uri += "?context=" + context;
        }

        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);

        return simpleGson.fromJson(response.jsonPath().prettify(), ExperimentCounts.class);
    }

    /**
     * Sends a POST request to receive cumulative experiments counts for an experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeCounts postExperimentCumulativeCounts(Experiment experiment,
            AnalyticsParameters parameters) {
        return postExperimentCumulativeCounts(experiment, parameters, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to receive cumulative experiments counts for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeCounts postExperimentCumulativeCounts(Experiment experiment,
            AnalyticsParameters parameters, int expectedStatus) {
        return postExperimentCumulativeCounts(experiment, parameters, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to receive cumulative experiments counts for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeCounts postExperimentCumulativeCounts(Experiment experiment,
            AnalyticsParameters parameters, int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "analytics/experiments/" + experiment.id + "/counts/dailies";

        response = apiServerConnector.doPost(uri, simpleGson.toJson(parameters));
        assertReturnCode(response, expectedStatus);

        return simpleGson.fromJson(response.jsonPath().prettify(), ExperimentCumulativeCounts.class);
    }

    /**
     * Sends a GET request to receive cumulative experiments counts for an experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeCounts getExperimentCumulativeCounts(Experiment experiment) {
        return getExperimentCumulativeCounts(experiment, null);
    }

    /**
     * Sends a GET request to receive cumulative experiments counts for an experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param context the context
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeCounts getExperimentCumulativeCounts(Experiment experiment, String context) {
        return getExperimentCumulativeCounts(experiment, context, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive cumulative experiments counts for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeCounts getExperimentCumulativeCounts(Experiment experiment, String context,
            int expectedStatus) {
        return getExperimentCumulativeCounts(experiment, context, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to receive cumulative experiments counts for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeCounts getExperimentCumulativeCounts(Experiment experiment, String context,
            int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "analytics/experiments/" + experiment.id + "/counts/dailies";
        if (context != null) {
            uri += "?context=" + context;
        }

        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);

        return simpleGson.fromJson(response.jsonPath().prettify(), ExperimentCumulativeCounts.class);
    }

    /**
     * Sends a GET request to retrieve all experiment tags for any application. The response must contain HTTP
     * {@code HttpStatus.SC_OK}.
     *
     * @return an experiment
     */
    public String getExperimentTags() {
        String uri = "applications/tags";
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, HttpStatus.SC_OK);
        return response.jsonPath().prettify();
    }

    /**
     * Sends a POST request to receive statistics for an experiment. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @return the cumulative experiment counts
     */
    public ExperimentStatistics postStatistics(Experiment experiment, AnalyticsParameters parameters) {
        return postStatistics(experiment, parameters, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to receive statistics for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @return the cumulative experiment counts
     */
    public ExperimentStatistics postStatistics(Experiment experiment, AnalyticsParameters parameters,
            int expectedStatus) {
        return postStatistics(experiment, parameters, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to receive statistics for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the cumulative experiment counts
     */
    public ExperimentStatistics postStatistics(Experiment experiment, AnalyticsParameters parameters,
            int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "analytics/experiments/" + experiment.id + "/statistics";

        response = apiServerConnector.doPost(uri, simpleGson.toJson(parameters));
        assertReturnCode(response, expectedStatus);

        return simpleGson.fromJson(response.jsonPath().prettify(), ExperimentStatistics.class);
    }

    /**
     * Sends a GET request to receive statistics for an experiment. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @return the cumulative experiment counts
     */
    public ExperimentStatistics getStatistics(Experiment experiment) {
        return getStatistics(experiment, null);
    }

    /**
     * Sends a GET request to receive statistics for an experiment. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param context the context
     * @return the cumulative experiment counts
     */
    public ExperimentStatistics getStatistics(Experiment experiment, String context) {
        return getStatistics(experiment, context, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive statistics for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @return the cumulative experiment counts
     */
    public ExperimentStatistics getStatistics(Experiment experiment, String context, int expectedStatus) {
        return getStatistics(experiment, context, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to receive statistics for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the cumulative experiment counts
     */
    public ExperimentStatistics getStatistics(Experiment experiment, String context, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "analytics/experiments/" + experiment.id + "/statistics";
        if (context != null) {
            uri += "?context=" + context;
        }

        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);

        return simpleGson.fromJson(response.jsonPath().prettify(), ExperimentStatistics.class);
    }

    /**
     * Sends a POST request to receive daily statistics for an experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeStatistics postDailyStatistics(Experiment experiment, AnalyticsParameters parameters) {
        return postDailyStatistics(experiment, parameters, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to receive daily statistics for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeStatistics postDailyStatistics(Experiment experiment, AnalyticsParameters parameters,
            int expectedStatus) {
        return postDailyStatistics(experiment, parameters, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to receive daily statistics for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param parameters the parameters for the request body
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeStatistics postDailyStatistics(Experiment experiment, AnalyticsParameters parameters,
            int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "analytics/experiments/" + experiment.id + "/statistics/dailies";

        response = apiServerConnector.doPost(uri, simpleGson.toJson(parameters));
        assertReturnCode(response, expectedStatus);

        return simpleGson.fromJson(response.jsonPath().prettify(), ExperimentCumulativeStatistics.class);
    }

    /**
     * Sends a GET request to receive daily statistics for an experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeStatistics getDailyStatistics(Experiment experiment) {
        return getDailyStatistics(experiment, null);
    }

    /**
     * Sends a GET request to receive daily statistics for an experiment. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param experiment the experiment
     * @param context the context
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeStatistics getDailyStatistics(Experiment experiment, String context) {
        return getDailyStatistics(experiment, context, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive daily statistics for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeStatistics getDailyStatistics(Experiment experiment, String context,
            int expectedStatus) {
        return getDailyStatistics(experiment, context, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to receive daily statistics for an experiment. The response must contain HTTP
     * {@code expectedStatus}.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the cumulative experiment counts
     */
    public ExperimentCumulativeStatistics getDailyStatistics(Experiment experiment, String context, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "analytics/experiments/" + experiment.id + "/statistics/dailies";
        if (context != null) {
            uri += "?context=" + context;
        }

        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);

        return simpleGson.fromJson(response.jsonPath().prettify(), ExperimentCumulativeStatistics.class);
    }

    /**
     * Sends a GET request to receive assignment statistics. The response must contain {@link HttpStatus#SC_OK}.
     * <p>
     * Uses the experiment's applicationName and experimentLabel.
     *
     * @param experiment the experiment
     * @return the cumulative experiment counts
     */
    public String getAssignmentSummary(Experiment experiment) {
        return getAssignmentSummary(experiment, null);
    }

    /**
     * Sends a GET request to receive assignment statistics. The response must contain {@link HttpStatus#SC_OK}.
     * <p>
     * Uses the experiment's applicationName and experimentLabel.
     *
     * @param experiment the experiment
     * @param context the context
     * @return the cumulative experiment counts
     */
    public String getAssignmentSummary(Experiment experiment, String context) {
        return getAssignmentSummary(experiment, context, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to receive assignment statistics. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * Uses the experiment's applicationName and experimentLabel.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @return the cumulative experiment counts
     */
    public String getAssignmentSummary(Experiment experiment, String context, int expectedStatus) {
        return getAssignmentSummary(experiment, context, expectedStatus, apiServerConnector);
    }

    /**
     * TODO: just returns a JSON String as of now... Sends a GET request to receive assignment statistics. The response
     * must contain HTTP {@code expectedStatus}.
     * <p>
     * Uses the experiment's applicationName and experimentLabel.
     *
     * @param experiment the experiment
     * @param context the context
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the cumulative experiment counts
     */
    public String getAssignmentSummary(Experiment experiment, String context, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "analytics/applications/" + experiment.applicationName + "/experiments/" + experiment.label
                + "/assignments/counts";
        if (context != null) {
            uri += "?context=" + context;
        }

        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);

        return response.jsonPath().prettify();
    }

    /////////////////////
    // CUSTOM requests //
    /////////////////////

    /**
     * Sends a POST request to receive a response. The response must contain HTTP {@code expectedStatus}.
     *
     * @param uri the endpoint uri (without the base path, for example without /api/v1/), starting without /
     * @param uriParameters a map of uri parameters
     * @param requestBody the request body, if json needed transform first.
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response doPost(String uri, Map<String, Object> uriParameters, Object requestBody, int expectedStatus,
            APIServerConnector apiServerConnector) {
        if (uriParameters != null && uriParameters.size() > 0) {
            uri += "?";
            for (Map.Entry<String, Object> parameter : uriParameters.entrySet()) {
                uri += parameter.getKey() + "=" + parameter.getValue() + "&";
            }
            uri = uri.substring(uri.lastIndexOf("&"));
        }
        response = apiServerConnector.doPost(uri, requestBody);
        assertReturnCode(response, expectedStatus);

        return response;
    }

    /**
     * Sends a GET request to receive a response. The response must contain HTTP {@code expectedStatus}.
     *
     * @param uri the endpoint uri (without the base path, for example without /api/v1/), starting without /
     * @param uriParameters a map of uri parameters
     * @param requestBody the request body, if json needed transform first.
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response doGet(String uri, Map<String, Object> uriParameters, Object requestBody, int expectedStatus,
            APIServerConnector apiServerConnector) {
        if (uriParameters != null && uriParameters.size() > 0) {
            uri += "?";
            for (Map.Entry<String, Object> parameter : uriParameters.entrySet()) {
                uri += parameter.getKey() + "=" + parameter.getValue() + "&";
            }
            uri = uri.substring(uri.lastIndexOf("&"));
        }
        response = apiServerConnector.doGet(uri, requestBody);
        assertReturnCode(response, expectedStatus);

        return response;
    }

    /**
     * Sends a DELETE request to receive a response. The response must contain HTTP {@code expectedStatus}.
     *
     * @param uri the endpoint uri (without the base path, for example without /api/v1/), starting without /
     * @param uriParameters a map of uri parameters
     * @param requestBody the request body, if json needed transform first.
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response doDelete(String uri, Map<String, Object> uriParameters, Object requestBody, int expectedStatus,
            APIServerConnector apiServerConnector) {
        if (uriParameters != null && uriParameters.size() > 0) {
            uri += "?";
            for (Map.Entry<String, Object> parameter : uriParameters.entrySet()) {
                uri += parameter.getKey() + "=" + parameter.getValue() + "&";
            }
            uri = uri.substring(uri.lastIndexOf("&"));
        }
        response = apiServerConnector.doDelete(uri, requestBody);
        assertReturnCode(response, expectedStatus);

        return response;
    }

    /**
     * Sends a PUT request to receive a response. The response must contain HTTP {@code expectedStatus}.
     *
     * @param uri the endpoint uri (without the base path, for example without /api/v1/), starting without /
     * @param uriParameters a map of uri parameters
     * @param requestBody the request body, if json needed transform first.
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return the response
     */
    public Response doPut(String uri, Map<String, Object> uriParameters, Object requestBody, int expectedStatus,
            APIServerConnector apiServerConnector) {
        if (uriParameters != null && uriParameters.size() > 0) {
            uri += "?";
            for (Map.Entry<String, Object> parameter : uriParameters.entrySet()) {
                uri += parameter.getKey() + "=" + parameter.getValue() + "&";
            }
            uri = uri.substring(uri.lastIndexOf("&"));
        }
        response = apiServerConnector.doPut(uri, requestBody);
        assertReturnCode(response, expectedStatus);

        return response;
    }

    ///////////////////////////////
    // feedback endpoint BEGINS //
    //////////////////////////////

    /**
     * Sends a POST request to create an user feedback. The response must contain HTTP {@code expectedStatus}.
     *
     * @param userFeedback the userFeedback to POST
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return response The response of a request made by REST Assured
     */
    public Response postFeedback(UserFeedback userFeedback, int expectedStatus, APIServerConnector apiServerConnector) {
        response = apiServerConnector.doPost("feedback", userFeedback == null ? null : userFeedback.toJSONString());
        assertReturnCode(response, expectedStatus);
        return response;
    }

    /**
     * Sends a GET request to get all feedbacks. The response must contain HTTP {@code expectedStatus}.
     *
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return a list of user feedbacks
     */
    public List<UserFeedback> getFeedbacks(int expectedStatus, APIServerConnector apiServerConnector) {
        response = apiServerConnector.doGet("feedback");

        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("feedback");
        List<UserFeedback> userfeedbackList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            userfeedbackList.add(UserFeedbackFactory.createFromJSONString(jsonString));
        }
        return userfeedbackList;
    }

    /**
     * Sends a GET request to get feedbacks by username.
     * <p>
     * The response must contain HTTP {@code expectedStatus}.
     *
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @param username user name
     * @return a list of user feedbacks
     */
    public List<UserFeedback> getFeedbacksByUsername(int expectedStatus, APIServerConnector apiServerConnector,
            String username) {
        response = apiServerConnector.doGet("feedback/users/" + username);

        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("feedbackList");
        List<UserFeedback> userfeedbackList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            userfeedbackList.add(UserFeedbackFactory.createFromJSONString(jsonString));
        }
        return userfeedbackList;
    }

    ///////////////////////////////
    // feedback endpoint ENDS //
    //////////////////////////////

    /////////////////////////////
    // authentication endpoint //
    /////////////////////////////

    /**
     * Sends a POST request to get a login token. The response must contain HTTP {@link HttpStatus#SC_OK}.
     * <p>
     * Uses the default APIServerConnector's user credentials and requests a {@code grant_type} of "client_credentials".
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @return the token
     */
    public AccessToken postLogin() {
        return postLogin(null);
    }

    /**
     * Sends a POST request to get a login token. The response must contain {@link HttpStatus#SC_OK}.
     * <p>
     * If the apiUser is null, it uses the default APIServerConnector's user credentials and requests a
     * {@code grant_type} of "client_credentials". Otherwise it uses a copied APIServerConnector and sets the user
     * credentials according to apiUser.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param apiUser the APIUser
     * @return the token
     */
    public AccessToken postLogin(APIUser apiUser) {
        return postLogin(apiUser, "client_credentials");
    }

    /**
     * Sends a POST request to get a login token. The response must contain {@link HttpStatus#SC_OK}.
     * <p>
     * If the apiUser is null, it uses the default APIServerConnector's user credentials. Otherwise it uses a copied
     * APIServerConnector and sets the user credentials according to apiUser.
     * <p>
     * Requests the specified {@code grant_type}. (Default is "client_credentials"). If null, no grant_type is
     * requested.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param apiUser the APIUser
     * @param grant_type the requested grant_type
     * @return the token
     */
    public AccessToken postLogin(APIUser apiUser, String grant_type) {
        return postLogin(apiUser, grant_type, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to get a login token. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * If the apiUser is null, it uses the default APIServerConnector's user credentials. Otherwise it uses a copied
     * APIServerConnector and sets the user credentials according to apiUser.
     * <p>
     * Requests the specified {@code grant_type}. (Default is "client_credentials"). If null, no grant_type is
     * requested.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param apiUser the APIUser
     * @param grant_type the requested grant_type
     * @param expectedStatus the expected status code
     * @return the token
     */
    public AccessToken postLogin(APIUser apiUser, String grant_type, int expectedStatus) {
        return postLogin(apiUser, grant_type, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to get a login token. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * If the apiUser is null, it uses the provided APIServerConnector's user credentials. Otherwise it copies the
     * supplied APIServerConnector and sets the user credentials according to apiUser.
     * <p>
     * Requests the specified {@code grant_type}. (Default is "client_credentials"). If null, no grant_type is
     * requested.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param apiUser the APIUser
     * @param grant_type the requested grant_type
     * @param expectedStatus the expected status code
     * @param apiServerConnector the api server connector
     * @return the token
     */
    public AccessToken postLogin(APIUser apiUser, String grant_type, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "authentication/login";
        APIServerConnector asc = apiServerConnector.clone();
        String body = "";
        if (apiUser != null) {
            asc.setUserNameAndPassword(apiUser.username, apiUser.password);
        }
        if (grant_type != null) {
            asc.setContentType(ContentType.URLENC);
            body = "grant_type=" + grant_type;
        }

        response = asc.doPost(uri, body);
        assertReturnCode(response, expectedStatus);
        return AccessTokenFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a GET request to get the user info belonging to the given {@link APIUser#email}. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param apiUser the email provider
     * @return the complete APIUser
     */
    public APIUser getUserExists(APIUser apiUser) {
        return getUserExists(apiUser, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to get the user info belonging to the given {@link APIUser#email}. The response must contain
     * HTTP {@code expectedStatus}.
     *
     * @param apiUser the email provider
     * @param expectedStatus the expected HTTP status code
     * @return the complete APIUser
     */
    public APIUser getUserExists(APIUser apiUser, int expectedStatus) {
        return getUserExists(apiUser, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to get the user info belonging to the given {@link APIUser#email}. The response must contain
     * HTTP {@code expectedStatus}.
     *
     * @param apiUser the email provider
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the api server connector
     * @return the complete APIUser
     */
    public APIUser getUserExists(APIUser apiUser, int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "authentication/users/";
        if (apiUser.email != null) {
            uri += apiUser.email;
        }
        response = apiServerConnector.doGet(uri);
        assertReturnCode(response, expectedStatus);

        return APIUserFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a GET request to verify an access token. The response must contain {@link HttpStatus#SC_OK}.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param accessToken the access token to verify
     * @return a copy of the token returned from the server
     */
    public AccessToken getVerifyToken(AccessToken accessToken) {
        return getVerifyToken(accessToken, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to verify an access token. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param accessToken the access token to verify
     * @param expectedStatus the expected HTTP status
     * @return a copy of the token returned from the server
     */
    public AccessToken getVerifyToken(AccessToken accessToken, int expectedStatus) {
        return getVerifyToken(accessToken, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to verify an access token. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param accessToken the access token to verify
     * @param expectedStatus the expected HTTP status
     * @param apiServerConnector the server connector
     * @return a copy of the token returned from the server
     */
    public AccessToken getVerifyToken(AccessToken accessToken, int expectedStatus,
            APIServerConnector apiServerConnector) {
        String uri = "authentication/verifyToken";
        APIServerConnector asc = apiServerConnector.clone();
        asc.setAuthToken(accessToken.token_type, accessToken.access_token);
        asc.setUserNameAndPassword(null, null);
        response = asc.doGet(uri);
        assertReturnCode(response, expectedStatus);
        return AccessTokenFactory.createFromJSONString(response.jsonPath().prettify());
    }

    /**
     * Sends a GET request to logout. The response must contain {@link HttpStatus#SC_NO_CONTENT}.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param token the access token to invalidate
     * @return the response
     */
    public Response getLogout(AccessToken token) {
        return getLogout(token, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a GET request to logout. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param token the access token to invalidate
     * @param expectedStatus the expected status code
     * @return the response
     */
    public Response getLogout(AccessToken token, int expectedStatus) {
        return getLogout(token, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to logout. The response must contain HTTP {@code expectedStatus}.
     * <p>
     * It always copies the APIServerConnector before setting any additional fields.
     *
     * @param token the access token to invalidate
     * @param expectedStatus the expected status code
     * @param apiServerConnector the api server connector
     * @return the response
     */
    public Response getLogout(AccessToken token, int expectedStatus, APIServerConnector apiServerConnector) {
        String uri = "authentication/logout";
        APIServerConnector asc = apiServerConnector.clone();
        asc.setAuthToken(token.token_type, token.access_token);
        asc.setUserNameAndPassword(null, null);
        response = asc.doGet(uri);
        assertReturnCode(response, expectedStatus);
        return response;
    }

    ////////////////////////
    // favorites endpoint //
    ////////////////////////

    /**
     * Sends a GET request to favorites. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @return a list of all favorite IDs
     */
    public List<String> getFavorites() {
        response = apiServerConnector.doGet("favorites");
        assertReturnCode(response, HttpStatus.SC_OK);
        return response.jsonPath().get("experimentIDs");
    }

    /**
     * Sends a POST request to favorites. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experimentID the experiment's ID to add.
     * @return a list of all favorite IDs
     */
    public List<String> addFavorite(String experimentID) {
        Map<String, String> favMap = new HashMap<>();
        favMap.put("id", experimentID);
        response = apiServerConnector.doPost("favorites", favMap);
        assertReturnCode(response, HttpStatus.SC_OK);
        return response.jsonPath().get("experimentIDs");
    }

    /**
     * Sends a DELETE request to favorites. The response must contain {@link HttpStatus#SC_OK}.
     *
     * @param experimentID the experiment's ID to delete.
     * @return a list of remaining favorite IDs
     */
    public List<String> deleteFavorite(String experimentID) {
        response = apiServerConnector.doDelete("favorites/" + experimentID);
        assertReturnCode(response, HttpStatus.SC_OK);
        return response.jsonPath().get("experimentIDs");
    }

    ////////////////////////
    // authorization endpoint //
    ////////////////////////

    /**
     * Sends a DELETE request to delete a user role within an application
     *
     * @param userID - the userID of the user whose role we want to delete
     * @param applicationName - the applicationName of the application
     */
    public void deleteUserRole(String userID, String applicationName) {
        deleteUserRole(userID, applicationName, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Sends a DELETE request to delete a user role within an application
     *
     * @param userID - the userID of the user whose role we want to delete
     * @param applicationName - the applicationName of the application
     * @param expectedStatus - the expected HTTP status code
     */
    public void deleteUserRole(String userID, String applicationName, int expectedStatus) {
        deleteUserRole(userID, applicationName, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a DELETE request to delete a user role within an application
     *
     * @param userID - the userID of the user whose role we want to delete
     * @param applicationName - the applicationName of the application
     * @param expectedStatus - the expected HTTP status code
     * @param apiServerConnector - the server connector to use
     */
    public void deleteUserRole(String userID, String applicationName, int expectedStatus,
            APIServerConnector apiServerConnector) {

        response = apiServerConnector
                .doDelete("authorization/applications/" + applicationName + "/users/" + userID + "/roles");
        assertReturnCode(response, expectedStatus);
    }

    /**
     * Sends a POST request to assign roles for a list of users against list of applications.
     *
     * @param userID - the userID to whom we are going to assign application
     * @param appList- the list of applications to whom we gonna assign the user
     * @param role - the role of the user when assigning him to the application
     * @return list - the list of Assignment statuses
     */
    public List<AssignmentStatus> postUserRolePermission(String userID, List<Application> appList, String role) {
        List<UserRole> userRoleList = new ArrayList<>();
        for (Application appln : appList) {
            UserRole userRole = new UserRole();
            userRole.setApplicationName(appln.name);
            userRole.setUserID(userID);
            userRole.setRole(role);
            userRoleList.add(userRole);
        }
        return postUserRolePermission(userRoleList);
    }

    /**
     * Sends a GET request to get all the applications assigned to user. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param user the user whose list of applications we are interested
     * @return List of application names
     */
    public List<String> getUserApplications(APIUser user) {
        return getUserApplications(user, HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to get all the applications assigned to user. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param user the user whose list of applications we are interested
     * @param expectedStatus the expected HTTP status code
     * @return List of application names
     */
    public List<String> getUserApplications(APIUser user, int expectedStatus) {
        return getUserApplications(user, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a GET request to get all the applications assigned to user. The response must contain
     * {@link HttpStatus#SC_OK}.
     *
     * @param user the user whose list of applications we are interested
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return List of application names
     */
    public List<String> getUserApplications(APIUser user, int expectedStatus, APIServerConnector apiServerConnector) {
        apiServerConnector.setUserNameAndPassword(user.userId, user.password);
        response = apiServerConnector.doGet("authorization/applications");
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().get();
        List<String> applicationList = new ArrayList<>(jsonStrings.size());

        // here I am only interested in the name of the application
        for (int i = 0; i < jsonStrings.size(); i++) {
            String appName = response.jsonPath().get(Constants.ROLE_LIST + "[" + i + "].applicationName[0]").toString();
            applicationList.add(appName);
        }
        apiServerConnector.setUserNameAndPassword(appProperties.getProperty("user-name"),
                appProperties.getProperty("password"));
        return applicationList;
    }

    /**
     * Sends a POST request to assign roles to users against applications. The response must contain
     * {@link HttpStatus#SC_OK}. Sets createNewApplication to {@code true}.
     *
     * @param roles the list of userRoles
     * @return List of assignment status
     */
    public List<AssignmentStatus> postUserRolePermission(List<UserRole> roles) {
        return postUserRolePermission(roles, HttpStatus.SC_OK);
    }

    /**
     * Sends a POST request to assign roles to users against applications. The response must contain
     * {@link HttpStatus#SC_OK}. Sets createNewApplication to {@code true}.
     *
     * @param roles the list of userRoles
     * @param expectedStatus the expected HTTP status code
     * @return List of assignment status
     */
    public List<AssignmentStatus> postUserRolePermission(List<UserRole> roles, int expectedStatus) {
        return postUserRolePermission(roles, expectedStatus, apiServerConnector);
    }

    /**
     * Sends a POST request to assign roles to users against applications. The response must contain
     * {@link HttpStatus#SC_OK}.
     * <p>
     *
     * @param roles
     * @param expectedStatus the expected HTTP status code
     * @param apiServerConnector the server connector to use
     * @return List of assignment status
     */
    public List<AssignmentStatus> postUserRolePermission(List<UserRole> roles, int expectedStatus,
            APIServerConnector apiServerConnector) {
        // building the payload
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append("\"" + Constants.ROLE_LIST + "\":");
        stringBuilder.append(roles);
        stringBuilder.append("}");

        response = apiServerConnector.doPost("authorization/roles", stringBuilder.toString());
        assertReturnCode(response, expectedStatus);
        List<Map<String, Object>> jsonStrings = response.jsonPath().getList("assignmentStatuses");
        List<AssignmentStatus> assignmentStatusList = new ArrayList<>(jsonStrings.size());
        for (Map jsonMap : jsonStrings) {
            String jsonString = simpleGson.toJson(jsonMap);
            assignmentStatusList.add(AssignmentStatus.createFromJSONString(jsonString));
        }
        return assignmentStatusList;
    }

    ///////////
    // OTHER //
    ///////////

    /**
     * If the last response was an error message, this returns the message. Otherwise this returns the empty String..
     *
     * @return the last error message
     */
    public String lastError() {
        try {
            return response.jsonPath().get("error.message");
        } catch (IllegalArgumentException | JsonPathException e) {
            return "";
        }
    }

    /**
     * Cleans up all experiments currently in {@link #toCleanUp}. Empties the list.
     */
    @AfterTest
    protected void cleanUpExperiments() {
        for (Experiment experiment : toCleanUp) {
            response = apiServerConnector.doGet("experiments/" + experiment.id);
            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                continue;
            }
            Experiment realExperiment = ExperimentFactory.createFromJSONString(response.jsonPath().prettify());
            if (realExperiment != null && realExperiment.state != null) {
                switch (realExperiment.state) {
                    case Constants.EXPERIMENT_STATE_RUNNING: // fall through
                    case Constants.EXPERIMENT_STATE_PAUSED: // terminate and fall through
                        putExperiment(realExperiment.setState(Constants.EXPERIMENT_STATE_TERMINATED));
                    case Constants.EXPERIMENT_STATE_DRAFT: // fall through
                    case Constants.EXPERIMENT_STATE_TERMINATED:
                        response = apiServerConnector.doDelete("experiments/" + realExperiment.id);
                    case Constants.EXPERIMENT_STATE_DELETED:
                        // do nothing
                }
            }
        }
        toCleanUp.clear();
    }

    public boolean clearAssignmentsMetadataCache() {
        String uri = "assignments/clearMetadataCache";
        response = apiServerConnector.doPost(uri);
        assertReturnCode(response, HttpStatus.SC_OK);
        return true;
    }

    /**
     * This util method returns the timeZone offset with respect to UTC in HH:MM format
     * 
     * @param timeZone - the timeZone whose offset we want to calculate with respect to UTC timeZone
     * @return the String representation of the offset in HH:MM format
     */
    public String getTimeZoneOffSet(String timeZone) {
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        int offsetInMills = tz.getOffset(new Date().getTime());
        String offset = String.format("%02d:%02d", Math.abs(offsetInMills / 3600000),
                Math.abs((offsetInMills / 60000) % 60));
        offset = (offsetInMills >= 0 ? "+" : "-") + offset;

        return offset;
    }
    
    /**
     * This util method creates buckets to the experiment specified
     *
     * @param experiment - the experiment to which we want to assign the bucket
     * @param numberOfBucketsPerExperiment - number of buckets per each experiment
     */
    public void createBucketsToExperiment(Experiment experiment, int numberOfBucketsPerExperiment) {
 
        List<Bucket> bucketList = BucketFactory.createBuckets(experiment, numberOfBucketsPerExperiment);
        postBuckets(bucketList);
        
    }
}
