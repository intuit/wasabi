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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joda.time.DateTime;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 5/9/16.
 */
@Test
public class SegmentationDataProvider {
    static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-0000");
    public static final long time = new Date().getTime();
    public static final DateTime dateTime = new DateTime(time);
    public static final String startTime = dateParser.format(dateTime.minusDays(7).toDate());
    public static final String endTime = dateParser.format(dateTime.plusDays(21).toDate());

    @DataProvider(name = "batchSetup")
    public static Object[][] batchSegmentationSetup() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"segmentation_" + time + "\", \"label\": \"batch_seg_label_" + time + "_0\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'CA'\", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"segmentation_" + time + "\", \"label\": \"batch_seg_label_" + time + "_1\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'RI'\", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"segmentation_" + time + "\", \"label\": \"batch_seg_label_" + time + "_2\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'VA'\", \"description\": \"some description\"}"
                }
        };
    }


    @DataProvider(name = "batchAssignmentData")
    public static Object[][] invalidSegmentationRules() {
        return new Object[][]{
                new Object[]{
                        0,
                        "segmentation_" + time,
                        "user_0",
                        "{\"labels\":[\"batch_seg_label_" + time + "_0\",\"batch_seg_label_" + time + "_1\",\"batch_seg_label_" + time + "_2\"], " +
                                "\"profile\": {\"state\":\"CA\"}}"
                },
                new Object[]{
                        1,
                        "segmentation_" + time,
                        "user_1",
                        "{\"labels\":[\"batch_seg_label_" + time + "_0\",\"batch_seg_label_" + time + "_1\",\"batch_seg_label_" + time + "_2\"], " +
                                "\"profile\": {\"state\":\"RI\"}}"
                },
                new Object[]{
                        2,
                        "segmentation_" + time,
                        "user_2",
                        "{\"labels\":[\"batch_seg_label_" + time + "_0\",\"batch_seg_label_" + time + "_1\",\"batch_seg_label_" + time + "_2\"], " +
                                "\"profile\": {\"state\":\"VA\"}}"
                }

        };
    }

    @DataProvider(name = "experimentSetup")
    public static Object[][] experimentSetup() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"segmutex_" + time + "\", \"label\": \"exp_label_" + time + "_0\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'CA\", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"segmutex_" + time + "\", \"label\": \"exp_label_" + time + "_1\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'RI\", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"segmutex_" + time + "\", \"label\": \"exp_label_" + time + "_2\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'VA\", \"description\": \"some description\"}"
                }
        };
    }

    @DataProvider(name = "mutexBatchExperimentSetup")
    public static Object[][] mutexExperimentSetup() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"segmutexbatch_" + time + "\", \"label\": \"batch_exp_label_" + time + "_0\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'CA\", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"segmutexbatch_" + time + "\", \"label\": \"batch_exp_label_" + time + "_1\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'RI\", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"segmutexbatch_" + time + "\", \"label\": \"batch_exp_label_" + time + "_2\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"state = 'VA\", \"description\": \"some description\"}"
                }
        };
    }

    @DataProvider(name = "exclusionRulesExperimentSetup")
    public static Object[][] exclusionRulesExperimentSetup() {
        return new Object[][]{
                new Object[]{
                        "{\"applicationName\": \"segmutex_" + time + "\", \"label\": \"excl_exp_label_" + time + "_0\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"(salary > 80000 && state = 'CA') || (salary > 60000 && vet = true)\"" +
                                ", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"segmutex_" + time + "\", \"label\": \"excl_exp_label_" + time + "_1\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"(salary > 80000 && state = 'CA') || (salary > 60000 && vet = true)\"" +
                                ", \"description\": \"some description\"}"
                },
                new Object[]{
                        "{\"applicationName\": \"segmutex_" + time + "\", \"label\": \"excl_exp_label_" + time + "_2\", " +
                                "\"samplingPercent\": 1, \"startTime\": \"" + startTime + "\", \"endTime\": \"" + endTime + "\"," +
                                "\"rule\": \"(salary > 80000 && state = 'CA') || (salary > 60000 && vet = true)\"" +
                                ", \"description\": \"some description\"}"
                }
        };
    }

    @DataProvider(name = "invalidInput")
    public static Object[][] invalidRules() {
        return new Object[][]{
                new Object[]{
                        "{\n" +
                                "    \"samplingPercent\": 1,\n" +
                                "    \"rule\": \"(salary > 80000 || (salary < 60000\",\n" +
                                "    \"startTime\": \"" + startTime + "\",\n" +
                                "    \"endTime\": \"" + endTime + "\",\n" +
                                "    \"label\": \"exp_" + time + "\",\n" +
                                "    \"applicationName\": \"seg_valid_rule_" + time + "\",\n" +
                                "    \"description\": \"some description\" " +
                                "}"
                },
                new Object[]{
                        "{\n" +
                                "    \"samplingPercent\": 1,\n" +
                                "    \"rule\": \"salary > 80000 ||| salary < 60000\",\n" +
                                "    \"startTime\": \"" + startTime + "\",\n" +
                                "    \"endTime\": \"" + endTime + "\",\n" +
                                "    \"label\": \"exp_" + time + "\",\n" +
                                "    \"applicationName\": \"seg_valid_rule_" + time + "\",\n" +
                                "    \"description\": \"some description\" " +
                                "}"
                },
                new Object[]{
                        "{\n" +
                                "    \"samplingPercent\": 1,\n" +
                                "    \"rule\": 5,\n" +
                                "    \"startTime\": \"" + startTime + "\",\n" +
                                "    \"endTime\": \"" + endTime + "\",\n" +
                                "    \"label\": \"exp_" + time + "\",\n" +
                                "    \"applicationName\": \"seg_valid_rule_" + time + "\",\n" +
                                "    \"description\": \"some description\" " +
                                "}"
                }
        };
    }

    @DataProvider(name = "validInput")
    public static Object[][] validSegmentationRules() {
        Gson gson = new GsonBuilder().create();
        Map<String, Object> baseData = new HashMap<>();
        List<Object[]> output = new ArrayList<Object[]>();
        baseData.put("startTime", startTime);
        baseData.put("endTime", endTime);
        baseData.put("samplingPercent", 1);
        baseData.put("description", "some description");
        List<String> applications = new ArrayList<>();
        applications.add("seg_no_rule");
        applications.add("seg_valid_rule");
        for (String application : applications) {
            baseData.put("applicationName", application + "_" + time);
            baseData.put("label", "valid_exp_" + time);
            if ("seg_valid_rule".equalsIgnoreCase(application)) {
                baseData.put("rule", "(salary > 80000 && state = 'CA') || (salary > 60000 && vet = true)");
            } else {
                baseData.remove("rule");
            }
            output.add(new Object[]{gson.toJson(baseData)});
        }
        return output.toArray(new Object[output.size()][]);
    }

    @DataProvider(name = "states")
    public static Object[][] allowedStates() {
        return new Object[][]{
                new Object[]{"RUNNING"},
                new Object[]{"PAUSED"},
                new Object[]{"RUNNING"}
        };
    }
}
