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

/**
 * Tests for {@link RetriableException}.
 */
public class RetriableExceptionTest {

    /**
     * Tests {@link RetriableException#RetriableException(String)}.
     */
    @Test
    public void testMessageException() {
        try {
            throw new RetriableException("Some test exception");
        } catch (RetriableException e) {
            Assert.assertEquals(e.getMessage(), "Some test exception");
        }
    }


    /**
     * Tests {@link RetriableException#RetriableException(String, Throwable)}.
     */
    @Test
    public void testMessageThrowableException() {
        RuntimeException rte = new RuntimeException("RuntimeException");
        try {
            throw new RetriableException("Some test exception", rte);
        } catch (RetriableException e) {
            Assert.assertEquals("Some test exception", e.getMessage());
            Assert.assertEquals(rte, e.getCause());
        }

        try {
            throw new RetriableException("Some test exception", null);
        } catch (RetriableException e) {
            Assert.assertEquals("Some test exception", e.getMessage());
            Assert.assertEquals(null, e.getCause());
        }
    }

}
