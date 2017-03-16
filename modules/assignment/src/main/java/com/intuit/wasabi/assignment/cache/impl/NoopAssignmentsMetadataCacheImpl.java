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

import com.google.inject.Inject;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Local cache created and used during user assignment flow.
 */

public class NoopAssignmentsMetadataCacheImpl implements AssignmentsMetadataCache {
    private final Logger LOGGER = LoggerFactory.getLogger(NoopAssignmentsMetadataCacheImpl.class);

    @Inject
    public NoopAssignmentsMetadataCacheImpl() {
        LOGGER.info("Noop Assignments metadata cache has been created successfully...");
    }

    /**
     * This method is used to clear cache.
     */
    @Override
    public boolean clear() {
        // NOOP
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
        // NOOP
        return Boolean.FALSE;
    }

    /**
     * @param appName
     * @return List of experiments created in the given application.
     */
    @Override
    public List<Experiment> getExperimentsByAppName(Application.Name appName) {
        // NOOP
        return null;
    }

    /**
     * @param expId
     * @return An experiment for given experiment id.
     */
    @Override
    public Optional<Experiment> getExperimentById(Experiment.ID expId) {
        // NOOP
        return Optional.empty();
    }


    /**
     * @param appName
     * @return prioritized list of experiments for given application.
     */
    @Override
    public Optional<PrioritizedExperimentList> getPrioritizedExperimentListMap(Application.Name appName) {
        // NOOP
        return Optional.empty();
    }

    /**
     * @param expId
     * @return List of experiments which are mutually exclusive to the given experiment.
     */
    @Override
    public List<Experiment.ID> getExclusionList(Experiment.ID expId) {
        // NOOP
        return null;
    }

    /**
     * @param expId
     * @return BucketList for given experiment.
     */
    @Override
    public BucketList getBucketList(Experiment.ID expId) {
        // NOOP
        return null;
    }

    /**
     * @param appName
     * @param pageName
     * @return List experiments associated to the given application and page.
     */
    @Override
    public List<PageExperiment> getPageExperiments(Application.Name appName, Page.Name pageName) {
        // NOOP
        return null;
    }

    /**
     * @return Last cache refresh time.
     */
    @Override
    public Date getLastRefreshTime() {
        // NOOP
        return null;

    }

    /**
     * @return Get metadata cache details
     */
    @Override
    public Map<String, String> getDetails() {
        // NOOP
        return null;
    }
}



