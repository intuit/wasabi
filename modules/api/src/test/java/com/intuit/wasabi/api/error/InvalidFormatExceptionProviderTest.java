package com.intuit.wasabi.api.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.intuit.wasabi.api.HttpHeader;
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
public class InvalidFormatExceptionProviderTest {

    @Mock
    private HttpHeader httpHeader;
    @Mock
    private ExceptionJsonifier exceptionJsonifier;
    @Mock
    private InvalidFormatException invalidFormatException;
    @Mock
    private ResponseBuilder responseBuilder;
    @Mock
    private Response response;
    @Captor
    private ArgumentCaptor<Object> entity;
    private InvalidFormatExceptionProvider invalidFormatExceptionProvider;

    @Before
    public void before() {
        invalidFormatExceptionProvider = new InvalidFormatExceptionProvider(httpHeader, exceptionJsonifier);
    }

    @Test
    public void toResponse() throws Exception {
        when(httpHeader.headers(BAD_REQUEST)).thenReturn(responseBuilder);
        when(responseBuilder.type(APPLICATION_JSON_TYPE)).thenReturn(responseBuilder);
        when(invalidFormatException.getMessage()).thenReturn("error");
        when(exceptionJsonifier.serialize(BAD_REQUEST, "error")).thenReturn("json");
        when(responseBuilder.entity("json")).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        invalidFormatExceptionProvider.toResponse(invalidFormatException);

        verify(httpHeader).headers(BAD_REQUEST);
        verify(exceptionJsonifier).serialize(BAD_REQUEST, "error");
        verify(responseBuilder).type(APPLICATION_JSON_TYPE);
        verify(responseBuilder).entity("json");
        verify(responseBuilder).build();
    }
}
