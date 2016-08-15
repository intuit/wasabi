package com.intuit.wasabi.repository.cassandra.pojo;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Table(name="application")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @PartitionKey(0)
    @Column(name = "app_name")
    String appName;

    @Singular  List<UUID> priorities;
}