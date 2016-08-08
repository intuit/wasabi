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
public class ExperimentPage {
    @PartitionKey(0)
    @Column(name = "exp_id")
    UUID experimentId;

    @PartitionKey(1)
    String page;

    boolean assign;
}