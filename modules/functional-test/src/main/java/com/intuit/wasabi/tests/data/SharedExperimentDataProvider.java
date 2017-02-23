/*
 * ******************************************************************************
 *  * Copyright 2016 Intuit
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package com.intuit.wasabi.tests.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Test
public class SharedExperimentDataProvider extends CombinableDataProvider {
    private final Logger LOGGER = LoggerFactory.getLogger(SharedExperimentDataProvider.class);
    public final static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-0000");
    public final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss-0000");
    public final static long time = new Date().getTime();
    public final static LocalDateTime dateTime = LocalDateTime.now();
    public final static LocalDateTime todayDT = dateTime.withHour(0).withMinute(0).withSecond(0);
    public final static LocalDateTime yesterdayDT = todayDT.minusDays(1);
    public final static LocalDateTime tomorrowDT = todayDT.plusDays(1);
    public final static DateTimeFormatter dateOnlyFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public final static String yesterday = yesterdayDT.format(dateOnlyFormat);
    public final static String today = todayDT.format(dateOnlyFormat);
    public final static String tomorrow = tomorrowDT.format(dateOnlyFormat);

    /**
     * This is a shared Experiment that derived from the python integration test exp_a and exp_b
     * only label is different from the original definition by appending a time to it
     *
     * @return
     */
    @DataProvider(name = "ExperimentAAndB")
    public static Object[][] experimentAAndB() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"qbo\", \"label\": \"exp_a_" + time + "\"," +
                                "\"samplingPercent\": 1.0, \"startTime\": \"" + dateTime.minusDays(1).format(formatter) + "\", " +
                                "\"endTime\": \"" + dateTime.plusDays(1).format(formatter) + "\", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"qbo\", \"label\": \"exp_b_" + time + "\"," +
                                "\"samplingPercent\": 1.0, \"startTime\": \"" + dateTime.minusDays(3).format(formatter) + "\", " +
                                "\"endTime\": \"" + dateTime.plusDays(8).format(formatter) + "\", \"description\": \"some description\"}"
                }
        };
    }

    @DataProvider(name = "ExperimentBuckets")
    public static Object[][] experimentBuckets() {
        return new Object[][]{
                new Object[]{
                        " {\"label\": \"blue\", \"allocationPercent\": \"0.5\", \"isControl\": \"true\", " +
                                "\"description\": \"blue bucket\",\"payload\": \"HTML-JS-blue\"}",
                        " {\"label\": \"red\", \"allocationPercent\": \"0.5\", \"isControl\": \"false\", " +
                                "\"description\": \"red bucket\",\"payload\": \"HTML-JS-red\"}"
                }
        };
    }

    @DataProvider(name = "ExperimentUsers")
    public static Object[][] experimentUsers() {
        return new Object[][]{
                new Object[]{"user-a"},
                new Object[]{"user-b"},
                new Object[]{"user-c"},
                new Object[]{"user-d"}
        };
    }

    @DataProvider(name = "ValidEvents")
    public static Object[][] validEvents() {
        return new Object[][]{
                new Object[]{
                        "red",
                        "{\"events\": [{\"timestamp\": \"" + yesterday + "T14:23:45-0000\", \"name\": \"IMPRESSION\", " +
                                "\"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}, " +
                                "{\"timestamp\": \"" + today + "T14:23:45-0000\", \"name\": \"IMPRESSION\", " +
                                "\"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}, " +
                                "{\"timestamp\": \"" + yesterday + "T14:54:32-0000\", \"name\": \"click\", " +
                                "\"value\": null, \"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}, " +
                                "{\"timestamp\": \"" + today + "T14:54:32-0000\", \"name\": \"love it\", " +
                                "\"value\": null, \"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}, " +
                                "{\"timestamp\": \"" + tomorrow + "T08:54:32-0000\", \"name\": \"click\", " +
                                "\"value\": null, \"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}]}"
                },
                new Object[]{
                        "blue",
                        "{\"events\": [{\"timestamp\": \"" + yesterday + "T22:23:45-0000\", \"name\": \"IMPRESSION\", " +
                                "\"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}, " +
                                "{\"timestamp\": \"" + today + "T12:23:45-0000\", \"name\": \"IMPRESSION\", " +
                                "\"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}, " +
                                "{\"timestamp\": \"" + yesterday + "T22:54:32-0000\", \"name\": \"click\", " +
                                "\"value\": null, \"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}, " +
                                "{\"timestamp\": \"" + yesterday + "T23:54:32-0000\", \"name\": \"love it\", " +
                                "\"value\": null, \"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}, " +
                                "{\"timestamp\": \"" + today + "T22:54:32-0000\", \"name\": \"click\", " +
                                "\"value\": null, \"payload\": \"{\\\"testKey\\\":\\\"testValue\\\"}\"}]}"

                }
        };
    }


    @DataProvider(name = "EmptyTimeRangeQueryAndResponse")
    public static Object[][] emptyTimeRangeQueryAndResponse() {
        return new Object[][]{
                new Object[]{
                        null,
                        dateTime.minusDays(5).format(formatter),
                        0
                },
                new Object[]{
                        dateTime.plusDays(10).format(formatter),
                        null,
                        0
                },
                new Object[]{
                        dateTime.plusDays(1).format(formatter),
                        dateTime.minusDays(1).format(formatter),
                        0
                },
        };
    }


    @DataProvider(name = "TimeRanges")
    public static Object[][] timeRange() {
        return new Object[][]{
                new Object[]{
                        yesterday + "T10:00:00-0000",
                        tomorrow + "T10:00:00-0000"
                },
                new Object[]{
                        yesterday + "T20:00:00-0000",
                        tomorrow + "T10:00:00-0000"
                },
                new Object[]{
                        yesterday + "T20:00:00-0000",
                        today + "T23:00:00-0000"
                },
                new Object[]{
                        today + "T13:00:00-0000",
                        today + "T23:00:00-0000"
                },
                new Object[]{
                        yesterday + "T20:00:00-0000",
                        today + "T10:00:00-0000"
                },
                new Object[]{
                        today + "T13:00:00-0000",
                        today + "T13:00:00-0000"
                }
        };
    }

    @DataProvider(name = "UsersAndBucketEvents")
    public static Object[][] getUsersAndBucketEvents() {
        return combine(experimentUsers(), validEvents());
    }


    @DataProvider(name = "TimeRangeQueryNonEmpty")
    public static Object[][] timeRangeQueryNonEmpty() {
        return new Object[][]{
                new Object[]{
                        null,
                        todayDT.format(formatter),
                        4,
                        2
                },
                new Object[]{
                        todayDT.format(formatter),
                        null,
                        4,
                        2
                },
                new Object[]{
                        yesterdayDT.format(formatter),
                        tomorrowDT.format(formatter),
                        4,
                        3
                },
                new Object[]{
                        todayDT.format(formatter),
                        todayDT.format(formatter),
                        4,
                        1
                },
                new Object[]{
                        todayDT.format(formatter),
                        tomorrowDT.format(formatter),
                        4,
                        2
                },
                new Object[]{
                        todayDT.minusDays(5).format(formatter),
                        todayDT.plusDays(10).format(formatter),
                        4,
                        16
                }
        };
    }


}
