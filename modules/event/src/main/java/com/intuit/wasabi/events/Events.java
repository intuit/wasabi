/*******************************************************************************
 * Copyright 2016 Intuit
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
package com.intuit.wasabi.events;

import com.intuit.wasabi.analyticsobjects.EventList;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application.Name;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.Map;
import java.util.Set;

/**
 * Represents events recording methods
 */
public interface Events {

    /**
     * Holds the length of events queues stored in MySQL and ingestion executors.
     *
     * @return Map of number of elements in each event queue
     */
    public Map<String, Integer> queuesLength();


    /**
     * Records the given events list for application name, experiment label, user ID and context set
     *
     * @param appName         Application name
     * @param experimentLabel Experiment label
     * @param userID          User ID
     * @param events          Events list
     * @param contextSet      Context set
     */
    void recordEvents(Name appName, Experiment.Label experimentLabel, User.ID userID, EventList events,
                      Set<Context> contextSet);

    /**
     * Shutdown the queue, draining the remainder of the queue into the
     * database before returning
     */
    void shutdown();
}
