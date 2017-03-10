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

import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;

/**
 * Should be thrown if an experiment should be updated or created and has no hypothesis.
 */
public class ExperimentMissingHypothesisException extends WasabiClientException {

    /**
     * Should be thrown if an experiment should be updated or created and has no hypothesis.
     *
     * @param errorCode the error code (usually INVALID_ARGUMENT)
     * @param detailMessage a detailed error message which allows the end user to debug
     */
    public ExperimentMissingHypothesisException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

}

