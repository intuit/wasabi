/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.accessor.index;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentUserByUserIdContextAppNameExperimentId;

import java.util.UUID;

/**
 * Interface for accessor
 */
@Accessor
public interface ExperimentUserIndexAccessor {
    @Query("insert into experiment_user_index (user_id, context, app_name, experiment_id, bucket)" +
            " values (?, ?, ?, ?, ?)")
    ResultSet insertBy(String userId, String context, String appName, UUID experimentId, String bucketLabel);

    @Query("insert into experiment_user_index (user_id, context, app_name, experiment_id)" +
            " values (?, ?, ?, ?)")
    ResultSet insertBy(String userId, String context, String appName, UUID experimentId);


    @Query("insert into experiment_user_index (user_id, context, app_name, experiment_id, bucket)" +
            " values (?, ?, ?, ?, ?)")
    BoundStatement insertBoundStatement(String userId, String context, String appName, UUID experimentId, String bucketLabel);

    @Query("insert into experiment_user_index (user_id, context, app_name, experiment_id)" +
            " values (?, ?, ?, ?)")
    BoundStatement insertBoundStatement(String userId, String context, String appName, UUID experimentId);


    @Query("select * from experiment_user_index where user_id = ? and app_name = ? and context = ?")
    Result<ExperimentUserByUserIdContextAppNameExperimentId> selectBy(String userId, String appName, String context);


    @Query("select * from experiment_user_index where user_id = ? and app_name = ? and context = ?")
    ListenableFuture<Result<ExperimentUserByUserIdContextAppNameExperimentId>> asyncSelectBy(String userId, String appName, String context);

    @Query("select * from experiment_user_index where user_id = ? and app_name = ? and experiment_id = ? and context = ?")
    ListenableFuture<Result<ExperimentUserByUserIdContextAppNameExperimentId>> asyncSelectBy(String userId, String appName, UUID experimentId, String context);

    @Query("delete from experiment_user_index where user_id = ? and experiment_id = ? and context = ? and app_name = ?")
    ResultSet deleteBy(String userId, UUID experimentId, String context, String appName);

}
