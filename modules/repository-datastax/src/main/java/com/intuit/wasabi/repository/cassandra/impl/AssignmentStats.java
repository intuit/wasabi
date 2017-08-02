package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.cassandra.accessor.count.HourlyBucketCountAccessor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class AssignmentStats {

//    private Experiment experiment;
//    private Assignment assignment;
//    private boolean countUp;
//    private HourlyBucketCountAccessor hourlyBucketCountAccessor;
    private static Map<Integer, Map<String, AtomicInteger>> hourlyCountMap;

    static {
        hourlyCountMap = new ConcurrentHashMap<>();
        for (int hour = 0; hour <= 23; hour++){
            hourlyCountMap.put(hour, new ConcurrentHashMap<>());
        }
    }

    public static void incrementCount(Experiment experiment, Assignment assignment){
        System.out.println("--- incrementCount():");
        int assignmentHour = getHour(assignment.getCreated());
        System.out.println("assignment hour = " + assignmentHour);
        Map<String, AtomicInteger> hourMap = hourlyCountMap.get(assignmentHour);
        Experiment.ID id = experiment.getID();
        System.out.println("id = " + id);
        Bucket.Label bucketLabel = assignment.getBucketLabel();
        System.out.println("bucketLabel = " + bucketLabel);
        // ExpBucket expBucket = new ExpBucket(id, bucketLabel);
        AtomicInteger oldCount = hourMap.get(ExpBucket.getKey(id, bucketLabel));
        if (oldCount == null){
            synchronized (hourMap) {                                // First would be initialized to 1 twice w/o this
                oldCount = hourMap.get(ExpBucket.getKey(id, bucketLabel));
                if (oldCount == null){                              // Double-checked locking
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

    public static int getCount(Experiment experiment, Bucket.Label bucketLabel, int assignmentHour){
        System.out.println("--- getCount():");
        System.out.println("assignmentHour = " + assignmentHour);
        System.out.println("experiment id = " + experiment.getID());
        System.out.println("bucketLabel = " + bucketLabel);
        // ExpBucket expBucket = new ExpBucket(experiment.getID(), bucketLabel);
        Map<String, AtomicInteger> hourMap = hourlyCountMap.get(assignmentHour);
        System.out.println("hourMap(expBucket) = " + hourMap.get(ExpBucket.getKey(experiment.getID(), bucketLabel)));
        AtomicInteger atomicInteger = hourMap.get(ExpBucket.getKey(experiment.getID(), bucketLabel));
        return atomicInteger.get();
    }

    public static void writeCounts(){
        // TODO: Figure out Bucket.Label --> toString OR labelOptional.orElseGet()
        // TODO: This is writing to the DB after every assignment, do this only once per hour instead (hour change var)
//              Date completedHour = getLastCompletedHour(System.currentTimeMillis());

//        int count = hourlyCountMap.get(eventTimeHour).get(expBucket).incrementAndGet();
//        hourlyBucketCountAccessor.incrementCountBy(experiment.getID().getRawID(),
//                labelOptional.orElseGet(() -> NULL_LABEL).toString(), eventTimeHour, count);
//        hourlyCountMap.put(eventTimeHour, null);            // Set the hour's data to null to delete unnecessary counts

//        hourlyBucketCountAccessor.decrementCountBy(experiment.getID().getRawID(),
//                assignment.getBucketLabel().toString(), eventTimeHour, 1);
    }


    public static Date getLastCompletedHour(long time) {
        return new Date(time - 3600 * 1000);
    }

    public static int getHour(Date completedHour) {
        DateFormat hourFormatter = new SimpleDateFormat("HH");
        return Integer.parseInt(hourFormatter.format(completedHour));
    }
}
