/*******************************************************************************
 * Copyright 2017 Intuit
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

package com.intuit.wasabi.assignment.cache.impl;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
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
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_HEALTH_CHECK;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_REFRESH_CACHE_SERVICE;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_REFRESH_INTERVAL;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_REFRESH_TASK;
import static com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache.CACHE_NAME.APP_NAME_N_PAGE_TO_EXPERIMENTS_CACHE;
import static com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache.CACHE_NAME.APP_NAME_TO_EXPERIMENTS_CACHE;
import static com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache.CACHE_NAME.APP_NAME_TO_PRIORITIZED_EXPERIMENTS_CACHE;
import static com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache.CACHE_NAME.EXPERIMENT_ID_TO_BUCKET_CACHE;
import static com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache.CACHE_NAME.EXPERIMENT_ID_TO_EXCLUSION_CACHE;
import static com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache.CACHE_NAME.EXPERIMENT_ID_TO_EXPERIMENT_CACHE;
import static java.util.Objects.isNull;

/**
 * Local cache created and used during user assignment flow.
 */

public class AssignmentsMetadataCacheImpl implements AssignmentsMetadataCache {
    private final Logger LOGGER = LoggerFactory.getLogger(AssignmentsMetadataCacheImpl.class);

    private ExperimentRepository experimentRepository = null;
    private PrioritiesRepository prioritiesRepository = null;
    private MutexRepository mutexRepository = null;
    private PagesRepository pagesRepository = null;
    private AssignmentsMetadataCacheRefreshTask metadataCacheRefreshTask;
    private CacheManager cacheManager;

    private static final String ASSIGNMENT_METADATA_CACHE_SERVICE_NAME = "AssignmentsMetadataCache";

    @Inject
    public AssignmentsMetadataCacheImpl(@CassandraRepository ExperimentRepository experimentRepository,
                                        PrioritiesRepository prioritiesRepository,
                                        MutexRepository mutexRepository,
                                        PagesRepository pagesRepository,
                                        @Named(ASSIGNMENTS_METADATA_CACHE_REFRESH_CACHE_SERVICE)
                                                ScheduledExecutorService refreshCacheService,
                                        @Named(ASSIGNMENTS_METADATA_CACHE_REFRESH_INTERVAL) Integer refreshIntervalInMinutes,
                                        @Named(ASSIGNMENTS_METADATA_CACHE_REFRESH_TASK) Runnable metadataCacheRefreshTask,
                                        HealthCheckRegistry healthCheckRegistry,
                                        @Named(ASSIGNMENTS_METADATA_CACHE_HEALTH_CHECK) HealthCheck metadataCacheHealthCheck,
                                        CacheManager cacheManager) {

        this.experimentRepository = experimentRepository;
        this.prioritiesRepository = prioritiesRepository;
        this.mutexRepository = mutexRepository;
        this.pagesRepository = pagesRepository;
        this.metadataCacheRefreshTask = (AssignmentsMetadataCacheRefreshTask) metadataCacheRefreshTask;
        this.cacheManager = cacheManager;

        //Schedule metadata cache refresh task
        refreshCacheService.scheduleAtFixedRate(metadataCacheRefreshTask, refreshIntervalInMinutes, refreshIntervalInMinutes, TimeUnit.MINUTES);

        //Register health check
        healthCheckRegistry.register(ASSIGNMENT_METADATA_CACHE_SERVICE_NAME, metadataCacheHealthCheck);

        //Create new caches
        for (CACHE_NAME name : CACHE_NAME.values()) {
            cacheManager.addCache(name.toString());
        }

        LOGGER.info("Assignments metadata cache has been created successfully...");
    }

    /**
     * This method is used to clear cache.
     */
    @Override
    public boolean clear() {
        try {
            for (CACHE_NAME name : CACHE_NAME.values()) {
                cacheManager.getCache(name.toString()).removeAll();
            }
            LOGGER.info("Assignments metadata cache has been cleared successfully...");
            return Boolean.TRUE;
        } catch (Exception e) {
            LOGGER.error("Exception occurred while clearing a cache...", e);
        }
        return Boolean.FALSE;
    }

