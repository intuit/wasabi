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
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;

/**
 * Base exception for all server-side exceptions
 */
public abstract class WasabiServerException extends WasabiException {

    private static final long serialVersionUID = -1579632247543700865L;

    protected WasabiServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected WasabiServerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    protected WasabiServerException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    protected WasabiServerException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }
}
