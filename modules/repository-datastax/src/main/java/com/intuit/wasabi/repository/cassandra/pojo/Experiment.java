/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.pojo;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

/*****
 * Watchouts:
 * 1) all getters and setters must be public
 * 2) must have a public default constructor
 * 3) for boolean method, uses setXXXX for set value, but isXXXX for get value
 */

@Table(name = "experiment")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {

    @PartitionKey
    private UUID id;

    private String description;

    @Column(name = "sample_percent")
    private Double samplePercent;

    @Column(name = "start_time")
    private Date startTime;

    @Column(name = "end_time")
    private Date endTime;

    private String state;

    @Column(name = "app_name")
    private String appName;

    private String label;

    private Date created;

    private Date modified;

    private String rule;

    @Column(name = "model_name")
    private String modelName = "";

    @Column(name = "model_version")
    private String modelVersion = "";

    @Column(name = "is_personalized")
    private boolean personalized = false;

    @Column(name = "is_rapid_experiment")
    private boolean rapidExperiment = false;

    @Column(name = "user_cap")
    private int userCap = Integer.MAX_VALUE;

    @Column(name = "creatorid")
    private String creatorId;

    @Column(name = "tags")
    private Set<String> tags;

    @Column(name = "hypothesis_is_correct")
    private String hypothesisIsCorrect;

    @Column(name = "results")
    private String results;

    @Column(name = "source_url")
    private String sourceURL;

    @Column(name = "experiment_type")
    private String experimentType;


}