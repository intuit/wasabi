package com.intuit.wasabi.repository.cassandra.impl;


import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Bucket;


class ExpBucket {
    private Experiment.ID expID;
    private Bucket.Label bucket;

    ExpBucket(Experiment.ID expID, Bucket.Label bucket){
        this.expID = expID;
        this.bucket = bucket;
    }

    public static String getKey(Experiment.ID expID, Bucket.Label bucket){
        // TODO: Instead of concatenating two strings, redefine hashcode and equals methods.
        // This ensures that two different objects with same experiment ID and bucket label compare as equal.
        // Then, use expBucket objects as keys in the AssignmentStats hourlyCountMap.
        return expID.toString() + bucket.toString();
    }
}
