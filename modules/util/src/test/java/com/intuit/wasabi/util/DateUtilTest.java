/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

public class DateUtilTest {

    @Test
    public void parseISOTimestamp_full() throws Exception {
        // 2013-09-15T13:00:00+0000 = 1379250000000L is number of ms since Epoch
        // To be locale independent, we are comparing the number of ms instead of date strings.
        Date date = DateUtil.parseISOTimestamp("2013-09-15T13:00:00+0000");

        assertThat("\"2013-09-15T13:00:00+0000\" == 2013-09-15 13:00:00 GMT", 1379250000000L, is(date.getTime()));
    }

    @Test
    public void parseISOTimestamp_condensed() throws Exception {
        // Timezone of parsed output should be unchanged
        Date date = DateUtil.parseISOTimestamp("2013-09-15T13:00:00");

        assertThat("\"2013-09-15T13:00:00\" == 2013-09-15 13:00:00 GMT", 1379250000000L, is(date.getTime()));
    }

    @Test
    public void parseISOTimestamp_dateOnly() throws Exception {
        // 1379203200000L = 2013-09-15T00:00:00+0000 ms since epoch
        Date date = DateUtil.parseISOTimestamp("2013-09-15");

        assertThat("\"2013-09-15\" == 2013-09-15 00:00:00", 1379203200000L, is(date.getTime()));
    }

    @Test
    public void parseISOTimestamp_impossible() throws Exception {
        Date date = DateUtil.parseISOTimestamp("2013-02-30T33:00:00");

        assertThat("Testing an impossible date", date, nullValue());
    }

    @Test
    public void parseISOTimestamp_wrongOrder() throws Exception {
        Date date = DateUtil.parseISOTimestamp("09-15-2013T13:00:00");

        assertThat("Testing out of order date", date, nullValue());
    }

    @Test
    public void parseISOTimestamp_testFail() throws Exception {
        assertThat(null, is(DateUtil.parseISOTimestamp("helloworld123$")));
    }

    @Test
    public void createCalendarMidnightTest() {
        Date date = new Date(1992312312L);
        Date expected = new Date(1987200000L);
        Calendar result = DateUtil.createCalendarMidnight(date);
        assertThat(result.getTime().getTime(), is(expected.getTime()));
    }

    @Test
    public void getUTCTimeTest() {
        String currentUTCTime = DateUtil.getUTCTime();
        System.out.println(currentUTCTime);
        assertThat(currentUTCTime, is(not(nullValue())));
    }
}
