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

import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Application;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.Page;
import com.intuit.wasabi.tests.model.factory.ApplicationFactory;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.PageFactory;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;
import static org.slf4j.LoggerFactory.getLogger;

///////////////////////////////////////////////////////////////////////////

/**
 * <tt>SetupPerformanceTest</tt> can be used to setup E experiments for one application, each with B buckets, and each assigned as running on the same page, P.
 * <p>
 * All parameters can be passed in. To pass in new parameters use the -Dparameter-name switches to set the appProperties.
 * The available switches and their default values are: ({@code parametername: defaultvalue})
 * <p>
 * <ul>
 * <li>application.name: SW50ZWdyVGVzdA_Application_Perf-</li>
 * <li>experiment.prefix: SW50ZWdyVGVzdA_Experiment_Perf-</li>
 * <li>experiment.count: 3</li>
 * <li>page.name: SW50ZWdyVGVzdA_Page_Perf-</li>
 * <li>bucket.prefix: SW50ZWdyVGVzdA_Bucket_Perf-</li>
 * <li>buckets.in.experiment: 1</li>
 * <li>mutualexclusions: false</li>
 * </ul>
 * <p>
 * Additionally the parameter {@code mutualexclusions} is available. If it is set to {@code true}, all experiments
 * are mutual exclusive to each other. By default they are not.
 * <p>
 * NOTE: passing in the string "NULL" for the page name results in that the experiments will not be associated with any page.
 * <p>
 * Priority will automatically be in order of creation,
 * which will be in same order as the experiment names:
 * &lt;experimentPrefix&gt;1
 * &lt;experimentPrefix&gt;2
 * ...
 * &lt;experimentPrefix&gt;m
 * <p>
 * Bucket names will be:
 * &lt;bucketPrefix&gt;1
 * &lt;bucketPrefix&gt;2
 * ...
 * &lt;bucketPrefix&gt;n
 * <p>
 * Allocation for the buckets will be as follows:
 * With n = bucketCount
 * allocate 1/(n+1) of 100% to each non-control bucket
 * and give the reminder ~ 2/(n+1) of 100% to the control bucket
 */
public class SetupPerformanceTest extends TestBase {

    private static final Logger LOGGER = getLogger(SetupPerformanceTest.class);
    private int experimentCount = 3; // default
    private int bucketCount = 1;     // default
    private boolean mutualExclusive = false;
    private String pageName = Constants.DEFAULT_PREFIX_PAGE + "Perf";
    private String experimentPrefix = Constants.DEFAULT_PREFIX_EXPERIMENT + "Perf-";
    private String bucketPrefix = Constants.DEFAULT_PREFIX_BUCKET + "Perf-";
    private String applicationName = Constants.DEFAULT_PREFIX_APPLICATION + "Perf";
    private List<Experiment> experiments;
    private Application application;
    private Page page;

    //////////////////////
    // Before and After //
    //////////////////////

    /**
     * Initializes private variables.
     */
    @BeforeClass
    protected void init() {
        LOGGER.info("Init: " + this.getClass().getName());

        setPropertyFromSystemProperty("application.name", "application-name");
        setPropertyFromSystemProperty("experiment.prefix", "experiment-prefix");
        setPropertyFromSystemProperty("experiment.count", "experiment-count");
        setPropertyFromSystemProperty("page.name", "page-name");
        setPropertyFromSystemProperty("buckets.prefix", "buckets-prefix");
        setPropertyFromSystemProperty("buckets.in.experiment", "buckets-in-experiment");
        setPropertyFromSystemProperty("mutualexclusions", "mutualexclusions");
        setPropertyFromSystemProperty("database.url", "database-url");
        setPropertyFromSystemProperty("database.username", "database-username");
        setPropertyFromSystemProperty("database.password", "database-password");

        applicationName = appProperties.getProperty("application-name", applicationName);
        experimentPrefix = appProperties.getProperty("experiment-prefix", experimentPrefix);
        experimentCount = Integer.valueOf(appProperties.getProperty("experiment-count", String.valueOf(experimentCount)));
        pageName = appProperties.getProperty("page-name", Constants.DEFAULT_PAGE_NAME);
        bucketPrefix = appProperties.getProperty("bucket-prefix", bucketPrefix);
        bucketCount = Integer.valueOf(appProperties.getProperty("buckets-in-experiment", String.valueOf(bucketCount)));
        mutualExclusive = Boolean.valueOf(appProperties.getProperty("mutualexclusions", String.valueOf(mutualExclusive)));

        LOGGER.info("KVP used: applicationName=" + applicationName);
        LOGGER.info("KVP used: experimentCount=" + experimentCount);
        LOGGER.info("KVP used: bucketCount=" + bucketCount);
        LOGGER.info("KVP used: pageName=" + pageName);
        LOGGER.info("KVP used: experimentPrefix=" + experimentPrefix);
        LOGGER.info("KVP used: bucketPrefix=" + bucketPrefix);

        application = ApplicationFactory.createApplication().setName(applicationName);
        page = PageFactory.createPage().setName(pageName);
        experiments = new ArrayList<>(experimentCount);
        ExperimentFactory.createExperiment().setSerializationStrategy(new DefaultNameExclusionStrategy("creationTime", "modificationTime"));
    }

