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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

// TODO: Confirm if PageExperiment and ExperimentPage could be combined
public class ExperimentPage implements Cloneable {

    @ApiModelProperty(value = "name of the page", dataType = "String", required = true)
    private Page.Name name;

    @ApiModelProperty(value = "flag to allow new assignments", required = true)
    private boolean allowNewAssignment;

    public static Builder withAttributes(Page.Name name, boolean allowNewAssignment) {
        return new Builder(name, allowNewAssignment);
    }

    public static class Builder {
        private ExperimentPage instance;

        public Builder(Page.Name name, boolean allowNewAssignment) {
            super();
            instance = new ExperimentPage();
            instance.name = name;
            instance.allowNewAssignment = allowNewAssignment;
        }

        public ExperimentPage build() {
            ExperimentPage result = instance;
            instance = null;
            return result;
        }
    }

    public Page.Name getName() {
        return name;
    }

    public void setName(Page.Name name) {
        this.name = name;
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
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public ExperimentPage clone() throws CloneNotSupportedException {
        return (ExperimentPage) super.clone();
    }
}
