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

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * {@code TestPassFailResults} is mostly here as POC for how to add unit tests for helper functions.
 * <p>
 * There is no intent to add unit tests for the tests themselves.
 */
public class TestPassFailResults {

    private PassFailResults passFailResults = new PassFailResults();

    @DataProvider
    public static Object[][] statusCodeAndStringPairs() {
        return new Object[][]{
                {ITestResult.SUCCESS, "PASSED"},
                {ITestResult.FAILURE, "FAILED"},
                {ITestResult.SKIP, "SKIPPED"},
                {666, "ERROR: unknown result status code: 666"}
        };
    }


    @Test(dataProvider = "statusCodeAndStringPairs")
    public void testStatusString(int A, String B) {
        Assert.assertEquals(passFailResults.statusString(A), B);
    }

}
