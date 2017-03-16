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
package com.intuit.wasabi.events.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.EventList;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.eventobjects.EventEnvelopePayload;
import com.intuit.wasabi.events.EventIngestionExecutor;
import com.intuit.wasabi.events.Events;
import com.intuit.wasabi.events.EventsMBean;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static com.intuit.wasabi.events.EventsAnnotations.EXECUTOR_THREADPOOL_SIZE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class <b>asychronously</b> posts events to the events database (mysql in this implementation).
 * <br></br>
 * <b>Note: Since the post to the events database is asynchronous, there is a chance of loosing events if the process is shut down abruptly while the event queue is not empty.</b>
 * <br></br>
 * <STRONG>WARNING: This class may loose events data</STRONG>
 */
public class EventsImpl implements Events, EventsMBean {

    private static final Logger LOGGER = getLogger(EventsImpl.class);
    protected static final String MYSQL = "mysql";
    private Assignments assignments;
    private TransactionFactory transactionFactory;
    private LinkedBlockingQueue mysqlQueue = new LinkedBlockingQueue<>();
    private ThreadPoolExecutor mysqlExecutor;
    /**
     * Executors to ingest event data to real time ingestion system.
     */
    protected Map<String, EventIngestionExecutor> eventIngestionExecutors;

    @Inject
    public EventsImpl(Map<String, EventIngestionExecutor> eventIngestionExecutors,
                      final @Named(EXECUTOR_THREADPOOL_SIZE) Integer threadPoolSize,
                      final Assignments assignments,
                      final TransactionFactory transactionFactory) {
        super();
        this.eventIngestionExecutors = eventIngestionExecutors;
        this.transactionFactory = transactionFactory;
        this.assignments = assignments;
        mysqlExecutor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize,
                0L, MILLISECONDS, mysqlQueue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEvents(Application.Name applicationName,
                             Experiment.Label experimentLabel, User.ID userID, EventList events, Set<Context> contextSet) {
        Map<Context, Assignment> assignmentHashMap = getAssignments(userID, applicationName, experimentLabel, contextSet);

        for (Event event : events.getEvents()) {
            Assignment assignment = assignmentHashMap.get(event.getContext());
            if (assignment != null) {
                postEventToMysql(assignment, event);
                ingestEventToRealTimeSystems(applicationName, experimentLabel, event, assignment);
            }
        }
    }

    // This method ingests event to real time ingestion systems.
    private void ingestEventToRealTimeSystems(Application.Name applicationName, Experiment.Label experimentLabel, Event event,
                                              Assignment assignment) {
        for (String name : eventIngestionExecutors.keySet()) {
            eventIngestionExecutors.get(name).execute(new EventEnvelopePayload(applicationName, experimentLabel, assignment, event));
        }
    }

    protected Map<Context, Assignment> getAssignments(User.ID userID, Application.Name applicationName,
                                                      Experiment.Label experimentLabel, Set<Context> contextSet) {
        Map<Context, Assignment> assignmentHashMap = new HashMap<>();
        for (Context context : contextSet) {
            Assignment assignment = assignments.getExistingAssignment(userID, applicationName, experimentLabel, context);
            assignmentHashMap.put(context, assignment);
        }
        return assignmentHashMap;
    }

    private void postEventToMysql(Assignment assignment, Event event) {
        try {
            mysqlExecutor.execute(makeEventEnvelope(assignment, event));
        } catch (Exception e) {
            LOGGER.warn("Mysql error: Unable to record event " + event.toString() + " for the user "
                    + assignment.getUserID().toString() + " for context " + assignment.getContext(), e);
        }
    }

    /**
     * Helper method to instantiate events envelope
     *
     * @param assignment
     * @param event
     * @return
     */
    protected EventsEnvelope makeEventEnvelope(Assignment assignment, Event event) {
        return new EventsEnvelope(assignment, event, transactionFactory.newTransaction());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        mysqlExecutor.shutdown();
    }

    @Override
    public Map<String, Integer> queuesLength() {
        Map<String, Integer> queueLengthMap = new HashMap<String, Integer>();
        queueLengthMap.put(MYSQL, new Integer(mysqlQueue.size()));
        for (String name : eventIngestionExecutors.keySet()) {
            queueLengthMap.put(name.toLowerCase(), new Integer(eventIngestionExecutors.get(name).queueLength()));
        }
        return queueLengthMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getQueueSize() {
        // FIXME: is this MBean method really used??
        return mysqlQueue.size();
    }
}
