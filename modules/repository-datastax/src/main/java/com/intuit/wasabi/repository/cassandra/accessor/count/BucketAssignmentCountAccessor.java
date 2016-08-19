package com.intuit.wasabi.repository.cassandra.accessor.count;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.count.BucketAssignmentCount;

import java.util.UUID;

@Accessor
public interface BucketAssignmentCountAccessor {
    @Query("UPDATE bucket_assignment_counts SET bucket_assignment_count = bucket_assignment_count + 1 " +
            "WHERE experiment_id =? and bucket_label = ?")
    ResultSet incrementCountBy(UUID experimentId, String bucketLabel);

    @Query("UPDATE bucket_assignment_counts SET bucket_assignment_count = bucket_assignment_count - 1 " +
            "WHERE experiment_id =? and bucket_label = ?")
    ResultSet decrementCountBy(UUID experimentId, String bucketLabel);

    @Query("SELECT * FROM bucket_assignment_counts WHERE experiment_id =?")
    Result<BucketAssignmentCount> selectBy(UUID experimentId);

}
