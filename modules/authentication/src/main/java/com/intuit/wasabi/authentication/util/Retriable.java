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

import org.slf4j.Logger;

import java.security.NoSuchAlgorithmException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A Retriable is used to retry authentication calls which can fail due to network errors or timeouts.
 *
 * A typical way to use it looks like this:
 * <pre>
 *     {@code
 *     int times = 5;
 *     int timeout = 200;
 *     Retriable<T> retriable = new Retriable<T>() {
 *        {@literal @}Override
 *         public T retriableCall() throws NoSuchAlgorithmException {
 *             return getSomeT();
 *         }
 *     };
 *     T retrievedT = retriable.retryCallOnFail(times, timeout);
 * }
 * </pre>
 * This will retry the {@code retriable} up to five times, with a pause of 200 milliseconds.
 *
 * @param <T> the return parameter of {@link #retriableCall()}
 */
public abstract class Retriable<T> {

    private static final Logger LOGGER = getLogger(Retriable.class);

    /**
     * A method which contains a retriable call.
     *
     * This method can throw a {@link NoSuchAlgorithmException} as it is meant to wrap calls from
     * {@link com.intuit.wasabi.authentication.impl.AuthenticationHelpers}s, which can throw these.
     *
     * @return An object of type T which should be retrieved with this call.
     * @throws NoSuchAlgorithmException if the method used in this call throws such an exception.
     */
    public abstract T retriableCall() throws NoSuchAlgorithmException;

    /**
     * Waits for specified number of milliseconds.
     *
     * @param timeout sleep time in milliseconds.
     * @throws RetriableException when sleep is interrupted
     */
    protected void waitTimeout(long timeout) throws RetriableException {
        if (timeout > 0) {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException interException) {
                LOGGER.debug("Interrupted during timeout", interException);
            }
        }
    }

    /**
     * Calls the {@link #retriableCall()} ('the call') up to {@code times} times and returns the result.
     *
     * If the call was successful, this method returns.
     * If the call was not successful due to a missing implementation (throws {@link NoSuchAlgorithmException}),
     * this method throws a {@link RetriableException}.
     * If the call was not successful due to an {@link java.net.UnknownHostException} and the number of trials does
     * not exceed {@code times}, this method retries the call after sleeping for {@code timeout} milliseconds.
     * If the call was not successful and/or the number of trials is exceeded, this method throws a
     * {@link RetriableException}.
     *
     * @param times the number of times this method should be called
     * @param timeout the timeout in milliseconds
     * @return the result of {@link #retriableCall()}
     * @throws RetriableException if {@link #retriableCall()} throws or the maximum number of trials was reached
     */
    public T retryCallOnFail(int times, long timeout) throws RetriableException {
        int trial = 0;
        while (true) {
            try {
                trial++;
                return retriableCall();
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause() instanceof java.net.UnknownHostException) {
                    if (trial == times) {
                        throw new RetriableException("Failed after " + times + " tries.", e);
                    }
                    LOGGER.debug("Retrying (" + (times - trial) + " tries left).");
                    waitTimeout(timeout);
                } else {
                    throw new RetriableException(e instanceof NoSuchAlgorithmException ? "Feature not available." : "Can not retry.", e);
                }
            }
        }
    }
}
