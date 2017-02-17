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
import com.intuit.wasabi.repository.cassandra.pojo.audit.BucketAuditLog;

import java.util.Date;
import java.util.UUID;

/**
 * Bucket audit log accessor
 */
@Accessor
public interface BucketAuditLogAccessor {

    /**
     * Insert a row in to audit log table
     *
     * @param experimentId
     * @param label
     * @param modified
     * @param attributeName
     * @param oldValue
     * @param newValue
     * @return result
     */
    @Query("insert into bucket_audit_log (experiment_id, label, modified, attribute_name, old_value, new_value) " +
            "values (?,?,?,?,?,?)")
    ResultSet insertBy(UUID experimentId, String label, Date modified, String attributeName, String oldValue, String newValue);

    /**
     * Get rows by arguments
     *
     * @param experimentId
     * @param label
     * @return result
     */
    @Query("select * from bucket_audit_log where experiment_id = ? and label = ? ")
    Result<BucketAuditLog> selectBy(UUID experimentId, String label);

    /**
     * Delete row from table by argument
     *
     * @param experimentId
     * @param label
     * @return result
     */
    @Query("delete from bucket_audit_log where experiment_id = ? and label = ? ")
    ResultSet deleteBy(UUID experimentId, String label);
}