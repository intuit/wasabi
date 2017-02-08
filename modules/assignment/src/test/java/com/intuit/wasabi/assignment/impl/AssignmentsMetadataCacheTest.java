/*
  *****************************************************************************
  Copyright 2016 Intuit

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.intuit.wasabi.assignment.impl;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.name.Named;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheHealthCheck;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheImpl;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheRefreshTask;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.PagesRepository;
import com.intuit.wasabi.repository.PrioritiesRepository;
import net.sf.ehcache.CacheManager;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Class to unit test AssignmentsMetadataCache
 *
 */
public class AssignmentsMetadataCacheTest {
    //Define mock objects
    private static ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
    private static PrioritiesRepository prioritiesRepository = mock(PrioritiesRepository.class);
    private static MutexRepository mutexRepository = mock(MutexRepository.class);
    private static PagesRepository pagesRepository = mock(PagesRepository.class);
    private static ScheduledExecutorService refreshCacheService = mock(ScheduledExecutorService.class);
    private static Integer refreshIntervalInMinutes = 5;
    private static AssignmentsMetadataCacheRefreshTask metadataCacheRefreshTask = mock(AssignmentsMetadataCacheRefreshTask.class);
    private static HealthCheckRegistry healthCheckRegistry = mock(HealthCheckRegistry.class);
    private static AssignmentsMetadataCacheHealthCheck metadataCacheHealthCheck = mock(AssignmentsMetadataCacheHealthCheck.class);
    private static CacheManager cacheManager = CacheManager.getInstance();
    private static Map resultMap = mock(Map.class);
    private static List resultList = mock(List.class);
    private static Experiment exprResultObject = mock(Experiment.class);
    private static PrioritizedExperimentList pExpListResultObject = mock(PrioritizedExperimentList.class);
    private static BucketList bucketListResultObject = mock(BucketList.class);

    //Define real object not a mock
    private static AssignmentsMetadataCache cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
            mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
            healthCheckRegistry, metadataCacheHealthCheck, cacheManager);

    @Test
    public void happyPathTestForGetExperimentsByAppName() {
        //Mock the dependencies
        Application.Name appName1 = Application.Name.valueOf("Test-App-1");
        Set<Application.Name> appName1Set = singleEntrySet(appName1);
        when(resultMap.get(appName1)).thenReturn(resultList);
        when(experimentRepository.getExperimentsForApps(appName1Set)).thenReturn(resultMap);

        Application.Name appName2 = Application.Name.valueOf("Test-App-2");
        Set<Application.Name> appName2Set = singleEntrySet(appName2);
        when(resultMap.get(appName2)).thenReturn(resultList);
        when(experimentRepository.getExperimentsForApps(appName2Set)).thenReturn(resultMap);

        //Call actual business logic
        //Call same method for same appName 3 times
        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName2);
        cache.getExperimentsByAppName(appName2);
        cache.getExperimentsByAppName(appName2);

        //Verify that even though cache is called 3 times there is only single database call.
        verify(experimentRepository, times(2)).getExperimentsForApps(any());

    }

    @Test
    public void happyPathTestForGetExperimentById() {
        //Mock the dependencies
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        Set<Experiment.ID> expIdSet = singleEntrySet(expId);
        when(resultMap.get(expId)).thenReturn(exprResultObject);
        when(experimentRepository.getExperimentsMap(expIdSet)).thenReturn(resultMap);

        //Call actual business logic
        //Call same method for same appName 3 times
        cache.getExperimentById(expId);
        cache.getExperimentById(expId);
        cache.getExperimentById(expId);

        //Verify that even though cache is called 3 times there is only single database call.
        verify(experimentRepository, times(1)).getExperimentsMap(any());
    }

    @Test
    public void happyPathTestForGetPrioritizedExperimentListMap() {
        //Mock the dependencies
        Application.Name appName1 = Application.Name.valueOf("Test-App-1");
        Set<Application.Name> appName1Set = singleEntrySet(appName1);
        when(resultMap.get(appName1)).thenReturn(pExpListResultObject);
        when(prioritiesRepository.getPriorities(appName1Set)).thenReturn(resultMap);

        //Call actual business logic
        //Call same method for same appName 3 times
        cache.getPrioritizedExperimentListMap(appName1);
        cache.getPrioritizedExperimentListMap(appName1);
        cache.getPrioritizedExperimentListMap(appName1);

        //Verify that even though cache is called 3 times there is only single database call.
        verify(prioritiesRepository, times(1)).getPriorities(appName1Set);
    }

    @Test
    public void happyPathTestForGetExclusionList() {
        //Mock the dependencies
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        Set<Experiment.ID> expIdSet = singleEntrySet(expId);
        when(resultMap.get(expId)).thenReturn(resultList);
        when(mutexRepository.getExclusivesList(expIdSet)).thenReturn(resultMap);

        //Call actual business logic
        //Call same method for same appName 3 times
        cache.getExclusionList(expId);
        cache.getExclusionList(expId);
        cache.getExclusionList(expId);

        //Verify that even though cache is called 3 times there is only single database call.
        verify(mutexRepository, times(1)).getExclusivesList(expIdSet);
    }

    @Test
    public void happyPathTestForGetBucketList() {
        //Mock the dependencies
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        Set<Experiment.ID> expIdSet = singleEntrySet(expId);
        when(resultMap.get(expId)).thenReturn(bucketListResultObject);
        when(experimentRepository.getBucketList(expIdSet)).thenReturn(resultMap);

        //Call actual business logic
        //Call same method for same appName 3 times
        cache.getBucketList(expId);
        cache.getBucketList(expId);
        cache.getBucketList(expId);

        //Verify that even though cache is called 3 times there is only single database call.
        verify(experimentRepository, times(1)).getBucketList(expIdSet);
    }

    @Test
    public void happyPathTestForGetPageExperiments() {
        //Mock the dependencies
        Application.Name appName = Application.Name.valueOf("Test-App-1");
        Page.Name pageName = Page.Name.valueOf("Test-Page-1");
        Pair<Application.Name, Page.Name> appPagePair = Pair.of(appName, pageName);
        Set<Pair<Application.Name, Page.Name>> appPagePairSet = singleEntrySet(appPagePair);
        when(resultMap.get(appPagePair)).thenReturn(resultList);
        when(pagesRepository.getExperimentsWithoutLabels(appPagePairSet)).thenReturn(resultMap);

        //Call actual business logic
        //Call same method for same appName 3 times
        cache.getPageExperiments(appName, pageName);
        cache.getPageExperiments(appName, pageName);
        cache.getPageExperiments(appName, pageName);

        //Verify that even though cache is called 3 times there is only single database call.
        verify(pagesRepository, times(1)).getExperimentsWithoutLabels(appPagePairSet);
    }




    /**
     * Utility to create single entry set
     *
     * @param entry
     * @param <T>
     * @return
     */
    private <T> Set<T> singleEntrySet(T entry) {
        Set aSet = new HashSet<T>(1);
        aSet.add(entry);
        return aSet;
    }

}
