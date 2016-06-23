package com.intuit.wasabi.exceptions;

import org.hamcrest.core.Is;
import org.junit.Test;

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.ANALYTICS_FAILED;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AnalyticsExceptionTest {

    @Test
    public void testWithMessage() {
        AnalyticsException e = new AnalyticsException("e1");

        assertThat(e.getErrorCode(), is(ANALYTICS_FAILED));
        assertThat(e.getMessage(), is("e1"));
        assertThat(e.getCause(), is(nullValue()));
    }

    @Test
    public void testWithMessageAndException() {
        Exception r = new RuntimeException("r");
        AnalyticsException e = new AnalyticsException("e1", r);

        assertThat(e.getErrorCode(), is(ANALYTICS_FAILED));
        assertThat(e.getMessage(), is("e1"));
        assertThat(e.getCause(), Is.<Throwable>is(r));
    }
}
