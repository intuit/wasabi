/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.assignment.impl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Provider;
import com.intuit.hyrule.Rule;
import com.intuit.wasabi.assignment.AssignmentDecorator;
import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.assignmentobjects.*;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experiment.Mutex;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.*;
import com.intuit.wasabi.export.DatabaseExport;
import com.intuit.wasabi.export.Envelope;
import com.intuit.wasabi.export.WebExport;
import com.intuit.wasabi.export.rest.Driver;
import com.intuit.wasabi.repository.AnalyticsRepository;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.cassandra.impl.ExperimentRuleCacheUpdateEnvelope;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.RETURNS_DEEP_STUBS;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentsImplTest {

    final static Application.Name testApp = Application.Name.valueOf("testApp");
    final static Context context = Context.valueOf("PROD");
    AssignmentsImpl cassandraAssignments = mock(AssignmentsImpl.class);
    private ExperimentRepository cassandraRepository = mock(ExperimentRepository.class);
    private ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
    private AnalyticsRepository analyticsRepository = mock(AnalyticsRepository.class);
    private MutexRepository mutexRepository = mock(MutexRepository.class);
    private Mutex mutex = mock(Mutex.class);
    private Pages pages = mock(Pages.class);
    private Priorities priorities = mock(Priorities.class);
    private CassandraDriver cassandraDriver = mock(CassandraDriver.class);
    //private ExperimentsKeyspace keyspace = mock(ExperimentsKeyspace.class);
    private RuleCache ruleCache = mock(RuleCache.class);
    private Rule rule = mock(Rule.class);
    private Assignments assignments = mock(Assignments.class);
    private Driver restDriver = mock(Driver.class);
    private EventLog eventLog = mock(EventLog.class);
    private AssignmentDecorator assignmentDecorator = mock(AssignmentDecorator.class);
    private ThreadPoolExecutor threadPoolExecutor = mock(ThreadPoolExecutor.class, RETURNS_DEEP_STUBS);
    private Provider<Envelope<AssignmentEnvelopePayload, DatabaseExport>> assignmentDBEnvelopeProvider =
            mock(Provider.class, RETURNS_DEEP_STUBS);
    private Provider<Envelope<AssignmentEnvelopePayload, WebExport>> assignmentWebEnvelopeProvider =
            mock(Provider.class, RETURNS_DEEP_STUBS);
    private AssignmentsRepository assignmentsRepository = mock(AssignmentsRepository.class, RETURNS_DEEP_STUBS);
    private AssignmentsImpl assignmentsImpl;

    @Before
    public void setup() throws IOException, ConnectionException {
        this.assignmentsImpl = new AssignmentsImpl(new HashMap<String, AssignmentIngestionExecutor>(),
                experimentRepository, assignmentsRepository, mutexRepository,
                ruleCache, pages, priorities, 
                assignmentDecorator, threadPoolExecutor, eventLog);
    }

    @Test
    public void testQueueLength() {
        when(threadPoolExecutor.getQueue().size()).thenReturn(0);
        Map<String, Integer> queueLengthMap = new HashMap<String, Integer>();
        queueLengthMap.put(AssignmentsImpl.RULE_CACHE, new Integer(0));
        assertThat(assignmentsImpl.queuesLength(), is(queueLengthMap));
    }

    @Test
    public void testGetSingleAssignmentNullAssignmentExperimentNotFound() {
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        Assignment nullAssignment = Assignment.newInstance(null)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withUserID(user)
                .withContext(null)
                .withStatus(Assignment.Status.EXPERIMENT_NOT_FOUND)
                .build();
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(null);
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        Page.Name pageName = Page.Name.valueOf("p1");
        Assignment result = assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true,
                segmentationProfile, headers, pageName);
        assertThat(result.equals(nullAssignment), is(true));
    }

    @Test
    public void testGetSingleAssignmentNullAssignmentExperimentInDraftState() throws IOException, ConnectionException {
        AssignmentsImpl assignmentsImpl = spy(new AssignmentsImpl(new HashMap<String, AssignmentIngestionExecutor>(),
                experimentRepository, assignmentsRepository,
                mutexRepository, ruleCache, pages, priorities, assignmentDecorator, threadPoolExecutor,
                eventLog));
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class);
        when(experiment.getID()).thenReturn(id);
        when(experiment.getState()).thenReturn(Experiment.State.DRAFT);
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        Assignment nullAssignment = Assignment.newInstance(id)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withUserID(user)
                .withContext(null)
                .withStatus(Assignment.Status.EXPERIMENT_IN_DRAFT_STATE)
                .build();
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(experiment);
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        Page.Name pageName = Page.Name.valueOf("p1");
        Assignment result = assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true,
                segmentationProfile, headers, pageName);
        assertThat(result.equals(nullAssignment), is(true));
    }

    @Test
    public void testGetSingleAssignmentNullAssignmentExperimentNotStarted() {
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class, RETURNS_DEEP_STUBS);
        when(experiment.getID()).thenReturn(id);
        when(experiment.getStartTime().getTime()).thenReturn(new Date().getTime() + 1000000L);
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        Assignment nullAssignment = Assignment.newInstance(id)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withUserID(user)
                .withContext(null)
                .withStatus(Assignment.Status.EXPERIMENT_NOT_STARTED)
                .build();
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(experiment);
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        Page.Name pageName = Page.Name.valueOf("p1");
        Assignment result = assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true,
                segmentationProfile, headers, pageName);
        assertThat(result.equals(nullAssignment), is(true));
    }

    @Test
    public void testGetSingleAssignmentNullAssignmentExperimentExpired() {
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class, RETURNS_DEEP_STUBS);
        when(experiment.getID()).thenReturn(id);
        when(experiment.getEndTime().getTime()).thenReturn(1000000L);
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        Assignment nullAssignment = Assignment.newInstance(id)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withUserID(user)
                .withContext(null)
                .withStatus(Assignment.Status.EXPERIMENT_EXPIRED)
                .build();
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(experiment);
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        Page.Name pageName = Page.Name.valueOf("p1");
        Assignment result = assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true,
                segmentationProfile, headers, pageName);
        assertThat(result.equals(nullAssignment), is(true));
    }

    @Test
    public void testGetSingleAssignmentNullAssignmentExperimentPaused() {
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class, RETURNS_DEEP_STUBS);
        when(experiment.getID()).thenReturn(id);
        when(experiment.getState()).thenReturn(Experiment.State.PAUSED);
        when(experiment.getEndTime().getTime()).thenReturn(new Date().getTime() + 1000000L);
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        Assignment nullAssignment = Assignment.newInstance(id)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withUserID(user)
                .withContext(null)
                .withStatus(Assignment.Status.EXPERIMENT_PAUSED)
                .build();
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(experiment);
        when(assignmentsRepository.getAssignment(eq(id), eq(user), any(Context.class))).thenReturn(null);
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        Page.Name pageName = Page.Name.valueOf("p1");
        Assignment result = assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true,
                segmentationProfile, headers, pageName);
        assertThat(result.equals(nullAssignment), is(true));
    }

    @Test
    public void testGetSingleAssignmentNullAssignmentExperimentNoProfileMatch() throws IOException, ConnectionException {
        AssignmentsImpl assignmentsImpl = spy(new AssignmentsImpl(new HashMap<String, AssignmentIngestionExecutor>(),
                experimentRepository, assignmentsRepository,
                mutexRepository, ruleCache, pages, priorities, assignmentDecorator, threadPoolExecutor, eventLog));
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class, RETURNS_DEEP_STUBS);
        when(experiment.getID()).thenReturn(id);
        when(experiment.getState()).thenReturn(Experiment.State.RUNNING);
        when(experiment.getEndTime().getTime()).thenReturn(new Date().getTime() + 1000000L);
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        Assignment nullAssignment = Assignment.newInstance(id)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withUserID(user)
                .withContext(null)
                .withStatus(Assignment.Status.NO_PROFILE_MATCH)
                .build();
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(experiment);
        when(assignmentsRepository.getAssignment(eq(id), eq(user), any(Context.class))).thenReturn(null);
        doReturn(false).when(assignmentsImpl).doesProfileMatch(any(Experiment.class), any(SegmentationProfile.class),
                any(HttpHeaders.class), any(Context.class));
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        Page.Name pageName = Page.Name.valueOf("p1");
        Assignment result = assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true,
                segmentationProfile, headers, pageName);
        assertThat(result.equals(nullAssignment), is(true));
        verify(threadPoolExecutor, times(1)).execute(any(ExperimentRuleCacheUpdateEnvelope.class));
    }

    @Test(expected = AssertionError.class)
    public void testGetSingleAssignmentAssertExistingAssignment() {
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class, RETURNS_DEEP_STUBS);
        when(experiment.getID()).thenReturn(id);
        when(experiment.getState()).thenReturn(Experiment.State.RUNNING);
        when(experiment.getEndTime().getTime()).thenReturn(new Date().getTime() + 1000000L);
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(experiment);
        Assignment assignment = mock(Assignment.class);
        when(assignmentsRepository.getAssignment(eq(id), eq(user), any(Context.class))).thenReturn(assignment);
        when(assignment.getStatus()).thenReturn(Assignment.Status.NEW_ASSIGNMENT);
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        Page.Name pageName = Page.Name.valueOf("p1");
        verify(threadPoolExecutor, times(0)).execute(any(Runnable.class));
        assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true, segmentationProfile, headers, pageName);
        verify(threadPoolExecutor, times(0)).execute(any(Runnable.class));
    }

    @Test(expected = AssertionError.class)
    public void testGetSingleAssignmentProfileMatchAssertNewAssignment() throws IOException, ConnectionException {
        AssignmentsImpl assignmentsImpl = spy(new AssignmentsImpl(new HashMap<String, AssignmentIngestionExecutor>(),
                experimentRepository, assignmentsRepository,
                mutexRepository, ruleCache, pages, priorities, assignmentDecorator, threadPoolExecutor, eventLog));
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class, RETURNS_DEEP_STUBS);
        Assignment assignment = mock(Assignment.class);
        when(experiment.getID()).thenReturn(id);
        when(experiment.getState()).thenReturn(Experiment.State.RUNNING);
        when(experiment.getSamplingPercent()).thenReturn(0.5);
        when(experiment.getEndTime().getTime()).thenReturn(new Date().getTime() + 1000000L);
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(experiment);
        when(assignmentsRepository.getAssignment(eq(id), eq(user), any(Context.class))).thenReturn(null);
        doReturn(true).when(assignmentsImpl).doesProfileMatch(any(Experiment.class), any(SegmentationProfile.class),
                any(HttpHeaders.class), any(Context.class));
        doReturn(assignment).when(assignmentsImpl).generateAssignment(any(Experiment.class), eq(user),
                any(Context.class), any(Boolean.class), any(BucketList.class), any(Date.class), any(SegmentationProfile.class));
        doReturn(true).when(assignmentsImpl).checkMutex(any(Experiment.class), eq(user), any(Context.class));
        HttpHeaders headers = mock(HttpHeaders.class);
        when(assignment.getStatus()).thenReturn(Assignment.Status.EXPERIMENT_NOT_FOUND);
        Page.Name pageName = Page.Name.valueOf("p1");
        verify(threadPoolExecutor, times(0)).execute(any(Runnable.class));
        assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true, null, headers, pageName);
        verify(threadPoolExecutor, times(0)).execute(any(Runnable.class));
    }

    @Test
    public void testGetSingleAssignmentSuccess() throws IOException, ConnectionException {
        AssignmentsImpl assignmentsImpl = spy(new AssignmentsImpl(new HashMap<String, AssignmentIngestionExecutor>(),
                experimentRepository, assignmentsRepository,
                mutexRepository, ruleCache, pages, priorities, assignmentDecorator, threadPoolExecutor, eventLog));
        Experiment.ID id = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class, RETURNS_DEEP_STUBS);
        Assignment assignment = mock(Assignment.class);
        when(experiment.getID()).thenReturn(id);
        when(experiment.getState()).thenReturn(Experiment.State.RUNNING);
        when(experiment.getSamplingPercent()).thenReturn(0.5);
        when(experiment.getEndTime().getTime()).thenReturn(new Date().getTime() + 1000000L);
        Application.Name appName = Application.Name.valueOf("Test");
        Experiment.Label label = Experiment.Label.valueOf("label");
        User.ID user = User.ID.valueOf("testUser");
        Page.Name pageName = Page.Name.valueOf("p1");
        when(experimentRepository.getExperiment(eq(appName), eq(label))).thenReturn(experiment);
        when(assignmentsRepository.getAssignment(eq(id), eq(user), any(Context.class))).thenReturn(assignment);
        when(assignment.getStatus()).thenReturn(Assignment.Status.EXISTING_ASSIGNMENT);
        Assignment result = assignmentsImpl.getSingleAssignment(user, appName, label, context, true, true,
                null, null, pageName);
        verify(threadPoolExecutor, times(1)).execute(any(ExperimentRuleCacheUpdateEnvelope.class));
        assertThat(result, is(assignment));
    }


    @Test
    public void testGetAssignmentNullAssignment() throws IOException, ConnectionException {
        Application.Name appName = Application.Name.valueOf("testApp");
        User.ID userID = User.ID.valueOf("test");
        Experiment.Label label = Experiment.Label.valueOf("test");
        Table table = mock(Table.class, RETURNS_DEEP_STUBS);
        Collection collection = mock(Collection.class);
        when(table.column(eq(label)).values()).thenReturn(collection);
        when(collection.isEmpty()).thenReturn(true);
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        when(experimentRepository.getExperimentList(eq(appName))).thenReturn(table);
        Assignment nullAssignment = Assignment.newInstance(null)
                .withApplicationName(appName)
                .withBucketLabel(null)
                .withUserID(userID)
                .withContext(null)
                .withStatus(Assignment.Status.EXPERIMENT_NOT_FOUND)
                .build();
        Assignment result = this.assignmentsImpl.getAssignment(userID, appName, label, null, true, true,
                segmentationProfile, headers);
        assertThat(result, is(nullAssignment));
    }

    @Test
    public void testGetAssignment() throws IOException, ConnectionException {
        Application.Name appName = Application.Name.valueOf("testApp");
        User.ID userID = User.ID.valueOf("test");
        Experiment.Label label = Experiment.Label.valueOf("test");
        Table table = mock(Table.class, RETURNS_DEEP_STUBS);
        Experiment experiment = mock(Experiment.class);
        SegmentationProfile segmentationProfile = mock(SegmentationProfile.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        when(experimentRepository.getExperimentList(eq(appName))).thenReturn(table);
        Assignment assignment = mock(Assignment.class);
        AssignmentsImpl assignmentsImpl = spy(new AssignmentsImpl(new HashMap<String, AssignmentIngestionExecutor>(),
                experimentRepository, assignmentsRepository,
                mutexRepository, ruleCache, pages, priorities, assignmentDecorator, threadPoolExecutor, eventLog));

        doReturn(assignment).when(assignmentsImpl).getAssignment(eq(userID), eq(appName), eq(label),
                eq(context), any(boolean.class), any(boolean.class), eq(segmentationProfile),
                eq(headers), any(Page.Name.class), any(Experiment.class), any(BucketList.class),
                any(Table.class), any(Map.class));

        doReturn(experiment).when(assignmentsImpl).getExperimentFromTable(any(Table.class), any(Experiment.Label.class));

        Assignment result = assignmentsImpl.getAssignment(userID, appName, label, context, true, true,
                segmentationProfile, headers);
        assertThat(result, is(assignment));
    }

    // FIXME:
//    @Ignore("FIXME:refactor-core")
//    @Test
//    public void checkContextInSegmentation() throws IOException, ConnectionException {
//
//        //Create a segmentation profile
//        SegmentationProfile segmentationProfile = SegmentationProfile.newInstance().build();
//
//        //Check to show that there is no context parameter within the segmentation profile
//        assertFalse(segmentationProfile.hasAttribute("context"));
//
//        //Creating objects to help mockito
//        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments = HashBasedTable.create();
//        Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
//
//        //Creating a calendar object that will help setup the Experiment Object later.
//        Calendar c = Calendar.getInstance();
//        c.setTime(new Date());
//
//        //Creating a new Experiment Object
//        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp).build();
//        experiment.setLabel(Experiment.Label.valueOf("testExp"));
//        c.add(Calendar.DATE, -10);
//        experiment.setStartTime(c.getTime());
//        c.add(Calendar.DATE, 15);
//        experiment.setEndTime(c.getTime());
//        experiment.setState(Experiment.State.RUNNING);
//        experiment.setSamplingPercent(1.0);
//        experiment.setRule("TestRule");
//
//        //Put the experiment in allExperiments
//        allExperiments.put(experiment.getID(), experiment.getLabel(), experiment);
//
//        //Creating a new bucket within the experiment
//        Bucket redBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("red")).withAllocationPercent(1.0).build();
//        BucketList bucketList = new BucketList(1);
//        bucketList.addBucket(redBucket);
//
//        //Configuring mockito to return some objects that are created above
//        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-a"), testApp, context, allExperiments)).thenReturn(result);
//        Mockito.when(cassandraRepository.getExperimentList(testApp)).thenReturn(allExperiments);
//        Mockito.when(cassandraRepository.getBucketList(experiment.getID())).thenReturn(bucketList);
//        Mockito.when(ruleCache.getRule(experiment.getID())).thenReturn(rule);
//
//        cassandraAssignments = null; //new AssignmentsImpl(cassandraRepository, assignmentsRepository, mutexRepository, random,
////                ruleCache, pages, priorities, assignmentDBEnvelopeProvider, assignmentWebEnvelopeProvider, null, // FIXME
////                decisionEngineScheme, decisionEngineHost, decisionEnginePath,
////                decisionEngineReadTimeOut, decisionEngineConnectionTimeOut, decisionEngineUseProxy,
////                decisionEngineUseConnectionPooling, decisionEngineMaxConnectionsPerHost, proxyPort, proxyHost, eventLog);
//
//        //Pass the segmentation profile to be modified within getAssignment to have context parameter as an additional attribute.
//        cassandraAssignments.getAssignment(User.ID.valueOf("user-a"), testApp, experiment.getLabel(), context, true, false, segmentationProfile, null);
//
//        //Check to show that there is now a context parameter within the segmentation profile
//        assertTrue(segmentationProfile.hasAttribute("context"));
//
//        //Check to see that the context parameter within the segmentation profile has the correct value
//        assertEquals("ERROR: The context parameter passed in did NOT match with the context added to the segmentation profile. ", context.getContext(), segmentationProfile.getAttribute("context"));
//    }

    // FIXME:
//    @Ignore("FIXME:refactor-core")
//    @Test
//    public void getAssignment_test_1() throws IOException, ConnectionException {
//
//        cassandraAssignments = null; //new AssignmentsImpl(cassandraRepository, assignmentsRepository, mutexRepository, random,
////                ruleCache, pages, priorities, assignmentDBEnvelopeProvider, assignmentWebEnvelopeProvider, null, // FIXME
////                decisionEngineScheme, decisionEngineHost, decisionEnginePath,
////                decisionEngineReadTimeOut, decisionEngineConnectionTimeOut, decisionEngineUseProxy,
////                decisionEngineUseConnectionPooling, decisionEngineMaxConnectionsPerHost, proxyPort, proxyHost, eventLog);
//
//
//        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments = HashBasedTable.create();
//        Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
//        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-a"), testApp, context, allExperiments)).thenReturn(result);
//        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp).build();
//        experiment.setLabel(Experiment.Label.valueOf("testExp"));
//        experiment.setState(Experiment.State.DRAFT);
//        allExperiments.put(experiment.getID(), experiment.getLabel(), experiment);
//        Bucket redBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("red")).withAllocationPercent(1.0).build();
//        BucketList bucketList = new BucketList(1);
//        bucketList.addBucket(redBucket);
//        Mockito.when(cassandraRepository.getExperimentList(testApp)).thenReturn(allExperiments);
//        Mockito.when(cassandraRepository.getBucketList(experiment.getID())).thenReturn(bucketList);
//        Assignment assignment = cassandraAssignments.getAssignment(User.ID.valueOf("user-a"), testApp,
//                experiment.getLabel(), context, true, false, null, null);
//
//        assert assignment.getStatus() == Assignment.Status.EXPERIMENT_IN_DRAFT_STATE;
//
//        Calendar c = Calendar.getInstance();
//        c.setTime(new Date());
//        c.add(Calendar.DATE, 10);
//        experiment.setStartTime(c.getTime());
//        c.add(Calendar.DATE, 15);
//        experiment.setEndTime(c.getTime());
//        experiment.setState(Experiment.State.RUNNING);
//        assignment = cassandraAssignments.getAssignment(User.ID.valueOf("user-a"), testApp,
//                experiment.getLabel(), context, true, false, null, null);
//        assert assignment.getStatus() == Assignment.Status.EXPERIMENT_NOT_STARTED;
//
//        c = Calendar.getInstance();
//        c.setTime(new Date());
//        c.add(Calendar.DATE, -15);
//        experiment.setStartTime(c.getTime());
//        c.add(Calendar.DATE, -10);
//        experiment.setEndTime(c.getTime());
//        assignment = cassandraAssignments.getAssignment(User.ID.valueOf("user-a"), testApp,
//                experiment.getLabel(), context, true, false, null, null);
//        assert assignment.getStatus() == Assignment.Status.EXPERIMENT_EXPIRED;
//
//
//        c = Calendar.getInstance();
//        c.setTime(new Date());
//        c.add(Calendar.DATE, -1);
//        experiment.setStartTime(c.getTime());
//        c.add(Calendar.DATE, 10);
//        experiment.setEndTime(c.getTime());
//        experiment.setState(Experiment.State.PAUSED);
//        assignment = cassandraAssignments.getAssignment(User.ID.valueOf("user-a"), testApp,
//                experiment.getLabel(), context, true, false, null, null);
//        assert assignment.getStatus() == Assignment.Status.EXPERIMENT_PAUSED;
//    }

    // FIXME:
//    @Ignore("FIXME:refactor-core")
//    @Test
//    public void getAssignment_test_2() throws IOException, ConnectionException {
//        cassandraAssignments = null; //new AssignmentsImpl(cassandraRepository, assignmentsRepository, mutexRepository, random,
////                ruleCache, pages, priorities, assignmentDBEnvelopeProvider, assignmentWebEnvelopeProvider, null, // FIXME
////                decisionEngineScheme, decisionEngineHost, decisionEnginePath,
////                decisionEngineReadTimeOut, decisionEngineConnectionTimeOut, decisionEngineUseProxy,
////                decisionEngineUseConnectionPooling, decisionEngineMaxConnectionsPerHost, proxyPort, proxyHost, eventLog) {
////
////            @Override
////            protected Experiment getExperimentFromTable(Table<Experiment.ID, Experiment.Label, Experiment> allExperiments,
////                                                        Experiment.Label experimentLabel) {
////                return null;
////            }
////        };
//
//        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp).build();
//        experiment.setLabel(Experiment.Label.valueOf("testExp"));
//        experiment.setState(Experiment.State.DRAFT);
//        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments = HashBasedTable.create();
//        Mockito.when(cassandraRepository.getExperimentList(testApp)).thenReturn(allExperiments);
//        Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
//        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-a"), testApp, context, allExperiments)).thenReturn(result);
//        Assignment assignment = cassandraAssignments.getAssignment(User.ID.valueOf("user-a"), testApp,
//                experiment.getLabel(), context, true, false, null, null);
//        assert assignment.getStatus() == Assignment.Status.EXPERIMENT_NOT_FOUND;
//
//    }

    @Test
    public void getAssignment_test_3() throws IOException, ConnectionException {
        final Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -1);
        final Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp2")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        c.add(Calendar.DATE, 10);
        experiment.setEndTime(c.getTime());
        experiment2.setEndTime(c.getTime());

        final Bucket redBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("red"))
                .withState(Bucket.State.OPEN).withAllocationPercent(1.0).build();
        Bucket yellowBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("yellow"))
                .withState(Bucket.State.OPEN).withAllocationPercent(0.0).build();
        BucketList expBucketList = new BucketList(2);
        expBucketList.addBucket(redBucket);
        expBucketList.addBucket(yellowBucket);

        Bucket greenBucket = Bucket.newInstance(experiment2.getID(), Bucket.Label.valueOf("green")).withAllocationPercent(1.0).build();
        BucketList exp2bucketList = new BucketList(1);
        exp2bucketList.addBucket(greenBucket);

        experiment.setState(Experiment.State.RUNNING);
        experiment2.setState(Experiment.State.RUNNING);

        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments = HashBasedTable.create();
        allExperiments.put(experiment.getID(), experiment.getLabel(), experiment);
        allExperiments.put(experiment2.getID(), experiment2.getLabel(), experiment2);

        List<Experiment.ID> exclusivesList = new ArrayList<>(1);
        exclusivesList.add(experiment2.getID());

        List<Experiment.ID> exclusivesList2 = new ArrayList<>(1);
        exclusivesList2.add(experiment.getID());

        Set<Experiment.ID> experimentSet = allExperiments.rowKeySet();

        Map<Experiment.ID, List<Experiment.ID>> exclusivesMap = new HashMap<>(2);
        exclusivesMap.put(experiment.getID(), exclusivesList);
        exclusivesMap.put(experiment2.getID(), exclusivesList2);

        Mockito.when(cassandraRepository.getExperimentList(testApp)).thenReturn(allExperiments);
        Mockito.when(cassandraRepository.getBucketList(experiment.getID())).thenReturn(expBucketList);
        Mockito.when(cassandraRepository.getBucketList(experiment2.getID())).thenReturn(exp2bucketList);
        Mockito.when(mutexRepository.getExclusivesList(experimentSet)).thenReturn(exclusivesMap);

        final Date DATE = new Date();

        Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
        result.put(experiment.getID(), experiment.getLabel(), "red");
        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-b"), testApp, context, allExperiments)).thenReturn(result);
        Assignment.Builder builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(null);
        Assignment assignment = builder.build();

        Assignment newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();
        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-a"), context)).thenReturn(newAssignment);

        assignment = assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-a"), context);
        assert assignment.getBucketLabel() == null;
        assert assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT;

        redBucket.setState(Bucket.State.CLOSED);
        redBucket.setAllocationPercent(0.0);
        yellowBucket.setAllocationPercent(1.0);

        result = HashBasedTable.create();
        result.put(experiment.getID(), experiment.getLabel(), "red");
        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-b"), testApp, context, allExperiments)).thenReturn(result);
        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-a"), testApp, context, allExperiments)).thenReturn(result);

        builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(null);
        assignment = builder.build();

        newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();
        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context)).thenReturn(newAssignment);

        assignment = assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context);
        assert assignment.getBucketLabel() == null;
        assert assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT;

        newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();

        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context)).thenReturn(newAssignment);
        assignment = assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context);

        assert assignment.getStatus() == Assignment.Status.EXISTING_ASSIGNMENT;
        assert assignment.getBucketLabel() == null;

        builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-c"))
                .withContext(context)
                .withBucketLabel(yellowBucket.getLabel());
        assignment = builder.build();

        newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();
        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-c"), context)).thenReturn(newAssignment);
        assignment = assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-c"), context);

        assert assignment.getBucketLabel() == yellowBucket.getLabel();
        assert assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT;

    }

    @Test
    public void checkMutexWithExperimentNullTrue() throws Exception {
        AssignmentsImpl impl = new AssignmentsImpl(assignmentsRepository, mutexRepository);
        boolean value = impl.checkMutex(null, null, Context.valueOf("dummystring"));
        then(value).isEqualTo(true);
    }

    /* FIXME
    @Test
    public void checkMutexWithEmptyExclusionListStateRunning() throws Exception {
    	AssignmentsImpl impl = new AssignmentsImpl(assignmentsRepository, mutexRepository);
    	ExperimentList list = new ExperimentList();
    	HashSet<ID> emptyIds = new HashSet<>();
    	Experiment experiment =
    			Experiment.withID(ID.newInstance()).
    			withApplicationName(Name.valueOf("test")).withState(Experiment.State.RUNNING).
    			build();
    	given(mutexRepository.getExclusions(isA(Experiment.ID.class))).willReturn(list);
    	given(assignmentsRepository.getUserAssignments(isA(User.ID.class), isA(Name.class), (Context) isNull()))
    		.willReturn(emptyIds);
    	boolean value = impl.checkMutex(experiment, User.ID.valueOf("user1"), null);
    	then(value).isEqualTo(true);
    }

    @Test
    public void checkMutexWithOneExclusionListButAssignmentEmptyStateRunning() throws Exception {
    	AssignmentsImpl impl = new AssignmentsImpl(assignmentsRepository, mutexRepository);
    	Experiment experiment =
    			Experiment.withID(ID.newInstance()).
    			withApplicationName(Name.valueOf("test")).withState(Experiment.State.RUNNING).
    			build();
    	ExperimentList list = new ExperimentList();
    	list.addExperiment(experiment);
    	HashSet<ID> emptyIds = new HashSet<>();
    	given(mutexRepository.getExclusions(isA(Experiment.ID.class))).willReturn(list);
    	given(assignmentsRepository.getUserAssignments(isA(User.ID.class), isA(Name.class), (Context) isNull()))
    		.willReturn(emptyIds);
    	boolean value = impl.checkMutex(experiment, User.ID.valueOf("user1"), null);
    	then(value).isEqualTo(true);
    }

    @Test
    public void checkMutexWithOneExclusionListAssignmentSameStateRunning() throws Exception {
    	AssignmentsImpl impl = new AssignmentsImpl(assignmentsRepository, mutexRepository);
    	Experiment experiment =
    			Experiment.withID(ID.newInstance()).
    			withApplicationName(Name.valueOf("test")).withState(Experiment.State.RUNNING).
    			build();
    	ExperimentList list = new ExperimentList();
    	list.addExperiment(experiment);
    	HashSet<ID> assignmentIds = new HashSet<>();
    	assignmentIds.add(experiment.getID());
    	given(mutexRepository.getExclusions(isA(Experiment.ID.class))).willReturn(list);
    	given(assignmentsRepository.getUserAssignments(isA(User.ID.class), isA(Name.class), (Context) isNull()))
    		.willReturn(assignmentIds);
    	boolean value = impl.checkMutex(experiment, User.ID.valueOf("user1"), null);
    	then(value).isEqualTo(false);
    }

    @Test
    public void checkMutexWithOneExclusionListAssignmentDeletedStateRunning() throws Exception {
    	AssignmentsImpl impl = new AssignmentsImpl(assignmentsRepository, mutexRepository);
    	Experiment experiment =
    			Experiment.withID(ID.newInstance()).
    			withApplicationName(Name.valueOf("test")).withState(Experiment.State.RUNNING).
    			build();
    	Experiment experimentassignment =
    			Experiment.withID(ID.newInstance()).
    			withApplicationName(Name.valueOf("test")).withState(Experiment.State.DELETED).
    			build();
    	ExperimentList list = new ExperimentList();
    	list.addExperiment(experiment);
    	HashSet<ID> assignmentIds = new HashSet<>();
    	assignmentIds.add(experimentassignment.getID());
    	given(mutexRepository.getExclusions(isA(Experiment.ID.class))).willReturn(list);
    	given(assignmentsRepository.getUserAssignments(isA(User.ID.class), isA(Name.class), (Context) isNull()))
    		.willReturn(assignmentIds);
    	boolean value = impl.checkMutex(experiment, User.ID.valueOf("user1"), null);
    	then(value).isEqualTo(true);
    }
    */

    @Test
    public void getAssignment_test_4() throws IOException, ConnectionException {
        final Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -1);
        final Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp_DE1")).withStartTime(c.getTime()).withSamplingPercent(1.0).withIsPersonalizationEnabled(true).build();
        c.add(Calendar.DATE, 10);
        experiment.setEndTime(c.getTime());

        Bucket experienceA = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("experienceA"))
                .withState(Bucket.State.OPEN).withAllocationPercent(.90).build();
        Bucket experienceB = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("experienceB"))
                .withState(Bucket.State.OPEN).withAllocationPercent(0.05).build();
        Bucket experienceC = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("experienceC"))
                .withState(Bucket.State.OPEN).withAllocationPercent(0.05).build();
        BucketList expBucketList = new BucketList(3);
        expBucketList.addBucket(experienceA);
        expBucketList.addBucket(experienceB);
        expBucketList.addBucket(experienceC);
        experiment.setState(Experiment.State.RUNNING);

        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments = HashBasedTable.create();
        allExperiments.put(experiment.getID(), experiment.getLabel(), experiment);
        Set<Experiment.ID> experimentSet = allExperiments.rowKeySet();

        List<Experiment.ID> exclusivesList = new ArrayList<>(1);
        exclusivesList.add(experiment.getID());

        Map<Experiment.ID, List<Experiment.ID>> exclusivesMap = new HashMap<>(2);
        exclusivesMap.put(experiment.getID(), exclusivesList);

        Mockito.when(cassandraRepository.getExperimentList(testApp)).thenReturn(allExperiments);
        Mockito.when(cassandraRepository.getBucketList(experiment.getID())).thenReturn(expBucketList);
        Mockito.when(mutexRepository.getExclusivesList(experimentSet)).thenReturn(exclusivesMap);

        final Date DATE = new Date();

        Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
        result.put(experiment.getID(), experiment.getLabel(), "experienceA");
        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-a"), testApp, context, allExperiments)).thenReturn(result);
        Assignment.Builder builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-a"))
                .withContext(context)
                .withBucketLabel(Bucket.Label.valueOf("experienceA"));
        Assignment assignment = builder.build();

        Assignment newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();
        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-a"), context)).thenReturn(newAssignment);

        assignment = assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-a"), context);
        assertTrue(assignment.getBucketLabel().equals(experienceA.getLabel()));
        assertTrue(assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT);


        result = HashBasedTable.create();
        result.put(experiment.getID(), experiment.getLabel(), "experienceA");
        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-b"), testApp, context, allExperiments)).thenReturn(result);
        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-a"), testApp, context, allExperiments)).thenReturn(result);

        builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(Bucket.Label.valueOf("experienceB"));
        assignment = builder.build();

        newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();
        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context)).thenReturn(newAssignment);

        assignment = assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context);
        assertTrue(assignment.getBucketLabel().equals(experienceB.getLabel()));
        assertTrue(assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT);

        newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();

        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context)).thenReturn(newAssignment);
        assignment = assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context);

        assertTrue(assignment.getStatus() == Assignment.Status.EXISTING_ASSIGNMENT);
        assertTrue(assignment.getBucketLabel().equals(experienceB.getLabel()));
        assertTrue(assignment.getStatus() == Assignment.Status.EXISTING_ASSIGNMENT);

        builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-c"))
                .withContext(context)
                .withBucketLabel(experienceC.getLabel());
        assignment = builder.build();

        newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();
        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-c"), context)).thenReturn(newAssignment);
        assignment = assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-c"), context);

        assertTrue(assignment.getBucketLabel() == experienceC.getLabel());
        assertTrue(assignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT);

    }

    @Test
    public void createAssignmentObjectTest() throws IOException, ConnectionException {

        //------- Prepare input data
        Date date = new Date();
        SegmentationProfile segmentationProfile = null;
        User.ID userID = User.ID.valueOf("test-user-1");
        Context context = Context.valueOf("TEST");
        boolean selectBucket = true;
        Experiment exp1 = Experiment.withID(Experiment.ID.valueOf(UUID.randomUUID()))
                .withApplicationName(Application.Name.valueOf("test-app-1"))
                .withLabel(Experiment.Label.valueOf("test-exp-1"))
                .withState(Experiment.State.RUNNING)
                .withIsPersonalizationEnabled(false)
                .build();
        BucketList bucketList1 = new BucketList();
        bucketList1.addBucket(Bucket.newInstance(exp1.getID(), Bucket.Label.valueOf("bucket-1")).withAllocationPercent(0.9d).withPayload("bucket1").withState(Bucket.State.OPEN).build());
        bucketList1.addBucket(Bucket.newInstance(exp1.getID(), Bucket.Label.valueOf("bucket-2")).withAllocationPercent(0.1d).withPayload("bucket-2").withState(Bucket.State.OPEN).build());

        //--------- Mock calls
        when(assignmentDecorator.getBucketList(exp1, userID, segmentationProfile)).thenReturn(bucketList1);

        //--------- Make actual call
        Assignment newAssignment = assignmentsImpl.createAssignmentObject(exp1, userID, context, selectBucket, bucketList1, date, segmentationProfile);

        //---------- Validate result
        assertTrue(newAssignment!=null);
        assertTrue(newAssignment.getStatus().equals(Assignment.Status.NEW_ASSIGNMENT));

    }

    @Test
    public void createAssignmentObjectTestForNoOpenBucket() throws IOException, ConnectionException {

        //------- Prepare input data
        Date date = new Date();
        SegmentationProfile segmentationProfile = null;
        User.ID userID = User.ID.valueOf("test-user-1");
        Context context = Context.valueOf("TEST");
        boolean selectBucket = true;
        Experiment exp1 = Experiment.withID(Experiment.ID.valueOf(UUID.randomUUID()))
                .withApplicationName(Application.Name.valueOf("test-app-1"))
                .withLabel(Experiment.Label.valueOf("test-exp-1"))
                .withState(Experiment.State.RUNNING)
                .withIsPersonalizationEnabled(false)
                .build();
        BucketList bucketList1 = new BucketList();
        bucketList1.addBucket(Bucket.newInstance(exp1.getID(), Bucket.Label.valueOf("bucket-1")).withAllocationPercent(0.0d).withPayload("bucket1").withState(Bucket.State.CLOSED).build());
        bucketList1.addBucket(Bucket.newInstance(exp1.getID(), Bucket.Label.valueOf("bucket-2")).withAllocationPercent(0.0d).withPayload("bucket-2").withState(Bucket.State.CLOSED).build());

        //--------- Mock calls
        when(assignmentDecorator.getBucketList(exp1, userID, segmentationProfile)).thenReturn(bucketList1);

        //--------- Make actual call
        Assignment newAssignment = assignmentsImpl.createAssignmentObject(exp1, userID, context, selectBucket, bucketList1, date, segmentationProfile);

        //---------- Validate result
        assertTrue(newAssignment!=null);
        assertTrue(newAssignment.getStatus().equals(Assignment.Status.NO_OPEN_BUCKETS));

    }


    @Test
    public void doBatchAssignmentsTest() throws IOException, ConnectionException {
        final Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -1);
        final Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp2")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        c.add(Calendar.DATE, 10);
        experiment.setEndTime(c.getTime());
        experiment2.setEndTime(c.getTime());

        final Bucket redBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("red"))
                .withState(Bucket.State.OPEN).withAllocationPercent(1.0).build();
        Bucket yellowBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("yellow"))
                .withState(Bucket.State.OPEN).withAllocationPercent(0.0).build();
        BucketList expBucketList = new BucketList(2);
        expBucketList.addBucket(redBucket);
        expBucketList.addBucket(yellowBucket);

        Bucket greenBucket = Bucket.newInstance(experiment2.getID(), Bucket.Label.valueOf("green")).withAllocationPercent(1.0).build();
        BucketList exp2bucketList = new BucketList(1);
        exp2bucketList.addBucket(greenBucket);

        experiment.setState(Experiment.State.RUNNING);
        experiment2.setState(Experiment.State.RUNNING);

        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments = HashBasedTable.create();
        allExperiments.put(experiment.getID(), experiment.getLabel(), experiment);
        allExperiments.put(experiment2.getID(), experiment2.getLabel(), experiment2);

        List<Experiment.ID> exclusivesList = new ArrayList<>(1);
        exclusivesList.add(experiment2.getID());

        List<Experiment.ID> exclusivesList2 = new ArrayList<>(1);
        exclusivesList2.add(experiment.getID());

        PrioritizedExperiment prioritizedExperiment1 = PrioritizedExperiment.from(experiment, 1).build();
        PrioritizedExperiment prioritizedExperiment2 = PrioritizedExperiment.from(experiment2, 2).build();
        PrioritizedExperimentList prioritizedExperimentList = new PrioritizedExperimentList();
        prioritizedExperimentList.addPrioritizedExperiment(prioritizedExperiment1);
        prioritizedExperimentList.addPrioritizedExperiment(prioritizedExperiment2);

        HashSet<Experiment.Label> labels = new HashSet<>(2);
        labels.add(experiment.getLabel());
        labels.add(experiment2.getLabel());
        ExperimentBatch experimentBatch;
        experimentBatch = ExperimentBatch.newInstance().withLabels(labels).build();

        HashSet<Experiment.ID> experimentIDs = new HashSet<>(2);
        experimentIDs.add(experiment.getID());
        experimentIDs.add(experiment2.getID());
        Set<Experiment.ID> experimentSet = allExperiments.rowKeySet();

        Map<Experiment.ID, BucketList> bucketListMap = new HashMap<>(2);
        bucketListMap.put(experiment.getID(), expBucketList);
        bucketListMap.put(experiment2.getID(), exp2bucketList);

        Map<Experiment.ID, List<Experiment.ID>> exclusivesMap = new HashMap<>(2);
        exclusivesMap.put(experiment.getID(), exclusivesList);
        exclusivesMap.put(experiment2.getID(), exclusivesList2);

        Mockito.when(cassandraRepository.getExperimentList(testApp)).thenReturn(allExperiments);
        Mockito.when(cassandraRepository.getBucketList(experiment.getID())).thenReturn(expBucketList);
        Mockito.when(cassandraRepository.getBucketList(experiment2.getID())).thenReturn(exp2bucketList);
        Mockito.when(mutexRepository.getExclusivesList(experimentSet)).thenReturn(exclusivesMap);
        Mockito.when(priorities.getPriorities(testApp, false)).thenReturn(prioritizedExperimentList);
        Mockito.when(cassandraRepository.getBucketList(experimentIDs)).thenReturn(bucketListMap);

        Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
        result.put(experiment.getID(), experiment.getLabel(), "red");
        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-a"), testApp, context, allExperiments)).thenReturn(result);

        final Date DATE = new Date();
        Assignment.Builder builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(null);
        Assignment assignment = builder.build();

        Assignment newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();
        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context)).thenReturn(newAssignment);

        builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(redBucket.getLabel());
        Assignment assignment_2 = builder.build();

        Assignment newAssignment_2 = Assignment.newInstance(assignment_2.getExperimentID())
                .withBucketLabel(assignment_2.getBucketLabel())
                .withUserID(assignment_2.getUserID())
                .withContext(assignment_2.getContext())
                .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();

        List<Map> batchAssignments = new ArrayList<>();
        Map<String, Object> tempResult = new HashMap<>();
        tempResult.put("assignment", newAssignment_2.getBucketLabel() != null ? newAssignment_2.getBucketLabel().toString() : null);
        tempResult.put("status", newAssignment_2.getStatus());
        batchAssignments.add(tempResult);
        Map<String, Object> tempResult1 = new HashMap<>();
        tempResult1.put("assignment", newAssignment.getBucketLabel() != null ? newAssignment.getBucketLabel().toString() : null);
        tempResult1.put("status", newAssignment.getStatus());
        batchAssignments.add(tempResult1);

        Mockito.when(cassandraAssignments.doBatchAssignments(User.ID.valueOf("user-a"), testApp,
                context, true, false, null, experimentBatch, null, null)).thenReturn(batchAssignments);

        assert batchAssignments.size() == 2;
        assert batchAssignments.get(0).get("assignment").toString().equals(redBucket.getLabel().toString());
        assert batchAssignments.get(0).get("status").toString().equals(Assignment.Status.EXISTING_ASSIGNMENT.toString());
        assert batchAssignments.get(1).get("assignment") == null;
        assert batchAssignments.get(1).get("status").toString().equals(Assignment.Status.NEW_ASSIGNMENT.toString());
    }

    @Test
    public void doPageAssignmentsTest() throws IOException, ConnectionException {
        final Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -1);
        final Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp2")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        c.add(Calendar.DATE, 10);
        experiment.setEndTime(c.getTime());
        experiment2.setEndTime(c.getTime());

        final Bucket redBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("red"))
                .withState(Bucket.State.OPEN).withAllocationPercent(1.0).build();
        Bucket yellowBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("yellow"))
                .withState(Bucket.State.OPEN).withAllocationPercent(0.0).build();
        BucketList expBucketList = new BucketList(2);
        expBucketList.addBucket(redBucket);
        expBucketList.addBucket(yellowBucket);

        Bucket greenBucket = Bucket.newInstance(experiment2.getID(), Bucket.Label.valueOf("green")).withAllocationPercent(1.0).build();
        BucketList exp2bucketList = new BucketList(1);
        exp2bucketList.addBucket(greenBucket);

        experiment.setState(Experiment.State.RUNNING);
        experiment2.setState(Experiment.State.RUNNING);

        Table<Experiment.ID, Experiment.Label, Experiment> allExperiments = HashBasedTable.create();
        allExperiments.put(experiment.getID(), experiment.getLabel(), experiment);
        allExperiments.put(experiment2.getID(), experiment2.getLabel(), experiment2);
        Set<Experiment.ID> experimentSet = allExperiments.rowKeySet();

        List<Experiment.ID> exclusivesList = new ArrayList<>(1);
        exclusivesList.add(experiment2.getID());

        List<Experiment.ID> exclusivesList2 = new ArrayList<>(1);
        exclusivesList2.add(experiment.getID());

        PrioritizedExperiment prioritizedExperiment1 = PrioritizedExperiment.from(experiment, 1).build();
        PrioritizedExperiment prioritizedExperiment2 = PrioritizedExperiment.from(experiment2, 2).build();
        PrioritizedExperimentList prioritizedExperimentList = new PrioritizedExperimentList();
        prioritizedExperimentList.addPrioritizedExperiment(prioritizedExperiment1);
        prioritizedExperimentList.addPrioritizedExperiment(prioritizedExperiment2);

        HashSet<Experiment.ID> experimentIDs = new HashSet<>(2);
        experimentIDs.add(experiment.getID());
        experimentIDs.add(experiment2.getID());

        HashMap<Experiment.ID, BucketList> bucketListMap = new HashMap<>(2);
        bucketListMap.put(experiment.getID(), expBucketList);
        bucketListMap.put(experiment2.getID(), exp2bucketList);

        Map<Experiment.ID, List<Experiment.ID>> exclusivesMap = new HashMap<>(2);
        exclusivesMap.put(experiment.getID(), exclusivesList);
        exclusivesMap.put(experiment2.getID(), exclusivesList2);

        PageExperiment pageExperiment = PageExperiment.withAttributes(experiment.getID(), experiment.getLabel(), true)
                .build();
        PageExperiment pageExperiment2 = PageExperiment.withAttributes(experiment2.getID(), experiment2.getLabel(),
                true).build();

        List<PageExperiment> pageExperimentList = new ArrayList<>(2);
        pageExperimentList.add(pageExperiment);
        pageExperimentList.add(pageExperiment2);

        final Page.Name pageName = Page.Name.valueOf("testPage");
        Mockito.when(cassandraRepository.getExperimentList(testApp)).thenReturn(allExperiments);
        Mockito.when(cassandraRepository.getBucketList(experiment.getID())).thenReturn(expBucketList);
        Mockito.when(cassandraRepository.getBucketList(experiment2.getID())).thenReturn(exp2bucketList);
        Mockito.when(mutexRepository.getExclusivesList(experimentSet)).thenReturn(exclusivesMap);
        Mockito.when(priorities.getPriorities(testApp, false)).thenReturn(prioritizedExperimentList);
        Mockito.when(cassandraRepository.getBucketList(experimentIDs)).thenReturn(bucketListMap);
        Mockito.when(pages.getExperiments(testApp, pageName)).thenReturn(pageExperimentList);

        Table<Experiment.ID, Experiment.Label, String> result = HashBasedTable.create();
        result.put(experiment.getID(), experiment.getLabel(), "red");
        Mockito.when(assignmentsRepository.getAssignments(User.ID.valueOf("user-a"), testApp, context, allExperiments)).thenReturn(result);

        final Date DATE = new Date();
        Assignment.Builder builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(null);
        Assignment assignment = builder.build();

        Assignment newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();
        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context)).thenReturn(newAssignment);

        builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(redBucket.getLabel());
        Assignment assignment_2 = builder.build();

        Assignment newAssignment_2 = Assignment.newInstance(assignment_2.getExperimentID())
                .withBucketLabel(assignment_2.getBucketLabel())
                .withUserID(assignment_2.getUserID())
                .withContext(assignment_2.getContext())
                .withStatus(Assignment.Status.EXISTING_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();

        List<Map> pageAssignments = new ArrayList<>();
        Map<String, Object> tempResult = new HashMap<>();
        tempResult.put("assignment", newAssignment_2.getBucketLabel() != null ? newAssignment_2.getBucketLabel().toString() : null);
        tempResult.put("status", newAssignment_2.getStatus());
        pageAssignments.add(tempResult);
        Map<String, Object> tempResult1 = new HashMap<>();
        tempResult1.put("assignment", newAssignment.getBucketLabel() != null ? newAssignment.getBucketLabel().toString() : null);
        tempResult1.put("status", newAssignment.getStatus());
        pageAssignments.add(tempResult1);

        Mockito.when(cassandraAssignments.doPageAssignments(testApp, pageName, User.ID.valueOf("user-a"), context, true, false, null, null)).thenReturn(pageAssignments);

        assert pageAssignments.size() == 2;
        assert pageAssignments.get(0).get("assignment").toString().equals(redBucket.getLabel().toString());
        assert pageAssignments.get(0).get("status").toString().equals(Assignment.Status.EXISTING_ASSIGNMENT.toString());
        assert pageAssignments.get(1).get("assignment") == null;
        assert pageAssignments.get(1).get("status").toString().equals(Assignment.Status.NEW_ASSIGNMENT.toString());
    }

    @Test
    public void putAssignment_test() throws IOException, ConnectionException {
        final Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        final Experiment.Label expLabel = Experiment.Label.valueOf("testExp");
        final User.ID userA = User.ID.valueOf("user-a");
        Bucket.Label bucketLabel = Bucket.Label.valueOf("red");
        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp).build();
        experiment.setLabel(expLabel);
        experiment.setState(Experiment.State.DRAFT);

        Bucket redBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("red"))
                .withState(Bucket.State.OPEN).withAllocationPercent(0.5).build();
        final Bucket blueBucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("blue"))
                .withState(Bucket.State.OPEN).withAllocationPercent(0.5).build();
        BucketList expBucketList = new BucketList(2);
        expBucketList.addBucket(redBucket);
        expBucketList.addBucket(blueBucket);

        Mockito.when(cassandraRepository.getExperiment(testApp, expLabel)).thenReturn(null);

        final Date DATE = new Date();
        Assignment.Builder builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(blueBucket.getLabel());
        Assignment assignment = builder.build();

        Assignment newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();

        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context)).thenReturn(newAssignment);
        Mockito.doNothing().when(assignmentsRepository).removeIndexUserToBucket(User.ID.valueOf("user-b"), experiment.getID(), context, assignment.getBucketLabel());
        Mockito.doNothing().when(assignmentsRepository).deleteAssignment(experiment, User.ID.valueOf("user-b"), context, testApp, assignment);

        experiment.setState(Experiment.State.TERMINATED);
        Mockito.when(cassandraRepository.getExperiment(testApp, experiment.getLabel())).thenReturn(experiment);
        Mockito.when(cassandraRepository.getBuckets(experiment.getID(), false)).thenReturn(expBucketList);

        boolean assertionErrorCaught = false;
        try {
            cassandraAssignments.putAssignment(userA, testApp, expLabel, context, bucketLabel, true);
        } catch (AssertionError ae) {
            assertionErrorCaught = true;
        }
        assertFalse(assertionErrorCaught);

        builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(redBucket.getLabel());
        assignment = builder.build();

        newAssignment = Assignment.newInstance(assignment.getExperimentID())
                .withBucketLabel(assignment.getBucketLabel())
                .withUserID(assignment.getUserID())
                .withContext(assignment.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();

        Mockito.when(assignmentsRepository.assignUser(assignment, experiment, DATE)).thenReturn(newAssignment);
        Mockito.when(assignmentsRepository.getAssignment(experiment.getID(), User.ID.valueOf("user-b"), context)).thenReturn(null);
        Mockito.doNothing().when(assignmentsRepository).removeIndexUserToBucket(User.ID.valueOf("user-b"), experiment.getID(), context, assignment.getBucketLabel());
        Mockito.doNothing().when(assignmentsRepository).deleteAssignment(experiment, User.ID.valueOf("user-b"), context, testApp, assignment);

        Mockito.when(cassandraAssignments.putAssignment(userA, testApp, expLabel, context, redBucket.getLabel(), true)).thenReturn(newAssignment);
        assert newAssignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT;
        assert newAssignment.getBucketLabel().toString().equals(redBucket.getLabel().toString());

        Mockito.when(cassandraAssignments.putAssignment(userA, testApp, expLabel, context, redBucket.getLabel(), false)).thenReturn(newAssignment);
        assert newAssignment.getStatus() == Assignment.Status.NEW_ASSIGNMENT;
        assert newAssignment.getBucketLabel().toString().equals(redBucket.getLabel().toString());

        builder = Assignment.newInstance(experiment.getID())
                .withApplicationName(experiment.getApplicationName())
                .withUserID(User.ID.valueOf("user-b"))
                .withContext(context)
                .withBucketLabel(null);
        Assignment assignment_2 = builder.build();

        Assignment newAssignment_2 = Assignment.newInstance(assignment_2.getExperimentID())
                .withBucketLabel(assignment_2.getBucketLabel())
                .withUserID(assignment_2.getUserID())
                .withContext(assignment_2.getContext())
                .withStatus(Assignment.Status.NEW_ASSIGNMENT)
                .withCreated(DATE)
                .withCacheable(null)
                .build();

        Mockito.when(cassandraAssignments.putAssignment(userA, testApp, expLabel, context, null, true)).thenReturn(newAssignment);
        assert newAssignment_2.getStatus() == Assignment.Status.NEW_ASSIGNMENT;
        assert newAssignment_2.getBucketLabel() == null;
    }

    // FIXME:
