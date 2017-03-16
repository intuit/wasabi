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
package com.intuit.wasabi.tests.service.factory;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.service.StateInconsistencyTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Creates a number of tests.
 */
public class RepeatStateInconsistencyTestFactory extends TestBase {

    /**
     * Instantiates repetitionCount {@link StateInconsistencyTest} tests.
     *
     * @param repetitionCount the number of times to repeat
     * @return repetitionCount tests.
     */
    @Factory
    @Parameters("repetitionCount")
    public Object[] factoryMethod(@Optional("20") String repetitionCount) {
        Object[] tests = new Object[Integer.parseInt(repetitionCount)];
        for (int i = 0; i < tests.length; ++i) {
            tests[i] = new StateInconsistencyTest(ExperimentFactory.createExperiment());
        }
        return tests;
    }
}
