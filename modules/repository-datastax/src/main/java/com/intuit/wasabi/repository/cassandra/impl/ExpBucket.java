package com.intuit.wasabi.repository.cassandra.impl;


import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Bucket;


class ExpBucket {
    private final Experiment.ID expID;
    private final Bucket.Label bucket;

    ExpBucket(Experiment.ID expID, Bucket.Label bucket){
        this.expID = expID;
        this.bucket = bucket;
    }

    public String getKey(){
        // TODO: Instead of concatenating two strings, redefine hashcode and equals methods.
        // Then, use expBucket objects as keys in the AssignmentStats hourlyCountMap.
        // The current method works but this may improve performance.
        return expID.toString() + bucket.toString();
    }
}
