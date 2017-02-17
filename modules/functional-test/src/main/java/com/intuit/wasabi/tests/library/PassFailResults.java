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

/**
 * An <tt>PassFailResults</tt> object accumulates the count of passed and failed tests and returns strings with the results fit for logging.
 *
 * @since September 13, 2014
 */

import org.slf4j.Logger;
import org.testng.ITestResult;

import static org.slf4j.LoggerFactory.getLogger;

public class PassFailResults {

    private static final Logger LOGGER = getLogger(PassFailResults.class);
    /** counts the totals */
    private int totalCount = 0;
    /** counts ITestResult.SUCCESS */
    private int passedCount = 0;
    /** counts ITestResult.FAILURE */
    private int failedCount = 0;
    /** counts ITestResult.SKIP */
    private int skippedCount = 0;
    /** counts ITestResult.SUCCESS_PERCENTAGE_FAILURE */
    private int percentCount = 0;
    private Long totalDurationMilliseconds = 0L;

    protected String statusString(int statusCode) {
        String strStatus;

        switch (statusCode) {
            case ITestResult.SUCCESS:
                strStatus = "PASSED";
                break;
            case ITestResult.FAILURE:
                strStatus = "FAILED";
                break;
            case ITestResult.SKIP:
                strStatus = "SKIPPED";
                break;
            case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                strStatus = "SUCCESS_PERCENTAGE";
                break;
            default:
                // NIT How to handle unexpected result status code?
                strStatus = "ERROR: unknown result status code: " + statusCode;
                break;
        }
        LOGGER.debug("Status string for code " + statusCode + " set to: " + strStatus);
        return strStatus;
    }

    /**
     * Takes the result from {@link org.testng.ITestResult} and adds it to the accumulated pass/fail rate. 
     * Then return a string formatted for printing with the tests pass/fail status and a condensed summary of results so far.
     *
     * @param result  The {@link org.testng.ITestResult} object from TestNG
     * @return The result string
     */
    public String getResultString(ITestResult result) {
        LOGGER.debug("getResultString");

        String methodName = result.getMethod().getMethodName();
        Long durationMilliSeconds = (result.getEndMillis() - result.getStartMillis());
        totalDurationMilliseconds = totalDurationMilliseconds + durationMilliSeconds;

        String status = statusString(result.getStatus());

        totalCount++;
        switch (status) {
            case "PASSED":
                passedCount++;
                break;
            case "FAILED":
                failedCount++;
                break;
            case "SKIPPED":
                skippedCount++;
                break;
            case "SUCCESS_PERCENTAGE":
                percentCount++;
                break;
            default:
                LOGGER.error("Unknown status: " + status);
        }

        return ("(T:" + totalCount + ",P:" + passedCount + ",F:" + failedCount + ",S:" + skippedCount + ",%S:" + percentCount + ") - "
                + status + " " + methodName
                + " - duration: " + durationMilliSeconds + " milliseconds");
    }


    /**
     * Return a string formatted for printing with a summary of results
     *
     * @return The summary string
     */
    public String getSummaryString() {

        Long averageDuration = totalDurationMilliseconds / totalCount;
        return ("Class complete with: T:" + totalCount +
                ",P:" + passedCount +
                ",F:" + failedCount +
                ",S:" + skippedCount +
                ",%S:" + percentCount +
                ". - Total duration: " + totalDurationMilliseconds + " milliseconds. Average duration: " + averageDuration + " milliseconds.");
    }

}
