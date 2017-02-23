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
package com.intuit.wasabi.tests.service;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.ModelAssert;
import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.jayway.restassured.path.json.JsonPath;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class PaginationTest extends TestBase {
    private final List<Experiment> experimentList = new ArrayList<>(12);
    private final String experimentPrefix = "PaginationExperimentTestExperiments";

    public PaginationTest() {
        setResponseLogLengthLimit(500);
    }

    /**
     * Sets up 12 experiments to test pagination properly.
     * <p>
     * The experiments can easily be ordered from first to last ascending by their name (ending in Exp00...11)
     * or descending by their sampling percentage (100...89).
     */
    @BeforeClass
    public void setup() {
        cleanup();
        for (int i = 0; i < 12; i++) {
            experimentList.add(postExperiment(
                    ExperimentFactory.createCompleteExperiment()
                            .setLabel(String.format(experimentPrefix + "Exp%02d", i))
                            .setSamplingPercent((100 - i) / 100.0d)));
        }
    }

    @AfterClass
    private void cleanup() {
        List<Map<String, Object>> experimentMaps = apiServerConnector
                .doGet("experiments?per_page=-1&filter=" + experimentPrefix)
                .jsonPath().getList("experiments");
        List<Experiment> experiments = experimentMaps.stream()
                .map(experimentMap -> ExperimentFactory.createFromJSONString(simpleGson.toJson(experimentMap)))
                .collect(Collectors.toList());
        deleteExperiments(experiments);
    }

    @Test(dependsOnGroups = {"ping"}, groups = {"pagination_smoke"})
    public void t_ExperimentPaginationSmokeByName() {
        List<Experiment> results = getPaginatedExperiments(2, 4, "experiment_label", null);

        ModelAssert.assertEqualModelItems(results, experimentList.subList(4, 8));
    }

    @Test(dependsOnGroups = {"ping"}, groups = {"pagination_smoke"})
    public void t_ExperimentPaginationSmokeByCreationTime() {
        List<Experiment> results = getPaginatedExperiments(4, 2, "-creation_time", null);

        List<Experiment> expected = new ArrayList<>(experimentList.subList(4, 6));
        Collections.reverse(expected);

        ModelAssert.assertEqualModelItems(results, expected);
    }

    @Test(dependsOnGroups = {"pagination_smoke"}, groups = {"pagination_pages"})
    public void t_ExperimentPaginationNonExistingLatePage() {
        List<Experiment> results = getPaginatedExperiments(4, 10, "experiment_label", null);

        ModelAssert.assertEqualModelItems(results, Collections.<Experiment>emptyList());
    }

    @Test(dependsOnGroups = {"pagination_smoke"}, groups = {"pagination_pages"})
    public void t_ExperimentPaginationNonExistingNegativePage() {
        List<Experiment> results = getPaginatedExperiments(-4, 10, "experiment_label", null);

        ModelAssert.assertEqualModelItems(results, Collections.<Experiment>emptyList());
    }

    @Test(dependsOnGroups = {"pagination_smoke"}, groups = {"pagination_pages"})
    public void t_ExperimentPaginationEmptyPage() {
        List<Experiment> results = getPaginatedExperiments(1, 0, "experiment_label", null);
        ModelAssert.assertEqualModelItems(results, Collections.<Experiment>emptyList());
    }

    @Test(dependsOnGroups = {"pagination_smoke"}, groups = {"pagination_pages"})
    public void t_ExperimentPaginationFilterByName() {
        List<Experiment> results = getPaginatedExperiments(1, 10, "experiment_label", "experiment_label=5");

        ModelAssert.assertEqualModelItems(results, experimentList.subList(5, 6));
    }

    @Test(dependsOnGroups = {"pagination_smoke"}, groups = {"pagination_pages"})
    public void t_ExperimentPaginationFilterByIllegalKey() {
        String exception = getPaginationException(1, 10, "experiment_label", "illegal_key=some_value");

        Assert.assertTrue(exception.contains("illegal_key"));
    }

    @Test(dependsOnGroups = {"pagination_smoke"}, groups = {"pagination_pages"})
    public void t_ExperimentPaginationSortByIllegalKey() {
        String exception = getPaginationException(1, 10, "non_existent_sortorder", null);

        Assert.assertTrue(exception.contains("non_existent_sortorder"));
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_date_constraints"})
    public void t_ExperimentPaginationFilterByDateConstraintAfter() {
        List<Experiment> results = getPaginatedExperiments(1, 10, "experiment_label", "date_constraint_start=isafter:03/15/2000");

        ModelAssert.assertEqualModelItems(results, experimentList.subList(0, 10));
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_date_constraints"})
    public void t_ExperimentPaginationFilterByDateConstraintBefore() {
        List<Experiment> results = getPaginatedExperiments(1, 10, "experiment_label", "date_constraint_start=isbefore:03/15/3000");

        ModelAssert.assertEqualModelItems(results, experimentList.subList(0, 10));
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_date_constraints"})
    public void t_ExperimentPaginationFilterByDateConstraintOn() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = "00/00/0000";
        try {
            date = sdf.format(TestUtils.parseTime(experimentList.get(0).startTime).getTime());
        } catch (ParseException exception) {
            Assert.fail("Parsing of date failed!");
        }
        List<Experiment> results = getPaginatedExperiments(2, 10, "experiment_label",
                "date_constraint_start=ison:" + date);

        ModelAssert.assertEqualModelItems(results, experimentList.subList(10, 12));
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_date_constraints"})
    public void t_ExperimentPaginationFilterByDateConstraintBetween() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = "00/00/0000";
        try {
            date = sdf.format(TestUtils.parseTime(experimentList.get(0).startTime).getTime());
        } catch (ParseException exception) {
            Assert.fail("Parsing of date failed!");
        }
        List<Experiment> results = getPaginatedExperiments(2, 10, "experiment_label", "date_constraint_start=isbetween:"
                + date + ":10/05/3000");

        ModelAssert.assertEqualModelItems(results, experimentList.subList(10, 12));
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_date_constraints"})
    public void t_ExperimentPaginationFilterByIllegalDateConstraints() {
        String exception = getPaginationException(1, 10, "experiment_label", "date_constraint_start=isafter:15/3/2000");
        Assert.assertTrue(exception.contains("Wrong format"), "Format day/month");

        exception = getPaginationException(1, 10, "experiment_label", "date_constraint_start=isbetween:5/5/2014");
        Assert.assertTrue(exception.contains("Wrong format"), "Format isBetween one arg");

        exception = getPaginationException(1, 10, "experiment_label", "date_constraint_start=isbetween:5/5/2014:5");
        Assert.assertTrue(exception.contains("Wrong format"), "Format isBetween partial");

        exception = getPaginationException(1, 10, "experiment_label", "date_constraint_start=ison:");
        Assert.assertTrue(exception.contains("Wrong format"), "Format isOn no arg");

        exception = getPaginationException(1, 10, "experiment_label", "date_constraint_end=isbefore:1");
        Assert.assertTrue(exception.contains("Wrong format"), "Format isBefore partial");
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_experiment_state"})
    public void t_ExperimentPaginationFilterByStateNotTerminated() {
        List<Experiment> results = getPaginatedExperiments(1, 5, "experiment_label", "state_exact=notterminated");

        ModelAssert.assertEqualModelItems(results, experimentList.subList(0, 5));
    }


    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_experiment_state"})
    public void t_ExperimentPaginationFilterByStateAny() {
        List<Experiment> results = getPaginatedExperiments(1, 5, "experiment_label", "state_exact=any");

        ModelAssert.assertEqualModelItems(results, experimentList.subList(0, 5));
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_experiment_state"})
    public void t_ExperimentPaginationFilterByStateTerminated() {
        List<Experiment> results = getPaginatedExperiments(1, 5, "experiment_label", "state_exact=terminated");

        ModelAssert.assertEqualModelItems(results, Collections.<Experiment>emptyList());
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_experiment_state"})
    public void t_ExperimentPaginationFilterByStateRunning() {
        List<Experiment> results = getPaginatedExperiments(1, 5, "experiment_label", "state_exact=running");

        ModelAssert.assertEqualModelItems(results, Collections.<Experiment>emptyList());
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_experiment_state"})
    public void t_ExperimentPaginationFilterByStateDraft() {
        List<Experiment> results = getPaginatedExperiments(1, 5, "experiment_label", "state_exact=draft");

        ModelAssert.assertEqualModelItems(results, experimentList.subList(0, 5));
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"pagination_experiment_state"})
    public void t_ExperimentPaginationFilterByStatePaused() {
        List<Experiment> results = getPaginatedExperiments(1, 5, "experiment_label", "state_exact=paused");

        ModelAssert.assertEqualModelItems(results, Collections.<Experiment>emptyList());
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"auditlog_smoke"})
    public void t_AuditLogPaginationSmoke() {
        List<Map<String, Object>> auditLogEntryMaps = apiServerConnector
                .doGet("logs?per_page=10&page=1&sort=time&filter=" + experimentPrefix + ",username=" + appProperties.getProperty("user-name"))
                .jsonPath()
                .getList("logEntries");

        Assert.assertEquals(auditLogEntryMaps.size(), 10, "There is not the correct number of audit log entries.");
    }

    @Test(dependsOnGroups = {"pagination_pages"}, groups = {"experiment_details"})
    public void t_ExperimentDetailsPaginationSmoke() {

        List<Map<String, Object>> experimentDetails = apiServerConnector
                .doGet("analytics/experiments?per_page=10&page=1&filter=" + experimentPrefix + ",state=DRAFT")
                .jsonPath()
                .getList("experimentDetails");
        Assert.assertNotNull(experimentDetails);
        Assert.assertEquals(experimentDetails.size(), 10, "There is not the correct number of ExperimentDetail entries.");
    }

    private List<Experiment> getPaginatedExperiments(int page, int perPage, String sort, String filter) {
        List<Map<String, Object>> experimentMaps = getExperimentJsonPath(page, perPage, sort, filter)
                .getList("experiments");
        return experimentMaps.stream()
                .map(experimentMap -> ExperimentFactory.createFromJSONString(simpleGson.toJson(experimentMap)))
                .collect(Collectors.toList());
    }

    private String getPaginationException(int page, int perPage, String sort, String filter) {
        return getExperimentJsonPath(page, perPage, sort, filter).getString("error.message");
    }

    private JsonPath getExperimentJsonPath(int page, int perPage, String sort, String filter) {
        return apiServerConnector.doGet("experiments?per_page=" + perPage
                + "&page=" + page
                + "&sort=" + (sort != null ? sort : "")
                + "&filter=" + experimentPrefix + (filter != null ? "," + filter : "")
                + "&timezone=+0000").jsonPath();
    }
}
