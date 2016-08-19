package com.intuit.wasabi.repository.cassandra.accessor.audit;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

import java.util.Date;
import java.util.UUID;

@Accessor
public interface ExperimentAuditLogAccessor {
    @Query("insert into experiment_audit_log (experiment_id, modified,attribute_name, old_value, new_value) " +
            "values(?,?,?,?,?)")
    ResultSet insertBy(UUID experimentId, Date modified, String attributeName, String oldValue, String newValue);
}