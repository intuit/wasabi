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

import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.intuit.wasabi.repository.PagesRepository;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.data.PageRepositoryDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Test
@Guice(modules = CassandraRepositoryModule.class)
public class CassandraPagesRepositoryIT {
    private final Logger logger = LoggerFactory.getLogger(CassandraPagesRepositoryIT.class);
    @Inject
    PagesRepository pagesRepository;
    @Inject
    MappingManager mappingManager;
    @Inject
    ExperimentAccessor experimentAccessor;
    Map<Page.Name, List<PageExperiment>> pageExperimentListMap = new HashMap<>();
    Map<Application.Name, List<Page>> pageListMap = new HashMap<>();
    Set<Experiment.ID> experimentIds = new HashSet<>();

    @Test(groups = {"setup"},
            dataProvider = "postPagesDataProvider",
            dataProviderClass = PageRepositoryDataProvider.class)
    public void setup(Application.Name appName,
                      Experiment.ID experimentId,
                      Experiment.Label label,
                      ExperimentPageList experimentPageList) {
        List<Page> pageList = pageListMap.getOrDefault(appName, new ArrayList<>());
        experimentIds.add(experimentId);
        for (ExperimentPage experimentPage : experimentPageList.getPages()) {
            List<PageExperiment> pageExperimentList = pageExperimentListMap.getOrDefault(experimentPage.getName(), new ArrayList<>());
            pageExperimentList.add(PageExperiment.withAttributes(experimentId, label, experimentPage.getAllowNewAssignment()).build());
            pageExperimentListMap.put(experimentPage.getName(), pageExperimentList);
            pageList.add(Page.withName(experimentPage.getName()).build());
        }

        pageListMap.put(appName, pageList);
    }


    @Test(groups = {"setup"},
            dataProvider = "postPagesDataProvider",
            dataProviderClass = PageRepositoryDataProvider.class)
    public void prepare(Application.Name appName,
                        Experiment.ID experimentId,
                        Experiment.Label label,
                        ExperimentPageList experimentPageList) {
        //TODO: how to insert just partial experiment for testing without creating tombstones
        experimentAccessor.insertExperiment(experimentId.getRawID(),
                "",
                "",
                1.0,
                new Date(),
                new Date(),
                "DRAFT",
                label.toString(),
                appName.toString(),
                new Date(),
                new Date(),
                false,
                "",
                "",
                false,
                1_000_000,
                "TestNG");
        pagesRepository.postPages(appName, experimentId, experimentPageList);
    }


    @Test(dependsOnGroups = "setup", groups = "getTest",
            dataProvider = "postPagesDataProvider",
            dataProviderClass = PageRepositoryDataProvider.class)
    public void testGetByExperimenList(Application.Name appName,
                                       Experiment.ID experimentId,
                                       Experiment.Label label,
                                       ExperimentPageList experimentPageList) {
        Map<Page.Name, List<PageExperiment>> result = pagesRepository.getPageExperimentList(appName);
        for (ExperimentPage page : experimentPageList.getPages()) {
            List<PageExperiment> pageExperimentList = result.get(page.getName());
            assertThat(pageExperimentList, is(notNullValue()));
            PageExperiment pageExperiment = PageExperiment.withAttributes(experimentId, label, page.getAllowNewAssignment()).build();
            assertThat(page.getAllowNewAssignment(), is(pageExperiment.getAllowNewAssignment()));
            List<Experiment.Label> labels = pageExperimentList.stream().map(t -> t.getLabel()).collect(Collectors.toList());
            assertThat(labels, hasItem(label));
        }
    }

    @Test(dependsOnGroups = "setup", groups = "getTest",
            dataProvider = "postPagesDataProvider",
            dataProviderClass = PageRepositoryDataProvider.class)
    public void testGetExperimentPages(Application.Name appName,
                                       Experiment.ID experimentId,
                                       Experiment.Label label,
                                       ExperimentPageList experimentPageList) {
        ExperimentPageList experimentPageListResult = pagesRepository.getExperimentPages(experimentId);
        assertThat(experimentPageList.getPages(), is(experimentPageListResult.getPages()));
    }


    @Test(dependsOnGroups = "setup", groups = "getTest",
            dataProvider = "postPagesDataProvider",
            dataProviderClass = PageRepositoryDataProvider.class)
    public void testGetExperiments(Application.Name appName,
                                   Experiment.ID experimentId,
                                   Experiment.Label labe,
                                   ExperimentPageList experimentPageList) {
        for (ExperimentPage page : experimentPageList.getPages()) {
            List<PageExperiment> result = pagesRepository.getExperiments(appName, page.getName());
            List<PageExperiment> expected = pageExperimentListMap.get(page.getName());
            for (PageExperiment pageExperiment : result) {
                assertThat(expected, hasItem(pageExperiment));
            }
        }
    }

    @Test(dependsOnGroups = "setup", groups = "getTest",
            dataProvider = "postPagesDataProvider",
            dataProviderClass = PageRepositoryDataProvider.class)
    public void testGetPageList(Application.Name appName,
                                Experiment.ID experimentId,
                                Experiment.Label label,
                                ExperimentPageList experimentPageList) {
        List<Page> result = pagesRepository.getPageList(appName);
        List<Page> expected = pageListMap.get(appName);
        for (Page page : result) {
            assertThat(expected, hasItem(page));
        }
    }

    @Test(dependsOnGroups = "setup", groups = "getTest",
            dataProvider = "postPagesDataProvider",
            dataProviderClass = PageRepositoryDataProvider.class)
    public void testRemovePageData(Application.Name appName,
                                   Experiment.ID experimentId,
                                   Experiment.Label label,
                                   ExperimentPageList experimentPageList) {
        ExperimentPageList oldPageList = pagesRepository.getExperimentPages(experimentId);
        assertThat(oldPageList.getPages().size(), is(experimentPageList.getPages().size()));
        pagesRepository.erasePageData(appName, experimentId);
        ExperimentPageList newPageList = pagesRepository.getExperimentPages(experimentId);
        assertThat(newPageList.getPages().size(), is(0));
        assertThat(oldPageList.getPages().size(), is(not(newPageList.getPages().size())));
    }


    @AfterTest
    public void cleanup() {
        logger.debug("cleaning up experiment table and experiment_audit_log table");
//        mappingManager.getSession().execute("TRUNCATE TABLE experiment");
//        mappingManager.getSession().execute("TRUNCATE TABLE experiment_audit_log");
        for (Experiment.ID experimentId : experimentIds) {
            experimentAccessor.deleteExperiment(experimentId.getRawID());
            mappingManager.getSession().execute("DELETE FROM experiment_audit_log where experiment_id=" + experimentId);
        }

        logger.debug("cleaning up page_experiment_index table");
        mappingManager.getSession().execute("TRUNCATE TABLE page_experiment_index");
        logger.debug("cleaning up experiment_page table");
        mappingManager.getSession().execute("TRUNCATE TABLE experiment_page");
//        logger.debug("cleaning up app_page_index table");
//        mappingManager.getSession().execute("TRUNCATE TABLE app_page_index");
    }


}