/*******************************************************************************
 * Copyright 2017 Intuit
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
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.cassandra.accessor.count.HourlyBucketCountAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.intuit.wasabi.repository.cassandra.impl.CassandraAssignmentsRepository.NULL_LABEL;

public class AssignmentStats {

    private HourlyBucketCountAccessor hourlyBucketCountAccessor;
    private static Map<Integer, Map<String, AtomicLong>> hourlyCountMap;
    private static final Object lock = new Object();
    private final Logger LOGGER = LoggerFactory.getLogger(CassandraAssignmentsRepository.class);
    private final int UUID_LENGTH = 36;

    /**
     * Constructor
     */
    @Inject
    public AssignmentStats(HourlyBucketCountAccessor hourlyBucketCountAccessor) {
        this.hourlyBucketCountAccessor = hourlyBucketCountAccessor;
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
        LOGGER.debug("incrementCount - START: experiment={}, assignment={}", experiment, assignment);
        Optional<Bucket.Label> labelOptional = Optional.ofNullable(assignment.getBucketLabel());
        int assignmentHour = AssignmentStatsUtil.getHour(new Date(System.currentTimeMillis()));
        Map<String, AtomicLong> hourMap = hourlyCountMap.get(assignmentHour);
        // Using the experimentID and bucket label as the key for hourMap, which contains an hour's worth of counts
        Experiment.ID id = experiment.getID();
        Bucket.Label bucketLabel = labelOptional.orElseGet(() -> NULL_LABEL);
        String key = new ExperimentBucketKey(id, bucketLabel).getKey();
        AtomicLong oldCount = hourMap.get(key);        // oldCount is the value of a certain expID + bucket combo
        if (oldCount == null) {
            synchronized (lock) {                         // oldCount would be initialized to 1 twice w/o this
                oldCount = hourMap.get(key);
                if (oldCount == null) {                   // double-checked locking
                    AtomicLong count = new AtomicLong(1);
                    hourMap.put(key, count);
                } else {
                    oldCount.getAndIncrement();
                }
            }
        } else {
            oldCount.getAndIncrement();
        }
        LOGGER.debug("incrementCount - FINISHED");
    }

    /**
     * Returns the number of assignments for an experiment and bucket during a specified hour
     *
     * @param experiment
     * @param bucketLabel
     * @param assignmentHour
     * @return int representing the counts for a given bucket during the given hour
     */
    public long getCount(Experiment experiment, Bucket.Label bucketLabel, int assignmentHour) {
        Map<String, AtomicLong> hourMap = hourlyCountMap.get(assignmentHour);
        if (hourMap.get(new ExperimentBucketKey(experiment.getID(), bucketLabel).getKey()) == null){
            return 0;
        }else {
            return hourMap.get(new ExperimentBucketKey(experiment.getID(), bucketLabel).getKey()).get();
        }
    }

    /**
     * Writes hourly assignment counts to cassandra for the last completed hour based on current time
     */
    public void writeCounts() {
        Date completedHour = AssignmentStatsUtil.getLastCompletedHour(System.currentTimeMillis());
        int assignmentHour = AssignmentStatsUtil.getHour(completedHour);
        String day = AssignmentStatsUtil.getDayString(completedHour);

        for (String key : hourlyCountMap.get(assignmentHour).keySet()){
            String experimentID = key.substring(0, UUID_LENGTH);
            String bucketLabel = key.substring(UUID_LENGTH);
            UUID experimentUUID = UUID.fromString(experimentID);
            long count = hourlyCountMap.get(assignmentHour).get(key).get();
            hourlyBucketCountAccessor.incrementCountBy(count, experimentUUID, day, bucketLabel, assignmentHour);
            LOGGER.debug("Wrote counts for " + experimentUUID + bucketLabel + ". Count = " + count + ". Hour = " + assignmentHour);
        }
        hourlyCountMap.put(assignmentHour, new ConcurrentHashMap<>());
    }
}
