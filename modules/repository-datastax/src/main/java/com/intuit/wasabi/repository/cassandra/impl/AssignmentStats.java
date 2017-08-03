package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.cassandra.accessor.count.HourlyBucketCountAccessor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


class AssignmentStats {

    private static DateFormat hourFormatter = new SimpleDateFormat("HH");
    private static Map<Integer, Map<String, AtomicInteger>> hourlyCountMap;
    private static HourlyBucketCountAccessor hourlyBucketCountAccessor;
    private static final Bucket.Label NULL_LABEL = Bucket.Label.valueOf("NULL");
    private static final Object lock = new Object();


    // TODO: Delete system.out.println statements before I check in my code

    AssignmentStats() {
        hourlyCountMap = new ConcurrentHashMap<>();
        for (int hour = 0; hour <= 23; hour++){
            hourlyCountMap.put(hour, new ConcurrentHashMap<>());
        }
    }

    void incrementCount(Experiment experiment, Assignment assignment){
        int assignmentHour = getHour(assignment.getCreated());
        Map<String, AtomicInteger> hourMap = hourlyCountMap.get(assignmentHour);
        Experiment.ID id = experiment.getID();
        Bucket.Label bucketLabel = assignment.getBucketLabel();
        // Print statistics to confirm accuracy
        System.out.println("--- incrementCount():");
        System.out.println("assignment hour = " + assignmentHour);
        System.out.println("id = " + id);
        System.out.println("bucketLabel = " + bucketLabel);                         // operate on expBucket object
        AtomicInteger oldCount = hourMap.get(ExpBucket.getKey(id, bucketLabel)); // Equals method and hashcode method
        if (oldCount == null){
            synchronized (lock) {                                // First would be initialized to 1 twice w/o this
                oldCount = hourMap.get(ExpBucket.getKey(id, bucketLabel));
                if (oldCount == null){
                    AtomicInteger count = new AtomicInteger(1);
                    hourMap.put(ExpBucket.getKey(id, bucketLabel), count);
                } else {
                    oldCount.getAndIncrement();
                }
            }
        }else{
            oldCount.getAndIncrement();
        }
        System.out.println("hourMap(expBucket) = " + hourMap.get(ExpBucket.getKey(id, bucketLabel)));
    }

    int getCount(Experiment experiment, Bucket.Label bucketLabel, int assignmentHour){
        // Print statistics to confirm accuracy
        System.out.println("--- getCount():");
        System.out.println("assignmentHour = " + assignmentHour);
        System.out.println("id = " + experiment.getID());
        System.out.println("bucketLabel = " + bucketLabel);
        Map<String, AtomicInteger> hourMap = hourlyCountMap.get(assignmentHour);
        System.out.println("hourMap(expBucket) = " + hourMap.get(ExpBucket.getKey(experiment.getID(), bucketLabel)));
        return hourMap.get(ExpBucket.getKey(experiment.getID(), bucketLabel)).get();
    }

    void writeCounts(Experiment experiment, Assignment assignment){
        // TODO: Make write interval configurable instead of only hourly

        Optional<Bucket.Label> labelOptional = Optional.ofNullable(assignment.getBucketLabel());
        Date completedHour = getLastCompletedHour(System.currentTimeMillis());
        int assignmentHour = getHour(completedHour);

        for (int i = 0; i < hourlyCountMap.get(assignmentHour).size(); i++){
            hourlyBucketCountAccessor.incrementCountBy(experiment.getID().getRawID(),
                                      labelOptional.orElseGet(() -> NULL_LABEL).toString(), assignmentHour,
                                      getCount(experiment, assignment.getBucketLabel(), assignmentHour));
        }
        hourlyCountMap.put(assignmentHour, null);
        hourlyCountMap.put(assignmentHour, new ConcurrentHashMap<>());

//        hourlyBucketCountAccessor.decrementCountBy(experiment.getID().getRawID(),
//                assignment.getBucketLabel().toString(), eventTimeHour, 1);
    }


    Date getLastCompletedHour(long time) {
        return new Date(time - 3600 * 1000);
    }

    int getHour(Date completedHour) {
        return Integer.parseInt(hourFormatter.format(completedHour));   // Thread safe method
    }
}
