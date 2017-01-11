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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentExceptionTest {

    private String message = "AssignmentException error: ";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAssignmentException1() {
        thrown.expect(AssignmentException.class);
        throw new AssignmentException(message);
    }

    @Test
    public void testAssignmentException2() {
        thrown.expect(AssignmentException.class);
        throw new AssignmentException(message, new Throwable());
    }

}
