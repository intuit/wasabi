/*******************************************************************************
 * Copyright 2017 Intuit
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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentTagsByApplication;

import java.util.Set;
import java.util.UUID;

/**
 * Accessor for the tags associated with Experiments.
 */
@Accessor
public interface ExperimentTagAccessor {

    @Query("select * from experiment_tag where app_name = ?")
    ListenableFuture<Result<ExperimentTagsByApplication>> getExperimentTagsAsync(String appName);

    @Query("insert into experiment_tag(app_name, exp_id, tags) values (?,?,?)")
    Statement insert(String appName, UUID experimentId, Set<String> tags);

}
