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


import com.intuit.wasabi.exceptions.WasabiClientException;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.Date;

import static com.intuit.wasabi.exceptions.ErrorCode.END_TIME_PASSED;

/**
 * Indicates that an experiment has passed its end time.
 */
public class EndTimeHasPassedException extends WasabiClientException {

    private static final long serialVersionUID = 5387714991480783130L;

    public EndTimeHasPassedException(Experiment.ID experimentID, Date endTime) {
        this(experimentID, endTime, null);
    }

    public EndTimeHasPassedException(Experiment.ID experimentID, Date endTime,
                                     Throwable rootCause) {
        super(END_TIME_PASSED,
                "Experiment \"" + experimentID + "\" has passed its end time of \"" +
                        endTime + "\".", rootCause);
    }
}
