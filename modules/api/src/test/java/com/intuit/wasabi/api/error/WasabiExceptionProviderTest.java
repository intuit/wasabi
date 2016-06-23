package com.intuit.wasabi.api.error;

import com.intuit.wasabi.api.HttpHeader;
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ErrorCode.class})
public class WasabiExceptionProviderTest {

    @Mock
    private HttpHeader httpHeader;
    @Mock
    private ExceptionJsonifier exceptionJsonifier;
    @Mock
    private WasabiException wasabiException;
    @Mock
    private ErrorCode errorCode;
    @Mock
    private Response.ResponseBuilder responseBuilder;
    @Mock
    private Response response;
    @Captor
    private ArgumentCaptor<Object> entity;
    private WasabiExceptionProvider wasabiExceptionProvider;

    @Before
    public void before() {
        wasabiExceptionProvider = new WasabiExceptionProvider(httpHeader, exceptionJsonifier);
    }

    @Test
    public void toResponse() throws Exception {
        when(httpHeader.headers(any(Status.class))).thenReturn(responseBuilder);
        when(responseBuilder.type(APPLICATION_JSON_TYPE)).thenReturn(responseBuilder);
        when(wasabiException.getErrorCode()).thenReturn(errorCode);
        when(errorCode.getResponseCode()).thenReturn(INTERNAL_SERVER_ERROR.getStatusCode());
        when(wasabiException.getMessage()).thenReturn("error");
        when(exceptionJsonifier.serialize(INTERNAL_SERVER_ERROR, "error")).thenReturn("json");
        when(responseBuilder.entity("json")).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        wasabiExceptionProvider.toResponse(wasabiException);

        verify(httpHeader).headers(INTERNAL_SERVER_ERROR);
        verify(exceptionJsonifier).serialize(INTERNAL_SERVER_ERROR, "error");
        verify(responseBuilder).type(APPLICATION_JSON_TYPE);
        verify(responseBuilder).entity("json");
        verify(responseBuilder).build();
    }
}
