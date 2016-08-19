package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

import java.util.UUID;

@Accessor
public interface UserExperimentIndexAccessor {
    @Query("insert into user_experiment_index (user_id, context, app_name, experiment_id, bucket)" +
            " values (?, ?, ?, ?, ?)")
    ResultSet insertBy(String userId, String context, String appName, UUID experimentId, String bucketLabel);

    @Query("insert into user_experiment_index (user_id, context, app_name, experiment_id)" +
            " values (?, ?, ?, ?)")
    ResultSet insertBy(String userId, String context, String appName, UUID experimentId);


    @Query("delete from user_experiment_index where user_id = ? and experiment_id = ? and context = ? and app_name = ?")
    ResultSet deleteBy(String userId, String experimentId, String context, String appName);

}
