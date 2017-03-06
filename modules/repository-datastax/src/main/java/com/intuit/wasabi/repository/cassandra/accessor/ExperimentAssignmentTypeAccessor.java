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

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.ExperimentAssignmentType;
import com.intuit.wasabi.repository.cassandra.pojo.index.PageExperimentByAppNamePage;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Accessor interface
 * @param <ExperimentAssignmentType>
 */
@Accessor
public interface ExperimentAssignmentTypeAccessor {
    @Query("SELECT bucket_assignment FROM experiment_assignment_type "
    		+ "WHERE experiment_id = ? AND timestamp >= ? AND timestamp < ?")
    Result<ExperimentAssignmentType> selectBy(UUID experimentId, Date startTime, Date endTime);

    @Query("INSERT INTO experiment_assignment_type ( experiment_id, timestamp, bucket_assignment ) VALUES ( ?, ?, ? )")
    Result insert(UUID experimentId, Date timestamp, boolean bucketAssignment);

}
