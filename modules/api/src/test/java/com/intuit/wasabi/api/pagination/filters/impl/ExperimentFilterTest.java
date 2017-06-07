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
package com.intuit.wasabi.api.pagination.filters.impl;

import com.intuit.wasabi.exceptions.PaginationException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ExperimentFilter}.
 */
public class ExperimentFilterTest {

    private Experiment experiment;

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
        experiment = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(Application.Name.valueOf("MyTestApplication"))
                .withLabel(Experiment.Label.valueOf("MyTestExperiment"))
                .withCreationTime(creationTime)
                .withModificationTime(modificationTime)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withState(Experiment.State.RUNNING)
                .withCreatorID("TheCreator")
                .withTags(new HashSet<>(Arrays.asList("tag1", "tag2", "foo")))
                .build();
    }

    @Test
    public void testTest() throws Exception {
        ExperimentFilter experimentFilter = new ExperimentFilter();

        HashMap<String, Boolean> testCases = new HashMap<>();
        testCases.put("MyTest", true);
        testCases.put("application_name=App", true);
        testCases.put("application_name_exact=App", false);
        testCases.put("application_name_exact=MyTestApplication", true);
        testCases.put("Test,created_by=Creator", true);
        testCases.put("abcdefg,created_by=Creator", false);
        testCases.put("creation_time=13:42", true);
        testCases.put("start_time=Feb 14", true);
        testCases.put("end_time=15:59:58", false);
        testCases.put("modification_time=2015", false);
        testCases.put("state=term", false);
        testCases.put("state=run", true);
        testCases.put("state_exact=notterminated", true);
        testCases.put("state_exact=non-terminated", false);
        testCases.put("state_exact=draft", false);
        testCases.put("tags=tag2", true);
        testCases.put("tags=tag42", false);
        testCases.put("tags=foo;tag", true);

        for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
            experimentFilter.replaceFilter(testCase.getKey(), "+0000");

            Assert.assertEquals("test case " + testCase.getKey() + " failed.",
                    testCase.getValue(),
                    experimentFilter.test(experiment));
        }
    }

    @Test
    public void testStateTest() throws Exception {
        class TestCase {
            private Experiment.State state;
            private String filter;
            private boolean result;

            TestCase(Experiment.State state, String filter, boolean result) {
                this.state = state;
                this.filter = filter;
                this.result = result;
            }
        }

        List<TestCase> testCases = new ArrayList<>();

        testCases.add(new TestCase(Experiment.State.DRAFT, "notterminated", true));
        testCases.add(new TestCase(Experiment.State.DRAFT, "draft", true));
        testCases.add(new TestCase(Experiment.State.DRAFT, "any", true));
        testCases.add(new TestCase(Experiment.State.RUNNING, "notterminated", true));
        testCases.add(new TestCase(Experiment.State.RUNNING, "running", true));
        testCases.add(new TestCase(Experiment.State.RUNNING, "any", true));
        testCases.add(new TestCase(Experiment.State.PAUSED, "notterminated", true));
        testCases.add(new TestCase(Experiment.State.PAUSED, "paused", true));
        testCases.add(new TestCase(Experiment.State.PAUSED, "any", true));
        testCases.add(new TestCase(Experiment.State.TERMINATED, "terminated", true));
        testCases.add(new TestCase(Experiment.State.TERMINATED, "any", true));

        testCases.add(new TestCase(Experiment.State.DRAFT, "terminated", false));
        testCases.add(new TestCase(Experiment.State.DRAFT, "running", false));
        testCases.add(new TestCase(Experiment.State.DRAFT, "paused", false));
        testCases.add(new TestCase(Experiment.State.RUNNING, "terminated", false));
        testCases.add(new TestCase(Experiment.State.RUNNING, "draft", false));
        testCases.add(new TestCase(Experiment.State.RUNNING, "paused", false));
        testCases.add(new TestCase(Experiment.State.PAUSED, "terminated", false));
        testCases.add(new TestCase(Experiment.State.PAUSED, "running", false));
        testCases.add(new TestCase(Experiment.State.PAUSED, "draft", false));
        testCases.add(new TestCase(Experiment.State.TERMINATED, "notterminated", false));
        testCases.add(new TestCase(Experiment.State.TERMINATED, "running", false));
        testCases.add(new TestCase(Experiment.State.TERMINATED, "draft", false));
        testCases.add(new TestCase(Experiment.State.TERMINATED, "paused", false));
        testCases.add(new TestCase(Experiment.State.DELETED, "notterminated", false));
        testCases.add(new TestCase(Experiment.State.DELETED, "terminated", false));
        testCases.add(new TestCase(Experiment.State.DELETED, "running", false));
        testCases.add(new TestCase(Experiment.State.DELETED, "draft", false));
        testCases.add(new TestCase(Experiment.State.DELETED, "paused", false));
        testCases.add(new TestCase(Experiment.State.DELETED, "any", false));

        testCases.add(new TestCase(Experiment.State.RUNNING, "nonexistent", false));

        for (TestCase testCase : testCases) {
            Assert.assertEquals(testCase.state + " should " + (testCase.result ? "" : "not ")
                            + "match " + testCase.filter,
                    testCase.result, ExperimentFilter.stateTest(testCase.state, testCase.filter));
        }
    }

    @Test
    public void testConstraintTest() throws Exception {
        Date date = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2014-02-13T13:42:45+00:00")));

        HashMap<String, Boolean> testCases = new HashMap<>();
        testCases.put("", true);
        testCases.put("isAny", true);
        testCases.put("isBefore:2/13/2014", false);
        testCases.put("isBefore:2/14/2014", true);
        testCases.put("isAfter:2/13/2014", false);
        testCases.put("isAfter:2/12/2014", true);
        testCases.put("isOn:2/13/2014", true);
        testCases.put("isOn:2/12/2014", false);
        testCases.put("isOn:2/13/2014", true);
        testCases.put("isOn:2/12/2014", false);
        testCases.put("isOn:2/2/2014", false);
        testCases.put("isOn:02/02/2014", false);
        testCases.put("isOn:02/02/2014\t+0000", false);
        testCases.put("isBetween:2/5/2013:12/23/2014", true);
        testCases.put("isBetween:2/5/2013:12/23/2013", false);
        testCases.put("isBetween:02/13/2014:02/15/2014", true);
        testCases.put("isBetween:2/01/2013:02/13/2014", true);
        testCases.put("isBetween:2/5/2013:12/12/2013", false);

        for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
            Assert.assertEquals("Test case " + testCase.getKey() + " failed.",
                    testCase.getValue(),
                    ExperimentFilter.constraintTest(date, testCase.getKey()));
        }

        List<String> throwCases = new ArrayList<>();
        throwCases.add("isBefore:");
        throwCases.add("isAfter:");
        throwCases.add(":12/5/2013");
        throwCases.add("isBefore:13/5/2013");
        throwCases.add("isBefore:11/3/13");
        throwCases.add("isBetween:11/3/2013:12/3/14");
        throwCases.add("isBetween:11/3/2013:");
        throwCases.add("isBetween:11/3/2013");
        throwCases.add("isOn:13/12/2013");
        throwCases.add("isSomeday:5/21/2011");

        for (String throwCase : throwCases) {
            try {
                ExperimentFilter.constraintTest(date, throwCase);
                Assert.fail(throwCase + " passed, but should have thrown.");
            } catch (PaginationException ignored) {
                // expected
            } catch (Exception exception) {
                Assert.fail(throwCase + " threw the wrong exception: " + exception);
            }
        }
    }
}
