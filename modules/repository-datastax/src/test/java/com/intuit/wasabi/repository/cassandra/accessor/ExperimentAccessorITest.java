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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.pojo.Experiment;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ExperimentAccessorITest extends IntegrationTestBase {
    static ExperimentAccessor accessor;
    static UUID experimentId1 = UUID.randomUUID();
    static UUID experimentId2 = UUID.randomUUID();
    static Date date1 = new Date();
    static Date date2 = new Date();

    @BeforeClass
    public static void setup() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = manager.createAccessor(ExperimentAccessor.class);
    }

    @Test
    public void insertOneExperiments() {
        accessor.insertExperiment(experimentId1,
                "d1", "yes", "r1", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l1",
                "app1", date1, date2, true,
                "m1", "v1", true, 5000, "c1", null, null,null);

        Result<Experiment> experiment1 = accessor.getExperimentById(experimentId1);
        List<Experiment> experimentResult = experiment1.all();
        assertEquals("size should be same", 1, experimentResult.size());
        Experiment exp = experimentResult.get(0);
        assertEquals("Value should be same", experimentId1, exp.getId());
        assertEquals("Value should be same", "d1", exp.getDescription());
        assertEquals("Value should be same", "yes", exp.getHypothesisIsCorrect());
        assertEquals("Value should be same", "r1", exp.getResults());
        assertEquals("Value should be same", "", exp.getRule());
        assertEquals("Value should be same", 1.0, exp.getSamplePercent(), 0.0001d);
        assertEquals("Value should be same", date1, exp.getStartTime());
        assertEquals("Value should be same", date2, exp.getEndTime());
        assertEquals("Value should be same", com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(),
                exp.getState());
        assertEquals("Value should be same", "l1", exp.getLabel());
        assertEquals("Value should be same", "app1", exp.getAppName());
        assertEquals("Value should be same", date1, exp.getCreated());
        assertEquals("Value should be same", date2, exp.getModified());
        assertEquals("Value should be same", 5000, exp.getUserCap());
        assertEquals("Value should be same", "c1", exp.getCreatorId());

    }

    @Test
    public void insertTwoExperiments() {
        accessor.insertExperiment(experimentId1,
                "d1", "yes", "r1", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l1",
                "app1", date1, date2, true,
                "m1", "v1", true, 5000, "c1", null,null,null);
        accessor.insertExperiment(experimentId2,
                "d2", "no", "r2", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l2",
                "app2", date1, date2, true,
                "m2", "v2", true, 5000, "c2", null,null,null);


        List<UUID> experimentIds = new ArrayList<>();
        experimentIds.add(experimentId1);
        experimentIds.add(experimentId2);
        experimentIds.sort(new Comparator<UUID>() {

            @Override
            public int compare(UUID o1, UUID o2) {
                return o1.compareTo(o2);
            }
        });

        List<Experiment> experimentResult = new ArrayList<>(2);
        experimentIds.forEach(expId -> experimentResult.add(accessor.getExperimentById(expId).one()));

        experimentResult.sort(new Comparator<Experiment>() {
            @Override
            public int compare(Experiment o1, Experiment o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });

        assertEquals("size should be same", 2, experimentResult.size());

        Experiment exp1 = experimentResult.get(0);

        assertEquals("Value should be same", experimentIds.get(0), exp1.getId());
        Experiment exp2 = experimentResult.get(1);

        assertEquals("Value should be same", experimentIds.get(1), exp2.getId());
    }
}