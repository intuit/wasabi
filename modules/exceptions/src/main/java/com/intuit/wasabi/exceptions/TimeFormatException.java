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

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.INVALID_TIME_FORMAT;


/**
 * Error catch for url entered time stamps
 */
public class TimeFormatException extends WasabiClientException {

    public TimeFormatException(String timeStamp) {
        this(timeStamp, null);
    }

    public TimeFormatException(String timeStamp, Throwable rootCause) {
        super(INVALID_TIME_FORMAT,
                "Invalid time format " + timeStamp + "." + "The correct time format is yyyy-MM-dd HH:mm:ss " +
                        "and it should be URL encoded. For example, 2015-06-09 08:28:37 should be " +
                        "written as 2015-06-09%2008:28:37", rootCause);
    }
}
