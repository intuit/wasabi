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
package com.intuit.wasabi.tests.data;

import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

@Test
public class AssignmentDataProvider extends CombinableDataProvider {
    public final static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-0000");
    public final static long time = new Date().getTime();
    public final static DateTime dateTime = new DateTime(time);

    @DataProvider(name = "ExperimentTimes")
    public static Object[][] getExperimentTimes() {
        return new Object[][]{
                new Object[]{
                        dateParser.format(dateTime.plusDays(1).toDate()),
                        dateParser.format(dateTime.plusDays(21).toDate()),
                        "future",
                        "0" //experiment count
                },
                new Object[]{
                        dateParser.format(dateTime.minusDays(21).toDate()),
                        dateParser.format(dateTime.minusDays(1).toDate()),
                        "past",
                        "1"
                },
                new Object[]{
                        dateParser.format(dateTime.minusDays(3).toDate()),
                        dateParser.format(dateTime.plusDays(21).toDate()),
                        "present",
                        "2"
                }
        };
    }

    @DataProvider(name = "ExperimentBucket")
    public static Object[][] getExperimentBucket() {
        return new Object[][]{
                new Object[]{
                        "blue",
                        0.5,
                        true
                },
                new Object[]{
                        "red",
                        0.5,
                        false
                }
        };
    }

    @DataProvider(name = "PutAssignmentExperimentData")
    public static Object[][] putAssignmentExperimentData() {
        return new Object[][]{
                new Object[]{
                        "testingApp",
                        "PutAssignments_" + time,
                        "{\"applicationName\": \"testingApp\", \"label\": \"PutAssignments_" +
                                time + "\",\"samplingPercent\": 0.67, \"startTime\": \"" +
                                dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                                "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}",
                        "onlybucket",
                        "{\"label\": \"onlybucket\", \"allocationPercent\": 1.0, \"isControl\": false, \"description\": \"the only bucket\",\"payload\": \"jklfew\"}"

                }
        };
    }

    @DataProvider(name = "PutAssignmentStates")
    public static Object[][] putAssignmentStates() {
        return new Object[][]{
                new Object[]{"DRAFT", HttpStatus.SC_BAD_REQUEST},
                new Object[]{"PAUSED", HttpStatus.SC_OK},
                new Object[]{"RUNNING", HttpStatus.SC_OK},
                new Object[]{"TERMINATED", HttpStatus.SC_NOT_FOUND}
        };
    }


    @DataProvider(name = "BatchAssignmentExperimentData")
    public static Object[][] BatchAssignmentExperimentData() {
        return new Object[][]{
                new Object[]{"{\"applicationName\": \"testBatch\", \"label\": \"batchAssign_" + 1 + "-" +
                        time + "\",\"samplingPercent\": 1.0, \"startTime\": \"" +
                        dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                        "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"},
                new Object[]{"{\"applicationName\": \"testBatch\", \"label\": \"batchAssign_" + 2 + "-" +
                        time + "\",\"samplingPercent\": 1.0, \"startTime\": \"" +
                        dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                        "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"},
                new Object[]{"{\"applicationName\": \"testBatch\", \"label\": \"batchAssign_" + 3 + "-" +
                        time + "\",\"samplingPercent\": 1.0, \"startTime\": \"" +
                        dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                        "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"},
                new Object[]{"{\"applicationName\": \"testBatch\", \"label\": \"batchAssign_" + 4 + "-" +
                        time + "\",\"samplingPercent\": 1.0, \"startTime\": \"" +
                        dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                        "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"},
                new Object[]{"{\"applicationName\": \"testBatch\", \"label\": \"batchAssign_" + 5 + "-" +
                        time + "\",\"samplingPercent\": 1.0, \"startTime\": \"" +
                        dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                        "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"}
        };
    }

    @DataProvider(name = "BatchAssignmentBadExperimentData")
    public static Object[][] BatchAssignmentBadExperimentData() {
        return new Object[][]{
                new Object[]{"{\"applicationName\": \"testBadBatch\", \"label\": \"batchBadAssign_" + 1 + "-" +
                        time + "\",\"samplingPercent\": 1.0, \"startTime\": \"" +
                        dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                        "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"},
                new Object[]{"{\"applicationName\": \"testBadBatch\", \"label\": \"batchBadAssign_" + 2 + "-" +
                        time + "\",\"samplingPercent\": 1.0, \"startTime\": \"" +
                        dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                        "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"},
                new Object[]{"{\"applicationName\": \"testBadBatch\", \"label\": \"batchBadAssign_" + 3 + "-" +
                        time + "\",\"samplingPercent\": 1.0, \"startTime\": \"" +
                        dateParser.format(dateTime.minusDays(3).toDate()) + "\"," +
                        "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"}
        };
    }


