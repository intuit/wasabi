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
package com.intuit.wasabi.experimentobjects.exceptions;

/**
 * Exceptions handling errors within the client e.g. validating experiments or buckets
 */
public abstract class WasabiClientException extends WasabiException {

    protected WasabiClientException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    protected WasabiClientException(ErrorCode errorCode, String detailMessage,
                                    Throwable cause) {
        super(errorCode, detailMessage, cause);
    }
}
