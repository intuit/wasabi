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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.repository.cassandra.pojo.Experiment;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentTagsByApplication;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Accessor interface
 */
@Accessor
public interface ExperimentAccessor {

    @Query("select * from experiment where id IN ? ")
    Result<Experiment> getExperiments(List<UUID> experimentIds);

    @Query("select * from experiment where id = ?")
    Result<Experiment> getExperimentById(UUID experimentID);

    @Query("select * from experiment where id = ?")
    ListenableFuture<Result<Experiment>> asyncGetExperimentById(UUID experimentID);

    @Query("delete from experiment where id = ?")
    void deleteExperiment(UUID id);

    //TODO: figure out a better name?
    @Query("update experiment set state = ?, modified = ? where id = ?")
    ResultSet updateExperiment(String state, Date modifiedOn, UUID experimentId);

    @Query("update experiment " +
            "set description = ?, hypothesis_is_correct = ?, results = ?," +
            "rule = ?, sample_percent = ?, " +
            "start_time = ?, end_time = ?, " +
            "state=?, label=?, app_name=?, modified=? , is_personalized=?, model_name=?, model_version=?," +
            " is_rapid_experiment=?, user_cap=?, tags=?, source_url=?, experiment_type=?" +
            " where id = ?")
    ResultSet updateExperiment(String description, String hypothesisIsCorrect, String results,
                               String rule, double sample_percent,
                               Date start_time, Date end_time, String state, String label, String app_name,
                               Date modified, boolean is_personalized, String model_name, String model_version,
                               boolean is_rapid_experiment, int user_cap, Set<String> tags, String sourceURL, String experimentType, UUID experimentId);


    @Query("select * from experiment where app_name = ?")
    Result<Experiment> getExperimentByAppName(String appName);

    @Query("select * from experiment where app_name = ?")
    ListenableFuture<Result<Experiment>> asyncGetExperimentByAppName(String appName);

    @Query("select * from experiment where id = ?")
    Result<Experiment> selectBy(UUID experimentId);


    @Query("insert into experiment " +
            "(id, description, hypothesis_is_correct, results, rule, sample_percent, start_time, end_time, " +
            "   state, label, app_name, created, modified, is_personalized, model_name, model_version," +
            " is_rapid_experiment, user_cap, creatorid, tags, source_url, experiment_type) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    Statement insertExperiment(UUID experimentId, String description, String hypothesisIsCorrect, String results,
                          String rule, double samplePercent,
                          Date startTime, Date endTime, String state, String label, String appName,
                          Date created, Date modified, boolean isPersonalized, String modelName,
                          String modelVersion, boolean isRapidExperiment, int userCap, String creatorid,
                          Set<String> tags, String sourceURL, String experimentType);

    @Query("select tags from experiment where app_name = ?")
    Result<ExperimentTagsByApplication> getAllTags(String appName);
}