    @DataProvider(name = "BatchAssignmentStateAndExpectedValues")
    public static Object[][] BatchAssignmentStateAndExpectedValues() {
        return new Object[][]{
                new Object[]{"DRAFT", false, "EXPERIMENT_IN_DRAFT_STATE"},
                new Object[]{"PAUSED", false, "EXPERIMENT_PAUSED"},
                new Object[]{"RUNNING", true, "NEW_ASSIGNMENT"},
                new Object[]{"PAUSED", true, "EXISTING_ASSIGNMENT"},
                new Object[]{"RUNNING", true, "EXISTING_ASSIGNMENT"},
                new Object[]{"TERMINATED", false, "EXPERIMENT_NOT_FOUND"}
        };
    }


    @DataProvider(name = "ExportAssignmentExperimentData")
    public static Object[][] ExportAssignmentExperimentData() {
        return new Object[][]{
                new Object[]{"{\"applicationName\": \"testExportAssignment_" + time + "\", \"label\": \"assignments\"," +
                        "\"samplingPercent\": 1.0, \"startTime\": \"" + dateParser.format(dateTime.minusDays(3).toDate())
                        + "\"," + "\"endTime\": \"" + dateParser.format(dateTime.plusDays(3).toDate()) + "\", \"description\": \"Some hypothesis\"}"}
        };
    }

    @DataProvider(name = "ExportAssignmentExperimentUser")
    public static Object[][] ExportAssignmentExperimentUser() {
        return new Object[][]{
                new Object[]{"user-1", "{\"events\": [{\"name\": \"IMPRESSION\", \"payload\": \"{\"testKey\":\"testValue\"}\", \"timestamp\": \"" + dateParser.format(dateTime.minusDays(2).toDate()) + "\"}]}"},
                new Object[]{"user-2", "{\"events\": [{\"name\": \"IMPRESSION\", \"payload\": \"{\"testKey\":\"testValue\"}\", \"timestamp\": \"" + dateParser.format(dateTime.minusDays(2).toDate()) + "\"}]}"},
                new Object[]{"user-3", "{\"events\": [{\"name\": \"IMPRESSION\", \"payload\": \"{\"testKey\":\"testValue\"}\", \"timestamp\": \"" + dateParser.format(dateTime.minusDays(2).toDate()) + "\"}]}"},
                new Object[]{"user-4", "{\"events\": [{\"name\": \"IMPRESSION\", \"payload\": \"{\"testKey\":\"testValue\"}\", \"timestamp\": \"" + dateParser.format(dateTime.minusDays(2).toDate()) + "\"}]}"},
                new Object[]{"user-5", "{\"events\": [{\"name\": \"IMPRESSION\", \"payload\": \"{\"testKey\":\"testValue\"}\", \"timestamp\": \"" + dateParser.format(dateTime.minusDays(2).toDate()) + "\"}]}"}
        };
    }


    @DataProvider(name = "ExperimentStates")
    public static Object[][] getExperimentState() {
        return new Object[][]{
                new Object[]{"DRAFT", "EXPERIMENT_IN_DRAFT_STATE"},
                new Object[]{"RUNNING", "NEW_ASSIGNMENT"},
                new Object[]{"PAUSED", "EXISTING_ASSIGNMENT"},
                new Object[]{"RUNNING", "EXISTING_ASSIGNMENT"}
        };
    }

    @DataProvider(name = "ExperimentUsers")
    public static Object[][] getExperimentUsers() {
        return new Object[][]{
                new Object[]{"user-a"},
                new Object[]{"user-ignoreSampling?ignoreSamplingPercent=true"},
                new Object[]{"user-dontCreate?createAssignment=false"}
        };
    }

    @DataProvider(name = "ExperimentStateAndUser")
    public static Object[][] getExperimentStateAndUser() {
        return combine(getExperimentState(), getExperimentUsers());
    }


}
