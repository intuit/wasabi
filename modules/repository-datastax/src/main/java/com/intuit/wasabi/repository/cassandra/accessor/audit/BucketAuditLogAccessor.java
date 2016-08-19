package com.intuit.wasabi.repository.cassandra.accessor.audit;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

import java.util.Date;
import java.util.UUID;

@Accessor
public interface BucketAuditLogAccessor {
    @Query("insert into bucket_audit_log (experiment_id, label, modified, attribute_name, old_value, new_value) " +
            "values (?,?,?,?,?,?)")
    ResultSet insertBy(UUID experimentId, String label, Date modified, String attributeName, String oldValue, String newValue);
}