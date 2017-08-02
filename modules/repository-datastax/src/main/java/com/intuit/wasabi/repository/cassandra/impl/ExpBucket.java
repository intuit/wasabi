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
        return expID.toString() + bucket.toString();
    }
}
