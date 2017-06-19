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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentByAppNameLabel;

import java.util.Date;
import java.util.UUID;

/**
 * Accessor for experiment label index table
 */
@Accessor
public interface ExperimentLabelIndexAccessor {

    /**
     * Get experiment by app name and label
     *
     * @param appName
     * @param Label
     * @return experiment label result
     */
    @Query("select * from experiment_label_index where app_name = ? and label = ?")
    Result<ExperimentByAppNameLabel> getExperimentBy(String appName, String Label);

    /**
     * Update experiment label index
     *
     * @param uuid
     * @param modified
     * @param startTime
     * @param endTime
     * @param state
     * @param appName
     * @param label
     * @return result set
     */
    @Query("update experiment_label_index set id = ?, modified = ?, start_time = ?, end_time = ?, state = ? " +
            "where app_name = ? and label = ?")
    ResultSet insertOrUpdateBy(UUID uuid, Date modified, Date startTime, Date endTime, String state, String appName, String label);

    /**
     * Insert experiment label index statement
     *
     * @param uuid
     * @param modified
     * @param startTime
     * @param endTime
     * @param state
     * @param appName
     * @param label
     * @return Statement
     */
    @Query("update experiment_label_index set id = ?, modified = ?, start_time = ?, end_time = ?, state = ? " +
            "where app_name = ? and label = ?")
    Statement insertOrUpdateStatementBy(UUID uuid, Date modified, Date startTime, Date endTime, String state, String appName, String label);

    /**
     * Delete entry from experiment label index
     *
     * @param appName
     * @param label
     * @return result set
     */
    @Query("delete from experiment_label_index where app_name = ? and label = ?")
    ResultSet deleteBy(String appName, String label);
}