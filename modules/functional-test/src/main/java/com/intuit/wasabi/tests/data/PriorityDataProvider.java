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

import org.joda.time.DateTime;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

@Test
public class PriorityDataProvider {
    public final static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-0000");
    public final static long time = new Date().getTime();
    public final static DateTime dateTime = new DateTime(time);


    @DataProvider(name = "Application")
    public static Object[][] getApplicationName() {
        return new Object[][]{
                new Object[]{
                        "priority_" + time
                }
        };
    }

    @DataProvider(name = "Experiments")
    public static Object[][] getExperiments() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"priority_" + time + "\", \"label\": \"exp_" + time + "_1_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"Some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"priority_" + time + "\", \"label\": \"exp_" + time + "_2_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"Some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"priority_" + time + "\", \"label\": \"exp_" + time + "_3_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"Some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"priority_" + time + "\", \"label\": \"exp_" + time + "_4_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"Some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"priority_" + time + "\", \"label\": \"exp_" + time + "_5_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"Some description\"}"
                }
        };
    }

    @DataProvider(name = "NewExperiments")
    public static Object[][] newExperiments() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"priority_" + time + "\", \"label\": \"exp_" + time + "_6_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"Some description\"}"
                }
        };
    }

    @DataProvider(name = "differentApp")
    public static Object[][] differentApp() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"different_priority_" + time + "\", \"label\": \"exp_" + time + "_6_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"Some description\"}"
                }
        };
    }


    @DataProvider(name = "terminatedExperiment")
    public static Object[][] terminatedExperiment() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"priority_" + time + "\", \"label\": \"terminated\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"some description\"}",
                        "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}",
                        "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}"
                }
        };
    }

    @DataProvider(name = "deletedExperiment")
    public static Object[][] deletedExperiment() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"priority_" + time + "\", \"label\": \"deleted\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"Some description\"}"
                }
        };
    }


    @DataProvider(name = "batchExperiments")
    public static Object[][] batchExperiments() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"batch_priority_" + time + "\", \"label\": \"batch_exp_" + time + "_1_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"some description\"}",
                        "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}",
                        "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"batch_priority_" + time + "\", \"label\": \"batch_exp_" + time + "_2_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"some description\"}",
                        "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}",
                        "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"batch_priority_" + time + "\", \"label\": \"batch_exp_" + time + "_3_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"some description\"}",
                        "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}",
                        "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"batch_priority_" + time + "\", \"label\": \"batch_exp_" + time + "_4_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"some description\"}",
                        "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}",
                        "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"batch_priority_" + time + "\", \"label\": \"batch_exp_" + time + "_5_priority\"," +
                                "\"samplingPercent\": 1, \"startTime\": \"" + dateParser.format(dateTime.minusDays(7).toDate()) + "\", " +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(21).toDate()) + "\", \"description\": \"some description\"}",
                        "{\"label\": \"red\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}",
                        "{\"label\": \"blue\", \"allocationPercent\": 0.5, \"isControl\": false, \"description\": \"some description\"}"
                }
        };
    }

}
