package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.cassandra.accessor.count.HourlyBucketCountAccessor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class AssignmentStats {

    private static DateFormat hourFormatter = new SimpleDateFormat("HH");
    private static Map<Integer, Map<String, AtomicInteger>> hourlyCountMap;
    private static HourlyBucketCountAccessor hourlyBucketCountAccessor;
    private static final Object lock = new Object();
    private static final int UUID_LENGTH = 36;

    AssignmentStats() {
        hourlyCountMap = new ConcurrentHashMap<>();
        for (int hour = 0; hour <= 23; hour++) {
            hourlyCountMap.put(hour, new ConcurrentHashMap<>());
        }
    }

    public void incrementCount(Experiment experiment, Assignment assignment) {
        int assignmentHour = getHour(assignment.getCreated());
        Map<String, AtomicInteger> hourMap = hourlyCountMap.get(assignmentHour);
        Experiment.ID id = experiment.getID();
        Bucket.Label bucketLabel = assignment.getBucketLabel();
        AtomicInteger oldCount = hourMap.get(ExpBucket.getKey(id, bucketLabel)); // Equals method and hashcode method
        if (oldCount == null) {
            synchronized (lock) {
                oldCount = hourMap.get(ExpBucket.getKey(id, bucketLabel));
                if (oldCount == null) {
                    AtomicInteger count = new AtomicInteger(1);
                    hourMap.put(ExpBucket.getKey(id, bucketLabel), count);
                } else {
                    oldCount.getAndIncrement();
                }
            }
        } else {
            oldCount.getAndIncrement();
        }
    }

    public int getCount(Experiment experiment, Bucket.Label bucketLabel, int assignmentHour) {
        Map<String, AtomicInteger> hourMap = hourlyCountMap.get(assignmentHour);
        return hourMap.get(ExpBucket.getKey(experiment.getID(), bucketLabel)).get();
    }

    public void writeCounts() {
        // TODO: Use UTC time
        Date completedHour = getLastCompletedHour(System.currentTimeMillis());
        int assignmentHour = getHour(completedHour);
        String day = getDayString(completedHour);

        Iterator it = hourlyCountMap.get(assignmentHour).entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            UUID experimentID = getExpUUID(pair);
            String bucketLabel = getBucketLabel(pair);
            int count = (int)pair.getValue();
            hourlyBucketCountAccessor.incrementCountBy(experimentID, day, bucketLabel, assignmentHour, count);
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
        hourlyCountMap.put(assignmentHour, new ConcurrentHashMap<>());
    }

    public Date getLastCompletedHour(long time) {
        return new Date(time - 3600 * 1000);
    }

    public int getHour(Date completedHour) {
        return Integer.parseInt(hourFormatter.format(completedHour));
    }

    public String getDayString(Date completedHour) {
        DateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dayFormatter.format(completedHour);
    }

    public UUID getExpUUID(Map.Entry pair){
        String expIDString = pair.getKey().toString().substring(0, UUID_LENGTH);
        return UUID.fromString(expIDString);
    }

    public String getBucketLabel(Map.Entry pair){
        return pair.getKey().toString().substring(UUID_LENGTH);
    }
}
