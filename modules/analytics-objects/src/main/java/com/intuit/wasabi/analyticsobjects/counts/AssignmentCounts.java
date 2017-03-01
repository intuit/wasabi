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
package com.intuit.wasabi.analyticsobjects.counts;

import com.intuit.wasabi.experimentobjects.Experiment;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * Assignment count wrapper for an experiment
 */
public class AssignmentCounts {

    @ApiModelProperty(value = "UUID for the experiment")
    private Experiment.ID experimentID;
    @ApiModelProperty(value = "assignments count per bucket")
    private List<BucketAssignmentCount> assignments;
    @ApiModelProperty(value = "total assignments delivered")
    private TotalUsers totalUsers;

    private AssignmentCounts() {
        super();
    }

    public Experiment.ID getExperimentID() {
        return experimentID;
    }

    public List<BucketAssignmentCount> getAssignments() {
        return assignments;
    }

    public TotalUsers getTotalUsers() {
        return totalUsers;
    }

    public static class Builder {
        private AssignmentCounts instance;

        public Builder() {
            super();
            instance = new AssignmentCounts();
        }

        public Builder(AssignmentCounts other) {
            this();
            instance.experimentID = other.experimentID;
            instance.assignments = other.assignments;
            instance.totalUsers = other.totalUsers;
        }

        public Builder withExperimentID(Experiment.ID experimentID) {
            instance.experimentID = experimentID;
            return this;
        }

        public Builder withBucketAssignmentCount(List<BucketAssignmentCount> assignments) {
            instance.assignments = assignments;
            return this;
        }

        public Builder withTotalUsers(TotalUsers totalUsers) {
            instance.totalUsers = totalUsers;
            return this;
        }

        public AssignmentCounts build() {
            AssignmentCounts result = instance;
            instance = null;
            return result;
        }
    }
}
