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
package com.intuit.wasabi.repository.cassandra.accessor.audit;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.audit.ExperimentAuditLog;

import java.util.Date;
import java.util.UUID;

/**
 * Experiment audit log accessor
 */
@Accessor
public interface ExperimentAuditLogAccessor {
    /**
     * Insert row into table
     *
     * @param experimentId
     * @param modified
     * @param attributeName
     * @param oldValue
     * @param newValue
     * @return result
     */
    @Query("insert into experiment_audit_log (experiment_id, modified,attribute_name, old_value, new_value) " +
            "values(?,?,?,?,?)")
    ResultSet insertBy(UUID experimentId, Date modified, String attributeName, String oldValue, String newValue);

    /**
     * select row by argument
     *
     * @param experimentId
     * @return result
     */
    @Query("select * from experiment_audit_log where experiment_id = ? ")
    Result<ExperimentAuditLog> selectBy(UUID experimentId);

    /**
     * Delete row by argument
     *
     * @param experimentId
     */
    @Query("delete from experiment_audit_log where experiment_id = ? ")
    void deleteBy(UUID experimentId);
}