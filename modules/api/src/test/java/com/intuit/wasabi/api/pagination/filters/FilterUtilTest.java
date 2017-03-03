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
package com.intuit.wasabi.api.pagination.filters;

import com.intuit.wasabi.exceptions.PaginationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Tests for {@link FilterUtil}.
 */
public class FilterUtilTest {

    /**
     * Test object
     */
    private final PaginationFilter paginationFilter = new PaginationFilter() {
        @Override
        public boolean test(Object o) {
            return false;
        }
    };

    private Calendar testCalendar;

    @Before
    public void setup() throws Exception {
        paginationFilter.replaceFilter("", "");

        testCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        testCalendar.set(2015, Calendar.FEBRUARY, 28, 3, 54, 35);
        testCalendar.set(Calendar.MILLISECOND, 12);
    }

    @Test
    public void testFilterUtilCtor() throws Exception {
        new FilterUtil();
    }

    @Test
    public void testFilterModifier() throws Exception {
        Assert.assertEquals("APPEND_TIMEZONEOFFSET produces a wrong result with empty filter.",
                FilterUtil.TIMEZONE_SEPARATOR + paginationFilter.getTimeZoneOffset(),
                FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET.apply("", paginationFilter));

        Assert.assertEquals("APPEND_TIMEZONEOFFSET produces a wrong result with default timezone.",
                "filter" + FilterUtil.TIMEZONE_SEPARATOR + paginationFilter.getTimeZoneOffset(),
                FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET.apply("filter", paginationFilter));

        paginationFilter.replaceFilter("filter", "+4239");
        Assert.assertEquals("APPEND_TIMEZONEOFFSET produces a wrong result with a changed timezone.",
                "filter" + FilterUtil.TIMEZONE_SEPARATOR + "+4239",
                FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET.apply("filter", paginationFilter));
    }

