package com.intuit.wasabi.api.error;

import com.google.inject.Inject;
import com.intuit.hyrule.exceptions.InvalidConditionException;
import com.intuit.wasabi.api.HttpHeader;

import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * Created on 4/22/16.
 */
@Provider
public class InvalidConditionExceptionProvider extends ExceptionProvider<InvalidConditionException> {

    @Inject
    public InvalidConditionExceptionProvider(final HttpHeader httpHeader, final ExceptionJsonifier exceptionJsonifier) {
        super(BAD_REQUEST, APPLICATION_JSON_TYPE, httpHeader, exceptionJsonifier);
    }
}
