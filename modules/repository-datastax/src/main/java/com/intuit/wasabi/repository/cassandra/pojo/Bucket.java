package com.intuit.wasabi.repository.cassandra.pojo;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Table(name="bucket")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Bucket {
    @PartitionKey(0)
    @Column(name="experiment_id")
    UUID experimentId;

    @PartitionKey(1)
    String label;

    String state;

    String description;

    double allocation;

    @Column(name="is_control")
    boolean control;
    
    String payload;
}