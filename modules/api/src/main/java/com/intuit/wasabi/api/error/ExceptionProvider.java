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
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Objects;

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

        if (responseStatus == null && e instanceof WasabiException) {
            responseStatus = fromStatusCode(((WasabiException) e).getErrorCode().getResponseCode());
        }

        return httpHeader.headers(responseStatus)
                .type(type)
                .entity(serialize(responseStatus, e))
                .build();
    }

    private String serialize(final Status status, final Throwable exception) {
        Throwable exceptionCause = exception;
        while (Objects.isNull(exceptionCause.getMessage()) && Objects.nonNull(exceptionCause.getCause())) {
            exceptionCause = exceptionCause.getCause();
        }
        return exceptionJsonifier.serialize(status, exceptionCause.getMessage());
    }
}
