package com.intuit.wasabi.repository.cassandra.pojo;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Table(name="experiment_user_index")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentUserByUserIdContextAppNameExperimentId {

    @PartitionKey(0)
    @Column(name = "user_id")
    String userId;

    @PartitionKey(1)
    String context;

    @PartitionKey(2)
    @Column(name = "app_name")
    String appName;

    @PartitionKey(3)
    @Column(name = "experiment_id")
    UUID experimentId;

    String bucket;

}