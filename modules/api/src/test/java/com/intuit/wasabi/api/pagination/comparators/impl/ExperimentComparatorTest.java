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
package com.intuit.wasabi.api.pagination.comparators.impl;

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link ExperimentComparator}
 */
public class ExperimentComparatorTest {

    private Experiment experiment1;
    private Experiment experiment2;

    @Before
    public void setup() throws Exception {
        Date creationTime = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME
                .parse("2014-02-13T13:42:45+00:00")));
        Date modificationTime = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME
                .parse("2014-02-13T14:21:23+00:00")));
        Date startTime = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME
                .parse("2014-02-14T16:00:00+00:00")));
        Date endTime = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME
                .parse("2014-02-16T15:59:59+00:00")));
        experiment1 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(Application.Name.valueOf("MyApplication"))
                .withLabel(Experiment.Label.valueOf("Alpha"))
                .withCreatorID("AlphaCreator")
                .withCreationTime(creationTime)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withModificationTime(modificationTime)
                .withState(Experiment.State.RUNNING)
                .build();


        Date startTime2 = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME
                .parse("2014-02-14T17:00:00+00:00")));
        Date endTime2 = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME
                .parse("2014-02-16T18:59:59+00:00")));

        experiment2 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(Application.Name.valueOf("MyOtherApplication"))
                .withLabel(Experiment.Label.valueOf("Beta"))
                .withCreatorID("BetaCreator")
                .withCreationTime(creationTime)
                .withStartTime(startTime2)
                .withEndTime(endTime2)
                .withModificationTime(modificationTime)
                .withState(Experiment.State.TERMINATED)
                .build();
    }

    @Test
    public void testCompare() throws Exception {
        ExperimentComparator experimentComparator = new ExperimentComparator();

        HashMap<String, Integer> testCases = new HashMap<>();
        testCases.put("-application_name", 1);
        testCases.put("experiment_label", -1);
        testCases.put("created_by", -1);
        testCases.put("creation_time", 0);
        testCases.put("-start_time", 1);
        testCases.put("end_time", -1);
        testCases.put("modification_time", 0);
        testCases.put("state", -1);

        for (Map.Entry<String, Integer> testCase : testCases.entrySet()) {
            experimentComparator.replaceSortorder(testCase.getKey());

            int magnitudeResult = experimentComparator.compare(experiment1, experiment2);
            if (magnitudeResult != 0) {
                magnitudeResult /= Math.abs(magnitudeResult);
            }
            Assert.assertEquals("test case " + testCase.getKey() + " failed.",
                    testCase.getValue().intValue(),
                    magnitudeResult);
        }
    }
}
