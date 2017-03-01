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

import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.INVALID_TIME_ZONE;


/**
 * Error catch for url entered time stamps
 */
public class TimeZoneFormatException extends WasabiClientException {

    public TimeZoneFormatException(String timeZoneString) {
        this(timeZoneString, null);
    }

    public TimeZoneFormatException(String timeZoneString, Throwable rootCause) {
        super(INVALID_TIME_ZONE,
                "Invalid time zone " + timeZoneString + " Please refer to the following link for valid time zones" +
                        " http://tutorials.jenkov.com/java-date-time/java-util-timezone.html ", rootCause);
    }
}
