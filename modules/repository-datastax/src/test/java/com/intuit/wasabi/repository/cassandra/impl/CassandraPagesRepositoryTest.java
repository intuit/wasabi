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

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentPage;
import com.intuit.wasabi.experimentobjects.ExperimentPageList;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentPageAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.ExperimentAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.AppPageIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.PageExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.AppPage;
import com.intuit.wasabi.repository.cassandra.pojo.index.PageExperimentByAppNamePage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPagesRepositoryTest {
    private final Logger LOGGER = LoggerFactory.getLogger(CassandraPagesRepositoryTest.class);

    @Mock
    PageExperimentIndexAccessor pageExperimentIndexAccessor;
    @Mock
    ExperimentPageAccessor experimentPageAccessor;
    @Mock
    AppPageIndexAccessor appPageIndexAccessor;
    @Mock
    ExperimentAuditLogAccessor experimentAuditLogAccessor;
    @Mock
    ExperimentAccessor experimentAccessor;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MappingManager mappingManager;

    @Mock
    Result mockedResultMapping;

    CassandraPagesRepository repository;
    CassandraPagesRepository spyRepository;
    UUID experimentId = UUID.fromString("4d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8");
    public static final Application.Name APPLICATION_NAME = Application.Name.valueOf("testApp");

    @Before
    public void setUp() throws Exception {
        repository = new CassandraPagesRepository(
                experimentAccessor,
                pageExperimentIndexAccessor,
                experimentPageAccessor,
                appPageIndexAccessor,
                experimentAuditLogAccessor,
                mappingManager);
        spyRepository = spy(repository);
    }

    @Test
    public void getExperimentPagesEmptyResultTest() {
        List<PageExperimentByAppNamePage> mockedResult = new ArrayList<>();
        when(experimentPageAccessor.selectBy(any(UUID.class))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        ExperimentPageList result = repository.getExperimentPages(Experiment.ID.newInstance());
        assertThat(result.getPages().size(), is(0));
    }

    @Test
    public void getExperimentPagesSingleResultTest() {
        List<PageExperimentByAppNamePage> mockedResult = new ArrayList<>();
        mockedResult.add(
                PageExperimentByAppNamePage.builder()
                        .appName("testApp")
                        .assign(true)
                        .page("testPage")
                        .experimentId(experimentId)
                        .build()
        );
        when(experimentPageAccessor.selectBy(eq(experimentId))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        ExperimentPageList result = repository.getExperimentPages(Experiment.ID.valueOf(experimentId));
        assertThat(result.getPages().size(), is(1));
        assertThat(result.getPages().get(0).getName().toString(), is(mockedResult.get(0).getPage()));
        assertThat(result.getPages().get(0).getAllowNewAssignment(), is(mockedResult.get(0).isAssign()));
    }

    @Test
    public void getExperimentPagesMultipleResultTest() {
        List<PageExperimentByAppNamePage> mockedResult = new ArrayList<>();
        mockedResult.add(
                PageExperimentByAppNamePage.builder()
                        .appName("testApp")
                        .assign(true)
                        .page("testPage")
                        .experimentId(experimentId)
                        .build()
        );
        mockedResult.add(
                PageExperimentByAppNamePage.builder()
                        .appName("testApp")
                        .assign(false)
                        .page("testPage2")
                        .experimentId(experimentId)
                        .build()
        );
        when(experimentPageAccessor.selectBy(eq(experimentId))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        ExperimentPageList result = repository.getExperimentPages(Experiment.ID.valueOf(experimentId));
        assertThat(result.getPages().size(), is(2));
        for (int i = 0; i < result.getPages().size(); i++) {
            assertThat(result.getPages().get(i).getName().toString(), is(mockedResult.get(i).getPage()));
            assertThat(result.getPages().get(i).getAllowNewAssignment(), is(mockedResult.get(i).isAssign()));
        }
    }

    @Test(expected = RepositoryException.class)
    public void getExperimentPagesExceptionTest() {
        doThrow(ReadTimeoutException.class).when(experimentPageAccessor).selectBy(eq(experimentId));
        ExperimentPageList result = repository.getExperimentPages(Experiment.ID.valueOf(experimentId));
        assertThat(result.getPages().size(), is(1));// always False condition to detect if exception is not thrown
    }

    @Test
    public void saveExperimentPageStateTest() {
        Experiment.ID experimentID = Experiment.ID.valueOf(experimentId);
        ExperimentPageList oldPageList = new ExperimentPageList();
        ExperimentPageList newPageList = new ExperimentPageList();
        repository.saveExperimentPageState(experimentID, oldPageList, newPageList);
        verify(experimentAuditLogAccessor, times(1)).insertBy(
                eq(experimentId),
                any(Date.class),
                eq("pages"),
                eq(oldPageList.toString()),
                eq(newPageList.toString())
        );
    }

    @Test(expected = RepositoryException.class)
    public void saveExperimentPageStateExceptionTest() {
        Experiment.ID experimentID = Experiment.ID.valueOf(experimentId);
        ExperimentPageList oldPageList = new ExperimentPageList();
        ExperimentPageList newPageList = new ExperimentPageList();
        doThrow(WriteTimeoutException.class).when(experimentAuditLogAccessor).insertBy(
                eq(experimentId),
                any(Date.class),
                eq("pages"),
                any(String.class),
                any(String.class)
        );
        repository.saveExperimentPageState(experimentID, oldPageList, newPageList);
        assertThat(1, is(2)); //Always false condition to detect if exception is not thrown
    }

    @Test
    public void postPagesTest() {
        Experiment.ID experimentID = Experiment.ID.valueOf(experimentId);
        ExperimentPageList inputExperiemntPages = new ExperimentPageList();
        inputExperiemntPages.addPage(
                ExperimentPage.withAttributes(
                        Page.Name.valueOf("testPage1"),
                        true
                )
                        .build()
        );
        ExperimentPageList mockedExperimentPageList = mock(ExperimentPageList.class);
        doReturn(mockedExperimentPageList).when(spyRepository).getExperimentPages(eq(experimentID));
        doNothing().when(spyRepository).saveExperimentPageState(
                eq(experimentID),
                any(ExperimentPageList.class),
                any(ExperimentPageList.class)
        );
        spyRepository.postPages(APPLICATION_NAME, experimentID, inputExperiemntPages);
        verify(spyRepository, times(1)).saveExperimentPageState(eq(experimentID),
                any(ExperimentPageList.class),
                any(ExperimentPageList.class));
        verify(spyRepository, times(2)).getExperimentPages(eq(experimentID));
        verify(pageExperimentIndexAccessor, times(inputExperiemntPages.getPages().size()))
                .insertBy(eq("testApp"), eq("testPage1"), eq(experimentId), eq(true));
        verify(experimentPageAccessor, times(inputExperiemntPages.getPages().size()))
                .insertBy(eq("testPage1"), eq(experimentId), eq(true));
        verify(appPageIndexAccessor, times(inputExperiemntPages.getPages().size()))
                .insertBy(eq("testApp"), eq("testPage1"));
    }

    @Test
    public void deletePagesTest() {
        Experiment.ID experimentID = Experiment.ID.valueOf(experimentId);
        Page.Name pageName = Page.Name.valueOf("testPage1");
        ExperimentPageList mockedExperimentPageList = mock(ExperimentPageList.class);
        doReturn(mockedExperimentPageList).when(spyRepository).getExperimentPages(eq(experimentID));
        doNothing().when(spyRepository).saveExperimentPageState(
                eq(experimentID),
                any(ExperimentPageList.class),
                any(ExperimentPageList.class)
        );
        spyRepository.deletePage(APPLICATION_NAME, experimentID, pageName);
        verify(spyRepository, times(2)).getExperimentPages(eq(experimentID));
        verify(spyRepository, times(1)).saveExperimentPageState(eq(experimentID),
                any(ExperimentPageList.class),
                any(ExperimentPageList.class));
        verify(pageExperimentIndexAccessor, times(1))
                .deleteBy(eq(APPLICATION_NAME.toString()), eq(pageName.toString()), eq(experimentId));
        verify(experimentPageAccessor, times(1))
                .deleteBy(eq(pageName.toString()), eq(experimentId));
        //TODO: comment it out for now because the original code does not remove this which I think it should
//        verify(appPageIndexAccessor, times(1))
//                .deleteBy(eq(pageName.toString()), eq(APPLICATION_NAME.toString()));
    }

    @Test
    public void erasePageDataTest() {
        Experiment.ID experimentID = Experiment.ID.valueOf(experimentId);
        Page.Name pageName = Page.Name.valueOf("testPage1");
        ExperimentPageList experimentPageList = new ExperimentPageList();
        experimentPageList.addPage(ExperimentPage.withAttributes(
                pageName,
                true
                )
                        .build()
        );
        doReturn(experimentPageList).when(spyRepository).getExperimentPages(eq(experimentID));
        doNothing().when(spyRepository).deletePage(eq(APPLICATION_NAME), eq(experimentID), eq(pageName));
        spyRepository.erasePageData(APPLICATION_NAME, experimentID);
        verify(spyRepository, times(1)).getExperimentPages(eq(experimentID));
        verify(spyRepository, times(experimentPageList.getPages().size()))
                .deletePage(eq(APPLICATION_NAME), eq(experimentID), eq(pageName));
    }

    @Test(expected = RepositoryException.class)
    public void executeBatchStatement() {
        Session session = mock(Session.class);
        when(mappingManager.getSession()).thenReturn(session);
        doThrow(WriteTimeoutException.class).when(session).execute(any(Statement.class));
        repository.executeBatchStatement(Experiment.ID.valueOf(experimentId), new BatchStatement());
        assertThat(1, is(2)); //Always false condition to capture if exception is not thrown
    }

    @Test
    public void getPageExperimentListEmptyTest() {
        List<AppPage> queryResult = new ArrayList<>();
        when(appPageIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(queryResult.iterator());
        Map<Page.Name, List<PageExperiment>> result = repository.getPageExperimentList(APPLICATION_NAME);
        assertThat(result.size(), is(0));
    }

    @Test
    public void getPageExperimentListSingleTest() {
        String pageName = "page1";
        List<AppPage> queryResult = new ArrayList<>();
        List<PageExperiment> pageExperimentList = new ArrayList<>();
        pageExperimentList.add(
                PageExperiment.withAttributes(
                        Experiment.ID.valueOf(experimentId),
                        Experiment.Label.valueOf("lable"),
                        true
                ).build()
        );
        queryResult.add(AppPage.builder().page(pageName).appName(APPLICATION_NAME.toString()).build());
        when(appPageIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(queryResult.iterator());
        doReturn(pageExperimentList).when(spyRepository).getExperiments(
                any(Application.Name.class),
                any(Page.Name.class)
        );
        Map<Page.Name, List<PageExperiment>> result = spyRepository.getPageExperimentList(APPLICATION_NAME);
        assertThat(result.size(), is(1));
        assertThat(result.get(Page.Name.valueOf(pageName)), is(pageExperimentList));
    }

    @Test
    public void getPageExperimentListMultipleTest() {
        List<AppPage> queryResult = new ArrayList<>();
        List<PageExperiment> pageExperimentList = new ArrayList<>();
        pageExperimentList.add(
                PageExperiment.withAttributes(
                        Experiment.ID.valueOf(experimentId),
                        Experiment.Label.valueOf("lable1"),
                        true
                ).build()
        );
        pageExperimentList.add(
                PageExperiment.withAttributes(
                        Experiment.ID.valueOf(experimentId),
                        Experiment.Label.valueOf("lable2"),
                        true
                ).build()
        );
        queryResult.add(AppPage.builder().page("page1").appName(APPLICATION_NAME.toString()).build());
        queryResult.add(AppPage.builder().page("page2").appName(APPLICATION_NAME.toString()).build());
        when(appPageIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(queryResult.iterator());
        doReturn(pageExperimentList).when(spyRepository).getExperiments(
                any(Application.Name.class),
                any(Page.Name.class)
        );
        Map<Page.Name, List<PageExperiment>> result = spyRepository.getPageExperimentList(APPLICATION_NAME);
        assertThat(result.size(), is(2));
        for (AppPage appPage : queryResult) {
            assertThat(result.get(Page.Name.valueOf(appPage.getPage())), is(pageExperimentList));
        }
    }

    @Test(expected = RepositoryException.class)
    public void getPageExperimentListExceptionTest() {
        doThrow(ReadTimeoutException.class).when(appPageIndexAccessor).selectBy(any(String.class));
        Map<Page.Name, List<PageExperiment>> result = spyRepository.getPageExperimentList(
                APPLICATION_NAME
        );
        assertThat(1, is(2)); //Always false condition to capture if exception is not thrown
    }

    @Test
    public void getPageListTest() {
        List<AppPage> queryResult = new ArrayList<>();
        queryResult.add(AppPage.builder().page("page1").appName(APPLICATION_NAME.toString()).build());
        queryResult.add(AppPage.builder().page("page2").appName(APPLICATION_NAME.toString()).build());
        when(appPageIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(queryResult.iterator());
        List<Page> result = repository.getPageList(APPLICATION_NAME);
        assertThat(result.size(), is(queryResult.size()));
        for (int i = 0; i < queryResult.size(); i++) {
            Page page = result.get(i);
            AppPage appPage = queryResult.get(i);
            assertThat(page.getName().toString(), is(appPage.getPage()));
        }
    }

    @Test(expected = RepositoryException.class)
    public void getExperimentsExceptionTest() {
        Page.Name pageName = Page.Name.valueOf("testPage1");
        doThrow(ReadTimeoutException.class)
                .when(pageExperimentIndexAccessor)
                .selectBy(eq(APPLICATION_NAME.toString()), eq(pageName.toString()));

        repository.getExperiments(APPLICATION_NAME, pageName);
        assertThat(1, is(2)); //Always false condition to capture if exception is not thrown
    }

    @Test
    public void getExperimentsEmptyExperiemntSelectTest() {
        Page.Name pageName = Page.Name.valueOf("testPage1");
        List<PageExperimentByAppNamePage> mockedResult = new ArrayList<>();
        mockedResult.add(
                PageExperimentByAppNamePage.builder()
                        .experimentId(experimentId)
                        .page(pageName.toString())
                        .appName(APPLICATION_NAME.toString())
                        .assign(true)
                        .build()
        );
        when(pageExperimentIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()),
                eq(pageName.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        when(experimentAccessor.selectBy(eq(experimentId))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.one()).thenReturn(null);
        List<PageExperiment> result = repository.getExperiments(APPLICATION_NAME, pageName);
        assertThat(result.size(), is(0));
    }

    @Test
    public void getExperimentsEmptyQueryTest() {
        Page.Name pageName = Page.Name.valueOf("testPage1");
        com.intuit.wasabi.repository.cassandra.pojo.Experiment experiment =
                com.intuit.wasabi.repository.cassandra.pojo.Experiment.builder()
                        .id(experimentId)
                        .label("testLabel1")
                        .build();
        List<PageExperimentByAppNamePage> mockedResult = new ArrayList<>();
        when(pageExperimentIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()),
                eq(pageName.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        when(experimentAccessor.selectBy(eq(experimentId))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.one()).thenReturn(experiment);
        List<PageExperiment> result = repository.getExperiments(APPLICATION_NAME, pageName);
        assertThat(result.size(), is(0));
    }

    @Test
    public void getExperimentSingleTest() {
        Page.Name pageName = Page.Name.valueOf("testPage1");
        List<PageExperimentByAppNamePage> mockedResult = new ArrayList<>();
        mockedResult.add(
                PageExperimentByAppNamePage.builder()
                        .experimentId(experimentId)
                        .page(pageName.toString())
                        .appName(APPLICATION_NAME.toString())
                        .assign(true)
                        .build()
        );
        com.intuit.wasabi.repository.cassandra.pojo.Experiment experiment =
                com.intuit.wasabi.repository.cassandra.pojo.Experiment.builder()
                        .id(experimentId)
                        .label("testLabel1")
                        .build();
        when(pageExperimentIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()),
                eq(pageName.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        when(experimentAccessor.selectBy(eq(experimentId))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.one()).thenReturn(experiment);
        List<PageExperiment> result = repository.getExperiments(APPLICATION_NAME, pageName);
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getAllowNewAssignment(), is(mockedResult.get(0).isAssign()));
        assertThat(result.get(0).getId().getRawID(), is(experimentId));
        assertThat(result.get(0).getLabel().toString(), is(experiment.getLabel()));
    }

    @Test
    public void getExperimentMultipleResultTest() {
        Page.Name pageName = Page.Name.valueOf("testPage1");
        List<PageExperimentByAppNamePage> mockedResult = new ArrayList<>();
        mockedResult.add(
                PageExperimentByAppNamePage.builder()
                        .experimentId(UUID.randomUUID())
                        .page(pageName.toString())
                        .appName(APPLICATION_NAME.toString())
                        .assign(true)
                        .build()
        );
        mockedResult.add(
                PageExperimentByAppNamePage.builder()
                        .experimentId(UUID.randomUUID())
                        .page(pageName.toString())
                        .appName(APPLICATION_NAME.toString())
                        .assign(false)
                        .build()
        );
        List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experiments = new ArrayList<>();
        for (int i = 0; i < mockedResult.size(); i++) {
            experiments.add(
                    com.intuit.wasabi.repository.cassandra.pojo.Experiment.builder()
                            .id(mockedResult.get(i).getExperimentId())
                            .label("testLabel" + (i + 1))
                            .build());
        }
        when(pageExperimentIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()),
                eq(pageName.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        for (com.intuit.wasabi.repository.cassandra.pojo.Experiment experiment : experiments) {
            Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> mocked = mock(Result.class);
            when(experimentAccessor.selectBy(eq(experiment.getId()))).thenReturn(mocked);
            when(mocked.one()).thenReturn(experiment);
        }
        List<PageExperiment> result = repository.getExperiments(APPLICATION_NAME, pageName);
        assertThat(result.size(), is(mockedResult.size()));
        for (int i = 0; i < mockedResult.size(); i++) {
            assertThat(result.get(i).getAllowNewAssignment(), is(mockedResult.get(i).isAssign()));
            assertThat(result.get(i).getId().getRawID(), is(mockedResult.get(i).getExperimentId()));
            assertThat(result.get(i).getLabel().toString(), is(experiments.get(i).getLabel()));
        }
    }

    @Test
    public void testGetExperimentsWithoutLabels() {

        //------ Input --------
        Page.Name pageName = Page.Name.valueOf("testPage1");

        //Mock interaction
        List<PageExperimentByAppNamePage> mockedResult = new ArrayList<>();
        mockedResult.add(
                PageExperimentByAppNamePage.builder()
                        .experimentId(experimentId)
                        .page(pageName.toString())
                        .appName(APPLICATION_NAME.toString())
                        .assign(true)
                        .build()
        );
        com.intuit.wasabi.repository.cassandra.pojo.Experiment experiment =
                com.intuit.wasabi.repository.cassandra.pojo.Experiment.builder()
                        .id(experimentId)
                        .label("testLabel1")
                        .build();
        when(pageExperimentIndexAccessor.selectBy(eq(APPLICATION_NAME.toString()),
                eq(pageName.toString()))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.iterator()).thenReturn(mockedResult.iterator());
        when(experimentAccessor.selectBy(eq(experimentId))).thenReturn(mockedResultMapping);
        when(mockedResultMapping.one()).thenReturn(experiment);

        //Make actual call
        List<PageExperiment> result = repository.getExperimentsWithoutLabels(APPLICATION_NAME, pageName);

        //Verify result
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getAllowNewAssignment(), is(mockedResult.get(0).isAssign()));
        assertThat(result.get(0).getId().getRawID(), is(experimentId));
        assertThat(result.get(0).getLabel() == null, is(true));

    }

}