package com.intuit.wasabi.repository.cassandra.pojo.count;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Table(name="bucket_assignment_counts")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BucketAssignmentCount {
    @PartitionKey(0)
    @Column(name = "experiment_id")
    UUID experimentId;

    @PartitionKey(1)
    @Column(name = "bucket_label")
    String bucketLabel;

    @Column(name = "bucket_assignment_count")
    long count;

}