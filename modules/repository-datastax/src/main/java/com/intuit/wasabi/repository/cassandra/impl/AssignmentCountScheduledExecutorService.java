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
package com.intuit.wasabi.repository.cassandra.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.repository.cassandra.AssignmentHourlyCountTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class AssignmentCountScheduledExecutorService extends ScheduledThreadPoolExecutor implements Runnable {
    private AssignmentStats assignmentStats;
    private AssignmentHourlyCountTimeService timeService;
    private final Logger LOGGER = LoggerFactory.getLogger(CassandraAssignmentsRepository.class);

    @Inject
    public AssignmentCountScheduledExecutorService(@Named("export.pool.size") int corePoolSize,
                                                   AssignmentStats assignmentStats,
                                                   AssignmentHourlyCountTimeService timeService) {
        super(corePoolSize, new ThreadFactoryBuilder().setNameFormat("AssignmentCountScheduledExecutorService-%d").setDaemon(true).build());
        this.assignmentStats = assignmentStats;
        this.timeService = timeService;
    }

    @Override
    public void run() {
        try{
            Date startTime = timeService.getCurrentTime();
            LOGGER.debug("Writing hourly counts to database, started at = {}", startTime);
            assignmentStats.writeCounts();
            Date endTime = timeService.getCurrentTime();
            LOGGER.info("Finished writing hourly counts to database and took = {} ms", (endTime.getTime() - startTime.getTime()));
        } catch(Exception e){
            LOGGER.error("Exception happened: ", e);
        }

    }
}