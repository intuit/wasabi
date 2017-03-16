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
package com.intuit.wasabi.tests.library.util;

import org.slf4j.Logger;
import org.testng.IResultMap;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Allows to retry tests multiple times without their (eventually succeeded) failures being reported.
 * <p>
 * Annotate your test class with {@code @Listeners(value = RetryListener.class)} to enable this listener.
 * Addition
 */
public class RetryListener extends TestListenerAdapter {

    private static final Logger LOGGER = getLogger(RetryListener.class);
    private List<ITestNGMethod> retryMethods = new ArrayList<>();

    /**
     * Will pause the test depending on the {@link RetryTest#warmup()} time, if it was seen before.
     * That means tests are only paused on their second and subsequent tries.
     *
     * @param result the test result.
     */
    @Override
    public void onTestStart(ITestResult result) {
        Annotation[] annotations = result.getMethod().getConstructorOrMethod().getMethod().getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof RetryTest) {
                if (!retryMethods.contains(result.getMethod())) {
                    retryMethods.add(result.getMethod());
                } else {
                    long warmUp;
                    if ((warmUp = ((RetryTest) annotation).warmup()) > 0) {
                        LOGGER.info("WarmUp of " + warmUp + " milliseconds.");
                        long start = System.currentTimeMillis();
                        try {
                            Thread.sleep(warmUp);
                        } catch (InterruptedException e) {
                            LOGGER.warn("Interrupted while warming up.", e);
                        }
                        LOGGER.info("WarmUp took " + (System.currentTimeMillis() - start) + " milliseconds.");
                    }
                }
            }
        }
    }

    /**
     * This runs after the test class annotated with {@code @Listeners(value = RetryListener.class)}.
     * It will remove all tests annotated with {@link RetryTest} from the list of failed but within success percentage
     * tests.
     *
     * @param testContext the test context, will be passed by TestNG
     */
    @Override
    public void onFinish(ITestContext testContext) {
        IResultMap passedMap = testContext.getPassedTests();
        IResultMap skippedMap = testContext.getSkippedTests();
        IResultMap failedMap = testContext.getFailedTests();
        IResultMap failedButMap = testContext.getFailedButWithinSuccessPercentageTests();

        for (ITestNGMethod method : retryMethods) {
            if (passedMap.getAllMethods().contains(method)) {
                for (ITestResult result : testContext.getSkippedTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getSkippedTests().removeResult(result);
                    }
                }
                for (ITestResult result : testContext.getFailedTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getFailedTests().removeResult(result);
                    }
                }
                for (ITestResult result : testContext.getFailedButWithinSuccessPercentageTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getFailedButWithinSuccessPercentageTests().removeResult(result);
                    }
                }
                continue;
            }
            if (failedButMap.getAllMethods().contains(method)) {
                boolean failed = false;
                ITestResult failResult = null;
                for (ITestResult result : testContext.getFailedTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        if (!failed) {
                            failed = true;
                        } else {
                            testContext.getFailedTests().removeResult(result);
                        }
                    }
                }
                for (ITestResult result : testContext.getFailedButWithinSuccessPercentageTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getFailedButWithinSuccessPercentageTests().removeResult(result);
                        if (!failed && failResult == null) {
                            failResult = result;
                        }
                    }
                }
                for (ITestResult result : testContext.getPassedTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getPassedTests().removeResult(result);
                        if (!failed && failResult == null) {
                            failResult = result;
                        }
                    }
                }
                for (ITestResult result : testContext.getSkippedTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getSkippedTests().removeResult(result);
                        if (!failed && failResult == null) {
                            failResult = result;
                        }
                    }
                }
                if (!failed && failResult != null) {
                    testContext.getFailedTests().addResult(failResult, method);
                }
                continue;
            }
            if (failedMap.getAllMethods().contains(method)) {
                boolean firstFail = false;
                for (ITestResult result : testContext.getFailedTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        if (!firstFail) {
                            firstFail = true;
                        } else {
                            testContext.getFailedTests().removeResult(result);
                        }
                    }
                }
                for (ITestResult result : testContext.getPassedTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getPassedTests().removeResult(result);
                    }
                }
                for (ITestResult result : testContext.getSkippedTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getSkippedTests().removeResult(result);
                    }
                }
                for (ITestResult result : testContext.getFailedButWithinSuccessPercentageTests().getAllResults()) {
                    if (result.getMethod().equals(method)) {
                        testContext.getFailedButWithinSuccessPercentageTests().removeResult(result);
                    }
                }
            }
        }

        int total = passedMap.getAllMethods().size() + skippedMap.getAllMethods().size()
                + failedMap.getAllMethods().size() + failedButMap.getAllMethods().size();

        String search = "], ";
        String replace = "],\n\t";
        String resultString = ("\n\n\nINTEGRATION TESTS DETAILED RESULTS:"
                + "\n TOTAL TESTS: " + total
                + String.format("\n  %4d %-10s\n\t", passedMap.getAllMethods().size(), "passed:")
                + (passedMap.getAllMethods().size() == 0 ? "" : passedMap.getAllMethods())
                + String.format("\n  %4d %-10s\n\t", failedMap.getAllMethods().size(), "failed:")
                + (failedMap.getAllMethods().size() == 0 ? "" : failedMap.getAllMethods())
                + String.format("\n  %4d %-10s\n\t", skippedMap.getAllMethods().size(), "skipped:")
                + (skippedMap.getAllMethods().size() == 0 ? "" : skippedMap.getAllMethods())
                + String.format("\n  %4d %-10s\n\t", failedButMap.getAllMethods().size(), "failedBut:")
                + (failedButMap.getAllMethods().size() == 0 ? "" : failedButMap.getAllMethods())
                + "\n\n\n").replace(search, replace).replace("\t[", "\t").replace("]]", "]");
        LOGGER.info(resultString);
    }
}
