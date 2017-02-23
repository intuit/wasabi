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

package com.intuit.wasabi.tests.service.assignment;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test for the /{experimentID}/assignments/traffic/{from}/{to} endpoint.
 */
public class DailyAssignmentCountsTest extends TestBase {
    private final List<Experiment> experiments = new ArrayList<>();
    private final LocalDateTime startDate = LocalDateTime.now().minusDays(1);
    private final LocalDateTime endDate = LocalDateTime.now().plusDays(1);
    private List<Experiment> mutexListA;
    private List<Experiment> mutexListB;
    private List<Experiment> mutexListC;

    /**
     * Sets up 10 experiments.
     * Experiments 1 - 3 are mutually exclusive to each other (List A).
     * Experiments 4 and 5 are mutually exclusive to each other (List B).
     * Experiments 6 - 9 are mutually exclusive in a circular fashion (List C).
     * Experiment 10 is not mutually exclusive.
     */
    @BeforeClass(dependsOnGroups = {"ping"})
    public void setup() {
        for (int i = 0; i < 10; ++i) {
            Experiment experiment = postExperiment(ExperimentFactory.createExperiment());
            experiments.add(experiment);

            postBucket(BucketFactory.createBucket(experiment).setLabel("A").setAllocationPercent(1));
            putExperiment(experiment.setState(Constants.EXPERIMENT_STATE_RUNNING));
            for (int j = 0; j < 10; ++j) {
                postAssignment(experiment, UserFactory.createUser());
            }
        }
        mutexListA = experiments.subList(0, 3);
        mutexListB = experiments.subList(3, 5);
        mutexListC = experiments.subList(5, 9);

        postExclusions(mutexListA); // list A is mutually exclusive to each other
        postExclusions(mutexListB); // list B is mutually exclusive to each other

        postExclusions(mutexListC.subList(0, 3)); // list C is mutually exclusive in a circle
        postExclusions(mutexListC.subList(1, 4));

        // experiments.get(9) stays alone for now, will be added to a more complex test.
    }

    /**
     * Asserts that all experiment labels are present in the result.
     *
     * @param experiments the expected experiments
     * @param result      the result
     */
    private void assertExperimentLabels(List<Experiment> experiments, Map<String, List<?>> result) {
        Assert.assertTrue(result.containsKey("experiments"), "Expected 'experiments' key.");

        @SuppressWarnings("unchecked")
        List<String> experimentLabels = (List<String>) result.get("experiments");
        List<String> expectedLabelsNoOrder = experiments.stream()
                .map(experiment -> experiment.label)
                .collect(Collectors.toList());
        Assert.assertEqualsNoOrder(experimentLabels.toArray(), expectedLabelsNoOrder.toArray(), "Not all experiment labels are given.");
    }

    /**
     * Asserts that all priorities are present in the result, then asserts that their order matches the order of the
     * experiment labels.
     *
     * @param experiments the expected experiments
     * @param result      the result
     */
    private void assertPriorities(List<Experiment> experiments, Map<String, List<?>> result) {
        Assert.assertTrue(result.containsKey("priorities"), "Expected 'priorities' key.");

        @SuppressWarnings("unchecked")
        List<Integer> priorities = (List<Integer>) result.get("priorities");
        List<Integer> expectedPriorities = experiments.stream()
                .map(experiment -> this.experiments.indexOf(experiment) + 1)
                .collect(Collectors.toList());
        Assert.assertEqualsNoOrder(priorities.toArray(), expectedPriorities.toArray(), "Not all experiment priorities are given.");

        @SuppressWarnings("unchecked")
        List<String> experimentLabels = (List<String>) result.get("experiments");
        List<Integer> expectedPrioritiesSorted = experiments.stream()
                .sorted((experiment1, experiment2) ->
                        experimentLabels.indexOf(experiment1.label) - experimentLabels.indexOf(experiment2.label))
                .map(experiment -> this.experiments.indexOf(experiment) + 1)
                .collect(Collectors.toList());
        Assert.assertEquals(priorities, expectedPrioritiesSorted, "Priorities and Labels are not in the same order.");
    }

