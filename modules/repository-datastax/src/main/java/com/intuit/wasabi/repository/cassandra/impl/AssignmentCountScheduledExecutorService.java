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

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class AssignmentCountScheduledExecutorService extends ScheduledThreadPoolExecutor implements Runnable {
    private AssignmentStats assignmentStats;

    @Inject
    public AssignmentCountScheduledExecutorService(@Named("export.pool.size") int corePoolSize, AssignmentStats assignmentStats) {
        super(corePoolSize, new ThreadFactoryBuilder().setNameFormat("AssignmentCountScheduledExecutorService-%d").setDaemon(true).build());
        this.assignmentStats = assignmentStats;
    }

    @Override
    public void run() {
        assignmentStats.writeCounts();
    }
}