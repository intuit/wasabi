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

import com.intuit.wasabi.exceptions.ConstraintViolationException.Reason;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class ConstraintViolationExceptionTest {

    private String message = "ConstraintViolationException error: ";
    private Map<String, Object> properties;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        properties = new ConcurrentHashMap<String, Object>();
    }

    @Test
    public void testConstraintViolationException1() {
        thrown.expect(ConstraintViolationException.class);
        throw new ConstraintViolationException(Reason.APPLICATION_CONSTRAINT_VIOLATION);
    }

    @Test
    public void testConstraintViolationException2() {
        thrown.expect(ConstraintViolationException.class);
        ConstraintViolationException cve = new ConstraintViolationException(Reason.UNIQUE_CONSTRAINT_VIOLATION, message, properties);
        assertNotNull(cve.getReason());
        throw cve;
    }

}
