package com.intuit.wasabi.repository.cassandra.pojo.audit;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Table(name="experiment_audit_log")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentAuditLog {

    @PartitionKey(0)
    @Column(name = "experiment_id")
    UUID experimentId;

    @PartitionKey(1)
    Date modified;

    @PartitionKey(2)
    @Column(name = "attribute_name")
    String attributeName;

    @Column(name = "old_value")
    String oldValue;

    @Column(name = "new_value")
    String newValue;

}