    /**
     * Asserts that for each experiment there are all sampling percentages given.
     *
     * @param experiments the expected experiments
     * @param result      the result
     */
    private void assertSamplingPercentages(List<Experiment> experiments, Map<String, List<?>> result) {
        Assert.assertTrue(result.containsKey("samplingPercentages"), "Expected 'samplingPercentages' key.");

        @SuppressWarnings("unchecked")
        List<Float> samplingPercentages = (List<Float>) result.get("samplingPercentages");
        List<Float> expectedSamplingPercentages = experiments.stream()
                .map(experiment -> (float) experiment.samplingPercent)
                .collect(Collectors.toList());
        Assert.assertEqualsNoOrder(samplingPercentages.toArray(), expectedSamplingPercentages.toArray(), "Not all sampling percentages are given.");
    }

    /**
     * Asserts that all assignment ratios for each day are given and correct: on the first and last day 0 percent (= 0.0),
     * on the day in the middle 100 percent (= 1.0).
     *
     * @param experiments the expected experiments
     * @param result      the result
     */
    private void assertAssignmentRatios(List<Experiment> experiments, Map<String, List<?>> result) {
        Assert.assertTrue(result.containsKey("assignmentRatios"), "Expected 'assignmentRatios' key.");

        List<Float> ones = IntStream.range(0, experiments.size()).mapToObj(i -> 1f).collect(Collectors.toList());
        List<Float> zeros = IntStream.range(0, experiments.size()).mapToObj(i -> 0f).collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        List<Map<String, ?>> assignmentRatios = (List<Map<String, ?>>) result.get("assignmentRatios");
        List<Map<String, ?>> expectedAssignmentRatios = IntStream.range(0, 3)
                .mapToObj(startDate::plusDays)
                .map(date -> {
                    Map<String, Object> m = new HashMap<>(2);
                    m.put("date", TestUtils.formatDateForUI(date));
                    m.put("values", zeros);
                    if (date.isAfter(startDate.plusHours(1)) && date.isBefore(endDate.minusHours(1))) {
                        m.put("values", ones);
                    }
                    return m;
                })
                .collect(Collectors.toList());
        Assert.assertEqualsNoOrder(assignmentRatios.toArray(), expectedAssignmentRatios.toArray(), "Not all assignment ratios are correct.");
    }

    /**
     * Asserts all important asserts in this order: Experiment labels, priorities, sampling percentages, assignment ratios.
     *
     * @param experiments the expected experiments
     * @param result      the result
     */
    private void runAllAsserts(List<Experiment> experiments, Map<String, List<?>> result) {
        assertExperimentLabels(experiments, result);
        assertPriorities(experiments, result);
        assertSamplingPercentages(experiments, result);
        assertAssignmentRatios(experiments, result);
    }

    /**
     * Tests the single, non-mutex experiment.
     */
    @Test(groups = "basicTrafficTests")
    public void testSingleExperiment() {
        Map<String, List<?>> result = getTraffic(experiments.get(9), startDate, endDate);
        List<Experiment> experimentsList = Collections.singletonList(experiments.get(9));
        runAllAsserts(experimentsList, result);
    }

    /**
     * Tests list A.
     */
    @Test(groups = "basicTrafficTests")
    public void testAllMutuallyExclusiveA() {
        Map<String, List<?>> resultA = getTraffic(mutexListA.get(0), startDate, endDate);
        runAllAsserts(mutexListA, resultA);
    }

    /**
     * Tests list B.
     */
    @Test(groups = "basicTrafficTests")
    public void testAllMutuallyExclusiveB() {
        Map<String, List<?>> resultB = getTraffic(mutexListB.get(0), startDate, endDate);
        runAllAsserts(mutexListB, resultB);
    }

    /**
     * Tests list C.
     */
    @Test(groups = "basicTrafficTests")
    public void testCircularMutuallyExclusive() {
        Map<String, List<?>> resultC = getTraffic(mutexListC.get(0), startDate, endDate);
        runAllAsserts(mutexListC, resultC);
    }

