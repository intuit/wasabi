package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.UserAssignment;

import java.util.Date;
import java.util.UUID;

@Accessor
public interface UserAssignmentAccessor {
    @Query("insert into user_assignment (experiment_id, user_id, context, created, bucket_label)" +
            " values (?, ?, ?, ?, ?)")
    ResultSet insertBy(UUID uuid, String userId, String context, Date created, String bucketLabel);

    @Query("insert into user_assignment (experiment_id, user_id, context, created)" +
            " values (?, ?, ?, ?, ?)")
    ResultSet insertBy(UUID uuid, String userId, String context, Date created);

    @Query("select * from user_assignment where experiment_id = ? and user_id = ? and context = ?")
    Result<UserAssignment> selectBy(UUID experimentId, String userId, String context);

    @Query("delete from user_assignment where experiment_id = ? and user_id = ? and context = ?")
    ResultSet deleteBy(UUID experimentId, String userId, String context);

}
