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
package com.intuit.wasabi.authentication.util;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.security.NoSuchAlgorithmException;

/**
 * Tests for {@link Retriable}.
 */
public class RetriableTest {

    /**
     * Tests {@link Retriable#retriableCall()}.
     *
     * @throws Exception expects a {@link NoSuchAlgorithmException}.
     */
    @Test(expected = NoSuchAlgorithmException.class)
    public void testRetriableCall() throws Exception {
        // Simple success
        Retriable<String> retriableString = new Retriable<String>() {
            @Override
            public String retriableCall() throws NoSuchAlgorithmException {
                return "TestString";
            }
        };
        Assert.assertEquals("TestString", retriableString.retriableCall());

        // Direct fail due to NoSuchAlgorithmException
        Retriable<Object> retriableThrows = new Retriable<Object>() {
            @Override
            public Object retriableCall() throws NoSuchAlgorithmException {
                throw new NoSuchAlgorithmException();
            }
        };
        try {
            retriableThrows.retriableCall();
            Assert.fail("retriableThrows does not throw!");
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Tests {@link Retriable#retryCallOnFail(int, long)} without retrials.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testRetryCallOnFailNoRetrials() throws Exception {
        // simple success
        Retriable<String> retriableString = new Retriable<String>() {
            @Override
            public String retriableCall() throws NoSuchAlgorithmException {
                return "TestString";
            }
        };
        Assert.assertEquals("TestString", retriableString.retryCallOnFail(1, 0));

        // direct fail due to NoSuchAlgorithmException
        Retriable<Object> retriableThrows = new Retriable<Object>() {
            @Override
            public Object retriableCall() throws NoSuchAlgorithmException {
                throw new NoSuchAlgorithmException();
            }
        };
        try {
            retriableThrows.retryCallOnFail(1, 0);
            Assert.fail("retriableThrows does not throw!");
        } catch (RetriableException e) {
            Assert.assertTrue("Expected exception cause is not NoSuchAlgorithmException",
                    e.getCause() instanceof NoSuchAlgorithmException);
            Assert.assertEquals("Feature not available.", e.getMessage());
        }
    }

    /**
     * Tests {@link Retriable#retryCallOnFail(int, long)} with retrials.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testRetryCallOnFailWithRetrials() throws Exception {
        // no timeout
        Retriable retriableFails = Mockito.mock(Retriable.class);
        Mockito.doThrow(new RuntimeException(new java.net.UnknownHostException("UnknownHostException"))).when(retriableFails).retriableCall();
        final int invocations3 = 3;
        final int timeout0 = 0;
        Mockito.when(retriableFails.retryCallOnFail(invocations3, timeout0)).thenCallRealMethod();

        try {
            retriableFails.retryCallOnFail(invocations3, timeout0);
            Assert.fail("retriableFails did not throw.");
        } catch (RetriableException e) {
            Assert.assertTrue("Expected exception cause for retriableFails is not UnknownHostException but"
                            + e.getCause().getCause().toString(),
                    e.getCause().getCause() instanceof java.net.UnknownHostException);
            Assert.assertEquals("Failed after " + invocations3 + " tries.", e.getMessage());
        }
        Mockito.verify(retriableFails, Mockito.times(invocations3)).retriableCall();
        Mockito.verify(retriableFails, Mockito.times(invocations3 - 1)).waitTimeout(timeout0);

        // with timeout
        Retriable retriableFailsTimeout = Mockito.mock(Retriable.class);
        Mockito.doThrow(new RuntimeException(new java.net.UnknownHostException("UnknownHostException"))).when(retriableFailsTimeout).retriableCall();
        final int invocations2 = 2;
        final int timeout5 = 5;
        Mockito.when(retriableFailsTimeout.retryCallOnFail(invocations2, timeout5)).thenCallRealMethod();

        try {
            retriableFailsTimeout.retryCallOnFail(invocations2, timeout5);
            Assert.fail("retriableFailsTimeout did not throw.");
        } catch (RetriableException e) {
            Assert.assertTrue("Expected exception cause for retriableFailsTimeout is not UnknownHostException but"
                            + e.getCause().getCause().toString(),
                    e.getCause().getCause() instanceof java.net.UnknownHostException);
            Assert.assertEquals("Failed after " + invocations2 + " tries.", e.getMessage());
        }
        Mockito.verify(retriableFailsTimeout, Mockito.times(invocations2)).retriableCall();
        Mockito.verify(retriableFailsTimeout, Mockito.times(invocations2 - 1)).waitTimeout(timeout5);
    }

    /**
     * Tests {@link Retriable#waitTimeout(long)}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testWaitTimeout() throws Exception {
        final Retriable<Object> retriable = new Retriable<Object>() {
            @Override
            public Object retriableCall() throws NoSuchAlgorithmException {
                return null;
            }
        };

        retriable.waitTimeout(0);
        retriable.waitTimeout(1);
    }

}
