/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.email.exceptions;

import com.intuit.wasabi.exceptions.ErrorCode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link EmailException}
 */
public class EmailExceptionTest {

    @Test
    public void testEmailException() throws Exception {
        try {
            throw new EmailException("EmailException.");
        } catch (EmailException e) {
            Assert.assertTrue(e.getMessage().contains("EmailException."));
            Assert.assertEquals(ErrorCode.EMAIL_SERVICE_ERROR, e.getErrorCode());
        }
    }

    @Test
    public void testEmailException2() throws Exception {
        try {
            throw new EmailException(ErrorCode.EMAIL_NOT_ACTIVE_ERROR, "Error!");
        } catch (EmailException e) {
            Assert.assertTrue(e.getMessage().contains("Error!"));
            Assert.assertEquals(ErrorCode.EMAIL_NOT_ACTIVE_ERROR, e.getErrorCode());
        }
    }
}