//    @Ignore("FIXME:refactor-core")
//    @Test
//    public void URIConstructorTest() throws IOException, URISyntaxException, ConnectionException {
//
//        cassandraAssignments = null; //new AssignmentsImpl(cassandraRepository, assignmentsRepository, mutexRepository, random,
////                ruleCache, pages, priorities, assignmentDBEnvelopeProvider, assignmentWebEnvelopeProvider, null, //FIXME
////                decisionEngineScheme, decisionEngineHost, decisionEnginePath,
////                decisionEngineReadTimeOut, decisionEngineConnectionTimeOut, decisionEngineUseProxy,
////                decisionEngineUseConnectionPooling, decisionEngineMaxConnectionsPerHost, proxyPort, proxyHost, eventLog);
//
//
//        final Calendar c = Calendar.getInstance();
//        c.setTime(new Date());
//        c.add(Calendar.DATE, -1);
//        //Simple case of an experiment with model name and model version
//        final Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
//                .withLabel(Experiment.Label.valueOf("exp_DE1")).withStartTime(c.getTime()).withSamplingPercent(1.0).withIsPersonalizationEnabled(true)
//                .withModelName("model")
//                .withModelVersion("1.0")
//                .build();
//        c.add(Calendar.DATE, 10);
//        experiment.setEndTime(c.getTime());
//        // FIXME:refactor-core
////        assertTrue(cassandraAssignments.URIConstructor(experiment).toString().equals("http://foo.com/model"));
//
//        //To test URL encoding when there is space in model name
//        final Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
//                .withLabel(Experiment.Label.valueOf("exp_DE1")).withStartTime(c.getTime()).withSamplingPercent(1.0).withIsPersonalizationEnabled(true)
//                .withModelName("Model")
//                .withModelVersion("1.0")
//                .build();
//        c.add(Calendar.DATE, 10);
//        experiment.setEndTime(c.getTime());
//        // FIXME:refactor-core
////        assertTrue(cassandraAssignments.URIConstructor(experiment2).toString().equals("http://foo.com/model"));
//
//        //When model version is absent
//        final Experiment experiment3 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
//                .withLabel(Experiment.Label.valueOf("exp_DE1")).withStartTime(c.getTime()).withSamplingPercent(1.0).withIsPersonalizationEnabled(true)
//                .withModelName("model")
//                .build();
//        c.add(Calendar.DATE, 10);
//        experiment.setEndTime(c.getTime());
//        // FIXME:refactor-core
////        assertTrue(cassandraAssignments.URIConstructor(experiment3).toString().equals("http://foo.com/model"));
//
//        //When model name and model version are both absent
//        final Experiment experiment4 = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
//                .withLabel(Experiment.Label.valueOf("exp_DE1")).withStartTime(c.getTime()).withSamplingPercent(1.0).withIsPersonalizationEnabled(true)
//                .build();
//        c.add(Calendar.DATE, 10);
//        experiment.setEndTime(c.getTime());
//        // FIXME:refactor-core
//
////        assertNull(cassandraAssignments.URIConstructor(experiment4));
//    }

    /**
     * This methods tests the {@link AssignmentsImpl#mergePersonalizationResponseWithSegmentation(SegmentationProfile, PersonalizationEngineResponse)}
     */
    // FIXME:
