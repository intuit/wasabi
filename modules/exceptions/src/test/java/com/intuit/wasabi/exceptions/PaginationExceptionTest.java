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
package com.intuit.wasabi.exceptions;

import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link PaginationException}
 */
public class PaginationExceptionTest {

    @Test
    public void testPaginationException() throws Exception {
        try {
            throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE, "PaginationException.");
        } catch (PaginationException e) {
            Assert.assertEquals(e.getMessage(), "PaginationException.");
            Assert.assertEquals(ErrorCode.FILTER_KEY_UNPROCESSABLE, e.getErrorCode());
        }
    }

    @Test
    public void testPaginationException2() throws Exception {
        try {
            try {
                throw new NullPointerException();
            } catch (NullPointerException cause) {
                throw new PaginationException(ErrorCode.SORT_KEY_UNPROCESSABLE, "Error!", cause);
            }
        } catch (PaginationException e) {
            Assert.assertEquals(e.getMessage(), "Error!");
            Assert.assertEquals(ErrorCode.SORT_KEY_UNPROCESSABLE, e.getErrorCode());
            Assert.assertEquals(e.getCause().getClass(), NullPointerException.class);
        }
    }
}
