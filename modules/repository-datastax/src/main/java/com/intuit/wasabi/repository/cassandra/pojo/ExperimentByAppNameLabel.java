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

@Table(name="experiment_label_index")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentByAppNameLabel {
    @PartitionKey(0)
    @Column(name = "app_name")
    String appName;

    @PartitionKey(1)
    String label;

    UUID id;

    Date modified;

    @Column(name = "start_time")
    Date startTime;

    @Column(name = "end_time")
    Date endTime;

    String state;
}