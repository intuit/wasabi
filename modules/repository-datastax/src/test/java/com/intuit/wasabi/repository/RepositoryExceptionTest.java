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
package com.intuit.wasabi.repository;

import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RepositoryExceptionTest {

    @Test
    public void testThrowable() {
        RepositoryException exception = new RepositoryException(new RuntimeException());
        assertEquals(RuntimeException.class, exception.getCause().getClass());
        assertEquals(ErrorCode.REPOSITORY_ERROR, exception.getErrorCode());

    }

    @Test
    public void testMessage() {
        RepositoryException exception = new RepositoryException("test");
        assertEquals(null, exception.getCause());
        assertEquals(ErrorCode.REPOSITORY_ERROR, exception.getErrorCode());
        assertEquals("test", exception.getMessage());

    }

    @Test
    public void testDefault() {
        RepositoryException exception = new RepositoryException();
        assertEquals(null, exception.getCause());
        assertEquals(ErrorCode.REPOSITORY_ERROR, exception.getErrorCode());
        assertEquals(null, exception.getMessage());

    }

    @Test
    public void testMessageAndThrowable() {
        RepositoryException exception = new RepositoryException("test", new RuntimeException());
        assertEquals(RuntimeException.class, exception.getCause().getClass());
        assertEquals(ErrorCode.REPOSITORY_ERROR, exception.getErrorCode());
        assertEquals("test", exception.getMessage());

    }
}
