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
 * <tt>ServiceTestBase</tt> - a base class for Web Service API testing using TestNG and Rest-Assured.
 */


import com.jayway.restassured.response.Response;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

import static org.slf4j.LoggerFactory.getLogger;

///////////////////////////////////////////////////////////////////////////

public class ServiceTestBase {

    private static Logger logger;
    protected APIServerConnector apiServerConnector;
    protected PassFailResults passFailResults = new PassFailResults();
    /**
     * NOTE: {@code response} is global, so it can be logged from {@code runAfterMethod}
     */
    protected Response response;
    /**
     * Set this to limit the length of logged response contents. Negative values for no limit.
     */
    private int responseLogLengthLimit = -1;

    ////////////////////
    // Constructors   //
    ////////////////////

    /**
     * Initializes the ServiceTestBase without a limit for logged responses.
     */
    public ServiceTestBase() {
        this(-1);
    }

    /**
     * Initializes the ServiceTestBase with a specific character limit for the logged responses.
     *
     * @param responseLogLengthLimit the length limit for logged responses.
     */
    public ServiceTestBase(int responseLogLengthLimit) {
        this.responseLogLengthLimit = responseLogLengthLimit;

        logger = getLogger(this.getClass());
    }

    ////////////////////
    // Helper methods //
    ////////////////////

    public static void main(String[] args) {
        System.out.println("Usage: Run as TestNG tests...");
    }

    /**
     * Returns {@code response} as a String attempting to improve readability
     *
     * If body is empty return the empty string.
     *
     * @param response  the response returned by a REST API call
     * @return the response as a string
     */
    public String prettyResponse(Response response) {
        String prettyResponse = "";
        if (response != null && response.toString() != null && !response.toString().isEmpty()) {
            try {
                prettyResponse = response.jsonPath().prettify();
                prettyResponse = prettyResponse.replaceAll("\\\\n", System.getProperty("line.separator"));
                prettyResponse = prettyResponse.replaceAll("\\\\t", "\t");
            } catch (Exception e) {
                try {
                    prettyResponse = response.body().prettyPrint();
                    prettyResponse = prettyResponse.replaceAll("\\\\n", System.getProperty("line.separator"));
                    prettyResponse = prettyResponse.replaceAll("\\\\t", "\t");
                } catch (Exception ex) {
                    logger.error("Caught error trying to print response body. Exception: ", ex);
                    logger.error("response.toString() gives: " + response.toString());
                }
            }
        }
        return prettyResponse;
    }

    /**
     * Writes the response to log at the INFO level
     *
     * @param response  the response returned by a REST API call
     */
    public void infoLogResponse(Response response) {
        String prettyResponse = prettyResponse(response);
        if (responseLogLengthLimit > 0 && prettyResponse.length() > responseLogLengthLimit) {
            prettyResponse = prettyResponse.substring(0, responseLogLengthLimit) + "... (cut off for the log)";
        }
        logger.info("Response as string: " + prettyResponse);
    }

    /**
     * Asserts that the {@code response} has the {code expectedStatusCode}
     *
     * @param response  the response returned by a REST API call
     * @param expectedStatusCode  the status code expected in the response
     */
    public void assertReturnCode(Response response, int expectedStatusCode) {
        System.out.println("Response was: " + prettyResponse(response));
        int actualStatusCode = response.getStatusCode();

        Assert.assertEquals(actualStatusCode, expectedStatusCode,
                "HTTP Status Code. Was: '" + actualStatusCode
                        + "' Expected: '" + expectedStatusCode
                        + "'"
        );
    }

    //////////////////////
    // Before and After //
    //////////////////////

    /**
     * Sets the character limit for response logs.
     *
     * @param responseLogLengthLimit the character limit for response logs
     */
    protected void setResponseLogLengthLimit(int responseLogLengthLimit) {
        this.responseLogLengthLimit = responseLogLengthLimit;
        if (this.responseLogLengthLimit > 0) {
            logger.info("Response log length limited to " + this.responseLogLengthLimit);
        } else {
            logger.info("Response log length unlimited.");
        }
    }

    @BeforeClass
    protected void runBeforeClassBase() {
        logger.debug("======> Base BeforeClass: " + this.getClass().getName() + " <======");
    }

    @AfterClass
    protected void runAfterClassBase() {
        logger.info(passFailResults.getSummaryString());
    }

    /**
     * Will run before all annotated test methods.
     *
     * Prints the method name to the log.
     * Also, set the global {@code response} object to null o avoid that the
     * AfterMethod prints a prior methods response, if the failure/abort occurred before a new response is set.
     *
     * @param method TestNG will pass in the method about to be called.
     */
    @BeforeMethod
    protected void runBeforeMethodBase(Method method) {
        logger.info("======> Base Starting test: " + method.getName() + " <======");
        response = null;
    }


    //////////////////////
    // Main             //
    //////////////////////

    @AfterMethod
    protected void runAfterMethodBase(ITestResult result) throws Exception {
        logger.debug("======> Base AfterMethod <======");
        if (result.getStatus() == ITestResult.FAILURE) {
            infoLogResponse(response);
        }
        String summary = passFailResults.getResultString(result);
        logger.info(summary);
    }
}
