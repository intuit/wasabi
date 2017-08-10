package com.intuit.wasabi.assignment.countAggregation.impl;

import com.intuit.wasabi.repository.cassandra.impl.AssignmentStats;
import com.intuit.wasabi.repository.cassandra.impl.CassandraAssignmentsRepository;

import javax.inject.Inject;


public class AssignmentsHourlyAggregatorTask implements Runnable {

    private AssignmentStats assignmentStats;

    @Inject
    public AssignmentsHourlyAggregatorTask(AssignmentStats assignmentStats){
        // TODO: Figure out how to pass the correct AssignmentStats instance from CassandraAssignmentsRepository
        this.assignmentStats = assignmentStats;
    }

    @Override
    public void run() {
        assignmentStats.writeCounts();
    }
}