    /**
     * Makes A0 and B0 mutex, tests the combined (mutex-chained) lists thereafter.
     */
    @Test(dependsOnGroups = "basicTrafficTests")
    public void testTwoListCombination() {
        // Setup: make A and B mutex
        postExclusions(Arrays.asList(mutexListA.get(0), mutexListB.get(0))); // A1 and B1 are mutually now exclusive
        List<Experiment> combinedList = new ArrayList<>(mutexListA.size() + mutexListB.size());
        combinedList.addAll(mutexListA);
        combinedList.addAll(mutexListB);

        Map<String, List<?>> result = getTraffic(combinedList.get(0), startDate, endDate);
        runAllAsserts(combinedList, result);
    }

    /**
     * Combines C2 and the single experiment, as well as A2 and the single experiment, leading to a chain between
     * all ten experiments via the then formerly single experiment.
     * Tests the resulting list of all experiments.
     */
    @Test(dependsOnMethods = "testTwoListCombination")
    public void testFullCombination() {
        postExclusions(Arrays.asList(mutexListC.get(2), experiments.get(9))); // combine the setA/B combination with set C
        postExclusions(Arrays.asList(mutexListA.get(2), experiments.get(9))); // via the singular experiment
        Map<String, List<?>> result = getTraffic(mutexListB.get(1), startDate, endDate); // contains all experiments
        runAllAsserts(experiments, result);
    }

    /**
     * Deletes the link between lists A/B and list C, i.e. the formerly single experiment. Checks if the results are
     * back to the previous state.
     */
    @Test(dependsOnMethods = "testFullCombination")
    public void testConnectingExperimentDeleted() {
        putExperiment(experiments.get(9).setState(Constants.EXPERIMENT_STATE_TERMINATED));
        deleteExperiment(experiments.get(9)); // remove linking experiment

        List<Experiment> combinedList = new ArrayList<>(mutexListA.size() + mutexListB.size());
        combinedList.addAll(mutexListA);
        combinedList.addAll(mutexListB);
        Map<String, List<?>> resultA = getTraffic(combinedList.get(0), startDate, endDate); // contains A and B
        runAllAsserts(combinedList, resultA);

        Map<String, List<?>> resultC = getTraffic(mutexListC.get(0), startDate, endDate); // contains C
        runAllAsserts(mutexListC, resultC);
    }

    /**
     * Tests for an unknown experiment ID. Should simply return a 404.
     */
    @Test(dependsOnGroups = "ping")
    public void testUnknownExperiment() {
        getTraffic(ExperimentFactory.createExperiment().setId(UUID.randomUUID().toString()), startDate, endDate, HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Provides a set of illegal date values: not properly URL encoded or strings which are not date parseable.
     *
     * @return illegal date values to be provided for {@link #testIllegalDateInputs(String, String, int)}.
     */
    @DataProvider
    public Object[][] illegalDateProvider() {
        return new Object[][]{
                new Object[]{"5/6/2013", "9%2F5%2F2014", HttpStatus.SC_NOT_FOUND},
                new Object[]{"5%2F6%2F2013", "9/5/2014", HttpStatus.SC_NOT_FOUND},
                new Object[]{"yesterday", "9/5/2014", HttpStatus.SC_NOT_FOUND},
                new Object[]{"9/5/2014", "tomorrow", HttpStatus.SC_NOT_FOUND},
        };
    }

    /**
     * Tests the illegal date values provided by {@link #illegalDateProvider()}.
     * <p>
     * Uses the default API Server connector to avoid the automatic URL encoding done for convenience
     * in {@link TestBase#getTraffic(Experiment, String, String, int)}.
     *
     * @param start          the start date
     * @param end            the end date
     * @param expectedStatus the expected status
     */
    @Test(dependsOnGroups = "ping", dataProvider = "illegalDateProvider")
    public void testIllegalDateInputs(String start, String end, int expectedStatus) {
        getTraffic(experiments.get(0), start, end, expectedStatus, apiServerConnector);
    }

    /**
     * Deletes all experiments.
     */
    @AfterClass
    public void cleanUp() {
        toCleanUp.addAll(experiments);
        cleanUpExperiments();
    }
}
