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

import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.slf4j.LoggerFactory.getLogger;

///////////////////////////////////////////////////////////////////////////

/**
 * This is a test to see how Jenkins reports failures.
 */
public class ForcedFailureTest {

    private static final Logger LOGGER = getLogger(ForcedFailureTest.class);

    ///////////////////////////////
    // Testing Jenkins reporting //
    ///////////////////////////////

    @Test()
    public void jenkins_report_failure_test() {
        LOGGER.info("=====> Temp experiment to force failure, want to see Jenkins correctly report it <======");
        Assert.assertFalse(true, "Forcing a failed assert");
    }

    @Test()
    public void jenkins_report_failure_test2() {
        LOGGER.info("=====> Temp experiment to force failure, want to see Jenkins correctly report it <======");
        Assert.assertFalse(true, "Forcing a failed assert");
    }

    @Test()
    public void jenkins_report_failure_test3() {
        LOGGER.info("=====> Temp experiment to force failure, want to see Jenkins correctly report it <======");
        Assert.assertFalse(true, "Forcing a failed assert");
    }

    @Test(dependsOnMethods = {"jenkins_report_failure_test"})
    public void jenkins_report_skip_me_test() {
        LOGGER.info("=====> Temp experiment to force skipped tests, want to see Jenkins correctly report it <======");
        Assert.assertFalse(true, "Should not run");
    }

    @Test(dependsOnMethods = {"jenkins_report_failure_test"})
    public void jenkins_report_skip_me_test2() {
        LOGGER.info("=====> Temp experiment to force skipped tests, want to see Jenkins correctly report it <======");
        Assert.assertFalse(true, "Should not run");
    }

    @Test(dependsOnMethods = {"jenkins_report_failure_test"})
    public void jenkins_report_skip_me_test3() {
        LOGGER.info("=====> Temp experiment to force skipped tests, want to see Jenkins correctly report it <======");
        Assert.assertFalse(true, "Should not run");
    }

    @Test(dependsOnMethods = {"jenkins_report_failure_test"})
    public void jenkins_report_skip_me_test4() {
        LOGGER.info("=====> Temp experiment to force skipped tests, want to see Jenkins correctly report it <======");
        Assert.assertFalse(true, "Should not run");
    }

}