    /**
     * This method refresh the existing cache (keys) with the updated data from Database.
     * <p>
     * This method doesn't add new keys into the cache.
     *
     * @return TRUE if cache is successfully refreshed else FALSE.
     */
    @Override
    public boolean refresh() {
        //------------------------------------------------------------------------------------------------
        //Get updated data from database for each caches
        //------------------------------------------------------------------------------------------------
        List<Element> appToExperiments = makeElementsList(experimentRepository.getExperimentsForApps(keys(APP_NAME_TO_EXPERIMENTS_CACHE)));
        List<Element> expIdsToExperiments = makeElementsList(experimentRepository.getExperimentsMap(keys(EXPERIMENT_ID_TO_EXPERIMENT_CACHE)));
        List<Element> appToPrioritizedExperiments = makeElementsList(prioritiesRepository.getPriorities(keys(APP_NAME_TO_PRIORITIZED_EXPERIMENTS_CACHE)));
        List<Element> expIdsToExclusions = makeElementsList(mutexRepository.getExclusivesList(keys(EXPERIMENT_ID_TO_EXCLUSION_CACHE)));
        List<Element> expIdsToBuckets = makeElementsList(experimentRepository.getBucketList(keys(EXPERIMENT_ID_TO_BUCKET_CACHE)));
        List<Element> appNPageToExperiments = makeElementsList(pagesRepository.getExperimentsWithoutLabels(keys(APP_NAME_N_PAGE_TO_EXPERIMENTS_CACHE)));

        //------------------------------------------------------------------------------------------------
        //Now update individual caches
        //------------------------------------------------------------------------------------------------
        cacheManager.getCache(APP_NAME_TO_EXPERIMENTS_CACHE.toString()).putAll(appToExperiments);
        cacheManager.getCache(EXPERIMENT_ID_TO_EXPERIMENT_CACHE.toString()).putAll(expIdsToExperiments);
        cacheManager.getCache(APP_NAME_TO_PRIORITIZED_EXPERIMENTS_CACHE.toString()).putAll(appToPrioritizedExperiments);
        cacheManager.getCache(EXPERIMENT_ID_TO_EXCLUSION_CACHE.toString()).putAll(expIdsToExclusions);
        cacheManager.getCache(EXPERIMENT_ID_TO_BUCKET_CACHE.toString()).putAll(expIdsToBuckets);
        cacheManager.getCache(APP_NAME_N_PAGE_TO_EXPERIMENTS_CACHE.toString()).putAll(appNPageToExperiments);

        LOGGER.debug("Assignments metadata cache has been refreshed successfully...");
        return Boolean.TRUE;
    }

    /**
     * @param appName
     * @return List of experiments created in the given application.
     */
    @Override
    public List<Experiment> getExperimentsByAppName(Application.Name appName) {
        List<Experiment> expList = null;
        Cache cache = cacheManager.getCache(APP_NAME_TO_EXPERIMENTS_CACHE.toString());
        Element val = cache.get(appName);
        if (isNull(val)) {
            expList = experimentRepository.getExperimentsForApps(singleEntrySet(appName)).get(appName);
            cache.put(new Element(appName, expList));
        } else {
            expList = (List<Experiment>) val.getObjectValue();
        }

        return isNull(expList) ? new ArrayList<>() : expList;
    }

    /**
     * @param expId
     * @return An experiment for given experiment id.
     */
    @Override
    public Optional<Experiment> getExperimentById(Experiment.ID expId) {
        Experiment exp = null;
        Cache cache = cacheManager.getCache(EXPERIMENT_ID_TO_EXPERIMENT_CACHE.toString());
        Element val = cache.get(expId);
        if (isNull(val)) {
            exp = experimentRepository.getExperimentsMap(singleEntrySet(expId)).get(expId);
            cache.put(new Element(expId, exp));
        } else {
            exp = (Experiment) val.getObjectValue();
        }

        return Optional.ofNullable(exp);
    }


    /**
     * @param appName
     * @return prioritized list of experiments for given application.
     */
    @Override
    public Optional<PrioritizedExperimentList> getPrioritizedExperimentListMap(Application.Name appName) {
        PrioritizedExperimentList expList = null;
        Cache cache = cacheManager.getCache(APP_NAME_TO_PRIORITIZED_EXPERIMENTS_CACHE.toString());
        Element val = cache.get(appName);
        if (isNull(val)) {
            expList = prioritiesRepository.getPriorities(singleEntrySet(appName)).get(appName);
            cache.put(new Element(appName, expList));
        } else {
            expList = (PrioritizedExperimentList) val.getObjectValue();
        }

        return Optional.ofNullable(expList);
    }

