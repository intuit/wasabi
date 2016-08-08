package com.intuit.wasabi.repository.cassandra.pojo;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name="app_roles")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AppRole {
    @PartitionKey(0)
    @Column(name = "app_name")
    String app_name;

    @PartitionKey(1)
    @Column(name = "user_id")
    String userId;

    String role;
}