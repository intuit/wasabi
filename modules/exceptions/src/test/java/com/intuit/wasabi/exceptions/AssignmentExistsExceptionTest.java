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

import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentExistsExceptionTest {

    private Application.Name applicationName = Application.Name.valueOf("test_app");
    private User.ID userID = User.ID.valueOf("12345");
    private Experiment.Label experimentLabel = Experiment.Label.valueOf("testExp");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAssignmentExistsException1() {
        thrown.expect(AssignmentExistsException.class);
        throw new AssignmentExistsException(userID, applicationName, experimentLabel);
    }

    @Test
    public void testAssignmentExistsException2() {
        thrown.expect(AssignmentExistsException.class);
        throw new AssignmentExistsException(userID, applicationName, experimentLabel, new Throwable());
    }

}
