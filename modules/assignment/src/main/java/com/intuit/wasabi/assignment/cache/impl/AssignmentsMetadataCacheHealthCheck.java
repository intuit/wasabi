/*
 ****************************************************************************
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

package com.intuit.wasabi.assignment.cache.impl;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.assignment.cache.AssignmentMetadataCacheTimeService;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import org.slf4j.Logger;

import java.util.Date;

import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_ALLOWED_STALE_TIME;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_ENABLED;
import static com.intuit.wasabi.assignment.AssignmentsAnnotations.ASSIGNMENTS_METADATA_CACHE_REFRESH_INTERVAL;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Health check for assignment metadata cache.
 */
public class AssignmentsMetadataCacheHealthCheck extends HealthCheck {
    private static final Logger LOGGER = getLogger(AssignmentsMetadataCacheHealthCheck.class);

    private AssignmentsMetadataCache metadataCache;
    private Integer metadataCacheInterval;
    private Integer allowedStaleTime;
    private Boolean metadataCacheEnabled;
    private AssignmentMetadataCacheTimeService timeService;

    @Inject
    public AssignmentsMetadataCacheHealthCheck(AssignmentsMetadataCache metadataCache,
                                               AssignmentMetadataCacheTimeService timeService,
                                               @Named(ASSIGNMENTS_METADATA_CACHE_REFRESH_INTERVAL) Integer metadataCacheInterval,
                                               @Named(ASSIGNMENTS_METADATA_CACHE_ALLOWED_STALE_TIME) Integer allowedStaleTime,
                                               @Named(ASSIGNMENTS_METADATA_CACHE_ENABLED) Boolean metadataCacheEnabled) {
        super();
        this.metadataCache = metadataCache;
        this.metadataCacheInterval = metadataCacheInterval;
        this.allowedStaleTime = allowedStaleTime;
        this.metadataCacheEnabled = metadataCacheEnabled;
        this.timeService = timeService;
    }

    /**
     * @return Result of healthy or unhealthy based on last refresh time
     */
    @Override
    public HealthCheck.Result check() {
        boolean res;
        String msg;
        try {
            if (metadataCacheEnabled) {
                Date currentTime = timeService.getCurrentTime();
                Date lastRefreshTime = metadataCache.getLastRefreshTime();

                //Convert current time, last refresh time and metadata cache interval in milliseconds precision...
                long currentTimeMs = currentTime.getTime();
                long lastRefreshTimeMs = lastRefreshTime.getTime();
                long metadataCacheIntervalMs = metadataCacheInterval * 60 * 1000;

                //Calculate the maximum allowed time (in milliseconds) for cache to be not being refreshed.
                long maxAllowedDifferenceMS = allowedStaleTime * 60 * 1000;

                //Calculate time (in milliseconds) for 2 cache refresh intervals
                long warnDifferenceMS = 2 * metadataCacheIntervalMs;

                //Calculate actual time (in milliseconds) cache has not been refreshed.
                long diffMS = currentTimeMs - lastRefreshTimeMs;

                if (diffMS > maxAllowedDifferenceMS) {
                    //If cache has NOT been refreshed since maximum allowed stale time so fail the health check

                    res = false;
                    msg = new StringBuffer("AssignmentsMetadataCache hasn't been refreshed since last ").append(allowedStaleTime).append(" minutes. ")
                            .append("Defined interval is of ").append(metadataCacheInterval).append(" minutes. ")
                            .append("Last refresh time was ").append(lastRefreshTime)
                            .append(" and current time is ").append(currentTime).append(".")
                            .toString();
                    LOGGER.error(msg);

                } else if (diffMS > warnDifferenceMS) {
                    //If cache has NOT been refreshed since last 2 intervals then log an error.

                    String warnMsg = new StringBuffer("AssignmentsMetadataCache hasn't been refreshed since, at least, last two intervals...")
                            .append("Defined interval is of ").append(metadataCacheInterval).append(" minutes. ")
                            .append("Last refresh time was ").append(lastRefreshTime)
                            .append(" and current time is ").append(currentTime).append(".")
                            .toString();

                    res = true;
                    msg = warnMsg;
                    LOGGER.warn(warnMsg);

                } else {
                    //All good, cache had been refreshed in last interval.

                    msg = new StringBuffer("AssignmentsMetadataCache refresh was successful. ")
                            .append("Defined interval is of ").append(metadataCacheInterval).append(" minutes. ")
                            .append("Last refresh time was ").append(lastRefreshTime)
                            .append(" and current time is ").append(currentTime).append(".")
                            .toString();
                    res = true;
                }

            } else {
                res = true;
                msg = "AssignmentsMetadataCache is NOT enabled...";
            }

        } catch (Exception ex) {
            LOGGER.error("Some error happened while checking the health of AssignmentsMetadataCache", ex);
            res = false;
            msg = ex.getMessage();
        }

        return res ? HealthCheck.Result.healthy(msg) : HealthCheck.Result.unhealthy(msg);
    }
}
