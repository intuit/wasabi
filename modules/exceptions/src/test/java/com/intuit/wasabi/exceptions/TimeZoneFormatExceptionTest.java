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

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.INVALID_TIME_ZONE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created on 3/3/16.
 */
public class TimeZoneFormatExceptionTest {


    @Test
    public void defaultConstructorTest() {
        TimeZoneFormatException timeFormatException = new TimeZoneFormatException("JST");

        assertThat(timeFormatException.toString(), is("com.intuit.wasabi.exceptions.TimeZoneFormatException: " +
                "Invalid time zone JST Please refer to the following link for valid time zones " +
                "http://tutorials.jenkov.com/java-date-time/java-util-timezone.html "));
        assertThat(timeFormatException.getErrorCode(), is(INVALID_TIME_ZONE));
    }

    @Test
    public void throwableConstructorTest() {
        Throwable throwable = new Throwable("test");
        TimeZoneFormatException
                timeFormatException = new TimeZoneFormatException("JST", throwable);
        assertThat(timeFormatException.toString(), is("com.intuit.wasabi.exceptions.TimeZoneFormatException: " +
                "Invalid time zone JST Please refer to the following link for valid time zones " +
                "http://tutorials.jenkov.com/java-date-time/java-util-timezone.html "));
        assertThat(timeFormatException.getCause(), is(throwable));
        assertThat(timeFormatException.getErrorCode(), is(INVALID_TIME_ZONE));
    }
}
