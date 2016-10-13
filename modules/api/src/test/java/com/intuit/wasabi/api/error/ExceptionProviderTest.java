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
package com.intuit.wasabi.api.error;

import com.intuit.wasabi.api.HttpHeader;
import com.intuit.wasabi.exceptions.ErrorCode;
import com.intuit.wasabi.exceptions.WasabiClientException;
import com.intuit.wasabi.exceptions.WasabiException;
import com.intuit.wasabi.exceptions.WasabiServerException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ExceptionProviderTest {
    private ExceptionProvider<WasabiException> exceptionProvider;
    private String testIdentifier;
    private WasabiException exception;
    private Response.Status expectedResponseStatus;

    public ExceptionProviderTest(String testIdentifier, WasabiException exception, Response.Status expectedResponseStatus) {
        this.testIdentifier = testIdentifier;
        this.exception = exception;
        this.expectedResponseStatus = expectedResponseStatus;
    }

    @Parameterized.Parameters(name = "{index}: {0} -> {2}")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[]{"WasabiServerException ErrorCode != null",
                        new TestWasabiServerException(ErrorCode.ILLEGAL_ARGUMENT),
                        Response.Status.BAD_REQUEST},
                new Object[]{"WasabiServerException ErrorCode == null",
                        new TestWasabiServerException(null),
                        Response.Status.INTERNAL_SERVER_ERROR},
                new Object[]{"WasabiClientException ErrorCode != null",
                        new TestWasabiClientException(ErrorCode.ILLEGAL_ARGUMENT),
                        Response.Status.BAD_REQUEST},
                new Object[]{"WasabiClientException ErrorCode == null",
                        new TestWasabiClientException(null),
                        Response.Status.BAD_REQUEST},
                new Object[]{"WasabiException ErrorCode != null",
                        new TestWasabiException(ErrorCode.ILLEGAL_ARGUMENT),
                        Response.Status.BAD_REQUEST},
                new Object[]{"WasabiException ErrorCode == null",
                        new TestWasabiException(null),
                        Response.Status.INTERNAL_SERVER_ERROR}
        );
    }

    @Before
    public void setup() {
        exceptionProvider = new WasabiExceptionProvider(new HttpHeader("Application"), new ExceptionJsonifier());
    }

    @Test
    public void testGetWasabiExceptionResponseStatus() {
        Response response = exceptionProvider.toResponse(exception);
        Assert.assertEquals("Status code was incorrect.",
                expectedResponseStatus,
                Response.Status.fromStatusCode(response.getStatus()));
    }

    private static class TestWasabiServerException extends WasabiServerException {

        protected TestWasabiServerException(ErrorCode errorCode) {
            super(errorCode, "Unit test server exception.");
        }
    }

    private static class TestWasabiClientException extends WasabiClientException {

        protected TestWasabiClientException(ErrorCode errorCode) {
            super(errorCode, "Unit test client exception.");
        }
    }

    private static class TestWasabiException extends WasabiException {

        protected TestWasabiException(ErrorCode errorCode) {
            super(errorCode, "Unit test exception.");
        }
    }
}