    ////////////////////
    // Helper methods //
    ///////////////////

    /**
     * Provides an iterator of indices.
     *
     * @return an iterator providing indices
     */
    @DataProvider(name = "repeatDataProvider")
    public Iterator<Object[]> repeatDataProvider() {
        Collection<Object[]> dp = new ArrayList<>(experimentCount);
        for (int count = 0; count < experimentCount; count++) {
            dp.add(new Object[]{count});
        }
        return dp.iterator();
    }

    ///////////////
    // The Tests //
    ///////////////

    /**
     * Creates an experiment.
     *
     * @param index the experiment index
     */
    @Test(dependsOnGroups = {"ping"}, dataProvider = "repeatDataProvider")
    public void t1_createExperiment(int index) {
        experiments.add(index, ExperimentFactory.createExperiment()
                .setApplication(application));
        experiments.get(index).update(postExperiment(experiments.get(index), HttpStatus.SC_CREATED));
        LOGGER.info("Created experiment " + experiments.get(index));
        postExperimentPriority(experiments.get(index), 3);
    }

    /**
     * Creates several buckets.
     *
     * @param index the index to reference the experiment
     */
    @Test(dependsOnMethods = {"t1_createExperiment"}, dataProvider = "repeatDataProvider")
    public void t2_createBuckets(int index) {
        if (bucketCount < 1) {
            LOGGER.warn("bucketCount < 1, setting it to 1!");
            bucketCount = 1;
        }

        double[] allocPercentages = new double[bucketCount];
        String[] labels = new String[bucketCount];
        for (int i = 1; i < bucketCount; ++i) {
            allocPercentages[i] = Math.round(100.d / (bucketCount + 1)) / 100.d;
            labels[i] = bucketPrefix + (i + 1);
        }
        // this is for the control bucket (automatically created as control by the factory to be control)
        allocPercentages[0] = Math.round(100.0d - (100.0d * Math.round(100.d / (bucketCount + 1)) / 100.d * (bucketCount - 1))) / 100.0d;
        labels[0] = bucketPrefix + "1";

        List<Bucket> buckets = BucketFactory.createCompleteBuckets(experiments.get(index), allocPercentages, labels);
        List<Bucket> responseBuckets = postBuckets(buckets);
        assertEqualModelItems(responseBuckets, buckets);
    }


    /**
     * Creates pages.
     *
     * @param index the experiment index
     */
    @Test(dependsOnMethods = {"t2_createBuckets"}, dataProvider = "repeatDataProvider")
    public void t3_createPages(int index) {
        if (pageName.equals("NULL")) {
            LOGGER.info("Page name is \"NULL\", experiment " + experiments.get(index).label + " not added to any page.");
        } else {
            postPages(experiments.get(index), page);
        }
    }

    /**
     * If mutual exclusions should be used, then this test creates mutual exclusions.
     */
    @Test(dependsOnMethods = {"t3_createPages"})
    public void t4_mutualExclusions() {
        if (!mutualExclusive) {
            LOGGER.info("Experiments are not mutually exclusive, test will automatically pass..");
            Assert.assertTrue(true);
            return;
        }
        postExclusions(experiments);
    }

    /**
     * Starts all experiments.
     *
     * @param index the index for each experiment.
     */
    @Test(dependsOnMethods = {"t4_mutualExclusions"}, dataProvider = "repeatDataProvider")
    public void t5_setFromDraftToRunningState(int index) {
        experiments.get(index).setState(Constants.EXPERIMENT_STATE_RUNNING);
        Experiment experiment = putExperiment(experiments.get(index));
        assertEqualModelItems(experiment, experiments.get(index));
    }

}