    @Test
    public void testExtractTimeZoneAndTestDate() throws Exception {
        HashMap<String, Boolean> testCases = new HashMap<>();
        testCases.put("Feb 28" + FilterUtil.TIMEZONE_SEPARATOR + "+0000",
                true); // Feb 28, 2015 03:54:35 AM
        testCases.put("Feb 27" + FilterUtil.TIMEZONE_SEPARATOR + "-0700",
                true); // Feb 27, 2015 20:54:35 PM
        testCases.put("27, 2015" + FilterUtil.TIMEZONE_SEPARATOR + "+0700",
                false); // Feb 28, 2015 10:54:35 AM
        testCases.put("14:24" + FilterUtil.TIMEZONE_SEPARATOR + "+1030",
                true); // Feb 28, 2015 14:24:35 PM
        testCases.put("" + FilterUtil.TIMEZONE_SEPARATOR + "+1030",
                true); // Feb 28, 2015 14:24:35 PM
        testCases.put("AM" + FilterUtil.TIMEZONE_SEPARATOR + "-0700",
                false); // Feb 27, 2015 20:54:35 PM
        for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
            Assert.assertEquals("Test case handled incorrectly: " + testCase.getKey(),
                    testCase.getValue(),
                    FilterUtil.extractTimeZoneAndTestDate(testCalendar.getTime(), testCase.getKey()));
        }
    }

    @Test
    public void testConvertDateToOffsetDateTime() throws Exception {
        Assert.assertEquals("Date was not correctly converted",
                OffsetDateTime.of(
                        testCalendar.get(Calendar.YEAR),
                        testCalendar.get(Calendar.MONTH) + 1,
                        testCalendar.get(Calendar.DAY_OF_MONTH),
                        testCalendar.get(Calendar.HOUR_OF_DAY),
                        testCalendar.get(Calendar.MINUTE),
                        testCalendar.get(Calendar.SECOND),
                        (int) (testCalendar.get(Calendar.MILLISECOND) * 1e6),
                        ZoneOffset.of("+0000")
                ),
                FilterUtil.convertDateToOffsetDateTime(testCalendar.getTime()));
    }

    @Test
    public void testExtractTimeZone() throws Exception {
        String[] extracted = FilterUtil.extractTimeZone("myFilter" + FilterUtil.TIMEZONE_SEPARATOR + "+0234");
        Assert.assertEquals("Extracted values are not two!", 2, extracted.length);

        Assert.assertEquals("First value is not the original filter!", "myFilter", extracted[0]);
        Assert.assertEquals("Second value is not the original timezone!", "+0234", extracted[1]);
    }

    @Test
    public void testFormatDateTimeAsUI() throws Exception {
        HashMap<String, String> testCases = new HashMap<>();
        testCases.put("+0000", "Feb 28, 2015 03:54:35 AM");
        testCases.put("-0700", "Feb 27, 2015 20:54:35 PM");
        testCases.put("+0700", "Feb 28, 2015 10:54:35 AM");
        testCases.put("+1030", "Feb 28, 2015 14:24:35 PM");

        OffsetDateTime date = FilterUtil.convertDateToOffsetDateTime(testCalendar.getTime());
        for (Map.Entry<String, String> testCase : testCases.entrySet()) {
            Assert.assertEquals("Date not formatted correctly.",
                    testCase.getValue(),
                    FilterUtil.formatDateTimeAsUI(date, testCase.getKey()));
        }

        try {
            FilterUtil.formatDateTimeAsUI(date, " 0200");
            Assert.fail("Was able to format incorrect timezone.");
        } catch (PaginationException expected) {
            // pass
        }
    }

    @Test
    public void testParseUIDate() throws Exception {
        HashMap<String, OffsetDateTime> testCases = new HashMap<>();
        testCases.put("3/22/2014" + FilterUtil.TIMEZONE_SEPARATOR + "+0700",
                OffsetDateTime.of(2014, 3, 22, 0, 0, 0, 0, ZoneOffset.of("+0000")));
        testCases.put("03/22/2014" + FilterUtil.TIMEZONE_SEPARATOR + "+0330",
                OffsetDateTime.of(2014, 3, 22, 0, 0, 0, 0, ZoneOffset.of("+0000")));
        testCases.put("4/2/2005" + FilterUtil.TIMEZONE_SEPARATOR + "-1000",
                OffsetDateTime.of(2005, 4, 2, 0, 0, 0, 0, ZoneOffset.of("+0000")));
        testCases.put("12/02/2014" + FilterUtil.TIMEZONE_SEPARATOR + "-0700",
                OffsetDateTime.of(2014, 12, 2, 0, 0, 0, 0, ZoneOffset.of("+0000")));

        for (Map.Entry<String, OffsetDateTime> testCase : testCases.entrySet()) {
            String[] extracted = FilterUtil.extractTimeZone(testCase.getKey());
            Assert.assertEquals("Date not correctly parsed.",
                    testCase.getValue(),
                    FilterUtil.parseUIDate(extracted[0], extracted[1]));
        }

        testCases.clear();
        testCases.put("some string" + FilterUtil.TIMEZONE_SEPARATOR + "+0000",
                OffsetDateTime.now());
        testCases.put("5/4/12" + FilterUtil.TIMEZONE_SEPARATOR + "+0000",
                OffsetDateTime.now());
        testCases.put("5/4/412" + FilterUtil.TIMEZONE_SEPARATOR + "+0000",
                OffsetDateTime.now());
        testCases.put("1/0/12" + FilterUtil.TIMEZONE_SEPARATOR + "+0000",
                OffsetDateTime.now());
        testCases.put("1//-12" + FilterUtil.TIMEZONE_SEPARATOR + "+0000",
                OffsetDateTime.now());
        testCases.put("//-12" + FilterUtil.TIMEZONE_SEPARATOR + "+0000",
                OffsetDateTime.now());
        for (Map.Entry<String, OffsetDateTime> testCase : testCases.entrySet()) {
            String[] extracted = FilterUtil.extractTimeZone(testCase.getKey());
            try {
                OffsetDateTime result = FilterUtil.parseUIDate(extracted[0], extracted[1]);
                Assert.fail("No exception was thrown for " + testCase.getKey() + ", got: " + result);
            } catch (PaginationException ignored) {
                // expected
            } catch (Exception exception) {
                Assert.fail("Got unexpected exception for " + testCase.getKey() + ": " + exception);
            }
        }
    }
}
