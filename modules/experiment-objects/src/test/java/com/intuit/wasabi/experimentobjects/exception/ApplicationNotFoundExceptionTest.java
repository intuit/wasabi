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
package com.intuit.wasabi.experimentobjects.exception;

import com.intuit.wasabi.experimentobjects.exception.ApplicationNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.intuit.wasabi.experimentobjects.Application;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationNotFoundExceptionTest {

    private Application.Name appName = Application.Name.valueOf("testApp");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testApplicationNotFoundException1() {
        thrown.expect(ApplicationNotFoundException.class);
        throw new ApplicationNotFoundException(appName);
    }

    @Test
    public void testApplicationNotFoundException2() {
        thrown.expect(ApplicationNotFoundException.class);
        throw new ApplicationNotFoundException(appName, new Throwable());
    }

}
