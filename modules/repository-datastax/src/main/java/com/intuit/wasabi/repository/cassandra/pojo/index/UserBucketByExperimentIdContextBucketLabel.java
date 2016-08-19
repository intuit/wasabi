package com.intuit.wasabi.repository.cassandra.pojo.index;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Table(name="user_bucket_index")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserBucketByExperimentIdContextBucketLabel {

    @PartitionKey(0)
    @Column(name = "experiment_id")
    UUID experimentId;

    @PartitionKey(1)
    String context;

    @PartitionKey(2)
    @Column(name = "bucket_label")
    String bucketLabel;

    @ClusteringColumn(0)
    @Column(name = "user_id")
    String userId;

    Date assigned;

}