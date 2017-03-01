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

package com.intuit.wasabi.assignment.cache;

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Local cache created and used during user assignment flow.
 */

public interface AssignmentsMetadataCache {
    enum CACHE_NAME {
        APP_NAME_TO_EXPERIMENTS_CACHE,
        EXPERIMENT_ID_TO_EXPERIMENT_CACHE,
        APP_NAME_TO_PRIORITIZED_EXPERIMENTS_CACHE,
        EXPERIMENT_ID_TO_EXCLUSION_CACHE,
        EXPERIMENT_ID_TO_BUCKET_CACHE,
        APP_NAME_N_PAGE_TO_EXPERIMENTS_CACHE
    }

    /**
     * This method refresh the existing cache (keys) with the updated data from Database.
     * <p>
     * This method doesn't add new keys into the cache.
     *
     * @return TRUE if cache is successfully refreshed or FALSE.
     */
    boolean refresh();

    /**
     * @param appName
     * @return List of experiments created in the given application.
     */
    List<Experiment> getExperimentsByAppName(Application.Name appName);


    /**
     * @param expId
     * @return An experiment for given experiment id.
     */
    Optional<Experiment> getExperimentById(Experiment.ID expId);


    /**
     * @param appName
     * @return prioritized list of experiments for given application.
     */
    Optional<PrioritizedExperimentList> getPrioritizedExperimentListMap(Application.Name appName);

    /**
     * @param expId
     * @return List of experiments which are mutually exclusive to the given experiment.
     */
    List<Experiment.ID> getExclusionList(Experiment.ID expId);

    /**
     * @param expId
     * @return BucketList for given experiment.
     */
    BucketList getBucketList(Experiment.ID expId);

    /**
     * @param appName
     * @param pageName
     * @return List experiments associated to the given application and page.
     */
    List<PageExperiment> getPageExperiments(Application.Name appName, Page.Name pageName);

    /**
     * This method is used to clear cache.
     *
     * @return TRUE if cache is cleared successfully else FALSE.
     */
    boolean clear();

    /**
     * This methods returns when was cache refreshed last time.
     *
     * @return
     */
    Date getLastRefreshTime();

    /**
     * @return metadata cache details..
     */
    Map<String, String> getDetails();
}
