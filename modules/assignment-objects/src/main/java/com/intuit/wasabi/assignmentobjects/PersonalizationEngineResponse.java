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
package com.intuit.wasabi.assignmentobjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Map;


@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalizationEngineResponse {

    @ApiModelProperty(value = "Decision Engine transaction id", required = true)
    private String tid;
    @ApiModelProperty(value = "Decision Engine data")
    private Map<String, Double> data;
    @ApiModelProperty(value = "Decision Engine model")
    private String model;

    protected PersonalizationEngineResponse() {
        super();
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public Map<String, Double> getData() {
        return data;
    }

    public void setData(Map<String, Double> data) {
        this.data = data;
    }

    public String getModel() {

        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public static Builder withTid(String tid) {
        return new Builder(tid);
    }

    public static Builder from(PersonalizationEngineResponse personalizationEngineResponse) {
        return new Builder(personalizationEngineResponse);
    }

    public static class Builder {

        private PersonalizationEngineResponse instance;

        private Builder(String tid) {
            super();
            instance = new PersonalizationEngineResponse();
            instance.tid = Preconditions.checkNotNull(tid);
        }

        private Builder(PersonalizationEngineResponse other) {
            this(other.tid);
            instance.data = other.data;
            instance.model = other.model;
        }


        public Builder withModel(final String model) {
            instance.model = model;
            return this;
        }

        public Builder withData(final Map<String, Double> data) {
            instance.data = data;
            return this;
        }

        public PersonalizationEngineResponse build() {
            PersonalizationEngineResponse result = instance;
            instance = null;
            return result;
        }
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
}
