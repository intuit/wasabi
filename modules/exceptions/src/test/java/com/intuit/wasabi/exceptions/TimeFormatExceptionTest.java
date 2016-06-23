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

import org.junit.Test;

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.INVALID_TIME_FORMAT;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TimeFormatExceptionTest {

    @Test
    public void defaultConstructorTest() {
        TimeFormatException timeFormatException = new TimeFormatException("111111");

        assertThat(timeFormatException.toString(), is("com.intuit.wasabi.exceptions.TimeFormatException: " +
                "Invalid time format 111111.The correct time format is yyyy-MM-dd HH:mm:ss and it should " +
                "be URL encoded. For example, 2015-06-09 08:28:37 should be written as 2015-06-09%2008:28:37"));
        assertThat(timeFormatException.getErrorCode(), is(INVALID_TIME_FORMAT));
    }

    @Test
    public void throwableConstructorTest() {
        Throwable throwable = new Throwable("test");
        TimeFormatException timeFormatException = new TimeFormatException("2000", throwable);

        assertThat(timeFormatException.toString(), is("com.intuit.wasabi.exceptions.TimeFormatException: " +
                "Invalid time format 2000.The correct time format is yyyy-MM-dd HH:mm:ss and it should " +
                "be URL encoded. For example, 2015-06-09 08:28:37 should be written as 2015-06-09%2008:28:37"));
        assertThat(timeFormatException.getCause(), is(throwable));
        assertThat(timeFormatException.getErrorCode(), is(INVALID_TIME_FORMAT));
    }
}
