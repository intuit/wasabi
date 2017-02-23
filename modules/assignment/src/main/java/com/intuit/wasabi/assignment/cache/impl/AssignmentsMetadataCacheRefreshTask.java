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
import com.intuit.wasabi.assignment.cache.AssignmentMetadataCacheTimeService;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is used to refresh assignment metadata cache.
 */
public class AssignmentsMetadataCacheRefreshTask implements Runnable {
    private final Logger LOGGER = LoggerFactory.getLogger(AssignmentsMetadataCacheRefreshTask.class);

    private AssignmentsMetadataCache metadataCache;
    private AssignmentMetadataCacheTimeService timeService;

    private AtomicBoolean refreshInProgress;
    private Date lastRefreshTime;

    @Inject
    public AssignmentsMetadataCacheRefreshTask(AssignmentsMetadataCache metadataCache, AssignmentMetadataCacheTimeService timeService) {
        this.metadataCache = metadataCache;
        this.refreshInProgress = new AtomicBoolean(Boolean.FALSE);
        this.timeService = timeService;
        this.lastRefreshTime = timeService.getCurrentTime();
    }

    @Override
    public void run() {
        try {
            Date startTime = timeService.getCurrentTime();
            LOGGER.debug("AssignmentsMetadataCache refresh started at = {}", startTime);

            if (!refreshInProgress.get()) {
                //Mark that refresh has been started...
                refreshInProgress.set(Boolean.TRUE);

                //Refresh metadata cache
                metadataCache.refresh();

                //Update last refresh time
                lastRefreshTime = timeService.getCurrentTime();

                LOGGER.info("AssignmentsMetadataCache has been refreshed and took = {} ms", (lastRefreshTime.getTime() - startTime.getTime()));
            } else {
                LOGGER.info("AssignmentsMetadataCache refresh is skipped as previous refresh is in progress at = {}", timeService.getCurrentTime());
            }
        } catch (Exception e) {
            //In case of any exception, clear the cache and mark refresh complete.
            LOGGER.error("AssignmentsMetadataCache - Exception happened while refreshing cache...", e);
            metadataCache.clear();
        } finally {
            //Mark that refresh has been finished...
            refreshInProgress.set(Boolean.FALSE);
        }
    }

    public boolean isRefreshInProgress() {
        return refreshInProgress.get();
    }

    public Date getLastRefreshTime() {
        return lastRefreshTime;
    }

}
