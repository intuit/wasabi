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
package com.intuit.wasabi.experimentobjects;


import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

// TODO: Confirm if PageExperiment and ExperimentPage could be combined
public class PageExperiment implements Cloneable, Serializable {
    @ApiModelProperty(value = "unique experiment ID", required = true)
    private Experiment.ID id;

    @ApiModelProperty(value = "experiment label; unique within the application", required = true)
    private Experiment.Label label;

    @ApiModelProperty(value = "flag to allow new assignments", required = true)
    private boolean allowNewAssignment;

    public static Builder withAttributes(Experiment.ID experimentID, Experiment.Label label, boolean allowNewAssignment) {
        return new Builder(experimentID, label, allowNewAssignment);
    }

    public static class Builder {
        private PageExperiment instance;

        public Builder(Experiment.ID experimentID, Experiment.Label label, boolean allowNewAssignment) {
            instance = new PageExperiment();
            instance.id = experimentID;
            instance.label = label;
            instance.allowNewAssignment = allowNewAssignment;
        }

        public PageExperiment build() {
            PageExperiment result = instance;
            instance = null;
            return result;
        }

        public Experiment.ID getId() {
            return instance.id;
        }

        public void setId(Experiment.ID id) {
            instance.id = id;
        }

        public Experiment.Label getLabel() {
            return instance.label;
        }

        public void setLabel(Experiment.Label label) {
            instance.label = label;
        }

        public boolean getAllowNewAssignment() {
            return instance.allowNewAssignment;
        }

        public void setAllowNewAssignment(boolean allowNewAssignment) {
            instance.allowNewAssignment = allowNewAssignment;
        }
    }

    public Experiment.ID getId() {
        return id;
    }

    public void setId(Experiment.ID id) {
        this.id = id;
    }

    public Experiment.Label getLabel() {
        return label;
    }

    public void setLabel(Experiment.Label label) {
        this.label = label;
    }

    public boolean getAllowNewAssignment() {
        return allowNewAssignment;
    }

    public void setAllowNewAssignment(boolean allowNewAssignment) {
        this.allowNewAssignment = allowNewAssignment;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "PageExperiment{" +
                "id=" + id +
                ", label=" + label +
                ", allowNewAssignment=" + allowNewAssignment + "}";
    }

    @Override
    public PageExperiment clone() throws CloneNotSupportedException {
        return (PageExperiment) super.clone();
    }

}
