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
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.accessor.ExclusionAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CassandraMutexRepositoryITest extends IntegrationTestBase {

    ExclusionAccessor accessor;

    CassandraMutexRepository repository;

    Application.Name applicationName;


    private Mapper<com.intuit.wasabi.repository.cassandra.pojo.Exclusion> mapper;

    private CassandraDriver driver;

    private ExperimentAccessor experimentAccessor;
    static Date date1 = new Date();
    static Date date2 = new Date();


    @Before
    public void setUp() throws Exception {
        IntegrationTestBase.setup();
        if (repository != null) return;

        driver = injector.getInstance(CassandraDriver.class);
        manager = new MappingManager(session);
        mapper = manager.mapper(com.intuit.wasabi.repository.cassandra.pojo.Exclusion.class);
        accessor = manager.createAccessor(ExclusionAccessor.class);
        experimentAccessor = manager.createAccessor(ExperimentAccessor.class);
        repository = new CassandraMutexRepository(experimentAccessor, accessor, driver);
        applicationName = Application.Name.valueOf("TestApplicationName");
    }

    @Test
    public void testGetExclusionsSuccess() {

        Experiment.ID base = Experiment.ID.newInstance();
        Experiment.ID pair = Experiment.ID.newInstance();

        experimentAccessor.insertExperiment(base.getRawID(),
                "d1", "yes", "r1", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l1",
                "app1", date1, date2, true,
                "m1", "v1", true, 5000, "c1", null,null,null);
        experimentAccessor.insertExperiment(pair.getRawID(),
                "d2", "no", "r2", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l2",
                "app2", date1, date2, true,
                "m2", "v2", true, 5000, "c2", null,null,null);

        repository.createExclusion(base, pair);

        ExperimentList exclusionList = repository.getExclusions(base);

        assertEquals("Size should be same", 1, exclusionList.getExperiments().size());
        assertEquals("Size should be same", pair, exclusionList.getExperiments().get(0).getID());

        repository.deleteExclusion(base, pair);

    }

    @Test
    public void testGetExclusionsWithTwoExclusiosSuccess() {

        Experiment.ID base = Experiment.ID.newInstance();
        Experiment.ID pair1 = Experiment.ID.newInstance();
        Experiment.ID pair2 = Experiment.ID.newInstance();

        experimentAccessor.insertExperiment(base.getRawID(),
                "d1", "yes", "r1", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l1",
                "app1", date1, date2, true,
                "m1", "v1", true, 5000, "c1", null,null,null);
        experimentAccessor.insertExperiment(pair1.getRawID(),
                "d2", "yes", "r2", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l2",
                "app2", date1, date2, true,
                "m2", "v2", true, 5000, "c2", null,null,null);
        experimentAccessor.insertExperiment(pair2.getRawID(),
                "d2", "yes", "r2", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l2",
                "app2", date1, date2, true,
                "m2", "v2", true, 5000, "c2", null,null,null);

        repository.createExclusion(base, pair1);
        repository.createExclusion(base, pair2);

        ExperimentList exclusionList = repository.getExclusions(base);

        assertEquals("Size should be same", 2, exclusionList.getExperiments().size());

        Set<Experiment.ID> setOfExperimentIds = new HashSet<>();
        setOfExperimentIds.add(pair1);
        setOfExperimentIds.add(pair2);

        assertTrue("Ids should be in set", setOfExperimentIds.contains(
                exclusionList.getExperiments().get(0).getID()));
        assertTrue("Ids should be in set", setOfExperimentIds.contains(
                exclusionList.getExperiments().get(1).getID()));

        repository.deleteExclusion(base, pair1);
        repository.deleteExclusion(base, pair2);

    }

    @Test
    public void testGetNotExclusionsSuccess() {
        session.execute("truncate wasabi_experiments.exclusion");

        Experiment.ID base = Experiment.ID.newInstance();
        Experiment.ID pair1 = Experiment.ID.newInstance();
        Experiment.ID notExclusion = Experiment.ID.newInstance();

        String appName = "app" + System.currentTimeMillis();

        experimentAccessor.insertExperiment(base.getRawID(),
                "d1", "yes", "r1", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l1",
                appName, date1, date2, true,
                "m1", "v1", true, 5000, "c1", null,null,null);

        experimentAccessor.insertExperiment(pair1.getRawID(),
                "d2", "yes", "r2", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l2",
                appName, date1, date2, true,
                "m2", "v2", true, 5000, "c2", null,null,null);

        experimentAccessor.insertExperiment(notExclusion.getRawID(),
                "d2", "yes", "r2", "", 1.0, date1, date2,
                com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l2",
                appName, date1, date2, true,
                "m2", "v2", true, 5000, "c2", null,null,null);

        repository.createExclusion(base, pair1);

        ExperimentList exclusionList = repository.getExclusions(base);

        assertEquals("Size should be same", 1, exclusionList.getExperiments().size());
        assertEquals("value should be same", pair1, exclusionList.getExperiments().get(0).getID());

        ExperimentList notExclusionList = repository.getNotExclusions(base);

        assertEquals("Size should be same", 1, notExclusionList.getExperiments().size());
        assertEquals("value should be same", notExclusion, notExclusionList.getExperiments().get(0).getID());

        repository.deleteExclusion(base, pair1);
        repository.deleteExclusion(base, notExclusion);
    }

    @Test
    public void testCreateAndDeleteExclusionOneBaseOnePairIdSuccess() {
        Experiment.ID base = Experiment.ID.newInstance();
        Experiment.ID pair = Experiment.ID.newInstance();

        repository.createExclusion(base, pair);

        List<ID> exclusionList = repository.getExclusionList(base);
        assertEquals("Size should be same", 1, exclusionList.size());
        assertEquals("Values should be same", exclusionList.get(0), pair);

        List<ID> exclusionListReverse = repository.getExclusionList(pair);
        assertEquals("Size should be same", 1, exclusionListReverse.size());
        assertEquals("Values should be same", exclusionListReverse.get(0), base);

        repository.deleteExclusion(base, pair);

        List<ID> exclusionListAfterDeleteBase = repository.getExclusionList(base);
        assertEquals("Size should be same", 0, exclusionListAfterDeleteBase.size());

        List<ID> exclusionListReverseAfterDeletePair = repository.getExclusionList(pair);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeletePair.size());
    }

    @Test
    public void testCreateAndDeleteReverseExclusionOneBaseOnePairIdSuccess() {
        Experiment.ID base = Experiment.ID.newInstance();
        Experiment.ID pair = Experiment.ID.newInstance();

        repository.createExclusion(base, pair);

        List<ID> exclusionList = repository.getExclusionList(base);
        assertEquals("Size should be same", 1, exclusionList.size());
        assertEquals("Values should be same", exclusionList.get(0), pair);

        List<ID> exclusionListReverse = repository.getExclusionList(pair);
        assertEquals("Size should be same", 1, exclusionListReverse.size());
        assertEquals("Values should be same", exclusionListReverse.get(0), base);

        repository.deleteExclusion(pair, base);

        List<ID> exclusionListAfterDeleteBase = repository.getExclusionList(base);
        assertEquals("Size should be same", 0, exclusionListAfterDeleteBase.size());

        List<ID> exclusionListReverseAfterDeletePair = repository.getExclusionList(pair);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeletePair.size());
    }

    @Test
    public void testCreateDeleteExclusionOneBaseTwoPairIdSuccess() {
        Experiment.ID base = Experiment.ID.newInstance();
        Experiment.ID pair1 = Experiment.ID.newInstance();
        Experiment.ID pair2 = Experiment.ID.newInstance();

        repository.createExclusion(base, pair1);
        repository.createExclusion(base, pair2);

        List<ID> exclusionList = repository.getExclusionList(base);
        assertEquals("Size should be same", 2, exclusionList.size());
        assertTrue("Values should be in the list", exclusionList.contains(pair1));
        assertTrue("Values should be in the list", exclusionList.contains(pair2));

        List<ID> exclusionListReverse1 = repository.getExclusionList(pair1);
        assertEquals("Size should be same", 1, exclusionListReverse1.size());
        assertEquals("Values should be same", exclusionListReverse1.get(0), base);

        List<ID> exclusionListReverse2 = repository.getExclusionList(pair2);
        assertEquals("Size should be same", 1, exclusionListReverse2.size());
        assertEquals("Values should be same", exclusionListReverse2.get(0), base);

        repository.deleteExclusion(base, pair1);

        List<ID> exclusionListReverseAfterDeleteBase = repository.getExclusionList(base);
        assertEquals("Size should be same", 1, exclusionListReverseAfterDeleteBase.size());
        assertEquals("Values should be same", exclusionListReverseAfterDeleteBase.get(0),
                pair2);

        List<ID> exclusionListReverseAfterDeletePair1 = repository.getExclusionList(pair1);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeletePair1.size());

        List<ID> exclusionListReverseAfterDeletePair2 = repository.getExclusionList(pair2);
        assertEquals("Size should be same", 1, exclusionListReverseAfterDeletePair2.size());
        assertEquals("Values should be same", exclusionListReverseAfterDeletePair2.get(0),
                base);

        repository.deleteExclusion(base, pair2);

        exclusionListReverseAfterDeleteBase = repository.getExclusionList(base);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeleteBase.size());

        exclusionListReverseAfterDeletePair1 = repository.getExclusionList(pair1);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeletePair1.size());

        exclusionListReverseAfterDeletePair2 = repository.getExclusionList(pair2);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeletePair2.size());
    }


    @Test
    public void testCreateDeleteReverseExclusionOneBaseTwoPairIdSuccess() {
        Experiment.ID base = Experiment.ID.newInstance();
        Experiment.ID pair1 = Experiment.ID.newInstance();
        Experiment.ID pair2 = Experiment.ID.newInstance();

        repository.createExclusion(base, pair1);
        repository.createExclusion(base, pair2);

        List<ID> exclusionList = repository.getExclusionList(base);
        assertEquals("Size should be same", 2, exclusionList.size());
        assertTrue("Values should be in the list", exclusionList.contains(pair1));
        assertTrue("Values should be in the list", exclusionList.contains(pair2));

        List<ID> exclusionListReverse1 = repository.getExclusionList(pair1);
        assertEquals("Size should be same", 1, exclusionListReverse1.size());
        assertEquals("Values should be same", exclusionListReverse1.get(0), base);

        List<ID> exclusionListReverse2 = repository.getExclusionList(pair2);
        assertEquals("Size should be same", 1, exclusionListReverse2.size());
        assertEquals("Values should be same", exclusionListReverse2.get(0), base);

        repository.deleteExclusion(pair1, base);

        List<ID> exclusionListReverseAfterDeleteBase = repository.getExclusionList(base);
        assertEquals("Size should be same", 1, exclusionListReverseAfterDeleteBase.size());
        assertEquals("Values should be same", exclusionListReverseAfterDeleteBase.get(0),
                pair2);

        List<ID> exclusionListReverseAfterDeletePair1 = repository.getExclusionList(pair1);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeletePair1.size());

        List<ID> exclusionListReverseAfterDeletePair2 = repository.getExclusionList(pair2);
        assertEquals("Size should be same", 1, exclusionListReverseAfterDeletePair2.size());
        assertEquals("Values should be same", exclusionListReverseAfterDeletePair2.get(0),
                base);

        repository.deleteExclusion(pair2, base);

        exclusionListReverseAfterDeleteBase = repository.getExclusionList(base);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeleteBase.size());

        exclusionListReverseAfterDeletePair1 = repository.getExclusionList(pair1);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeletePair1.size());

        exclusionListReverseAfterDeletePair2 = repository.getExclusionList(pair2);
        assertEquals("Size should be same", 0, exclusionListReverseAfterDeletePair2.size());
    }
}