    /**
     * @param expId
     * @return List of experiments which are mutually exclusive to the given experiment.
     */
    @Override
    public List<Experiment.ID> getExclusionList(Experiment.ID expId) {
        List<Experiment.ID> exclusionList = null;
        Cache cache = cacheManager.getCache(EXPERIMENT_ID_TO_EXCLUSION_CACHE.toString());
        Element val = cache.get(expId);
        if (isNull(val)) {
            exclusionList = mutexRepository.getExclusivesList(singleEntrySet(expId)).get(expId);
            cache.put(new Element(expId, exclusionList));
        } else {
            exclusionList = (List<Experiment.ID>) val.getObjectValue();
        }

        return isNull(exclusionList) ? new ArrayList<>() : exclusionList;
    }

    /**
     * @param expId
     * @return BucketList for given experiment.
     */
    @Override
    public BucketList getBucketList(Experiment.ID expId) {
        BucketList bucketList = null;
        Cache cache = cacheManager.getCache(EXPERIMENT_ID_TO_BUCKET_CACHE.toString());
        Element val = cache.get(expId);
        if (isNull(val)) {
            bucketList = experimentRepository.getBucketList(singleEntrySet(expId)).get(expId);
            cache.put(new Element(expId, bucketList));
        } else {
            bucketList = (BucketList) val.getObjectValue();
        }

        return isNull(bucketList) ? new BucketList() : makeCopy(bucketList);
    }

    /**
     * @param appName
     * @param pageName
     * @return List experiments associated to the given application and page.
     */
    @Override
    public List<PageExperiment> getPageExperiments(Application.Name appName, Page.Name pageName) {
        Pair<Application.Name, Page.Name> appPagePair = Pair.of(appName, pageName);
        List<PageExperiment> pageExperiments = null;
        Cache cache = cacheManager.getCache(APP_NAME_N_PAGE_TO_EXPERIMENTS_CACHE.toString());
        Element val = cache.get(appPagePair);
        if (isNull(val)) {
            pageExperiments = pagesRepository.getExperimentsWithoutLabels(singleEntrySet(appPagePair)).get(appPagePair);
            cache.put(new Element(appPagePair, pageExperiments));
        } else {
            pageExperiments = (List<PageExperiment>) val.getObjectValue();
        }

        return isNull(pageExperiments) ? new ArrayList<>() : pageExperiments;
    }

    /**
     * @return Last cache refresh time.
     */
    @Override
    public Date getLastRefreshTime() {
        return metadataCacheRefreshTask.getLastRefreshTime();
    }

    /**
     * @return Get metadata cache details
     */
    @Override
    public Map<String, String> getDetails() {
        Map<String, String> details = new HashMap<>();
        details.put("status", "Enabled");
        for (CACHE_NAME name : CACHE_NAME.values()) {
            details.put(name + ".SIZE", String.valueOf(cacheManager.getCache(name.toString()).getSize()));
        }
        return details;
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
     * Make elements list which is specific to the ehcache format..
     *
     * @param elementsMap
     * @param <K>
     * @param <V>
     * @return Elements list
     */
    private <K, V> List<Element> makeElementsList(Map<K, V> elementsMap) {
        List<Element> elementsList = new ArrayList<>();
        elementsMap.forEach((key, value) -> {
            elementsList.add(new Element(key, value));
        });
        return elementsList;
    }

    /**
     * Get the cache keys
     *
     * @param cacheName
     * @return cache keys
     */
    private Collection keys(CACHE_NAME cacheName) {
        return cacheManager.getCache(cacheName.name()).getKeys();
    }

    /**
     * Creating a separate copy here to avoid ConcurrentModificationException as BucketList gets modified
     * during assignment business logic execution.
     *
     * @param bucketList
     * @return New object/deep copy of BucketList.
     */
    private BucketList makeCopy(BucketList bucketList) {
        List<Bucket> iBucketList = new ArrayList<>(bucketList.getBuckets());
        BucketList newBucketList = new BucketList();
        newBucketList.setBuckets(iBucketList);
        return newBucketList;
    }
}



