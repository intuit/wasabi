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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.intuit.wasabi.experimentobjects.Experiment;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentNotFoundExceptionTest {

    private Experiment.ID experimentID = Experiment.ID.newInstance();
    private Experiment.Label label = Experiment.Label.valueOf("testExp");
    private String message = "ExperimentNotFoundException error: ";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testExperimentNotFoundException1() {
        thrown.expect(ExperimentNotFoundException.class);
        throw new ExperimentNotFoundException(experimentID);
    }

    @Test
    public void testExperimentNotFoundException2() {
        thrown.expect(ExperimentNotFoundException.class);
        throw new ExperimentNotFoundException(label);
    }

    @Test
    public void testExperimentNotFoundException3() {
        thrown.expect(ExperimentNotFoundException.class);
        throw new ExperimentNotFoundException(message);
    }

}
