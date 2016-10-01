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
package com.intuit.wasabi.api.error;

import com.intuit.wasabi.api.HttpHeader;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

abstract class ExceptionProvider<T extends Throwable> implements ExceptionMapper<T> {

    private final Status status;
    private final MediaType type;
    private final HttpHeader httpHeader;
    private final ExceptionJsonifier exceptionJsonifier;

    public ExceptionProvider(@Nullable final Status status, final MediaType type, final HttpHeader httpHeader,
                             final ExceptionJsonifier exceptionJsonifier) {
        this.status = status;
        this.type = type;
        this.httpHeader = httpHeader;
        this.exceptionJsonifier = exceptionJsonifier;
    }

    @Override
    public Response toResponse(final T e) {
        Status responseStatus = this.status;

        if (null == responseStatus && e instanceof WasabiException) {
            responseStatus = getWasabiExceptionResponseStatus((WasabiException) e);
        }

        return httpHeader.headers(responseStatus)
                .type(type)
                .entity(serialize(responseStatus, e.getMessage()))
                .build();
    }

    private String serialize(final Status status, final String message) {
        return exceptionJsonifier.serialize(status, message);
    }

    <U extends WasabiException> Status getWasabiExceptionResponseStatus(final U e) {
        if (null != e.getErrorCode()) {
            return fromStatusCode(e.getErrorCode().getResponseCode());
        }
        return e instanceof WasabiClientException ? BAD_REQUEST : INTERNAL_SERVER_ERROR;
    }
}
