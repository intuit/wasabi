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
package com.intuit.wasabi.tests.library;

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

///////////////////////////////////////////////////////////////////////////

/**
 * Deletes all experiments starting with specified prefixes.
 * <p>
 * You can add more prefixes via the testng_teardown.xml or by supplying the parameter
 * -Dapplication-prefixes=appprefix1|appprefix2|...|appprefixN
 * <p>
 * Applications starting with {@link Constants#DEFAULT_PREFIX_APPLICATION} are always deleted.
 */
public class TearDownTestExperiments extends TestBase {

    private static final Logger LOGGER = getLogger(TearDownTestExperiments.class);
    private String[] applicationPrefixes;
    private String appNameRegex;

    /**
     * Initializes a TearDownTestExperiments instance which deletes all experiments belonging to applications
     * starting with the given application prefixes or {@link Constants#DEFAULT_PREFIX_APPLICATION}.
     *
     * @param applicationPrefixes the application prefixes, a String containing the prefixes separated by |
     */
    @Parameters({"applicationPrefixes"})
    @BeforeClass
    public void init(@Optional String applicationPrefixes) {
        String appNameRegexPre = "experiments.findAll { e -> e.applicationName =~ /(";
        String appNameRegexPost = ").*/ }";
        String appNameRegexCore = Constants.DEFAULT_PREFIX_APPLICATION;

        if (applicationPrefixes != null && applicationPrefixes.length() > 0) {
            appNameRegexCore += "|" + applicationPrefixes;
        }

        String appApplicationPrefixes = appProperties.getProperty("application-prefixes", "");
        if (appApplicationPrefixes != null && appApplicationPrefixes.length() > 0) {
            appNameRegexCore += "|" + appApplicationPrefixes;
        }

        // prevent matching all prefixes
        if (appNameRegexCore.endsWith("|")) {
            appNameRegexCore = appNameRegexCore.substring(0, appNameRegexCore.length() - 1);
        }
        if (appNameRegexCore.startsWith("|")) {
            appNameRegexCore = appNameRegexCore.substring(1);
        }
        while (appNameRegexCore.contains("||")) {
            appNameRegexCore = appNameRegexCore.replace("||", "|");
        }

        this.applicationPrefixes = appNameRegexCore.split("\\|");
        this.appNameRegex = appNameRegexPre + appNameRegexCore + appNameRegexPost;

        LOGGER.info("About to delete experiments which belong to applications starting with any of "
                + Arrays.toString(this.applicationPrefixes));
    }

    /**
     * Deletes the experiments belonging to the tests.
     */
    @Test(dependsOnGroups = {"ping"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 5)
    public void deleteExperimentsInTest_API_Functional_Apps() {
        String url = "experiments?per_page=-1";
        response = apiServerConnector.doGet(url);

        List<Map<String, Object>> listOfExperiments = response.jsonPath().get(appNameRegex);

        if (listOfExperiments.size() == 0) {
            LOGGER.info("No experiments in applications left to delete.");
        } else {

            LOGGER.info("About to delete " + listOfExperiments.size() + " integration test experiments ....");
            for (Map<String, Object> temp : listOfExperiments) {
                Experiment currentExperiment = ExperimentFactory.createFromJSONString(new GsonBuilder().create().toJson(temp));

                boolean deleted = false;
                //  Extra pattern match - just to be safe before potentially running this on prod...
                for (String prefix : this.applicationPrefixes) {
                    if (currentExperiment.applicationName.startsWith(prefix)) {
                        LOGGER.info("Deleting Experiment " + currentExperiment);

                        // possible terminate first
                        if (!currentExperiment.state.equals(Constants.EXPERIMENT_STATE_DRAFT) && !currentExperiment.state.equals(Constants.EXPERIMENT_STATE_TERMINATED)) {
                            currentExperiment.state = Constants.EXPERIMENT_STATE_TERMINATED;
                            Experiment updatedExperiment = putExperiment(currentExperiment);
                            Assert.assertEquals(updatedExperiment.state, Constants.EXPERIMENT_STATE_TERMINATED, "State not changed.");
                        }

                        // Delete without checking the response, especially not the Status Code (could be 204 or 404)
                        response = apiServerConnector.doDelete("experiments/" + currentExperiment.id);
                        deleted = true;
                        break;
                    }
                }
                if (!deleted) {
                    Assert.fail("ABORT - Filter failed! Selected application name not in " + Arrays.toString(this.applicationPrefixes) + "!");
                }
            }
        }
    }

    /**
     * Checks if all experiments were deleted.
     */
    @Test(dependsOnMethods = {"deleteExperimentsInTest_API_Functional_Apps"})
    public void getTest_API_Functional_AppExperimentsExpectNone() {
        String url = "experiments?per_page=-1";
        response = apiServerConnector.doGet(url);
        List<Map<String, Object>> listOfExperiments = response.jsonPath().get(appNameRegex);
        Assert.assertEquals(listOfExperiments.size(), 0, "There are still integration test experiments left.");
    }

}
