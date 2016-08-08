package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.annotations.Accessor;

@Accessor
public interface BucketAccessor {

//    @Query("select * from bucket where experiment_id = ?")
//    Bucket getBucketBy(UUID experimentId);
//
//    @Query("select * from bucket where experiment_id = ? and app_name = ?")
//    Result<Bucket> getBucketBy(UUID uuid, String appName);

}
