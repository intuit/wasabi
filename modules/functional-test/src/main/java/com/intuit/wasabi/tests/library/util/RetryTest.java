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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to allow retrials of @Test methods.
 * <p>
 * Testmethods should be annotated similar to this to be retried multiple times:
 * <pre>
 * {@code
 *  {@literal @}Test
 *  {@literal @}RetryTest(maxTries="5", timeout="0")
 *   public void myTestMethod() {
 *       ...
 *   }
 * }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryTest {

    /**
     * The number of maximum tries.
     *
     * @return maximum number of tries
     */
    int maxTries() default 1;

    /**
     * Warm-up time before a test.
     *
     * @return time to wait for warmup
     */
    long warmup() default 0;
}
