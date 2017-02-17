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
import com.jayway.restassured.response.Response;
import org.slf4j.Logger;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class PathSetAssertHelper {

    private static final Logger LOGGER = getLogger(PathSetAssertHelper.class);
    private List<String> actualStrAL = new ArrayList<String>();
    private List<String> expectedStrAL = new ArrayList<String>();
    private Response response;

    /**
     * Instantiates an {@code PathSetAssertHelper} object.
     *
     * @param response the response object
     */
    public PathSetAssertHelper(Response response) {
        this.response = response;
    }

    // FIXME Abstract instead of copy-past
    // FIXME Improve try-catch: log error, set expected to ~ "No such path"
    // TODO Test with float, and with String
    // TODO Add Java Docs including usage example

    public void add(String path, String expectedValue) {
        expectedStrAL.add(path + ": " + expectedValue);
        String actualValue;
        try {
            actualValue = response.jsonPath().get(path);
        } catch (Exception e) {
            actualValue = "Error: Could not get value from path!";
            LOGGER.info("ERROR: " + e.toString());
        }
        actualStrAL.add(path + ": " + actualValue);
    }

    public void add(String path, Integer expectedValue) {
        expectedStrAL.add(path + ": " + expectedValue);
        String actualValue;
        Integer actualValueInt;
        try {
            actualValueInt = response.jsonPath().get(path);
            actualValue = actualValueInt.toString();
        } catch (Exception e) {
            actualValue = "Error: Could not get value from path!";
            LOGGER.info("ERROR: " + e.toString());
        }
        actualStrAL.add(path + ": " + actualValue);
    }

    public void add(String path, int expectedValue) {
        expectedStrAL.add(path + ": " + expectedValue);
        String actualValue;
        int actualValueInt;
        try {
            actualValueInt = response.jsonPath().get(path);
            actualValue = Integer.toString(actualValueInt);
        } catch (Exception e) {
            actualValue = "Error: Could not get value from path!";
            LOGGER.info("ERROR: " + e.toString());
        }
        actualStrAL.add(path + ": " + actualValue);
    }

    public void add(String path, Float expectedValue) {
        expectedStrAL.add(path + ": " + expectedValue);
        String actualValue;
        Float actualValueFloat;
        try {
            actualValueFloat = response.jsonPath().get(path);
            actualValue = Float.toString(actualValueFloat);
        } catch (Exception e) {
            actualValue = "Error: Could not get value from path!";
            LOGGER.info("ERROR: " + e.toString());
        }
        actualStrAL.add(path + ": " + actualValue);
    }

    public void doAssert() {
        StringBuffer actualSB = new StringBuffer();
        Iterator<String> itrActual = actualStrAL.iterator();
        while (itrActual.hasNext()) {
            actualSB.append(itrActual.next() + Constants.NEW_LINE);
        }

        StringBuffer expectedSB = new StringBuffer();
        Iterator<String> itrExpected = expectedStrAL.iterator();
        while (itrExpected.hasNext()) {
            expectedSB.append(itrExpected.next() + Constants.NEW_LINE);
        }

        Assert.assertEquals(actualSB.toString(), expectedSB.toString());
    }


}
