package com.intuit.wasabi.repository.cassandra.pojo;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Table(name="user_experiment_index")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserExperimentByAppNameUserIdContextExperimentId {
    @PartitionKey(0)
    @Column(name = "app_name")
    String appName;

    @PartitionKey(1)
    @Column(name = "user_id")
    String userId;

    @PartitionKey(2)
    String context;

    @PartitionKey(3)
    @Column(name = "experiment_id")
    UUID experimentId;

    @Column(name = "bucket_label")
    String bucketLabel;
}