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

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.index.StateExperimentIndex;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * State experiment index accedssor
 */
@Accessor
public interface StateExperimentIndexAccessor {

    /**
     * Insert a row into the table
     *
     * @param index_key
     * @param experiment_id
     * @param byteBuffer
     */
    @Query("insert into state_experiment_index(index_key, experiment_id, value) " +
            "values(?,?,?)")
    Statement insert(String index_key, UUID experiment_id, ByteBuffer byteBuffer);

    /**
     * Delete from table
     *
     * @param index_key
     * @param experiment_id
     */
    @Query("delete from state_experiment_index where index_key = ? and experiment_id = ?")
    Statement deleteBy(String index_key, UUID experiment_id);

    /**
     * Get rows by parameters
     *
     * @param index_key
     * @param experiment_id
     * @return result
     */
    @Query("select * from state_experiment_index where index_key = ? and experiment_id = ?")
    Result<StateExperimentIndex> selectByKeyAndExperimentId(String index_key, UUID experiment_id);

    /**
     * Get rows by parameters
     *
     * @param index_key
     * @return result rows
     */
    @Query("select * from state_experiment_index where index_key = ? ")
    Result<StateExperimentIndex> selectByKey(String index_key);

}