//    @Test
//    @Ignore("FIXME")
//    public void testMergePersonalizationResponseWithSegmentation() {
//        AssignmentsImpl assignmentsImpl = null; //new AssignmentsImpl();
//
//        //set up segmentation profile mock
//        SegmentationProfile segmentationProfile = SegmentationProfile.newInstance().build();
//        //set up personalization mock
//        PersonalizationEngineResponse persRes = mock(PersonalizationEngineResponse.class);
//        when(persRes.getTid()).thenReturn("Tid");
//        Map<String, Double> testData = new HashMap<>();
//        when(persRes.getData()).thenReturn(testData);
//        when(persRes.getModel()).thenReturn("model");
//
//        assignmentsImpl.mergePersonalizationResponseWithSegmentation(segmentationProfile, persRes);
//        verify(persRes).getData();
//        verify(persRes).getModel();
//        verify(persRes).getTid();
//
//        assertEquals(segmentationProfile.getAttribute("tid"), "Tid");
//        assertEquals(segmentationProfile.getAttribute("data"), testData);
//        assertEquals(segmentationProfile.getAttribute("model"), "model");
//    }
        /*
    FIXME: Traffic Analyzer change commented for Datastax-driver-migration release...

    @Test
    public void testGetExperimentAssignmentRatioPerDay() {
        Experiment experiment1 = Experiment.withID(Experiment.ID.newInstance()).build();
        Map<OffsetDateTime, Double> map1 = new HashMap<>();
        map1.put(OffsetDateTime.now(), 1d);

        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance()).build();
        Map<OffsetDateTime, Double> map2 = new HashMap<>();
        map2.put(OffsetDateTime.now(), 2d);

        Experiment experiment3 = Experiment.withID(Experiment.ID.newInstance()).build();
        Map<OffsetDateTime, Double> map3 = new HashMap<>();
        map3.put(OffsetDateTime.now(), 3d);

        doReturn(map1).when(assignmentsRepository).getExperimentBucketAssignmentRatioPerDay(experiment1.getID(), OffsetDateTime.MIN, OffsetDateTime.MAX);
        doReturn(map2).when(assignmentsRepository).getExperimentBucketAssignmentRatioPerDay(experiment2.getID(), OffsetDateTime.MIN, OffsetDateTime.MAX);
        doReturn(map3).when(assignmentsRepository).getExperimentBucketAssignmentRatioPerDay(experiment3.getID(), OffsetDateTime.MIN, OffsetDateTime.MAX);

        List<Experiment> experiments = new ArrayList<>();
        experiments.add(experiment1);
        experiments.add(experiment2);

        Map<Experiment.ID, Map<OffsetDateTime, Double>> actual = assignmentsImpl.getExperimentAssignmentRatioPerDay(experiments, OffsetDateTime.MIN, OffsetDateTime.MAX);
        Assert.assertEquals("T1 Should exactly return two items.", 2, actual.size());
        Assert.assertEquals("T1 EntrySet for ID 1 should contain 1 element", 1, actual.get(experiment1.getID()).size());
        Assert.assertEquals("T1 EntrySet for ID 1 should contain map1", map1, actual.get(experiment1.getID()));
        Assert.assertEquals("T1 EntrySet for ID 2 should contain 1 element", 1, actual.get(experiment2.getID()).size());
        Assert.assertEquals("T1 EntrySet for ID 2 should contain map2", map2, actual.get(experiment2.getID()));

        experiments.add(experiment3);
        actual = assignmentsImpl.getExperimentAssignmentRatioPerDay(experiments, OffsetDateTime.MIN, OffsetDateTime.MAX);
        Assert.assertEquals("T2 Should exactly return three items.", 3, actual.size());
        Assert.assertEquals("T2 EntrySet for ID 1 should contain 1 element", 1, actual.get(experiment1.getID()).size());
        Assert.assertEquals("T2 EntrySet for ID 1 should contain map1", map1, actual.get(experiment1.getID()));
        Assert.assertEquals("T2 EntrySet for ID 2 should contain 1 element", 1, actual.get(experiment2.getID()).size());
        Assert.assertEquals("T2 EntrySet for ID 2 should contain map2", map2, actual.get(experiment2.getID()));
        Assert.assertEquals("T2 EntrySet for ID 3 should contain 1 element", 1, actual.get(experiment3.getID()).size());
        Assert.assertEquals("T2 EntrySet for ID 3 should contain map3", map3, actual.get(experiment3.getID()));
    }

    FIXME: Traffic Analyzer change commented for Datastax-driver-migration release...

    @Test
    public void testGetExperimentAssignmentRatioPerDayTable() {
        // Prepare return values for assignmentsRepository mock
        Map<OffsetDateTime, Double> assignmentRatios = new HashMap<>(10);
        Map<OffsetDateTime, Double> assignmentRatiosPart = new HashMap<>(4);
        List<OffsetDateTime> offsetDateTimes = new ArrayList<>(10);
        List<String> dateStrings = new ArrayList<>(10);
        for (int i = 1; i <= 10; ++i) {
            OffsetDateTime odt = OffsetDateTime.of(2000, 1, i, 0, 0, 0, 0, ZoneOffset.UTC);
            offsetDateTimes.add(odt);
            dateStrings.add(DateTimeFormatter.ofPattern("M/d/y").format(odt));

            assignmentRatios.put(odt, i * 0.1);
            if (i <= 7 && i >= 4) {
                assignmentRatiosPart.put(odt, i * 0.1);
            }
        }

        // Prepare list of experiments and their priorities, and set up assignmentsRepository to return the right maps
        List<Experiment> experiments = new ArrayList<>();
        Map<Experiment.ID, Integer> priorities = new HashMap<>();
        for (int i = 1; i <= 5; ++i) {
            Experiment experiment = Experiment.withID(Experiment.ID.newInstance())
                    .withLabel(Experiment.Label.valueOf(String.format("Exp%s", i)))
                    .withSamplingPercent(i * 0.1)
                    .build();
            experiments.add(experiment);
            priorities.put(experiment.getID(), (i + 1) % 5 + 1); // results in priorities: 3, 4, 5, 1, 2
            doReturn(assignmentRatios).when(assignmentsRepository).getExperimentBucketAssignmentRatioPerDay(experiment.getID(), offsetDateTimes.get(0), offsetDateTimes.get(9));
            doReturn(assignmentRatiosPart).when(assignmentsRepository).getExperimentBucketAssignmentRatioPerDay(experiment.getID(), offsetDateTimes.get(3), offsetDateTimes.get(6));
        }

        // query and check results: with all data
        List<ImmutableMap<String, ?>> results = new ArrayList<>(2);
        results.add(assignmentsImpl.getExperimentAssignmentRatioPerDayTable(experiments, priorities, offsetDateTimes.get(0), offsetDateTimes.get(9)));
        results.add(assignmentsImpl.getExperimentAssignmentRatioPerDayTable(experiments, priorities, offsetDateTimes.get(3), offsetDateTimes.get(6)));
        for (int i = 0; i < results.size(); ++i) {
            ImmutableMap result = results.get(i);
            Assert.assertTrue("Result must contain key 'experiments'", result.containsKey("experiments"));
            Assert.assertTrue("Result must contain a list for key 'experiments'", result.get("experiments") instanceof List);

            Assert.assertTrue("Result must contain key 'priorities'", result.containsKey("priorities"));
            Assert.assertTrue("Result must contain a list for key 'priorities'", result.get("priorities") instanceof List);

            Assert.assertTrue("Result must contain key 'samplingPercentages'", result.containsKey("samplingPercentages"));
            Assert.assertTrue("Result must contain a list for key 'samplingPercentages'", result.get("samplingPercentages") instanceof List);

            Assert.assertTrue("Result must contain key 'assignmentRatios'", result.containsKey("assignmentRatios"));
            Assert.assertTrue("Result must contain a list for key 'assignmentRatios'", result.get("assignmentRatios") instanceof List);

            Assert.assertEquals("List 'experiments' must contain 5 items.", 5, ((List) result.get("experiments")).size());
            Assert.assertEquals("List 'priorities' must contain 5 items.", 5, ((List) result.get("priorities")).size());
            Assert.assertEquals("List 'samplingPercentages' must contain 5 items.", 5, ((List) result.get("samplingPercentages")).size());
            Assert.assertEquals(String.format("List 'assignmentRatios' must contain %d items.", i == 0 ? 10 : 4), i == 0 ? 10 : 4, ((List) result.get("assignmentRatios")).size());

            @SuppressWarnings("unchecked")
            List<Map<String, ?>> assignmentRatiosResult = (List<Map<String, ?>>) result.get("assignmentRatios");

            Assert.assertTrue(String.format("ratios do not contain 'date' keys (i == %d)", i),
                    assignmentRatiosResult.stream()
                            .allMatch(m -> m.containsKey("date")));
            Assert.assertTrue(String.format("ratios do not contain all dates (i == %d)", i),
                    assignmentRatiosResult.stream()
                            .map(m -> m.get("date"))
                            .collect(Collectors.toList())
                            .containsAll(i == 0 ? dateStrings : dateStrings.subList(3, 7)));

            Assert.assertTrue(String.format("ratios do not contain 'values' keys (i == %d)", i),
                    assignmentRatiosResult.stream()
                            .allMatch(m -> m.containsKey("values")));
            Assert.assertTrue(String.format("ratios should always have 5 values (i == %d)", i),
                    assignmentRatiosResult.stream()
                            .map(m -> ((List) m.get("values")).size())
                            .allMatch(val -> val == 5));
        }
    }
    */
}
