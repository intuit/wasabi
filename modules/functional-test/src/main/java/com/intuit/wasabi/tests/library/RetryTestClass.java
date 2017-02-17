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

import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryListener;
import com.intuit.wasabi.tests.library.util.RetryTest;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A class to showcase test retrials.
 * <p>
 * The final (total) results should be:
 * (T)otal: 7, (P)ass: 4, (F)ail: 2, (S)kip: 1, (%S)uccessWithinPercentage: 0
 * <p>
 * Group results:
 */
@Listeners({RetryListener.class})
public class RetryTestClass {

    private static final Logger LOGGER = getLogger(RetryTestClass.class);
    /**
     * Will be used with {@link #retryTest()}.
     */
    private int counter = 0;
    /**
     * Will be used with {@link #retryTest2()}.
     */
    private int counter2 = 0;
    /**
     * Will be used with {@link #failRetryTest()}.
     */
    private int counter3 = 0;
    /**
     * Will be used by the {@link #retryWithWarmUpTest()}
     */
    private boolean warmupvariable = true;
    /**
     * Will be used to keep track of the numbers.
     */
    private PassFailResults pfr = new PassFailResults();

    /**
     * Will run before a method is invoked.
     *
     * @param method the method.
     */
    @BeforeMethod
    private void runBeforeMethod(Method method) {
        LOGGER.info("==============> Invoking " + method);
    }

    /**
     * Will run after a method was invoked.
     *
     * @param result the test result
     */
    @AfterMethod
    private void runAfterMethod(ITestResult result) {
        LOGGER.info(pfr.getResultString(result) + " <===============\n\n\n");
    }

    /**
     * Will be called for counter = {0, 1, 2} and always fail.
     * <p>
     * Expected result: F
     */
    @Test(retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3)
    public void retryTest() {
        LOGGER.info("retryTest -- counter: " + counter);
        Assert.assertTrue(counter++ > 2, "Smaller number than 3!");
    }

    /**
     * Will be called for counter2 = {0, 1, 2, 3, 4} and succeed on 2, thus not retrying the 4th and 5th times.
     * <p>
     * Expected result: P
     */
    @Test(retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 5, warmup = 100)
    public void retryTest2() {
        LOGGER.info("retryTest2 -- counter: " + counter2);
        Assert.assertTrue(counter2++ >= 2, "Smaller number than " + 2 + "!");
    }

    /**
     * Will always pass, but needs three seconds to warmup.
     * <p>
     * Expected result: P
     * Expected:       ~3 s warmup before invocation
     */
    @Test
    @RetryTest(warmup = 3000)
    public void warmUpTest() {
        LOGGER.info("warmUpTest -- always passing, should have ~3 s warmup.");
        Assert.assertTrue(true);
    }

    /**
     * A simple test which passes.
     * <p>
     * Expected result: P
     */
    @Test
    public void passTest() {
        LOGGER.info("passTest -- always passing.");
        Assert.assertTrue(true);
    }

    /**
     * Will warmup, then try twice and fail the first time.
     * <p>
     * Expected result: P
     */
    @Test(retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 2, warmup = 3000)
    public void retryWithWarmUpTest() {
        LOGGER.info("retryWithWarmUpTest -- fails once, passes then. ~3 s warmup before each try.");
        warmupvariable = !warmupvariable;
        Assert.assertTrue(warmupvariable, "WarmUpVariable not true");
    }

    /**
     * Will always fail after 2 tries.
     * <p>
     * Expected result: F
     */
    @Test(retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 2, warmup = 0)
    public void failRetryTest() {
        LOGGER.info("failRetryTest -- counter: " + ++counter3 + ". Always fails.");
        Assert.assertTrue(false);
    }

    /**
     * Will always be skipped.
     * <p>
     * Expected result: S
     */
    @Test(dependsOnMethods = {"failRetryTest"})
    public void skippedTest() {
        LOGGER.error("skippedTest -- This should not appear in your log, it means the skippedTest was not skipped!!");
        Assert.assertTrue(true);
    }


}
