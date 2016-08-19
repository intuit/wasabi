package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.export.UserAssignmentExport;

import java.util.Date;
import java.util.UUID;

@Accessor
public interface UserAssignmentExportAccessor {
    @Query("insert into user_assignment_export (experiment_id, user_id, context, created, day_hour, bucket_label, is_bucket_null)" +
            " values (?, ?, ?, ?, ?, ?, ?)")
    ResultSet insertBy(UUID uuid, String userId, String context, Date created, Date dayHour, String bucketLabel, boolean isBucketNull);

    @Query("select * from user_assignment_export where experiment_id = ? and day_hour = ? and context = ?")
    Result<UserAssignmentExport> selectBy(UUID experimentId, Date dayHour, String context);

    @Query("select * from user_assignment_export where experiment_id = ? and day_hour = ? and context = ? and is_bucket_null = ?")
    Result<UserAssignmentExport> selectBy(UUID experimentId, Date dayHour, String context, boolean isBucketNull);
}
