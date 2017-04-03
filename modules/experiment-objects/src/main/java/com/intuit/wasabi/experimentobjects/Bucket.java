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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;

/**
 * An experiment bucket
 */
public class Bucket implements Serializable {

    @ApiModelProperty(value = "unique bucket label in the experiment", dataType = "String", required = true)
    private Bucket.Label label;
    @ApiModelProperty(value = "unique experiment ID", dataType = "UUID", required = true)
    private Experiment.ID experimentID;
    @ApiModelProperty(value = "probability of a user being assigned this bucket if they are in the experiment; " +
            "in range (0, 1]",
            required = true)
    private Double allocationPercent;
    @JsonProperty("isControl")
    @ApiModelProperty(value = "a single control bucket may be specified for each experiment", required = true)
    private Boolean control;
    @ApiModelProperty(value = "description of the bucket treatment", required = true)
    private String description;
    @ApiModelProperty(value = "optional snippet of HTML, JS, etc; returned with a user bucket assignment",
            required = true)
    private String payload;
    @ApiModelProperty(value = "state of the bucket", required = false)
    private State state;

    public Bucket() {
        super();
    }

    public static Builder newInstance(Experiment.ID experimentID, Bucket.Label label) {
        return new Builder(experimentID, label);
    }

    public static Builder from(Bucket bucket) {
        return new Builder(bucket);
    }

    public Experiment.ID getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(Experiment.ID value) {
        Preconditions.checkNotNull(value, "The Experiment.ID shall not be null for a Bucket");
        this.experimentID = value;
    }

    public Bucket.Label getLabel() {
        return label;
    }

    public void setLabel(Bucket.Label value) {
        if (value == null) {
            throw new IllegalArgumentException("The Bucket label can not be empty, choose a name!");
        }
        this.label = value;
    }

    @JsonIgnore
    public Boolean isControl() {
        return control;
    }

    @JsonIgnore
    public void setControl(Boolean value) {
        if (value == null) {
            throw new IllegalArgumentException("It has to be specified whether this Bucket is control");
        }
        this.control = value;
    }

    public Double getAllocationPercent() {
        return allocationPercent;
    }

    public void setAllocationPercent(Double value) {
        if (value == null || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Bucket allocation percent (" + value +
                    ") must be between 0.0 and 1.0");
        }
        this.allocationPercent = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String value) {
        this.payload = value;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public boolean isStateTransitionValid(State desiredState) {
        if (state.equals(desiredState)) {
            return true;
        }

        switch (state) {
            case OPEN:
                return desiredState.equals(State.CLOSED)
                        || desiredState.equals(State.EMPTY);
            case CLOSED:
                return desiredState.equals(State.EMPTY);
            default:
                assert false : "Unhandled bucket state case";
        }

        return false;
    }

    public enum State {
        OPEN, CLOSED, EMPTY;

        //TODO: this is a work in progress
        public enum BucketStateTransition {

            OPEN(State.OPEN, State.CLOSED, State.EMPTY),
            CLOSED(State.CLOSED, State.EMPTY),
            EMPTY(State.EMPTY);

            private static final Map<State, ArrayList<State>> m =
                    new EnumMap<>(State.class);

            static {
                for (BucketStateTransition trans :
                        BucketStateTransition.values()) {
                    for (State s : trans.getAllowedStateTransitions()) {
                        m.put(s, new ArrayList<>(
                                trans.getAllowedStateTransitions()));
                    }
                }
            }

            //TODO: remove state from enum - this is a work in progress
            private final transient List<State> allowedStateTransitions;

            BucketStateTransition(State... allowedTransitions) {
                this.allowedStateTransitions = asList(allowedTransitions);
            }

            private List<State> getAllowedStateTransitions() {
                return allowedStateTransitions;
            }

        }

    }

    public static class Builder {

        private Bucket instance;

        private Builder(Experiment.ID experimentID, Bucket.Label label) {
            instance = new Bucket();
            instance.experimentID = experimentID;
            instance.label = label;
            instance.control = false;
        }

        private Builder(Bucket other) {
            instance = new Bucket();
            instance.experimentID = other.getExperimentID();
            instance.label = other.getLabel();
            instance.control = other.isControl() == null ? Boolean.FALSE : other.isControl();
            instance.allocationPercent = other.getAllocationPercent();
            instance.description = other.getDescription();
            instance.payload = other.getPayload();
            instance.state = other.getState();
        }

        public Builder withExperimentID(final Experiment.ID experimentID) {
            instance.experimentID = experimentID;
            return this;
        }

        public Builder withControl(final Boolean isControl) {
            instance.control = isControl;
            return this;
        }

        public Builder withAllocationPercent(final Double samplingRate) {
            instance.allocationPercent = samplingRate;
            return this;
        }

        public Builder withDescription(final String description) {
            instance.description = description;
            return this;
        }

        public Builder withPayload(final String payload) {
            instance.payload = payload;
            return this;
        }

        public Builder withState(final State state) {
            instance.state = state;
            return this;
        }


        public Bucket build() {
            Bucket result = new Bucket();
            result.setControl(instance.isControl());
            result.setState(instance.getState());
            result.setLabel(instance.getLabel());
            result.setPayload(instance.getPayload());
            result.setAllocationPercent(instance.getAllocationPercent());
            result.setDescription(instance.getDescription());
            result.setExperimentID(instance.getExperimentID());
            instance = null;
            return result;
        }
    }

    @JsonSerialize(using = Bucket.Label.Serializer.class)
    @JsonDeserialize(using = Bucket.Label.Deserializer.class)
    public static class Label implements Serializable {

        private String label;

        private Label(String label) {
            this.label = Preconditions.checkNotNull(label);

            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("Bucket label cannot be " +
                        "an empty string");
            }

            if (!label.matches("^[_\\-$A-Za-z][_\\-$A-Za-z0-9]*")) {
                throw new InvalidIdentifierException("Bucket label \"" + label +
                        "\" must begin with a letter, dollar sign, or underscore");
            }
        }

        public static Label valueOf(String value) {
            return new Label(value);
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.label);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        public static class Serializer extends JsonSerializer<Label> {
            @Override
            public void serialize(Label label, JsonGenerator generator, SerializerProvider provider)
                    throws IOException {
                generator.writeString(label.toString());
            }
        }

        public static class Deserializer
                extends JsonDeserializer<Label> {
            @Override
            public Label deserialize(JsonParser parser, DeserializationContext context)
                    throws IOException {
                return Label.valueOf(parser.getText());
            }
        }
    }

    public static class BucketAuditInfo {
        private String attributeName;
        private String oldValue;
        private String newValue;

        public BucketAuditInfo(String attributeName, String oldValue, String newValue) {
            this.attributeName = attributeName;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }
    }
}
