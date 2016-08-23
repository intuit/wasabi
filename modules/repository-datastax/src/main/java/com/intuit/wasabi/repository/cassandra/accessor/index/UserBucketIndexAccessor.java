package com.intuit.wasabi.repository.cassandra.accessor.index;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

import java.util.Date;
import java.util.UUID;

@Accessor
public interface UserBucketIndexAccessor {

    @Query("select count(*) from user_bucket_index where experiment_id = ? and context = ? and "
    		+ "bucket_label = ? ")
    ResultSet countUserBy(UUID experimentId, String context, String bucketLable);

    @Query("insert into user_bucket_index (experiment_id, user_id, context, assigned, bucket_label) values (?, ?, ?, ?, ?)")
    void insertBy(UUID experimentId, String userId, String context, Date assigned, String bucketLabel);

    @Query("delete from user_bucket_index where experiment_id = ? and user_id = ? and context = ? and bucket_label = ?")
    void deleteBy(UUID experimentId, String userId, String context, String bucketLabel);

}
