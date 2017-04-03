/*
  *****************************************************************************
  Copyright 2017 Intuit

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

import com.codahale.metrics.health.HealthCheckRegistry;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheHealthCheck;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheImpl;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheRefreshTask;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.PagesRepository;
import com.intuit.wasabi.repository.PrioritiesRepository;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Class to unit test AssignmentsMetadataCache
 */
public class AssignmentsMetadataCacheTest {
    //Define mock objects
    private ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
    private PrioritiesRepository prioritiesRepository = mock(PrioritiesRepository.class);
    private MutexRepository mutexRepository = mock(MutexRepository.class);
    private PagesRepository pagesRepository = mock(PagesRepository.class);
    private ScheduledExecutorService refreshCacheService = mock(ScheduledExecutorService.class);
    private Integer refreshIntervalInMinutes = 5;
    private AssignmentsMetadataCacheRefreshTask metadataCacheRefreshTask = mock(AssignmentsMetadataCacheRefreshTask.class);
    private HealthCheckRegistry healthCheckRegistry = mock(HealthCheckRegistry.class);
    private AssignmentsMetadataCacheHealthCheck metadataCacheHealthCheck = mock(AssignmentsMetadataCacheHealthCheck.class);

    private List appResultList = mock(List.class);
    private Map appResultMap = mock(Map.class);

    private Experiment expResultObject = mock(Experiment.class);
    private Map expResultMap = mock(Map.class);

    private PrioritizedExperimentList priorityResultObject = mock(PrioritizedExperimentList.class);
    private Map priorityResultMap = mock(Map.class);

    private BucketList bucketResultObject = mock(BucketList.class);
    private Map bucketResultMap = mock(Map.class);

    private List exclusionResultList = mock(List.class);
    private Map exclusionResultMap = mock(Map.class);

    private List pageResultList = mock(List.class);
    private Map pageResultMap = mock(Map.class);

    @Test
    public void happyPathTestForGetExperimentsByAppName() {

        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForGetExperimentsByAppName");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Application.Name appName1 = Application.Name.valueOf("Test-App-1");
        Set<Application.Name> appName1Set = singleEntrySet(appName1);
        when(appResultMap.get(appName1)).thenReturn(appResultList);
        when(experimentRepository.getExperimentsForApps(appName1Set)).thenReturn(appResultMap);

        //Call actual business logic
        //Call same method for same appName 3 times
        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName1);

