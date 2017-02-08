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

package com.intuit.wasabi.assignment.cache.impl;

import com.google.inject.Inject;
import com.intuit.wasabi.assignment.cache.AssignmentMetadataCacheTimeService;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class AssignmentsMetadataCacheRefreshTask implements Runnable {
    private final Logger LOGGER = LoggerFactory.getLogger(AssignmentsMetadataCacheRefreshTask.class);

    private AssignmentsMetadataCache metadataCache;
    private AssignmentMetadataCacheTimeService timeService;

    private Boolean refreshInProgress;
    private Date lastRefreshTime;

    @Inject
    public AssignmentsMetadataCacheRefreshTask(AssignmentsMetadataCache metadataCache, AssignmentMetadataCacheTimeService timeService) {
        this.metadataCache = metadataCache;
        this.refreshInProgress = Boolean.FALSE;
        this.timeService = timeService;
        this.lastRefreshTime = timeService.getCurrentTime();
    }

    @Override
    public void run() {
        try {
            LOGGER.info("AssignmentsMetadataCache refresh started at = {}", timeService.getCurrentTime());

            if(!refreshInProgress) {
                //Mark that refresh has been started...
                refreshInProgress=Boolean.TRUE;

                //Refresh metadata cache
                metadataCache.refresh();

                //Update last refresh time
                lastRefreshTime = timeService.getCurrentTime();

                LOGGER.info("AssignmentsMetadataCache has been refreshed at = {}", lastRefreshTime);
            } else {
                LOGGER.info("AssignmentsMetadataCache refresh is skipped as previous refresh is in progress at = {}", timeService.getCurrentTime());
            }
        } catch (Exception e) {
            //In case of any exception, clear the cache and mark refresh complete.
            LOGGER.error("Exception happened while refreshing AssignmentsMetadataCache...", e);
            metadataCache.clear();
        } finally {
            //Mark that refresh has been finished...
            refreshInProgress = Boolean.FALSE;
        }
    }

    public boolean isRefreshInProgress() {
        return refreshInProgress;
    }

    public Date getLastRefreshTime() {
        return lastRefreshTime;
    }

}
