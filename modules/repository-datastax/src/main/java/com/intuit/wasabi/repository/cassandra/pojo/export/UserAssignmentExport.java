package com.intuit.wasabi.repository.cassandra.pojo.export;

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

@Table(name="user_assignment_export")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserAssignmentExport {
    @PartitionKey(0)
    @Column(name = "experiment_id")
    UUID experimentId;

    @PartitionKey(1)
    @Column(name = "day_hour")
    Date dayHour;


    @ClusteringColumn(0)
    String Context;

    @ClusteringColumn(1)
    @Column(name = "is_bucket_null")
    boolean nullBucket;

    @ClusteringColumn(2)
    @Column(name = "user_id")
    String userId;

    @Column(name = "bucket_label")
    String bucketLabel;

    Date created;

}