        //Verify that even though cache is called 3 times there is only single database call.
        verify(experimentRepository, times(1)).getExperimentsForApps(any());

    }

    @Test
    public void happyPathTestForGetExperimentById() {

        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForGetExperimentById");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        Set<Experiment.ID> expIdSet = singleEntrySet(expId);
        when(expResultMap.get(expId)).thenReturn(expResultObject);
        when(experimentRepository.getExperimentsMap(expIdSet)).thenReturn(expResultMap);

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
        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForGetPrioritizedExperimentListMap");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Application.Name appName1 = Application.Name.valueOf("Test-App-1");
        Set<Application.Name> appName1Set = singleEntrySet(appName1);
        when(priorityResultMap.get(appName1)).thenReturn(priorityResultObject);
        when(prioritiesRepository.getPriorities(appName1Set)).thenReturn(priorityResultMap);

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
        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForGetExclusionList");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        Set<Experiment.ID> expIdSet = singleEntrySet(expId);
        when(exclusionResultMap.get(expId)).thenReturn(exclusionResultList);
        when(mutexRepository.getExclusivesList(expIdSet)).thenReturn(exclusionResultMap);

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
        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForGetBucketList");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        Set<Experiment.ID> expIdSet = singleEntrySet(expId);
        when(bucketResultMap.get(expId)).thenReturn(bucketResultObject);
        when(experimentRepository.getBucketList(expIdSet)).thenReturn(bucketResultMap);

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
        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForGetPageExperiments");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Application.Name appName = Application.Name.valueOf("Test-App-1");
        Page.Name pageName = Page.Name.valueOf("Test-Page-1");
        Pair<Application.Name, Page.Name> appPagePair = Pair.of(appName, pageName);
        Set<Pair<Application.Name, Page.Name>> appPagePairSet = singleEntrySet(appPagePair);
        when(pageResultMap.get(appPagePair)).thenReturn(pageResultList);
        when(pagesRepository.getExperimentsWithoutLabels(appPagePairSet)).thenReturn(pageResultMap);

        //Call actual business logic
        //Call same method for same appName 3 times
        cache.getPageExperiments(appName, pageName);
        cache.getPageExperiments(appName, pageName);
        cache.getPageExperiments(appName, pageName);

        //Verify that even though cache is called 3 times there is only single database call.
        verify(pagesRepository, times(1)).getExperimentsWithoutLabels(appPagePairSet);
    }


    //===============================================================================================================


    @Test
    public void errorPath1TestForGetExperimentsByAppName() {

        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("errorPath1TestForGetExperimentsByAppName");
        AssignmentsMetadataCacheImpl cache2 = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Application.Name appName1 = Application.Name.valueOf("Test-App-1");
        Set<Application.Name> appName1Set = singleEntrySet(appName1);
        when(experimentRepository.getExperimentsForApps(appName1Set)).thenThrow(new NullPointerException());

        //Call actual business logic
        try {
            cache2.getExperimentsByAppName(appName1);
            fail("Any database exception should be propagated to the caller method..");
        } catch (Exception e) {

        }

        //-------------------------------------------------------------------------------------------------------------

        //Mock the dependencies
        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        Set<Experiment.ID> expIdSet = singleEntrySet(expId);
        when(experimentRepository.getExperimentsMap(expIdSet)).thenThrow(new NullPointerException());

        //Call actual business logic
        try {
            cache2.getExperimentById(expId);
            fail("Any database exception should be propagated to the caller method..");
        } catch (Exception e) {

        }
        //-------------------------------------------------------------------------------------------------------------


        //Mock the dependencies
        when(prioritiesRepository.getPriorities(appName1Set)).thenThrow(new NullPointerException());

        //Call actual business logic
        try {
            cache2.getPrioritizedExperimentListMap(appName1);
            fail("Any database exception should be propagated to the caller method..");
        } catch (Exception e) {

        }

        //-------------------------------------------------------------------------------------------------------------

        //Mock the dependencies
        when(mutexRepository.getExclusivesList(expIdSet)).thenThrow(new NullPointerException());

        //Call actual business logic
        try {
            cache2.getExclusionList(expId);
            fail("Any database exception should be propagated to the caller method..");
        } catch (Exception e) {

        }

        //-------------------------------------------------------------------------------------------------------------

        //Mock the dependencies
        when(experimentRepository.getBucketList(expIdSet)).thenThrow(new NullPointerException());

        //Call actual business logic
        try {
            cache2.getBucketList(expId);
            fail("Any database exception should be propagated to the caller method..");
        } catch (Exception e) {

        }

        //-------------------------------------------------------------------------------------------------------------

        //Mock the dependencies
        Application.Name appName = Application.Name.valueOf("Test-App-1");
        Page.Name pageName = Page.Name.valueOf("Test-Page-1");
        Pair<Application.Name, Page.Name> appPagePair = Pair.of(appName, pageName);
        Set<Pair<Application.Name, Page.Name>> appPagePairSet = singleEntrySet(appPagePair);
        when(pagesRepository.getExperimentsWithoutLabels(appPagePairSet)).thenThrow(new NullPointerException());

        //Call actual business logic
        try {
            cache2.getPageExperiments(appName, pageName);
            fail("Any database exception should be propagated to the caller method..");
        } catch (Exception e) {

        }

    }

    //===============================================================================================================

    @Test
    public void happyPathTestForClear() {
        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForClear");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Application.Name appName1 = Application.Name.valueOf("Test-App-1");
        Set<Application.Name> appName1Set = singleEntrySet(appName1);
        when(appResultMap.get(appName1)).thenReturn(appResultList);
        when(experimentRepository.getExperimentsForApps(appName1Set)).thenReturn(appResultMap);

        //Call actual business logic
        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName1);

        cache.clear();

        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName1);
        cache.getExperimentsByAppName(appName1);

        //Verify that there are 2 database calls, one before cache clear() and another after cache clear().
        verify(experimentRepository, times(2)).getExperimentsForApps(any());

    }

    @Test
    public void happyPathTestForRefresh() {
        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForRefresh");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Application.Name appName = Application.Name.valueOf("Test-App-1");
        Set<Application.Name> appNameSet = singleEntrySet(appName);
        List<Application.Name> appNameList = singleEntryList(appName);

        Experiment.ID expId = Experiment.ID.valueOf(UUID.randomUUID());
        Set<Experiment.ID> expIdSet = singleEntrySet(expId);
        List<Experiment.ID> expIdList = singleEntryList(expId);

        Page.Name pageName = Page.Name.valueOf("Test-Page-1");
        Pair<Application.Name, Page.Name> appPagePair = Pair.of(appName, pageName);
        Set<Pair<Application.Name, Page.Name>> appPagePairSet = singleEntrySet(appPagePair);
        List<Pair<Application.Name, Page.Name>> appPagePairList = singleEntryList(appPagePair);


        when(appResultMap.get(appName)).thenReturn(appResultList);
        when(experimentRepository.getExperimentsForApps(appNameSet)).thenReturn(appResultMap);

        when(expResultMap.get(expId)).thenReturn(expResultObject);
        when(experimentRepository.getExperimentsMap(expIdSet)).thenReturn(expResultMap);

        when(priorityResultMap.get(appName)).thenReturn(priorityResultObject);
        when(prioritiesRepository.getPriorities(appNameSet)).thenReturn(priorityResultMap);

        when(exclusionResultMap.get(expId)).thenReturn(exclusionResultList);
        when(mutexRepository.getExclusivesList(expIdSet)).thenReturn(exclusionResultMap);

        when(bucketResultMap.get(expId)).thenReturn(bucketResultObject);
        when(experimentRepository.getBucketList(expIdSet)).thenReturn(bucketResultMap);

        when(pageResultMap.get(appPagePair)).thenReturn(pageResultList);
        when(pagesRepository.getExperimentsWithoutLabels(appPagePairSet)).thenReturn(pageResultMap);

        //Actual calls
        //Fill up cache by requesting entities
        cache.getExperimentsByAppName(appName);
        cache.getExperimentById(expId);
        cache.getBucketList(expId);
        cache.getPrioritizedExperimentListMap(appName);
        cache.getExclusionList(expId);
        cache.getPageExperiments(appName, pageName);

        cache.refresh();
        cache.refresh();

        //Verify that there are total 3 database calls for each entity, first to fill up cache and 2 while refresh() as refresh() is called 2 times.
        verify(experimentRepository, times(1)).getExperimentsForApps(appNameSet);
        verify(experimentRepository, times(1)).getExperimentsMap(expIdSet);
        verify(prioritiesRepository, times(1)).getPriorities(appNameSet);
        verify(mutexRepository, times(1)).getExclusivesList(expIdSet);
        verify(experimentRepository, times(1)).getBucketList(expIdSet);
        verify(pagesRepository, times(1)).getExperimentsWithoutLabels(appPagePairSet);

        verify(experimentRepository, times(2)).getExperimentsForApps(appNameList);
        verify(experimentRepository, times(2)).getExperimentsMap(expIdList);
        verify(prioritiesRepository, times(2)).getPriorities(appNameList);
        verify(mutexRepository, times(2)).getExclusivesList(expIdList);
        verify(experimentRepository, times(2)).getBucketList(expIdList);
        verify(pagesRepository, times(2)).getExperimentsWithoutLabels(appPagePairList);
    }

    @Test
    public void happyPathTestForCacheDetails() {
        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForCacheDetails");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Application.Name appName1 = Application.Name.valueOf("Test-App-1");
        Set<Application.Name> appName1Set = singleEntrySet(appName1);
        Application.Name appName2 = Application.Name.valueOf("Test-App-2");
        Set<Application.Name> appName2Set = singleEntrySet(appName2);

        //Mock for first input and then make actual call to fill cache
        when(appResultMap.get(appName1)).thenReturn(appResultList);
        when(experimentRepository.getExperimentsForApps(appName1Set)).thenReturn(appResultMap);
        cache.getExperimentsByAppName(appName1);

        //Mock for second input and then make actual call to fill cache
        when(appResultMap.get(appName2)).thenReturn(appResultList);
        when(experimentRepository.getExperimentsForApps(appName2Set)).thenReturn(appResultMap);
        cache.getExperimentsByAppName(appName2);

        //Make actual call to get cache details
        Map<String, String> cacheDetails = cache.getDetails();

        //Verify that there are 2 entries in the APP_NAME_TO_EXPERIMENTS_CACHE cache
        String size = cacheDetails.get(AssignmentsMetadataCache.CACHE_NAME.APP_NAME_TO_EXPERIMENTS_CACHE + ".SIZE");
        assertThat(size, is("2"));

        //Verify that total size of cache details map is 7 = 1 for status & other 6 caches
        assertThat(cacheDetails.size(), is(7));

    }

    @Test
    public void happyPathTestForLastRefreshTime() {
        Configuration conf = ConfigurationFactory.parseConfiguration();
        conf.setName("happyPathTestForLastRefreshTime");
        AssignmentsMetadataCacheImpl cache = new AssignmentsMetadataCacheImpl(experimentRepository, prioritiesRepository,
                mutexRepository, pagesRepository, refreshCacheService, refreshIntervalInMinutes, metadataCacheRefreshTask,
                healthCheckRegistry, metadataCacheHealthCheck, CacheManager.newInstance(conf));

        //Mock the dependencies
        Date cTime = new Date();
        when(metadataCacheRefreshTask.getLastRefreshTime()).thenReturn(cTime);

        //Actual calls
        Date lastRefreshTime = cache.getLastRefreshTime();

        //Verify that cache returns the same time which is returned by the refresh task
        assertThat(cTime, is(lastRefreshTime));
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

    /**
     * Utility to create single entry List
     *
     * @param entry
     * @param <T>
     * @return
     */
    private <T> List<T> singleEntryList(T entry) {
        List aSet = new ArrayList<T>(1);
        aSet.add(entry);
        return aSet;
    }

}
