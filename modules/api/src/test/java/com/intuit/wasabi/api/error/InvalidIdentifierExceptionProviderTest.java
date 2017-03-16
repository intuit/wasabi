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
import com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InvalidIdentifierExceptionProviderTest {

    @Mock
    private HttpHeader httpHeader;
    private ExceptionJsonifier exceptionJsonifier;
    @Mock
    private InvalidIdentifierException invalidIdentifierException;
    @Mock
    private ResponseBuilder responseBuilder;
    @Mock
    private Response response;
    @Captor
    private ArgumentCaptor<Object> entity;
    private InvalidIdentifierExceptionProvider invalidIdentifierExceptionProvider;

    @Before
    public void before() {
        exceptionJsonifier = new ExceptionJsonifier();
        invalidIdentifierExceptionProvider = new InvalidIdentifierExceptionProvider(httpHeader, exceptionJsonifier);
    }

    @Test
    public void toResponse() throws Exception {
        when(httpHeader.headers(BAD_REQUEST)).thenReturn(responseBuilder);
        when(responseBuilder.type(APPLICATION_JSON_TYPE)).thenReturn(responseBuilder);
        when(invalidIdentifierException.getMessage()).thenReturn("error");
        when(responseBuilder.entity("{\"error\":{\"code\":400,\"message\":\"error\"}}")).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        invalidIdentifierExceptionProvider.toResponse(invalidIdentifierException);

        verify(httpHeader).headers(BAD_REQUEST);
        verify(responseBuilder).type(APPLICATION_JSON_TYPE);
        verify(responseBuilder).entity("{\"error\":{\"code\":400,\"message\":\"error\"}}");
        verify(responseBuilder).build();
    }
}
