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
package com.intuit.wasabi.tests.library.util;

import org.testng.IRetryAnalyzer;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A retry analyzer which can be used as a retry strategy for experiments. It gets the number of maximum tries from
 * {@link RetryTest#maxTries()}.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private Map<ITestNGMethod, AtomicInteger> methods = new HashMap<>();

    /**
     * Determines if the method should be retried or not.
     *
     * @param result The result of the test
     * @return true if the method should be retried
     */
    @Override
    public synchronized boolean retry(ITestResult result) {
        if (!methods.containsKey(result.getMethod())) {
            int maxTries = 0;
            Annotation[] annotations = result.getMethod().getConstructorOrMethod().getMethod().getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof RetryTest) {
                    maxTries = ((RetryTest) annotation).maxTries();
                }
            }
            methods.put(result.getMethod(), new AtomicInteger(maxTries));
        }
        return methods.get(result.getMethod()).getAndDecrement() > 1;
    }

}
