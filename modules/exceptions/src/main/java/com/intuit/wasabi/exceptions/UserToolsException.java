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
package com.intuit.wasabi.exceptions;

import static com.intuit.wasabi.exceptions.ErrorCode.USER_TOOLS_FAILED;

public class UserToolsException extends WasabiServerException {

    private static final long serialVersionUID = 253608364981436158L;

    public UserToolsException(String message) {
        this(message, null);
    }

    public UserToolsException(String message, Throwable rootCause) {
        super(USER_TOOLS_FAILED, message, rootCause);
    }
}
