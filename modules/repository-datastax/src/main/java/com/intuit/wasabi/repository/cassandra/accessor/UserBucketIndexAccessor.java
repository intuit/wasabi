package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.annotations.Accessor;

@Accessor
public interface UserBucketIndexAccessor {

//    @Query("select count(*) from user_bucket_index where experiment_id = ? and context ='?' and bucket_label = '?'")
//    Result<Integer> countUserBy(UUID experimentId, String context, String bucketLable);
}
