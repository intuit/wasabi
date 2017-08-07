package com.intuit.wasabi.assignment.countAggregation.impl;

import com.intuit.wasabi.repository.cassandra.impl.AssignmentStats;

import javax.inject.Inject;


public class AssignmentsHourlyAggregatorTask implements Runnable {

    private AssignmentStats assignmentStats;

    @Inject
    public AssignmentsHourlyAggregatorTask(AssignmentStats assignmentStats){
        this.assignmentStats = assignmentStats;
    }

    @Override
    public void run() {
        assignmentStats.writeCounts();
    }
}
