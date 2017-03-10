package com.intuit.wasabi.assignmentobjects;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created on 4/18/16.
 */
public class DateHourTest {
    private long dateLong = 10239129312L;

    @Test
    public void testBasicMethods() {
        Date seed = new Date(dateLong);
        DateHour date1 = new DateHour();
        DateHour date2 = new DateHour();
        date1.setDateHour(seed);
        date2.setDateHour(seed);

        assertThat(date1.getDayHour(), is(date2.getDayHour()));
        assertThat(date1.hashCode(), is(date2.hashCode()));
        assertThat(date1.equals(date2), is(true));

    }

}
