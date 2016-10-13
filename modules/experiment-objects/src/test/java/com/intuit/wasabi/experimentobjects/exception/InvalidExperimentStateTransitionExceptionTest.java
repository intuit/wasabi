package com.intuit.wasabi.experimentobjects.exception;

import com.intuit.wasabi.exceptions.ErrorCode;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InvalidExperimentStateTransitionExceptionTest {

    @Test
    public void testWithMessage() {
        InvalidExperimentStateException e = new InvalidExperimentStateException("e1");

        assertThat(e.getErrorCode(), is(ErrorCode.INVALID_EXPERIMENT_STATE));
        assertThat(e.getDetailMessage(), is("e1"));
        assertThat(e.getCause(), is(nullValue()));
    }

    @Test
    public void testWithMessageAndException() {
        Exception r = new RuntimeException("r");
        InvalidExperimentStateException e = new InvalidExperimentStateException("e1", r);

        assertThat(e.getErrorCode(), is(ErrorCode.INVALID_EXPERIMENT_STATE));
        assertThat(e.getDetailMessage(), is("e1"));
        assertThat(e.getCause(), Is.<Throwable>is(r));
    }
}
