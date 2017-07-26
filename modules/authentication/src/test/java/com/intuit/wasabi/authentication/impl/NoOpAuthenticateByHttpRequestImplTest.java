package com.intuit.wasabi.authentication.impl;

import com.intuit.wasabi.authentication.AuthenticateByHttpRequest;
import com.sun.jersey.api.core.HttpRequestContext;
import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

public class NoOpAuthenticateByHttpRequestImplTest {

    @Test
    public void authenticateTest() {
        HttpRequestContext httpRequestContextMock = Mockito.mock(HttpRequestContext.class);
        AuthenticateByHttpRequest authenticateByHttpRequest = new NoOpAuthenticateByHttpRequestImpl();
        Map<String, String> infoFromAuthMap = authenticateByHttpRequest.authenticate(httpRequestContextMock);
        TestCase.assertNotNull(infoFromAuthMap);
        TestCase.assertTrue(infoFromAuthMap.isEmpty());
    }
}
