package com.intuit.wasabi.api.error;

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
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FallbackExceptionProviderTest {

    @Mock
    private HttpHeader httpHeader;
    @Mock
    private ExceptionJsonifier exceptionJsonifier;
    @Mock
    private Exception exception;
    @Mock
    private ResponseBuilder responseBuilder;
    @Mock
    private Response response;
    @Captor
    private ArgumentCaptor<Object> entity;
    private FallbackExceptionProvider fallbackExceptionProvider;

    @Before
    public void before() {
        fallbackExceptionProvider = new FallbackExceptionProvider(httpHeader, exceptionJsonifier);
    }

    @Test
    public void toResponse() throws Exception {
        when(httpHeader.headers(INTERNAL_SERVER_ERROR)).thenReturn(responseBuilder);
        when(responseBuilder.type(APPLICATION_JSON_TYPE)).thenReturn(responseBuilder);
        when(exception.getMessage()).thenReturn("error");
        when(exceptionJsonifier.serialize(INTERNAL_SERVER_ERROR, "error")).thenReturn("json");
        when(responseBuilder.entity("json")).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        fallbackExceptionProvider.toResponse(exception);

        verify(httpHeader).headers(INTERNAL_SERVER_ERROR);
        verify(exceptionJsonifier).serialize(INTERNAL_SERVER_ERROR, "error");
        verify(responseBuilder).type(APPLICATION_JSON_TYPE);
        verify(responseBuilder).entity("json");
        verify(responseBuilder).build();
    }
}
