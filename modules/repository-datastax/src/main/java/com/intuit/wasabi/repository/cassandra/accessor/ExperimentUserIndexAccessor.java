package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.ExperimentUserByUserIdContextAppNameExperimentId;

import java.util.UUID;

@Accessor
public interface ExperimentUserIndexAccessor {
    @Query("insert into experiment_user_index (user_id, context, app_name, experiment_id, bucket)" +
            " values (?, ?, ?, ?, ?)")
    ResultSet insertBy(String userId, String context, String appName, UUID experimentId, String bucketLabel);

    @Query("insert into experiment_user_index (user_id, context, app_name, experiment_id)" +
            " values (?, ?, ?, ?)")
    ResultSet insertBy(String userId, String context, String appName, UUID experimentId);

    
    @Query("select * from experiment_user_index where user_id = ? and app_name = ? and context = ?")
    Result<ExperimentUserByUserIdContextAppNameExperimentId> selectBy(String userId, String appName, String context);

    @Query("delete from experiment_user_index where user_id = ? and experiment_id = ? and context = ? and app_name = ?")
    ResultSet deleteBy(String userId, UUID experimentId, String context, String appName);

}
