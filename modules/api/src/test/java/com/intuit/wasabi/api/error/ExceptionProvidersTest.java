package com.intuit.wasabi.api.error;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.intuit.hyrule.exceptions.InvalidConditionException;
import com.intuit.wasabi.api.HttpHeader;
import com.intuit.wasabi.exceptions.ApplicationNotFoundException;
import com.intuit.wasabi.exceptions.AssignmentNotFoundException;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.exceptions.InvalidProfileException;
import com.intuit.wasabi.exceptions.WasabiServerException;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiClientException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Provides a class to test ExceptionProviders.
 */
@RunWith(Parameterized.class)
public class ExceptionProvidersTest<P extends ExceptionProvider<E>, E extends Exception> {
    
    private static final String ERROR = "ExceptionProviderTest tested this exception.";
    protected static HttpHeader httpHeader = new HttpHeader("Application");
    protected static ExceptionJsonifier exceptionJsonifier = new ExceptionJsonifier();
    private final Response.Status expectedStatusCode;
    protected E exception;
    private P provider;

    /**
     * Mocks an exception for the provided class and stores the expected status code.
     *
     * @param provider           an instance of the exception provider to test
     * @param exceptionClass     the exception class to mock it properly
     * @param expectedStatusCode the expected status code for this exception
     */
    public ExceptionProvidersTest(P provider, Class<E> exceptionClass, Response.Status expectedStatusCode) {
        this.provider = provider;
        this.expectedStatusCode = expectedStatusCode;
        exception = Mockito.mock(exceptionClass);
        when(exception.getMessage()).thenAnswer(i -> ERROR);
        System.out.println(provider.getClass());
    }

    /**
     * Provides test cases for ExceptionProviders.
     *
     * @return a collection of test cases
     */
    @Parameters
    public static Collection<Object[]> testCases() {
        List<Object[]> testCases = new LinkedList<>();
        testCases.add(new Object[]{new ApplicationNotFoundExceptionProvider(httpHeader, exceptionJsonifier),
                ApplicationNotFoundException.class,
                Response.Status.NOT_FOUND});
        testCases.add(new Object[]{new AssignmentNotFoundExceptionProvider(httpHeader, exceptionJsonifier),
                AssignmentNotFoundException.class,
                Response.Status.NOT_FOUND});
        testCases.add(new Object[]{new AuthenticationExceptionProvider(httpHeader, exceptionJsonifier),
                AuthenticationException.class,
                Response.Status.UNAUTHORIZED});
        testCases.add(new Object[]{new ExperimentNotFoundExceptionProvider(httpHeader, exceptionJsonifier),
                ExperimentNotFoundException.class,
                Response.Status.NOT_FOUND});
        testCases.add(new Object[]{new FallbackExceptionProvider(httpHeader, exceptionJsonifier),
                Exception.class,
                Response.Status.INTERNAL_SERVER_ERROR});
        testCases.add(new Object[]{new IllegalArgumentExceptionProvider(httpHeader, exceptionJsonifier),
                IllegalArgumentException.class,
                Response.Status.BAD_REQUEST});
        testCases.add(new Object[]{new InvalidConditionExceptionProvider(httpHeader, exceptionJsonifier),
                InvalidConditionException.class,
                Response.Status.BAD_REQUEST});
        testCases.add(new Object[]{new InvalidFormatExceptionProvider(httpHeader, exceptionJsonifier),
                InvalidFormatException.class,
                Response.Status.BAD_REQUEST});
        testCases.add(new Object[]{new InvalidIdentifierExceptionProvider(httpHeader, exceptionJsonifier),
                InvalidIdentifierException.class,
                Response.Status.BAD_REQUEST});
        testCases.add(new Object[]{new InvalidProfileExceptionProvider(httpHeader, exceptionJsonifier),
                InvalidProfileException.class,
                Response.Status.BAD_REQUEST});
        testCases.add(new Object[]{new JsonMappingExceptionProvider(httpHeader, exceptionJsonifier),
                JsonMappingException.class,
                Response.Status.INTERNAL_SERVER_ERROR});
        testCases.add(new Object[]{new WebApplicationExceptionProvider(httpHeader, exceptionJsonifier),
                WebApplicationException.class,
                Response.Status.INTERNAL_SERVER_ERROR});
        testCases.add(new Object[]{new WasabiExceptionProvider(httpHeader, exceptionJsonifier),
                WasabiClientException.class,
                Response.Status.BAD_REQUEST});
        testCases.add(new Object[]{new WasabiExceptionProvider(httpHeader, exceptionJsonifier),
                WasabiServerException.class,
                Response.Status.INTERNAL_SERVER_ERROR});
        return testCases;
    }

    @Test
    public void testToResponse() throws IOException {
        Response response = provider.toResponse(exception);

        Assert.assertEquals("Status code was incorrect.",
                expectedStatusCode,
                Response.Status.fromStatusCode(response.getStatus()));
        Assert.assertEquals("Content-Type should always be application/json ()",
                MediaType.APPLICATION_JSON_TYPE,
                response.getMetadata().getFirst("Content-Type"));

        Map<String, Map<String, Object>> result =
                new ObjectMapper().readValue((String) response.getEntity(),
                        new TypeReference<Map<String, Map<String, Object>>>() {
                        });

        Assert.assertTrue("Entity does not contain 'error' key", result.containsKey("error"));

        Map<String, Object> error = result.get("error");
        Assert.assertEquals("Entity's 'error.code' does not match the response status.",
                error.get("code"),
                response.getStatus());
        Assert.assertEquals("Entity's 'error.message' does not match the exception's error message.",
                error.get("message"),
                ERROR);
    }

}
