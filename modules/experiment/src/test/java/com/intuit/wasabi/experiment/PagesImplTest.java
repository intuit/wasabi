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
package com.intuit.wasabi.experiment;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.exceptions.ApplicationNotFoundException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experiment.impl.PagesImpl;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentPage;
import com.intuit.wasabi.experimentobjects.ExperimentPageList;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidExperimentStateException;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.PagesRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PagesImplTest {

    private final static Application.Name testApp = Application.Name.valueOf("testApp");
    private final static Page page = new Page.Builder().withName(Page.Name.valueOf("p1")).build();
    @Mock
    private ExperimentRepository cassandraRepository;
    @Mock
    private Experiments experiments;
    @Mock
    private PagesRepository pagesRepository;
    @Mock
    private EventLog eventLog;
    private Experiment.ID experimentID;

    @Before
    public void setup() {
        experimentID = Experiment.ID.newInstance();
    }

    @Test
    public void testPostPages() {
        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);

        //Create Experiment and PageList for App
        Experiment experiment = Experiment.withID(experimentID).withApplicationName(testApp).withLabel(Experiment.Label.valueOf("ExperimentLabel")).build();
        ExperimentPageList pageList = new ExperimentPageList();
        ExperimentPage expPage = ExperimentPage.withAttributes(Page.Name.valueOf("PageName"), true).build();
        pageList.addPage(expPage);

        when(experiments.getExperiment(experimentID)).thenReturn(null);
        try {
            pagesImpl.postPages(experimentID, pageList, null);
        } catch (ExperimentNotFoundException e) {
            // Expecting this so nothing to do
        }

        experiment.setState(Experiment.State.TERMINATED);
        when(experiments.getExperiment(experimentID)).thenReturn(experiment);
        try {
            pagesImpl.postPages(experimentID, pageList, null);
        } catch (InvalidExperimentStateException e) {
            // Expecting this so nothing to do
        }

        experiment.setState(Experiment.State.RUNNING);
        experiment.setEndTime(new Timestamp(System.currentTimeMillis() - 1000000));
        try {
            pagesImpl.postPages(experimentID, pageList, null);
        } catch (IllegalArgumentException e) {
            // Expecting this so nothing to do
        }

        experiment.setEndTime(new Timestamp(System.currentTimeMillis() + 1000000));
        doNothing().when(pagesRepository).postPages(testApp, experimentID, pageList);
        pagesImpl.postPages(experimentID, pageList, null);
    }

    @Test
    public void testDeletePage() {
        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);

        //Create Experiment and PageList for App
        Experiment experiment = Experiment.withID(experimentID).withApplicationName(testApp).build();
        Page page = new Page.Builder().withName(Page.Name.valueOf("somePage")).build();
        when(experiments.getExperiment(experimentID)).thenReturn(null);

        try {
            pagesImpl.deletePage(experimentID, page.getName(), null);
        } catch (ExperimentNotFoundException e) {
            // Expecting this so nothing to do
        }

        experiment.setState(Experiment.State.TERMINATED);
        when(experiments.getExperiment(experimentID)).thenReturn(experiment);
        try {
            pagesImpl.deletePage(experimentID, page.getName(), null);
        } catch (InvalidExperimentStateException e) {
            // Expecting this so nothing to do
        }

        experiment.setState(Experiment.State.RUNNING);
        experiment.setEndTime(new Timestamp(System.currentTimeMillis() - 1000000));
        try {
            pagesImpl.deletePage(experimentID, page.getName(), null);
        } catch (IllegalArgumentException e) {
            // Expecting this so nothing to do
        }

        experiment.setEndTime(new Timestamp(System.currentTimeMillis() + 1000000));
        doNothing().when(pagesRepository).deletePage(testApp, experimentID, page.getName());

        UserInfo user = UserInfo.from(UserInfo.Username.valueOf("user")).build();
        pagesImpl.deletePage(experimentID, page.getName(), user);
    }

    @Test
    public void testGetExperimentPages() {
        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);

        //Create Experiment and PageList for App
        Experiment experiment = Experiment.withID(experimentID).withApplicationName(testApp).build();
        ExperimentPageList pageList = new ExperimentPageList();
        pageList.addPage(new ExperimentPage());

        when(experiments.getExperiment(experimentID)).thenReturn(null);
        try {
            pagesImpl.getExperimentPages(experimentID);
        } catch (ExperimentNotFoundException e) {
            // Expecting this so nothing to do
        }

        when(experiments.getExperiment(experimentID)).thenReturn(experiment);
        when(pagesRepository.getExperimentPages(experimentID)).thenReturn(pageList);
        ExperimentPageList result = pagesImpl.getExperimentPages(experimentID);
        assert result.equals(pageList);
    }

    @Test
    public void testGetPageExperiments() throws Exception {

        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);

        //Create Experiments for App
        Experiment experiment = Experiment.withID(experimentID).withApplicationName(testApp).build();
        Experiment experiment1 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp).build();

        when(cassandraRepository.getExperiment(experiment.getID())).thenReturn(experiment);
        when(cassandraRepository.getExperiment(experiment1.getID())).thenReturn(experiment1);

        //Put Pages for first experiment, app
        ExperimentList experimentList = new ExperimentList();
        experimentList.addExperiment(experiment);
        String pageName1 = "testPage";
        ExperimentPage expPage1 = ExperimentPage.withAttributes(Page.Name.valueOf(pageName1), false).build();
        Page.Name page1 = expPage1.getName();
        when(pagesImpl.getPageExperiments(testApp, page1)).thenReturn(experimentList);
        assertTrue(experimentList.getExperiments().size() == 1);

        //Confirm null pageName and null app, will return no experiments.
        experimentList = pagesImpl.getPageExperiments(null, null);
        assertTrue(experimentList.getExperiments().size() == 0);

        //Confirm null pageName for proper app, will return no experiments.
        experimentList = pagesImpl.getPageExperiments(testApp, null);
        assertTrue(experimentList.getExperiments().size() == 0);
    }

    @Test
    public void testGetPageExperimentsWithPages() throws Exception {

        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);

        //Create Experiments for App
        Experiment experiment = Experiment.withID(experimentID).withApplicationName(testApp).build();

        //Put Pages for first experiment, app
        ExperimentList experimentList = new ExperimentList();
        experimentList.addExperiment(experiment);

        List<PageExperiment> pageExperiments = new ArrayList<>();
        PageExperiment pe = new PageExperiment();
        pe.setId(ID.newInstance());
        pageExperiments.add(pe);

        Collection<Experiment.ID> ids = new ArrayList<>();
        ids.add(experiment.getID());

        when(pagesRepository.getExperiments(any(Application.Name.class), any(Page.Name.class))).thenReturn(pageExperiments);
        when(cassandraRepository.getExperiments(anyCollection())).thenReturn(experimentList);

        ExperimentList experimentListResult = pagesImpl.getPageExperiments(testApp, page.getName());

        assertEquals(1, experimentListResult.getExperiments().size());
    }

    @Test
    public void testErasePageDataExperimentNull() {
        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);
        UserInfo user = UserInfo.from(UserInfo.Username.valueOf("user")).build();
        pagesImpl.erasePageData(testApp, experimentID, user);
    }

    @Test
    public void testErasePageDataSuccessful() {
        Experiment experiment = Experiment.withID(experimentID).withApplicationName(testApp).build();
        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);
        given(experiments.getExperiment(experimentID)).willReturn(experiment);
        UserInfo user = UserInfo.from(UserInfo.Username.valueOf("user")).build();
        willDoNothing().given(eventLog).postEvent(any(ExperimentChangeEvent.class));
        pagesImpl.erasePageData(testApp, experimentID, user);

        verify(experiments).getExperiment(experimentID);
        verify(eventLog).postEvent(any(ExperimentChangeEvent.class));

    }

    @Test
    public void testGetPageList() {

        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);

        Page page = new Page.Builder().withName(Page.Name.valueOf("somePage")).build();
        List<Page> pageList = new ArrayList<>(1);
        pageList.add(page);
        when(pagesRepository.getPageList(testApp)).thenReturn(pageList);
        List<Page> result = pagesImpl.getPageList(testApp);
        assert result == pageList;
        verifyException(pagesImpl, ApplicationNotFoundException.class).getPageList(null);
    }

    @Test
    public void testGetExperiments() {

        PagesImpl pagesImpl = new PagesImpl(cassandraRepository, pagesRepository, experiments, eventLog);

        Experiment.ID expID = Experiment.ID.newInstance();
        Experiment.Label label = Experiment.Label.valueOf("expLabel");
        Page.Name pageName = Page.Name.valueOf("somePage");

        PageExperiment pageExperiment = PageExperiment.withAttributes(expID, label, true).build();
        List<PageExperiment> pageExperimentList = new ArrayList<>();
        pageExperimentList.add(pageExperiment);

        when(pagesRepository.getExperiments(testApp, pageName)).thenReturn(pageExperimentList);
        List<PageExperiment> result = pagesImpl.getExperiments(testApp, pageName);
        assert result == pageExperimentList;
        verifyException(pagesImpl, ApplicationNotFoundException.class).getExperiments(null, pageName);
    }

}
