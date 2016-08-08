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

/*****
 * Watchouts:
 * 1) all getters and setters must be public
 * 2) must have a public default constructor
 * 3) for boolean method, uses setXXXX for set value, but isXXXX for get value
 */

@Table(name="experiment")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {

    @PartitionKey
    private UUID id;

    private String description;

    @Column(name="sample_percent")
    private double samplePercent;

    @Column(name="start_time")
    private Date startTime;

    @Column(name="end_time")
    private Date endTime;

    private String state;

    @Column(name="app_name")
    private String appName;

    private Date created;

    private Date modified;

    private String rule;

    @Column(name="model_name")
    private String modelName;

    @Column(name="model_version")
    private String modelVersion;

    @Column(name="is_personalized")
    private boolean personalized;

    @Column(name="is_rapid_experiment")
    private boolean rapidExperiment;

    @Column(name="user_cap")
    private int userCap;

    @Column(name="creatorid")
    private String creatorId;

}