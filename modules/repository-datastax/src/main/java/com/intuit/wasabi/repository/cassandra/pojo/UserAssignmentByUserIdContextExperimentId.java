package com.intuit.wasabi.repository.cassandra.pojo;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;


//TODO: this seems redundent since UserAssignment contains the same data with same primary key but different order
@Table(name="user_assignment")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserAssignmentByUserIdContextExperimentId {
    @PartitionKey(0)
    @Column(name = "user_id")
    String userId;

    @PartitionKey(1)
    String context;

    @PartitionKey(2)
    @Column(name = "experiment_id")
    UUID experimentId;


    @Column(name = "bucket_label")
    String bucketLabel;

    Date created;

}