package com.intuit.wasabi.repository.cassandra.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
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

import static com.intuit.wasabi.repository.cassandra.impl.CassandraAssignmentsRepository.NULL_LABEL;


public class AssignmentStats {


    private Experiment experiment;
    private Assignment assignment;
    private boolean countUp;
    private Date date;
    private boolean assignUserToExport;
    private boolean assignBucketCount;
    private HourlyBucketCountAccessor hourlyBucketCountAccessor;
    private Map<Integer, Map<ExpBucket, AtomicInteger>> hourlyCountMap;

    /**
     * Constructor
     *
     * @param experiment                    experiment object
     * @param assignment                    assignment object
     * @param countUp                       boolean value of countup
     * @param date                          date
     * @param assignUserToExport            assignUserToExport
     * @param assignBucketCount             assignBucketCount
     */
    @Inject
    public AssignmentStats(Experiment experiment, Assignment assignment, boolean countUp, Date date,
                           final @Named("assign.user.to.export") Boolean assignUserToExport,
                           final @Named("assign.bucket.count") Boolean assignBucketCount) {
        super();


        this.experiment = experiment;
        this.assignment = assignment;
        this.countUp = countUp;
        this.date = date;
        this.assignUserToExport = assignUserToExport;
        this.assignBucketCount = assignBucketCount;
        this.hourlyBucketCountAccessor = hourlyBucketCountAccessor;
        this.hourlyCountMap = new ConcurrentHashMap<>();

        for (int hour = 0; hour <= 23; hour++){
            hourlyCountMap.put(hour, new ConcurrentHashMap<>());
        }

    }

    public void run(){
        Optional<Bucket.Label> labelOptional = Optional.ofNullable(assignment.getBucketLabel());
        Date completedHour = getLastCompletedHour(System.currentTimeMillis());
        int eventTimeHour = getHour(completedHour);
        ExpBucket expBucket = new ExpBucket(experiment.getID(), assignment.getBucketLabel());
        fillMaps(expBucket, eventTimeHour);

//        Map<ExpBucket, AtomicInteger> hourMap = hourlyCountMap.get(eventTimeHour);
//        //hourMap.putIfAbsent(expBucket, new AtomicInteger(0)); // This returns the value, doesn't update it
//        AtomicInteger oldCount = hourMap.get(expBucket);
//        if (oldCount == null){
//            synchronized (hourMap) {                                // First would be initialized to 1 twice w/o this
//                oldCount = hourMap.get(expBucket);
//                if (oldCount == null){                              // Double-checked locking
//                    oldCount = new AtomicInteger(1);
//                    hourMap.put(expBucket, oldCount);
//                }else{
//                    oldCount.getAndIncrement();
//                }
//            }
//        }else{
//            oldCount.getAndIncrement();
//        }


        // TODO: Figure out Bucket.Label --> toString OR labelOptional.orElseGet()
        // TODO: This is writing to the DB after every assignment, do this only once per hour instead (hour change var)

        int count = hourlyCountMap.get(eventTimeHour).get(expBucket).incrementAndGet();
        hourlyBucketCountAccessor.incrementCountBy(experiment.getID().getRawID(),
                labelOptional.orElseGet(() -> NULL_LABEL).toString(), eventTimeHour, count);
        hourlyCountMap.put(eventTimeHour, null);            // Set the hour's data to null to delete unnecessary counts
    }

    public void fillMaps(ExpBucket expBucket, int eventTimeHour){
        Map<ExpBucket, AtomicInteger> hourMap = hourlyCountMap.get(eventTimeHour);
        AtomicInteger oldCount = hourMap.get(expBucket);
        if (oldCount == null){
            synchronized (hourMap) {                                // First would be initialized to 1 twice w/o this
                oldCount = hourMap.get(expBucket);
                if (oldCount == null){                              // Double-checked locking
                    oldCount = new AtomicInteger(1);
                    hourMap.put(expBucket, oldCount);
                }else{
                    oldCount.getAndIncrement();
                }
            }
        }else{
            oldCount.getAndIncrement();
        }
    }


    public static Date getLastCompletedHour(long time) {
        return new Date(time - 3600 * 1000);
    }

    public static int getHour(Date completedHour) {
        DateFormat hourFormatter = new SimpleDateFormat("HH");
        return Integer.parseInt(hourFormatter.format(completedHour));
    }


}

