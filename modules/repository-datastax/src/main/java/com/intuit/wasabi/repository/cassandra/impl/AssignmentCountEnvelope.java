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
package com.intuit.wasabi.repository.cassandra.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.slf4j.Logger;

import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Asynchronous thread to update bucket assignment counts.
 * Also, changes the state of rapid experiments to PAUSED after a set userCap.
 */
public class AssignmentCountEnvelope implements Runnable {

    private static final Logger LOGGER = getLogger(AssignmentCountEnvelope.class);
    private final EventLog eventLog;
    private AssignmentsRepository assignmentsRepository;
    private ExperimentRepository cassandraExperimentRepository;
    private ExperimentRepository dbExperimentRepository;
    private Experiment experiment;
    private Assignment assignment;
    private boolean countUp;
    private Date date;
    private boolean assignUserToExport;
    private boolean assignBucketCount;

    /**
     * Constructor
     *
     * @param assignmentsRepository         assignment repository
     * @param cassandraExperimentRepository cassandra experiment repository
     * @param dbExperimentRepository        database experiment repository
     * @param experiment                    experiment object
     * @param assignment                    assignment object
     * @param countUp                       boolean value of countup
     * @param eventLog                      event log object
     * @param date                          date
     * @param assignUserToExport            assignUserToExport
     * @param assignBucketCount             assignBucketCount
     */
    @Inject
    public AssignmentCountEnvelope(AssignmentsRepository assignmentsRepository,
                                   ExperimentRepository cassandraExperimentRepository,
                                   ExperimentRepository dbExperimentRepository,
                                   Experiment experiment, Assignment assignment, boolean countUp, EventLog eventLog,
                                   Date date, final @Named("assign.user.to.export") Boolean assignUserToExport,
                                   final @Named("assign.bucket.count") Boolean assignBucketCount) {
        super();

        this.assignmentsRepository = assignmentsRepository;
        this.cassandraExperimentRepository = cassandraExperimentRepository;
        this.dbExperimentRepository = dbExperimentRepository;
        this.experiment = experiment;
        this.assignment = assignment;
        this.countUp = countUp;
        this.eventLog = eventLog;
        this.date = date;
        this.assignUserToExport = assignUserToExport;
        this.assignBucketCount = assignBucketCount;
    }

    @Override
    public void run() {
        try {
            // Updates the bucket assignment counts
            if (assignBucketCount) {
                assignmentsRepository.updateBucketAssignmentCount(experiment, assignment, countUp);
            }
        } catch (Exception e) {
            LOGGER.error("Error updating the assignment counts for experiment: ", experiment.getID() +
                    " bucket label: " + assignment.getBucketLabel() + " with exception: ", e);
        }

        //The exports table will be written only for not deleted assignments
        try {
            // adds an assignment to the user export table
            if (countUp && assignUserToExport) {
                assignmentsRepository.assignUserToExports(assignment, date);
            }
        } catch (Exception e) {
            LOGGER.error("Error updating the assignment to the user export for experiment: ", experiment.getID() +
                    " bucket label: " + assignment.getBucketLabel() + " with exception: ", e);
        }
    }
}
