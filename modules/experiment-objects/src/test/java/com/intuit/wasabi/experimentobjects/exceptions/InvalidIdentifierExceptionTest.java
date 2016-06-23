package com.intuit.wasabi.experimentobjects.exceptions;

import org.hamcrest.core.Is;
import org.junit.Test;

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.INVALID_IDENTIFIER;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InvalidIdentifierExceptionTest {

    @Test
    public void testWithMessage() {
        InvalidIdentifierException e = new InvalidIdentifierException("e1");

        assertThat(e.getErrorCode(), is(INVALID_IDENTIFIER));
        assertThat(e.getDetailMessage(), is("e1"));
        assertThat(e.getCause(), is(nullValue()));
    }

    @Test
    public void testWithMessageAndException() {
        Exception r = new RuntimeException("r");
        InvalidIdentifierException e = new InvalidIdentifierException("e1", r);

        assertThat(e.getErrorCode(), is(INVALID_IDENTIFIER));
        assertThat(e.getDetailMessage(), is("e1"));
        assertThat(e.getCause(), Is.<Throwable>is(r));
    }
}
