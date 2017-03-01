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
import com.intuit.wasabi.exceptions.ApplicationNotFoundException;
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
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationNotFoundExceptionProviderTest {

    @Mock
    private HttpHeader httpHeader;
    @Mock
    private ExceptionJsonifier exceptionJsonifier;
    @Mock
    private ApplicationNotFoundException exception;
    @Mock
    private ResponseBuilder responseBuilder;
    @Mock
    private Response response;
    @Captor
    private ArgumentCaptor<Object> entity;
    private ApplicationNotFoundExceptionProvider applicationNotFoundExceptionProvider;

    @Before
    public void before() {
        applicationNotFoundExceptionProvider = new ApplicationNotFoundExceptionProvider(httpHeader, exceptionJsonifier);
    }

    @Test
    public void toResponse() throws Exception {
        when(httpHeader.headers(NOT_FOUND)).thenReturn(responseBuilder);
        when(responseBuilder.type(APPLICATION_JSON_TYPE)).thenReturn(responseBuilder);
        when(exception.getMessage()).thenReturn("error");
        when(exceptionJsonifier.serialize(NOT_FOUND, "error")).thenReturn("json");
        when(responseBuilder.entity("json")).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        applicationNotFoundExceptionProvider.toResponse(exception);

        verify(httpHeader).headers(NOT_FOUND);
        verify(exceptionJsonifier).serialize(NOT_FOUND, "error");
        verify(responseBuilder).type(APPLICATION_JSON_TYPE);
        verify(responseBuilder).entity("json");
        verify(responseBuilder).build();
    }
}
