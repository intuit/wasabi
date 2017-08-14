package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.cassandra.accessor.count.HourlyBucketCountAccessor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intuit.wasabi.repository.cassandra.impl.CassandraAssignmentsRepository.NULL_LABEL;


public class AssignmentStats {

    private static HourlyBucketCountAccessor hourlyBucketCountAccessor;

    private static Map<Integer, Map<String, AtomicInteger>> hourlyCountMap;
    private static final Object lock = new Object();


    /**
     * Constructor
     */
    public AssignmentStats() {
        hourlyCountMap = new ConcurrentHashMap<>();
        for (int hour = 0; hour <= 23; hour++) {
            hourlyCountMap.put(hour, new ConcurrentHashMap<>());
        }
    }


    /**
     * Increments the count of a particular bucket for a specific hour
     *
     * @param experiment
     * @param assignment
     */
    public void incrementCount(Experiment experiment, Assignment assignment) {
        int assignmentHour = AssignmentStatsUtil.getHour(assignment.getCreated());
        Map<String, AtomicInteger> hourMap = hourlyCountMap.get(assignmentHour);
        Experiment.ID id = experiment.getID();
        Optional<Bucket.Label> labelOptional = Optional.ofNullable(assignment.getBucketLabel());
        Bucket.Label bucketLabel = labelOptional.orElseGet(() -> NULL_LABEL);
        String key = new ExpBucket(id, bucketLabel).getKey();
        AtomicInteger oldCount = hourMap.get(key);
        if (oldCount == null) {
            synchronized (lock) {                         // oldCount would be initialized to 1 twice w/o this
                oldCount = hourMap.get(key);
                if (oldCount == null) {                   // double-checked locking
                    AtomicInteger count = new AtomicInteger(1);
                    hourMap.put(key, count);
                } else {
                    oldCount.getAndIncrement();
                }
            }
        } else {
            oldCount.getAndIncrement();
        }
    }

    /**
     * Returns the number of assignments for an experiment and bucket during a specified hour
     *
     * @param experiment
     * @param bucketLabel
     * @param assignmentHour
     * @return int representing the counts for a given bucket during the given hour
     */
    public int getCount(Experiment experiment, Bucket.Label bucketLabel, int assignmentHour) {
        Map<String, AtomicInteger> hourMap = hourlyCountMap.get(assignmentHour);
        return hourMap.get(new ExpBucket(experiment.getID(), bucketLabel).getKey()).get();
    }

    /**
     * Writes hourly assignment counts to cassandra for the last completed hour based on current time
     */
    public void writeCounts() {
        Date completedHour = AssignmentStatsUtil.getLastCompletedHour(System.currentTimeMillis());
        int assignmentHour = AssignmentStatsUtil.getHour(completedHour);
        String day = AssignmentStatsUtil.getDayString(completedHour);
        
        Iterator it = hourlyCountMap.get(assignmentHour).entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            UUID experimentID = AssignmentStatsUtil.getExpUUID(pair);
            String bucketLabel = AssignmentStatsUtil.getBucketLabel(pair);
            int count = (int)pair.getValue();
            for (int i = 0; i < count; i++){
                hourlyBucketCountAccessor.incrementCountBy(experimentID, day, bucketLabel, assignmentHour);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        hourlyCountMap.put(assignmentHour, new ConcurrentHashMap<>());
    }
}
